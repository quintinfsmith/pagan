package com.qfs.apres.event

class SoftPedal(channel: Int, value: Int): VariableControlChange(channel, 0x43, value)