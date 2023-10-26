package com.qfs.apres.event

class EffectsLevel(channel: Int, value: Int): VariableControlChange(channel, 0x5B, value)