package com.qfs.pagan

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.qfs.pagan.ComponentActivity.ComponentActivityLanding
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

class ComposeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivityLanding>()
    @Test
    fun myTest() {
        val new_project_label = composeTestRule.activity.getString(R.string.btn_landing_new)
        composeTestRule.onNodeWithText(new_project_label).performClick()
        composeTestRule.onNodeWithTag("LineLabel: 1|0|null").performClick()
        composeTestRule.onNodeWithTag("NewLine").performClick()
        composeTestRule.onNodeWithTag("NewLine").performClick()
        composeTestRule.onNodeWithTag("SetInstrument").performClick()
        composeTestRule.onNodeWithTag("MenuItem 8").performClick()

        composeTestRule.onNodeWithTag("LineLabel: 1|1|null").performClick()
        composeTestRule.onNodeWithTag("SetInstrument").performClick()
        composeTestRule.onNodeWithTag("MenuItem 13").performClick()

        composeTestRule.onNodeWithTag("LineLabel: 1|0|null").performClick()
        composeTestRule.onNodeWithTag("SetInstrument").performClick()
        composeTestRule.onNodeWithTag("MenuItem 15").performClick()

        composeTestRule.onNodeWithTag("LeafView: 1|0|0|[]").performClick()
        composeTestRule.onNodeWithTag("SplitLeaf").performClick()
        composeTestRule.onNodeWithTag("LeafView: 1|0|0|[1]").performClick()
        composeTestRule.onNodeWithTag("TogglePercussion").performClick()
        composeTestRule.onNodeWithTag("LeafView: 1|0|0|[1]").performTouchInput {
            longClick(Offset(this.width / 2F,this.height / 2F))
        }

        composeTestRule.onNodeWithTag("LineLabel: 1|0|null").performClick()
        composeTestRule.onNodeWithTag("DialogNumberInput").performTextInput("8")
        composeTestRule.onNodeWithTag("DialogPositive").performClick()
        for (i in 0 until 4) {
            composeTestRule.onNodeWithTag("MainField").performScrollToIndex((i * 2))
            composeTestRule.onNodeWithTag("LeafView: 1|2|${(i * 2)}|[]").performClick()
            composeTestRule.onNodeWithTag("TogglePercussion").performClick()
            composeTestRule.onNodeWithTag("LeafView: 1|1|${(i * 2) + 1}|[]").performClick()
            composeTestRule.onNodeWithTag("TogglePercussion").performClick()
        }

        //composeTestRule.onNodeWithTag("LeafView: 0|0|0|[]").performClick()
        //composeTestRule.onNodeWithTag("SplitLeaf").performClick()


        //composeTestRule.onNodeWithTag("Octave 4").performClick()
        //composeTestRule.onNodeWithTag("Offset 3").performClick()
        //composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
