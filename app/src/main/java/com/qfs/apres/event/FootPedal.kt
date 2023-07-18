package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class FootPedal(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x04
}
class FootPedalMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x04
}
class FootPedalLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x24
}
