package com.qfs.radixulous

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
class BaseLayerUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_new() {
        var manager = OpusManager()
        manager.new()
        assertNotEquals(manager.opus_beat_count, 0)
    }

    @Test
    fun test_insert_after() {
        var manager = OpusManager()
        manager.new()
        var beat_key = BeatKey(0, 0, 0)
        var beat_tree = manager.get_beat_tree(beat_key)
        beat_tree.set_size(1)
        var tree = manager.get_tree(beat_key, listOf(0))
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
        manager.set_event(beat_key, position, OpusEvent(30, 0, 0,false))
        manager.split_tree(beat_key, position, split_count)
        var subtree = manager.get_tree(beat_key, position)
        assertEquals(subtree.size, split_count)
    }



}
