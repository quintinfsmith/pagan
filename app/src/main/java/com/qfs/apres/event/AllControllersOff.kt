package com.qfs.apres.event

class AllControllersOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x79
}