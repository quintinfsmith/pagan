package com.qfs.apres.event

class GeneralPurpose5(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x50
}