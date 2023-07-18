package com.qfs.apres.event

class TremuloLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5C
}