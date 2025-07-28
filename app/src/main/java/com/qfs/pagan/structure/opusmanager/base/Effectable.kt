package com.qfs.pagan.structure.opusmanager.base

import com.qfs.pagan.structure.opusmanager.base.activecontroller.EffectController

interface Effectable {
    fun <T: OpusControlEvent> get_controller(type: ControlEventType): EffectController<T>
}