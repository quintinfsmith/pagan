package com.qfs.pagan

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.rule.GrantPermissionRule
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test


class ComposeTest {
    @get:Rule
    val editor_test_rule = createAndroidComposeRule<ComponentActivityEditor>()

    @JvmField
    @Rule
    var mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule
        .grant(Manifest.permission.POST_NOTIFICATIONS)

    private fun click_elm(tag: TestTag, vararg args: Any?) {
        val interaction = get_interaction(tag, *args)
        interaction.performClick()
    }

    private fun long_click_elm(tag: TestTag, vararg args: Any?) {
        get_interaction(tag, *args).performTouchInput {
            longClick(Offset(this.width / 2F,this.height / 2F))
        }
    }

    private fun get_interaction(tag: TestTag, vararg args: Any?): SemanticsNodeInteraction {
        when (tag) {
            TestTag.Leaf -> get_interaction(TestTag.MainRow).performScrollToIndex(args[3] as Int)
            TestTag.BeatLabel -> get_interaction(TestTag.MainRow).performScrollToIndex(args[0] as Int)
            else -> {}
        }
        return editor_test_rule.onNodeWithTag(test_tag_to_string(tag, *args), true)
    }

    @Test
    fun test_new_project() {
        //click_elm(TestTag.LandingNewProject)
        click_elm(TestTag.TopBarKebab)
        editor_test_rule.onNodeWithText("New Project").performClick()
    }

