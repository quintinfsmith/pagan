package com.qfs.apres.event

class LocalControl(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x7A
}