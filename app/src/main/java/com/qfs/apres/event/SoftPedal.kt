package com.qfs.apres.event

class SoftPedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x43
}