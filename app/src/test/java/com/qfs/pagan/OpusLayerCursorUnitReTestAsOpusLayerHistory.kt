/*
* DO NOT MODIFY THE CONTENTS OF THIS FILE. IT WAS GENERATED IN /scripts/build_other_layer_tests.py
*/
package com.qfs.pagan

import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Test
import com.qfs.pagan.structure.opusmanager.history.OpusLayerHistory as OpusManager


class OpusLayerCursorUnitReTestAsOpusLayerHistory {
    // TODO: assert events selection (both std and ctl), test range selections
    @Test
    fun test_is_selected() {
        val manager = OpusManager()
        manager.project_change_new()

        manager.new_line(0, 0)
        manager.new_channel(1, 2)

        manager.new_line_controller(EffectType.Volume, 0, 0)
        manager.new_line_controller(EffectType.Volume, 0, 1)
        manager.new_line_controller(EffectType.Pan, 0, 0)
        manager.new_line_controller(EffectType.Pan, 0, 1)

        manager.new_line_controller(EffectType.Volume, 1, 0)
        manager.new_line_controller(EffectType.Volume, 1, 1)
        manager.new_line_controller(EffectType.Pan, 1, 0)
        manager.new_line_controller(EffectType.Pan, 1, 1)


        val channels = manager.get_all_channels()
        for (i in channels.indices) {
            val channel = channels[i]
            this.assert_channel_selection(manager, i)
            for (j in channel.lines.indices) {
                this.assert_line_selections(manager, i, j)
                for (i in 0 until manager.length) {
                    this.assert_beat_selection(manager, i)
                }
                for ((type, _) in channel.lines[j].controllers.get_all()) {
                    this.assert_line_controller_line_selection(manager, i, j, type)
                }
            }
            for ((type, _) in channel.controllers.get_all()) {
                this.assert_channel_controller_line_selection(manager, i, type)
            }
        }

        for ((type, _) in manager.controllers.get_all()) {
            this.assert_global_controller_line_selection(manager, type)
        }
    }

    private fun assert_beat_selection(manager: OpusManager, selected_beat: Int) {
        manager.cursor_select_column(selected_beat)
        val channels = manager.get_all_channels()
        for (k in 0 until manager.length) {
            assertEquals(k == selected_beat, manager.is_beat_selected(k))
        }

        for (i in channels.indices) {
            for (j in channels[i].lines.indices) {
                assertFalse(manager.is_line_selected(i, j))
                assertFalse(manager.is_line_selected_secondary(i, j))

                var working_beatkey = BeatKey(i,j, 0)
                var working_position = manager.get_first_position(working_beatkey)
                while (true) {
                    assertFalse(manager.is_selected(working_beatkey, working_position))
                    assertEquals(
                        working_beatkey.beat == selected_beat,
                        manager.is_secondary_selection(working_beatkey, working_position)
                    )

                    val pair = manager.get_proceeding_leaf_position(working_beatkey, working_position) ?: break
                    working_beatkey = pair.first
                    working_position = pair.second
                }

                val line = channels[i].lines[j]
                for ((type, _) in line.controllers.get_all()) {
                    assertFalse(manager.is_line_control_line_selected(type, i, j))
                    assertFalse(manager.is_line_control_line_selected_secondary(type, i, j))
                    working_beatkey = BeatKey(i, j, 0)
                    working_position = manager.get_first_position_line_ctl(type, working_beatkey, listOf())
                    while (true) {
                        assertFalse(manager.is_line_control_selected(type, working_beatkey, working_position))
                        assertEquals(
                            working_beatkey.beat == selected_beat,
                            manager.is_line_control_secondary_selected(type, working_beatkey, working_position)
                        )
                        val pair = manager.get_line_ctl_proceeding_leaf_position(type, working_beatkey, working_position) ?: break
                        working_beatkey = BeatKey(working_beatkey.channel, working_beatkey.line_offset, pair.first)
                        working_position = pair.second
                    }
                }
            }

            for ((type, _) in channels[i].controllers.get_all()) {
                assertFalse(manager.is_channel_control_line_selected(type, i))
                assertFalse(manager.is_channel_control_line_selected_secondary(type, i))
                var working_beat = 0
                var working_position = manager.get_first_position_channel_ctl(type, i, 0)

                while (true) {
                    assertFalse(manager.is_channel_control_selected(type, i, working_beat, working_position))
                    assertEquals(
                        working_beat == selected_beat,
                        manager.is_channel_control_secondary_selected(type, i, working_beat, working_position)
                    )
                    val pair = manager.get_channel_ctl_proceeding_leaf_position(type, i, working_beat, working_position) ?: break
                    working_beat = pair.first
                    working_position = pair.second
                }
            }
        }

        for ((type, _) in manager.controllers.get_all()) {
            assertFalse(manager.is_global_control_line_selected(type))
            assertFalse(manager.is_global_control_line_selected_secondary(type))
            var working_beat = 0
            var working_position = manager.get_first_position_global_ctl(type, 0)
            while (true) {
                assertFalse(manager.is_global_control_selected(type, working_beat, working_position))
                assertEquals(
                    working_beat == selected_beat,
                    manager.is_global_control_secondary_selected(type, working_beat, working_position)
                )
                val pair = manager.get_global_ctl_proceeding_leaf_position(type, working_beat, working_position) ?: break
                working_beat = pair.first
                working_position = pair.second
            }
        }
    }

