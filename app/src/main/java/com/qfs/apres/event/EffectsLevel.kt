package com.qfs.apres.event

class EffectsLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5B
}