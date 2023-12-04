package com.qfs.pagan

import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Test
import com.qfs.pagan.opusmanager.HistoryLayer as OpusManager

class HistoryCacheUnitTest {
    @Test
    fun test_historycache_push_set_cursor() {
        //TODO("test_historycache_push_set_cursor")
    }
    @Test
    fun test_historycache_clear() {
        //TODO("test_historycache_clear")
    }

    ///------------------------------------------------------

   @Test
   fun test_remove() {
       var key = BeatKey(0,0,0)
       var test_event = OpusEvent(12,12,0,false)

       var manager = OpusManager()
       manager.new()
       manager.split_tree(key, listOf(), 2)
       manager.set_event(key, listOf(1), test_event)
       manager.remove(key, listOf(1))
       manager.apply_undo()
       assertEquals(
           "Failed to undo remove",
           2,
           manager.get_tree(key, listOf()).size,
       )

       assertEquals(
           "Failed to undo remove with correct tree",
           test_event,
           manager.get_tree(key, listOf(1)).get_event()
       )
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

        try {
            manager.set_percussion_event(BeatKey(0,0,0), listOf())
        } catch (e: Exception) {}

        assertEquals(
            "Appended to history stack on failure.",
            true,
            manager.history_cache.isEmpty()
        )

        manager.set_percussion_event(BeatKey(1,0,0), listOf())

        manager.apply_undo()

        assertEquals(
            "Failed to undo set_percussion_event().",
            false,
            manager.get_tree(BeatKey(0,0,0), listOf()).is_event()
        )
    }

    @Test
    fun test_set_event() {
        var event = OpusEvent(12, 12, 0, false)
        var event_b = OpusEvent(5, 12, 0, false)
        var manager = OpusManager()
        manager.new()

        manager.set_event(BeatKey(0,0,0), listOf(), event)
        manager.apply_undo()

        assertEquals(
            "Failed to undo set_event()",
            null,
            manager.get_tree(BeatKey(0,0,0), listOf()).get_event()
        )

        manager.set_event(BeatKey(0,0,0), listOf(), event_b)
        manager.set_event(BeatKey(0,0,0), listOf(), event)

        manager.apply_undo()

        assertEquals(
            "Failed to undo set_event()",
            event_b,
            manager.get_tree(BeatKey(0,0,0), listOf()).get_event()
        )
    }

    @Test
    fun test_unset() {
        var event = OpusEvent(12, 12, 0, false)
        var manager = OpusManager()
        manager.new()
        manager.set_event(BeatKey(0,0,0), listOf(), event)
        manager.clear_history()

        var original = manager.to_json()

        manager.unset(BeatKey(0,0,0), listOf())
        manager.apply_undo()

        assertEquals(
            "Failed to undo unset()",
            manager.to_json(),
            original
        )

        try {
            manager.unset(BeatKey(0, 0, 0), listOf(0, 2))
        } catch (e: Exception) { }

        assertEquals(
            "Didn't clean history stack on error",
            true,
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_new_channel() {
        var manager = OpusManager()
        manager.new()

        var original = manager.to_json()
        manager.new_channel()
        manager.apply_undo()

        assertEquals(
            "Failed to undo new_channel",
            manager.to_json(),
            original
        )
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
    fun test_overwrite_beat() {
        //TODO("test_overwrite_beat")
    }
    @Test
    fun test_remove_beat() {
        var manager = OpusManager()
        manager.new()
        var original = manager.to_json()

        manager.remove_beat(0)
        manager.apply_undo()
        var new = manager.to_json()

        assertEquals(
            "undo remove_beat broken",
            original,
            new
        )

        try {
            manager.remove_beat(200)
        } catch (e: Exception) {}
        assertEquals(
            "history stack not cleared with error",
            true,
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_remove_channel() {
        var manager = OpusManager()
        manager.new()
        manager.new_channel()
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.clear_history()

        var original = manager.to_json()

        manager.remove_channel(0)
        manager.apply_undo()

        var new = manager.to_json()
        assertEquals(
            "undo remove_channel broken",
            original,
            new
        )

        try {
            manager.remove_channel(4)
        } catch (e: Exception) {}
        assertEquals(
            "history stack not cleared with error",
            true,
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_remove_line() {
        var manager = OpusManager()
        manager.new()
        manager.new_line(0,0)
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        manager.clear_history()

        var original = manager.to_json()

        manager.remove_line(0, 0)
        manager.apply_undo()

        var new = manager.to_json()
        assertEquals(
            "undo remove_line broken",
            original,
            new
        )

        try {
            manager.remove_line(4, 0)
        } catch (e: Exception) {}
        assertEquals(
            "history stack not cleared with error",
            true,
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_new_line() {
        var manager = OpusManager()
        manager.new()
        var original = manager.to_json()

        manager.new_line(0, 0)
        manager.apply_undo()

        var new = manager.to_json()
        assertEquals(
            "undo new_line broken",
            original,
            new
        )

        try {
            manager.new_line(4, 0)
        } catch (e: Exception) {}
        assertEquals(
            "history stack not cleared with error",
            true,
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_replace_beat() {
        //TODO("test_replace_beat")
    }
    @Test
    fun test_replace_tree() {
        var manager = OpusManager()
        manager.new()
        var original = manager.to_json()
        var new_tree = OpusTree<OpusEvent>()
        new_tree.set_size(5)

        manager.replace_tree(BeatKey(0,0,0), listOf(), new_tree)
        manager.apply_undo()
        var new = manager.to_json()
        assertEquals(
            "undo replace_tree broken",
            original,
            new
        )

        try {
            manager.replace_tree(BeatKey(5,0,0), listOf(), new_tree)
        } catch (e: Exception) { }

        assertEquals(
            "Didn't clean history stack on error",
            true,
            manager.history_cache.isEmpty()
        )
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
        manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        // Need to clean stack for final check
        manager.clear_history()

        var original = manager.to_json()

        manager.insert_after(BeatKey(0,0,0), listOf(0), 1)

        manager.apply_undo()
        var new = manager.to_json()
        assertEquals(
            "undo insert_after broken",
            original,
            new
        )

        try {
            manager.split_tree(BeatKey(0, 0, 0), listOf(0, 2), 1)
        } catch (e: Exception) { }

        assertEquals(
            "Didn't clean history stack on error",
            true,
            manager.history_cache.isEmpty()
        )
    }

    @Test
    fun test_split_tree() {
        var manager = OpusManager()
        manager.new()
        var original = manager.to_json()

        manager.split_tree(BeatKey(0,0,0), listOf(), 3)

        manager.apply_undo()


        var new = manager.to_json()
        assertEquals(
            "undo split_tree broken",
            original,
            new
        )

        try {
            manager.split_tree(BeatKey(0, 0, 0), listOf(0, 2), 3)
        } catch (e: Exception) { }

        assertEquals(
            "Didn't clean history stack on error",
            true,
            manager.history_cache.isEmpty()
        )
    }
}
