package com.qfs.radixulous.apres.riffreader

import android.content.res.AssetManager
import java.io.InputStream
import kotlin.experimental.and

class Riff(var assets: AssetManager, var file_name: String, init_callback: ((riff: Riff) -> Unit)? = null) {
    class InputStreamClosed(): Exception("Input Stream is Closed")
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
    var input_stream: InputStream? = null
    var input_position: Int = 0

    init {
        this.open_stream()

        var fourcc = this.get_string(0, 4)
        var riff_size = this.get_little_endian(4, 4)
        var typecc = this.get_string(8, 4)

        var working_index = 12
        while (working_index < riff_size - 4) {
            var header_index = working_index - 12
            var tag = this.get_string(working_index, 4)
            working_index += 4

            var chunk_size = this.get_little_endian(working_index, 4)
            working_index += 4

            var type = this.get_string(working_index, 4)
            working_index += 4

            this.list_chunks.add(
                ListChunkHeader(
                    header_index,
                    tag,
                    chunk_size,
                    type
                )
            )

            var sub_chunk_list: MutableList<SubChunkHeader> = mutableListOf()
            var sub_index = 0
            while (sub_index < chunk_size - 4) {
                var subchunkheader = SubChunkHeader(
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
        this.input_stream = this.assets.open(this.file_name)
        this.input_stream?.mark(input_stream?.available()!!)
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

    fun move_to_offset(offset: Long) {
        var stream: InputStream? = this.input_stream ?: throw InputStreamClosed()

        if (this.input_position < offset) {
            var start = System.currentTimeMillis()
            stream?.skip(offset - this.input_position)
        } else if (this.input_position > offset) {
            var start = System.currentTimeMillis()
            stream?.reset()
            this.input_position = 0
            stream?.skip(offset)
        }
        this.input_position = offset.toInt()
    }

    fun get_bytes(offset: Int, size: Int): ByteArray {
        var stream: InputStream? = this.input_stream ?: throw InputStreamClosed()
        this.move_to_offset(offset.toLong())
        var output = ByteArray(size)
        stream?.read(output)
        this.input_position += size
        return output

    }

    fun get_string(offset: Int, size: Int): String {
        return this.get_bytes(offset, size).toString(Charsets.UTF_8)
    }

    fun get_little_endian(offset: Int, size: Int): Int {
        var output = 0
        var bytes = this.get_bytes(offset, size)
        for (i in 0 until size) {
            output *= 256
            output += toUInt(bytes[size - 1 - i])
        }
        return output
    }
}

fun toUInt(byte: Byte): Int {
    var new_int = (byte and 0x7F.toByte()).toInt()
    if (byte.toInt() < 0) {
        new_int += 128
    }
    return new_int
}

fun toUInt(number: Short): Int {
    var new_int = (number and 0x7FFF.toShort()).toInt()
    if (number.toInt() < 0) {
        new_int += 0x8000
    }
    return new_int
}
