package com.qfs.apres.event

class SoundReleaseTime(channel: Int, value: Int): VariableControlChange(channel, 0x48, value)