    private fun assert_line_selections(manager: OpusManager, channel: Int, line_offset: Int) {
        val channels = manager.get_all_channels()
        manager.cursor_select_line(channel, line_offset)
        for (i in channels.indices) {
            for (j in channels[i].lines.indices) {
                assertEquals(
                    i == channel && j == line_offset,
                    manager.is_line_selected(i, j)
                )
                assertFalse(manager.is_line_selected_secondary(i, j))

                var working_beatkey = BeatKey(i,j, 0)
                var working_position = manager.get_first_position(working_beatkey)
                while (true) {
                    assertFalse(manager.is_selected(working_beatkey, working_position))
                    assertEquals(
                        i == channel && j == line_offset,
                        manager.is_secondary_selection(working_beatkey, working_position)
                    )

                    val pair = manager.get_proceeding_leaf_position(working_beatkey, working_position) ?: break
                    working_beatkey = pair.first
                    working_position = pair.second
                }


                val line = channels[i].lines[j]
                for ((type, _) in line.controllers.get_all()) {
                    assertFalse(manager.is_line_control_line_selected(type, i, j))
                    assertEquals(
                        i == channel && line_offset == j,
                        manager.is_line_control_line_selected_secondary(type, i, j)
                    )
                    working_beatkey = BeatKey(i, j, 0)
                    working_position = manager.get_first_position_line_ctl(type, working_beatkey, listOf())
                    while (true) {
                        assertFalse(manager.is_line_control_selected(type, working_beatkey, working_position))
                        assertFalse(manager.is_line_control_secondary_selected(type, working_beatkey, working_position))
                        val pair = manager.get_line_ctl_proceeding_leaf_position(type, working_beatkey, working_position) ?: break
                        working_beatkey = BeatKey(working_beatkey.channel, working_beatkey.line_offset, pair.first)
                        working_position = pair.second
                    }
                }
            }
            for ((type, _) in channels[i].controllers.get_all()) {
                assertFalse(manager.is_channel_control_line_selected(type, i))
                assertFalse(manager.is_channel_control_line_selected_secondary(type, i))
                var working_beat = 0
                var working_position = manager.get_first_position_channel_ctl(type, i, 0)

                while (true) {
                    assertFalse(manager.is_channel_control_selected(type, i, working_beat, working_position))
                    assertFalse(manager.is_channel_control_secondary_selected(type, i, working_beat, working_position))
                    val pair = manager.get_channel_ctl_proceeding_leaf_position(type, i, working_beat, working_position) ?: break
                    working_beat = pair.first
                    working_position = pair.second
                }
            }
        }
        for ((type, _) in manager.controllers.get_all()) {
            assertFalse(manager.is_global_control_line_selected(type))
            assertFalse(manager.is_global_control_line_selected_secondary(type))
            var working_beat = 0
            var working_position = manager.get_first_position_global_ctl(type, 0)
            while (true) {
                assertFalse(manager.is_global_control_selected(type, working_beat, working_position))
                assertFalse(manager.is_global_control_secondary_selected(type, working_beat, working_position))
                val pair = manager.get_global_ctl_proceeding_leaf_position(type, working_beat, working_position) ?: break
                working_beat = pair.first
                working_position = pair.second
            }
        }
    }

