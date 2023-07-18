package com.qfs.apres.event

class SoundControl3(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4D
}