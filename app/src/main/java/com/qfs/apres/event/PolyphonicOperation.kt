package com.qfs.apres.event

class PolyphonicOperation(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0xFF
}