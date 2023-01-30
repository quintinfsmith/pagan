package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.structure.OpusTree

// Block set functions that don't actually change anything
open class BlockerLayer(): HistoryLayer() {
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.set_percussion_event(beat_key, position)
    }

    override fun apply_undo() {
        if (this.history_cache.isEmpty()) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.apply_undo()
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        if (this.get_percussion_instrument(line_offset) == instrument) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.set_percussion_instrument(line_offset, instrument)
    }

    override fun set_channel_instrument(channel:Int, instrument: Int) {
        if (this.get_channel_instrument(channel) == instrument) {
            Log.e("blocker", "inane event blocked")
            return
        }

        super.set_channel_instrument(channel, instrument)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beat_key, position, event)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (tree.is_leaf()) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.unset(beat_key, position)
    }


    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        if (new_beat == old_beat) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.overwrite_beat(old_beat, new_beat)
    }

    override fun remove_channel(channel: Int) {
        if (this.channels[channel].size == 0) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.remove_channel(channel)
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        if (this.get_tree(beat_key, position) == tree) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.replace_tree(beat_key, position, tree)
    }

    override fun swap_channels(channel_a: Int, channel_b: Int) {
        if (channel_a == channel_b) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.swap_channels(channel_a, channel_b)
    }

    override fun set_beat_count(new_count: Int) {
        if (new_count == this.opus_beat_count) {
            Log.e("blocker", "inane event blocked")
            return
        }
        super.set_beat_count(new_count)
    }
}
