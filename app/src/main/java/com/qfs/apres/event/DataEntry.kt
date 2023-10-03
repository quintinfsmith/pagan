package com.qfs.apres.event

class DataEntry(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x06
}
class DataEntryMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x06
}
class DataEntryLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x26
}
