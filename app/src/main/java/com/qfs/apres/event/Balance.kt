package com.qfs.apres.event

class Balance(channel: Int, value: Int): CompoundEvent(channel, value, 0x08)
class BalanceMSB(channel: Int, value: Int): VariableControlChange(channel, 0x08, value)
class BalanceLSB(channel: Int, value: Int): VariableControlChange(channel, 0x28, value)