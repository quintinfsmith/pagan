package com.qfs.pagan.opusmanager

class ActiveControlSet(var beat_count: Int, default_enabled: Set<ControlEventType>? = null) {
    val controllers = HashMap<ControlEventType, ActiveController<out OpusControlEvent>>()

    init {
        for (type in default_enabled ?: setOf()) {
            this.new_controller(type)
        }
    }
    private fun _init_blocked_tree_caches() {
        var beat = 0
        var position = this.get_first_position(beat, listOf())
        while (true) {
            val working_tree = this.get_tree(beat_key, position)
            if (working_tree.is_event()) {
                this._cache_tree_overlaps(beat_key, position)
            }

            val pair = this.get_proceding_leaf_position(beat_key, position) ?: break
            beat_key.beat = pair.first
            position = pair.second
        }
    }

    fun clear() {
        this.beat_count = 0
        this.controllers.clear()
    }

    fun size(): Int {
        return this.controllers.size
    }

    fun get_all(): Array<Pair<ControlEventType, ActiveController<out OpusControlEvent>>> {
        var keys = this.controllers.keys.toList().sorted()
        return Array(this.controllers.size) {
            Pair(keys[it], this.controllers[keys[it]]!!)
        }
    }

    fun new_controller(type: ControlEventType, controller: ActiveController<*>? = null) {
        if (controller == null) {
            this.controllers[type] = when (type) {
                ControlEventType.Tempo -> TempoController(this.beat_count)
                ControlEventType.Volume -> VolumeController(this.beat_count)
                ControlEventType.Reverb -> ReverbController(this.beat_count)
            }
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


    fun <T: OpusControlEvent> get_controller(type: ControlEventType): ActiveController<T> {
        if (!this.controllers.containsKey(type)) {
            this.new_controller(type)
        }
        return this.controllers[type] as ActiveController<T>
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
