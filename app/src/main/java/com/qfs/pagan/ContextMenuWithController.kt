package com.qfs.pagan

import com.qfs.pagan.controlwidgets.ControlWidget
import com.qfs.pagan.structure.opusmanager.base.OpusControlEvent

interface ContextMenuWithController<T: OpusControlEvent> {
    abstract fun get_widget(): ControlWidget<T>
}