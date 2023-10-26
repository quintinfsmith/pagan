package com.qfs.apres.event

class Legato(channel: Int, value: Int): VariableControlChange(channel, 0x44, value)