package com.qfs.pagan

import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class OpusLayerOverlapControlUnitTest {
    @Test
    fun test_get_leaf_offset_and_width() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.split_tree(BeatKey(0,0,0), listOf(0), 3)
        manager.split_tree(BeatKey(0,0,0), listOf(1), 2)


        assertEquals(
            Pair(Rational(0, 6), 6),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(0, 0))
        )

        assertEquals(
            Pair(Rational(1, 6), 6),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(0, 1))
        )


        assertEquals(
            Pair(Rational(2, 6), 6),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(0, 2))
        )

        assertEquals(
            Pair(Rational(1, 2), 4),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(1, 0))
        )
        assertEquals(
            Pair(Rational(3, 4), 4),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(1, 1))
        )
    }


    fun test_set_event() {
        val manager = OpusManager()
        manager._project_change_new()

        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.split_tree(BeatKey(0,0,1), listOf(), 2)
        manager.set_event(BeatKey(0,0,0), listOf(0), AbsoluteNoteEvent(10, 3))

        assertThrows(OpusLayerBase.BlockedTreeException::class.java) {
            manager.set_event(BeatKey(0,0,1), listOf(0), AbsoluteNoteEvent(10))
        }

        manager.unset(BeatKey(0,0,0), listOf(0))
        try {
            manager.set_event(BeatKey(0,0,1), listOf(0), AbsoluteNoteEvent(10))
            assertTrue(true)
        } catch (e: OpusLayerBase.BlockedTreeException) {
            assertFalse(true)
        }
    }
    
    fun test_replace_tree() {
        val manager = OpusManager()
        manager._project_change_new()

        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.split_tree(BeatKey(0,0,1), listOf(), 2)
        manager.set_event(BeatKey(0,0,0), listOf(0), AbsoluteNoteEvent(10, 3))

        assertThrows(OpusLayerBase.BlockedTreeException::class.java) {
            val tree = OpusTree<InstrumentEvent>()
            tree.set_event(AbsoluteNoteEvent(10))
            manager.replace_tree(BeatKey(0,0,1), listOf(0), tree)
        }

        manager.unset(BeatKey(0,0,0), listOf(0))
        assertTrue(try {
            val tree = OpusTree<InstrumentEvent>()
            tree.set_event(AbsoluteNoteEvent(10))
            manager.replace_tree(BeatKey(0,0,1), listOf(0), tree)
            true
        } catch (e: OpusLayerBase.BlockedTreeException) {
            false
        })
    }

    fun test_remove_beat() {
        val manager = OpusManager()
        manager._project_change_new()

        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.set_event(BeatKey(0,0,0), listOf(0), AbsoluteNoteEvent(10, 3))
        manager.set_event(BeatKey(0,0,2), listOf(), AbsoluteNoteEvent(10))

        assertThrows(OpusLayerBase.BlockedTreeException::class.java) {
            manager.remove_beat(1)
        }
    }

    fun test_remove() {
        val manager = OpusManager()
        manager._project_change_new()

        manager.split_tree(BeatKey(0,0,0), listOf(), 3)
        manager.set_event(BeatKey(0,0,0), listOf(0), AbsoluteNoteEvent(10, 2))
        manager.set_event(BeatKey(0,0,0), listOf(2), AbsoluteNoteEvent(10))

        assertThrows(OpusLayerBase.BlockedTreeException::class.java) {
            manager.remove(BeatKey(0,0,0), listOf(1))
        }
    }
}
