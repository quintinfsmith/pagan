package com.qfs.apres.event

class CelesteLevel(channel: Int, value: Int): VariableControlChange(channel, 0x5E, value)