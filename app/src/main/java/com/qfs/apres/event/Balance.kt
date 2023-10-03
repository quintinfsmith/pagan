package com.qfs.apres.event

class Balance(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x08
}
class BalanceMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x08
}
class BalanceLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x28
}
