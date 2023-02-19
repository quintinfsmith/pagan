package com.qfs.radixulous.opusmanager
import java.io.File

open class LinksLayer() : AbsoluteValueLayer() {
    var linked_beat_map: HashMap<BeatKey, BeatKey> = HashMap<BeatKey, BeatKey>()
    var inv_linked_beat_map: HashMap<BeatKey, MutableList<BeatKey>> = HashMap<BeatKey, MutableList<BeatKey>>()
    var link_locker: Int = 0

    open fun unlink_beat(beat_key: BeatKey) {
        if (! this.linked_beat_map.containsKey(beat_key)) {
           return
        }

        var target_key = this.linked_beat_map.get(beat_key)
        var beats: MutableList<BeatKey> = this.inv_linked_beat_map.get(target_key)!!
        beats.remove(beat_key)
        if (beats.size == 0) {
            this.inv_linked_beat_map.remove(target_key)
        }
        this.linked_beat_map.remove(beat_key)
    }

    fun clear_links_to_beat(beat_key: BeatKey) {
        if (! this.inv_linked_beat_map.containsKey(beat_key)) {
            return
        }
        var links = this.inv_linked_beat_map[beat_key]!!
        for (link_key in links) {
            this.linked_beat_map.remove(link_key)
        }
        this.inv_linked_beat_map.remove(beat_key)
    }

    fun clear_links_in_network(beat_key: BeatKey) {
        if (this.is_reflection(beat_key.channel, beat_key.line_offset, beat_key.beat)) {
            this.clear_links_to_beat(this.linked_beat_map[beat_key]!!)
        } else if (this.is_reflected(beat_key.channel, beat_key.line_offset, beat_key.beat)) {
            this.clear_links_to_beat(beat_key)
        }

    }

    // Remove a link from a network without destroying the remaining links
    // this will arbitrarily pick another central beat to be reflected
    // TODO: Remove the need for a central beat?
    fun remove_link_from_network(beat_key: BeatKey) {
        if (this.is_reflection(beat_key.channel, beat_key.line_offset, beat_key.beat)) {
            // Nothing special, can just remove the link
            this.unlink_beat(beat_key)
        } else if (this.is_reflected(beat_key.channel, beat_key.line_offset, beat_key.beat)) {
            if (this.inv_linked_beat_map[beat_key]!!.size > 1) {
                // arbitrarily pick a new focal beat, and relink all beats to it
                var new_center = this.inv_linked_beat_map[beat_key]!!.first()
                this.unlink_beat(new_center)

                for (to_relink in this.inv_linked_beat_map[beat_key]!!) {
                    this.link_beats(to_relink, new_center)
                }
            } else {
                // network isn't large enough to need re-focusing. just remove the 1 link
                this.clear_links_to_beat(beat_key)
            }
        }
    }

    open fun link_beats(beat_key: BeatKey, target: BeatKey) {
        if (beat_key == target) {
            return
        }
        // Don't chain links. if attempting to reflect a reflection, find the root beat
        // and reflect that
        if (this.linked_beat_map.containsKey(target)) {
            this.link_beats(
                beat_key,
                this.linked_beat_map[target]!!
            )
            return
        }

        // Remove any existing link
        this.unlink_beat(beat_key)
        // Replace existing tree with a copy of the target
        this.overwrite_beat(beat_key, target)
        this.linked_beat_map[beat_key] = target
        if (! this.inv_linked_beat_map.containsKey(target)) {
            this.inv_linked_beat_map[target] = mutableListOf()
        }
        this.inv_linked_beat_map[target]!!.add(beat_key)
    }

