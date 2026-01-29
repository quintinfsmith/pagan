package com.qfs.pagan

import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.TunedInstrumentEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.qfs.pagan.structure.opusmanager.history.OpusLayerHistory as OpusManager

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
            manager.history_cache.is_empty()
        )
    }

    @Test
    fun test_set_project_name() {
        val manager = OpusManager()
        manager._project_change_new()
        this.undo_and_check(manager) {
            it.set_project_name("Test Project Name")
        }
    }

    @Test
    fun test_remove() {
        val key = BeatKey(0,0,0)
        val test_event = AbsoluteNoteEvent(12)

        val manager = OpusManager()
        manager._project_change_new()

        manager.split_tree(key, listOf(), 2)
        manager.split_tree(key, listOf(1), 3)
        manager.set_event(key, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.remove_repeat(key, listOf(1,2), 3)
        }

        this.undo_and_check(manager) {
            it.remove_repeat(key, listOf(1,0), 1)
        }
    }

    @Test
    fun test_controller_global_remove() {
        val key = 0
        val test_event = OpusTempoEvent(25F)
        val type = EffectType.Tempo

        val manager = OpusManager()
        manager._project_change_new()

        manager.controller_global_split_tree(type, key, listOf(), 2)
        manager.controller_global_split_tree(type, key, listOf(1), 3)
        manager.controller_global_set_event(type, key, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.controller_global_remove(type, key, listOf(1,2))
        }

        this.undo_and_check(manager) {
            it.controller_global_remove(type, key, listOf(1,0))
        }
    }

    @Test
    fun test_controller_channel_remove() {
        val key = 0
        val test_event = OpusVolumeEvent(.25f)
        val type = EffectType.Volume

        val manager = OpusManager()
        manager._project_change_new()

        manager.controller_channel_split_tree(type, key, 0, listOf(), 2)
        manager.controller_channel_split_tree(type, key, 0, listOf(1), 3)
        manager.controller_channel_set_event(type, key, 0, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.controller_channel_remove(type, key, 0, listOf(1,2))
        }

        this.undo_and_check(manager) {
            it.controller_channel_remove(type, key, 0, listOf(1,0))
        }
    }

    @Test
    fun test_controller_line_remove() {
        val key = BeatKey(0,0,0)
        val test_event = OpusVolumeEvent(.25f)
        val type = EffectType.Volume

        val manager = OpusManager()
        manager._project_change_new()

        manager.controller_line_split_tree(type, key, listOf(), 2)
        manager.controller_line_split_tree(type, key, listOf(1), 3)
        manager.controller_line_set_event(type, key, listOf(1, 0), test_event)

        this.undo_and_check(manager) {
            it.controller_line_remove(type, key, listOf(1,2), 3)
        }

        this.undo_and_check(manager) {
            it.controller_line_remove(type, key, listOf(1,0), 1)
        }
    }

    @Test
    fun test_set_percussion_event() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.percussion_set_event(BeatKey(1,0,0), listOf())
        }
        manager.percussion_set_event(BeatKey(1,0,0), listOf())
        this.undo_and_check(manager) {
            it.unset(BeatKey(1,0,0), listOf())
        }
    }

    @Test
    fun test_set_event() {
        val event = AbsoluteNoteEvent(12)
        val event_b = AbsoluteNoteEvent(5)
        val manager = OpusManager()
        manager._project_change_new()

        manager.set_event(BeatKey(0,0,0), listOf(), event_b)

        this.undo_and_check(manager) {
            it.set_event(BeatKey(0,0,0), listOf(), event)
        }
    }

    @Test
    fun test_unset() {
        val event = AbsoluteNoteEvent(12)
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_event(BeatKey(0,0,0), listOf(), event)

        this.undo_and_check(manager) {
            it.unset(BeatKey(0,0,0), listOf())
        }
    }

    @Test
    fun test_new_channel() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.new_channel()
        }
   }

    @Test
    fun test_remove_beat() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.remove_beat(0)
        }
    }

    @Test
    fun test_remove_channel() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_channel()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        this.undo_and_check(manager) {
            it.remove_channel(0)
        }

    }

    @Test
    fun test_remove_line() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0,0)
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)

        manager.new_line_repeat(0,0,4)
        this.undo_and_check(manager) {
            it.remove_line_repeat(0,0,4)
        }

    }

    @Test
    fun test_new_line() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.new_line_repeat(0, 0, 10)
        }

    }

    @Test
    fun test_swap_lines() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(0)
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(0))
        manager.set_event(BeatKey(0,1,0), listOf(), AbsoluteNoteEvent(1))

        this.undo_and_check(manager) {
            it.swap_lines(0, 0, 0, 1)
        }
    }

    @Test
    fun test_replace_tree() {
        val manager = OpusManager()
        manager._project_change_new()
        val new_tree = ReducibleTree<TunedInstrumentEvent>()
        new_tree.set_size(5)

        this.undo_and_check(manager) {
            it.replace_tree(BeatKey(0,0,0), listOf(), new_tree)
        }
    }


    @Test
    fun test_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)

        this.undo_and_check(manager) { om: OpusManager ->
            om.insert_after(BeatKey(0,0,0), listOf(0))
        }
    }

    @Test
    fun test_split_tree() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.split_tree(BeatKey(0,0,0), listOf(), 3)
        }
    }

    @Test
    fun test_split_channel_ctl_tree() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.controller_channel_split_tree(EffectType.Volume, 0, 0, listOf(), 3)
        }
    }

    @Test
    fun test_split_global_ctl_tree() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            it.controller_global_split_tree(EffectType.Tempo, 0, listOf(), 3)
        }
    }

    @Test
    fun test_split_line_ctl_tree() {
        val manager = OpusManager()
        manager._project_change_new()
        this.undo_and_check(manager) {
            it.controller_line_split_tree(EffectType.Volume, BeatKey(0,0,0), listOf(), 3)
        }
    }

    @Test
    fun test_insert() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)

        this.undo_and_check(manager) {
            it.insert_at_cursor(1)
        }

    }

    @Test
    fun test_set_tuning_map() {
        val manager = OpusManager()
        manager._project_change_new()

        this.undo_and_check(manager) {
            val tuning_map = it.tuning_map.clone()
            tuning_map[0] = Pair(4, 36)
            it.set_tuning_map_and_transpose(
                tuning_map,
                Pair(10, 12)
            )
        }
    }

    //@Test
    //fun test_merge_leafs() {
    //    var manager = OpusManager()
    //    manager.new()

    //    var key_a = BeatKey(0,0,0)
    //    var key_b = BeatKey(0,0,1)

    //    manager.split_tree(key_a, listOf(), 2)
    //    manager.split_tree(key_b, listOf(), 3)
    //    manager.set_event(key_a, listOf(0), AbsoluteNoteEvent(10))
    //    manager.set_event(key_a, listOf(1), AbsoluteNoteEvent(11))
    //    manager.set_event(key_b, listOf(1), AbsoluteNoteEvent(13))
    //    manager.set_event(key_b, listOf(2), AbsoluteNoteEvent(14))

    //    this.undo_and_check(manager) {
    //        manager.merge_leafs(key_a, listOf(), key_b, listOf())
    //    }
    //}

    @Test
    fun test_set_initial_events() {
        val manager = OpusManager()
        manager._project_change_new()
        val initial_line_event = manager.get_line_controller_initial_event<OpusVolumeEvent>(EffectType.Volume, 0, 0)
        val initial_channel_event = manager.get_channel_controller_initial_event<OpusVolumeEvent>(EffectType.Volume, 0)
        val initial_global_event = manager.get_global_controller_initial_event<OpusTempoEvent>(EffectType.Tempo)

        this.undo_and_check(manager) {
            it.controller_line_set_initial_event(EffectType.Volume, 0, 0, OpusVolumeEvent(.25f))
        }

        this.undo_and_check(manager) {
            it.controller_channel_set_initial_event(EffectType.Volume, 0, OpusVolumeEvent(.25f))
        }

        this.undo_and_check(manager) {
            it.controller_global_set_initial_event(EffectType.Tempo, OpusTempoEvent(60F))
        }
    }

    @Test
    fun test_controller_channel_overwrite_range_horizontally() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        val type = EffectType.Volume
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(.24f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(.24f))

        this.undo_and_check(manager) {
            it.controller_channel_overwrite_range_horizontally(type, 0, 0, 0, 1)
        }
    }

    @Test
    fun test_controller_global_overwrite_range_horizontally() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        val type = EffectType.Tempo
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(24f))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(24f))

        this.undo_and_check(manager) {
            it.controller_global_overwrite_range_horizontally(type, 0, 1)
        }
    }

    @Test
    fun test_controller_line_overwrite_range_horizontally() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        val type = EffectType.Volume
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.24f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.24f))

        this.undo_and_check(manager) {
            it.controller_line_overwrite_range_horizontally(type, 0, 0, BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_overwrite_beat_range_horizontally() {
        val manager = OpusManager()
        manager._project_change_new()
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
        manager._project_change_new()
        manager.set_beat_count(12)
        val event = AbsoluteNoteEvent(24)
        manager.set_event(BeatKey(0,0,0), listOf(), event)
        this.undo_and_check(manager) {
            it.overwrite_line(0, 0, BeatKey(0,0,0))
        }
    }
    @Test
    fun test_overwrite_channel_ctl_row() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        val type = EffectType.Volume
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(.24f))

        this.undo_and_check(manager) {
            it.controller_channel_overwrite_line(type, 0, 0, 1)
        }
    }

    @Test
    fun test_overwrite_global_ctl_row() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        val type = EffectType.Tempo
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(24f))

        this.undo_and_check(manager) {
            it.controller_global_overwrite_line(type, 0)
        }
    }

    @Test
    fun test_overwrite_line_ctl_row() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_beat_count(12)
        val type = EffectType.Volume
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.24f))
        this.undo_and_check(manager) {
            it.controller_line_overwrite_line(type, 0, 0, BeatKey(0, 0, 0))
        }
    }

    @Test
    fun test_set_duration() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_event(BeatKey(0,0,0), listOf(), AbsoluteNoteEvent(24))
        this.undo_and_check(manager) {
            it.set_duration(BeatKey(0,0,0), listOf(), 3)
        }
    }

    @Test
    fun test_controller_global_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.controller_global_split_tree(EffectType.Tempo, 0, listOf(), 2)
        this.undo_and_check(manager) {
            it.controller_global_insert_after(EffectType.Tempo, 0, listOf(0), 3)
        }
    }

    @Test
    fun test_controller_channel_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.controller_channel_split_tree(EffectType.Volume, 0, 0, listOf(), 2)
        this.undo_and_check(manager) {
            it.controller_channel_insert_after(EffectType.Volume, 0, 0, listOf(0), 3)
        }
    }

    @Test
    fun test_controller_line_insert_after() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.controller_line_split_tree(EffectType.Volume, BeatKey(0, 0, 0), listOf(), 2)
        this.undo_and_check(manager) {
            it.controller_line_insert_after(EffectType.Volume, BeatKey(0, 0, 0), listOf(0), 3)
        }
    }

    @Test
    fun test_insert_beat() {
        val manager = OpusManager()
        manager._project_change_new()

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
        manager._project_change_new()

        manager.set_event(BeatKey(0, 0, 0), listOf(), AbsoluteNoteEvent(15))
        manager.set_event(BeatKey(0, 0, 1), listOf(), AbsoluteNoteEvent(16))
        manager.set_event(BeatKey(0, 0, 2), listOf(), AbsoluteNoteEvent(17))

        this.undo_and_check(manager) {
            it.move_leaf(BeatKey(0, 0, 0), listOf(), BeatKey(0, 0, 1), listOf())
        }
    }

    @Test
    fun test_controller_line_move_leaf() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()

        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.15f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.16f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(.17f))

        this.undo_and_check(manager) {
            it.controller_line_move_leaf(type, BeatKey(0, 0, 0), listOf(), BeatKey(0, 0, 1), listOf())
        }
    }

    @Test
    fun test_controller_channel_move_leaf() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()

        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(.15f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(.16f))
        manager.controller_channel_set_event(type, 0, 2, listOf(), OpusVolumeEvent(.17f))

        this.undo_and_check(manager) {
            it.controller_channel_move_leaf(type, 0, 0, listOf(), 0, 1, listOf())
        }
    }

    @Test
    fun test_controller_global_move_leaf() {
        val manager = OpusManager()
        val type = EffectType.Tempo
        manager._project_change_new()

        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(15f))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(16f))
        manager.controller_global_set_event(type, 2, listOf(), OpusTempoEvent(17f))

        this.undo_and_check(manager) {
            it.controller_global_move_leaf(type, 0, listOf(), 1, listOf())
        }
    }

    @Test
    fun test_move_beat_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_event(BeatKey(0, 0, 0), listOf(), AbsoluteNoteEvent(10))
        manager.set_event(BeatKey(0, 0, 1), listOf(), AbsoluteNoteEvent(11))
        manager.set_event(BeatKey(0, 0, 2), listOf(), AbsoluteNoteEvent(13))
        manager.set_event(BeatKey(0, 0, 3), listOf(), AbsoluteNoteEvent(14))

        this.undo_and_check(manager) {
            it.move_beat_range(BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_controller_line_move_range() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.10f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.11f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(.13f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 3), listOf(), OpusVolumeEvent(.14f))

        this.undo_and_check(manager) {
            it.controller_line_move_range(type, BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_controller_line_overwrite_range() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.10f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.11f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(.13f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 3), listOf(), OpusVolumeEvent(.14f))

        this.undo_and_check(manager) {
            it.controller_line_overwrite_range(type, BeatKey(0, 0, 2), BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_controller_global_move_range() {
        val manager = OpusManager()
        val type = EffectType.Tempo
        manager._project_change_new()
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.controller_global_set_event(type, 2, listOf(), OpusTempoEvent(13F))
        manager.controller_global_set_event(type, 3, listOf(), OpusTempoEvent(14F))

        this.undo_and_check(manager) {
            it.controller_global_move_range(type, 2, 0, 1)
        }
    }
    @Test
    fun test_controller_global_overwrite_range() {
        val manager = OpusManager()
        val type = EffectType.Tempo
        manager._project_change_new()
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.controller_global_set_event(type, 2, listOf(), OpusTempoEvent(13F))
        manager.controller_global_set_event(type, 3, listOf(), OpusTempoEvent(14F))

        this.undo_and_check(manager) {
            it.controller_global_overwrite_range(type, 2, 0, 1)
        }
    }

    @Test
    fun test_controller_channel_move_range() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(.10f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(.11f))
        manager.controller_channel_set_event(type, 0, 2, listOf(), OpusVolumeEvent(.13f))
        manager.controller_channel_set_event(type, 0, 3, listOf(), OpusVolumeEvent(.14f))

        this.undo_and_check(manager) {
            it.controller_channel_move_range(type, 0, 2, 0, 0, 1)
        }
    }

    @Test
    fun test_controller_channel_overwrite_range() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(.10f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(.11f))
        manager.controller_channel_set_event(type, 0, 2, listOf(), OpusVolumeEvent(.13f))
        manager.controller_channel_set_event(type, 0, 3, listOf(), OpusVolumeEvent(.14f))

        this.undo_and_check(manager) {
            it.controller_channel_overwrite_range(type, 0, 0, 0, 2, 3)
        }
    }

    @Test
    fun test_unset_range() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.set_event(BeatKey(0, 0, 0), listOf(), AbsoluteNoteEvent(10))
        manager.set_event(BeatKey(0, 0, 1), listOf(), AbsoluteNoteEvent(11))
        manager.set_event(BeatKey(0, 0, 2), listOf(), AbsoluteNoteEvent(13))
        manager.set_event(BeatKey(0, 0, 3), listOf(), AbsoluteNoteEvent(14))
        this.undo_and_check(manager) {
            it.unset_range(BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_controller_line_unset_range() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.controller_line_set_event(type, BeatKey(0, 0, 0), listOf(), OpusVolumeEvent(.10f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 1), listOf(), OpusVolumeEvent(.11f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 2), listOf(), OpusVolumeEvent(.13f))
        manager.controller_line_set_event(type, BeatKey(0, 0, 3), listOf(), OpusVolumeEvent(.14f))
        this.undo_and_check(manager) {
            it.controller_line_unset_range(type, BeatKey(0, 0, 0), BeatKey(0,0,1))
        }
    }

    @Test
    fun test_controller_global_unset_range() {
        val manager = OpusManager()
        val type = EffectType.Tempo
        manager._project_change_new()
        manager.controller_global_set_event(type, 0, listOf(), OpusTempoEvent(10F))
        manager.controller_global_set_event(type, 1, listOf(), OpusTempoEvent(11F))
        manager.controller_global_set_event(type, 2, listOf(), OpusTempoEvent(13F))
        manager.controller_global_set_event(type, 3, listOf(), OpusTempoEvent(14F))
        this.undo_and_check(manager) {
            it.controller_global_unset_range(type, 1, 2)
        }
    }

    @Test
    fun test_controller_channel_unset_range() {
        val manager = OpusManager()
        val type = EffectType.Volume
        manager._project_change_new()
        manager.controller_channel_set_event(type, 0, 0, listOf(), OpusVolumeEvent(.10f))
        manager.controller_channel_set_event(type, 0, 1, listOf(), OpusVolumeEvent(.11f))
        manager.controller_channel_set_event(type, 0, 2, listOf(), OpusVolumeEvent(.13f))
        manager.controller_channel_set_event(type, 0, 3, listOf(), OpusVolumeEvent(.14f))
        this.undo_and_check(manager) {
            it.controller_channel_unset_range(type, 0, 1, 2)
        }
    }

    @Test
    fun test_set_percussion_instrument() {
        val manager = OpusManager()
        manager._project_change_new()
        manager.new_line(1)
        manager.percussion_set_instrument(1, 0, 8)
        manager.percussion_set_instrument(1, 1, 15)
        this.undo_and_check(manager) {
            it.percussion_set_instrument(1, 1, 21)
        }
    }

    @Test
    fun test_set_channel_instrument() {
        val manager = OpusManager()
        manager._project_change_new()
        this.undo_and_check(manager) {
            manager.channel_set_preset(0, Pair(0, 12))
        }
    }

}
