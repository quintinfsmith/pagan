package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class GeneralPurpose3(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x12
}
class GeneralPurpose3MSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x12
}
class GeneralPurpose3LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x32
}
