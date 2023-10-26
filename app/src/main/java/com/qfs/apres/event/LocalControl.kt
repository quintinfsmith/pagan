package com.qfs.apres.event

class LocalControl(channel: Int, value: Int): VariableControlChange(channel, 0x7A, value)