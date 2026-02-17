package com.qfs.pagan

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class ComposeTest {
    @get:Rule
    val editor_test_rule = createAndroidComposeRule<ComponentActivityEditor>()

    private fun click_elm(tag: TestTag, vararg args: Any?) {
        get_interaction(tag, *args).performClick()
    }

    private fun long_click_elm(tag: TestTag, vararg args: Any?) {
        get_interaction(tag, *args).performTouchInput {
            longClick(Offset(this.width / 2F,this.height / 2F))
        }
    }

    private fun get_interaction(tag: TestTag, vararg args: Any?): SemanticsNodeInteraction {
        when (tag) {
            TestTag.Leaf -> {
                get_interaction(TestTag.MainRow).performScrollToIndex(args[2] as Int)
            }
            TestTag.BeatLabel -> {
                get_interaction(TestTag.MainRow).performScrollToIndex(args[0] as Int)
            }
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

        click_elm(TestTag.Leaf, 1, 0, 0)
        click_elm(TestTag.LeafSplit)
        assertEquals(2, opus_manager.get_tree(BeatKey(1, 0, 0)).size)

        click_elm(TestTag.Leaf, 1, 0, 0, 1)
        click_elm(TestTag.LeafSplit)
        assertEquals(2, opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1)).size)

        click_elm(TestTag.PercussionToggle)
        assertNotEquals(
            null,
            opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1, 0)).event
        )

        click_elm(TestTag.Leaf, 1, 0, 0, 1, 1)
        click_elm(TestTag.PercussionToggle)
        assertNotEquals(
            null,
            opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1, 1)).event
        )

        long_click_elm(TestTag.Leaf, 1, 0, 0, 1, 1)
        click_elm(TestTag.LineLabel, 1, 0, null)
        get_interaction(TestTag.DialogNumberInput).performTextInput("28")
        click_elm(TestTag.DialogPositive)
        for (i in 0 until 28) {
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 0, 0)),
                opus_manager.get_tree(BeatKey(1, 0, i))
            )
        }

        click_elm(TestTag.Leaf, 1, 2, 0)
        click_elm(TestTag.PercussionToggle)
        click_elm(TestTag.Leaf, 1, 1, 1)
        click_elm(TestTag.PercussionToggle)
        long_click_elm(TestTag.Leaf, 1, 1, 1)
        long_click_elm(TestTag.Leaf, 1, 2, 0)
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
}
