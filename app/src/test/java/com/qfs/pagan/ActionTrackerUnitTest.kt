package com.qfs.pagan

import com.qfs.json.JSONBoolean
import com.qfs.json.JSONFloat
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ActionTrackerUnitTest {
    @Test
    fun test_to_json() {
        for (enum in ActionTracker.TrackedAction.values()) {
            val input = when (enum) {
                // -------------- No arguments --------------
                ActionTracker.TrackedAction.RemoveController,
                ActionTracker.TrackedAction.ToggleControllerVisibility,
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
                ActionTracker.TrackedAction.ApplyUndo -> {
                    Pair(enum, listOf())
                }

                // ------- Boolean ------------
                ActionTracker.TrackedAction.GoBack -> {
                    Pair(enum, listOf(1))
                }

                // Special C
                ActionTracker.TrackedAction.SetProjectNameAndNotes -> {
                    val project_name = "Project Name".toByteArray()
                    val project_notes = "Project Notes".toByteArray()
                    Pair(
                        enum,
                        listOf(project_name.size)
                            + List(project_name.size) { i: Int -> project_name[i].toInt() }
                            + List(project_notes.size) { i: Int -> project_notes[i].toInt() }
                    )
                }

                // -------- Single String Argument -------------
                ActionTracker.TrackedAction.SetTransitionAtCursor,
                ActionTracker.TrackedAction.ShowLineController,
                ActionTracker.TrackedAction.ShowGlobalController,
                ActionTracker.TrackedAction.ShowChannelController,
                ActionTracker.TrackedAction.ImportSong,
                ActionTracker.TrackedAction.SetCopyMode,
                ActionTracker.TrackedAction.LoadProject -> {
                    val test_string = "Some String"
                    val test_bytes = test_string.toByteArray()
                    Pair(enum, List(test_bytes.size) { i: Int -> test_bytes[i].toInt() })
                }

                // ------- Single Int Argument ----------------
                ActionTracker.TrackedAction.UntagColumn,
                ActionTracker.TrackedAction.AdjustSelection,
                ActionTracker.TrackedAction.MuteChannel,
                ActionTracker.TrackedAction.UnMuteChannel,
                ActionTracker.TrackedAction.CopyGlobalCtlToBeat,
                ActionTracker.TrackedAction.MoveGlobalCtlToBeat,
                ActionTracker.TrackedAction.SetPanAtCursor,
                ActionTracker.TrackedAction.InsertLine,
                ActionTracker.TrackedAction.RemoveLine,
                ActionTracker.TrackedAction.RemoveChannel,
                ActionTracker.TrackedAction.SetDuration,
                ActionTracker.TrackedAction.RemoveBeat,
                ActionTracker.TrackedAction.InsertBeat,
                ActionTracker.TrackedAction.SetDurationCtl,
                ActionTracker.TrackedAction.SetVolumeAtCursor,
                ActionTracker.TrackedAction.SetVelocityAtCursor,
                ActionTracker.TrackedAction.SetOffset,
                ActionTracker.TrackedAction.SetOctave,
                ActionTracker.TrackedAction.SplitLeaf,
                ActionTracker.TrackedAction.InsertLeaf,
                ActionTracker.TrackedAction.RemoveLeaf,
                ActionTracker.TrackedAction.SetPercussionInstrument,
                ActionTracker.TrackedAction.SetRelativeMode,
                ActionTracker.TrackedAction.CursorSelectChannel,
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

                //ActionTracker.TrackedAction.UntagColumn -> {
                ActionTracker.TrackedAction.TagColumn -> {
                    val string = "TAGNAME"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(56) + List(bytes.size) { bytes[it].toInt() }
                    )
                }



                ActionTracker.TrackedAction.InsertChannel,
                ActionTracker.TrackedAction.MuteLine,
                ActionTracker.TrackedAction.UnMuteLine,
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

                ActionTracker.TrackedAction.InsertBeatAt,
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

                ActionTracker.TrackedAction.MoveLine,
                ActionTracker.TrackedAction.SwapLines -> {
                    Pair(enum, listOf(0, 0, 1, 0))
                }

                ActionTracker.TrackedAction.MoveChannel -> {
                    Pair(enum, listOf(1, 3, 1))
                }
                ActionTracker.TrackedAction.SetDelayAtCursor -> {
                    Pair(enum, listOf(4,3, 0.75F.toBits(), 4))
                }
            }

            val json_name = JSONString(enum.name)
            val json_item = ActionTracker.item_to_json(input)

            assertEquals(
                "Failed to convert ${enum.name} to json Correctly",
                when (enum) {
                    // -------------- No arguments --------------
                    ActionTracker.TrackedAction.RemoveController,
                    ActionTracker.TrackedAction.ToggleControllerVisibility,
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
                    ActionTracker.TrackedAction.ApplyUndo -> {
                        JSONList(json_name)
                    }

                    // ------- Boolean ------------
                    ActionTracker.TrackedAction.GoBack -> {
                        JSONList(json_name, JSONBoolean(true))
                    }

                    ActionTracker.TrackedAction.SetProjectNameAndNotes -> {
                        val project_name = "Project Name"
                        val project_notes = "Project Notes"
                        JSONList(json_name, JSONString(project_name), JSONString(project_notes))
                    }

                    // -------- Single String Argument -------------
                    ActionTracker.TrackedAction.SetTransitionAtCursor,
                    ActionTracker.TrackedAction.ShowLineController,
                    ActionTracker.TrackedAction.ShowGlobalController,
                    ActionTracker.TrackedAction.ShowChannelController,
                    ActionTracker.TrackedAction.ImportSong,
                    ActionTracker.TrackedAction.SetCopyMode,
                    ActionTracker.TrackedAction.LoadProject -> {
                        val test_string = "Some String"
                        JSONList(json_name, JSONString(test_string))
                    }

                    // ------- Single Int Argument ----------------
                    ActionTracker.TrackedAction.UntagColumn,
                    ActionTracker.TrackedAction.AdjustSelection,
                    ActionTracker.TrackedAction.MuteChannel,
                    ActionTracker.TrackedAction.UnMuteChannel,
                    ActionTracker.TrackedAction.CopyGlobalCtlToBeat,
                    ActionTracker.TrackedAction.MoveGlobalCtlToBeat,
                    ActionTracker.TrackedAction.SetPanAtCursor,
                    ActionTracker.TrackedAction.InsertLine,
                    ActionTracker.TrackedAction.RemoveLine,
                    ActionTracker.TrackedAction.RemoveChannel,
                    ActionTracker.TrackedAction.SetDuration,
                    ActionTracker.TrackedAction.RemoveBeat,
                    ActionTracker.TrackedAction.InsertBeat,
                    ActionTracker.TrackedAction.SetDurationCtl,
                    ActionTracker.TrackedAction.SetVolumeAtCursor,
                    ActionTracker.TrackedAction.SetVelocityAtCursor,
                    ActionTracker.TrackedAction.SetOffset,
                    ActionTracker.TrackedAction.SetOctave,
                    ActionTracker.TrackedAction.SplitLeaf,
                    ActionTracker.TrackedAction.InsertLeaf,
                    ActionTracker.TrackedAction.RemoveLeaf,
                    ActionTracker.TrackedAction.SetPercussionInstrument,
                    ActionTracker.TrackedAction.SetRelativeMode,
                    ActionTracker.TrackedAction.CursorSelectChannel,
                    ActionTracker.TrackedAction.CursorSelectColumn -> {
                        JSONList(json_name, JSONInteger(5))
                    }


                    ActionTracker.TrackedAction.RepeatSelectionCtlChannel,
                    ActionTracker.TrackedAction.CursorSelectGlobalCtlRange -> {
                        val string = JSONString("test")
                        val test_ints = listOf(0,1)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }
                    ActionTracker.TrackedAction.RepeatSelectionCtlLine,
                    ActionTracker.TrackedAction.CursorSelectChannelCtlRange -> {
                        val string = JSONString("test")
                        val test_ints = listOf(0,1,2)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }
                    ActionTracker.TrackedAction.CursorSelectLineCtlRange,
                    ActionTracker.TrackedAction.CursorSelectLeafCtlGlobal,
                    ActionTracker.TrackedAction.CursorSelectLeafCtlChannel,
                    ActionTracker.TrackedAction.CursorSelectLeafCtlLine -> {
                        val string = JSONString("test")
                        val test_ints = listOf(0,1,2,3,4,5)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionTracker.TrackedAction.CursorSelectLineCtlLine -> {
                        val string = JSONString("Pan")
                        val test_ints = listOf(0,1)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionTracker.TrackedAction.RepeatSelectionCtlGlobal,
                    ActionTracker.TrackedAction.CursorSelectChannelCtlLine -> {
                        val string = JSONString("Reverb")
                        val test_ints = listOf(0)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionTracker.TrackedAction.CursorSelectGlobalCtlLine -> {
                        val string = JSONString("Tempo")
                        JSONList(json_name, string)
                    }

                    ActionTracker.TrackedAction.InsertChannel,
                    ActionTracker.TrackedAction.SwapChannels,
                    ActionTracker.TrackedAction.MuteLine,
                    ActionTracker.TrackedAction.UnMuteLine,
                    ActionTracker.TrackedAction.CursorSelectLine -> {
                        val test_ints = listOf(0, 1)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // --------- >= 3 int args ------------------
                    ActionTracker.TrackedAction.CursorSelectLeaf,
                    ActionTracker.TrackedAction.CursorSelectRange -> {
                        val test_ints = listOf(0, 1, 2, 3, 4, 5)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // -------- 3 int args ----------------------
                    ActionTracker.TrackedAction.CopyLineCtlToBeat,
                    ActionTracker.TrackedAction.MoveLineCtlToBeat,
                    ActionTracker.TrackedAction.RepeatSelectionStd,
                    ActionTracker.TrackedAction.MoveSelectionToBeat,
                    ActionTracker.TrackedAction.CopySelectionToBeat,
                    ActionTracker.TrackedAction.MergeSelectionIntoBeat,
                    ActionTracker.TrackedAction.SetChannelInstrument -> {
                        val test_ints = listOf(4,2,6)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionTracker.TrackedAction.InsertBeatAt,
                    ActionTracker.TrackedAction.CopyChannelCtlToBeat,
                    ActionTracker.TrackedAction.MoveChannelCtlToBeat -> {
                        val test_ints = listOf(4,2)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // ------------2 + 2n args ----------------
                    ActionTracker.TrackedAction.SetTuningTable -> {
                        val test_ints = listOf(0, 12, 0, 12, 1, 12, 2, 12, 3, 12, 4, 12, 5, 12, 6, 12, 7, 12, 8, 12, 9, 12, 10, 12, 11, 12)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // ----------- Float --------------
                    ActionTracker.TrackedAction.SetTempoAtCursor -> {
                        JSONList(json_name, JSONFloat(35f))
                    }

                    ActionTracker.TrackedAction.MoveLine,
                    ActionTracker.TrackedAction.SwapLines -> {
                        val test_ints = arrayOf(0, 0, 1, 0)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // ---------- int + string -----------
                    ActionTracker.TrackedAction.TagColumn -> {
                        JSONList(json_name, JSONInteger(56), JSONString("TAGNAME"))
                    }

                    ActionTracker.TrackedAction.MoveChannel -> {
                        JSONList(json_name, JSONInteger(1), JSONInteger(3), JSONBoolean(true))
                    }
                    ActionTracker.TrackedAction.SetDelayAtCursor -> {
                        JSONList(json_name, JSONInteger(4), JSONInteger(3), JSONFloat(.75F), JSONInteger(4))
                    }
                },
                json_item
            )

            assertEquals(
                "Incorrect conversion of ${enum.name}",
                input,
                ActionTracker.from_json_entry(json_item)
            )
        }
    }
}