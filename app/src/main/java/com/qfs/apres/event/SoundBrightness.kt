package com.qfs.apres.event

class SoundBrightness(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4A
}