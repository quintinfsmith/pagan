package com.qfs.apres.soundfont

import android.content.res.AssetManager
import com.qfs.apres.toUInt
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream

class Riff(private var file_path: String, init_callback: ((riff: Riff) -> Unit)? = null) {
    class InputStreamClosed : Exception("Input Stream is Closed")
    data class ListChunkHeader(
        val index: Int,
        val tag: String,
        val size: Int,
        val type: String
    )

    data class SubChunkHeader(
        val index: Int,
        val tag: String,
        val size: Int
    )

    var list_chunks: MutableList<ListChunkHeader> = mutableListOf()
    var sub_chunks: MutableList<List<SubChunkHeader>> = mutableListOf()
    private var input_stream: InputStream? = null
    private var input_position: Int = 0

    init {
        this.open_stream()

        this.get_string(0, 4) // fourcc
        val riff_size = this.get_little_endian(4, 4)
        this.get_string(8, 4) // typecc

        var working_index = 12
        while (working_index < riff_size - 4) {
            val header_index = working_index - 12
            val tag = this.get_string(working_index, 4)
            working_index += 4

            val chunk_size = this.get_little_endian(working_index, 4)
            working_index += 4

            val type = this.get_string(working_index, 4)
            working_index += 4

            this.list_chunks.add(
                ListChunkHeader(
                    header_index,
                    tag,
                    chunk_size,
                    type
                )
            )

            val sub_chunk_list: MutableList<SubChunkHeader> = mutableListOf()
            var sub_index = 0
            while (sub_index < chunk_size - 4) {
                val subchunkheader = SubChunkHeader(
                    index = sub_index + working_index - 12,
                    tag = this.get_string(working_index + sub_index, 4),
                    size = this.get_little_endian(working_index + sub_index + 4, 4)
                )
                sub_chunk_list.add(subchunkheader)

                sub_index += 8 + subchunkheader.size
            }

            working_index += sub_index
            this.sub_chunks.add(sub_chunk_list)
        }
        if (init_callback != null) {
            init_callback(this)
        }

        this.close_stream()
    }

    fun open_stream() {
        this.input_stream = FileInputStream(this.file_path)
        //this.input_stream?.mark(input_stream?.available()!!)
    }

    fun close_stream() {
        this.input_stream?.close()
        this.input_position = 0
        this.input_stream = null
    }

    fun with(callback: (Riff) -> Unit) {
        this.open_stream()
        try {
            callback(this)
        } finally {
            this.close_stream()
        }
    }

    fun get_chunk_data(header: SubChunkHeader): ByteArray {
        return this.get_bytes(header.index + 8 + 12, header.size)
    }

    fun get_chunk_data(header: ListChunkHeader): ByteArray {
        return this.get_bytes(header.index + 12 + 12, header.size)
    }

    fun get_list_chunk_data(header: ListChunkHeader, inner_offset: Int? = null, cropped_size: Int? = null): ByteArray {
        // First get the offset of the sub chunk
        var offset = header.index + 8

        // get the *actual* size of the sub chunk
        var size = header.size

        if (inner_offset != null) {
            size -= inner_offset
            offset += inner_offset
        }

        if (cropped_size != null && cropped_size <= size) {
            size = cropped_size
        }

        return this.get_bytes(offset, size)
    }


    fun get_sub_chunk_data(header: SubChunkHeader, inner_offset: Int? = null, cropped_size: Int? = null): ByteArray {
        // First get the offset of the sub chunk
        var offset = header.index + 8

        // get the *actual* size of the sub chunk
        var size = header.size
        if (inner_offset != null) {
            size -= inner_offset
            offset += inner_offset
        }

        if (cropped_size != null && cropped_size <= size) {
            size = cropped_size
        }

        return this.get_bytes(offset, size)
    }

    private fun move_to_offset(offset: Long) {
        val stream: InputStream = this.input_stream ?: throw InputStreamClosed()

        if (this.input_position < offset) {
            stream.skip(offset - this.input_position)

        } else if (this.input_position > offset) {
            this.close_stream()
            this.open_stream()
            this.input_stream?.skip(offset)
        }
        this.input_position = offset.toInt()
    }

    private fun get_bytes(offset: Int, size: Int): ByteArray {
        val stream: InputStream = this.input_stream ?: throw InputStreamClosed()
        this.move_to_offset(offset.toLong())
        val output = ByteArray(size)
        this.input_stream?.read(output)
        this.input_position += size
        return output
    }

    private fun get_string(offset: Int, size: Int): String {
        return this.get_bytes(offset, size).toString(Charsets.UTF_8)
    }

    private fun get_little_endian(offset: Int, size: Int): Int {
        var output = 0
        val bytes = this.get_bytes(offset, size)
        for (i in 0 until size) {
            output *= 256
            output += toUInt(bytes[size - 1 - i])
        }
        return output
    }
}