package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        // TODO:  Use a UI variable here instead of accessing opus_manager.marked_sections
        if (opus_manager.marked_sections.isNotEmpty()) {
            val section_dropdown_visible = remember { mutableStateOf(false) }
            Box {
                IconCMenuButton(
                    onClick = {
                        section_dropdown_visible.value = !section_dropdown_visible.value
                    },
                    icon = R.drawable.icon_tag_jump,
                    description = R.string.jump_to_section,
                    shape = Shapes.ContextMenuButtonPrimaryStart
                )

                DropdownMenu(
                    onDismissRequest = { section_dropdown_visible.value = false },
                    expanded = section_dropdown_visible.value
                ) {
                    var section_index = 0
                    var max_beat_factor = 1
                    val marked_sections = opus_manager.marked_sections.toList().sortedBy { it.first }
                    for ((i, tag) in marked_sections) {
                        max_beat_factor = max(max_beat_factor,log(i.toFloat(), 10F).toInt())
                    }

                    section_index = 0
                    for ((i, tag) in marked_sections) {
                        DropdownMenuItem(
                            onClick = {
                                vm_state.selecting_beat.value = false
                                section_dropdown_visible.value = false
                                opus_manager.cursor_select_column(i)
                            },
                            text = {
                                val beat_string = "%0${max_beat_factor + 1}d".format(i)
                                val section_string = "%02d".format(section_index)
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        modifier = Modifier.padding(end = Dimensions.SectionMenuInternalPadding),
                                        text = "$section_string | $beat_string)"
                                    )
                                    Text(tag ?: stringResource(R.string.untitled_section))
                                }
                            }
                        )
                        section_index++
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.width(Dimensions.ContextMenuButtonWidth))
        }

        Column(
            modifier = Modifier.weight(1F, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    textAlign = TextAlign.Center,
                    text = "${slider_position.floatValue.roundToInt()}",
                    modifier = Modifier.padding(PaddingValues(horizontal = 16.dp))
                )
                Text("/")
                Text(
                    textAlign = TextAlign.Center,
                    text = "${vm_state.beat_count.value - 1}",
                    modifier = Modifier.padding(PaddingValues(horizontal = 16.dp))
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = slider_position.floatValue,
                    valueRange = 0F .. (vm_state.beat_count.value - 1).toFloat(),
                    onValueChange = { value ->
                        slider_position.floatValue = value
                        opus_manager.cursor_select_column(min(vm_state.beat_count.value - 1, value.roundToInt()))
                    },
                    onValueChangeFinished = {
                        slider_position.floatValue = slider_position.floatValue.roundToInt().toFloat()
                    }
                )
            }
        }
        Box(
            Modifier
                .width(41.dp)
                .height(41.dp)
                .clip(CircleShape)
                .clickable { vm_state.selecting_beat.value = false },
            contentAlignment = Alignment.Center
        ) {
            ProvideContentColorTextStyle(MaterialTheme.colorScheme.primary) {
                Icon(
                    painter = painterResource(R.drawable.icon_cross_circle),
                    contentDescription = stringResource(R.string.close_beat_selector),
                )
            }
        }
    }
}




