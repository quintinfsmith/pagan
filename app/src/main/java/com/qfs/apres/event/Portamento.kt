package com.qfs.apres.event

class Portamento(channel: Int, value: Int): VariableControlChange(channel, 0x41, value)