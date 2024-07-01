package com.qfs.pagan

import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.qfs.pagan.opusmanager.OpusLayerHistory as OpusManager

class HistoryCacheUnitTest {
    fun undo_and_check(manager: OpusManager, callback: (OpusManager) -> Unit) {
        manager.clear_history()

        val original = OpusManager()
        original.import_from_other(manager)
        callback(manager)
        try {
            manager.apply_undo()
        } catch (e: Exception) {
            throw e
        }
        assertEquals(
            "Undo Didn't Work Correctly",
            original,
            manager
        )

        assertTrue(
            "Some actions weren't applied",
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_set_project_name() {
        var manager = OpusManager()
        manager.new()
        this.undo_and_check(manager) {
            it.set_project_name("Test Project Name")
        }
    }

    @Test
    fun test_remove() {
        var key = BeatKey(0,0,0)
        var test_event = AbsoluteNoteEvent(12)

        var manager = OpusManager()
        manager.new()

        manager.split_tree(key, listOf(), 2)
        manager.split_tree(key, listOf(1), 3)
        manager.set_event(key, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.remove(key, listOf(1,2), 3)
        }

        this.undo_and_check(manager) {
            it.remove(key, listOf(1,0), 1)
        }
    }

    @Test
    fun test_remove_global_ctl() {
        var key = 0
        var test_event = OpusTempoEvent(25F)
        val type = ControlEventType.Tempo

        var manager = OpusManager()
        manager.new()

        manager.split_global_ctl_tree(type, key, listOf(), 2)
        manager.split_global_ctl_tree(type, key, listOf(1), 3)
        manager.set_global_ctl_event(type, key, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.remove_global_ctl(type, key, listOf(1,2), 3)
        }

        this.undo_and_check(manager) {
            it.remove_global_ctl(type, key, listOf(1,0), 1)
        }
    }

    @Test
    fun test_remove_channel_ctl() {
        var key = 0
        var test_event = OpusVolumeEvent(25)
        val type = ControlEventType.Volume

        var manager = OpusManager()
        manager.new()

        manager.split_channel_ctl_tree(type, key, 0, listOf(), 2)
        manager.split_channel_ctl_tree(type, key, 0, listOf(1), 3)
        manager.set_channel_ctl_event(type, key, 0, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.remove_channel_ctl(type, key, 0, listOf(1,2), 3)
        }

        this.undo_and_check(manager) {
            it.remove_channel_ctl(type, key, 0, listOf(1,0), 1)
        }
    }

    @Test
    fun test_remove_line_ctl() {
        var key = BeatKey(0,0,0)
        var test_event = OpusVolumeEvent(25)
        val type = ControlEventType.Volume

        var manager = OpusManager()
        manager.new()

        manager.split_line_ctl_tree(type, key, listOf(), 2)
        manager.split_line_ctl_tree(type, key, listOf(1), 3)
        manager.set_line_ctl_event(type, key, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.remove_line_ctl(type, key, listOf(1,2), 3)
        }

        this.undo_and_check(manager) {
            it.remove_line_ctl(type, key, listOf(1,0), 1)
        }
    }

    @Test
    fun test_convert_event_to_relative() {
        //var manager = OpusManager()
        //manager.new()
        //manager.set_event(BeatKey(0,0,0), listOf(), OpusEvent(12,12,0,false))
        //manager.set_event(BeatKey(0,0,1), listOf(), OpusEvent(24,12,0,false))
        //manager.convert_event_to_relative(BeatKey(0,0,1), listOf())
        //manager.apply_undo()
        //TODO("test_convert_event_to_relative")
    }

    @Test
    fun test_convert_event_to_absolute() {
        //TODO("test_convert_event_to_absolute")
    }

    @Test
    fun test_set_percussion_event() {
        var manager = OpusManager()
        manager.new()

        println("CHANNELs: ${manager.get_channel_count()}")
        this.undo_and_check(manager) {
            it.set_percussion_event(BeatKey(1,0,0), listOf())
        }
        manager.set_percussion_event(BeatKey(1,0,0), listOf())
        this.undo_and_check(manager) {
            it.unset(BeatKey(1,0,0), listOf())
        }
    }

    @Test
    fun test_set_event() {
        var event = AbsoluteNoteEvent(12)
        var event_b = AbsoluteNoteEvent(5)
        var manager = OpusManager()
        manager.new()

        manager.set_event(BeatKey(0,0,0), listOf(), event_b)

        this.undo_and_check(manager) {
            it.set_event(BeatKey(0,0,0), listOf(), event)
        }
    }

    @Test
    fun test_unset() {
        var event = AbsoluteNoteEvent(12)
        var manager = OpusManager()
        manager.new()
        manager.set_event(BeatKey(0,0,0), listOf(), event)

        this.undo_and_check(manager) {
            it.unset(BeatKey(0,0,0), listOf())
        }
    }

    @Test
    fun test_new_channel() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.new_channel()
        }
   }

