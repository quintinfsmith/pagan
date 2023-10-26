package com.qfs.apres.event

class Expression(channel: Int, value: Int): CompoundEvent(channel, value, 0x0B)
class ExpressionMSB(channel: Int, value: Int): VariableControlChange(channel, 0x0B, value)
class ExpressionLSB(channel: Int, value: Int): VariableControlChange(channel, 0x2B, value)
