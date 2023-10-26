package com.qfs.apres.event

class SoundAttack(channel: Int, value: Int): VariableControlChange(channel, 0x49, value)