    fun get_all_linked(beat_key: BeatKey): Set<BeatKey> {
        if (this.link_locker > 1) {
            return setOf(beat_key)
        }

        var output: MutableSet<BeatKey> = mutableSetOf()
        if (this.inv_linked_beat_map.containsKey(beat_key)) {
            output.add(beat_key)
            for (linked_key in this.inv_linked_beat_map[beat_key]!!) {
                output.add(linked_key)
            }
        } else if (this.linked_beat_map.contains(beat_key)) {
            var target_key = this.linked_beat_map[beat_key]!!
            output.add(target_key)
            for (linked_key in this.inv_linked_beat_map[target_key]!!) {
                output.add(linked_key)
            }
        } else {
            output.add(beat_key)
        }

        return output
    }


    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.insert_after(linked_key, position)
        }
        this.link_locker -= 1
    }
    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.remove(linked_key, position)
        }
        this.link_locker -= 1
    }
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.set_percussion_event(linked_key, position)
        }
        this.link_locker -= 1
    }
    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.set_event(linked_key, position, event)
        }
        this.link_locker -= 1
    }
    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.split_tree(linked_key, position, splits)
        }
        this.link_locker -= 1
    }
    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.unset(linked_key, position)
        }
        this.link_locker -= 1
    }

    /////////
    private fun remap_links(remap_hook: (beat_key: BeatKey, args: List<Int>) -> BeatKey?, args: List<Int>) {
        var new_link_map = HashMap<BeatKey, BeatKey>()
        this.inv_linked_beat_map.clear()
        for (beat in this.linked_beat_map.keys) {
            var target = this.linked_beat_map.get(beat)!!
            var new_beat = remap_hook(beat, args)
            var new_target = remap_hook(target, args)
            if (new_beat == null || new_target == null) {
                continue
            }
            new_link_map[new_beat] = new_target

            if (! this.inv_linked_beat_map.containsKey(new_target)) {
                this.inv_linked_beat_map.put(new_target, mutableListOf())
            }
            this.inv_linked_beat_map.get(new_target)!!.add(new_beat)
        }
        this.linked_beat_map = new_link_map
    }

    override fun change_line_channel(old_channel: Int, line_offset: Int, new_channel: Int) {
        super.change_line_channel(old_channel, line_offset, new_channel)
        var new_offset = this.channels[new_channel].size - 1
        this.remap_links(this::rh_change_line_channel, listOf(old_channel, line_offset, new_channel, new_offset))
    }

    private fun rh_change_line_channel(beat_key: BeatKey, args: List<Int>): BeatKey? {
        var old_channel = args[0]
        var line_offset = args[1]
        var new_channel = args[2]
        var new_offset = args[3]

        var new_beat = beat_key
        if (beat_key.channel == old_channel) {
            if (beat_key.line_offset == line_offset) {
                new_beat = BeatKey(new_channel, new_offset, beat_key.beat)
            } else if (beat_key.line_offset > line_offset) {
                new_beat = BeatKey(beat_key.channel, beat_key.line_offset - 1, beat_key.beat)
            }
        }
        return new_beat
    }


    private fun rh_remove_beat(beat: BeatKey, args: List<Int>): BeatKey? {
        var index = args[0]
        var new_beat = if (beat.beat >= index) {
            BeatKey(beat.channel, beat.line_offset, beat.beat - 1)
        } else {
            beat
        }

        return new_beat
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.remap_links(this::rh_remove_channel, listOf(channel))
    }

    private fun rh_remove_channel(beat: BeatKey, args: List<Int>): BeatKey? {
        return if (beat.channel == args[0]) {
            null
        } else {
            beat
        }
    }

    private fun rh_remove_line(beat: BeatKey, args: List<Int>): BeatKey? {
        var channel = args[0]
        var line_offset = args[1]
        var new_beat: BeatKey? = beat
        if (beat.channel == channel) {
            if (beat.line_offset == line_offset) {
                new_beat = null
            } else if (beat.line_offset > line_offset) {
                new_beat = BeatKey(channel, line_offset - 1, beat.beat)
            }
        }
        return new_beat
    }

    fun get_reflected(channel: Int, line_offset: Int, beat: Int): BeatKey? {
        return this.linked_beat_map[BeatKey(channel, line_offset, beat)]
    }

    fun is_reflection(channel: Int, line_offset: Int, beat: Int): Boolean {
        return this.linked_beat_map.containsKey(BeatKey(channel, line_offset, beat))
    }

    fun is_reflected(channel: Int, line_offset: Int, beat: Int): Boolean {
        return this.inv_linked_beat_map.containsKey(BeatKey(channel, line_offset, beat))
    }

    fun is_networked(channel: Int, line_offset: Int, beat: Int): Boolean {
        return this.linked_beat_map.containsKey(BeatKey(channel, line_offset, beat)) || this.inv_linked_beat_map.containsKey(BeatKey(channel, line_offset, beat))
    }


    override fun load_json(json_data: LoadedJSONData) {
        super.load_json(json_data)
        if (json_data.reflections == null) {
            return
        }

        for (pool in json_data.reflections!!) {
            var target = pool[0]
            this.inv_linked_beat_map[target] = mutableListOf()
            for (i in 1 until pool.size) {
                this.linked_beat_map[pool[i]] = target
                inv_linked_beat_map[target]!!.add(pool[i])
            }
        }
    }

    override fun to_json(): LoadedJSONData {
        var data = super.to_json()
        var reflections: MutableList<List<BeatKey>> = mutableListOf()
        for ((target, others) in this.inv_linked_beat_map) {
            var pool: MutableList<BeatKey> = mutableListOf()
            pool.add(target)
            for (other in others) {
                pool.add(other)
            }
            reflections.add(pool)
        }
        data.reflections = reflections
        return data
    }
}
