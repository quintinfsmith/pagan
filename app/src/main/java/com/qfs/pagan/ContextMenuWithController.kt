package com.qfs.pagan

import com.qfs.pagan.ControlWidget.ControlWidget
import com.qfs.pagan.opusmanager.OpusControlEvent

interface ContextMenuWithController<T: OpusControlEvent> {
    abstract fun get_widget(): ControlWidget<T>
}