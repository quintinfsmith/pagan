package com.qfs.apres.event

class SoundReleaseTime(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x48
}