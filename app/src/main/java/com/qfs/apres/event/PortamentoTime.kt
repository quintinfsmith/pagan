package com.qfs.apres.event

class PortamentoTime(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x05
}
class PortamentoTimeMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x05
}
class PortamentoTimeLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x25
}
