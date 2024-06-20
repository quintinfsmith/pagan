package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import com.qfs.json.*
import com.qfs.pagan.opusmanager.OpusTreeJsonParser

class ControllerParser() {
    companion object {
        fun from_json(obj: ParsedHashMap): ActiveController {
            val size = (obj.hash_map["size"] as ParsedInt).value
            val new_controller: ActiveController = when ((obj.hash_map["type"] as ParsedString).value) {
                "tempo" -> TempoController(size)
                "volume" -> VolumeController(size)
                else -> throw UnknownController(obj.hash_map["type"].value)
            }
            new_controller.set_initial_event(obj.hash_map["initial"])

            for (pair in obj.hash_map["events"]) {
                val index = (pair.list[0] as ParsedInt).value
                new_controller.events[index] = OpusTreeJsonParser.from_json(pair.list[1] as ParsedHashMap)
            }
            return new_controller
        }

        fun to_json(controller: ActiveController): ParsedHashMap {
            val map = HashMap<String, ParsedObject?>()
            
            map["events"] = ParsedList()
            this.events.forEachIndexed { i: Int, event: OpusTree<OpusControlEvent>? ->
                if (event == null) {
                    return@forEachIndexed
                }
                map["events"].list.add(
                    ParsedList(
                        listOf(
                            ParsedInt(i),
                            event.to_json()
                        )
                    )
                )
            }
            map["initial"] = this.initial_event.to_json()
            map["type"] = when (controller) {
                is TempoController -> "tempo"
                is VolumeController -> "volume"
            }

            return map
        }

    }
}

abstract class ActiveController(beat_count: Int) {
    companion object {
        fun default_event(type: ControlEventType): OpusControlEvent {
            return when (type) {
                ControlEventType.Tempo -> OpusTempoEvent(120F)
                ControlEventType.Volume -> OpusVolumeEvent(64)
                ControlEventType.Reverb -> OpusReverbEvent(0F)
            }
        }
    }

    abstract val default_event: OpusControlEvent

    var events = mutableListOf<OpusTree<OpusControlEvent>?>()
    var initial_event = this.default_event

    init {
        for (i in 0 until beat_count) {
            this.insert_beat(i)
        }
    }

    fun get_proceding_leaf_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        var working_position = position.toMutableList()
        var working_beat = beat

        var working_tree = this.get_tree(working_beat, working_position)

        // Move right/up
        while (true) {
            if (working_tree.parent != null) {
                if (working_tree.parent!!.size - 1 > working_position.last()) {
                    working_position[working_position.size - 1] += 1
                    working_tree = this.get_tree(working_beat, working_position)
                    break
                } else {
                    working_position.removeLast()
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat < this.events.size - 1) {
                working_beat += 1
                working_position = mutableListOf()
                working_tree = this.get_tree(working_beat, working_position)
                break
            } else {
                return null
            }
        }
        // Move left/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(0)
            working_tree = working_tree[0]
        }
        return Pair(working_beat, working_position)
    }

    fun set_initial_event(value: OpusControlEvent) {
        this.initial_event = value
    }

    fun get_latest_event(beat: Int, position: List<Int>): OpusControlEvent {
        val current_tree = this.get_tree(beat, position)
        if (current_tree.is_event()) {
            return current_tree.get_event()!!
        }

        var working_beat = beat
        var working_position = position.toList()
        var output = this.initial_event

        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat, working_position) ?: return output
            working_beat = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat, working_position)
            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                output = working_event
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

class VolumeController(beat_count: Int): ActiveController(beat_count) { }
class TempoController(beat_count: Int): AciveController(beat_count) {}

class ActiveControlSet(var beat_count: Int, default_enabled: Set<ControlEventType>? = null) {
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
        this.beat_count = 0
        this.controllers.clear()
    }

    fun size(): Int {
        return this.controllers.size
    }

    fun get_all(): Array<Pair<ControlEventType, ActiveController>> {
        var keys = this.controllers.keys.toList().sorted()
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

    fun has_controller(type: ControlEventType): Boolean {
        return this.controllers.containsKey(type)
    }

    fun set_beat_count(new_count: Int) {
        this.beat_count = new_count
        for ((_, controller) in this.get_all()) {
            controller.set_beat_count(new_count)
        }
    }
}

