package com.qfs.apres.event

class SoundVariation(channel: Int, value: Int): VariableControlChange(channel, 0x46, value)