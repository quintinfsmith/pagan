package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

abstract class OpusLineAbstract<T: InstrumentEvent>(beats: MutableList<OpusTree<T>>): OpusTreeArray<T>(beats) {
    class BlockedCtlTreeException(var type: ControlEventType, var e: BlockedTreeException): Exception(e.message)
    var controllers = ActiveControlSet(this.beats.size, setOf(ControlEventType.Volume))

    init {
        // Default volume to hidden
        for ((_, controller) in controllers.get_all()) {
            controller.visible = false
        }
    }

    override fun insert_beat(index: Int) {
        super.insert_beat(index)
        this.controllers.insert_beat(index)
    }

    override fun set_beat_count(new_beat_count: Int) {
        super.set_beat_count(new_beat_count)
        this.controllers.set_beat_count(new_beat_count)
    }

    override fun remove_beat(index: Int, count: Int) {
        super.remove_beat(index, count)
        for ((type, controller) in this.controllers.get_all()) {
            try {
                controller.remove_beat(index, count)
            } catch (e: OpusTreeArray.BlockedTreeException) {
                throw BlockedCtlTreeException(type, e)
            }
        }
    }

    fun remove_control_leaf(type: ControlEventType, beat: Int, position: List<Int>) {
        try {
            this.get_controller<OpusControlEvent>(type).remove_node(beat, position)
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun insert_control_leaf(type: ControlEventType, beat: Int, position: List<Int>) {
        try {
            this.get_controller<OpusControlEvent>(type).insert(beat, position)
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun insert_control_leaf_after(type: ControlEventType, beat: Int, position: List<Int>) {
        try {
            this.get_controller<OpusControlEvent>(type).insert_after(beat, position)
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun <T: OpusControlEvent> replace_control_leaf(type: ControlEventType, beat: Int, position: List<Int>, tree: OpusTree<T>) {
        try {
            this.get_controller<T>(type).replace_tree(beat, position, tree)
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun <T: OpusControlEvent> get_controller(type: ControlEventType): ActiveController<T> {
        return this.controllers.get_controller(type)
    }

    fun <T: OpusControlEvent> set_controller_event(type: ControlEventType, beat: Int, position: List<Int>, event: T) {
        try {
            this.get_controller<T>(type).set_event(beat, position, event)
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

}

