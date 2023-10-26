package com.qfs.apres.event

class FootPedal(channel: Int, value: Int): CompoundEvent(channel, value, 0x04)
class FootPedalMSB(channel: Int, value: Int): VariableControlChange(channel, 0x04, value)
class FootPedalLSB(channel: Int, value: Int): VariableControlChange(channel, 0x24, value)
