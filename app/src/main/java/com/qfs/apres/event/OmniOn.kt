package com.qfs.apres.event

class OmniOn(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7D
}