package com.qfs.apres.event

class Hold2Pedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x45
}