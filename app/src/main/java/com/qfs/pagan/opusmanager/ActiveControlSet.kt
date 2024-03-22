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

        fun get_beat(beat: Int): OpusTree<OpusControlEvent> {
            if (this.events[beat] == null) {
                this.events[beat] = OpusTree()
            }

            return this.events[beat]!!
        }

        fun replace_tree(beat: Int, position: List<Int>, new_tree: OpusTree<OpusControlEvent>) {
            if (position.isEmpty()) {
                this.events[beat] = new_tree
            } else {
                var tree = this.get_beat(beat)   
                for (i in position) {
                    tree = tree[i]
                }
                tree.replace_with(new_tree)
            }
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

    fun new_controller(type: ControlEventType, controller: ActiveController? = null) {
        if (controller == null) {
            this.controllers[type] = ActiveController(type, this.size)
        } else {
            this.controllers[type] = controller
        }
    }

    fun insert_beat(n: Int) {
        for (controller in this.controllers.values) {
            controller.insert_beat(n)
        }
    }

    fun remove_beat(n: Int) {
        for (controller in this.controllers.values) {
            controller.remove_beat(n)
        }
    }

    fun to_json(): List<ActiveControllerJSON> {
        var output = mutableListOf<ActiveControllerJSON>()
        for (controller in this.controllers.values) {
            output.add(controller.to_json())
        }
        return output
    }

    fun get_controller(type: ControlEventType): ActiveController {
        if (!this.controllers.containsKey(type)) {
            this.new_controller(type)
        }

        return this.controllers[type]!!
    }

}
