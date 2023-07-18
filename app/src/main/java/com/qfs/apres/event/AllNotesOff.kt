package com.qfs.apres.event

class AllNotesOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7B
}