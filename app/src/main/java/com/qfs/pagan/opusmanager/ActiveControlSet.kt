package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree


class ActiveControlSet(var beat_count: Int, default_enabled: Set<ControlEventType>? = null) {
    class ActiveController(var type: ControlEventType, beat_count: Int) {
        companion object {
            fun from_json(obj: ActiveControllerJSON, size: Int): ActiveController {
                val new_controller = ActiveController(obj.type, size)
                for ((index, json_tree) in obj.children) {
                    new_controller.events[index] = OpusTree.from_json(json_tree)
                }
                return new_controller
            }
        }

        var events = mutableListOf<OpusTree<OpusControlEvent>?>()

        init {
            for (i in 0 until beat_count) {
                this.insert_beat(i)
            }
        }

        fun get_current_value(beat: Int, position: List<Int>): Float {
            val current_tree = this.get_tree(beat, position)
            if (current_tree.is_event()) {
                return current_tree.get_event()!!.value
            }

            var working_beat = beat
            var working_position = position.toList()
            var output = 0F // TODO: Ctl Type Defaults

            while (true) {
                val pair = this.get_preceding_leaf_position(working_beat, working_position) ?: return output
                working_beat = pair.first
                working_position = pair.second

                val working_tree = this.get_tree(working_beat, working_position)
                if (working_tree.is_event()) {
                    val working_event = working_tree.get_event()!!
                    output = working_event.value
                    break
                }
            }
            return output
        }

        fun get_preceding_leaf_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
            val working_position = position.toMutableList()
            var working_beat = beat

            // Move left/up
            while (true) {
                if (working_position.isNotEmpty()) {
                    if (working_position.last() > 0) {
                        working_position[working_position.size - 1] -= 1
                        break
                    } else {
                        working_position.removeLast()
                    }
                } else if (working_beat > 0) {
                    working_beat -= 1
                    break
                } else {
                    return null
                }
            }

            var working_tree = this.get_tree(working_beat, working_position)

            // Move right/down to leaf
            while (!working_tree.is_leaf()) {
                working_position.add(working_tree.size - 1)
                working_tree = working_tree[working_tree.size - 1]
            }

            return Pair(working_beat, working_position)
        }

        fun beat_count(): Int {
            return this.events.size
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

        fun get_tree(beat: Int, position: List<Int>? = null): OpusTree<OpusControlEvent> {
            var tree = this.get_beat(beat)
            if (position != null) {
                for (i in position) {
                    tree = tree[i]
                }
            }

            return tree
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

        fun set_beat_count(beat_count: Int) {
            val current_beat_count = this.events.size
            if (beat_count > current_beat_count) {
                for (i in current_beat_count until beat_count) {
                    this.insert_beat(current_beat_count)
                }
            } else {
                for (i in beat_count until current_beat_count) {
                    this.remove_beat(current_beat_count - 1)
                }
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

    init {
        for (type in default_enabled ?: setOf()) {
            this.new_controller(type)
        }
    }

    fun clear() {
        this.controllers.clear()
    }

    fun size(): Int {
        return this.controllers.size
    }

    fun get_all(): Array<Pair<ControlEventType, ActiveController>> {
        // TODO: Guarantee some order
        var keys = this.controllers.keys.toList()
        return Array(this.controllers.size) {
            Pair(keys[it], this.controllers[keys[it]]!!)
        }
    }

    fun new_controller(type: ControlEventType, controller: ActiveController? = null) {
        if (controller == null) {
            this.controllers[type] = ActiveController(type, this.beat_count)
        } else {
            this.controllers[type] = controller
            controller.set_beat_count(this.beat_count)
        }
    }

    fun remove_controller(type: ControlEventType) {
        this.controllers.remove(type)
    }

    fun insert_beat(n: Int) {
        this.beat_count += 1
        for (controller in this.controllers.values) {
            controller.insert_beat(n)
        }
    }

    fun remove_beat(n: Int) {
        this.beat_count -= 1
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
