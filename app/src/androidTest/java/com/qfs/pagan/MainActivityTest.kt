package com.qfs.pagan


import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.qfs.json.JSONList
import com.qfs.json.JSONParser
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

    fun run_action(token: ActionTracker.TrackedAction, int_list: List<Int?>) {
        this.mActivityScenarioRule.scenario.onActivity { activity ->
            val tracker = activity?.get_action_interface()
            tracker?.process_queued_action(token, int_list)
        }
    }

    @Test
    fun mainActivityTest() {
        val context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        val stream = context.assets.open("tests/tracked_actions.json")
        val bytes = ByteArray(stream.available()) { 0 }
        stream.read(bytes)
        val text = bytes.decodeToString()

        val action_list = JSONParser.parse<JSONList>(text)

        if (action_list?.list?.isNotEmpty() == true) {
            for (i in 0 until action_list.list.size) {
                val item = action_list.get_list(i)
                val (token, intlist) = ActionTracker.from_json_entry(item)
                this.run_action(token, intlist)
            }
        }

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
