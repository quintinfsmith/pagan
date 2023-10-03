package com.qfs.apres.event

class NonRegisteredParameterNumber(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x63
    override val controller_lsb = 0x62
}
class NonRegisteredParameterNumberMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x63
}
class NonRegisteredParameterNumberLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x62
}
