package com.qfs.apres.event

class RegisteredParameterNumber(channel: Int, value: Int): CompoundEvent(channel, value, 0x65, 0x64)
class RegisteredParameterNumberMSB(channel: Int, value: Int): VariableControlChange(channel, 0x65, value)
class RegisteredParameterNumberLSB(channel: Int, value: Int): VariableControlChange(channel, 0x64, value)
