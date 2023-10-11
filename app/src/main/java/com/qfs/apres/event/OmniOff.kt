package com.qfs.apres.event

class OmniOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7C
}