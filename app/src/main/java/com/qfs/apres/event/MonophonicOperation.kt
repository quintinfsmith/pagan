package com.qfs.apres.event

class MonophonicOperation(channel: Int, value: Int): VariableControlChange(channel, 0xFE, value)