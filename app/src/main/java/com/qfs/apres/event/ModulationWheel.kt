package com.qfs.apres.event

class ModulationWheel(channel: Int, value: Int): CompoundEvent(channel, value) {
    override val controller = 0x01
}

class ModulationWheelMSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x01
}

class ModulationWheelLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x21
}
