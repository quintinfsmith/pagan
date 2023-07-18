package com.qfs.apres.event

class PhaserLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5F
}