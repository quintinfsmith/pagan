package com.qfs.pagan

import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.qfs.pagan.opusmanager.OpusLayerOverlapControl as OpusManager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class OpusLayerOverlapControlUnitTest {
    @Test
    fun test_get_leaf_offset_and_width() {
        val manager = OpusManager()
        manager.new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.split_tree(BeatKey(0,0,0), listOf(0), 3)
        manager.split_tree(BeatKey(0,0,0), listOf(1), 2)


        assertEquals(
            Rational(0, 6),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(0, 0)).first
        )

        assertEquals(
            Rational(1, 6),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(0, 1)).first
        )


        assertEquals(
            Rational(2, 6),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(0, 2)).first
        )

        assertEquals(
            Rational(1, 2),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(1, 0)).first
        )
        assertEquals(
            Rational(3, 4),
            manager.get_leaf_offset_and_width(BeatKey(0,0,0), listOf(1, 1)).first
        )

    }
}
