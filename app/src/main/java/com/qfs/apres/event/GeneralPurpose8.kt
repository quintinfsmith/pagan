package com.qfs.apres.event

class GeneralPurpose8(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x53
}