package com.qfs.pagan

import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import com.qfs.pagan.opusmanager.LinksLayer as OpusManager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LinkLayerUnitTest {
    @Test
    fun test_unlink_beat() {
        val (manager, main_key) = this.setup_linked_manager()
        val linked_keys = manager.get_all_linked(main_key)
        manager.unlink_beat(main_key)
        assertEquals(
            "Failed to unlink beat",
            1,
            manager.get_all_linked(main_key).size
        )
    }

    @Test
    fun test_clear_links_to_beat() {
        //TODO("test_clear_links_to_beat")
    }

    @Test
    fun test_clear_links_in_network() {
        //TODO("test_clear_links_in_network")
    }

    @Test
    fun test_remove_link_from_network() {
        //TODO("test_remove_link_from_network")
    }

    @Test
    fun test_link_beats() {
        val manager = OpusManager()
        manager.new()
        manager.new_line(0)
        val first_key = BeatKey(0, 0, 0)
        val second_key = BeatKey(0, 0, 1)
        val third_key = BeatKey(0, 1, 0)
        manager.link_beats(first_key, second_key)
        assertTrue(
            "Failed to link 2 beats",
            manager.get_all_linked(first_key).contains(second_key)
        )
        manager.link_beats(first_key, third_key)
        assertTrue(
            "Failed to link 3rd beat into pool",
            manager.get_all_linked(first_key).contains(third_key)
        )
    }

    @Test
    fun test_get_all_linked() {
        //TODO("test_get_all_linked")
    }
    @Test
    fun test_new() {
        //TODO("test_new")
    }
    @Test
    fun test_set_percussion_event() {
        //TODO("test_set_percussion_event")
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
    fun test_change_line_channel() {
        val manager = OpusManager()
        manager.new()
        manager.new_channel()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        manager.move_line(0,0,1,0)
        assertEquals(
            "Failed to move links with line",
            2,
            manager.get_all_linked(BeatKey(1,0,0)).size
        )
    }

    @Test
    fun test_insert_beat() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,2))
        manager.insert_beat(1)
        assertEquals(
            "Failed to move links when inserting beat",
            2,
            manager.get_all_linked(BeatKey(0,0,3)).size
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
    }
    @Test
    fun test_new_line() {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        manager.new_line(0, 0)
        assertEquals(
            "Failed to adjust links on new line",
            2,
            manager.get_all_linked(BeatKey(0,1,0)).size
        )
    }

    @Test
    fun test_overwrite_beat() {
        //TODO("test_overwrite_beat")
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
        val test_tree = OpusTree<OpusEvent>()
        test_tree.set_size(12)
        manager.replace_beat_tree(main_key, test_tree)

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
        val test_tree = OpusTree<OpusEvent>()
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
    fun test_insert_after() {
        val (manager, main_key) = this.setup_linked_manager()

        manager.split_tree(main_key, listOf(), 2)
        manager.insert_after(main_key, listOf(1))

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to remove subtree on linked tree",
                3,
                it.size
            )
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

        val new_duration = 12
        manager.set_event(main_key, listOf(), OpusEvent(20, 12, 0, false, 1))
        manager.set_duration(main_key, listOf(), new_duration)

        this.batch_link_test(manager, main_key) {
            assertEquals(
                "Failed to set duration on all linked beats",
                new_duration,
                it.get_event()!!.duration
            )
        }
    }

    fun batch_link_test(manager: OpusManager, main_key: BeatKey, callback: (OpusTree<OpusEvent>) -> Unit) {
        for (linked_key in manager.get_all_linked(main_key)) {
            if (linked_key == main_key) {
                continue
            }

            callback(manager.get_beat_tree(linked_key))
        }
    }

    fun setup_linked_manager(): Pair<OpusManager, BeatKey> {
        val manager = OpusManager()
        manager.new()
        manager.link_beats(BeatKey(0,0,0), BeatKey(0,0,1))
        return Pair(manager, BeatKey(0,0,0))
    }
}
