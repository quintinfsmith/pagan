package com.qfs.radixulous

import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.BeatKey
import org.junit.Test
import org.junit.Assert.*
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import com.qfs.radixulous.opusmanager.HistoryCache

class HistoryCacheUnitTest {
    @Test
    fun test_position() {
        var cache = HistoryCache()
        cache.add_position(listOf(0,1))

        var cached_position = cache.get_position()
        assertEquals(cached_position, listOf(0,1))
    }

    @Test
    fun test_beatkey() {
        var cache = HistoryCache()
        cache.add_beatkey(BeatKey(1,2,3))
        var cached_beatkey = cache.get_beatkey()
        assertEquals(cached_beatkey, BeatKey(1,2,3))
    }

    @Test
    fun test_boolean() {
        var cache = HistoryCache()
        cache.add_boolean(false)
        cache.add_boolean(true )
        assertEquals(cache.get_boolean(), true)
        assertEquals(cache.get_boolean(), false)
    }

    @Test
    fun test_int() {
        var cache = HistoryCache()
        cache.add_int(2)
        cache.add_int(5)
        assertEquals(cache.get_int(), 5)
        assertEquals(cache.get_int(),2)
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

   // @Test
   // fun test_push_split_tree() {
   //     var cache = HistoryCache()


   // }
}