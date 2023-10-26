package com.qfs.apres.event

class ChannelPressure(channel: Int, pressure: Int): ChannelVoiceMessage(0xD0, channel, arrayOf<Int>(pressure)) {
    fun get_pressure(): Int {
        return this.get_data(0)
    }
    fun set_pressure(pressure: Int) {
        this.set_data(0, pressure)
    }
}