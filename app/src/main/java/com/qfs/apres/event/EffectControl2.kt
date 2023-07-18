package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class EffectControl2(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x0D
}
class EffectControl2MSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0D
}
class EffectControl2LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2D
}
