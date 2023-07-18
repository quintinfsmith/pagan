package com.qfs.apres.event

import com.qfs.apres.event.VariableControlChange

class PolyphonicOperation(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0xFF
}