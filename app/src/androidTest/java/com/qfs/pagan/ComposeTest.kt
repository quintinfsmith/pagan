package com.qfs.pagan

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qfs.pagan.ComponentActivity.ComponentActivityLanding
import org.junit.Rule
import org.junit.Test

class ComposeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivityLanding>()
    @Test
    fun myTest() {
        val new_project_label = composeTestRule.activity.getString(R.string.btn_landing_new)
        composeTestRule.onNodeWithText(new_project_label).performClick()
        composeTestRule.onNodeWithTag("LeafView: 0|0|0|[]").performClick()
        composeTestRule.onNodeWithTag("SplitLeaf").performClick()
        composeTestRule.onNodeWithTag("InsertLeaf").performClick()

        //composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
