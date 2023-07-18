package com.qfs.apres.event

import com.qfs.apres.event.VariableControlChange

class OmniOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7C
}