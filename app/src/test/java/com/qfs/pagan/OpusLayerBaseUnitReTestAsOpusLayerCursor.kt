/*
* DO NOT MODIFY THE CONTENTS OF THIS FILE. IT WAS GENERATED IN /scripts/build_other_layer_tests.py
*/
package com.qfs.pagan


import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BadBeatKey
import com.qfs.pagan.structure.opusmanager.base.BadInsertPosition
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.EventlessTreeException
import com.qfs.pagan.structure.opusmanager.base.IncompatibleChannelException
import com.qfs.pagan.structure.opusmanager.base.InvalidChannel
import com.qfs.pagan.structure.opusmanager.base.NonEventConversion
import com.qfs.pagan.structure.opusmanager.base.NoteOutOfRange
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.RangeOverflow
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase.Companion.get_ordered_beat_key_pair
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.RemovingLastBeatException
import com.qfs.pagan.structure.opusmanager.base.RemovingRootException
import com.qfs.pagan.structure.opusmanager.base.TunedInstrumentEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.qfs.pagan.structure.opusmanager.cursor.OpusLayerCursor as OpusManager

class OpusLayerBaseUnitReTestAsOpusLayerCursor {
    @Test
    fun test_new() {
        val manager = OpusManager()
        manager._project_change_new()
        assertNotEquals(manager.length, 0)
    }

