package com.qfs.apres.event

abstract class VariableControlChange(channel: Int, controller: Int, value: Int): ChannelVoiceMessage(0xB0, channel, arrayOf<Int>(controller, value)) {
    fun get_value(): Int {
        return this.get_data(1)
    }
    fun set_value(value: Int) {
        this.set_data(1, value)
    }
}