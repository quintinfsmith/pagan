package com.qfs.radixulous.opusmanager
import java.io.File

open class LinksLayer() : OpusManagerBase() {
    var linked_beat_map: HashMap<BeatKey, BeatKey> = HashMap<BeatKey, BeatKey>()
    var inv_linked_beat_map: HashMap<BeatKey, MutableList<BeatKey>> = HashMap<BeatKey, MutableList<BeatKey>>()

    override fun reset() {
        this.linked_beat_map.clear()
        this.inv_linked_beat_map.clear()
        super.reset()
    }

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
    open fun link_beats(beat_key: BeatKey, target: BeatKey) {
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
        for (linked_key in this.get_all_linked(beat_key)) {
            super.insert_after(linked_key, position)
        }
    }
    override fun remove(beat_key: BeatKey, position: List<Int>) {
        for (linked_key in this.get_all_linked(beat_key)) {
            super.remove(linked_key, position)
        }
    }
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        for (linked_key in this.get_all_linked(beat_key)) {
            super.set_percussion_event(linked_key, position)
        }
    }
    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        for (linked_key in this.get_all_linked(beat_key)) {
            super.set_event(linked_key, position, event)
        }
    }
    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        for (linked_key in this.get_all_linked(beat_key)) {
            super.split_tree(linked_key, position, splits)
        }
    }
    override fun unset(beat_key: BeatKey, position: List<Int>) {
        for (linked_key in this.get_all_linked(beat_key)) {
            super.unset(linked_key, position)
        }
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
            new_link_map.put(new_beat, new_target)

            if (! this.inv_linked_beat_map.containsKey(new_target)) {
                this.inv_linked_beat_map.put(new_target, mutableListOf())
            }
            this.inv_linked_beat_map.get(new_target)!!.add(new_beat)
        }
        this.linked_beat_map = new_link_map
    }

    override fun change_line_channel(old_channel: Int, line_offset: Int, new_channel: Int) {
        super.change_line_channel(old_channel, line_offset, new_channel)
        var new_offset = this.channel_lines[new_channel].size - 1
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

    override fun move_line(channel: Int, old_index: Int, new_index: Int) {
        super.move_line(channel, old_index, new_index)

        this.remap_links(this::rh_move_line, listOf(channel, old_index, new_index))
    }

    private fun rh_move_line(beat: BeatKey, args: List<Int>): BeatKey? {
        var channel = args[0]
        var old_index = args[1]
        var new_index = args[2]

        var new_beat = beat
        if (beat.channel == channel) {
            if (beat.line_offset == old_index) {
                new_beat = BeatKey(beat.channel, new_index, beat.beat)
            } else if (old_index < beat.line_offset && beat.line_offset < new_index) {
                new_beat = BeatKey(beat.channel, beat.line_offset - 1, beat.beat)
            }
        }
        return new_beat
    }

    override fun insert_beat(index: Int?) {
        super.insert_beat(index)

        var index_remap: Int = index ?: this.opus_beat_count
        this.remap_links(this::rh_insert_beat, listOf(index_remap))
    }

    private fun rh_insert_beat(beat: BeatKey, args: List<Int>): BeatKey? {
        var index = args[0]
        var new_beat = if (beat.beat >= index) {
            BeatKey(beat.channel, beat.line_offset, beat.beat + 1)
        } else {
            beat
        }
        return new_beat
    }

    override fun remove_beat(index: Int?) {
        super.remove_beat(index)
        var adj_index = if (index == null) {
            -1
        } else {
            index
        }
        this.remap_links(this::rh_remove_beat, listOf(adj_index))
    }

    private fun rh_remove_beat(beat: BeatKey, args: List<Int>): BeatKey? {
        var index = args[0]
        var new_beat: BeatKey

        if (beat.beat >= index) {
            new_beat = BeatKey(beat.channel, beat.line_offset, beat.beat - 1)
        } else {
            new_beat = beat
        }

        return new_beat
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.remap_links(this::rh_remove_channel, listOf(channel))
    }

    private fun rh_remove_channel(beat: BeatKey, args: List<Int>): BeatKey? {
        if (beat.channel == args[0]) {
            return null
        } else {
            return beat
        }
    }

    override fun remove_line(channel: Int, line_offset: Int?) {
        super.remove_line(channel, line_offset)
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

    override fun swap_channels(channel_a: Int, channel_b: Int) {
        super.swap_channels(channel_a, channel_b)
        this.remap_links(this::rh_swap_channels, listOf(channel_a, channel_b))
    }
    private fun rh_swap_channels(beat: BeatKey, args: List<Int>): BeatKey? {
        return when (beat.channel) {
            args[0] -> {
                BeatKey(args[1], beat.line_offset, beat.beat)
            }
            args[1] -> {
                BeatKey(args[0], beat.line_offset, beat.beat)
            }
            else -> {
                beat
            }
        }
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

    override fun load_folder(path: String) {
        super.load_folder(path)
        var file = File("${this.path}/linkedbeats.json")
        if (! file.isFile()) {
            return
        }
        var json_patt = ".*?\"(?<a>\\d*)\\.(?<b>\\d*)\\.(?<c>\\d*)\".*?:.*?\\[(?<d>\\d*),(?<e>\\d*),(?<f>\\d*)\\]".toRegex()
        var content = file.readText()
        content = content.replace(" ", "")
        content = content.replace("\t", "")

        for (hit in json_patt.findAll(content)) {
            var key_beat = BeatKey(
                hit.groups[1]!!.value.toInt(),
                hit.groups[2]!!.value.toInt(),
                hit.groups[3]!!.value.toInt()
            )
            var target_beat = BeatKey(
                hit.groups[4]!!.value.toInt(),
                hit.groups[5]!!.value.toInt(),
                hit.groups[6]!!.value.toInt()
            )
            this.linked_beat_map[key_beat] = target_beat
        }

        for ((beat, target) in this.linked_beat_map) {
            if (!this.inv_linked_beat_map.containsKey(target)) {
                this.inv_linked_beat_map[target] = mutableListOf()
            }
            this.inv_linked_beat_map[target]!!.add(beat)
        }
    }

    override fun save(path: String?) {
        super.save(path)
        var lines: MutableList<String> = mutableListOf()
        for ((k, target) in this.linked_beat_map) {
            lines.add(
                "    \"${k.channel}.${k.line_offset}.${k.beat}\": [${target.channel},${target.line_offset},${target.beat}]\""
            )
        }
        var json_string: String = lines.joinToString(",\n", "{", "}")
        File("${this.path}/linkedbeats.json").writeText(json_string)
    }
}
