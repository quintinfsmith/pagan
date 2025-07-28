package com.qfs.pagan.structure.opusmanager.base.effectcontrol

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent

interface Effectable {
    fun <T: EffectEvent> get_controller(type: EffectType): EffectController<T>
}