    private fun assert_channel_selection(manager: OpusManager, channel: Int) {
        val channels = manager.get_all_channels()
        manager.cursor_select_channel(channel)

        for (i in channels.indices) {
            for (j in channels[i].lines.indices) {
                assertFalse(manager.is_line_selected(i, j))
                assertEquals(
                    i == channel,
                    manager.is_line_selected_secondary(i, j)
                )

                var working_beatkey = BeatKey(i,j, 0)
                var working_position = manager.get_first_position(working_beatkey)
                while (true) {
                    assertFalse(manager.is_selected(working_beatkey, working_position))
                    assertEquals(
                        i == channel,
                        manager.is_secondary_selection(working_beatkey, working_position)
                    )

                    val pair = manager.get_proceeding_leaf_position(working_beatkey, working_position) ?: break
                    working_beatkey = pair.first
                    working_position = pair.second
                }


                val line = channels[i].lines[j]
                for ((type, _) in line.controllers.get_all()) {
                    assertFalse(manager.is_line_control_line_selected(type, i, j))
                    assertEquals(
                        i == channel,
                        manager.is_line_control_line_selected_secondary(type, i, j)
                    )
                    working_beatkey = BeatKey(i, j, 0)
                    working_position = manager.get_first_position_line_ctl(type, working_beatkey, listOf())
                    while (true) {
                        assertFalse(manager.is_line_control_selected(type, working_beatkey, working_position))
                        assertEquals(
                            i == channel,
                            manager.is_line_control_secondary_selected(type, working_beatkey, working_position)
                        )
                        val pair = manager.get_line_ctl_proceeding_leaf_position(type, working_beatkey, working_position) ?: break
                        working_beatkey = BeatKey(working_beatkey.channel, working_beatkey.line_offset, pair.first)
                        working_position = pair.second
                    }
                }
            }

            for ((type, _) in channels[i].controllers.get_all()) {
                assertFalse(manager.is_channel_control_line_selected(type, i))
                var working_beat = 0
                var working_position = manager.get_first_position_channel_ctl(type, i, 0)

                while (true) {
                    assertFalse(manager.is_channel_control_selected(type, i, working_beat, working_position))
                    assertEquals(
                        i == channel,
                        manager.is_channel_control_secondary_selected(type, i, working_beat, working_position)
                    )
                    val pair = manager.get_channel_ctl_proceeding_leaf_position(type, i, working_beat, working_position) ?: break
                    working_beat = pair.first
                    working_position = pair.second
                }
            }
        }

        for ((type, _) in manager.controllers.get_all()) {
            assertFalse(manager.is_global_control_line_selected(type))
            assertFalse(manager.is_global_control_line_selected_secondary(type))
            var working_beat = 0
            var working_position = manager.get_first_position_global_ctl(type, 0)
            while (true) {
                assertFalse(manager.is_global_control_selected(type, working_beat, working_position))
                assertFalse(manager.is_global_control_secondary_selected(type, working_beat, working_position))
                val pair = manager.get_global_ctl_proceeding_leaf_position(type, working_beat, working_position) ?: break
                working_beat = pair.first
                working_position = pair.second
            }
        }
    }

