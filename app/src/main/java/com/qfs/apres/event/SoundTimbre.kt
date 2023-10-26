package com.qfs.apres.event

class SoundTimbre(channel: Int, value: Int): VariableControlChange(channel, 0x47, value)