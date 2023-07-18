package com.qfs.apres.event

class SoundControl2(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4C
}