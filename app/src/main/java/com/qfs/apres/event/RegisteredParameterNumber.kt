package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class RegisteredParameterNumber(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x65
    override val controller_lsb = 0x64
}
class RegisteredParameterNumberMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x65
}
class RegisteredParameterNumberLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x64
}