    @Test
    fun test_remove_beat() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.remove_beat(0)
        }
    }

    @Test
    fun test_remove_channel() {
        var manager = OpusManager()
        manager.new()
        manager.new_channel()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        this.undo_and_check(manager) {
            it.remove_channel(0)
        }

    }

    @Test
    fun test_remove_line() {
        var manager = OpusManager()
        manager.new()
        manager.new_line(0,0)
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)

        manager.new_line(0,0,4)
        this.undo_and_check(manager) {
            it.remove_line(0,0,4)
        }

    }

    @Test
    fun test_new_line() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.new_line(0, 0, 10)
        }

    }

    @Test
    fun test_swap_lines() {
        var manager = OpusManager() 
        manager.new()
        manager.new_line(0)
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(0))
        manager.set_event(BeatKey(0,1,0), listOf(), AbsoluteNoteEvent(1))

        this.undo_and_check(manager) {
            it.swap_lines(0, 0, 0, 1)
        }
    }

    @Test
    fun test_replace_tree() {
        var manager = OpusManager()
        manager.new()
        var new_tree = OpusTree<TunedInstrumentEvent>()
        new_tree.set_size(5)

        this.undo_and_check(manager) {
            it.replace_tree(BeatKey(0,0,0), listOf(), new_tree)
        }
    }


    @Test
    fun test_insert_after() {
        var manager = OpusManager()
        manager.new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)

        this.undo_and_check(manager) { om: OpusManager ->
            om.insert_after(BeatKey(0,0,0), listOf(0), 1)
        }
    }

    @Test
    fun test_split_tree() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.split_tree(BeatKey(0,0,0), listOf(), 3)
        }
    }

    @Test
    fun test_split_channel_ctl_tree() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.split_channel_ctl_tree(ControlEventType.Volume, 0, 0, listOf(), 3)
        }
    }

    @Test
    fun test_split_global_ctl_tree() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.split_global_ctl_tree(ControlEventType.Tempo, 0, listOf(), 3)
        }
    }

    @Test
    fun test_split_line_ctl_tree() {
        var manager = OpusManager()
        manager.new()
        this.undo_and_check(manager) {
            it.split_line_ctl_tree(ControlEventType.Volume, BeatKey(0,0,0), listOf(), 3)
        }
    }

    @Test
    fun test_insert() {
        var manager = OpusManager()
        manager.new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)

        this.undo_and_check(manager) {
            it.insert(BeatKey(0,0,0), listOf(0), 1)
        }

    }

    @Test
    fun test_set_tuning_map() {
        var manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            val tuning_map = it.tuning_map.clone()
            tuning_map[0] = Pair(4, 36)
            it.set_tuning_map_and_transpose(
                tuning_map,
                10
            )
        }
    }

    @Test
    fun test_merge_leafs() {
        var manager = OpusManager()
        manager.new()

        var key_a = BeatKey(0,0,0)
        var key_b = BeatKey(0,0,1)

        manager.split_tree(key_a, listOf(), 2)
        manager.split_tree(key_b, listOf(), 3)
        manager.set_event(key_a, listOf(0), AbsoluteNoteEvent(10))
        manager.set_event(key_a, listOf(1), AbsoluteNoteEvent(11))
        manager.set_event(key_b, listOf(1), AbsoluteNoteEvent(13))
        manager.set_event(key_b, listOf(2), AbsoluteNoteEvent(14))

        this.undo_and_check(manager) {
            manager.merge_leafs(key_a, listOf(), key_b, listOf())
        }
    }

    @Test
    fun test_set_initial_events() {
        val manager = OpusManager()
        manager.new()
        val initial_line_event = manager.get_line_controller_initial_event(ControlEventType.Volume, 0, 0)
        val initial_channel_event = manager.get_channel_controller_initial_event(ControlEventType.Volume, 0)
        val initial_global_event = manager.get_global_controller_initial_event(ControlEventType.Tempo)

        this.undo_and_check(manager) {
            it.set_line_controller_initial_event(ControlEventType.Volume, 0, 0, OpusVolumeEvent(25))
        }

        this.undo_and_check(manager) {
            it.set_channel_controller_initial_event(ControlEventType.Volume, 0, OpusVolumeEvent(25))
        }

        this.undo_and_check(manager) {
            it.set_global_controller_initial_event(ControlEventType.Tempo, OpusTempoEvent(60F))
        }
    }

    @Test
    fun test_overwrite_channel_ctl_range_horizontally() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val type = ControlEventType.Volume
        manager.set_channel_ctl_event(type, 0, 0, listOf(), OpusVolumeEvent(24))
        manager.set_channel_ctl_event(type, 0, 1, listOf(), OpusVolumeEvent(24))

        this.undo_and_check(manager) {
            it.overwrite_channel_ctl_range_horizontally(type, 0, 0, 1)
        }
    }

    @Test
    fun test_overwrite_global_ctl_range_horizontally() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val type = ControlEventType.Tempo
        manager.set_global_ctl_event(type, 0, listOf(), OpusTempoEvent(24f))
        manager.set_global_ctl_event(type, 1, listOf(), OpusTempoEvent(24f))

        this.undo_and_check(manager) {
            it.overwrite_global_ctl_range_horizontally(type, 0, 1)
        }
    }

    @Test
    fun test_overwrite_line_ctl_range_horizontally() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val type = ControlEventType.Volume
        manager.set_line_ctl_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(24))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(24))

        this.undo_and_check(manager) {
            it.overwrite_line_ctl_range_horizontally(type, 0, 0, BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_overwrite_beat_range_horizontally() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val event = AbsoluteNoteEvent(24)
        manager.set_event(BeatKey(0, 0, 0), listOf(), event)
        manager.set_event(BeatKey(0, 0, 1), listOf(), event)

        this.undo_and_check(manager) {
            it.overwrite_beat_range_horizontally(0, 0, BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_overwrite_row() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val event = AbsoluteNoteEvent(24)
        manager.set_event(BeatKey(0,0,0), listOf(), event)
        this.undo_and_check(manager) {
            it.overwrite_row(0, 0, BeatKey(0,0,0))
        }
    }
    @Test
    fun test_overwrite_channel_ctl_row() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val type = ControlEventType.Volume
        manager.set_channel_ctl_event(type, 0, 0, listOf(), OpusVolumeEvent(24))

        this.undo_and_check(manager) {
            it.overwrite_channel_ctl_row(type, 0, 0, 1)
        }
    }

    @Test
    fun test_overwrite_global_ctl_row() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val type = ControlEventType.Tempo
        manager.set_global_ctl_event(type, 0, listOf(), OpusTempoEvent(24f))

        this.undo_and_check(manager) {
            it.overwrite_global_ctl_row(type, 0)
        }
    }

    @Test
    fun test_overwrite_line_ctl_row() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        val type = ControlEventType.Volume
        manager.set_line_ctl_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(24))
        this.undo_and_check(manager) {
            it.overwrite_line_ctl_row(type, 0, 0, BeatKey(0, 0, 0))
        }
    }

    @Test
    fun test_set_duration() {
        val manager = OpusManager()
        manager.new()
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(24))
        this.undo_and_check(manager) {
            it.set_duration(BeatKey(0,0,0), listOf(), 3)
        }
    }

    @Test
    fun test_insert_after_global_ctl() {
        val manager = OpusManager()
        manager.new()
        manager.split_global_ctl_tree(ControlEventType.Tempo, 0, listOf(), 2)
        this.undo_and_check(manager) {
            it.insert_after_global_ctl(ControlEventType.Tempo, 0, listOf(0), 3)
        }
    }

    @Test
    fun test_insert_after_channel_ctl() {
        val manager = OpusManager()
        manager.new()
        manager.split_channel_ctl_tree(ControlEventType.Volume, 0, 0, listOf(), 2)
        this.undo_and_check(manager) {
            it.insert_after_channel_ctl(ControlEventType.Volume, 0, 0, listOf(0), 3)
        }
    }

    @Test
    fun test_insert_after_line_ctl() {
        val manager = OpusManager()
        manager.new()
        manager.split_line_ctl_tree(ControlEventType.Volume, BeatKey(0, 0, 0), listOf(), 2)
        this.undo_and_check(manager) {
            it.insert_after_line_ctl(ControlEventType.Volume, BeatKey(0, 0, 0), listOf(0), 3)
        }
    }

    @Test
    fun test_insert_beat() {
        val manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.insert_beats(0, 4)
        }
        manager.insert_beats(0, 4)

        this.undo_and_check(manager) {
            it.remove_beat(0, 4)
        }
    }

    @Test
    fun test_move_leaf() {
        val manager = OpusManager()
        manager.new()

        manager.set_event(BeatKey(0, 0, 0), listOf(), AbsoluteNoteEvent(15))
        manager.set_event(BeatKey(0, 0, 1), listOf(), AbsoluteNoteEvent(16))
        manager.set_event(BeatKey(0, 0, 2), listOf(), AbsoluteNoteEvent(17))

        this.undo_and_check(manager) {
            it.move_leaf(BeatKey(0, 0, 0), listOf(), BeatKey(0, 0, 1), listOf())
        }
    }

    @Test
    fun test_move_line_ctl_leaf() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()

        manager.set_line_ctl_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(15))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(16))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(17))

        this.undo_and_check(manager) {
            it.move_line_ctl_leaf(type, BeatKey(0, 0, 0), listOf(), BeatKey(0, 0, 1), listOf())
        }
    }

    @Test
    fun test_move_channel_ctl_leaf() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()

        manager.set_channel_ctl_event(type, 0, 0, listOf(), OpusVolumeEvent(15))
        manager.set_channel_ctl_event(type, 0, 1, listOf(), OpusVolumeEvent(16))
        manager.set_channel_ctl_event(type, 0, 2, listOf(), OpusVolumeEvent(17))

        this.undo_and_check(manager) {
            it.move_channel_ctl_leaf(type, 0, 0, listOf(), 0, 1, listOf())
        }
    }

    @Test
    fun test_move_global_ctl_leaf() {
        val manager = OpusManager()
        val type = ControlEventType.Tempo
        manager.new()

        manager.set_global_ctl_event(type, 0, listOf(), OpusTempoEvent(15f))
        manager.set_global_ctl_event(type, 1, listOf(), OpusTempoEvent(16f))
        manager.set_global_ctl_event(type, 2, listOf(), OpusTempoEvent(17f))

        this.undo_and_check(manager) {
            it.move_global_ctl_leaf(type, 0, listOf(), 1, listOf())
        }
    }

    @Test
    fun test_move_beat_range() {
        val manager = OpusManager()
        manager.new()
        manager.set_event(BeatKey(0, 0, 0), listOf(), AbsoluteNoteEvent(10))
        manager.set_event(BeatKey(0, 0, 1), listOf(), AbsoluteNoteEvent(11))
        manager.set_event(BeatKey(0, 0, 2), listOf(), AbsoluteNoteEvent(13))
        manager.set_event(BeatKey(0, 0, 3), listOf(), AbsoluteNoteEvent(14))

        this.undo_and_check(manager) {
            it.move_beat_range(BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_move_line_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()
        manager.set_line_ctl_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(10))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(11))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(13))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 3), listOf(), OpusVolumeEvent(14))

        this.undo_and_check(manager) {
            it.move_line_ctl_range(type, BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_overwrite_line_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()
        manager.set_line_ctl_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(10))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(11))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(13))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 3), listOf(), OpusVolumeEvent(14))

        this.undo_and_check(manager) {
            it.overwrite_line_ctl_range(type, BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_move_global_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Tempo
        manager.new()
        manager.set_global_ctl_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.set_global_ctl_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.set_global_ctl_event(type, 2, listOf(), OpusTempoEvent(13F))
        manager.set_global_ctl_event(type, 3, listOf(), OpusTempoEvent(14F))

        this.undo_and_check(manager) {
            it.move_global_ctl_range(type, 2, 0, 1)
        }
    }
    @Test
    fun test_overwrite_global_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Tempo
        manager.new()
        manager.set_global_ctl_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.set_global_ctl_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.set_global_ctl_event(type, 2, listOf(), OpusTempoEvent(13F))
        manager.set_global_ctl_event(type, 3, listOf(), OpusTempoEvent(14F))

        this.undo_and_check(manager) {
            it.overwrite_global_ctl_range(type, 2, 0, 1)
        }
    }

    @Test
    fun test_move_channel_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()
        manager.set_channel_ctl_event(type, 0, 0, listOf(), OpusVolumeEvent(10))
        manager.set_channel_ctl_event(type, 0, 1, listOf(), OpusVolumeEvent(11))
        manager.set_channel_ctl_event(type, 0, 2, listOf(), OpusVolumeEvent(13))
        manager.set_channel_ctl_event(type, 0, 3, listOf(), OpusVolumeEvent(14))

        this.undo_and_check(manager) {
            it.move_channel_ctl_range(type, 0, 2, 0, 0, 1)
        }
    }

    @Test
    fun test_overwrite_channel_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()
        manager.set_channel_ctl_event(type, 0, 0, listOf(), OpusVolumeEvent(10))
        manager.set_channel_ctl_event(type, 0, 1, listOf(), OpusVolumeEvent(11))
        manager.set_channel_ctl_event(type, 0, 2, listOf(), OpusVolumeEvent(13))
        manager.set_channel_ctl_event(type, 0, 3, listOf(), OpusVolumeEvent(14))

        this.undo_and_check(manager) {
            it.overwrite_channel_ctl_range(type, 0, 0, 0, 2, 3)
        }
    }

    @Test
    fun test_unset_range() {
        val manager = OpusManager()
        manager.new()
        manager.set_event(BeatKey(0, 0, 0), listOf(), AbsoluteNoteEvent(10))
        manager.set_event(BeatKey(0, 0, 1), listOf(), AbsoluteNoteEvent(11))
        manager.set_event(BeatKey(0, 0, 2), listOf(), AbsoluteNoteEvent(13))
        manager.set_event(BeatKey(0, 0, 3), listOf(), AbsoluteNoteEvent(14))
        this.undo_and_check(manager) {
            it.unset_range(BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_unset_line_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()
        manager.set_line_ctl_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(10))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(11))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(13))
        manager.set_line_ctl_event(type, BeatKey(0, 0, 3), listOf(), OpusVolumeEvent(14))
        this.undo_and_check(manager) {
            it.unset_line_ctl_range(type, BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_unset_global_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Tempo
        manager.new()
        manager.set_global_ctl_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.set_global_ctl_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.set_global_ctl_event(type, 2, listOf(), OpusTempoEvent(13F))
        manager.set_global_ctl_event(type, 3, listOf(), OpusTempoEvent(14F))
        this.undo_and_check(manager) {
            it.unset_global_ctl_range(type, 1, 2)
        }
    }

    @Test
    fun test_unset_channel_ctl_range() {
        val manager = OpusManager()
        val type = ControlEventType.Volume
        manager.new()
        manager.set_channel_ctl_event(type, 0, 0, listOf(), OpusVolumeEvent(10))
        manager.set_channel_ctl_event(type, 0, 1, listOf(), OpusVolumeEvent(11))
        manager.set_channel_ctl_event(type, 0, 2, listOf(), OpusVolumeEvent(13))
        manager.set_channel_ctl_event(type, 0, 3, listOf(), OpusVolumeEvent(14))
        this.undo_and_check(manager) {
            it.unset_channel_ctl_range(type, 0, 1, 2)
        }
    }

    @Test
    fun test_set_percussion_instrument() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(1)
        manager.set_percussion_instrument(0, 8)
        manager.set_percussion_instrument(1, 15)
        this.undo_and_check(manager) {
            it.set_percussion_instrument(1, 21)
        }
    }

    @Test
    fun test_link_beat() {
        val manager = OpusManager()
        manager.new()

        this.undo_and_check(manager) {
            it.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        }

        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))

        this.undo_and_check(manager) {
            it.link_beats(BeatKey(0,0,0), BeatKey(0,0,2))
        }
        this.undo_and_check(manager) {
            it.link_beats(BeatKey(0,0,2), BeatKey(0,0,0))
        }

        manager.link_beats(BeatKey(0,0,2), BeatKey(0,0,3))

        this.undo_and_check(manager) {
            it.link_beats(BeatKey(0,0,0), BeatKey(0,0,2))
        }
    }

    @Test
    fun test_link_beat_range() {
        val manager = OpusManager()
        manager.new()
        this.undo_and_check(manager) {
            it.link_beat_range(BeatKey(0,0,2), BeatKey(0,0,0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_unlink_beat() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        this.undo_and_check(manager) {
            it.unlink_beat(BeatKey(0,0,0))
        }
    }

    @Test
    fun test_unlink_range() {
        val manager = OpusManager()
        manager.new()
        manager.link_beat_range(BeatKey(0,0,2), BeatKey(0,0,0), BeatKey(0,0,1))
        this.undo_and_check(manager) {
            it.unlink_range(BeatKey(0,0,2), BeatKey(0,0,3))
        }
    }

    @Test
    fun test_link_row() {
        val manager = OpusManager()
        manager.new()
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(20))
        this.undo_and_check(manager) {
            it.link_row(0, 0, BeatKey(0,0,0))
        }
    }

    @Test
    fun test_link_beat_range_horizontally() {
        val manager = OpusManager()
        manager.new()
        manager.set_beat_count(12)
        manager.new_channel()
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(20))
        manager.set_event(BeatKey(1,0,1), listOf(), AbsoluteNoteEvent(21))
        this.undo_and_check(manager) {
            it.link_beat_range_horizontally(0, 0, BeatKey(0,0,0), BeatKey(1,0,1))
        }
    }

    @Test
    fun test_set_channel_instrument() {
        val manager = OpusManager()
        manager.new()
        this.undo_and_check(manager) {
            manager.set_channel_instrument(0, Pair(0, 12))
        }
    }

}
