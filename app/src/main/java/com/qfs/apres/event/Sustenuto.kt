package com.qfs.apres.event

class Sustenuto(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x42
}