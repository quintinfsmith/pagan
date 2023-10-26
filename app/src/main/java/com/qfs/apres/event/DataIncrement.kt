package com.qfs.apres.event

class DataIncrement(channel: Int): VariableControlChange(channel, 0x60, 0)