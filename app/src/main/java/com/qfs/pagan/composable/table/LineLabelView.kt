package com.qfs.pagan.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.EffectResourceMap
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.ui.theme.merge
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.ceil

@Composable
fun LineLabelView(
    modifier: Modifier = Modifier,
    opus_manager: OpusLayerInterface,
    vm_state: ViewModelEditorState,
    repeat_selection_dialog_visibility: MutableState<Boolean>,
    repeat_select_dialog_default: MutableIntState,
    y: Int
) {
    val line_info = vm_state.line_data[y]
    val active_cursor = vm_state.active_cursor.value
    val channel = line_info.channel.value
    val line_offset = line_info.line_offset.value
    val ctl_type = line_info.ctl_type.value

    val is_mute = line_info.is_mute.value || (channel != null && vm_state.channel_data[channel].is_mute.value)
    val is_selected = !line_info.is_selected.value && !line_info.is_secondary.value
    val is_percussion = line_info.channel.value != null && vm_state.channel_data[line_info.channel.value!!].percussion.value

    var (background, foreground) = if (is_selected) {
        Pair(
            Colors.active_color_scheme.button_line,
            Colors.active_color_scheme.button_line_foreground
        )
    } else {
        Pair(
            Colors.active_color_scheme.button_line_selected,
            Colors.active_color_scheme.button_line_selected_foreground
        )
    }

    if (is_mute) {
        background = Colors.active_color_scheme.muted(true, background)
        foreground = Colors.active_color_scheme.muted(true, foreground)
    }
    val text_style = if (ctl_type == null) {
        Typography.LineLabel
    } else {
        Typography.LineCtlLabel
    }
    ProvideContentColorTextStyle(foreground, text_style) {
        HalfBorderBox(
            modifier
                .testTag(
                    TestTag.LineLabel,
                    line_info.channel.value,
                    line_info.line_offset.value,
                    line_info.ctl_type.value
                )
                .combinedClickable(
                    onClick = {
                        val cursor = opus_manager.cursor
                        when (cursor.mode) {
                            CursorMode.Range -> {
                                if (opus_manager.selected_for_repetition(channel, line_offset, ctl_type)) {
                                    val cursor = opus_manager.cursor
                                    val (first, second) = cursor.get_ordered_range()!!
                                    repeat_select_dialog_default.intValue = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
                                    repeat_selection_dialog_visibility.value = true
                                } else {
                                    opus_manager.fuzzy_select_line(channel, line_offset, ctl_type)
                                }
                            }

                            CursorMode.Line -> {
                                val ctl_level = if (ctl_type == null) null
                                else if (channel == null) CtlLineLevel.Global
                                else if (line_offset == null) CtlLineLevel.Channel
                                else CtlLineLevel.Line

                                if (channel == cursor.channel && line_offset == cursor.line_offset && ctl_type == cursor.ctl_type && ctl_level == cursor.ctl_level) {
                                    opus_manager.cursor_select_channel(channel)
                                } else {
                                    opus_manager.fuzzy_select_line(channel, line_offset, ctl_type)
                                }
                            }

                            CursorMode.Single,
                            CursorMode.Beat,
                            CursorMode.Channel,
                            CursorMode.Unset -> {
                                opus_manager.fuzzy_select_line(channel, line_offset, ctl_type)
                            }
                        }
                    },
                    onLongClick = {
                        if (opus_manager.selected_for_repetition(channel, line_offset, ctl_type)) {
                            opus_manager.repeat_selection(null)
                        } else {
                            opus_manager.fuzzy_select_line(channel, line_offset, ctl_type)
                        }
                    }
                )
                .background(
                    shape = RectangleShape,
                    color = background
                ),
            border_color = Colors.active_color_scheme.table_line,
            content = {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (active_cursor?.type == CursorMode.Range && (line_info.is_selected.value || line_info.is_secondary.value)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.icon_repeat),
                            contentDescription = stringResource(R.string.repeat_selection_in_line),
                            tint = Colors.active_color_scheme.button_line_selected.merge(Color(0xFFFFFFFF))
                        )
                    } else if (line_info.is_selected.value) {
                        Spacer(
                            Modifier
                                .fillMaxSize()
                                .padding(Dimensions.SelectionBorderPadding)
                                .border(
                                    width = 1.dp,
                                    color = foreground,
                                    shape = RectangleShape
                                )
                        )
                    }

                    line_info.channel.value?.let { channel ->
                        val label_a = if (ctl_type == null || line_info.line_offset.value == null) {
                            if (is_percussion) {
                                "!$channel"
                            } else {
                                "$channel"
                            }
                        } else {
                            ""
                        }

                        val label_b  = if (line_info.assigned_offset.value != null) {
                            "${line_info.assigned_offset.value!!}"
                        } else if (line_info.line_offset.value != null) {
                            "${line_info.line_offset.value!!}"
                        } else {
                            ""
                        }

                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(if (ctl_type != null) {
                                    Dimensions.LineCtlLabelPadding
                                } else {
                                    Dimensions.LineLabelPadding
                                }),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(label_a, maxLines = 1)
                            }
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = label_b,
                                    maxLines = 1,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    if (ctl_type != null) {
                        val (drawable_id, description_id) = EffectResourceMap[ctl_type]
                        Icon(
                            modifier = Modifier.padding(
                                if (line_info.line_offset.value != null) {
                                    Dimensions.LineLabelIconPaddingLine
                                } else if (line_info.channel.value != null) {
                                    Dimensions.LineLabelIconPaddingChannel
                                } else {
                                    Dimensions.LineLabelIconPaddingGlobal
                                }
                            ),
                            painter = painterResource(drawable_id),
                            contentDescription = stringResource(description_id)
                        )
                    }
                }
            }
        )
    }
}