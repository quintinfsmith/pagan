package com.qfs.apres.event

class EffectControl2(channel: Int, value: Int): CompoundEvent(channel, value, 0x0D)
class EffectControl2MSB(channel: Int, value: Int): VariableControlChange(channel, 0x0D, value)
class EffectControl2LSB(channel: Int, value: Int): VariableControlChange(channel, 0x2D, value)
