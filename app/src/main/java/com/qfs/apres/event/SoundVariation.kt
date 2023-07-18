package com.qfs.apres.event

class SoundVariation(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x46
}