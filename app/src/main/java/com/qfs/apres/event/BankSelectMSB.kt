package com.qfs.apres.event

class BankSelectMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x00
}