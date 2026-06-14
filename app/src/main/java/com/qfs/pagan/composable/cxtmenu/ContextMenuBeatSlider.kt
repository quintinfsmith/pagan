package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ContextMenuBeatSliderPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) { }

@Composable
fun ContextMenuBeatSliderSecondary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val slider_position = remember {
        mutableFloatStateOf(
            when (vm_state.active_cursor.value?.type) {
                CursorMode.Beat -> vm_state.active_cursor.value!!.ints[0]
                CursorMode.Single -> vm_state.active_cursor.value!!.ints[1]
                CursorMode.Range -> vm_state.active_cursor.value!!.ints[1]

                null,
                CursorMode.Channel,
                CursorMode.Unset,
                CursorMode.Line -> vm_state.scroll_state_x.value.firstVisibleItemIndex
            }.toFloat()
        )
    }

    //Box(
    //    modifier = Modifier.fillMaxWidth(),
    //    contentAlignment = Alignment.Center
    //) {
    //    DialogTitle(
    //        stringResource(
    //            R.string.label_shortcut_scrollbar,
    //            slider_position.floatValue.toInt(),
    //            opus_manager.length - 1
    //        )
    //    )
    //}
    Row (verticalAlignment = Alignment.CenterVertically) {
        // TODO:  Use a UI variable here instead of accessing opus_manager.marked_sections
        if (opus_manager.marked_sections.isNotEmpty()) {
            val section_dropdown_visible = remember { mutableStateOf(false) }
            Box {
                IconCMenuButton(
                    onClick = {
                        section_dropdown_visible.value = !section_dropdown_visible.value
                    },
                    shape = Shapes.ContextMenuButtonPrimaryStart,
                    icon = R.drawable.icon_tag,
                    description = R.string.jump_to_section
                )

                DropdownMenu(
                    onDismissRequest = { section_dropdown_visible.value = false },
                    expanded = section_dropdown_visible.value
                ) {
                    var section_index = 0
                    for ((i, tag) in opus_manager.marked_sections.toList()
                        .sortedBy { it.first }) {
                        DropdownMenuItem(
                            onClick = {
                                vm_state.selecting_beat.value = false
                                section_dropdown_visible.value = false
                                opus_manager.cursor_select_column(i)
                            },
                            text = {
                                if (tag == null) {
                                    Text(
                                        stringResource(
                                            R.string.section_spinner_item,
                                            i,
                                            section_index
                                        )
                                    )
                                } else {
                                    Text("${"%02d".format(i)}: $tag")
                                }
                            }
                        )
                        section_index++
                    }
                }
            }
        }

        Slider(
            modifier = Modifier
                .weight(1F, fill = false)
                .padding(horizontal = 8.dp),
            value = slider_position.floatValue / (vm_state.beat_count.value - 1),
            valueRange = 0F..(opus_manager.length - 1).toFloat(),
            onValueChange = { value ->
                slider_position.floatValue = value * (vm_state.beat_count.value - 1)
                opus_manager.cursor_select_column(value.toInt())
            }
        )

        TextCMenuButton(
            onClick = {
                opus_manager.cursor_select_column(vm_state.beat_count.value - 1)
            },
            shape = Shapes.ContextMenuButtonPrimaryEnd,
            text = "/${vm_state.beat_count.value}"
        )
    }
}




