package com.qfs.apres.event

class GeneralPurpose1(channel: Int, value: Int): CompoundEvent(channel, value, 0x10)
class GeneralPurpose1MSB(channel: Int, value: Int): VariableControlChange(channel, 0x10, value)
class GeneralPurpose1LSB(channel: Int, value: Int): VariableControlChange(channel, 0x30, value)
