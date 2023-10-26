package com.qfs.apres.event

class DataEntry(channel: Int, value: Int): CompoundEvent(channel, value, 0x06)
class DataEntryMSB(channel: Int, value: Int): VariableControlChange(channel, 0x06, value)
class DataEntryLSB(channel: Int, value: Int): VariableControlChange(channel, 0x26, value)
