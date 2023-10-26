package com.qfs.apres.event


class BreathController(channel: Int, value: Int): CompoundEvent(channel, value, 0x02)
class BreathControllerMSB(channel: Int, value: Int): VariableControlChange(channel, 0x02, value)
class BreathControllerLSB(channel: Int, value: Int): VariableControlChange(channel, 0x22, value)
