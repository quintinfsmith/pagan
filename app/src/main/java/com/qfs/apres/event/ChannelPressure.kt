package com.qfs.apres.event

data class ChannelPressure(var channel: Int, var pressure: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xD0 or this.channel).toByte(),
            this.pressure.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun get_pressure(): Int {
        return this.pressure
    }
    fun set_pressure(pressure: Int) {
        this.pressure = pressure
    }
}