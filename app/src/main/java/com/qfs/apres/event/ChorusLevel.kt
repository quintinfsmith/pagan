package com.qfs.apres.event

class ChorusLevel(channel: Int, value: Int): VariableControlChange(channel, 0x5D, value)