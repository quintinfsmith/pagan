package com.qfs.apres.event

class SoundControl5(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4F
}