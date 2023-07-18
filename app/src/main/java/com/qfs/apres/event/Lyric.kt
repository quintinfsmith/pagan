package com.qfs.apres.event

import com.qfs.apres.to_variable_length_bytes

data class Lyric(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x05.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}