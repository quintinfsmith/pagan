package com.qfs.apres.event

import com.qfs.apres.event.CompoundEvent

class Volume(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x07
}
class VolumeMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x07
}
class VolumeLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x27
}
