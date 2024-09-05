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
import com.qfs.pagan.opusmanager.OpusLayerLinks as OpusManager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class OpusLayerLinksUnitTest {
    private fun batch_link_test(manager: OpusManager, main_key: BeatKey, callback: (OpusTree<out InstrumentEvent>) -> Unit) {
        for (linked_key in manager.get_all_linked(main_key)) {
            if (linked_key == main_key) {
                continue
            }

            callback(manager.get_tree(linked_key))
        }
    }

    private fun setup_linked_manager(): Pair<OpusManager, BeatKey> {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        return Pair(manager, BeatKey(0,0,0))
    }

    @Test
    fun test_unlink_beat() {
        val (manager, main_key) = this.setup_linked_manager()
        manager.unlink_beat(main_key)
        assertEquals(
            "Failed to unlink beat",
            1,
            manager.get_all_linked(main_key).size
        )
    }

    @Test
    fun test_link_beats() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)
        val first_key = BeatKey(0, 0, 0)
        val second_key = BeatKey(0, 0, 1)
        val third_key = BeatKey(0, 1, 0)
        val fourth_key = BeatKey(0, 1, 1)
        //-----------
        assertThrows(OpusLayerLinks.SelfLinkError::class.java) {
            manager.link_beats(first_key, first_key)
        }
        assertThrows(OpusLayerBase.MixedInstrumentException::class.java) {
            manager.link_beats(first_key, BeatKey(1,0,0))
        }
        //-----------
        manager.link_beats(first_key, second_key)
        assertTrue(
            "Failed to link 2 beats",
            manager.get_all_linked(first_key).contains(second_key)
        )
        //-----------
        manager.link_beats(first_key, third_key)
        assertTrue(
            "Failed to link 3rd beat into pool",
            manager.get_all_linked(first_key).contains(third_key)
        )

        //............
        manager.unlink_beat(third_key)
        manager.link_beats(third_key, fourth_key)
        //-----------
        manager.link_beats(first_key, third_key)
        assertTrue(
            "Failed to merge pools",
            manager.get_all_linked(first_key).contains(fourth_key)
        )
        //.............
        manager.unlink_beat(third_key)
        manager.link_beats(third_key, first_key)
        assertTrue(manager.get_all_linked(first_key).contains(third_key))

    }

    //@Test
    //fun test_change_line_channel() {
    //    val manager = OpusManager()
    //    manager.new()
    //    manager.new_channel()
    //    manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
    //    manager.move_line(0,0,1,0)
    //    assertEquals(
    //        "Failed to move links with line",
    //        2,
    //        manager.get_all_linked(BeatKey(1,0,0)).size
    //    )
    //}

    //@Test
    //fun test_insert_beat() {
    //    val manager = OpusManager()
    //    manager.new()
    //    manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,2))
    //    manager.insert_beat(1)
    //    assertEquals(
    //        "Failed to move links when inserting beat",
    //        2,
    //        manager.get_all_linked(BeatKey(0,0,3)).size
    //    )
    //}

    @Test
    fun test_insert_beats() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,2))
        manager.insert_beat(1)
        assertEquals(
            "Failed to move links when inserting beat",
            2,
            manager.get_all_linked(BeatKey(0,0,3)).size
        )
        manager.insert_beats(1, 5)
        assertEquals(
            "Failed to move links when inserting beat",
            2,
            manager.get_all_linked(BeatKey(0,0,8)).size
        )
    }

    @Test
    fun test_remove_beat() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,2))
        manager.remove_beat(1)
        assertEquals(
            "Failed to move links when removing beat",
            2,
            manager.get_all_linked(BeatKey(0,0,1)).size
        )

        manager.remove_beat(0)
        assertEquals(
            1,
            manager.get_all_linked(BeatKey(0,0,0)).size
        )

    }
    @Test
    fun test_new_line() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        manager.new_line(0,0)
        assertEquals(
            "Failed to adjust links on new line",
            2,
            manager.get_all_linked(BeatKey(0,1,0)).size
        )

        manager.remove_line(0,0)
        manager.new_line(0,1)

        assertEquals(
            "Failed to adjust links on new line",
            2,
            manager.get_all_linked(BeatKey(0,0,0)).size
        )


        val test_line = manager.remove_line(0,1)

        manager.insert_line(0,0,test_line)
        assertEquals(
            "Failed to adjust links on insert line",
            2,
            manager.get_all_linked(BeatKey(0,1,0)).size
        )
        val test_line_2 = manager.remove_line(0,0)
        manager.insert_line(0,1,test_line_2)

        assertEquals(
            "Failed to adjust links on insert line",
            2,
            manager.get_all_linked(BeatKey(0,0,0)).size
        )
    }

    @Test
    fun test_new_channel() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        manager.new_channel(0)
        assertEquals(
            "Failed to adjust links on new channel",
            2,
            manager.get_all_linked(BeatKey(1,0,0)).size
        )
    }

    @Test
    fun test_remove_channel() {
        val manager = OpusManager()
        manager.new()
        manager.new_channel()
        manager.new_channel()
        manager.link_beats(BeatKey(2,0,0), BeatKey(2,0,1))
        manager.remove_channel(0)
        assertEquals(
            "Failed to adjust links when removing channel",
            2,
            manager.get_all_linked(BeatKey(1,0,0)).size
        )
    }

    @Test
    fun test_remove_line() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)
        manager.new_line(0)
        manager.link_beats(BeatKey(0,2,0), BeatKey(0,2,1))
        manager.remove_line(0, 0)
        assertEquals(
            "Failed to adjust links when removing Line",
            2,
            manager.get_all_linked(BeatKey(0,1,0)).size
        )
    }

    @Test
    fun test_replace_beat() {
        val (manager, main_key) = this.setup_linked_manager()
        val test_tree = OpusTree<TunedInstrumentEvent>()
        test_tree.set_size(12)
        manager.replace_tree(main_key, null, test_tree)

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to replace tree on all linked beats",
                test_tree.size,
                it.size
            )
        }
    }

    @Test
    fun test_replace_tree() {
        val (manager, main_key) = this.setup_linked_manager()
        val test_tree = OpusTree<TunedInstrumentEvent>()
        test_tree.set_size(12)
        manager.replace_tree(main_key, listOf(), test_tree)

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to replace tree on all linked beats",
                test_tree.size,
                it.size
            )
        }
    }

    @Test
    fun test_insert_after() {
        val (manager, main_key) = this.setup_linked_manager()

        manager.split_tree(main_key, listOf(), 2)
        manager.insert_after(main_key, listOf(1))

        this.batch_link_test(manager, main_key) {
            assertEquals(3, it.size)
        }
    }
    @Test
    fun test_insert() {
        val (manager, main_key) = this.setup_linked_manager()

        manager.split_tree(main_key, listOf(), 2)
        manager.insert(main_key, listOf(1))

        this.batch_link_test(manager, main_key) {
            assertEquals(3, it.size)
        }
    }

    @Test
    fun test_remove() {
        val (manager, main_key) = this.setup_linked_manager()

        val new_size = 3
        manager.split_tree(main_key, listOf(), new_size)
        manager.remove(main_key, listOf(1))

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to remove subtree on linked tree",
                new_size - 1,
                it.size
            )
        }
    }

    @Test
    fun test_split_tree() {
        val (manager, main_key) = this.setup_linked_manager()
        val new_size = 3
        manager.split_tree(main_key, listOf(), new_size)

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to split all linked trees",
                new_size,
                it.size
            )
        }

    }

    @Test
    fun test_set_duration() {
        val (manager, main_key) = this.setup_linked_manager()

        val new_duration = 3
        manager.split_tree(main_key, listOf(), new_duration)
        manager.set_event(main_key, listOf(0), AbsoluteNoteEvent(20))
        manager.set_duration(main_key, listOf(0), new_duration)

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to set duration on all linked beats",
                new_duration,
                it[0].get_event()!!.duration
            )
        }
    }

    @Test
    fun test_set_percussion_event() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(1,0,0), BeatKey(1,0,1))
        manager.set_percussion_event(BeatKey(1,0,0), listOf())
        this.batch_link_test(manager, BeatKey(1,0,0)) {
            assertEquals(
                "Failed to set percussion event on linked beat",
                true,
                it.is_event()
            )
        }
    }
    @Test
    fun test_set_event() {
        val (manager, main_key) = this.setup_linked_manager()
        val event = AbsoluteNoteEvent(20)
        manager.set_event(main_key, listOf(), event)

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to set event on all linked beats",
                true,
                it.is_event()
            )
        }
    }
    @Test
    fun test_unset() {
        val (manager, main_key) = this.setup_linked_manager()
        val event = AbsoluteNoteEvent(20)
        manager.set_event(main_key, listOf(), event)
        manager.unset(main_key, listOf())

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to unset event on all linked beats",
                false,
                it.is_event()
            )
        }
    }

    @Test
    fun test_link_beat_range() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)
        val first_key = BeatKey(0,0,0)
        val second_key = BeatKey(0,1,1)


        assertThrows(OpusLayerLinks.LinkRangeOverlap::class.java) {
            manager.link_beat_range(BeatKey(0,0,1), first_key, second_key)
        }
        assertThrows(OpusLayerBase.RangeOverflow::class.java) {
            manager.link_beat_range(BeatKey(0,0,3), first_key, second_key)
        }

        manager.link_beat_range(BeatKey(0,0,2), first_key, second_key)

        val d_keys = manager.get_beatkeys_in_range(first_key, second_key)
        val i_keys = manager._get_beatkeys_from_range(BeatKey(0,0,2), first_key, second_key)
        for (i in d_keys.indices) {
            manager.set_event(d_keys[i], listOf(), AbsoluteNoteEvent(i))
            assertEquals(
                manager.get_tree(d_keys[i]),
                manager.get_tree(i_keys[i])
            )
        }

        manager.unlink_range(first_key, second_key)
        for (i in d_keys.indices) {
            manager.unset(d_keys[i], listOf())
            assertTrue(manager.get_tree(i_keys[i]).is_event())
        }
        

    }

    @Test
    fun test_link_row() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)

        assertThrows(OpusLayerLinks.BadRowLink::class.java) {
            manager.link_row(0, 0, BeatKey(1,0,0))
        }
        assertThrows(OpusLayerLinks.BadRowLink::class.java) {
            manager.link_row(0, 0, BeatKey(0,1,0))
        }
        manager.link_row(0, 0, BeatKey(0,0,0))

        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(24))

        for (i in 1 until manager.beat_count) {
            assertEquals(
                manager.get_tree(BeatKey(0,0,0)),
                manager.get_tree(BeatKey(0,0,i))
            )
        }
    }

    @Test
    fun test_link_beat_range_horizontally() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)
        manager.set_beat_count(12)
        val first_key = BeatKey(0,0,0)
        val second_key = BeatKey(0,1,1)

        manager.link_beat_range_horizontally(0,0, first_key, second_key)

        val d_keys = manager.get_beatkeys_in_range(first_key, second_key)
        for (i in d_keys.indices) {
            manager.set_event(d_keys[i], listOf(), AbsoluteNoteEvent(i))
            for (j in 0 until 5) {
                assertEquals(
                    manager.get_tree(d_keys[i]),
                    manager.get_tree(
                        BeatKey(
                            d_keys[i].channel,
                            d_keys[i].line_offset,
                            d_keys[i].beat + ((j + 1) * 2)
                        )
                    )
                )
            }

        }
    }


    @Test
    fun test_swap_lines() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)
        manager.new_line(0)
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,1,1))

        manager.swap_lines(0, 0, 0, 1)

        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(24))
        assertEquals(
            manager.get_tree(BeatKey(0,0,1)),
            manager.get_tree(BeatKey(0,1,0))
        )

        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(22))
        assertFalse(manager.get_tree(BeatKey(0,1,1)).is_event())


        manager.swap_lines(0, 1, 0, 2)
        assertEquals(
            manager.get_tree(BeatKey(0,0,1)),
            manager.get_tree(BeatKey(0,2,0))
        )
    }

    @Test
    fun test_move_leaf() {
        val manager = OpusManager()
        manager.new()

        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(12))

        // --------------
        manager.move_leaf(BeatKey(0,0,0), listOf(), BeatKey(0,0,2), listOf())
        manager.set_event(BeatKey(0,0, 2), listOf(), AbsoluteNoteEvent(13))

        assertEquals(
            manager.get_tree(BeatKey(0,0,2)),
            manager.get_tree(BeatKey(0,0,1))
        )
        // --------------
        manager.move_leaf(BeatKey(0,0,2), listOf(), BeatKey(0,0,1), listOf())
        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(13))
        for (i in 0 until 4) {
            if (i == 1) {
                continue
            }
            assertFalse(manager.get_tree(BeatKey(0,0,i)).is_event())
        }
    }

    @Test
    fun test_move_beat_range() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)

        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(12))

        // --------------
        manager.move_beat_range(BeatKey(0,0,2), BeatKey(0,0,0), BeatKey(0,0,1))
        manager.set_event(BeatKey(0,0, 2), listOf(), AbsoluteNoteEvent(13))

        assertEquals(
            manager.get_tree(BeatKey(0,0,2)),
            manager.get_tree(BeatKey(0,0,3))
        )
        // --------------
        manager.move_beat_range(BeatKey(0,0,0), BeatKey(0,0,1), BeatKey(0,0,2))
        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(14))
        assertEquals(
            manager.get_tree(BeatKey(0,0,1)),
            manager.get_tree(BeatKey(0,0,3))
        )
        // --------------
        manager.move_beat_range(BeatKey(0,0,0), BeatKey(0,0,1), BeatKey(0,0,2))
        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(15))
        assertNotEquals(
            manager.get_tree(BeatKey(0,0,1)),
            manager.get_tree(BeatKey(0,0,3))
        )
    }

    @Test
    fun test_clear_links() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)

        for (i in 1 until 4) {
            manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,i))
        }

        manager.clear_link_pool(BeatKey(0,0,0))
        for (i in 0 until 4) {
            assertEquals(
                1,
                manager.get_all_linked(BeatKey(0,0,i)).size
            )
        }

        //-------------------------------
        for (i in 0 until 4) {
            manager.link_beats(BeatKey(0,0,i), BeatKey(0,0,i + 4))
        }

        manager.clear_link_pools_by_range(BeatKey(0,0,0), BeatKey(0,0,3))
        for (i in 0 until 4) {
            assertEquals(
                1,
                manager.get_all_linked(BeatKey(0,0,i + 4)).size
            )
        }

    }

}
