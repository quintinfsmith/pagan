package com.qfs.apres.event

class Portamento(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x41
}