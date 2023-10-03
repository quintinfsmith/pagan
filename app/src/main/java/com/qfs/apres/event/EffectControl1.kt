package com.qfs.apres.event

class EffectControl1(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x64
}
class EffectControl1MSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0C
}
class EffectControl1LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2C
}
