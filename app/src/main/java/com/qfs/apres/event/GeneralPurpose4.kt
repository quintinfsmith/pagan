package com.qfs.apres.event

class GeneralPurpose4(channel: Int, value: Int): CompoundEvent(channel, value, 0x13)
class GeneralPurpose4MSB(channel: Int, value: Int): VariableControlChange(channel, 0x13, value)
class GeneralPurpose4LSB(channel: Int, value: Int): VariableControlChange(channel, 0x33, value)

