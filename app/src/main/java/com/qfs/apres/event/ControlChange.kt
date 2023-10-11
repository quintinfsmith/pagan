package com.qfs.apres.event

open class ControlChange(var channel: Int, var controller: Int, open var value: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xB0 or this.get_channel()).toByte(),
            this.get_controller().toByte(),
            this.get_value().toByte()
        )
    }
    fun get_controller(): Int {
        return this.controller
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
    fun set_controller(controller: Int) {
        this.controller = controller
    }
    fun set_value(value: Int) {
        this.value = value
    }
}