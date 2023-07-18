package com.qfs.apres.event

class BankSelectLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x20
}