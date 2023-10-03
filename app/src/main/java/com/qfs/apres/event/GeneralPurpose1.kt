package com.qfs.apres.event

class GeneralPurpose1(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x10
}
class GeneralPurpose1MSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x10
}
class GeneralPurpose1LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x30
}
