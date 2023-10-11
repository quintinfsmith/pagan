package com.qfs.apres.event

data class MTCQuarterFrame(var time_code: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF1.toByte(), this.time_code.toByte())
    }

    fun set_time_code(new_value: Int) {
        this.time_code = new_value
    }
    fun get_time_code(): Int {
        return this.time_code
    }
}