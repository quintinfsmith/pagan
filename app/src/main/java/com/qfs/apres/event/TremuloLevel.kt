package com.qfs.apres.event

class TremuloLevel(channel: Int, value: Int): VariableControlChange(channel, 0x5C,value)