    @Test
    fun test_set_channel_instrument() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.channel_set_instrument(0, Pair(5,2))
        assertEquals(
            "Failed to set channel instrument",
            Pair(5,2),
            manager.get_channel_instrument(0)
        )
    }

   // @Test
   // fun test_get_percussion_instrument() {
   //     val manager = OpusManager()
   //     manager.new()
   //     assertEquals(
   //         "Percussion instrument wasn't correctly defaulted",
   //         OpusManager.DEFAULT_PERCUSSION,
   //         manager.get_percussion_instrument(0)
   //     )

   //     manager.set_percussion_channel(0)
   //     manager.set_percussion_instrument(0, OpusManager.DEFAULT_PERCUSSION + 1)


   //     assertEquals(
   //         "Percussion instrument wasn't set",
   //         OpusManager.DEFAULT_PERCUSSION + 1,
   //         manager.get_percussion_instrument(0)
   //     )
   // }

    @Test
    fun test_get_beat_tree() {
        val manager = OpusManager()
        manager._project_change_new()
        val beatkey = BeatKey(0,0,0)
        manager.split_tree(beatkey, listOf(), 12)

        val tree = manager.get_tree(beatkey)
        assertEquals(
            "Got wrong beat tree",
            12,
            tree.size
        )

        assertThrows(BadBeatKey::class.java) {
            manager.get_tree(BeatKey(0,3,1))
        }
        assertThrows(BadBeatKey::class.java) {
            manager.get_tree(BeatKey(2,0,0))
        }

    }

    @Test
    fun test_get_proceeding_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val first_beat_key = BeatKey(0,0,1)

        manager.split_tree(first_beat_key, listOf(), 4)
        manager.set_event(first_beat_key, listOf(3), AbsoluteNoteEvent(0))
        manager.set_event(first_beat_key, listOf(2), AbsoluteNoteEvent(1))
        val expected_leaf = manager.get_tree(first_beat_key, listOf(3))
        assertEquals(
            "get_proceding_leaf failed",
            expected_leaf,
            manager.get_proceeding_leaf(first_beat_key, listOf(2))
        )
        assertEquals(
            "get_proceding_leaf should be null",
            null,
            manager.get_proceeding_leaf(BeatKey(0,0,3), listOf())
        )
    }

    @Test
    fun test_get_global_ctl_proceding_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo

        manager.controller_global_split_tree(type, 1, listOf(), 4)
        manager.controller_global_set_event(type, 1, listOf(3), OpusTempoEvent(24f))
        manager.controller_global_set_event(type, 1, listOf(2), OpusTempoEvent(120f))
        assertEquals(
            Pair(1, listOf(3)),
            manager.get_global_ctl_proceeding_leaf_position(type, 1, listOf(2))
        )
        assertEquals(
            null,
            manager.get_global_ctl_proceeding_leaf_position(type, 3, listOf())
        )
    }
    @Test
    fun test_get_channel_ctl_proceding_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume

        manager.controller_channel_split_tree(type, 0, 1, listOf(), 4)
        manager.controller_channel_set_event(type, 0, 1, listOf(3), OpusVolumeEvent(0f))
        manager.controller_channel_set_event(type, 0, 1, listOf(2), OpusVolumeEvent(1f))
        assertEquals(
            Pair(1, listOf(3)),
            manager.get_channel_ctl_proceeding_leaf_position(type, 0, 1, listOf(2))
        )
        assertEquals(
            null,
            manager.get_channel_ctl_proceeding_leaf_position(type, 0, 3, listOf())
        )
    }
    @Test
    fun test_get_line_ctl_proceding_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val first_beat_key = BeatKey(0,0,1)

        manager.controller_line_split_tree(type, first_beat_key, listOf(), 4)
        manager.controller_line_set_event(type, first_beat_key, listOf(3), OpusVolumeEvent(0f))
        manager.controller_line_set_event(type, first_beat_key, listOf(2), OpusVolumeEvent(1f))
        assertEquals(
            Pair(1, listOf(3)),
            manager.get_line_ctl_proceeding_leaf_position(type, first_beat_key, listOf(2))
        )
        assertEquals(
            null,
            manager.get_line_ctl_proceeding_leaf_position(type, BeatKey(0,0,3), listOf())
        )
    }

    @Test
    fun test_get_preceding_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val first_beat_key = BeatKey(0,0,1)

        manager.split_tree(first_beat_key, listOf(), 4)
        manager.set_event(first_beat_key, listOf(3), AbsoluteNoteEvent(0))
        manager.set_event(first_beat_key, listOf(2), AbsoluteNoteEvent(1))
        val expected_leaf = manager.get_tree(first_beat_key, listOf(2))
        assertEquals(
            "get_preceding_leaf failed",
            expected_leaf,
            manager.get_preceding_leaf(first_beat_key, listOf(3))
        )
        assertEquals(
            "get_preceding_leaf should be null",
            null,
            manager.get_preceding_leaf(BeatKey(0,0,0), listOf())
        )
    }

    @Test
    fun test_get_proceeding_leaf_position() {
        val manager = OpusManager()
        manager._project_change_new()

        val first = BeatKey(0,0,1)

        manager.split_tree(first, listOf(), 2)
        assertEquals(
            "Failed to get proceding leaf when it was immediately next to it",
            Pair(first, listOf(1)),
            manager.get_proceeding_leaf_position(first, listOf(0))
        )

        manager.split_tree(first, listOf(1), 2)
        assertEquals(
            "Failed to get proceding leaf when it was a niece",
            Pair(first, listOf(1, 0)),
            manager.get_proceeding_leaf_position(first, listOf(0))
        )

        manager.split_tree(first, listOf(0), 2)
        assertEquals(
            "Failed to get proceding leaf when it was a cousin",
            Pair(first, listOf(1, 0)),
            manager.get_proceeding_leaf_position(first, listOf(0,1))
        )

        manager.split_tree(first, listOf(0, 1), 2)

        assertEquals(
            "Failed to return null when looking for proceding leaf after last position",
            null,
            manager.get_proceeding_leaf_position(BeatKey(0,0, manager.length - 1),listOf())
        )

        assertEquals(
            "Failed to get proceding leaf across beats",
            Pair(first, listOf(0,0)),
            manager.get_proceeding_leaf_position(BeatKey(0,0,0), listOf())
        )
    }
    @Test
    fun test_get_preceding_leaf_position() {
        val manager = OpusManager()
        manager._project_change_new()

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
        manager._project_change_new()

        val first_event = AbsoluteNoteEvent(25)
        val second_event = RelativeNoteEvent(1)
        val third_event = RelativeNoteEvent(-6)

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
            first_event.note + second_event.offset,
            manager.get_absolute_value(BeatKey(0,0,2), listOf()),
        )

        assertEquals(
            "Failed to get correct relative value on 2nd relative event",
            first_event.note + second_event.offset + third_event.offset,
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
        manager._project_change_new()

        val absolute_event = AbsoluteNoteEvent(25)

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
        manager._project_change_new()

        assertThrows(Exception::class.java) { manager.convert_event_to_relative(BeatKey(0,0,0), listOf()) }

        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(12))

        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(24))
        manager.convert_event_to_relative(BeatKey(0,0,1), listOf())
        assertEquals(
            "Failed to convert absolute event to relative",
            RelativeNoteEvent(12),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )

        manager.set_event(BeatKey(0,0,1), listOf(), RelativeNoteEvent(12))
        manager.convert_event_to_relative(BeatKey(0,0,1), listOf())
        assertEquals(
            "Somehow broke an existing relative event",
            RelativeNoteEvent(12),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )
    }

    @Test
    fun test_convert_event_to_absolute() {
        val manager = OpusManager()
        manager._project_change_new()

        assertThrows(NonEventConversion::class.java) {
            manager.convert_event_to_absolute(BeatKey(0,0,0), listOf())
        }

        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(12))

        manager.set_event(BeatKey(0,0,1), listOf(), RelativeNoteEvent(12))

        manager.convert_event_to_absolute(BeatKey(0,0,1), listOf())
        assertEquals(
            "Failed to convert absolute_event_to_absolute",
            AbsoluteNoteEvent(24),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )

        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(12))
        manager.convert_event_to_absolute(BeatKey(0,0,1), listOf())
        assertEquals(
            "Somehow broke an existing absolute event",
            AbsoluteNoteEvent(12),
            manager.get_tree(BeatKey(0,0,1), listOf()).get_event()!!
        )


        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(0))
        manager.set_event(BeatKey(0,0,1), listOf(), RelativeNoteEvent(-12))
        //assertThrows(NoteOutOfRange::class.java) {
        //    manager.convert_event_to_absolute(BeatKey(0,0,1), listOf())
        //}
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(94))
        manager.set_event(BeatKey(0,0,1), listOf(), RelativeNoteEvent(3))

        //assertThrows(NoteOutOfRange::class.java) {
        //    manager.convert_event_to_absolute(BeatKey(0,0,1), listOf())
        //}
    }

    @Test
    fun test_set_unset() {
        val manager = OpusManager()
        manager._project_change_new()

        // set/unset leaf
        val beatkey = BeatKey(0,0,0)
        val position: List<Int> = listOf()

        manager.set_event(beatkey, position, AbsoluteNoteEvent(10))
        val tree = manager.get_tree(beatkey, position)
        assertEquals(
            "Failed to set event",
            tree.has_event(),
            true
        )
        assertEquals(
            "Set event, but set it wrong",
            tree.get_event(),
            AbsoluteNoteEvent(10)
        )

        manager.unset(beatkey, position)
        assertEquals(
            "Failed to unset tree",
            manager.get_tree(beatkey, position).has_event(),
            false
        )
    }

    @Test
    fun test_new_channel() {
        val manager = OpusManager()
        manager._project_change_new()
        assertEquals(2, manager.get_channel_count())
        manager.new_channel(1, lines=0)
        assertEquals(3, manager.get_channel_count())
        assertEquals(0, manager.channels[1].size)
    }

    @Test
    fun test_insert_remove_beat() {
        val manager = OpusManager()
        manager._project_change_new()

        val beats = manager.length

        manager.insert_beat(0)
        assertEquals(beats + 1, manager.length)

        manager.insert_beat(manager.length)
        assertEquals(beats + 2, manager.length)

        assertThrows(RemovingRootException::class.java) {
            manager.remove(BeatKey(0,0,0), listOf())
        }

        assertThrows(IndexOutOfBoundsException::class.java) {
            manager.insert_beat(manager.length + 1)
        }

        assertThrows(IndexOutOfBoundsException::class.java) {
            manager.remove_beat(manager.length + 1)
        }

        while (manager.length > 1) {
            manager.remove_beat(0)
        }

        assertThrows(RemovingLastBeatException::class.java) {
            manager.remove_beat(0)
        }


    }

    @Test
    fun test_new_line() {
        val manager = OpusManager()
        manager._project_change_new()

        manager.new_line(0)
        assertEquals(manager.channels[0].size, 2)

        val line = manager.channels[0].lines[0]
        manager.new_line(0, 0)
        assertEquals("Didn't add new line to channel", manager.channels[0].size, 3)
        assertEquals("Inserted new line in wrong place", line, manager.channels[0].lines[1])

        val current_lines = manager.channels[0].size
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
        manager._project_change_new()

        val beatkey_a = BeatKey(0, 0, 0)
        val beatkey_b = BeatKey(0, 0, 1)

        manager.split_tree(beatkey_a, listOf(), 5)
        manager.replace_tree(beatkey_b, null, manager.get_tree(beatkey_a))

        assertEquals(
            "Failed to overwrite beat",
            manager.get_tree(beatkey_b, listOf()).size,
            manager.get_tree(beatkey_a, listOf()).size
        )
    }

    @Test
    fun test_replace_tree() {
        val manager = OpusManager()
        manager._project_change_new()

        val beatkey = BeatKey(0, 0, 0)
        val top_tree = ReducibleTree<TunedInstrumentEvent>()
        top_tree.set_size(5)
        manager.replace_tree(beatkey, listOf(), top_tree)

        assertEquals(
            "Failed to replace tree",
            manager.get_tree(beatkey, listOf()).size,
            top_tree.size
        )

        val new_tree = ReducibleTree<TunedInstrumentEvent>()
        manager.split_tree(beatkey, listOf(), 12)
        val position = listOf(0)
        val old_parent = manager.get_tree(beatkey, position).parent
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
        manager._project_change_new()
        for (i in 0 until 4) {
            manager.new_channel(manager.channels.size - 1)
            for (j in 0 until 4) {
                manager.new_line(manager.channels.size - 1, i)
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
        manager._project_change_new()
        val beat_key = BeatKey(0, 0, 0)
        val beat_tree = manager.get_tree(beat_key)
        beat_tree.set_size(1)
        val initial_length = beat_tree.size
        manager.insert_after(beat_key, listOf(0))
        assertEquals(beat_tree.size, initial_length + 1)

        assertThrows(BadInsertPosition::class.java) {
            manager.insert_after(BeatKey(0,0,0), listOf())
        }
    }

    @Test
    fun test_controller_line_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume

        val beat_key = BeatKey(0, 0, 0)
        val beat_tree = manager.get_line_ctl_tree<OpusVolumeEvent>(type, beat_key)
        beat_tree.set_size(1)
        val initial_length = beat_tree.size
        manager.controller_line_insert_after(type, beat_key, listOf(0))

        assertEquals(beat_tree.size, initial_length + 1)

        assertThrows(BadInsertPosition::class.java) {
            manager.controller_line_insert_after(type, BeatKey(0,0,0), listOf())
        }
    }

    @Test
    fun test_controller_channel_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume

        val beat_tree = manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, 0)
        beat_tree.set_size(1)
        val initial_length = beat_tree.size
        manager.controller_channel_insert_after(type, 0, 0, listOf(0))

        assertEquals(beat_tree.size, initial_length + 1)

        assertThrows(BadInsertPosition::class.java) {
            manager.controller_channel_insert_after(type, 0, 0, listOf())
        }
    }

    @Test
    fun test_controller_global_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume

        val beat_tree = manager.get_global_ctl_tree<OpusVolumeEvent>(type, 0)
        beat_tree.set_size(1)
        val initial_length = beat_tree.size
        manager.controller_global_insert_after(type, 0, listOf(0))

        assertEquals(beat_tree.size, initial_length + 1)

        assertThrows(BadInsertPosition::class.java) {
            manager.controller_global_insert_after(type, 0, listOf())
        }
    }

    @Test
    fun test_remove() {
        val manager = OpusManager()
        manager._project_change_new()
        val beat_key = BeatKey(0, 0, 0)
        val beat_tree = manager.get_tree(beat_key)
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
    fun test_controller_global_remove() {
        val manager = OpusManager()
        manager._project_change_new()
        val beat = 0
        val type = EffectType.Tempo
        manager.controller_global_split_tree(type, beat, listOf(), 2)

        assertThrows(RemovingRootException::class.java) {
            manager.controller_global_remove(type, beat, listOf())
        }

        val beat_tree = manager.get_global_ctl_tree<OpusTempoEvent>(type, beat)

        // Insert empty tree in the first beat
        manager.controller_global_insert_after(type, beat, listOf(0))

        //Then remove that tree
        manager.controller_global_remove(type, beat, listOf(1))
        assertEquals(beat_tree.size, 2)

        // Check that the siblings get adjusted
        for (i in 0 until 1) {
            manager.controller_global_insert_after(type, beat, listOf(0))
        }

        val tree = manager.get_global_ctl_tree<OpusTempoEvent>(type, beat, listOf(2))
        manager.controller_global_remove(type, beat, listOf(1))
        assertEquals(
            tree,
            manager.get_global_ctl_tree<OpusTempoEvent>(type, beat, listOf(1))
        )

        manager.controller_global_remove(type, beat, listOf(1))

        assertTrue(
            manager.get_global_ctl_tree<EffectEvent>(type, beat, listOf()).is_leaf()
        )

    }

    @Test
    fun test_controller_channel_remove() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        val beat = 0

        manager.controller_channel_split_tree(type, channel, beat, listOf(), 2)

        assertThrows(RemovingRootException::class.java) {
            manager.controller_channel_remove(type, 0, 0, listOf())
        }

        val beat_tree = manager.get_channel_ctl_tree<EffectEvent>(type, channel, beat)

        // Insert empty tree in the first beat
        manager.controller_channel_insert_after(type, channel, beat, listOf(0))

        //Then remove that tree
        manager.controller_channel_remove(type, channel, beat, listOf(1))
        assertEquals(beat_tree.size, 2)

        // Check that the siblings get adjusted
        for (i in 0 until 1) {
            manager.controller_channel_insert_after(type, channel, beat, listOf(0))
        }

        val tree = manager.get_channel_ctl_tree<EffectEvent>(type, channel, beat, listOf(2))
        manager.controller_channel_remove(type, channel, beat, listOf(1))
        assertEquals(
            tree,
            manager.get_channel_ctl_tree<EffectEvent>(type, channel, beat, listOf(1))
        )

        manager.controller_channel_remove(type, channel, beat, listOf(1))

        assertTrue(
            manager.get_channel_ctl_tree<EffectEvent>(type, channel, beat, listOf()).is_leaf()
        )
    }

    @Test
    fun test_controller_line_remove() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val beat_key = BeatKey(0, 0, 0)

        assertThrows(RemovingRootException::class.java) {
            manager.controller_line_remove(type, beat_key, listOf())
        }

        manager.controller_line_split_tree(type, beat_key, listOf(), 2)

        val beat_tree = manager.get_line_ctl_tree<EffectEvent>(type, beat_key)

        // Insert empty tree in the first beat
        manager.controller_line_insert_after(type, beat_key, listOf(0))

        //Then remove that tree
        manager.controller_line_remove(type, beat_key, listOf(1))
        assertEquals(beat_tree.size, 2)

        // Check that the siblings get adjusted
        for (i in 0 until 1) {
            manager.controller_line_insert_after(type, beat_key, listOf(0))
        }

        val tree = manager.get_line_ctl_tree<EffectEvent>(type, beat_key, listOf(2))
        manager.controller_line_remove(type, beat_key, listOf(1))
        assertEquals(
            tree,
            manager.get_line_ctl_tree<EffectEvent>(type, beat_key, listOf(1))
        )


        manager.controller_line_remove(type, beat_key, listOf(1))

        assertTrue(
            manager.get_line_ctl_tree<EffectEvent>(type, beat_key, listOf()).is_leaf()
        )
    }

    @Test
    fun test_split_tree() {
        val manager = OpusManager()
        manager._project_change_new()
        val split_count = 5
        val beat_key = BeatKey(0, 0, 0)

        // split a beat
        manager.split_tree(beat_key, listOf(), split_count)
        var beat_tree = manager.get_tree(beat_key)
        assertEquals(beat_tree.size, split_count)

        // Split an open leaf
        manager.split_tree(beat_key, listOf(split_count - 1), split_count)
        beat_tree = manager.get_tree(beat_key)
        assertEquals(beat_tree.get(split_count - 1).size, split_count)

        // split an event
        val position = mutableListOf(split_count - 1, 0)

        manager.set_event(beat_key, position, AbsoluteNoteEvent(30))

        manager.split_tree(beat_key, position, split_count)
        val subtree = manager.get_tree(beat_key, position)
        assertEquals(subtree.size, split_count)
    }

    @Test
    fun test_get_midi() {
        // This is not a particularily rigorous test, but its enough for now
        // Set up some Opus
        val manager = OpusManager()
        manager._project_change_new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.set_event(BeatKey(0,0,0), listOf(0), AbsoluteNoteEvent(12))
        manager.set_event(BeatKey(0,0,1), listOf(), AbsoluteNoteEvent(24))
        manager.set_event(BeatKey(0,0,2), listOf(), AbsoluteNoteEvent(12))
        manager.set_event(BeatKey(0,0,3), listOf(), RelativeNoteEvent(12))

        // Test Muted Line
        manager.new_line(0)
        manager.controller_line_set_initial_event(EffectType.Volume, 0, 1, OpusVolumeEvent(0f))

        manager.split_tree(BeatKey(1,0,0), listOf(), 2)
        manager.percussion_set_event(BeatKey(1,0,0), listOf(0))
        manager.percussion_set_event(BeatKey(1,0,1), listOf())
        manager.percussion_set_event(BeatKey(1,0,2), listOf())
        manager.percussion_set_event(BeatKey(1,0,3), listOf())


        manager.controllers.new_controller(EffectType.Tempo)
        manager.controller_global_split_tree(EffectType.Tempo, 0, listOf(), 2)
        manager.controller_global_set_event(EffectType.Tempo, 0, listOf(1), OpusTempoEvent(240F))
        manager.controller_global_set_event(EffectType.Tempo, 1, listOf(), OpusTempoEvent(120F))
        manager.controller_global_set_event(EffectType.Tempo, 2, listOf(), OpusTempoEvent(60F))

        val full_midi = manager.get_midi()
        val event_map = HashMap<Int, List<GeneralMIDIEvent>>()
        for ((position, events) in full_midi.get_all_events_grouped()) {
            event_map[position] = events
        }
        println("${event_map.values}")
        assertEquals(
            13, // Text, VolumeMSB(x2) SongPositionPointer, BankSelect, ProgramChange, BankSelect, ProgramChange, SetTempo, NoteOn, NoteOn, Balance(msb + lsb)
            event_map[0]!!.size
        )

        assertEquals(
            3, // NoteOff, NoteOff, SetTempo
            event_map[60]!!.size
        )

        assertEquals(
            4, // SongPositionPointer, SetTempo, NoteOn, NoteOn
            event_map[120]!!.size
        )

        assertEquals(
            6, // SongPositionPointer, SetTempo, NoteOff, NoteOff, NoteOn, NoteOn
            event_map[240]!!.size
        )

        assertEquals(
            5, // SongPositionPointer, NoteOff, NoteOff, NoteOn, NoteOn
            event_map[360]!!.size
        )

        assertEquals(
            2, // NoteOff, NoteOff
            event_map[480]!!.size
        )

        val partial_midi = manager.get_midi(1, 2)
        val event_map_b = HashMap<Int, List<GeneralMIDIEvent>>()
        for ((position, events) in partial_midi.get_all_events_grouped()) {
            event_map_b[position] = events
        }

        assertEquals(
            2,
            event_map_b.size
        )

        assertEquals(
            13, // SongPositionPointer, Text, VolumeMSBx2 BankSelect, ProgramChange, BankSelect, ProgramChange, SetTempo, NoteOn, NoteOn, BalanceLSB, BalanceMSB
            event_map_b[0]!!.size
        )

        assertEquals(
            4, // SongPositionPointer, SetTempo, NoteOff, NoteOff
            event_map[120]!!.size
        )
    }

    @Test
    fun test_import_midi() {
        val beat_count = 10

        val midi = Midi()
        midi.insert_event(0,0, SetTempo.from_bpm(80F))

        midi.insert_event(0,0, BankSelect(0, 0))
        midi.insert_event(0,0, ProgramChange(0, 1))
        midi.insert_event(0,0, BankSelect(1, 2))
        midi.insert_event(0,0, ProgramChange(1, 3))


        for (i in 0 until beat_count) {
            midi.insert_event(
                0,
                (i * midi.ppqn),
                NoteOn(0, 64, 64)
            )
            midi.insert_event(
                0,
                ((i + 1) * midi.ppqn),
                NoteOff(0, 64, 64)
            )

            for (j in 0 until 3) {
                midi.insert_event(
                    0,
                    (i * midi.ppqn) + (midi.ppqn * j / 3),
                    NoteOn(1, 50, 64)
                )
                midi.insert_event(
                    0,
                    (i * midi.ppqn) + (midi.ppqn * (j + 1) / 3),
                    NoteOff(1, 50, 64)
                )
            }

            for (j in 0 until 2) {
                midi.insert_event(
                    0,
                    (i * midi.ppqn) + (midi.ppqn * j / 2),
                    NoteOn(9, 30, 64)
                )
                midi.insert_event(
                    0,
                    (i * midi.ppqn) + (midi.ppqn * (j + 1) / 2),
                    NoteOff(9, 30, 64)
                )
            }
        }

        val manager = OpusManager()
        manager._project_change_midi(midi)

        assertEquals(
            OpusTempoEvent(80F),
            manager.controllers.get<OpusTempoEvent>(EffectType.Tempo).initial_event
        )

        assertEquals(
            beat_count,
            manager.length
        )

        assertEquals(
            1,
            manager.channels[1].lines.size
        )

        for (i in 0 until beat_count) {
            val position = manager.get_first_position(BeatKey(0,0,i), listOf())
            assertTrue(
                manager.get_tree(BeatKey(0,0,i), position).has_event()
            )

            assertEquals(
                3,
                manager.get_tree(BeatKey(1, 0, i), listOf()).size
            )
            assertEquals(
                2,
                manager.get_tree(BeatKey(2, 0, i), listOf()).size
            )
            for (j in 0 until 3) {
                assertTrue(
                    manager.get_tree(BeatKey(1,0,i), listOf(j)).has_event()
                )
            }
        }

        assertEquals(0, manager.channels[0].get_midi_bank())
        assertEquals(1, manager.channels[0].midi_program)
        assertEquals(2, manager.channels[1].get_midi_bank())
        assertEquals(3, manager.channels[1].midi_program)
    }

    @Test
    fun test_to_json() {
        //TODO("test_to_json")
    }
    @Test
    fun test_save() {
        //TODO("test_save")
    }

    // @Test
    // fun test_load_0() {
    //     val manager = OpusManager()
    //     manager.load_path("UTP0.json")
    //     assertEquals(
    //         "UnitTestProject0",
    //         manager.project_name
    //     )
    //     assertEquals(
    //         OpusTempoEvent(140F),
    //         manager.get_global_controller_initial_event(ControlEventType.Tempo)
    //     )
    //     assertEquals(27, manager.percussion_channel.lines[0].instrument)
    //     assertEquals(10, manager.percussion_channel.lines[1].instrument)
    //     assertEquals(8, manager.percussion_channel.lines[2].instrument)

    // }
    // @Test
    // fun test_load_1() {
    //     val manager = OpusManager()
    //     manager.load_path("UTP1.json")
    //     assertEquals(
    //         "UnitTestProject1",
    //         manager.project_name
    //     )
    //     assertEquals(
    //         OpusTempoEvent(140F),
    //         manager.get_global_controller_initial_event(ControlEventType.Tempo)
    //     )
    //     assertEquals(27, manager.percussion_channel.lines[0].instrument)
    //     assertEquals(10, manager.percussion_channel.lines[1].instrument)
    //     assertEquals(8, manager.percussion_channel.lines[2].instrument)
    // }

    //@Test
    //fun test_load_2() {
    //    val manager = OpusManager()
    //    manager.load_path("UTP2.json")
    //    assertEquals(
    //        "UnitTestProject2",
    //        manager.project_name
    //    )
    //    assertEquals(
    //        OpusTempoEvent(140F),
    //        manager.get_global_controller_initial_event(ControlEventType.Tempo)
    //    )

    //    assertEquals(27, manager.percussion_channel.lines[0].instrument)
    //    assertEquals(10, manager.percussion_channel.lines[1].instrument)
    //    assertEquals(8, manager.percussion_channel.lines[2].instrument)
    //}

    @Test
    fun test_set_duration() {
        val manager = OpusManager()
        manager._project_change_new()
        val beat_key = BeatKey(0, 0, 0)
        val event = AbsoluteNoteEvent(20)
        manager.set_event(beat_key, listOf(), event)

        val new_duration = 2
        manager.set_duration(beat_key, listOf(), new_duration)
        assertEquals(
            "Failed to set event duration",
            new_duration,
            manager.get_tree(beat_key).get_event()!!.duration
        )

        assertThrows(EventlessTreeException::class.java) {
            manager.set_duration(BeatKey(0,0,1), listOf(), new_duration)
        }
    }
    
    @Test
    fun test_get_global_ctl_tree() {
        val manager = OpusManager()
        manager._project_change_new()

        val event_a = OpusTempoEvent(100f)
        manager.controller_global_set_event(EffectType.Tempo, 2, listOf(), event_a)

        manager.controller_global_split_tree(EffectType.Tempo, 1, listOf(), 3)
        val event_b = OpusTempoEvent(50f)
        manager.controller_global_set_event(EffectType.Tempo, 1, listOf(2), event_b)

        assertEquals(
            "Failed get_global_ctl_tree",
            event_a,
            manager.get_global_ctl_tree<EffectEvent>(EffectType.Tempo, 2, listOf()).event
        )
        assertEquals(
            "Failed get_global_ctl_tree",
            event_b,
            manager.get_global_ctl_tree<EffectEvent>(EffectType.Tempo, 1, listOf(2)).event
        )
    }

    @Test
    fun test_get_channel_ctl_tree() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume

        val event_a = OpusVolumeEvent(100f)
        manager.controller_channel_set_event(type, 0, 0, listOf(), event_a)

        manager.controller_channel_split_tree(type, 0, 1, listOf(), 3)
        val event_b = OpusVolumeEvent(50f)
        manager.controller_channel_set_event(type, 0, 1, listOf(2), event_b)

        assertThrows(InvalidChannel::class.java) {
            manager.get_channel_ctl_tree<EffectEvent>(type, 2, 0, listOf())
        }

        assertEquals(
            "Failed get_channel_ctl_tree",
            event_a,
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 0, listOf()).event
        )
        assertEquals(
            "Failed get_channel_ctl_tree",
            event_b,
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 1, listOf(2)).event
        )
    }

    @Test
    fun test_get_line_ctl_tree() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume

        val beat_key_a = BeatKey(0,0,0)
        val event_a = OpusVolumeEvent(100f)
        manager.controller_line_set_event(type, beat_key_a, listOf(), event_a)

        val beat_key_b = BeatKey(0,0,1)
        manager.controller_line_split_tree(type, beat_key_b, listOf(), 3)
        val event_b = OpusVolumeEvent(50f)
        manager.controller_line_set_event(type, beat_key_b, listOf(2), event_b)

        assertThrows(BadBeatKey::class.java) {
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(2,0,1), listOf())
        }
        assertThrows(BadBeatKey::class.java) {
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0,3,1), listOf())
        }

        assertEquals(
            "Failed get_line_ctl_tree",
            event_a,
            manager.get_line_ctl_tree<EffectEvent>(type, beat_key_a, listOf()).event
        )
        assertEquals(
            "Failed get_line_ctl_tree",
            event_b,
            manager.get_line_ctl_tree<EffectEvent>(type, beat_key_b, listOf(2)).event
        )
    }

    @Test
    fun test_controller_global_overwrite_line() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        val event = OpusTempoEvent(100F)

        // Set Up first tree
        manager.controller_global_set_event(type, 0, listOf(), event)

        // apply overwrite
        manager.controller_global_overwrite_line(type, 0)

        for (beat in 0 until manager.length) {
            assertEquals(
                "Failed overwrite_global_Ctl_row",
                manager.get_global_ctl_tree<EffectEvent>(type, 0),
                manager.get_global_ctl_tree<EffectEvent>(type, beat)
            )
        }
        ////////////////////////
        manager._project_change_new()
        manager.set_beat_count(12)

        // Set Up first tree
        manager.controller_global_set_event(type, 3, listOf(), event)
        // add explicitly different tree
        manager.controller_global_split_tree(type, 0, listOf(), 3)

        // apply overwrite
        manager.controller_global_overwrite_line(type, 3)

        for (beat in 0 until 3) {
            assertNotEquals(
                "Incorrectly overwrote some trees - overwrite_global_Ctl_row",
                manager.get_global_ctl_tree<EffectEvent>(type, 3),
                manager.get_global_ctl_tree<EffectEvent>(type, beat)
            )
        }

        for (beat in 3 until manager.length) {
            assertEquals(
                "Failed overwrite_global_Ctl_row",
                manager.get_global_ctl_tree<EffectEvent>(type, 3),
                manager.get_global_ctl_tree<EffectEvent>(type, beat)
            )
        }


    }
    @Test
    fun test_controller_channel_overwrite_line() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val event = OpusVolumeEvent(.5F)
        val working_channel = 0

        // Set Up first tree
        manager.controller_channel_set_event(type, working_channel, 0, listOf(), event)

        // apply overwrite
        manager.controller_channel_overwrite_line(type, working_channel, 0, 0)

        for (beat in 0 until manager.length) {
            assertEquals(
                "Failed overwrite_channel_Ctl_row",
                manager.get_channel_ctl_tree<EffectEvent>(type, working_channel, 0),
                manager.get_channel_ctl_tree<EffectEvent>(type, working_channel, beat)
            )
        }
        ////////////////////////
        manager._project_change_new()
        manager.set_beat_count(12)

        // Set Up first tree
        manager.controller_channel_set_event(type, working_channel, 3, listOf(), event)
        // add explicitly different tree
        manager.controller_channel_split_tree(type, working_channel, 0, listOf(), 3)

        // apply overwrite
        manager.controller_channel_overwrite_line(type, working_channel, working_channel, 3)

        for (beat in 0 until 3) {
            assertNotEquals(
                "Incorrectly overwrote some trees - overwrite_channel_Ctl_row",
                manager.get_channel_ctl_tree<EffectEvent>(type, working_channel, 3),
                manager.get_channel_ctl_tree<EffectEvent>(type, working_channel, beat)
            )
        }

        for (beat in 3 until manager.length) {
            assertEquals(
                "Failed overwrite_channel_Ctl_row",
                manager.get_channel_ctl_tree<EffectEvent>(type, working_channel, 3),
                manager.get_channel_ctl_tree<EffectEvent>(type, working_channel, beat)
            )
        }
    }

    @Test
    fun test_controller_line_overwrite_line() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val event = OpusVolumeEvent(.9F)
        val working_key = BeatKey(0,0,0)
        val working_key_b = BeatKey(0,0,3)

        // Set Up first tree
        manager.controller_line_set_event(type, working_key, listOf(), event)

        // apply overwrite
        manager.controller_line_overwrite_line(type, working_key.channel, working_key.line_offset, working_key)

        for (beat in 0 until manager.length) {
            assertEquals(
                "Failed overwrite_line_Ctl_row",
                manager.get_line_ctl_tree<EffectEvent>(type, working_key),
                manager.get_line_ctl_tree<EffectEvent>(type, working_key)
            )
        }
        ////////////////////////
        manager._project_change_new()
        manager.set_beat_count(12)

        // Set Up first tree
        manager.controller_line_set_event(type, working_key_b, listOf(), event)
        // add explicitly different tree
        manager.controller_line_split_tree(type, working_key, listOf(), 5)

        // apply overwrite
        manager.controller_line_overwrite_line(type, working_key_b.channel, working_key_b.line_offset, working_key_b)

        for (beat in 0 until working_key_b.beat) {
            assertNotEquals(
                "Incorrectly overwrote some trees - overwrite_line_Ctl_row",
                manager.get_line_ctl_tree<EffectEvent>(type, working_key_b),
                manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(working_key_b.channel, working_key_b.line_offset, beat))
            )
        }

        for (beat in working_key_b.beat until manager.length) {
            assertEquals(
                "Failed overwrite_line_Ctl_row",
                manager.get_line_ctl_tree<EffectEvent>(type, working_key_b),
                manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(working_key_b.channel, working_key_b.line_offset, beat))
            )
        }
    }

    @Test
    fun test_get_beatkeys_from_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_line(0)
        manager.new_channel(1)
        manager.new_line(1)
        manager.new_line(1)
        manager.set_beat_count(6)

        assertThrows(RangeOverflow::class.java) {
            // Vertical overflow - channel
            manager._get_beatkeys_from_range(BeatKey(0, 0, 4), BeatKey(3, 0, 0), BeatKey(3, 2, 2))
        }
        assertThrows(RangeOverflow::class.java) {
            // Vertical overflow - line
            manager._get_beatkeys_from_range(BeatKey(0, 0, 4), BeatKey(0, 3, 0), BeatKey(1, 2, 2))
        }

        assertThrows(RangeOverflow::class.java) {
            manager._get_beatkeys_from_range(BeatKey(0, 0, 4), BeatKey(0, 0, 0), BeatKey(1, 4, 2))
        }

        assertThrows(RangeOverflow::class.java) {
            manager._get_beatkeys_from_range(BeatKey(0, 0, 4), BeatKey(0, 0, 0), BeatKey(1, 2, 2))
        }
        assertThrows(RangeOverflow::class.java) {
            manager._get_beatkeys_from_range(BeatKey(0, 2, 3), BeatKey(0, 0, 0), BeatKey(1, 2, 2))
        }
    }

    @Test
    fun test_overwrite_beat_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_line(0)
        manager.new_channel(1)
        manager.new_line(1)
        manager.new_line(1)
        manager.set_beat_count(6)

        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    manager.set_event(BeatKey(c, l, b), listOf(),  AbsoluteNoteEvent((c * 6) + (l * 3) + b))
                }
            }
        }
        // -------------------------------------------------------
        manager.overwrite_beat_range(BeatKey(0, 0, 3), BeatKey(0, 0, 0), BeatKey(1, 2, 2))
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    assertEquals(
                        manager.get_tree(BeatKey(c, l, b)),
                        manager.get_tree(BeatKey(c, l, b + 3))
                    )
                }
            }
        }
        // -------------------------------------------------------
        // Undo overwrite
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    manager.unset(BeatKey(c, l, b + 3), listOf())
                }
            }
        }
        // -------------------------------------------------------
        // Overwrite with overlapping ranges
        manager.overwrite_beat_range(BeatKey(0, 0, 1), BeatKey(0, 0, 0), BeatKey(1, 2, 2))
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    assertEquals(
                        (c * 6) + (l * 3) + b,
                        (manager.get_tree(BeatKey(c, l, b + 1)).event!! as AbsoluteNoteEvent).note
                    )
                }
            }
        }
        // -------------------------------------------------------
        // Undo All sets
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 6) {
                    manager.unset(BeatKey(c, l, b), listOf())
                }
            }
        }
        // Set second half
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    manager.set_event(BeatKey(c, l, b + 3), listOf(),  AbsoluteNoteEvent((c * 6) + (l * 3) + b))
                }
            }
        }
        // -------------------------------------------------------
        // Overwrite with overlapping ranges
        manager.overwrite_beat_range(BeatKey(0, 0, 1), BeatKey(0, 0, 3), BeatKey(1, 2, 5))
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    assertEquals(
                        (c * 6) + (l * 3) + b,
                        (manager.get_tree(BeatKey(c, l, b + 1)).event!! as AbsoluteNoteEvent).note
                    )
                }
            }
        }
    }

    @Test
    fun test_move_beat_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_line(0)
        manager.new_channel(1)
        manager.new_line(1)
        manager.new_line(1)
        manager.set_beat_count(6)

        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    manager.set_event(BeatKey(c, l, b), listOf(),  AbsoluteNoteEvent((c * 6) + (l * 3) + b))
                }
            }
        }

        manager.move_beat_range(
            BeatKey(0, 0, 3),
            BeatKey(0, 0, 0),
            BeatKey(1, 2, 2)
        )

        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    assertFalse(manager.get_tree(BeatKey(c, l, b)).has_event())

                    assertEquals(
                        (c * 6) + (l * 3) + b,
                        (manager.get_tree(BeatKey(c, l, b + 3)).event!! as AbsoluteNoteEvent).note
                    )
                }
            }
        }
        // -------------------------------------------------------
        // Overwrite with overlapping ranges
        manager.move_beat_range(BeatKey(0, 0, 1), BeatKey(0, 0, 3), BeatKey(1, 2, 5))
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    assertEquals(
                        (c * 6) + (l * 3) + b,
                        (manager.get_tree(BeatKey(c, l, b + 1)).event!! as AbsoluteNoteEvent).note
                    )
                }
            }
        }
        // -------------------------------------------------------
        // Overwrite with overlapping ranges
        manager.move_beat_range(BeatKey(0, 0, 2), BeatKey(0, 0, 1), BeatKey(1, 2, 3))
        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    assertEquals(
                        (c * 6) + (l * 3) + b,
                        (manager.get_tree(BeatKey(c, l, b + 2)).event!! as AbsoluteNoteEvent).note
                    )
                }
            }
        }
    }

    @Test
    fun test_controller_global_overwrite_range() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.controller_global_overwrite_range(type, 2, 0, 1)

        assertEquals(
            OpusTempoEvent(10F),
            manager.get_global_ctl_tree<EffectEvent>(type, 0, listOf()).event
        )
        assertEquals(
            OpusTempoEvent(11F),
            manager.get_global_ctl_tree<EffectEvent>(type, 1, listOf()).event
        )

        assertEquals(
            OpusTempoEvent(10F),
            manager.get_global_ctl_tree<EffectEvent>(type, 2, listOf()).event
        )
        assertEquals(
            OpusTempoEvent(11F),
            manager.get_global_ctl_tree<EffectEvent>(type, 3, listOf()).event
        )

        manager.controller_global_overwrite_range(type, 3, 2, 3)
        assertEquals(5, manager.length)
    }

    @Test
    fun test_controller_global_move_range() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.controller_global_move_range(type, 2, 0, 1)

        assertFalse(
            manager.get_global_ctl_tree<EffectEvent>(type, 0, listOf()).has_event()
        )
        assertFalse(
            manager.get_global_ctl_tree<EffectEvent>(type, 1, listOf()).has_event()
        )
        assertEquals(
            OpusTempoEvent(10F),
            manager.get_global_ctl_tree<EffectEvent>(type, 2, listOf()).event
        )
        assertEquals(
            OpusTempoEvent(11F),
            manager.get_global_ctl_tree<EffectEvent>(type, 3, listOf()).event
        )

        manager.controller_global_move_range(type, 3, 2, 3)
        assertEquals(5, manager.length)

    }

    @Test
    fun test_controller_channel_overwrite_range() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(10f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(11f))
        manager.controller_channel_overwrite_range(type, 0, 2, 0, 0, 1)

        assertEquals(
            OpusVolumeEvent(10f),
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 2, listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(11f),
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 3, listOf()).event
        )

        manager.controller_channel_overwrite_range(type, 0, 3, 0, 2, 3)
        assertEquals(5, manager.length)
    }

    @Test
    fun test_controller_channel_move_range() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(10f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(11f))

        manager.controller_channel_move_range(type, 0, 2, 0, 0, 1)

        assertFalse(
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 0, listOf()).has_event()
        )
        assertFalse(
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 1, listOf()).has_event()
        )

        assertEquals(
            OpusVolumeEvent(10f),
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 2, listOf()).event
        )
        assertEquals(
            OpusVolumeEvent(11f),
            manager.get_channel_ctl_tree<EffectEvent>(type, 0, 3, listOf()).event
        )

        manager.controller_channel_move_range(type, 0, 3, 0, 2, 3)
        assertEquals(5, manager.length)
    }

    @Test
    fun test_move_line_ctl_range() {
        val type = EffectType.Volume
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_channel(1)
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.10f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.11f))
        manager.controller_line_set_event(type, BeatKey(1, 0, 0), listOf(), OpusVolumeEvent(.12f))
        manager.controller_line_set_event(type, BeatKey(1, 0, 1), listOf(), OpusVolumeEvent(.13f))
        
        manager.controller_line_move_range(type, BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(1, 0, 1))

        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 0), listOf()).has_event()
        )
        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 1), listOf()).has_event()
        )
        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 0), listOf()).has_event()
        )
        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 1), listOf()).has_event()
        )

        assertEquals(
            OpusVolumeEvent(.10f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 2), listOf()).event
        )
        assertEquals(
            OpusVolumeEvent(.11F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 3), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.12F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 2), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.13F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 3), listOf()).event
        )

        manager.controller_line_move_range(type, BeatKey(0, 0, 0), BeatKey(0, 0, 2), BeatKey(1, 0, 3))

        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 2), listOf()).has_event()
        )
        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 3), listOf()).has_event()
        )
        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 2), listOf()).has_event()
        )
        assertFalse(
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 3), listOf()).has_event()
        )

        assertEquals(
            OpusVolumeEvent(.10F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 0), listOf()).event
        )
        assertEquals(
            OpusVolumeEvent(.11F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 1), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.12F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 0), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.13F),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 1), listOf()).event
        )
    }

    @Test
    fun test_controller_line_overwrite_range() {
        val type = EffectType.Volume
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_channel(1)
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.10F))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.11F))
        manager.controller_line_set_event(type, BeatKey(1, 0, 0), listOf(), OpusVolumeEvent(.12F))
        manager.controller_line_set_event(type, BeatKey(1, 0, 1), listOf(), OpusVolumeEvent(.13F))
        
        manager.controller_line_overwrite_range(type, BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(1, 0, 1))

        assertEquals(
            OpusVolumeEvent(.10f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 0), listOf()).event
        )
        assertEquals(
            OpusVolumeEvent(.11f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 1), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.12f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 0), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.13f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 1), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.10f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 2), listOf()).event
        )
        assertEquals(
            OpusVolumeEvent(.11f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0, 0, 3), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.12f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 2), listOf()).event
        )

        assertEquals(
            OpusVolumeEvent(.13f),
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(1, 0, 3), listOf()).event
        )
    }

    @Test
    fun test_add_remove_line_controller() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        val line_offset = 0
        manager.new_line_controller(type, channel, line_offset)

        assertEquals(
            "Failed to add line_ctl_line",
            true,
            manager.has_line_controller(type, channel, line_offset)
        )

        manager.remove_line_controller(type, channel, line_offset)

        assertEquals(
            "Failed to remove line_ctl_line",
            false,
            manager.has_line_controller(type, channel, line_offset)
        )
    }

    @Test
    fun test_add_remove_channel_controller() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        manager.new_channel_controller(type, channel)

        assertEquals(
            "Failed to add channel_ctl_line",
            true,
            manager.has_channel_controller(type, channel)
        )

        manager.remove_channel_controller(type, channel)

        assertEquals(
            "Failed to remove channel_ctl_line",
            false,
            manager.has_channel_controller(type, channel)
        )
    }

    @Test
    fun test_remove_global_controller() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        manager.new_global_controller(type)

        assertEquals(
            "Failed to add global_ctl_line",
            true,
            manager.has_global_controller(type)
        )

        manager.remove_global_controller(type)

        assertEquals(
            "Failed to remove global_ctl_line",
            false,
            manager.has_global_controller(type)
        )
    }

    @Test
    fun test_get_current_global_controller_value() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        val first_event = OpusTempoEvent(1F)
        val second_event = OpusTempoEvent(2F)
        val third_event = OpusTempoEvent(3F)

        manager.controller_global_set_initial_event(type, first_event)

        manager.controller_global_split_tree(type, 0, listOf(), 2)
        manager.controller_global_set_event(type, 0, listOf(1), second_event)

        manager.controller_global_set_event(type, 2, listOf(), third_event)

        assertEquals(
            "get_current_global_controller_value fail",
            first_event,
            manager.get_current_global_controller_event(type, 0, listOf())
        )

        assertEquals(
            "get_current_global_controller_value fail",
            second_event,
            manager.get_current_global_controller_event(type, 0, listOf(1))
        )

        assertEquals(
            "get_current_global_controller_value fail",
            second_event,
            manager.get_current_global_controller_event(type, 1, listOf())
        )

        assertEquals(
            "get_current_global_controller_value fail",
            third_event,
            manager.get_current_global_controller_event(type, 2, listOf())
        )

        assertEquals(
            "get_current_global_controller_value fail",
            third_event,
            manager.get_current_global_controller_event(type, 3, listOf())
        )
    }

    @Test
    fun test_get_current_channel_controller_value() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        val first_event = OpusVolumeEvent(.1f)
        val second_event = OpusVolumeEvent(.2f)
        val third_event = OpusVolumeEvent(.3f)

        manager.controller_channel_set_initial_event(type, channel, first_event)

        manager.controller_channel_split_tree(type, channel, 0, listOf(), 2)
        manager.controller_channel_set_event(type, channel, 0, listOf(1), second_event)

        manager.controller_channel_set_event(type, channel, 2, listOf(), third_event)

        assertEquals(
            "get_current_channel_controller_value fail",
            first_event,
            manager.get_current_channel_controller_event(type, channel, 0, listOf())
        )

        assertEquals(
            "get_current_channel_controller_value fail",
            second_event,
            manager.get_current_channel_controller_event(type, channel, 0, listOf(1))
        )

        assertEquals(
            "get_current_channel_controller_value fail",
            second_event,
            manager.get_current_channel_controller_event(type, channel, 1, listOf())
        )

        assertEquals(
            "get_current_channel_controller_value fail",
            third_event,
            manager.get_current_channel_controller_event(type, channel, 2, listOf())
        )

        assertEquals(
            "get_current_channel_controller_value fail",
            third_event,
            manager.get_current_channel_controller_event(type, channel, 3, listOf())
        )
    }

    @Test
    fun test_get_current_line_controller_value() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        val line_offset = 0

        val first_event = OpusVolumeEvent(.1f)
        val second_event = OpusVolumeEvent(.2f)
        val third_event = OpusVolumeEvent(.3f)

        manager.controller_line_set_initial_event(type, channel, line_offset, first_event)

        manager.controller_line_split_tree(type, BeatKey(channel, line_offset, 0), listOf(), 2)
        manager.controller_line_set_event(type, BeatKey(channel, line_offset, 0), listOf(1), second_event)

        manager.controller_line_set_event(type, BeatKey(channel, line_offset, 2), listOf(), third_event)

        assertEquals(
            "get_current_line_controller_value fail",
            first_event,
            manager.get_current_line_controller_event(type, BeatKey(channel, line_offset, 0), listOf())
        )

        assertEquals(
            "get_current_line_controller_value fail",
            second_event,
            manager.get_current_line_controller_event(type, BeatKey(channel, line_offset, 0), listOf(1))
        )

        assertEquals(
            "get_current_line_controller_value fail",
            second_event,
            manager.get_current_line_controller_event(type, BeatKey(channel, line_offset, 1), listOf())
        )

        assertEquals(
            "get_current_line_controller_value fail",
            third_event,
            manager.get_current_line_controller_event(type, BeatKey(channel, line_offset, 2), listOf())
        )

        assertEquals(
            "get_current_line_controller_value fail",
            third_event,
            manager.get_current_line_controller_event(type, BeatKey(channel, line_offset, 3), listOf())
        )
    }

    @Test
    fun test_set_global_controller_initial_value() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        val event = OpusTempoEvent(1F)

        manager.controller_global_set_initial_event(type, event)
        assertEquals(
            "get_current_global_controller_value fail",
            event,
            manager.get_global_controller_initial_event(type)
        )
    }

    @Test
    fun test_set_channel_controller_initial_value() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        val event = OpusVolumeEvent(1f)

        manager.controller_channel_set_initial_event(type, channel, event)
        assertEquals(
            "Failed set channel controller initial event",
            event,
            manager.get_channel_controller_initial_event(type, channel)
        )
    }

    @Test
    fun test_set_line_controller_initial_value() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val channel = 0
        val line_offset = 0

        val event = OpusVolumeEvent(.1f)
        manager.controller_line_set_initial_event(type, channel, line_offset, event)
        assertEquals(
            "Failed set line controller initial event",
            event,
            manager.get_line_controller_initial_event(type, channel, line_offset)
        )
    }

    @Test
    fun test_is_tuning_standard() {
        val manager = OpusManager()
        manager._project_change_new()

        manager.set_tuning_map(
            Array(12) {
                Pair(it, 12)
            }
        )

        assertTrue(
            "tuning map of size 12 and all entries in form Pair(i, 12) should be standard",
            manager.is_tuning_standard()
        )

        manager.set_tuning_map(
            Array(7) {
                Pair(it, 12)
            }
        )

        assertTrue(
            "tuning map should still be seen as standard when size != 12 but entries are all Pair(i, 12)",
            manager.is_tuning_standard()
        )

        manager.set_tuning_map(
            Array(24) {
                Pair(it, 24)
            }
        )

        assertFalse(
            "tuning map should not be standard with entries Pair(i, 24)",
            manager.is_tuning_standard()
        )

    }

    @Test
    fun test_set_tuning_map() {
        val manager = OpusManager()
        manager._project_change_new()
        val channel = 0
        val original_value = 12
        manager.set_event(BeatKey(channel,0,0), listOf(), AbsoluteNoteEvent(original_value))

        // First don't modify existing event
        manager.set_tuning_map(
            Array(4) {
                Pair(it, 4)
            },
            false
        )

        assertEquals(
            "Should not have changed event Value",
            original_value,
            (manager.get_tree(BeatKey(channel,0,0), listOf()).event!! as AbsoluteNoteEvent).note
        )

        // Return to original state
        manager.set_tuning_map(
            Array(12) {
                Pair(it, 12)
            },
            false
        )

        // Now Modify the event
        manager.set_tuning_map(
            Array(4) {
                Pair(it, 4)
            }
        )

        assertEquals(
            "Should have changed event Value",
            4,
            (manager.get_tree(BeatKey(channel,0,0), listOf()).event!! as AbsoluteNoteEvent).note
        )
    }

    @Test
    fun test_swap_lines() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0, 0)
        val test_event = RelativeNoteEvent(1)
        manager.set_event(BeatKey(0,0,0), listOf(), test_event)

        val line_a = manager.channels[0].lines[0]
        val line_b = manager.channels[0].lines[1]

        manager.swap_lines(0, 0, 0, 1)
        assertEquals(
            "Failed to swap lines",
            line_b,
            manager.channels[0].lines[0]
        )
        assertEquals(
            "Failed to swap lines",
            line_a,
            manager.channels[0].lines[1]
        )

        manager.new_line(1, 0)
        manager.percussion_set_event(BeatKey(1,0,0), listOf())
        assertThrows(IncompatibleChannelException::class.java) {
            manager.swap_lines(0, 0, 1, 0)
        }

        val percussion_channel = manager.get_channel(1)
        val p_line_a = percussion_channel.lines[0]
        val p_line_b = percussion_channel.lines[1]

        manager.swap_lines(1, 0, 1, 1)
        assertEquals(
            "Failed to swap percussion lines",
            p_line_b,
            percussion_channel.lines[0]
        )

        assertEquals(
            "Failed to swap percussion lines",
            p_line_a,
            percussion_channel.lines[1]
        )
    }

    @Test
    fun test_set_percussion_event() {
        val manager = OpusManager()
        manager._project_change_new()

        val first_instrument = manager.get_percussion_instrument(1, 0)
        manager.percussion_set_event(BeatKey(1,0,0), listOf())
        assertTrue(
            manager.get_tree(BeatKey(1,0,0), listOf()).has_event()
        )

        val second_instrument = 2
        manager.percussion_set_instrument(1, 0, second_instrument)
        manager.percussion_set_event(BeatKey(1,0,0), listOf())
        assertTrue(
            manager.get_tree(BeatKey(1,0,0), listOf()).has_event()
        )
    }

    @Test
    fun test_std_abs_offset() {
        val manager = OpusManager()
        manager._project_change_new()

        assertThrows(IndexOutOfBoundsException::class.java) {
            manager.get_channel_and_line_offset(2)
        }

        manager.new_line(0)
        manager.new_line(0)

        for (i in 1 until 3) {
            manager.new_channel(i)
            for (j in 0 until 3) {
                manager.new_line(i)
            }
        }

        var abs = 0
        manager.channels.forEachIndexed { i: Int, channel ->
            channel.lines.forEachIndexed { j: Int, line ->
                assertEquals(
                    "incorrect std_offset",
                    Pair(i, j),
                    manager.get_channel_and_line_offset(abs)
                )

                assertEquals(
                    "incorrect abs_offset",
                    abs,
                    manager.get_instrument_line_index(i, j)
                )

                abs += 1
            }
        }

        val beat_key_a = BeatKey(0,0,0)
        val beat_key_b = BeatKey(2,1,2)
        assertEquals(
            "get_abs_difference incorrect",
            manager.get_abs_difference(beat_key_a, beat_key_b),
            Pair(8,2)
        )
    }

    @Test
    fun test_insert() {
        val manager = OpusManager()
        manager._project_change_new()
        val test_event_a = AbsoluteNoteEvent(0)
        manager.set_event(BeatKey(0,0,0), listOf(), test_event_a)
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.insert(BeatKey(0,0,0), listOf(0))

        assertEquals(
            "Insert into beat_tree fail",
            3,
            manager.get_tree(BeatKey(0,0,0), listOf()).size
        )

        assertEquals(
            "Insert into beat_tree didn't insert correctly",
            test_event_a,
            manager.get_tree(BeatKey(0,0,0), listOf(1)).event
        )

        assertThrows(BadInsertPosition::class.java) {
            manager.insert(BeatKey(0,0,0), listOf())
        }

    }

    @Test
    fun test_insert_line_ctl() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val test_event_a = OpusVolumeEvent(.64f)
        manager.controller_line_set_event(type, BeatKey(0,0,0), listOf(), test_event_a)
        manager.controller_line_split_tree(type, BeatKey(0,0,0), listOf(), 2)
        manager.controller_line_insert(type, BeatKey(0,0,0), listOf(0))

        assertEquals(
            "Insert Line Controller Tree fail",
            3,
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0,0,0), listOf()).size
        )

        assertEquals(
            "Insert Line Controller Tree didn't insert correctly",
            test_event_a,
            manager.get_line_ctl_tree<EffectEvent>(type, BeatKey(0,0,0), listOf(1)).event
        )

        assertThrows(BadInsertPosition::class.java) {
            manager.controller_line_insert(type, BeatKey(0,0,0), listOf())
        }

    }

    @Test
    fun test_insert_channel_ctl() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Reverb
        val channel = 0
        val beat = 0
        val test_event_a = OpusReverbEvent(1F)
        manager.controller_channel_set_event(type, channel, beat, listOf(), test_event_a)
        manager.controller_channel_split_tree(type, channel, beat, listOf(), 2)
        manager.controller_channel_insert(type, channel, beat, listOf(0))

        assertEquals(
            "Insert Channel Controller Tree fail",
            3,
            manager.get_channel_ctl_tree<EffectEvent>(type, channel, beat, listOf()).size
        )

        assertEquals(
            "Insert Channel Controller Tree didn't insert correctly",
            test_event_a,
            manager.get_channel_ctl_tree<EffectEvent>(type, channel, beat, listOf(1)).event
        )

        assertThrows(BadInsertPosition::class.java) {
            manager.controller_channel_insert(type, channel, beat, listOf())
        }

    }

    @Test
    fun test_insert_global_ctl() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        val beat = 0
        val test_event_a = OpusTempoEvent(120F)
        manager.controller_global_set_event(type, beat, listOf(), test_event_a)
        manager.controller_global_split_tree(type, beat, listOf(), 2)
        manager.controller_global_insert(type, beat, listOf(0))

        assertEquals(
            "Insert global Controller Tree fail",
            3,
            manager.get_global_ctl_tree<EffectEvent>(type, beat, listOf()).size
        )

        assertEquals(
            "Insert global Controller Tree didn't insert correctly",
            test_event_a,
            manager.get_global_ctl_tree<EffectEvent>(type, beat, listOf(1)).event
        )

        assertThrows(BadInsertPosition::class.java) {
            manager.controller_global_insert(type, beat, listOf())
        }
    }

    @Test
    fun test_set_unset_global() {
        val manager = OpusManager()
        manager._project_change_new()

        // set/unset leaf
        val beat = 0
        val position: List<Int> = listOf()

        val type = EffectType.Tempo
        val test_event = OpusTempoEvent(1f)
        manager.controller_global_set_event(type, beat, position, test_event)
        val tree = manager.get_global_ctl_tree<EffectEvent>(type, beat, position)
        assertTrue(
            "Failed to set global ctl event",
            tree.has_event()
        )

        assertEquals(
            "Set global ctl event, but set it wrong",
            tree.get_event(),
            test_event
        )

        manager.controller_global_unset(type, beat, position)
        assertFalse(
            "Failed to unset tree",
            manager.get_global_ctl_tree<EffectEvent>(type, beat, position).has_event()
        )

        manager.controller_global_split_tree(type, beat, position, 2)
        manager.controller_global_set_event(type, beat, listOf(0), test_event)

        manager.controller_global_unset(type, beat, listOf(0))
        assertFalse(
            "Failed to unset tree",
            manager.get_global_ctl_tree<EffectEvent>(type, beat, listOf(0)).has_event()
        )

    }

    @Test
    fun test_set_unset_channel() {
        val manager = OpusManager()
        manager._project_change_new()

        // set/unset leaf
        val beat = 0
        val channel = 0
        val position: List<Int> = listOf()

        val type = EffectType.Volume
        val test_event = OpusVolumeEvent(.1f)
        manager.controller_channel_set_event(type, channel, beat, position, test_event)

        val tree = manager.get_channel_ctl_tree<OpusVolumeEvent>(type, channel, beat, position)
        assertTrue(
            "Failed to set global ctl event",
            tree.has_event()
        )

        assertEquals(
            "Set global ctl event, but set it wrong",
            tree.get_event(),
            test_event
        )

        manager.controller_channel_unset(type, channel, beat, position)
        assertFalse(
            "Failed to unset tree",
            manager.get_channel_ctl_tree<OpusVolumeEvent>(type, channel, beat, position).has_event()
        )
    }

    @Test
    fun test_set_unset_line() {
        val manager = OpusManager()
        manager._project_change_new()

        // set/unset leaf
        val beat_key = BeatKey(0,0,0)
        val position: List<Int> = listOf()

        val type = EffectType.Volume
        val test_event = OpusVolumeEvent(.1f)
        manager.controller_line_set_event(type, beat_key, position, test_event)

        val tree = manager.get_line_ctl_tree<OpusVolumeEvent>(type, beat_key, position)
        assertTrue(
            "Failed to set global ctl event",
            tree.has_event()
        )

        assertEquals(
            "Set global ctl event, but set it wrong",
            tree.get_event(),
            test_event
        )

        manager.controller_line_unset(type, beat_key, position)
        assertFalse(
            "Failed to unset tree",
            manager.get_line_ctl_tree<OpusVolumeEvent>(type, beat_key, position).has_event()
        )
    }

    @Test
    fun test_get_first_position() {
        val manager = OpusManager()
        manager._project_change_new()
        val beat_key = BeatKey(0,0,0)
        val max_depth = 3
        val splits = 3
        val stack = mutableListOf(listOf<Int>())
        while (stack.isNotEmpty()) {
            val position = stack.removeAt(0)
            manager.split_tree(beat_key, position, splits)

            if (position.size >= max_depth - 1) {
                continue
            }

            for (i in 0 until splits) {
                stack.add(
                    List(position.size + 1) {
                        if (it < position.size) {
                            position[it]
                        } else {
                            i
                        }
                    }
                )
            }
        }

        assertEquals(
            "get_first_position incorrect",
            listOf(0,0,0),
            manager.get_first_position(beat_key)
        )

        assertEquals(
            "get_first_position incorrect",
            listOf(0,0,0),
            manager.get_first_position(beat_key, listOf())
        )

        assertEquals(
            "get_first_position incorrect",
            listOf(1,0,0),
            manager.get_first_position(beat_key, listOf(1))
        )
    }

    @Test
    fun test_get_first_global_ctl_position() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        val beat = 0
        val max_depth = 3
        val splits = 3
        val stack = mutableListOf(listOf<Int>())
        while (stack.isNotEmpty()) {
            val position = stack.removeAt(0)
            manager.controller_global_split_tree(type, beat, position, splits)

            if (position.size >= max_depth - 1) {
                continue
            }

            for (i in 0 until splits) {
                stack.add(
                    List(position.size + 1) {
                        if (it < position.size) {
                            position[it]
                        } else {
                            i
                        }
                    }
                )
            }
        }

        assertEquals(
            "get_first_position_global_ctl incorrect",
            listOf(0,0,0),
            manager.get_first_position_global_ctl(type, beat)
        )

        assertEquals(
            "get_first_position_global_ctl incorrect",
            listOf(0,0,0),
            manager.get_first_position_global_ctl(type, beat, listOf())
        )

        assertEquals(
            "get_first_position_global_ctl incorrect",
            listOf(1,0,0),
            manager.get_first_position_global_ctl(type, beat, listOf(1))
        )
    }

    @Test
    fun test_get_first_channel_ctl_position() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Reverb
        val channel = 0
        val beat = 0
        val max_depth = 3
        val splits = 3
        val stack = mutableListOf(listOf<Int>())
        while (stack.isNotEmpty()) {
            val position = stack.removeAt(0)
            manager.controller_channel_split_tree(type, channel, beat, position, splits)

            if (position.size >= max_depth - 1) {
                continue
            }

            for (i in 0 until splits) {
                stack.add(
                    List(position.size + 1) {
                        if (it < position.size) {
                            position[it]
                        } else {
                            i
                        }
                    }
                )
            }
        }

        assertEquals(
            "get_first_position_channel_ctl incorrect",
            listOf(0,0,0),
            manager.get_first_position_channel_ctl(type, channel, beat)
        )

        assertEquals(
            "get_first_position_channel_ctl incorrect",
            listOf(0,0,0),
            manager.get_first_position_channel_ctl(type, channel, beat, listOf())
        )

        assertEquals(
            "get_first_position_channel_ctl incorrect",
            listOf(1,0,0),
            manager.get_first_position_channel_ctl(type, channel, beat, listOf(1))
        )

    }

    @Test
    fun test_get_first_line_ctl_position() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val beat_key = BeatKey(0,0,0)
        val max_depth = 3
        val splits = 3
        val stack = mutableListOf(listOf<Int>())
        while (stack.isNotEmpty()) {
            val position = stack.removeAt(0)
            manager.controller_line_split_tree(type, beat_key, position, splits)

            if (position.size >= max_depth - 1) {
                continue
            }

            for (i in 0 until splits) {
                stack.add(
                    List(position.size + 1) {
                        if (it < position.size) {
                            position[it]
                        } else {
                            i
                        }
                    }
                )
            }
        }

        assertEquals(
            "get_first_position_line_ctl incorrect",
            listOf(0,0,0),
            manager.get_first_position_line_ctl(type, beat_key)
        )

        assertEquals(
            "get_first_position_line_ctl incorrect",
            listOf(0,0,0),
            manager.get_first_position_line_ctl(type, beat_key, listOf())
        )

        assertEquals(
            "get_first_position_line_ctl incorrect",
            listOf(1,0,0),
            manager.get_first_position_line_ctl(type, beat_key, listOf(1))
        )
    }

    @Test
    fun test_move_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val key_a = BeatKey(0,0,0)
        val key_b = BeatKey(0,0,2)
        val event = AbsoluteNoteEvent(0)
        manager.split_tree(key_a, listOf(), 3)
        manager.split_tree(key_b, listOf(), 3)
        for (i in 0 until 3) {
            manager.split_tree(key_a, listOf(i), 3)
            manager.split_tree(key_b, listOf(i), 3)
        }

        manager.set_event(key_a, listOf(0,0), event)
        manager.move_leaf(key_a, listOf(0,0), key_a, listOf(0, 2))
        assertEquals(
            "move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_tree(key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_tree(key_a, listOf(0,2)).has_event()
        )

        manager.move_leaf(key_a, listOf(0,2), key_b, listOf())
        assertEquals(
            "move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_tree(key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_tree(key_b, listOf()).has_event()
        )
    }

    @Test
    fun test_controller_global_move_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Tempo
        val key_a = 0
        val key_b = 2
        val event = OpusTempoEvent(1f)

        manager.controller_global_split_tree(type, key_a, listOf(), 3)
        manager.controller_global_split_tree(type, key_b, listOf(), 3)
        for (i in 0 until 3) {
            manager.controller_global_split_tree(type, key_a, listOf(i), 3)
            manager.controller_global_split_tree(type, key_b, listOf(i), 3)
        }

        manager.controller_global_set_event(type, key_a, listOf(0,0), event)
        manager.controller_global_move_leaf(type, key_a, listOf(0,0), key_a, listOf(0, 2))
        assertEquals(
            "controller_global_move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_global_ctl_tree<OpusTempoEvent>(type, key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_global_ctl_tree<OpusTempoEvent>(type, key_a, listOf(0,2)).has_event()
        )

        manager.controller_global_move_leaf(type, key_a, listOf(0,2), key_b, listOf())
        assertEquals(
            "controller_global_move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_global_ctl_tree<OpusTempoEvent>(type, key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_global_ctl_tree<OpusTempoEvent>(type, key_b, listOf()).has_event()
        )
    }

    @Test
    fun test_controller_channel_move_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Reverb
        val channel = 0
        val key_a = 0
        val key_b = 2
        val event = OpusReverbEvent(2f)

        manager.controller_channel_split_tree(type, channel, key_a, listOf(), 3)
        manager.controller_channel_split_tree(type, channel, key_b, listOf(), 3)
        for (i in 0 until 3) {
            manager.controller_channel_split_tree(type, channel, key_a, listOf(i), 3)
            manager.controller_channel_split_tree(type, channel, key_b, listOf(i), 3)
        }

        manager.controller_channel_set_event(type, channel, key_a, listOf(0,0), event)
        manager.controller_channel_move_leaf(type, channel, key_a, listOf(0,0), channel, key_a, listOf(0, 2))
        assertEquals(
            "controller_channel_move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_channel_ctl_tree<OpusReverbEvent>(type, channel, key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_channel_ctl_tree<OpusReverbEvent>(type, channel, key_a, listOf(0,2)).has_event()
        )

        manager.controller_channel_move_leaf(type, channel, key_a, listOf(0,2), channel, key_b, listOf())
        assertEquals(
            "controller_channel_move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_channel_ctl_tree<OpusReverbEvent>(type, channel, key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_channel_ctl_tree<OpusReverbEvent>(type, channel, key_b, listOf()).has_event()
        )
    }

    @Test
    fun test_controller_line_move_leaf() {
        val manager = OpusManager()
        manager._project_change_new()
        val type = EffectType.Volume
        val key_a = BeatKey(0,0,0)
        val key_b = BeatKey(0,0,2)
        val event = OpusVolumeEvent(1F)

        manager.controller_line_split_tree(type, key_a, listOf(), 3)
        manager.controller_line_split_tree(type, key_b, listOf(), 3)
        for (i in 0 until 3) {
            manager.controller_line_split_tree(type, key_a, listOf(i), 3)
            manager.controller_line_split_tree(type, key_b, listOf(i), 3)
        }

        manager.controller_line_set_event(type, key_a, listOf(0,0), event)
        manager.controller_line_move_leaf(type, key_a, listOf(0,0), key_a, listOf(0, 2))
        assertEquals(
            "controller_line_move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_line_ctl_tree<OpusVolumeEvent>(type, key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_line_ctl_tree<OpusVolumeEvent>(type, key_a, listOf(0,2)).has_event()
        )

        manager.controller_line_move_leaf(type, key_a, listOf(0,2), key_b, listOf())
        assertEquals(
            "controller_line_move_leaf() shouldn't remove the original leaf. should only unset it",
            3,
            manager.get_line_ctl_tree<OpusVolumeEvent>(type, key_a, listOf(0)).size
        )
        assertTrue(
            manager.get_line_ctl_tree<OpusVolumeEvent>(type, key_b, listOf()).has_event()
        )
    }

    @Test
    fun test_get_ordered_beat_key_pair() {
        assertEquals(
            Pair(
                BeatKey(0,0,0),
                BeatKey(2,4,6)
            ),
            get_ordered_beat_key_pair(
                BeatKey(0,0,0),
                BeatKey(2,4,6)
            )
        )

        assertEquals(
            Pair(
                BeatKey(0,0,0),
                BeatKey(2,4,6)
            ),
            get_ordered_beat_key_pair(
                BeatKey(2,4,6),
                BeatKey(0,0,0)
            )
        )

        assertEquals(
            Pair(
                BeatKey(0,2,1),
                BeatKey(0,3,4)
            ),
            get_ordered_beat_key_pair(
                BeatKey(0, 2, 4),
                BeatKey(0, 3, 1)
            )
        )
    }

    @Test
    fun test_get_channel_count() {
        val manager = OpusManager()
        manager._project_change_new()
        val original_channel_count = manager.channels.size
        var line_count = original_channel_count // Start with 1 line each

        for (i in 0 until 10) {
            manager.new_channel(i)
            assertEquals(
                i + original_channel_count + 1,
                manager.get_channel_count()
            )

            line_count += 1

            for (j in 0 until 3) {
                manager.new_line(i)
                line_count += 1
                assertEquals(
                    line_count,
                    manager.get_total_line_count()
                )
            }
        }
    }

    @Test
    fun test_get_preceding_event() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 4)
        manager.set_event(BeatKey(0,0,0), listOf(1), AbsoluteNoteEvent(0))
        manager.set_event(BeatKey(0,0,0), listOf(3), AbsoluteNoteEvent(1))
        assertEquals(
            AbsoluteNoteEvent(1),
            manager.get_preceding_event(BeatKey(0,0,0), listOf(3))
        )

        assertEquals(
            AbsoluteNoteEvent(0),
            manager.get_preceding_event(BeatKey(0,0,0), listOf(2))
        )

        assertNull(
            manager.get_preceding_event(BeatKey(0,0,0), listOf(0))
        )
    }

    @Test
    fun test_overwrite_line() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(24)
        val event = AbsoluteNoteEvent(5)
        manager.set_event(BeatKey(0,0,12), listOf(), event)

        manager.overwrite_line(0, 0, BeatKey(0,0,12))

        for (i in 0 until 12) {
            assertNotEquals(
                manager.get_tree(BeatKey(0,0,12)),
                manager.get_tree(BeatKey(0,0,i))
            )
        }
        for (i in 13 until 24) {
            assertEquals(
                manager.get_tree(BeatKey(0,0,12)),
                manager.get_tree(BeatKey(0,0,i))
            )
        }

        val event_b = AbsoluteNoteEvent(6)
        manager.set_event(BeatKey(0,0,0), listOf(), event_b)
        manager.overwrite_line(0, 0, BeatKey(0,0,0))

        for (i in 1 until 24) {
            assertEquals(
                manager.get_tree(BeatKey(0,0,0)),
                manager.get_tree(BeatKey(0,0,i))
            )
        }

        manager.new_line(0)
        manager.new_channel(1)
    }

    @Test
    fun test_get_beatkeys_in_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_channel(1)
        manager.new_channel(1)
        manager.new_line(0)
        manager.new_line(0)
        manager.new_line(1)
        manager.new_line(1)
        manager.new_line(1)
        manager.new_line(2)
        manager.new_line(2)
        manager.new_line(2)
        manager.new_line(2)
        manager.new_line(3)
        manager.new_line(3)
        manager.new_line(3)
        manager.new_line(3)
        manager.set_beat_count(12)

        val cases: List<Pair<BeatKey, BeatKey>> = listOf(
            Pair(BeatKey(0, 0, 0), BeatKey(2, 0, 11)),
            Pair(BeatKey(1, 0, 1), BeatKey(1, 0, 2)),
            Pair(BeatKey(1, 0, 5), BeatKey(3, 0, 2)),
        )

        cases.forEachIndexed { c: Int, case: Pair<BeatKey, BeatKey> ->
            val expectation: MutableList<BeatKey> = mutableListOf()
            manager.channels.forEachIndexed channel_loop@{ i: Int, channel ->
                if (!(case.first.channel .. case.second.channel).contains(i)) {
                    return@channel_loop
                }

                channel.lines.forEachIndexed line_offset_loop@{ j: Int, line ->
                    if (case.first.channel == case.second.channel) {
                        if (!(case.first.line_offset .. case.second.line_offset).contains(j)) {
                            return@line_offset_loop
                        }
                    } else if (case.first.channel == i) {
                        if (case.first.line_offset > j) {
                            return@line_offset_loop
                        }
                    } else if (case.second.channel == i) {
                        if (case.second.line_offset < j) {
                            return@line_offset_loop
                        }
                    } else if (!(case.first.channel + 1 until case.second.channel).contains(i)) {
                        return@line_offset_loop
                    }

                    line.beats.forEachIndexed beat_loop@{ k: Int, beat_tree ->
                        if ((case.first.beat .. case.second.beat).contains(k)) {
                            expectation.add(BeatKey(i,j,k))
                        }
                    }
                }
            }

            val reality = manager.get_beatkeys_in_range(case.first, case.second)
            assertEquals(expectation, reality)

        }
    }

   // @Test
   // fun test_get_shallow_representation() {
   //     val manager = OpusManager()
   //     val json_string_test = """{
   //         "k0": 101,
   //         "k1": "TEST STRING",
   //         "k2": {
   //             "sub_k0": [ 0, 1, 2 ],
   //             "sub_k1": 2.0
   //         },
   //         "k3": null,
   //         "k4": true,
   //         "k5": false
   //     }"""

   //     val test_map = get_shallow_representation(json_string_test)
   //     assertEquals(
   //         setOf("k0", "k1", "k2", "k3", "k4", "k5"),
   //         test_map.keys.toSet()
   //     )
   // }

    @Test
    fun test_overwrite_beat_range_horizontally() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_line(0)
        manager.new_channel(1)
        manager.new_line(1)
        manager.new_line(1)
        manager.set_beat_count(12)

        for (c in 0 until 2) {
            for (l in 0 until 2) {
                for (b in 0 until 3) {
                    manager.set_event(BeatKey(c, l, b), listOf(), AbsoluteNoteEvent((c * 6) + (l * 3) + b))
                }
            }
        }
        // -------------------------------------------------------
        manager.overwrite_beat_range_horizontally(0, 0, BeatKey(0, 0, 0), BeatKey(1, 2, 2))
        for (k in 0 until 4) {
            for (c in 0 until 2) {
                for (l in 0 until 2) {
                    for (b in 0 until 3) {
                        assertEquals(
                            manager.get_tree(BeatKey(c, l, b)),
                            manager.get_tree(BeatKey(c, l, (k * 3) + b))
                        )
                    }
                }
            }
        }
    }

    @Test
    fun test_controller_line_overwrite_range_horizontally() {
        val type = EffectType.Volume
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)

        // Only works on single line currently.
        val c = 0
        val l = 0

        for (b in 0 until 3) {
            val beat_key = BeatKey(c, l, b)
            val value = ((c * 6) + (l * 3) + b)
            manager.controller_line_set_event(
                type,
                beat_key,
                listOf(),
                OpusVolumeEvent(value.toFloat())
            )
            println("$beat_key => $value")
        }

        // -------------------------------------------------------
        manager.controller_line_overwrite_range_horizontally(type, 0, 0, BeatKey(0, 0, 0), BeatKey(0, 0, 2))
        println("--------------------------")
        for (k in 0 until 4) {
            for (b in 0 until 3) {
                val key_a = BeatKey(c, l, b)
                val key_b = BeatKey(c, l, (k * 3) + b)
                assertEquals(
                    "$key_a != $key_b",
                    manager.get_line_ctl_tree<OpusVolumeEvent>(type, key_a).get_event(),
                    manager.get_line_ctl_tree<OpusVolumeEvent>(type, key_b).get_event()
                )
            }
        }
    }

    @Test
    fun test_controller_global_overwrite_range_horizontally() {
        val manager = OpusManager()
        val type = EffectType.Tempo
        manager._project_change_new()
        manager.set_beat_count(12)

        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(5F))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(6F))

        manager.controller_global_overwrite_range_horizontally(type, 0, 1)

        for (i in 0 until 6) {
            assertEquals(
                manager.get_global_ctl_tree<OpusTempoEvent>(type, 0, listOf()).event,
                manager.get_global_ctl_tree<OpusTempoEvent>(type, (i * 2), listOf()).event,
            )
            assertEquals(
                manager.get_global_ctl_tree<OpusTempoEvent>(type, 1, listOf()).event,
                manager.get_global_ctl_tree<OpusTempoEvent>(type, 1 + (i * 2), listOf()).event,
            )
        }
    }

    @Test
    fun test_controller_channel_overwrite_range_horizontally() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.set_beat_count(12)

        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(5F))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(6f))

        manager.controller_channel_overwrite_range_horizontally(type, 0, 0, 0, 1)

        for (i in 0 until 6) {
            assertEquals(
                manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, 0, listOf()).event,
                manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, (i * 2), listOf()).event,
            )
            assertEquals(
                manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, 1, listOf()).event,
                manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, 1 + (i * 2), listOf()).event,
            )
        }
    }


    @Test
    fun test_unset_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_channel(1)
        manager.set_beat_count(12)
        for (c in 0 until 2) {
            for (l in 0 until manager.channels[c].lines.size) {
                for (b in 0 until 12) {
                    manager.set_event(BeatKey(c, l, b), listOf(), AbsoluteNoteEvent(12))
                }
            }
        }

        val first_beat_key = BeatKey(0, 0, 4)
        val second_beat_key = BeatKey(1,0, 10)
        manager.unset_range(first_beat_key, second_beat_key)

        for (beat_key in manager.get_beatkeys_in_range(first_beat_key, second_beat_key)) {
            assertFalse(
                manager.get_tree(beat_key).has_event()
            )
        }

    }

    @Test
    fun test_controller_line_unset_range() {
        val type = EffectType.Volume
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_channel(1)
        manager.set_beat_count(12)
        for (c in 0 until 2) {
            for (l in 0 until manager.channels[c].lines.size) {
                for (b in 0 until 12) {
                    manager.controller_line_set_event(type, BeatKey(c, l, b), listOf(), OpusVolumeEvent(.64f))
                }
            }
        }

        val first_beat_key = BeatKey(0, 0, 4)
        val second_beat_key = BeatKey(1,0, 10)
        manager.controller_line_unset_range(type, first_beat_key, second_beat_key)

        for (beat_key in manager.get_beatkeys_in_range(first_beat_key, second_beat_key)) {
            assertFalse(
                manager.get_line_ctl_tree<OpusVolumeEvent>(type, beat_key).has_event()
            )
        }
    }

    @Test
    fun test_controller_channel_unset_range() {
        val type = EffectType.Volume
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.new_channel(1)
        manager.set_beat_count(12)
        for (b in 0 until 12) {
            manager.controller_channel_set_event(type, 0, b, listOf(), OpusVolumeEvent(.64f))
        }

        manager.controller_channel_unset_range(type, 0, 4, 10)

        for (i in 0 until 4) {
            assertEquals(
                OpusVolumeEvent(.64f),
                manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, i).get_event()
            )
        }
        for (i in 4 .. 10) {
            assertFalse(
                manager.get_channel_ctl_tree<OpusVolumeEvent>(type, 0, i).has_event()
            )
        }
        
    }

    @Test
    fun test_controller_global_unset_range() {
        val type = EffectType.Tempo
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        for (b in 0 until 12) {
            manager.controller_global_set_event(type, b, listOf(), OpusTempoEvent(25F))
        }

        manager.controller_global_unset_range(type, 4, 10)

        for (i in 0 until 4) {
            assertEquals(
                OpusTempoEvent(25F),
                manager.get_global_ctl_tree<OpusTempoEvent>(type, i).get_event()
            )
        }
        for (i in 4 .. 10) {
            assertFalse(
                manager.get_global_ctl_tree<OpusTempoEvent>(type, i).has_event()
            )
        }
        
    }

    @Test
    fun test_get_ctl_info() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_channel(1)
        manager.set_beat_count(12)

        assertEquals(
            EffectType.Volume,
            manager.get_ctl_line_type(1)
        )

        assertEquals(
            EffectType.Tempo,
            manager.get_ctl_line_type(6)
        )

        assertEquals(
            CtlLineLevel.Global,
            manager.ctl_line_level(6)
        )

        assertEquals(
            CtlLineLevel.Line,
            manager.ctl_line_level(1)
        )

        assertEquals(
            Triple(0, null, null),
            manager.get_ctl_line_info(0)
        )

        assertEquals(
            Triple(0, CtlLineLevel.Line, EffectType.Volume),
            manager.get_ctl_line_info(1)
        )

        assertEquals(
            Triple(-1, CtlLineLevel.Global, EffectType.Tempo),
            manager.get_ctl_line_info(6)
        )
    }

    @Test
    fun test_set_tag() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.insert_beats(0, 12)
        val tag_name = "Test Tag Name"
        val tag_position = 10

        manager.tag_section(tag_position, tag_name)
        assertEquals(
            listOf(Pair(tag_position, tag_name)),
            manager.get_marked_sections()
        )
        for (i in 0 until manager.length) {
            assertEquals(
                i == tag_position,
                manager.is_beat_tagged(i)
            )
        }

        manager.remove_beat(0)
        assertEquals(
            listOf(Pair(tag_position - 1, tag_name)),
            manager.get_marked_sections()
        )

        manager.remove_beat(0, 3)
        assertEquals(
            listOf(Pair(tag_position - 4, tag_name)),
            manager.get_marked_sections()
        )

        manager.insert_beat(4)
        assertEquals(
            listOf(Pair(tag_position - 3, tag_name)),
            manager.get_marked_sections()
        )

        manager.insert_beats(4, 3)
        assertEquals(
            listOf(Pair(tag_position, tag_name)),
            manager.get_marked_sections()
        )

        manager.remove_tagged_section(tag_position)
        assert(manager.get_marked_sections().isEmpty())

        manager.tag_section(tag_position, null)
        assertEquals(
            listOf(Pair(tag_position, null)),
            manager.get_marked_sections()
        )

        manager.remove_beat(tag_position)
        assert(manager.get_marked_sections().isEmpty())
    }

    //@Test
    //fun test_merge_leafs() {
    //    val manager = OpusManager()
    //    manager.new()
    //    var key_a = BeatKey(0,0,0)
    //    var key_b = BeatKey(0,0,1)
    //    var key_c = BeatKey(0,0,2)
    //    var key_d = BeatKey(0,0,3)

    //    manager.split_tree(key_a, listOf(), 2)
    //    manager.split_tree(key_b, listOf(), 3)
    //    manager.set_event(key_a, listOf(0), AbsoluteNoteEvent(10))
    //    manager.set_event(key_a, listOf(1), AbsoluteNoteEvent(11))
    //    manager.set_event(key_b, listOf(0), AbsoluteNoteEvent(12))
    //    manager.set_event(key_b, listOf(1), AbsoluteNoteEvent(13))
    //    manager.set_event(key_b, listOf(2), AbsoluteNoteEvent(14))

    //    assertThrows(OpusManager.InvalidMergeException::class.java) {
    //        manager.merge_leafs(key_a, listOf(), key_b, listOf())
    //    }
    //    assertThrows(OpusManager.InvalidMergeException::class.java) {
    //        manager.merge_leafs(key_a, listOf(), key_a, listOf())
    //    }

    //    manager.unset(key_b, listOf(0))
    //    manager.merge_leafs(key_a, listOf(), key_b, listOf())
    //    assertTrue(manager.get_tree(key_a).is_leaf() && !manager.get_tree(key_a).has_event())
    //    assertEquals(2, manager.get_tree(key_b).size)
    //    assertEquals(3, manager.get_tree(key_b, listOf(0)).size)
    //    assertEquals(3, manager.get_tree(key_b, listOf(1)).size)
    //    assertEquals(
    //        AbsoluteNoteEvent(10),
    //        manager.get_tree(key_b, listOf(0, 0)).event
    //    )
    //    assertEquals(
    //        AbsoluteNoteEvent(11),
    //        manager.get_tree(key_b, listOf(1, 0)).event
    //    )
    //    assertEquals(
    //        AbsoluteNoteEvent(13),
    //        manager.get_tree(key_b, listOf(0, 2)).event
    //    )
    //    assertEquals(
    //        AbsoluteNoteEvent(14),
    //        manager.get_tree(key_b, listOf(1, 1)).event
    //    )

    //    manager.set_event(key_c, listOf(), AbsoluteNoteEvent(15))
    //    manager.merge_leafs(key_c, listOf(), key_d, listOf())

    //    assertTrue(manager.get_tree(key_d).has_event())

    //    assertEquals(
    //        AbsoluteNoteEvent(15),
    //        manager.get_tree(key_d).event
    //    )
    //}

}
