package com.qfs.apres.event

class Volume(channel: Int, value: Int): CompoundEvent(channel, value, 0x07)
class VolumeMSB(channel: Int, value: Int): VariableControlChange(channel, 0x07, value)
class VolumeLSB(channel: Int, value: Int): VariableControlChange(channel, 0x27, value)
