package com.qfs.apres.event


class GeneralPurpose2(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x11
}
class GeneralPurpose2MSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x11
}
class GeneralPurpose2LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x31
}
