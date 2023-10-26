package com.qfs.apres.event

class Hold2Pedal(channel: Int, value: Int): VariableControlChange(channel, 0x45, value)