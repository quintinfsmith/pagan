package com.qfs.apres.event

data class SystemExclusive(var data: ByteArray): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF0.toByte()) + this.data + byteArrayOf(0xF7.toByte())
    }

    fun get_data(): ByteArray {
        return this.data
    }

    fun set_data(new_data: ByteArray) {
        this.data = new_data
    }
}