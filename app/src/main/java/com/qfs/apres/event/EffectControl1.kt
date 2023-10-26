package com.qfs.apres.event

class EffectControl1(channel: Int, value: Int): CompoundEvent(channel, value, 0x0C)
class EffectControl1MSB(channel: Int, value: Int): VariableControlChange(channel, 0x0C, value)
class EffectControl1LSB(channel: Int, value: Int): VariableControlChange(channel, 0x2C, value)
