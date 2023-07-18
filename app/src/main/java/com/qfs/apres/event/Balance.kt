package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class Balance(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x08
}
class BalanceMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x08
}
class BalanceLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x28
}
