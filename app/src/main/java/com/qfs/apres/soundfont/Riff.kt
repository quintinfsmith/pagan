package com.qfs.apres.soundfont

import com.qfs.apres.toUInt
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileInputStream
import java.io.InputStream

class Riff(private var file_path: String) {
    class InvalidRiff(file_path: String): Exception("$file_path is not a valid Riff")
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
    lateinit var type_cc: String

    var list_chunks: MutableList<ListChunkHeader> = mutableListOf()
    var sub_chunks: MutableList<List<SubChunkHeader>> = mutableListOf()
    private var _input_stream: InputStream? = null
    private var _input_position: Int = 0
    private var _read_mutex = Mutex()

    init {
        this.with {
            val header_check = this.get_string(0, 4) // fourcc
            if (header_check != "RIFF") {
                this.close_stream()
                throw InvalidRiff(file_path)
            }
            val riff_size = this.get_little_endian(4, 4)
            this.type_cc = this.get_string(8, 4)

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
        }
    }

    private fun open_stream() {
        this._input_stream = FileInputStream(this.file_path)
    }

    private fun close_stream() {
        this._input_stream?.close()
        this._input_position = 0
        this._input_stream = null
    }

    fun with(callback: (Riff) -> Unit) {
        runBlocking {
            this@Riff._read_mutex.withLock {
                this@Riff.open_stream()
                try {
                    callback(this@Riff)
                } finally {
                    this@Riff.close_stream()
                }
            }
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
        val stream: InputStream = this._input_stream ?: throw InputStreamClosed()

        if (this._input_position < offset) {
            stream.skip(offset - this._input_position)

        } else if (this._input_position > offset) {
            this.close_stream()
            this.open_stream()
            this._input_stream?.skip(offset)
        }
        this._input_position = offset.toInt()
    }

    private fun get_bytes(offset: Int, size: Int): ByteArray {
        this.move_to_offset(offset.toLong())
        val output = ByteArray(size)
        this._input_stream?.read(output)
        this._input_position += size
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