package com.qfs.apres.event

class PhaserLevel(channel: Int, value: Int): VariableControlChange(channel, 0x5F, value)