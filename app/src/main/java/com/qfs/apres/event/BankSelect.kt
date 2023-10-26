package com.qfs.apres.event

class BankSelect(channel: Int, value: Int): CompoundEvent(channel, value, 0x00)
class BankSelectLSB(channel: Int, value: Int): VariableControlChange(channel, 0x20, value)
class BankSelectMSB(channel: Int, value: Int): VariableControlChange(channel, 0x00, value)
