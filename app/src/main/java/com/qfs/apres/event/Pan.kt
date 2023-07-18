package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class Pan(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x0A
}
class PanMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0A
}
class PanLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2A
}
