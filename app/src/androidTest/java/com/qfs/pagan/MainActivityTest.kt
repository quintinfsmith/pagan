package com.qfs.pagan


// @JvmField
// @Rule
// var permissionRead: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
// 
// @LargeTest
// @RunWith(AndroidJUnit4::class)
// class MainActivityTest {
//     //@Rule
//     //var permissionCamera: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
//     //@Rule
//     //var permissionAudio: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)
//     //@Rule
//     //var permissionLocation: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
//     //@Rule
//     //var permissionWrite: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
// 
// 
//     @Rule
//     @JvmField
//     var mActivityScenarioRule = ActivityScenarioRule(ActivityEditor::class.java)
// 
//     private fun run_action(token: ActionTracker.TrackedAction, int_list: List<Int?> = listOf()) {
//         this.mActivityScenarioRule.scenario.onActivity { activity ->
//             val tracker = activity?.get_action_interface()
//             tracker?.process_queued_action(token, int_list)
//         }
//     }
// 
//     private fun with_opus_manager(callback: (OpusManager, ActivityEditor) -> Unit) {
//         this.mActivityScenarioRule.scenario.onActivity { activity ->
//             val opus_manager = activity?.get_opus_manager() ?: return@onActivity
//             callback(opus_manager, activity)
//         }
//     }
// 
//     @Test
//     fun mainActivityTest() {
//         val context = InstrumentationRegistry.getInstrumentation().targetContext
//         val stream = context.assets.open("tests/generated_1739987654989.json")
//         val bytes = ByteArray(stream.available()) { 0 }
//         stream.read(bytes)
//         val text = bytes.decodeToString()
// 
//         val action_list = JSONParser.parse<JSONList>(text)
// 
//         if (action_list?.isNotEmpty() == true) {
//             for (i in 0 until action_list.size) {
//                 val item = action_list.get_list(i)
//                 val (token, intlist) = ActionTracker.from_json_entry(item)
//                 try {
//                     this.run_action(token, intlist)
//                 } catch (e: Exception) {
//                     throw Exception("$i) Fail - $item")
//                 }
//             }
//         }
// 
//         this.with_opus_manager { opus_manager, activity ->
//             val other = OpusLayerBase()
//             val stream = context.assets.open("tests/opus_1739987654989.json")
//             val bytes = ByteArray(stream.available()) { 0 }
//             stream.read(bytes)
//             other.load(bytes)
//             val base_version = OpusLayerBase()
//             base_version.import_from_other(opus_manager)
// 
// 
//             assertEquals(other.length, base_version.length)
//             assertEquals(other.channels.size, base_version.channels.size)
// 
//             for (c in 0 until other.get_all_channels().size) {
//                 val other_channel = other.get_all_channels()[c]
//                 val this_channel = base_version.get_all_channels()[c]
//                 for (l in 0 until other_channel.lines.size) {
//                     assertEquals(other_channel.lines[l], this_channel.lines[l])
//                 }
//             }
// 
//             assertEquals(other.project_name, base_version.project_name)
//             assertEquals(other.project_notes, base_version.project_notes)
//         }
//     }
// 
//     /**
//      * Test bugfix #55
//      */
//     @Test
//     fun test_insert_line_width_map_adjust() {
//         this.run_action(TrackedAction.NewProject)
//         this.with_opus_manager { opus_manager, activity ->
//             val editor_table  = activity.findViewById<EditorTable>(R.id.etEditorTable)
//             assertEquals(
//                 List(4) { List(3) { 1 } },
//                 editor_table.get_column_width_map(),
//             )
//         }
//         this.run_action(TrackedAction.CursorSelectLeaf, listOf(0,0,0))
//         this.run_action(TrackedAction.SplitLeaf, listOf(2))
//         this.run_action(TrackedAction.CursorSelectLine, listOf(0,0,0))
//         this.run_action(
//             TrackedAction.ShowLineController,
//             ActionTracker.string_to_ints("Volume")
//         )
// 
//         this.with_opus_manager { opus_manager, activity ->
//             assertEquals(
//                 true,
//                 opus_manager.get_line_controller<OpusControlEvent>(ControlEventType.Volume, 0, 0).visible
//             )
//         }
// 
//         this.run_action(TrackedAction.CursorSelectLine, listOf(0,0,0))
//         this.run_action(TrackedAction.RemoveLine, listOf(1))
//         this.run_action(TrackedAction.ApplyUndo)
//         this.run_action(TrackedAction.CursorSelectLineCtlLine,ActionTracker.enum_to_ints(ControlEventType.Volume) + listOf(0,0))
//         this.run_action(TrackedAction.ToggleControllerVisibility)
//         this.run_action(TrackedAction.ApplyUndo)
// 
//         this.with_opus_manager { opus_manager, activity ->
//             val editor_table  = activity.findViewById<EditorTable>(R.id.etEditorTable)
//             assertEquals(
//                 listOf(2,1,1,1),
//                 editor_table.get_column_width_map()[0]
//             )
//         }
// 
//     }
// 
//     private fun childAtPosition(
//         parentMatcher: Matcher<View>, position: Int
//     ): Matcher<View> {
// 
//         return object : TypeSafeMatcher<View>() {
//             override fun describeTo(description: Description) {
//                 description.appendText("Child at position $position in parent ")
//                 parentMatcher.describeTo(description)
//             }
// 
//             public override fun matchesSafely(view: View): Boolean {
//                 val parent = view.parent
//                 return parent is ViewGroup && parentMatcher.matches(parent)
//                         && view == parent.getChildAt(position)
//             }
//         }
//     }
// }
// 