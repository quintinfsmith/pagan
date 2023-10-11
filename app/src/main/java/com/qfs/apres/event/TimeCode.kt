package com.qfs.apres.event

data class TimeCode(var rate: Int, var hour: Int, var minute: Int, var second: Int, var frame: Int):
    MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            ((this.rate shl 5) + this.hour).toByte(),
            (this.minute and 0x3F).toByte(),
            (this.second and 0x3F).toByte(),
            (this.frame and 0x1F).toByte()
        )
    }
}