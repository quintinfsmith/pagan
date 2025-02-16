package com.qfs.pagan


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
        this.assert_action(token, int_list)
    }

    private fun with_opus_manager(callback: (OpusManager, MainActivity) -> Unit) {
        this.mActivityScenarioRule.scenario.onActivity { activity ->
            val opus_manager = activity?.get_opus_manager() ?: return@onActivity
            callback(opus_manager, activity)
        }

    }

    private fun assert_action(token: ActionTracker.TrackedAction, int_list: List<Int?>) {
        with_opus_manager { opus_manager: OpusManager, activity: MainActivity ->
            when (token) {
                ActionTracker.TrackedAction.NewProject -> {
                    assertEquals(2, opus_manager.get_all_channels().size)
                    assertEquals(4, opus_manager.beat_count)
                }

                ActionTracker.TrackedAction.CursorSelectColumn -> {
                    val cursor = opus_manager.cursor
                    assertEquals(OpusManagerCursor.CursorMode.Column, cursor.mode)
                    assertEquals(int_list[0]!!, cursor.beat)
                }

                ActionTracker.TrackedAction.CursorSelectGlobalCtlRange -> {
                    val cursor = opus_manager.cursor
                    assertEquals(
                        OpusManagerCursor.CursorMode.Range,
                        cursor.mode
                    )
                    assertEquals(
                        CtlLineLevel.Global,
                        cursor.ctl_level
                    )
                    val type = ControlEventType.values()[int_list[0]!!]
                    val beat_a = int_list[1]
                    val beat_b = int_list[2]
                    assertEquals(
                        type,
                        cursor.ctl_type
                    )
                    assertEquals(
                        beat_a,
                        cursor.range!!.first.beat
                    )
                    assertEquals(
                        beat_b,
                        cursor.range!!.second.beat
                    )
                }
                //ActionTracker.TrackedAction.CursorSelectChannelCtlRange -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLineCtlRange -> TODO()
                //ActionTracker.TrackedAction.CursorSelectRange -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLeaf -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLeafCtlLine -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLeafCtlChannel -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLeafCtlGlobal -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLine -> TODO()
                //ActionTracker.TrackedAction.CursorSelectLineCtlLine -> TODO()
                //ActionTracker.TrackedAction.CursorSelectChannelCtlLine -> TODO()
                //ActionTracker.TrackedAction.CursorSelectGlobalCtlLine -> TODO()
                //ActionTracker.TrackedAction.RepeatSelectionStd -> TODO()
                //ActionTracker.TrackedAction.RepeatSelectionCtlLine -> TODO()
                //ActionTracker.TrackedAction.RepeatSelectionCtlChannel -> TODO()
                //ActionTracker.TrackedAction.RepeatSelectionCtlGlobal -> TODO()
                //ActionTracker.TrackedAction.MoveLineCtlToBeat -> TODO()
                //ActionTracker.TrackedAction.MoveChannelCtlToBeat -> TODO()
                //ActionTracker.TrackedAction.MoveGlobalCtlToBeat -> TODO()
                //ActionTracker.TrackedAction.MoveSelectionToBeat -> TODO()
                //ActionTracker.TrackedAction.CopyLineCtlToBeat -> TODO()
                //ActionTracker.TrackedAction.CopyChannelCtlToBeat -> TODO()
                //ActionTracker.TrackedAction.CopyGlobalCtlToBeat -> TODO()
                //ActionTracker.TrackedAction.CopySelectionToBeat -> TODO()
                //ActionTracker.TrackedAction.MergeSelectionIntoBeat -> TODO()
                //ActionTracker.TrackedAction.SetOffset -> TODO()
                //ActionTracker.TrackedAction.SetOctave -> TODO()
                //ActionTracker.TrackedAction.TogglePercussion -> TODO()
                //ActionTracker.TrackedAction.SplitLeaf -> TODO()
                //ActionTracker.TrackedAction.InsertLeaf -> TODO()
                //ActionTracker.TrackedAction.RemoveLeaf -> TODO()
                ActionTracker.TrackedAction.Unset -> {
                    val cursor = opus_manager.cursor
                    when (cursor.mode) {
                        OpusManagerCursor.CursorMode.Single -> {
                            assert(opus_manager.get_tree().is_leaf())
                            assertFalse(opus_manager.get_tree().is_event())
                        }
                        else -> {}
                    }
                }
                //ActionTracker.TrackedAction.UnsetRoot -> TODO()
                //ActionTracker.TrackedAction.SetDuration -> TODO()
                //ActionTracker.TrackedAction.SetDurationCtl -> TODO()
                //ActionTracker.TrackedAction.SetChannelInstrument -> TODO()
                //ActionTracker.TrackedAction.SetPercussionInstrument -> TODO()
                //ActionTracker.TrackedAction.TogglePercussionVisibility -> TODO()
                //ActionTracker.TrackedAction.ToggleControllerVisibility -> TODO()
                //ActionTracker.TrackedAction.ShowLineController -> TODO()
                //ActionTracker.TrackedAction.ShowChannelController -> TODO()
                //ActionTracker.TrackedAction.RemoveController -> TODO()
                //ActionTracker.TrackedAction.InsertLine -> TODO()
                //ActionTracker.TrackedAction.RemoveLine -> TODO()
                //ActionTracker.TrackedAction.InsertChannel -> TODO()
                //ActionTracker.TrackedAction.RemoveChannel -> TODO()
                //ActionTracker.TrackedAction.SetTransitionAtCursor -> TODO()
                //ActionTracker.TrackedAction.SetVolumeAtCursor -> TODO()
                //ActionTracker.TrackedAction.SetTempoAtCursor -> TODO()
                //ActionTracker.TrackedAction.SetPanAtCursor -> TODO()
                //ActionTracker.TrackedAction.RemoveBeat -> TODO()
                //ActionTracker.TrackedAction.InsertBeat -> TODO()
                //ActionTracker.TrackedAction.SetCopyMode -> TODO()
                ActionTracker.TrackedAction.DrawerOpen -> {
                    val drawer_layout = activity.findViewById<DrawerLayout>(R.id.drawer_layout)
                    assert(drawer_layout.isDrawerOpen(GravityCompat.START))
                }
                ActionTracker.TrackedAction.DrawerClose -> {
                    val drawer_layout = activity.findViewById<DrawerLayout>(R.id.drawer_layout)
                    assertFalse(drawer_layout.isDrawerOpen(GravityCompat.START))
                }
                ActionTracker.TrackedAction.OpenSettings -> {
                    val fragment = activity.get_active_fragment()
                    assert(fragment is FragmentGlobalSettings)
                }
                ActionTracker.TrackedAction.OpenAbout -> {
                    val fragment = activity.get_active_fragment()
                    assert(fragment is FragmentLicense)
                }
                ActionTracker.TrackedAction.SetSampleRate -> {
                    assertEquals(
                        int_list[0]!!,
                        activity.configuration.sample_rate
                    )
                }
                ActionTracker.TrackedAction.DisableSoundFont -> {
                    assertEquals(
                        null,
                        activity.configuration.soundfont
                    )
                }
                ActionTracker.TrackedAction.SetSoundFont -> {
                    assertEquals(
                        ActionTracker.string_from_ints(int_list),
                        activity.configuration.soundfont
                    )
                }
                ActionTracker.TrackedAction.SetProjectName -> {
                    assertEquals(
                        ActionTracker.string_from_ints(int_list),
                        opus_manager.project_name
                    )
                }
                //ActionTracker.TrackedAction.LoadProject -> TODO()
                else -> {}
            }
        }
    }

    @Test
    fun mainActivityTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
