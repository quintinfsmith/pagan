package com.qfs.apres.event

class NonRegisteredParameterNumber(channel: Int, value: Int): CompoundEvent(channel, value, 0x63, 0x62)
class NonRegisteredParameterNumberMSB(channel: Int, value: Int): VariableControlChange(channel, 0x63, value)
class NonRegisteredParameterNumberLSB(channel: Int, value: Int): VariableControlChange(channel, 0x62, value)
