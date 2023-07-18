package com.qfs.apres.event

class MonophonicOperation(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0xFE
}