package com.qfs.apres.event

class ChorusLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5D
}