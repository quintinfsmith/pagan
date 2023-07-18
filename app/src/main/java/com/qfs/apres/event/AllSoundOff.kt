package com.qfs.apres.event

class AllSoundOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x78
}