package com.qfs.apres.event

class HoldPedal(channel: Int, value: Int): VariableControlChange(channel, 0x40, value)