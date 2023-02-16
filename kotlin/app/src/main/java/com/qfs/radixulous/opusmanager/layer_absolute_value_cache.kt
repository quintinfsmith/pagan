package com.qfs.radixulous.opusmanager

import android.util.Log
import com.qfs.radixulous.structure.OpusTree

open class AbsoluteValueLayer: OpusManagerBase() {
    var absolute_values_cache = HashMap<Pair<BeatKey, List<Int>>, Int>()
    // Layer-Specific //
    private fun cache_absolute_value(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        var event_value = if (event.relative) {
            this.get_absolute_value(beat_key, position)
                ?: throw Exception("Can't cache relative value with no preceding absolute value")
        } else {
            event.note
        }

        this.absolute_values_cache[Pair(beat_key, position)] = event_value

    }

    // Update proceding absolute values in cache
    private fun cascade_cache_proceding_values(beat_key: BeatKey, position: List<Int>, initial_value: Int? = null) {
        var event_value = initial_value ?: (this.get_absolute_value(beat_key, position) ?: throw Exception("calling cascade without setting absolute value first"))

        var next: Pair<BeatKey, List<Int>> = Pair(beat_key, position)
        while (true) {
            next = this.get_proceding_leaf_position(next.first, next.second) ?: break

            var next_leaf = this.get_tree(next.first, next.second)
            var next_event = next_leaf.get_event() ?: continue

            // No need to continue updating if the next event is absolute
            if (!next_event.relative) {
                break
            }

            event_value += next_event.note
            this.absolute_values_cache[next] = event_value
        }
    }

    private fun decache_tree(old_beat: BeatKey, position: List<Int>) {
        var to_remove: MutableList<Pair<BeatKey, List<Int>>> = mutableListOf()

        for ((pair, _) in this.absolute_values_cache) {
            if (old_beat != pair.first) {
                continue
            }

            if (pair.second.subList(0, position.size) == position) {
                to_remove.add(pair)
            }
        }

        for (key in to_remove) {
            this.decache_absolute_value(key.first, key.second)
        }
    }

    private fun cache_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.decache_tree(beat_key, position)

        var stack = mutableListOf(Pair(position, tree))
        while (stack.isNotEmpty()) {
            var (working_position, working_tree) = stack.removeAt(0)
            if (working_tree.is_event()) {
                this.cache_absolute_value(beat_key, working_position, working_tree.get_event()!!)
            } else if (!working_tree.is_leaf()) {
                for (i in 0 until working_tree.size) {
                    var new_position = working_position.toMutableList()
                    new_position.add(i)
                    stack.add(Pair(new_position, working_tree.get(i)))
                }
            }
        }
    }

    private fun decache_absolute_value(beat_key: BeatKey, position: List<Int>) {
        this.absolute_values_cache.remove(Pair(beat_key, position))
    }

    private fun shift_absolute_value_cache_beat(index: Int, amount: Int) {
        var new_map = HashMap<Pair<BeatKey, List<Int>>, Int>()
        for ((pair, value) in this.absolute_values_cache) {
            var (beatkey, position) = pair
            if (beatkey.beat >= index) {
                beatkey.beat += amount
            }
            new_map[Pair(beatkey, position)] = value
        }
        this.absolute_values_cache = new_map
    }

    private fun shift_absolute_value_cache(beatkey: BeatKey, position: List<Int>, amount: Int) {
        var new_map = HashMap<Pair<BeatKey, List<Int>>, Int>()
        for ((pair, value) in this.absolute_values_cache) {
            var (working_beatkey, working_position_list) = pair
            if (working_beatkey != beatkey) {
                continue
            }
            var working_position = working_position_list.toMutableList()
            if (working_position.subList(0, position.size) == position) {
                working_position[position.size - 1] += amount
            }

            new_map[Pair(beatkey, working_position)] = value
        }
        this.absolute_values_cache = new_map
    }

    private fun shift_absolute_value_cache_line(channel: Int, line: Int, amount: Int) {
        var new_map = HashMap<Pair<BeatKey, List<Int>>, Int>()
        for ((pair, value) in this.absolute_values_cache) {
            var (beatkey, position) = pair
            if (beatkey.channel != channel) {
                continue
            }
            if (beatkey.line_offset >= line) {
                beatkey.line_offset += amount
            }
            new_map[Pair(beatkey, position)] = value
        }
        this.absolute_values_cache = new_map
    }

    // End Layer-Specific //

    override fun change_line_channel(old_channel: Int, line_index: Int, new_channel: Int) {
        //TODO: This needs a test. i'm not sure if iterating a hashmap's items is readonly or not
        super.change_line_channel(old_channel, line_index, new_channel)
        for ((pair, value) in this.absolute_values_cache) {
            if (pair.first.channel == old_channel && pair.first.line_offset == line_index) {
                pair.first.channel = new_channel
                pair.first.line_offset = this.channels[new_channel].size - 1
            }
        }
    }

    override fun new_line(channel: Int, index: Int?): List<OpusTree<OpusEvent>> {
        var output = super.new_line(channel, index)

        if (index != null) {
            this.shift_absolute_value_cache_line(channel, index, 1)
        }

        return output
    }


    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beat_key, position, event)
        this.cache_absolute_value(beat_key, position, event)
        this.cascade_cache_proceding_values(beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.decache_absolute_value(beat_key, position)

        // It's reasonable that there isn't always an absolute value set after an unset
        // So skip the cascade if that is the case
        var event_value = this.get_absolute_value(beat_key, position) ?: return

        this.cascade_cache_proceding_values(beat_key, position, event_value)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.shift_absolute_value_cache(beat_key, position, 1)
    }

    override fun insert_beat(index: Int?) {
        super.insert_beat(index)
        if (index != null) {
            this.shift_absolute_value_cache_beat(index, 1)
        }
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.shift_absolute_value_cache_beat(beat_index, -1)
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        var to_remove: MutableList<Pair<BeatKey, List<Int>>> = mutableListOf()

        for ((pair, _) in this.absolute_values_cache) {
            if (pair.first.channel == channel) {
                to_remove.add(pair)
            }
        }
        for (key in to_remove) {
            this.absolute_values_cache.remove(key)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.cache_tree(beat_key, position, tree)
    }

    override fun remove_line(channel: Int, index: Int) {
        super.remove_line(channel, index)
        this.shift_absolute_value_cache_line(channel, index, -1)
    }

    override fun set_beat_count(new_count: Int) {
        if (new_count <= this.opus_beat_count) {
            var new_cache = HashMap<Pair<BeatKey, List<Int>>, Int>()
            for ((pair, value) in this.absolute_values_cache) {
                if (pair.first.beat >= new_count) {
                    continue
                }
                new_cache[pair] = value
            }
            this.absolute_values_cache = new_cache
        }

        super.set_beat_count(new_count)
    }

    override fun set_percussion_event(beatkey: BeatKey, position: List<Int>) {
        super.set_percussion_event(beatkey, position)
        this.absolute_values_cache[Pair(beatkey, position)] = this.get_percussion_instrument(beatkey.line_offset)
    }

    override fun reset() {
        super.reset()
        this.absolute_values_cache.clear()
    }

}

