package com.qfs.apres.event


class GeneralPurpose2(channel: Int, value: Int): CompoundEvent(channel, value, 0x11)
class GeneralPurpose2MSB(channel: Int, value: Int): VariableControlChange(channel, 0x11, value)
class GeneralPurpose2LSB(channel: Int, value: Int): VariableControlChange(channel, 0x31, value)
