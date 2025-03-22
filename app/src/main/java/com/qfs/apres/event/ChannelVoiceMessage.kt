package com.qfs.apres.event

open class ChannelVoiceMessage(
    var status: Int,
    var channel: Int,
    var data: Array<Int>): MIDIEvent {

    override fun as_bytes(): ByteArray {
        val output = mutableListOf<Byte>(
            (this.status or this.channel).toByte()
        )
        for (value in this.data) {
            output.add(value.toByte())
        }

        return output.toByteArray()
    }

    override fun as_ump_bytes(): ByteArray {
        val output = ByteArray(1) { 0x20 } + this.as_bytes()
        TODO()
    }

    fun get_channel(): Int {
        return this.channel
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun set_data(index: Int, value: Int) {
        this.data[index] = value
    }

    fun get_data(index: Int): Int {
        return this.data[index]
    }
}
