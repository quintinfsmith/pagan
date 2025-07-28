package com.qfs.pagan

import com.qfs.pagan.controlwidgets.ControlWidget
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent

interface ContextMenuWithController<T: EffectEvent> {
    abstract fun get_widget(): ControlWidget<T>
}