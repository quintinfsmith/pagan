package com.qfs.apres.event

abstract class VariableControlChange(var channel: Int, var value: Int): MIDIEvent {
    abstract val controller: Int
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xB0 or this.get_channel()).toByte(),
            this.controller.toByte(),
            this.get_value().toByte()
        )
    }
    fun get_channel(): Int {
        return this.channel
    }
    fun get_value(): Int {
        return this.value
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
    fun set_value(value: Int) {
        this.value = value
    }
}