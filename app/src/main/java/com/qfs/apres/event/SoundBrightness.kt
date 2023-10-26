package com.qfs.apres.event

class SoundBrightness(channel: Int, value: Int): VariableControlChange(channel, 0x4A, value)