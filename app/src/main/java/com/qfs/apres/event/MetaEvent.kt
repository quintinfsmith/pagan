package com.qfs.apres.event

data class MetaEvent(var byte: Byte, var bytes: ByteArray): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val output = mutableListOf(0xFF.toByte(), this.byte)
        for (b in this.bytes) {
            output.add(b)
        }
        return output.toByteArray()
    }
}