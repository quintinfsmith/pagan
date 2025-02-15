package com.qfs.pagan

import com.qfs.json.JSONString
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ActionTrackerUnitTest {
    @Test
    fun test_to_json() {
        for (enum in ActionTracker.TrackedAction.values()) {
            val json_name = JSONString(enum.name)
            val input = when (enum) {
                // -------------- No arguments --------------
                ActionTracker.TrackedAction.TogglePercussionVisibility,
                ActionTracker.TrackedAction.Unset,
                ActionTracker.TrackedAction.UnsetRoot,
                ActionTracker.TrackedAction.SaveProject,
                ActionTracker.TrackedAction.DeleteProject,
                ActionTracker.TrackedAction.CopyProject,
                ActionTracker.TrackedAction.TogglePercussion,
                ActionTracker.TrackedAction.DrawerOpen,
                ActionTracker.TrackedAction.DrawerClose,
                ActionTracker.TrackedAction.OpenSettings,
                ActionTracker.TrackedAction.OpenAbout,
                ActionTracker.TrackedAction.NewProject,
                ActionTracker.TrackedAction.DisableSoundFont,
                ActionTracker.TrackedAction.ApplyUndo -> {
                    Pair(enum, listOf())
                }

                // ------- Boolean ------------
                ActionTracker.TrackedAction.GoBack,
                ActionTracker.TrackedAction.SetClipNotes,
                ActionTracker.TrackedAction.SetRelativeModeVisibility -> {
                    Pair(enum, listOf(1))
                }

                // -------- Single String Argument -------------
                ActionTracker.TrackedAction.SetTransitionAtCursor,
                ActionTracker.TrackedAction.ShowLineController,
                ActionTracker.TrackedAction.ShowChannelController,
                ActionTracker.TrackedAction.RemoveController,
                ActionTracker.TrackedAction.SetSoundFont,
                ActionTracker.TrackedAction.SetProjectName,
                ActionTracker.TrackedAction.DeleteSoundFont,
                ActionTracker.TrackedAction.ImportSong,
                ActionTracker.TrackedAction.ImportSoundFont,
                ActionTracker.TrackedAction.SetCopyMode,
                ActionTracker.TrackedAction.LoadProject -> {
                    val test_string = "Some String"
                    val test_bytes = test_string.toByteArray()
                    Pair(enum, List(test_bytes.size) { i: Int -> test_bytes[i].toInt() })
                }

                // ------- Single Int Argument ----------------
                ActionTracker.TrackedAction.CopyGlobalCtlToBeat,
                ActionTracker.TrackedAction.MoveGlobalCtlToBeat,
                ActionTracker.TrackedAction.SetPanAtCursor,
                ActionTracker.TrackedAction.SetSampleRate,
                ActionTracker.TrackedAction.ToggleControllerVisibility,
                ActionTracker.TrackedAction.InsertLine,
                ActionTracker.TrackedAction.RemoveLine,
                ActionTracker.TrackedAction.InsertChannel,
                ActionTracker.TrackedAction.RemoveChannel,
                ActionTracker.TrackedAction.SetDuration,
                ActionTracker.TrackedAction.RemoveBeat,
                ActionTracker.TrackedAction.InsertBeat,
                ActionTracker.TrackedAction.SetDurationCtl,
                ActionTracker.TrackedAction.SetVolumeAtCursor,
                ActionTracker.TrackedAction.SetOffset,
                ActionTracker.TrackedAction.SetOctave,
                ActionTracker.TrackedAction.SplitLeaf,
                ActionTracker.TrackedAction.InsertLeaf,
                ActionTracker.TrackedAction.RemoveLeaf,
                ActionTracker.TrackedAction.SetPercussionInstrument,
                ActionTracker.TrackedAction.SetRelativeMode,
                ActionTracker.TrackedAction.CursorSelectColumn -> {
                    Pair(enum, listOf(5))
                }

                ActionTracker.TrackedAction.RepeatSelectionCtlChannel,
                ActionTracker.TrackedAction.CursorSelectGlobalCtlRange -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1)
                    )
                }
                ActionTracker.TrackedAction.RepeatSelectionCtlLine,
                ActionTracker.TrackedAction.CursorSelectChannelCtlRange -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1, 2)
                    )
                }
                ActionTracker.TrackedAction.CursorSelectLineCtlRange -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1, 2, 3, 4, 5)
                    )
                }
                ActionTracker.TrackedAction.CursorSelectLeafCtlGlobal,
                ActionTracker.TrackedAction.CursorSelectLeafCtlChannel,
                ActionTracker.TrackedAction.CursorSelectLeafCtlLine -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1, 2, 3, 4, 5)
                    )
                }
                ActionTracker.TrackedAction.CursorSelectLineCtlLine -> {
                    val string = "Pan"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1)
                    )
                }
                ActionTracker.TrackedAction.RepeatSelectionCtlGlobal,
                ActionTracker.TrackedAction.CursorSelectChannelCtlLine -> {
                    val string = "Reverb"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0)
                    )
                }
                ActionTracker.TrackedAction.CursorSelectGlobalCtlLine -> {
                    val string = "Tempo"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() }
                    )
                }



                ActionTracker.TrackedAction.CursorSelectLine -> {
                    Pair(enum, listOf(0, 1))
                }


                // --------- >= 3 int args ------------------
                ActionTracker.TrackedAction.CursorSelectLeaf,
                ActionTracker.TrackedAction.CursorSelectRange -> {
                    Pair(enum, listOf(0, 1, 2, 3, 4, 5))
                }

                // -------- 3 int args ----------------------
                ActionTracker.TrackedAction.CopyLineCtlToBeat,
                ActionTracker.TrackedAction.MoveLineCtlToBeat,
                ActionTracker.TrackedAction.RepeatSelectionStd,
                ActionTracker.TrackedAction.MoveSelectionToBeat,
                ActionTracker.TrackedAction.CopySelectionToBeat,
                ActionTracker.TrackedAction.MergeSelectionIntoBeat,
                ActionTracker.TrackedAction.SetChannelInstrument -> {
                    Pair(enum, listOf(4, 2, 6))
                }

                ActionTracker.TrackedAction.CopyChannelCtlToBeat,
                ActionTracker.TrackedAction.MoveChannelCtlToBeat -> {
                    Pair(enum, listOf(4, 2))
                }

                // ------------2 + 2n args ----------------
                ActionTracker.TrackedAction.SetTuningTable -> {
                    Pair(enum, listOf(0, 12, 0, 12, 1, 12, 2, 12, 3, 12, 4, 12, 5, 12, 6, 12, 7, 12, 8, 12, 9, 12, 10, 12, 11, 12))
                }

                // ----------- Float --------------
                ActionTracker.TrackedAction.SetTempoAtCursor -> {
                    Pair(enum, listOf(35f.toBits()))
                }
            }

            val json_item = ActionTracker.item_to_json(input)

            assertEquals(
                "Incorrect conversion of ${enum.name}",
                input,
                ActionTracker.from_json_entry(json_item)
            )
        }
    }
}