package com.qfs.apres.event

class GeneralPurpose6(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x51
}