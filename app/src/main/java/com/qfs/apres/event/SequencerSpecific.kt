package com.qfs.apres.event

import com.qfs.apres.to_variable_length_bytes

data class SequencerSpecific(var data: ByteArray): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0x7F.toByte()) + to_variable_length_bytes(this.data.size).toByteArray() + this.data
    }
    fun get_data(): ByteArray {
        return this.data
    }
    fun set_data(new_data: ByteArray) {
        this.data = new_data
    }
}