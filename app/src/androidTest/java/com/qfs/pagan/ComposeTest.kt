package com.qfs.pagan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.qfs.pagan.ComponentActivity.ComponentActivityLanding
import org.junit.Rule
import org.junit.Test

class ComposeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivityLanding>()

    private fun click_elm(tag: TestTag, vararg args: Any?) {
        get_interaction(tag, *args).performClick()
    }
    private fun long_click_elm(tag: TestTag, vararg args: Any?) {
        get_interaction(tag, *args).performTouchInput {
            longClick(Offset(this.width / 2F,this.height / 2F))
        }
    }
    private fun get_interaction(tag: TestTag, vararg args: Any?): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(test_tag_to_string(tag, *args))
    }

    @Test
    fun test_build_song() {
        click_elm(TestTag.LandingNewProject)
        click_elm(TestTag.OuterInsertBeat)
        get_interaction(TestTag.DialogNumberInput).performTextInput("200")
        click_elm(TestTag.DialogPositive)

        // Build Drums
        click_elm(TestTag.LineLabel, 1, 0, null)
        click_elm(TestTag.LineNew)
        click_elm(TestTag.LineNew)
        click_elm(TestTag.InstrumentSet)
        click_elm(TestTag.MenuItem, 8)

        click_elm(TestTag.LineLabel, 1, 1, null)
        click_elm(TestTag.InstrumentSet)
        click_elm(TestTag.MenuItem, 13)

        click_elm(TestTag.LineLabel, 1, 0, null)
        click_elm(TestTag.InstrumentSet)
        click_elm(TestTag.MenuItem, 15)

        click_elm(TestTag.Leaf, 1, 0, 0)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.Leaf, 1, 0, 0, 1)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.PercussionToggle)
        click_elm(TestTag.Leaf, 1, 0, 0, 1, 1)
        click_elm(TestTag.PercussionToggle)
        long_click_elm(TestTag.Leaf, 1, 0, 0, 1, 1)
        click_elm(TestTag.LineLabel, 1, 0, null)
        get_interaction(TestTag.DialogNumberInput).performTextInput("28")
        click_elm(TestTag.DialogPositive)
        get_interaction(TestTag.MainRow).performScrollToIndex(0)
        click_elm(TestTag.Leaf, 1, 2, 0)
        click_elm(TestTag.PercussionToggle)
        get_interaction(TestTag.MainRow).performScrollToIndex(1)
        click_elm(TestTag.Leaf, 1, 1, 1)
        click_elm(TestTag.PercussionToggle)
        long_click_elm(TestTag.Leaf, 1, 1, 1)
        get_interaction(TestTag.MainRow).performScrollToIndex(0)
        long_click_elm(TestTag.Leaf, 1, 1, 1)
        click_elm(TestTag.LineLabel, 1, 1, null)
        get_interaction(TestTag.DialogNumberInput).performTextInput("14")

        get_interaction(TestTag.MainRow).performScrollToIndex(28)
        click_elm(TestTag.Leaf, 1, 2, 28)
        click_elm(TestTag.PercussionToggle)

        get_interaction(TestTag.MainRow).performScrollToIndex(29)
        click_elm(TestTag.Leaf, 1, 1, 29)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.PercussionToggle)
        click_elm(TestTag.Leaf, 1, 1, 29, 1)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.Leaf, 1, 1, 29, 1, 1)
        click_elm(TestTag.PercussionToggle)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.Leaf, 1, 1, 29, 1, 1, 1)
        click_elm(TestTag.LeafSplit)

        click_elm(TestTag.Leaf, 1, 0, 29)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.Leaf, 1, 0, 29, 1)

        // composeTestRule.onNodeWithTag("LeafView: 1|0|0|[1]").performClick()
        // composeTestRule.onNodeWithTag("TogglePercussion").performClick()
        // composeTestRule.onNodeWithTag("LeafView: 1|0|0|[1]").performTouchInput {
        //     longClick(Offset(this.width / 2F,this.height / 2F))
        // }

        // composeTestRule.onNodeWithTag("LineLabel: 1|0|null").performClick()
        // composeTestRule.onNodeWithTag("DialogNumberInput").performTextInput("8")
        // composeTestRule.onNodeWithTag("DialogPositive").performClick()
        // for (i in 0 until 4) {
        //     composeTestRule.onNodeWithTag("MainField").performScrollToIndex((i * 2))
        //     composeTestRule.onNodeWithTag("LeafView: 1|2|${(i * 2)}|[]").performClick()
        //     composeTestRule.onNodeWithTag("TogglePercussion").performClick()
        //     composeTestRule.onNodeWithTag("LeafView: 1|1|${(i * 2) + 1}|[]").performClick()
        //     composeTestRule.onNodeWithTag("TogglePercussion").performClick()
        // }

        // //composeTestRule.onNodeWithTag("LeafView: 0|0|0|[]").performClick()
        // //composeTestRule.onNodeWithTag("SplitLeaf").performClick()


        // //composeTestRule.onNodeWithTag("Octave 4").performClick()
        // //composeTestRule.onNodeWithTag("Offset 3").performClick()
        // //composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