    @Test
    fun test_build_song() {
        val opus_manager = editor_test_rule.activity.controller_model.opus_manager
        click_elm(TestTag.OuterInsertBeat)
        get_interaction(TestTag.DialogNumberInput).performTextInput("200")
        click_elm(TestTag.DialogPositive)
        assertEquals(204, opus_manager.length)

        // Build Drums
        click_elm(TestTag.LineLabel, 1, 0, null)
        click_elm(TestTag.LineNew)
        click_elm(TestTag.LineNew)
        assertEquals(
            3,
            opus_manager.get_channel(1).lines.size
        )

        click_elm(TestTag.InstrumentSet)
        click_elm(TestTag.MenuItem, 8)
        assertEquals(
            8,
            (opus_manager.get_channel(1).lines[2] as OpusLinePercussion).instrument
        )

        click_elm(TestTag.LineLabel, 1, 1, null)
        click_elm(TestTag.InstrumentSet)
        click_elm(TestTag.MenuItem, 13)
        assertEquals(
            13,
            (opus_manager.get_channel(1).lines[1] as OpusLinePercussion).instrument
        )

        click_elm(TestTag.LineLabel, 1, 0, null)
        click_elm(TestTag.InstrumentSet)
        click_elm(TestTag.MenuItem, 15)
        assertEquals(
            15,
            (opus_manager.get_channel(1).lines[0] as OpusLinePercussion).instrument
        )

        click_elm(TestTag.Leaf, null, 1, 0, 0)
        click_elm(TestTag.LeafSplit)
        assertEquals(2, opus_manager.get_tree(BeatKey(1, 0, 0)).size)

        click_elm(TestTag.Leaf, null, 1, 0, 0, 1)
        click_elm(TestTag.LeafSplit)
        assertEquals(2, opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1)).size)

        click_elm(TestTag.PercussionToggle)
        assertNotEquals(
            null,
            opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1, 0)).event
        )

        click_elm(TestTag.Leaf, null, 1, 0, 0, 1, 1)
        click_elm(TestTag.PercussionToggle)
        assertNotEquals(
            null,
            opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1, 1)).event
        )

        long_click_elm(TestTag.Leaf, null, 1, 0, 0, 1, 1)
        click_elm(TestTag.LineLabel, 1, 0, null)
        get_interaction(TestTag.DialogNumberInput).performTextInput("28")
        click_elm(TestTag.DialogPositive)
        for (i in 0 until 28) {
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 0, 0)),
                opus_manager.get_tree(BeatKey(1, 0, i))
            )
        }

        click_elm(TestTag.Leaf, null, 1, 2, 0)
        click_elm(TestTag.PercussionToggle)
        click_elm(TestTag.Leaf, null, 1, 1, 1)
        click_elm(TestTag.PercussionToggle)
        long_click_elm(TestTag.Leaf, null, 1, 1, 1)
        long_click_elm(TestTag.Leaf, null, 1, 2, 0)
        click_elm(TestTag.LineLabel, 1, 1, null)
        get_interaction(TestTag.DialogNumberInput).performTextInput("14")
        click_elm(TestTag.DialogPositive)
        for (i in 1 until 14) {
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 1, 0)).event,
                opus_manager.get_tree(BeatKey(1, 1, (i * 2))).event
            )
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 1, 1)).event,
                opus_manager.get_tree(BeatKey(1, 1, 1 + (i * 2))).event
            )
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 2, 0)).event,
                opus_manager.get_tree(BeatKey(1, 2, i * 2)).event
            )
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 2, 1)).event,
                opus_manager.get_tree(BeatKey(1, 2, 1 + (i * 2))).event
            )
        }

        for (i in 0 until 13) {
            click_elm(TestTag.Undo)
        }

        assertEquals(204, opus_manager.length)
        click_elm(TestTag.Undo)

        assertEquals(4, opus_manager.length)
        assertEquals(4, opus_manager.channels[1].get_beat_count())
        assertEquals(1, opus_manager.channels[1].lines.size)

        for (i in 0 until 3) {
            click_elm(TestTag.Redo)
        }
        assertEquals(204, opus_manager.length)
        assertEquals(204, opus_manager.channels[0].lines[0].beats.size)
    }

    @Test
    fun test_undo_global_effect_removal() {
        click_elm(TestTag.LineLabel, null, null, EffectType.Tempo)
        click_elm(TestTag.LineEffectRemove)
        click_elm(TestTag.Undo)
        click_elm(TestTag.LineLabel, null, null, EffectType.Tempo)
    }

    @Test
    fun reg_test_180() {
        val opus_manager = editor_test_rule.activity.controller_model.opus_manager

        //Enable an effect
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.LineEffectsShow)
        click_elm(TestTag.EffectMenuVolume)

        click_elm(TestTag.Leaf, EffectType.Volume, 0, 0, 0)
        click_elm(TestTag.LeafSplit)

        long_click_elm(TestTag.EventUnset)
        assert(
            opus_manager.get_line_ctl_tree_copy<OpusVolumeEvent>(
                EffectType.Volume,
                BeatKey(0,0,0),
                listOf()
            ).is_leaf()
        )

        // click twice since short click and long click have the same effect otherwise
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.LeafSplit)
        long_click_elm(TestTag.LeafRemove)
        assert(
            opus_manager.get_line_ctl_tree_copy<OpusVolumeEvent>(
                EffectType.Volume,
                BeatKey(0,0,0),
                listOf()
            ).is_leaf()
        )
    }

    @Test
    fun reg_test_185() {
        // Mute The last channel
        click_elm(TestTag.LineLabel, 1,0, null)
        click_elm(TestTag.LineLabel, 1,0, null)
        click_elm(TestTag.ChannelMute)

        click_elm(TestTag.EffectsToggleGlobal)
        click_elm(TestTag.EffectMenuDelay)

        val state_model = editor_test_rule.activity.state_model
        for (line in state_model.line_data) {
            if (line.channel.value != null) continue
            assert(!line.is_mute.value)
        }
    }

    @Test
    fun reg_test_186() {
        // Open Channel Menu & Remove it
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.ChannelRemove)

        // Again, Open Channel Menu & Remove it
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.ChannelRemove)

        val opus_manager = editor_test_rule.activity.controller_model.opus_manager
        assert(opus_manager.channels.isNotEmpty())
    }

    @Test
    fun reg_test_188() {
        click_elm(TestTag.EffectsToggleGlobal)
        click_elm(TestTag.EffectMenuDelay)
        click_elm(TestTag.Leaf, EffectType.Delay, null, null, 0)
        this.editor_test_rule.waitUntil {
            get_interaction(TestTag.DelayFadeButton).isDisplayed()
        }
    }

    @Test
    fun reg_test_189() {
        click_elm(TestTag.LineLabel, 0, 0, null)
        click_elm(TestTag.LineEffectsShow)
        click_elm(TestTag.EffectMenuDelay)
        click_elm(TestTag.LineLabel, 0, 0, null)
        click_elm(TestTag.LineLabel, 0, 0, null)

        click_elm(TestTag.ChannelEffects)
        this.editor_test_rule.waitUntil {
            get_interaction(TestTag.EffectMenuDelay).isDisplayed()
        }
    }
}
