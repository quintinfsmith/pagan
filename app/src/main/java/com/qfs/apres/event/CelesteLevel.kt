package com.qfs.apres.event

class CelesteLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5E
}