package com.qfs.apres.event

class DataDecrement(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x61
}