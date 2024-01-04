package com.qfs.pagan

import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import com.qfs.pagan.opusmanager.BaseLayer as OpusManager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BaseLayerUnitTest {
    @Test
    fun test_new() {
        val manager = OpusManager()
        manager.new()
        assertNotEquals(manager.beat_count, 0)
    }

    @Test
    fun test_set_channel_instrument() {
        val manager = OpusManager()
        manager.new()
        manager.set_channel_instrument(0, Pair(5,2))
        assertEquals(
            "Failed to set channel instrument",
            Pair(5,2),
            manager.get_channel_instrument(0)
        )

    }

    @Test
    fun test_get_percussion_instrument() {
        val manager = OpusManager()
        manager.new()
        assertEquals(
            "Percussion instrument wasn't correctly defaulted",
            OpusManager.DEFAULT_PERCUSSION,
            manager.get_percussion_instrument(0)
        )

        manager.set_percussion_channel(0)
        manager.set_percussion_instrument(0, OpusManager.DEFAULT_PERCUSSION + 1)


        assertEquals(
            "Percussion instrument wasn't set",
            OpusManager.DEFAULT_PERCUSSION + 1,
            manager.get_percussion_instrument(0)
        )
    }

    @Test
    fun test_get_beat_tree() {
        val manager = OpusManager()
        manager.new()
        val beatkey = BeatKey(0,0,0)
        manager.split_tree(beatkey, listOf(), 12)

        var tree = manager.get_beat_tree(beatkey)
        assertEquals(
            "Got wrong beat tree",
            12,
            tree.size
        )

        assertThrows(Exception::class.java) { manager.get_beat_tree(BeatKey(2,0,0)) }
    }

    @Test
    fun test_get_proceding_leaf() {
        // TODO("test_get_proceding_leaf")
    }
    @Test
    fun test_get_preceding_leaf() {
        //TODO("test_get_preceding_leaf")
    }

    @Test
    fun test_get_proceding_leaf_position() {
        val manager = OpusManager()
        manager.new()

        val first = BeatKey(0,0,1)

        manager.split_tree(first, listOf(), 2)
        assertEquals(
            "Failed to get proceding leaf when it was immediately next to it",
            Pair(first, listOf(1)),
            manager.get_proceding_leaf_position(first, listOf(0))
        )

        manager.split_tree(first, listOf(1), 2)
        assertEquals(
            "Failed to get proceding leaf when it was a niece",
            Pair(first, listOf(1, 0)),
            manager.get_proceding_leaf_position(first, listOf(0))
        )

        manager.split_tree(first, listOf(0), 2)
        assertEquals(
            "Failed to get proceding leaf when it was a cousin",
            Pair(first, listOf(1, 0)),
            manager.get_proceding_leaf_position(first, listOf(0,1))
        )

        manager.split_tree(first, listOf(0, 1), 2)

        assertEquals(
            "Failed to return null when looking for proceding leaf after last position",
            null,
            manager.get_proceding_leaf_position(BeatKey(0,0, manager.beat_count - 1),listOf())
        )

        assertEquals(
            "Failed to get proceding leaf across beats",
            Pair(first, listOf(0,0)),
            manager.get_proceding_leaf_position(BeatKey(0,0,0), listOf())
        )
    }
    @Test
    fun test_get_preceding_leaf_position() {
        val manager = OpusManager()
        manager.new()

        val first = BeatKey(0,0,0)
        manager.split_tree(first, listOf(), 2)
        assertEquals(
            "Failed to get preceding leaf when it was a sibling",
            Pair(first, listOf(0)),
            manager.get_preceding_leaf_position(first, listOf(1))
        )

        manager.split_tree(first, listOf(0), 2)
        assertEquals(
            "Failed to get preceding leaf when it was a neice",
            Pair(first, listOf(0,1)),
            manager.get_preceding_leaf_position(first, listOf(1))
        )

        manager.split_tree(first, listOf(1), 2)
        assertEquals(
            "Failed to get preceding leaf when it was a cousin",
            Pair(first, listOf(0,1)),
            manager.get_preceding_leaf_position(first, listOf(1, 0))
        )

        assertEquals(
            "Failed to return null when attempting to get preceding leaf before first position",
            null,
            manager.get_preceding_leaf_position(first, listOf(0,0))
        )

        assertEquals(
            "Failed to get preceding leaf across beats",
            Pair(first, listOf(1,1)),
            manager.get_preceding_leaf_position(BeatKey(0,0,1), listOf())
        )
    }

    @Test
    fun test_get_absolute_value() {
        val manager = OpusManager()
        manager.new()

        val first_event = OpusEvent(25, 0, false)
        val second_event = OpusEvent(1, 0, true)
        val third_event = OpusEvent(-6, 0, true)

        manager.set_event( BeatKey(0,0,1), listOf(), first_event )
        manager.set_event( BeatKey(0,0,2), listOf(), second_event )
        manager.set_event( BeatKey(0,0,3), listOf(), third_event )

        assertEquals(
            "Failed to get correct absolute value on absolute event",
            first_event.note,
            manager.get_absolute_value(BeatKey(0,0,1), listOf())
        )

        assertEquals(
            "Failed to get correct relative value on 1st relative event",
            first_event.note + second_event.note,
            manager.get_absolute_value(BeatKey(0,0,2), listOf()),
        )

        assertEquals(
            "Failed to get correct relative value on 2nd relative event",
            first_event.note + second_event.note + third_event.note,
            manager.get_absolute_value(BeatKey(0,0,3), listOf()),
        )

        assertEquals(
            "Failed to get null on leaf with no value or preceding value",
            null,
            manager.get_absolute_value(BeatKey(0,0,0), listOf()),
        )

        manager.unset(BeatKey(0,0,1), listOf())
        assertEquals(
            "Returned wrong value on leaf with no preceding absolute value",
            -5,
            manager.get_absolute_value(BeatKey(0,0,3), listOf()),
        )
    }

    @Test
    fun test_get_channel_line_counts() {
        //TODO("test_get_channel_line_counts")
    }

    @Test
    fun test_has_preceding_absolute_event() {
        val manager = OpusManager()
        manager.new()

        val absolute_event = OpusEvent(25, 0, false)

        assertEquals(
            "False positive on has_preceding_absolute_event()",
            false,
            manager.has_preceding_absolute_event(BeatKey(0,0,0), listOf())
        )

        manager.set_event(BeatKey(0,0,0), listOf(), absolute_event)
        assertEquals(
            "False positive on has_preceding_absolute_event()",
            false,
            manager.has_preceding_absolute_event(BeatKey(0,0,0), listOf())
        )

        assertEquals(
            "False negative on has_preceding_absolute_event()",
            true,
            manager.has_preceding_absolute_event(BeatKey(0,0,1), listOf())
        )
    }
    @Test
    fun test_convert_event_to_relative() {
        val manager = OpusManager()
        manager.new()

        assertThrows(Exception::class.java) { manager.convert_event_to_relative(BeatKey(0,0,0), listOf()) }

        manager.set_event(BeatKey(0,0,0), listOf(), OpusEvent(12,0,false))

        manager.set_event(BeatKey(0,0,1), listOf(), OpusEvent(24,0, false))
        manager.convert_event_to_relative(BeatKey(0,0,1), listOf())
        assertEquals(
            "Failed to convert absolute event to relative",
            OpusEvent(12, 0, true),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )

        manager.set_event(BeatKey(0,0,1), listOf(), OpusEvent(12,0, true))
        manager.convert_event_to_relative(BeatKey(0,0,1), listOf())
        assertEquals(
            "Somehow broke an existing relative event",
            OpusEvent(+12, 0, true),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )
    }

    @Test
    fun test_convert_event_to_absolute() {
        val manager = OpusManager()
        manager.new()

        assertThrows(Exception::class.java) { manager.convert_event_to_absolute(BeatKey(0,0,0), listOf()) }

        manager.set_event(BeatKey(0,0,0), listOf(), OpusEvent(12, 0, false))

        manager.set_event(BeatKey(0,0,1), listOf(), OpusEvent(12, 0, true))

        manager.convert_event_to_absolute(BeatKey(0,0,1), listOf())
        assertEquals(
            "Failed to convert absolute_event_to_absolute",
            OpusEvent(24, 0, false),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )

        manager.set_event(BeatKey(0,0,1), listOf(), OpusEvent(12, 0, false))
        manager.convert_event_to_absolute(BeatKey(0,0,1), listOf())
        assertEquals(
            "Somehow broke an existing absolute event",
            OpusEvent(12, 0, false),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )
    }

    @Test
    fun test_set_unset() {
        val manager = OpusManager()
        manager.new()

        // set/unset leaf
        val beatkey = BeatKey(0,0,0)
        val position: List<Int> = listOf()


        manager.set_event(beatkey, position, OpusEvent(10, 0, false))
        val tree = manager.get_tree(beatkey, position)
        assertEquals(
            "Failed to set event",
            tree.is_event(),
            true
        )
        assertEquals(
            "Set event, but set it wrong",
            tree.get_event(),
            OpusEvent(10, 0, false)
        )

        manager.unset(beatkey, position)
        assertEquals(
            "Failed to unset tree",
            manager.get_tree(beatkey, position).is_event(),
            false
        )
    }

    @Test
    fun test_new_channel() {
        val manager = OpusManager()
        manager.new()
        assertEquals(2, manager.channels.size)
        manager.new_channel(lines=0)
        assertEquals(3, manager.channels.size)
        assertEquals(0, manager.channels[1].size)
    }

    @Test
    fun test_insert_remove_beat() {
        val manager = OpusManager()
        manager.new()

        var beats = manager.beat_count

        manager.insert_beat(0)
        assertEquals(beats + 1, manager.beat_count)

        manager.insert_beat(manager.beat_count)
        assertEquals(beats + 2, manager.beat_count)

        beats = manager.beat_count
        assertThrows(Exception::class.java) { manager.insert_beat(manager.beat_count + 1) }
        assertThrows(Exception::class.java) { manager.remove_beat(manager.beat_count + 1) }
    }

    @Test
    fun test_new_line() {
        val manager = OpusManager()
        manager.new()

        manager.new_line(0)
        assertEquals(manager.channels[0].size, 2)

        val line = manager.channels[0].lines[0]
        manager.new_line(0, 0)
        assertEquals("Didn't add new line to channel", manager.channels[0].size, 3)
        assertEquals("Inserted new line in wrong place", line, manager.channels[0].lines[1])

        var current_lines = manager.channels[0].size
        assertThrows(Exception::class.java) { manager.new_line(0, manager.channels[0].size + 1) }
        assertEquals("[Correctly] Threw exception, but still added channel", manager.channels[0].size, current_lines)

        // Removing
        manager.remove_line(0, 0)
        assertEquals("Didn't remove line", manager.channels[0].size, current_lines - 1)
        assertEquals("Didn't remove line from correct place", line, manager.channels[0].lines[0])

        assertThrows(Exception::class.java) { manager.remove_line(0, current_lines + 10) }
        assertEquals(
            "[Correctly] Threw exception, but still removed line.\n",
            current_lines - 1,
            manager.channels[0].size
        )
    }

    @Test
    fun test_overwrite_beat() {
        val manager = OpusManager()
        manager.new()

        val beatkey_a = BeatKey(0, 0, 0)
        val beatkey_b = BeatKey(0, 0, 1)

        manager.split_tree(beatkey_a, listOf(), 5)
        manager.overwrite_beat(beatkey_b, beatkey_a)

        assertEquals(
            "Failed to overwrite beat",
            manager.get_tree(beatkey_b, listOf()).size,
            manager.get_tree(beatkey_a, listOf()).size
        )
    }

    @Test
    fun test_replace_tree() {
        val manager = OpusManager()
        manager.new()

        val beatkey = BeatKey(0, 0, 0)
        val top_tree = OpusTree<OpusEvent>()
        top_tree.set_size(5)
        manager.replace_tree(beatkey, listOf(), top_tree)

        assertEquals(
            "Failed to replace tree",
            manager.get_tree(beatkey, listOf()).size,
            top_tree.size
        )

        var new_tree = OpusTree<OpusEvent>()
        manager.split_tree(beatkey, listOf(), 12)
        var position = listOf<Int>(0)
        var old_parent = manager.get_tree(beatkey, position).parent
        manager.replace_tree(beatkey, position, new_tree)

        assertEquals(
            "Failed to set replacement tree's parent correctly",
            manager.get_tree(beatkey, position).parent,
            old_parent
        )

        manager.replace_tree(beatkey, listOf(), new_tree)
        assertEquals(
            "Failed to remove replacement tree's parent when setting as beat tree",
            null,
            new_tree.parent
        )
    }

    @Test
    fun test_set_beat_count() {
        val manager = OpusManager()
        manager.new()
        for (i in 0 until 4) {
            manager.new_channel()
            for (j in 0 until 4) {
                manager.new_line(i)
            }
        }

        manager.set_beat_count(128)
        for (i in 0 until 4) {
            for (j in 0 until manager.channels[i].size) {
                assertEquals(
                    "Didn't resize existing channel lines correctly when setting beat count",
                    manager.channels[i].lines[j].beats.size,
                    128
                )
            }
        }
    }

    @Test
    fun test_insert_after() {
        val manager = OpusManager()
        manager.new()
        val beat_key = BeatKey(0, 0, 0)
        val beat_tree = manager.get_beat_tree(beat_key)
        beat_tree.set_size(1)
        val initial_length = beat_tree.size
        manager.insert_after(beat_key, listOf(0))
        assertEquals(beat_tree.size, initial_length + 1)

        //manager.insert_after(beat_key, listOf())
    }

    @Test
    fun test_remove() {
        val manager = OpusManager()
        manager.new()
        val beat_key = BeatKey(0, 0, 0)
        val beat_tree = manager.get_beat_tree(beat_key)
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
        val tree = manager.get_tree(beat_key, listOf(3))
        manager.remove(beat_key, listOf(2))
        assertEquals(tree, manager.get_tree(beat_key, listOf(2)))
    }

    @Test
    fun test_split_tree() {
        val manager = OpusManager()
        manager.new()
        val split_count = 5
        val beat_key = BeatKey(0, 0, 0)

        // split a beat
        manager.split_tree(beat_key, listOf(), split_count)
        var beat_tree = manager.get_beat_tree(beat_key)
        assertEquals(beat_tree.size, split_count)

        // Split an open leaf
        manager.split_tree(beat_key, listOf(split_count - 1), split_count)
        beat_tree = manager.get_beat_tree(beat_key)
        assertEquals(beat_tree.get(split_count - 1).size, split_count)

        // split an event
        val position = mutableListOf(split_count - 1, 0)

        manager.set_event(beat_key, position, OpusEvent(30,  0, false))

        manager.split_tree(beat_key, position, split_count)
        val subtree = manager.get_tree(beat_key, position)
        assertEquals(subtree.size, split_count)
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
    fun test_set_duration() {
        val manager = OpusManager()
        manager.new()
        val beat_key = BeatKey(0, 0, 0)
        val event = OpusEvent(20, 0, false, 1)
        manager.set_event(beat_key, listOf(), event)

        val new_duration = 2
        manager.set_duration(beat_key, listOf(), new_duration)
        assertEquals(
            "Failed to set event duration",
            new_duration,
            manager.get_beat_tree(beat_key).get_event()!!.duration
        )
    }
}
