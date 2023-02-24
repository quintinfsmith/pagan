package com.qfs.radixulous

import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.BeatKey
import org.junit.Test
import org.junit.Assert.*
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import com.qfs.radixulous.opusmanager.HistoryCache
import com.qfs.radixulous.opusmanager.OpusManagerBase

class HistoryCacheUnitTest {
    @Test
    fun test_historycache_append_undoer_key() {
        //TODO("test_historycache_append_undoer_key")
    }
    @Test
    fun test_historycache_multi_counter() {
        //TODO("test_historycache_multi_counter")
    }
    @Test
    fun test_historycache_push_set_cursor() {
        //TODO("test_historycache_push_set_cursor")
    }
    @Test
    fun test_historycache_clear() {
        //TODO("test_historycache_clear")
    }
    @Test
    fun test_historycache_position() {
        var cache = HistoryCache()
        cache.add_position(listOf(0,1))

        var cached_position = cache.get_position()
        assertEquals(cached_position, listOf(0,1))
    }
    @Test
    fun test_historycache_beatkey() {
        var cache = HistoryCache()
        cache.add_beatkey(BeatKey(1,2,3))
        var cached_beatkey = cache.get_beatkey()
        assertEquals(cached_beatkey, BeatKey(1,2,3))
    }
    @Test
    fun test_historycache_boolean() {
        var cache = HistoryCache()
        cache.add_boolean(false)
        cache.add_boolean(true )
        assertEquals(cache.get_boolean(), true)
        assertEquals(cache.get_boolean(), false)
    }
    @Test
    fun test_historycache_int() {
        var cache = HistoryCache()
        cache.add_int(2)
        cache.add_int(5)
        assertEquals(cache.get_int(), 5)
        assertEquals(cache.get_int(),2)
    }
    @Test
    fun test_historycache_beat() {
        //TODO("test_historycache_beat")
    }
    @Test
    fun test_historycache_lock() {
        //TODO("test_historycache_lock")
    }
    @Test
    fun test_historycache_pop() {
        //TODO("test_historycache_pop")
    }
    ///------------------------------------------------------

    @Test
    fun test_setup_repopulate() {
        //TODO("test_setup_repopulate")
    }

    @Test
    fun test_repopulate() {
        var manager = OpusManager()
        manager.new()
        manager.split_tree(BeatKey(0,0,0), listOf(), 5)

        manager.apply_undo()
    }

   @Test
   fun test_remove() {
       var manager = OpusManager()
       manager.new()
       manager.split_tree(BeatKey(0,0,0), listOf(), 3)
       manager.remove(BeatKey(0,0,0), listOf(1))
       manager.apply_undo()
   }

   @Test
   fun test_get_channel_count() {
       //TODO("test_get_channel_count")
   }
    @Test
    fun test_get_channel_instrument() {
        //TODO("test_get_channel_instrument")
    }
    @Test
    fun test_get_percussion_instrument() {
        //TODO("test_get_percussion_instrument")
    }
    @Test
    fun test_get_beat_tree() {
        //TODO("test_get_beat_tree")
    }
    @Test
    fun test_get_proceding_leaf() {
        //TODO("test_get_proceding_leaf")
    }
    @Test
    fun test_get_preceding_leaf() {
        //TODO("test_get_preceding_leaf")
    }
    @Test
    fun test_get_proceding_leaf_position() {
        //TODO("test_get_proceding_leaf_position")
    }
    @Test
    fun test_get_preceding_leaf_position() {
        //TODO("test_get_preceding_leaf_position")
    }
    @Test
    fun test_get_absolute_value() {
        //TODO("test_get_absolute_value")
    }
    @Test
    fun test_get_channel_line_counts() {
        //TODO("test_get_channel_line_counts")
    }
    @Test
    fun test_has_preceding_absolute_event() {
        //TODO("test_has_preceding_absolute_event")
    }
    @Test
    fun test_is_percussion() {
        //TODO("test_is_percussion")
    }
    @Test
    fun test_convert_event_to_relative() {
        //TODO("test_convert_event_to_relative")
    }
    @Test
    fun test_convert_event_to_absolute() {
        //TODO("test_convert_event_to_absolute")
    }
    @Test
    fun test_set_percussion_event() {
        //TODO("test_set_percussion_event")
    }
    @Test
    fun test_set_percussion_instrument() {
        //TODO("test_set_percussion_instrument")
    }
    @Test
    fun test_set_percussion_channel() {
        //TODO("test_set_percussion_channel")
    }
    @Test
    fun test_unset_percussion_channel() {
        //TODO("test_unset_percussion_channel")
    }
    @Test
    fun test_set_event() {
        //TODO("test_set_event")
    }
    @Test
    fun test_unset() {
        //TODO("test_unset")
    }
    @Test
    fun test_new_channel() {
        //TODO("test_new_channel")
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
    fun test_new_line() {
        //TODO("test_new_line")
    }
    @Test
    fun test_overwrite_beat() {
        //TODO("test_overwrite_beat")
    }
    @Test
    fun test_remove_beat() {
        //TODO("test_remove_beat")
    }
    @Test
    fun test_remove_channel() {
        //TODO("test_remove_channel")
    }
    @Test
    fun test_remove_line() {
        //TODO("test_remove_line")
    }
    @Test
    fun test_replace_beat() {
        //TODO("test_replace_beat")
    }
    @Test
    fun test_replace_tree() {
        //TODO("test_replace_tree")
    }
    @Test
    fun test_set_beat_count() {
        //TODO("test_set_beat_count")
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
    fun test_purge_cache() {
        //TODO("test_purge_cache")
    }
    @Test
    fun test_reset_cache() {
        //TODO("test_reset_cache")
    }

    @Test
    fun test_insert_after() {
        //TODO("test_insert_after")
    }

    @Test
    fun test_split_tree() {
        //TODO("test_split_tree")
    }
}
