package com.qfs.apres.event

class Sustenuto(channel: Int, value: Int): VariableControlChange(channel, 0x42, value)