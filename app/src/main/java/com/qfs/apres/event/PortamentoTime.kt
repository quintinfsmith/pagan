package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class PortamentoTime(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x05
}
class PortamentoTimeMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x05
}
class PortamentoTimeLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x25
}
