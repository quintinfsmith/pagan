package com.qfs.apres.event

class Legato(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x44
}