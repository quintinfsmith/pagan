package com.qfs.apres.event

class BankSelect(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x00
}