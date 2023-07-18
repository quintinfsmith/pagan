package com.qfs.apres.event

data class ChannelPrefix(var channel: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x20.toByte(),
            0x01.toByte(),
            this.channel.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
}