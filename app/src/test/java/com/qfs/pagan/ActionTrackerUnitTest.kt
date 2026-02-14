package com.qfs.pagan

import com.qfs.json.JSONBoolean
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ActionTrackerUnitTest {
    @Test
    fun test_to_json() {
        for (enum in ActionDispatcher.TrackedAction.values()) {
            val input = when (enum) {
                // -------------- No arguments --------------
                ActionDispatcher.TrackedAction.RemoveController,
                ActionDispatcher.TrackedAction.ToggleControllerVisibility,
                ActionDispatcher.TrackedAction.Unset,
                ActionDispatcher.TrackedAction.UnsetRoot,
                ActionDispatcher.TrackedAction.SaveProject,
                ActionDispatcher.TrackedAction.DeleteProject,
                ActionDispatcher.TrackedAction.CopyProject,
                ActionDispatcher.TrackedAction.TogglePercussion,
                ActionDispatcher.TrackedAction.DrawerOpen,
                ActionDispatcher.TrackedAction.DrawerClose,
                ActionDispatcher.TrackedAction.NewProject,
                ActionDispatcher.TrackedAction.ApplyUndo -> {
                    Pair(enum, listOf())
                }


                // Special C
                ActionDispatcher.TrackedAction.SetProjectNameAndNotes -> {
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
                ActionDispatcher.TrackedAction.ShowLineController,
                ActionDispatcher.TrackedAction.ShowGlobalController,
                ActionDispatcher.TrackedAction.ShowChannelController,
                ActionDispatcher.TrackedAction.ImportSong,
                ActionDispatcher.TrackedAction.SetCopyMode -> {
                    val test_string = "Some String"
                    val test_bytes = test_string.toByteArray()
                    Pair(enum, List(test_bytes.size) { i: Int -> test_bytes[i].toInt() })
                }

                // ------- Single Int Argument ----------------
                ActionDispatcher.TrackedAction.UntagColumn,
                ActionDispatcher.TrackedAction.AdjustSelection,
                ActionDispatcher.TrackedAction.MuteChannel,
                ActionDispatcher.TrackedAction.UnMuteChannel,
                ActionDispatcher.TrackedAction.CopyGlobalCtlToBeat,
                ActionDispatcher.TrackedAction.MoveGlobalCtlToBeat,
                ActionDispatcher.TrackedAction.InsertLine,
                ActionDispatcher.TrackedAction.RemoveLine,
                ActionDispatcher.TrackedAction.RemoveChannel,
                ActionDispatcher.TrackedAction.SetDuration,
                ActionDispatcher.TrackedAction.RemoveBeat,
                ActionDispatcher.TrackedAction.InsertBeat,
                ActionDispatcher.TrackedAction.SplitLeaf,
                ActionDispatcher.TrackedAction.InsertLeaf,
                ActionDispatcher.TrackedAction.RemoveLeaf,
                ActionDispatcher.TrackedAction.SetPercussionInstrument,
                ActionDispatcher.TrackedAction.CursorSelectChannel,
                ActionDispatcher.TrackedAction.CursorSelectColumn -> {
                    Pair(enum, listOf(5))
                }

                ActionDispatcher.TrackedAction.RepeatSelectionCtlChannel,
                ActionDispatcher.TrackedAction.CursorSelectGlobalCtlRange -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1)
                    )
                }
                ActionDispatcher.TrackedAction.RepeatSelectionCtlLine,
                ActionDispatcher.TrackedAction.CursorSelectChannelCtlRange -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1, 2)
                    )
                }
                ActionDispatcher.TrackedAction.CursorSelectLineCtlRange -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1, 2, 3, 4, 5)
                    )
                }
                ActionDispatcher.TrackedAction.CursorSelectLeafCtlGlobal,
                ActionDispatcher.TrackedAction.CursorSelectLeafCtlChannel,
                ActionDispatcher.TrackedAction.CursorSelectLeafCtlLine -> {
                    val string = "test"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1, 2, 3, 4, 5)
                    )
                }
                ActionDispatcher.TrackedAction.CursorSelectLineCtlLine -> {
                    val string = "Pan"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0, 1)
                    )
                }
                ActionDispatcher.TrackedAction.RepeatSelectionCtlGlobal,
                ActionDispatcher.TrackedAction.CursorSelectChannelCtlLine -> {
                    val string = "Reverb"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() } + listOf(0)
                    )
                }

                ActionDispatcher.TrackedAction.CursorSelectGlobalCtlLine -> {
                    val string = "Tempo"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(bytes.size) + List(bytes.size) { bytes[it].toInt() }
                    )
                }

                //ActionTracker.TrackedAction.UntagColumn -> {
                ActionDispatcher.TrackedAction.TagColumn -> {
                    val string = "TAGNAME"
                    val bytes = string.toByteArray()
                    Pair(
                        enum,
                        listOf(56) + List(bytes.size) { bytes[it].toInt() }
                    )
                }



                ActionDispatcher.TrackedAction.InsertChannel,
                ActionDispatcher.TrackedAction.MuteLine,
                ActionDispatcher.TrackedAction.UnMuteLine,
                ActionDispatcher.TrackedAction.CursorSelectLine -> {
                    Pair(enum, listOf(0, 1))
                }


                // --------- >= 3 int args ------------------
                ActionDispatcher.TrackedAction.CursorSelectLeaf,
                ActionDispatcher.TrackedAction.CursorSelectRange -> {
                    Pair(enum, listOf(0, 1, 2, 3, 4, 5))
                }

                // -------- 3 int args ----------------------
                ActionDispatcher.TrackedAction.CopyLineCtlToBeat,
                ActionDispatcher.TrackedAction.MoveLineCtlToBeat,
                ActionDispatcher.TrackedAction.RepeatSelectionStd,
                ActionDispatcher.TrackedAction.MoveSelectionToBeat,
                ActionDispatcher.TrackedAction.CopySelectionToBeat,
                ActionDispatcher.TrackedAction.MergeSelectionIntoBeat,
                ActionDispatcher.TrackedAction.SetChannelPreset -> {
                    Pair(enum, listOf(4, 2, 6))
                }

                ActionDispatcher.TrackedAction.SetOffset,
                ActionDispatcher.TrackedAction.SetOctave -> {
                    Pair(enum, listOf(1, 0))
                }
                ActionDispatcher.TrackedAction.InsertBeatAt,
                ActionDispatcher.TrackedAction.CopyChannelCtlToBeat,
                ActionDispatcher.TrackedAction.MoveChannelCtlToBeat -> {
                    Pair(enum, listOf(4, 2))
                }

                // ------------2 + 2n args ----------------
                ActionDispatcher.TrackedAction.SetTuningTable -> {
                    Pair(enum, listOf(0, 12, 0, 12, 1, 12, 2, 12, 3, 12, 4, 12, 5, 12, 6, 12, 7, 12, 8, 12, 9, 12, 10, 12, 11, 12))
                }


                ActionDispatcher.TrackedAction.MoveLine -> {
                    Pair(enum, listOf(0, 0, 1, 0))
                }

                ActionDispatcher.TrackedAction.MoveChannel -> {
                    Pair(enum, listOf(1, 3, 1))
                }
            }

            val json_name = JSONString(enum.name)
            val json_item = ActionDispatcher.item_to_json(input)

            assertEquals(
                "Failed to convert ${enum.name} to json Correctly",
                when (enum) {
                    // -------------- No arguments --------------
                    ActionDispatcher.TrackedAction.RemoveController,
                    ActionDispatcher.TrackedAction.ToggleControllerVisibility,
                    ActionDispatcher.TrackedAction.Unset,
                    ActionDispatcher.TrackedAction.UnsetRoot,
                    ActionDispatcher.TrackedAction.SaveProject,
                    ActionDispatcher.TrackedAction.DeleteProject,
                    ActionDispatcher.TrackedAction.CopyProject,
                    ActionDispatcher.TrackedAction.TogglePercussion,
                    ActionDispatcher.TrackedAction.DrawerOpen,
                    ActionDispatcher.TrackedAction.DrawerClose,
                    ActionDispatcher.TrackedAction.NewProject,
                    ActionDispatcher.TrackedAction.ApplyUndo -> {
                        JSONList(json_name)
                    }


                    ActionDispatcher.TrackedAction.SetProjectNameAndNotes -> {
                        val project_name = "Project Name"
                        val project_notes = "Project Notes"
                        JSONList(json_name, JSONString(project_name), JSONString(project_notes))
                    }

                    // -------- Single String Argument -------------
                    ActionDispatcher.TrackedAction.ShowLineController,
                    ActionDispatcher.TrackedAction.ShowGlobalController,
                    ActionDispatcher.TrackedAction.ShowChannelController,
                    ActionDispatcher.TrackedAction.ImportSong,
                    ActionDispatcher.TrackedAction.SetCopyMode -> {
                        val test_string = "Some String"
                        JSONList(json_name, JSONString(test_string))
                    }

                    // ------- Single Int Argument ----------------
                    ActionDispatcher.TrackedAction.UntagColumn,
                    ActionDispatcher.TrackedAction.AdjustSelection,
                    ActionDispatcher.TrackedAction.MuteChannel,
                    ActionDispatcher.TrackedAction.UnMuteChannel,
                    ActionDispatcher.TrackedAction.CopyGlobalCtlToBeat,
                    ActionDispatcher.TrackedAction.MoveGlobalCtlToBeat,
                    ActionDispatcher.TrackedAction.InsertLine,
                    ActionDispatcher.TrackedAction.RemoveLine,
                    ActionDispatcher.TrackedAction.RemoveChannel,
                    ActionDispatcher.TrackedAction.SetDuration,
                    ActionDispatcher.TrackedAction.RemoveBeat,
                    ActionDispatcher.TrackedAction.InsertBeat,
                    ActionDispatcher.TrackedAction.SplitLeaf,
                    ActionDispatcher.TrackedAction.InsertLeaf,
                    ActionDispatcher.TrackedAction.RemoveLeaf,
                    ActionDispatcher.TrackedAction.SetPercussionInstrument,
                    ActionDispatcher.TrackedAction.CursorSelectChannel,
                    ActionDispatcher.TrackedAction.CursorSelectColumn -> {
                        JSONList(json_name, JSONInteger(5))
                    }


                    ActionDispatcher.TrackedAction.RepeatSelectionCtlChannel,
                    ActionDispatcher.TrackedAction.CursorSelectGlobalCtlRange -> {
                        val string = JSONString("test")
                        val test_ints = listOf(0,1)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }
                    ActionDispatcher.TrackedAction.RepeatSelectionCtlLine,
                    ActionDispatcher.TrackedAction.CursorSelectChannelCtlRange -> {
                        val string = JSONString("test")
                        val test_ints = listOf(0,1,2)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }
                    ActionDispatcher.TrackedAction.CursorSelectLineCtlRange,
                    ActionDispatcher.TrackedAction.CursorSelectLeafCtlGlobal,
                    ActionDispatcher.TrackedAction.CursorSelectLeafCtlChannel,
                    ActionDispatcher.TrackedAction.CursorSelectLeafCtlLine -> {
                        val string = JSONString("test")
                        val test_ints = listOf(0,1,2,3,4,5)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionDispatcher.TrackedAction.CursorSelectLineCtlLine -> {
                        val string = JSONString("Pan")
                        val test_ints = listOf(0,1)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionDispatcher.TrackedAction.RepeatSelectionCtlGlobal,
                    ActionDispatcher.TrackedAction.CursorSelectChannelCtlLine -> {
                        val string = JSONString("Reverb")
                        val test_ints = listOf(0)
                        JSONList(json_name, string, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionDispatcher.TrackedAction.CursorSelectGlobalCtlLine -> {
                        val string = JSONString("Tempo")
                        JSONList(json_name, string)
                    }

                    ActionDispatcher.TrackedAction.InsertChannel,
                    ActionDispatcher.TrackedAction.MuteLine,
                    ActionDispatcher.TrackedAction.UnMuteLine,
                    ActionDispatcher.TrackedAction.CursorSelectLine -> {
                        val test_ints = listOf(0, 1)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // --------- >= 3 int args ------------------
                    ActionDispatcher.TrackedAction.CursorSelectLeaf,
                    ActionDispatcher.TrackedAction.CursorSelectRange -> {
                        val test_ints = listOf(0, 1, 2, 3, 4, 5)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // -------- 3 int args ----------------------
                    ActionDispatcher.TrackedAction.CopyLineCtlToBeat,
                    ActionDispatcher.TrackedAction.MoveLineCtlToBeat,
                    ActionDispatcher.TrackedAction.RepeatSelectionStd,
                    ActionDispatcher.TrackedAction.MoveSelectionToBeat,
                    ActionDispatcher.TrackedAction.CopySelectionToBeat,
                    ActionDispatcher.TrackedAction.MergeSelectionIntoBeat,
                    ActionDispatcher.TrackedAction.SetChannelPreset -> {
                        val test_ints = listOf(4,2,6)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    ActionDispatcher.TrackedAction.SetOffset,
                    ActionDispatcher.TrackedAction.SetOctave -> {
                        JSONList(json_name, JSONInteger(1), JSONString(RelativeInputMode.Absolute.name))
                    }
                    ActionDispatcher.TrackedAction.InsertBeatAt,
                    ActionDispatcher.TrackedAction.CopyChannelCtlToBeat,
                    ActionDispatcher.TrackedAction.MoveChannelCtlToBeat -> {
                        val test_ints = listOf(4,2)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // ------------2 + 2n args ----------------
                    ActionDispatcher.TrackedAction.SetTuningTable -> {
                        val test_ints = listOf(0, 12, 0, 12, 1, 12, 2, 12, 3, 12, 4, 12, 5, 12, 6, 12, 7, 12, 8, 12, 9, 12, 10, 12, 11, 12)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // ----------- Float --------------
                    ActionDispatcher.TrackedAction.MoveLine -> {
                        val test_ints = arrayOf(0, 0, 1, 0)
                        JSONList(json_name, *Array(test_ints.size) { JSONInteger(test_ints[it]) })
                    }

                    // ---------- int + string -----------
                    ActionDispatcher.TrackedAction.TagColumn -> {
                        JSONList(json_name, JSONInteger(56), JSONString("TAGNAME"))
                    }

                    ActionDispatcher.TrackedAction.MoveChannel -> {
                        JSONList(json_name, JSONInteger(1), JSONInteger(3), JSONBoolean(true))
                    }
                },
                json_item
            )

            assertEquals(
                "Incorrect conversion of ${enum.name}",
                input,
                ActionDispatcher.from_json_entry(json_item)
            )
        }
    }
}