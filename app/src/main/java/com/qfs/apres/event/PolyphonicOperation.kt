package com.qfs.apres.event

class PolyphonicOperation(channel: Int): VariableControlChange(channel, 0xFF, 0)