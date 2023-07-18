package com.qfs.apres.event

class SoundTimbre(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x47
}