    private fun assert_line_controller_line_selection(manager: OpusManager, selected_channel: Int, selected_line_offset: Int, selected_type: EffectType) {
        val channels = manager.get_all_channels()
        manager.cursor_select_line_ctl_line(selected_type, selected_channel, selected_line_offset)

        for (i in channels.indices) {
            for (j in channels[i].lines.indices) {
                assertFalse(manager.is_line_selected(i, j))
                assertEquals(
                    i == selected_channel && j == selected_line_offset,
                    manager.is_line_selected_secondary(i, j)
                )

                var working_beatkey = BeatKey(i,j, 0)
                var working_position = manager.get_first_position(working_beatkey)
                while (true) {
                    assertFalse(manager.is_selected(working_beatkey, working_position))
                    assertFalse(manager.is_secondary_selection(working_beatkey, working_position))

                    val pair = manager.get_proceeding_leaf_position(working_beatkey, working_position) ?: break
                    working_beatkey = pair.first
                    working_position = pair.second
                }


                val line = channels[i].lines[j]
                for ((type, _) in line.controllers.get_all()) {
                    assertEquals(
                        type == selected_type && i == selected_channel && j == selected_line_offset,
                        manager.is_line_control_line_selected(type, i, j)
                    )
                    assertFalse(manager.is_line_control_line_selected_secondary(type, i, j))

                    working_beatkey = BeatKey(i, j, 0)
                    working_position = manager.get_first_position_line_ctl(type, working_beatkey, listOf())
                    while (true) {
                        assertFalse(manager.is_line_control_selected(type, working_beatkey, working_position))
                        assertEquals(
                            i == selected_channel && j == selected_line_offset && type == selected_type,
                            manager.is_line_control_secondary_selected(type, working_beatkey, working_position)
                        )
                        val pair = manager.get_line_ctl_proceeding_leaf_position(type, working_beatkey, working_position) ?: break
                        working_beatkey = BeatKey(working_beatkey.channel, working_beatkey.line_offset, pair.first)
                        working_position = pair.second
                    }
                }
            }

            for ((type, _) in channels[i].controllers.get_all()) {
                assertFalse(manager.is_channel_control_line_selected(type, i))
                assertFalse(manager.is_channel_control_line_selected_secondary(type, i))
                var working_beat = 0
                var working_position = manager.get_first_position_channel_ctl(type, i, 0)

                while (true) {
                    assertFalse(manager.is_channel_control_selected(type, i, working_beat, working_position))
                    assertFalse(manager.is_channel_control_secondary_selected(type, i, working_beat, working_position))
                    val pair = manager.get_channel_ctl_proceeding_leaf_position(type, i, working_beat, working_position) ?: break
                    working_beat = pair.first
                    working_position = pair.second
                }
            }
        }

        for ((type, _) in manager.controllers.get_all()) {
            assertFalse(manager.is_global_control_line_selected(type))
            assertFalse(manager.is_global_control_line_selected_secondary(type))
            var working_beat = 0
            var working_position = manager.get_first_position_global_ctl(type, 0)
            while (true) {
                assertFalse(manager.is_global_control_selected(type, working_beat, working_position))
                assertFalse(manager.is_global_control_secondary_selected(type, working_beat, working_position))
                val pair = manager.get_global_ctl_proceeding_leaf_position(type, working_beat, working_position) ?: break
                working_beat = pair.first
                working_position = pair.second
            }
        }
    }

    private fun assert_channel_controller_line_selection(manager: OpusManager, selected_channel: Int, selected_type: EffectType) {
        val channels = manager.get_all_channels()
        manager.cursor_select_channel_ctl_line(selected_type, selected_channel)

        for (i in channels.indices) {
            for (j in channels[i].lines.indices) {
                assertFalse(manager.is_line_selected(i, j))
                assertEquals(
                    i == selected_channel,
                    manager.is_line_selected_secondary(i, j)
                )

                var working_beatkey = BeatKey(i,j, 0)
                var working_position = manager.get_first_position(working_beatkey)
                while (true) {
                    assertFalse(manager.is_selected(working_beatkey, working_position))
                    assertFalse(manager.is_secondary_selection(working_beatkey, working_position))

                    val pair = manager.get_proceeding_leaf_position(working_beatkey, working_position) ?: break
                    working_beatkey = pair.first
                    working_position = pair.second
                }


                val line = channels[i].lines[j]
                for ((type, _) in line.controllers.get_all()) {
                    assertFalse(manager.is_line_control_line_selected(type, i, j))

                    // Should this be false? as UI quirk i mean. I'm not sure if having line controls be unselected or selected isn't confusing either way
                    assertEquals(
                        i == selected_channel,
                        manager.is_line_control_line_selected_secondary(type, i, j)
                    )

                    working_beatkey = BeatKey(i, j, 0)
                    working_position = manager.get_first_position_line_ctl(type, working_beatkey, listOf())
                    while (true) {
                        assertFalse(manager.is_line_control_selected(type, working_beatkey, working_position))
                        assertEquals(
                            i == selected_channel && type == selected_type,
                            manager.is_line_control_secondary_selected(type, working_beatkey, working_position)
                        )
                        val pair = manager.get_line_ctl_proceeding_leaf_position(type, working_beatkey, working_position) ?: break
                        working_beatkey = BeatKey(working_beatkey.channel, working_beatkey.line_offset, pair.first)
                        working_position = pair.second
                    }
                }
            }

            for ((type, _) in channels[i].controllers.get_all()) {
                assertEquals(
                    i == selected_channel && type == selected_type,
                    manager.is_channel_control_line_selected(type, i)
                )
                assertFalse(manager.is_channel_control_line_selected_secondary(type, i))


                var working_beat = 0
                var working_position = manager.get_first_position_channel_ctl(type, i, 0)

                while (true) {
                    assertEquals(
                        i == selected_channel && type == selected_type,
                        manager.is_channel_control_selected(type, i, working_beat, working_position)
                    )
                    assertFalse(manager.is_channel_control_secondary_selected(type, i, working_beat, working_position))
                    val pair = manager.get_channel_ctl_proceeding_leaf_position(type, i, working_beat, working_position) ?: break
                    working_beat = pair.first
                    working_position = pair.second
                }
            }
        }

        for ((type, _) in manager.controllers.get_all()) {
            assertFalse(manager.is_global_control_line_selected(type))
            assertFalse(manager.is_global_control_line_selected_secondary(type))
            var working_beat = 0
            var working_position = manager.get_first_position_global_ctl(type, 0)
            while (true) {
                assertFalse(manager.is_global_control_selected(type, working_beat, working_position))
                assertFalse(manager.is_global_control_secondary_selected(type, working_beat, working_position))
                val pair = manager.get_global_ctl_proceeding_leaf_position(type, working_beat, working_position) ?: break
                working_beat = pair.first
                working_position = pair.second
            }
        }
    }

