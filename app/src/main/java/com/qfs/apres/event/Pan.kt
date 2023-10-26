package com.qfs.apres.event

class Pan(channel: Int, value: Int): CompoundEvent(channel, value, 0x0A)
class PanMSB(channel: Int, value: Int): VariableControlChange(channel, 0x0A, value)
class PanLSB(channel: Int, value: Int): VariableControlChange(channel, 0x2A, value)
