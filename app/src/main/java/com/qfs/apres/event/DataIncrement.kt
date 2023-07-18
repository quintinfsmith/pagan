package com.qfs.apres.event

class DataIncrement(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x60
}