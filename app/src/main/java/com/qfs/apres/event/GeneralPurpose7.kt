package com.qfs.apres.event

class GeneralPurpose7(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x52
}