    private fun assert_global_controller_line_selection(manager: OpusManager, selected_type: EffectType) {
        val channels = manager.get_all_channels()
        manager.cursor_select_global_ctl_line(selected_type)

        for (i in channels.indices) {
            for (j in channels[i].lines.indices) {
                assertFalse(manager.is_line_selected(i, j))
                assertFalse(manager.is_line_selected_secondary(i, j))

                var working_beatkey = BeatKey(i,j, 0)
                var working_position = manager.get_first_position(working_beatkey)
                while (true) {
                    assertFalse(manager.is_selected(working_beatkey, working_position))
                    assertFalse(manager.is_secondary_selection(working_beatkey, working_position))

                    val pair = manager.get_proceeding_leaf_position(working_beatkey, working_position) ?: break
                    working_beatkey = pair.first
                    working_position = pair.second
                }


                val line = channels[i].lines[j]
                for ((type, _) in line.controllers.get_all()) {
                    assertFalse(manager.is_line_control_line_selected(type, i, j))
                    assertFalse(manager.is_line_control_line_selected_secondary(type, i, j))

                    working_beatkey = BeatKey(i, j, 0)
                    working_position = manager.get_first_position_line_ctl(type, working_beatkey, listOf())
                    while (true) {
                        assertFalse(manager.is_line_control_selected(type, working_beatkey, working_position))
                        assertFalse(manager.is_line_control_secondary_selected(type, working_beatkey, working_position))
                        val pair = manager.get_line_ctl_proceeding_leaf_position(type, working_beatkey, working_position) ?: break
                        working_beatkey = BeatKey(working_beatkey.channel, working_beatkey.line_offset, pair.first)
                        working_position = pair.second
                    }
                }
            }

            for ((type, _) in channels[i].controllers.get_all()) {
                assertFalse(manager.is_channel_control_line_selected(type, i))
                assertFalse(manager.is_channel_control_line_selected_secondary(type, i))
                var working_beat = 0
                var working_position = manager.get_first_position_channel_ctl(type, i, 0)

                while (true) {
                    assertFalse(manager.is_channel_control_selected(type, i, working_beat, working_position))
                    assertFalse(manager.is_channel_control_secondary_selected(type, i, working_beat, working_position))
                    val pair = manager.get_channel_ctl_proceeding_leaf_position(type, i, working_beat, working_position) ?: break
                    working_beat = pair.first
                    working_position = pair.second
                }
            }
        }

        for ((type, _) in manager.controllers.get_all()) {
            assertEquals(
                type == selected_type,
                manager.is_global_control_line_selected(type)
            )
            assertFalse(manager.is_global_control_line_selected_secondary(type))
            var working_beat = 0
            var working_position = manager.get_first_position_global_ctl(type, 0)
            while (true) {
                assertFalse(manager.is_global_control_selected(type, working_beat, working_position))
                assertEquals(
                    selected_type == type,
                    manager.is_global_control_secondary_selected(type, working_beat, working_position)
                )
                val pair = manager.get_global_ctl_proceeding_leaf_position(type, working_beat, working_position) ?: break
                working_beat = pair.first
                working_position = pair.second
            }
        }
    }
}
