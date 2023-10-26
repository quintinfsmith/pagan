package com.qfs.apres.event

class GeneralPurpose3(channel: Int, value: Int): CompoundEvent(channel, value, 0x12)
class GeneralPurpose3MSB(channel: Int, value: Int): VariableControlChange(channel, 0x12, value)
class GeneralPurpose3LSB(channel: Int, value: Int): VariableControlChange(channel, 0x32, value)
