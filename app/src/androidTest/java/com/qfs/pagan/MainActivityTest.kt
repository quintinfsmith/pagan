package com.qfs.pagan


import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.qfs.json.JSONList
import com.qfs.json.JSONParser
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusManagerCursor
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.qfs.pagan.OpusLayerInterface as OpusManager


@JvmField
@Rule
var permissionRead: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    //@Rule
    //var permissionCamera: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    //@Rule
    //var permissionAudio: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)
    //@Rule
    //var permissionLocation: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
    //@Rule
    //var permissionWrite: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)


    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private fun run_action(token: ActionTracker.TrackedAction, int_list: List<Int?>) {
        this.mActivityScenarioRule.scenario.onActivity { activity ->
            val tracker = activity?.get_action_interface()
            tracker?.process_queued_action(token, int_list)
        }
    }

    private fun with_opus_manager(callback: (OpusManager, MainActivity) -> Unit) {
        this.mActivityScenarioRule.scenario.onActivity { activity ->
            val opus_manager = activity?.get_opus_manager() ?: return@onActivity
            callback(opus_manager, activity)
        }
    }

    @Test
    fun mainActivityTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stream = context.assets.open("tests/tracked_actions_1.json")
        val bytes = ByteArray(stream.available()) { 0 }
        stream.read(bytes)
        val text = bytes.decodeToString()

        val action_list = JSONParser.parse<JSONList>(text)

        if (action_list?.list?.isNotEmpty() == true) {
            for (i in 0 until action_list.list.size) {
                val item = action_list.get_list(i)
                val (token, intlist) = ActionTracker.from_json_entry(item)
                try {
                    this.run_action(token, intlist)
                } catch (e: Exception) {
                    throw Exception("$i) Fail - $item")
                }
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
