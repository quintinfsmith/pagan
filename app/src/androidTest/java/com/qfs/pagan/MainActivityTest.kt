package com.qfs.pagan


import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.qfs.pagan.opusmanager.BeatKey
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    fun run_action(callback: (ActionTracker) -> Unit) {
        this.mActivityScenarioRule.getScenario().onActivity { activity ->
            val tracker = activity?.get_action_interface()
            if (tracker != null) {
                callback(tracker)
            }
        }
    }

    @Test
    fun mainActivityTest() {
        run_action { it.new_project() }
        run_action { it.cursor_select(BeatKey(0, 0, 0), listOf()) }
        run_action { it.set_octave(1) }
        run_action { it.split(2) }
        run_action { it.cursor_select(BeatKey(0, 0, 0), listOf(1)) }
        run_action { it.set_offset(5) }
        run_action { it.cursor_select(BeatKey(0, 0, 1), listOf()) }
        run_action { it.insert_leaf(1) }
        run_action { it.set_octave(4) }
        run_action { it.cursor_select(BeatKey(0, 0, 3), listOf()) }
        run_action { it.set_offset(7) }
        run_action { it.cursor_select(BeatKey(1, 0, 1), listOf()) }
        run_action { it.cursor_select_line_std(1, 0) }
        run_action { it.insert_line(1) }
        run_action { it.insert_line(1) }
        run_action { it.cursor_select_line_std(1, 2) }
        run_action { it.cursor_select_line_std(1, 2) }
        run_action { it.set_percussion_instrument(8) }
        run_action { it.cursor_select_line_std(1, 1) }
        run_action { it.set_percussion_instrument(11) }
        run_action { it.cursor_select_line_std(1, 1) }
        run_action { it.cursor_select_line_std(1, 0) }
        run_action { it.set_percussion_instrument(17) }
        run_action { it.cursor_select(BeatKey(1, 0, 0), listOf()) }
        run_action { it.split(2) }
        run_action { it.cursor_select(BeatKey(1, 0, 0), listOf(1)) }
        run_action { it.toggle_percussion() }
        run_action { it.cursor_select_range(BeatKey(1, 0, 0), BeatKey(1, 0, 0)) }
        run_action { it.repeat_selection_std(1, 0, -1) }
        run_action { it.cursor_select(BeatKey(1, 2, 0), listOf()) }
        run_action { it.toggle_percussion() }
        run_action { it.cursor_select(BeatKey(1, 1, 1), listOf()) }
        run_action { it.toggle_percussion() }
        run_action { it.cursor_select_range(BeatKey(1, 2, 1), BeatKey(1, 2, 1)) }
        run_action { it.cursor_select_range(BeatKey(1, 2, 1), BeatKey(1, 1, 0)) }
        run_action { it.repeat_selection_std(1, 1, -1) }

    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
