package com.qfs.radixulous.apres.riffreader

import java.io.InputStream
import kotlin.experimental.and

class Riff(input_stream: InputStream) {
    var list_chunks: MutableList<Int> = mutableListOf()
    var sub_chunks: MutableList<List<Int>> = mutableListOf()
    var bytes: ByteArray

    init {
        this.bytes = ByteArray(input_stream.available())
        input_stream.read(this.bytes)
        input_stream.close()

        var fourcc = this.get_string(0, 4)
        var riff_size = this.get_little_endian(4, 4)
        var typecc = this.get_string(8, 4)

        var working_index = 12
        while (working_index < riff_size - 4) {
            this.list_chunks.add(working_index)
            var tag = this.get_string(working_index, 4)
            working_index += 4

            var chunk_size = this.get_little_endian(working_index, 4)
            working_index += 4

            var type = this.get_string(working_index, 4)
            working_index += 4

            var sub_chunk_list: MutableList<Int> = mutableListOf()
            var sub_index = 0
            while (sub_index < chunk_size - 4) {
                sub_chunk_list.add(working_index + sub_index)

                var sub_chunk_tag = this.get_string(working_index + sub_index, 4)
                sub_index += 4

                var sub_chunk_size = this.get_little_endian(working_index + sub_index, 4)
                sub_index += 4

                sub_index += sub_chunk_size
            }

            working_index += sub_index
            this.sub_chunks.add(sub_chunk_list)
        }
    }

    fun get_list_chunk_type(list_index: Int): String {
        var offset = this.list_chunks[list_index]
        return this.get_string(offset + 8, 4)
    }

    fun get_sub_chunk_type(list_index: Int, chunk_index: Int): String {
        var start_time = System.currentTimeMillis()
        var offset = this.sub_chunks[list_index][chunk_index]
        return this.get_string(offset, 4)
    }

    fun get_sub_chunk_size(list_index: Int, chunk_index: Int): Int {
        var offset = this.sub_chunks[list_index][chunk_index]
        return this.get_little_endian(offset + 4, 4)
    }

    fun get_sub_chunk_data(list_index: Int, chunk_index: Int, inner_offset: Int? = null, cropped_size: Int? = null): ByteArray {
        // First get the offset of the sub chunk
        var offset = this.sub_chunks[list_index][chunk_index]

        // get the *actual* size of the sub chunk
        var size = this.get_little_endian(offset + 4, 4)
        offset += 8

        if (inner_offset != null) {
            size -= inner_offset
            offset += inner_offset
        }

        if (cropped_size != null && cropped_size <= size) {
            size = cropped_size
        }
        return this.get_bytes(offset, size)
    }

    fun get_bytes(offset: Int, size: Int): ByteArray {
        return this.bytes.sliceArray(offset until offset + size)
    }

    fun get_string(offset: Int, size: Int): String {
        return this.get_bytes(offset, size).toString(Charsets.UTF_8)
    }

    fun get_little_endian(offset: Int, size: Int): Int {
        var output = 0
        for (i in 0 until size) {
            output *= 256
            output += toUInt(this.bytes[offset + (size - 1 - i)])
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
