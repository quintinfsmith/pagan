package com.qfs.radixulous

import android.util.Log
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import org.junit.Test
import org.junit.Assert.*
import com.qfs.radixulous.opusmanager.OpusManagerBase as OpusManager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FlagLayerUnitTest {
    @Test
    fun test_new() {
        var manager = OpusManager()
        manager.new()
        assertNotEquals(manager.opus_beat_count, 0)
    }

    @Test
    fun test_get_channel_count() {
        //TODO("test_get_channel_count")
    }
    @Test
    fun test_get_channel_instrument() {
        //TODO("test_get_channel_instrument")
    }
    @Test
    fun test_get_percussion_instrument() {
        //TODO("test_get_percussion_instrument")
    }
    @Test
    fun test_get_beat_tree() {
        //TODO("test_get_beat_tree")
    }
    @Test
    fun test_get_proceding_leaf() {
        //TODO("test_get_proceding_leaf")
    }
    @Test
    fun test_get_preceding_leaf() {
        //TODO("test_get_preceding_leaf")
    }
    @Test
    fun test_get_proceding_leaf_position() {
        //TODO("test_get_proceding_leaf_position")
    }
    @Test
    fun test_get_preceding_leaf_position() {
        //TODO("test_get_preceding_leaf_position")
    }
    @Test
    fun test_get_absolute_value() {
        //TODO("test_get_absolute_value")
    }
    @Test
    fun test_get_channel_line_counts() {
        //TODO("test_get_channel_line_counts")
    }
    @Test
    fun test_has_preceding_absolute_event() {
        //TODO("test_has_preceding_absolute_event")
    }
    @Test
    fun test_is_percussion() {
        //TODO("test_is_percussion")
    }
    @Test
    fun test_convert_event_to_relative() {
        //TODO("test_convert_event_to_relative")
    }
    @Test
    fun test_convert_event_to_absolute() {
        //TODO("test_convert_event_to_absolute")
    }
    @Test
    fun test_set_percussion_event() {
        //TODO("test_set_percussion_event")
    }
    @Test
    fun test_set_percussion_instrument() {
        //TODO("test_set_percussion_instrument")
    }
    @Test
    fun test_set_percussion_channel() {
        //TODO("test_set_percussion_channel")
    }
    @Test
    fun test_unset_percussion_channel() {
        //TODO("test_unset_percussion_channel")
    }
    @Test
    fun test_set_event() {
        //TODO("test_set_event")
    }
    @Test
    fun test_unset() {
        //TODO("test_unset")
    }
    @Test
    fun test_new_channel() {
        //TODO("test_new_channel")
    }
    @Test
    fun test_change_line_channel() {
        //TODO("test_change_line_channel")
    }
    @Test
    fun test_insert_beat() {
        //TODO("test_insert_beat")
    }
    @Test
    fun test_new_line() {
        //TODO("test_new_line")
    }
    @Test
    fun test_overwrite_beat() {
        //TODO("test_overwrite_beat")
    }
    @Test
    fun test_remove_beat() {
        //TODO("test_remove_beat")
    }
    @Test
    fun test_remove_channel() {
        //TODO("test_remove_channel")
    }
    @Test
    fun test_remove_line() {
        //TODO("test_remove_line")
    }
    @Test
    fun test_replace_beat() {
        //TODO("test_replace_beat")
    }
    @Test
    fun test_replace_tree() {
        //TODO("test_replace_tree")
    }
    @Test
    fun test_set_beat_count() {
        //TODO("test_set_beat_count")
    }
    @Test
    fun test_get_midi() {
        //TODO("test_get_midi")
    }
    @Test
    fun test_to_json() {
        //TODO("test_to_json")
    }
    @Test
    fun test_save() {
        //TODO("test_save")
    }
    @Test
    fun test_load() {
        //TODO("test_load")
    }
    @Test
    fun test_import_midi() {
        //TODO("test_import_midi")
    }
    @Test
    fun test_purge_cache() {
        //TODO("test_purge_cache")
    }
    @Test
    fun test_reset_cache() {
        //TODO("test_reset_cache")
    }


    @Test
    fun test_insert_after() {
        var manager = OpusManager()
        manager.new()
        var beat_key = BeatKey(0, 0, 0)
        var beat_tree = manager.get_beat_tree(beat_key)
        beat_tree.set_size(1)
        var initial_length = beat_tree.size
        manager.insert_after(beat_key, listOf(0))
        assertEquals(beat_tree.size, initial_length + 1)

        //manager.insert_after(beat_key, listOf())
    }

    @Test
    fun test_remove() {
        var manager = OpusManager()
        manager.new()
        var beat_key = BeatKey(0, 0, 0)
        var beat_tree = manager.get_beat_tree(beat_key)
        beat_tree.set_size(2)
        // Insert empty tree in the first beat
        manager.insert_after(beat_key, listOf(0))

        //Then remove that tree
        manager.remove(beat_key, listOf(1))
        assertEquals(beat_tree.size, 2)

        // Check that the siblings get adjusted
        for (i in 0 until 3) {
            manager.insert_after(beat_key, listOf(0))
        }
        var tree = manager.get_tree(beat_key, listOf(3))
        manager.remove(beat_key, listOf(2))
        assertEquals(tree, manager.get_tree(beat_key, listOf(2)))
    }

    @Test
    fun test_split_tree() {
        var manager = OpusManager()
        manager.new()
        var split_count = 5
        var beat_key = BeatKey(0, 0, 0)

        // split a beat
        manager.split_tree(beat_key, listOf(), split_count)
        var beat_tree = manager.get_beat_tree(beat_key)
        assertEquals(beat_tree.size, split_count)

        // Split an open leaf
        manager.split_tree(beat_key, listOf(split_count - 1), split_count)
        beat_tree = manager.get_beat_tree(beat_key)
        assertEquals(beat_tree.get(split_count - 1).size, split_count)

        // split an event
        var position = mutableListOf(split_count - 1, 0)

        manager.set_event(beat_key, position, OpusEvent(30, 0, 0, false))

        manager.split_tree(beat_key, position, split_count)
        var subtree = manager.get_tree(beat_key, position)
        assertEquals(subtree.size, split_count)
    }
}
