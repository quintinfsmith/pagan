package com.qfs.apres.event

import com.qfs.apres.to_variable_length_bytes

data class InstrumentName(var name: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val name_bytes = this.name.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x04.toByte()) + to_variable_length_bytes(name_bytes.size) + name_bytes
    }

    fun get_name(): String {
        return this.name
    }

    fun set_name(new_name: String) {
        this.name = new_name
    }
}