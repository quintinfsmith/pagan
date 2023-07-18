package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent
class GeneralPurpose4(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x13
}
class GeneralPurpose4MSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x13
}
class GeneralPurpose4LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x33
}

