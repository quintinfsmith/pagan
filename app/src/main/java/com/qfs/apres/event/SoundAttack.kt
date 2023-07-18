package com.qfs.apres.event

class SoundAttack(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x49
}