package com.qfs.apres.event

import com.qfs.apres.event.VariableControlChange

class OmniOn(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7D
}