package com.qfs.pagan

import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusTreeArray
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpusTreeArrayUnitTest {
    @Test
    fun test_init() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        assertEquals(
            1,
            array.beat_count()
        )

        array.split_tree(0, listOf(), 2, false)
        assertEquals(
            2,
            array.get_tree(0, listOf()).size
        )
    }

    @Test
    fun test_set_event() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)

        val event = AbsoluteNoteEvent(20, 1)
        array.set_event(0, listOf(), event)
        assertEquals(
            array.get_tree(0, listOf()).get_event(),
            event
        )

        event.duration = 2
        array.set_event(0, listOf(), event)

        val blocked_event = AbsoluteNoteEvent(11, 1)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(1, listOf(), blocked_event)
        }

        array.split_tree(0, listOf(), 2, false)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(0, listOf(1), blocked_event)
        }

        event.duration = 3
        array.set_event(0, listOf(0), event)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(1, listOf(), blocked_event)
        }

        array.unset(0, listOf(0))
        array.set_event(1, listOf(), blocked_event)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(0, listOf(0), event)
        }

        array.split_tree(1, listOf(), 2, false)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(0, listOf(0), event)
        }
    }

    @Test
    fun test_split_tree() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)

        array.split_tree(0, listOf(), 2, false)
        array.split_tree(0, listOf(0), 4, false)

        assertEquals(
            2,
            array.get_tree(0, listOf()).size
        )

        assertEquals(
            4,
            array.get_tree(0, listOf(0)).size
        )

        val test_event = AbsoluteNoteEvent(2, 1)
        array.set_event(1, listOf(), test_event)
        array.split_tree(1, listOf(), 4, true)
        assertEquals(
            test_event,
            array.get_tree(1, listOf(3)).get_event()
        )

        // Trivial call
        array.split_tree(2, listOf(), 1, false)

        assertThrows(OpusTreeArray.SplittingBranchException::class.java) {
            array.split_tree(1, listOf(), 2, true)
        }

    }

    @Test
    fun test_insert() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)

        val test_event = AbsoluteNoteEvent(16, 1)

        assertThrows(OpusLayerBase.BadInsertPosition::class.java) {
            array.insert(0, listOf())
        }
        array.split_tree(0, listOf(), 2, false)
        array.set_event(0, listOf(0), test_event)

        array.insert(0, listOf(0))
        assertEquals(
            array.get_tree(0, listOf(1)).get_event(),
            test_event
        )
        assertEquals(
            3,
            array.get_tree(0, listOf()).size
        )

        array.unset(0, listOf(1))
        array.remove_beat(0)

        val test_event_a = AbsoluteNoteEvent(1, 3)
        val test_event_b = AbsoluteNoteEvent(2, 1)
        array.split_tree(0, listOf(), 3, false)
        array.split_tree(1, listOf(), 3, false)
        array.set_event(0, listOf(2), test_event_a)
        array.set_event(1, listOf(2), test_event_b)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            // The new tree should move event_b from 1/3 to 1/4 and event_a takes up 2/3 of the tree
            try {
                array.insert(1, listOf(3))
            } catch (e: OpusTreeArray.BlockedTreeException) {
                // TODO: This isn't a priority right now but it should be dealt with for ui's sake
                // assertEquals(
                //     Pair(e.blocker_beat, e.blocker_position),
                //     Pair(0, listOf(2))
                // )
                throw e
            }
        }
    }

    @Test
    fun test_replace_tree() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        val tree = OpusTree<OpusEvent>()
        tree.set_size(3)

        array.replace_tree(0, listOf(), tree)

        assertEquals(
            array.get_tree(0, listOf()),
            tree
        )

        val tree_b = OpusTree<OpusEvent>()
        tree_b.set_size(2)
        array.replace_tree(0, listOf(1), tree_b)

        assertEquals(
            array.get_tree(0, listOf(1)),
            tree_b
        )
    }

    @Test
    fun test_insert_remove_beat() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)
        array.insert_beat(0)
        array.split_tree(0, listOf(), 2, false)

        array.set_event(0, listOf(1), AbsoluteNoteEvent(2, 2))
        array.set_event(2, listOf(), AbsoluteNoteEvent(3, 2))

        array.insert_beat(1)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(1, listOf(), AbsoluteNoteEvent(2, 1))
        }

        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(4, listOf(), AbsoluteNoteEvent(2, 1))
        }

        array.remove_beat(1)
        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(1, listOf(), AbsoluteNoteEvent(2, 1))
        }

        assertThrows(OpusTreeArray.BlockedTreeException::class.java) {
            array.set_event(3, listOf(), AbsoluteNoteEvent(2, 1))
        }

        array.remove_beat(2)
        assertEquals(
            1,
            array._cache_blocked_tree_map.size
        )
    }


}