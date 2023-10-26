package com.qfs.apres.event

class PortamentoTime(channel: Int, value: Int): CompoundEvent(channel, value, 0x05)
class PortamentoTimeMSB(channel: Int, value: Int): VariableControlChange(channel, 0x05, value)
class PortamentoTimeLSB(channel: Int, value: Int): VariableControlChange(channel, 0x25, value)
