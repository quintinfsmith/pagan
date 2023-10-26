package com.qfs.apres.event

class ModulationWheel(channel: Int, value: Int): CompoundEvent(channel, value, 0x01)
class ModulationWheelMSB(channel: Int, value: Int): VariableControlChange(channel, 0x01, value)
class ModulationWheelLSB(channel: Int, value: Int): VariableControlChange(channel, 0x21, value)
