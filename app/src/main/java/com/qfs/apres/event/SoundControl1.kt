package com.qfs.apres.event

class SoundControl1(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4B
}