package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree


class ActiveControlSet(var size: Int) {
    class ActiveController(var type: ControlEventType, var size: Int) {
        companion object {
            fun from_json(obj: ActiveControllerJSON, size: Int): ActiveController {
                var new_controller = ActiveController(obj.type, size)
                for ((index, json_tree) in obj.children) {
                    new_controller.events[index] = OpusTree.from_json(json_tree)
                }
                return new_controller
            }
        }
        var events = mutableListOf<OpusTree<OpusControlEvent>?>()

        init {
            for (i in 0 until this.size) {
                this.insert_beat(i)
            }
        }

        fun insert_beat(n: Int) {
            this.events.add(n, null)
        }

        fun remove_beat(n: Int) {
            this.events.removeAt(n)
        }

        fun to_json(): ActiveControllerJSON {
            val children = mutableListOf<Pair<Int, OpusTreeJSON<OpusControlEvent>>>()

            this.events.forEachIndexed { i: Int, event: OpusTree<OpusControlEvent>? ->
                if (event == null) {
                    return@forEachIndexed
                }
                children.add(
                    Pair(i, event.to_json() ?: return@forEachIndexed)
                )
            }
            return ActiveControllerJSON(
                this.type,
                children
            )
        }
    }
    companion object {
        fun from_json(json_obj: List<ActiveControllerJSON>, size: Int): ActiveControlSet {
            val control_set = ActiveControlSet(size)
            for (json_controller in json_obj) {
                control_set.new_controller(
                    json_controller.type,
                    ActiveController.from_json(json_controller, size)
                )
            }
            return control_set
        }
    }

    val controllers = HashMap<ControlEventType, ActiveController>()

    fun new_controller(type: ControlEventType, controller: ActiveController?) {
        if (controller == null) {
            this.controllers[type] = ActiveController(type, this.size)
        } else {
            this.controllers[type] = controller
        }
    }

    fun insert_beat(n: Int) {
        for ((type, controller) in this.controllers) {
            controller.insert_beat(n)
        }
    }

    fun remove_beat(n: Int) {
        for ((type, controller) in this.controllers) {
            controller.remove_beat(n)
        }
    }

    fun to_json(): List<ActiveControllerJSON> {
        var output = mutableListOf<ActiveControllerJSON>()
        for ((type, controller) in this.controllers) {
            output.add(controller.to_json())
        }
        return output
    }

}