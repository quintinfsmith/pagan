package com.qfs.apres.event

class HoldPedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x40
}