package com.qfs.apres.event

data class SequenceNumber(var sequence: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x00.toByte(),
            0x02.toByte(),
            ((this.sequence shr 8) and 0xFF).toByte(),
            (this.sequence and 0xFF).toByte()
        )
    }

    fun get_sequence(): Int {
        return this.sequence
    }

    fun set_sequence(new_sequence: Int) {
        this.sequence = new_sequence
    }
}