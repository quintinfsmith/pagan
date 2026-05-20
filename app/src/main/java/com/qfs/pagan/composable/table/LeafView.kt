/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.table

import android.view.RoundedCorner
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentCompositionContext
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.composable.is_light
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RangeOverflow
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LeafView(
    modifier: Modifier = Modifier,
    opus_manager: OpusLayerInterface,
    vm_state: ViewModelEditorState,
    controller_model: ViewModelEditorController,
    line_info: ViewModelEditorState.LineData,
    beat: Int,
    position: List<Int>,
    leaf_data: ViewModelEditorState.LeafData,
) {
    val channel_data = line_info.channel.value?.let { vm_state.channel_data[it] }
    val radix = vm_state.radix.value
    val zoom = vm_state.get_active_zoom(beat)
    val ctl_type = line_info.ctl_type.value

    val event = leaf_data.event.value
    val leaf_state = if (leaf_data.is_spillover.value) Colors.LeafState.Spill
    else if (event != null) Colors.LeafState.Active
    else Colors.LeafState.Empty

    val leaf_selection = if (leaf_data.is_selected.value) Colors.LeafSelection.Primary
    else if (leaf_data.is_secondary.value) Colors.LeafSelection.Secondary
    else Colors.LeafSelection.Unselected

    val (leaf_color, text_color, highlight_color) = Colors.get_leaf_color(
        line_info.palette.value ?: OpusColorPalette(),
        channel_data?.palette?.value ?: OpusColorPalette(),
        leaf_state,
        leaf_selection,
        line_info.ctl_type.value != null,
        line_info.is_mute.value || channel_data?.is_mute?.value == true,
        !MaterialTheme.colorScheme.is_light()
    )

    val context = LocalContext.current

    ProvideContentColorTextStyle(
        contentColor = text_color,
        textStyle = LocalTextStyle.current.merge(
            shadow = Shadow(
                blurRadius = 4F,
                color = highlight_color
            )
        )
    ) {
        Box(
            modifier
                .combinedClickable(
                    onClick = {
                        val move_mode = vm_state.move_mode.value
                        val cursor = opus_manager.cursor
                        val selecting_range = cursor.mode == CursorMode.Range
                        val channel = line_info.channel.value
                        val line_offset = line_info.line_offset.value
                        if (selecting_range && cursor.ctl_type == ctl_type) {
                            try {
                                if (ctl_type == null) {
                                    val beat_key = BeatKey(channel!!, line_offset!!, beat)
                                    opus_manager.cmove_to_beat(beat_key, move_mode)
                                } else if (line_offset != null) {
                                    val beat_key = BeatKey(channel!!, line_offset, beat)
                                    opus_manager.cmove_line_ctl_to_beat(beat_key, move_mode)
                                } else if (channel != null) {
                                    opus_manager.cmove_channel_ctl_to_beat(channel, beat, move_mode)
                                } else {
                                    opus_manager.cmove_global_ctl_to_beat(beat, move_mode)
                                }
                            } catch (_: RangeOverflow) {
                                Toast.makeText(context, R.string.range_overflow, Toast.LENGTH_SHORT).show()
                            } catch (_: MixedInstrumentException) {
                                Toast.makeText(context, R.string.feedback_mixed_copy, Toast.LENGTH_SHORT).show()
                            }
                        } else if (ctl_type == null) {
                            val beat_key = BeatKey(channel!!, line_offset!!, beat)
                            opus_manager.cursor_select(beat_key, position)

                            val tree = opus_manager.get_tree() ?: return@combinedClickable
                            if (tree.has_event()) {
                                val note = if (opus_manager.is_percussion(channel)) {
                                    opus_manager.get_percussion_instrument(channel, line_offset)
                                } else {
                                    opus_manager.get_absolute_value(beat_key, position) ?: return@combinedClickable
                                }

                                controller_model.play_event(
                                    beat_key.channel,
                                    note
                                )
                            }
                        } else if (line_offset != null) {
                            val beat_key = BeatKey(channel!!, line_offset, beat)
                            opus_manager.cursor_select_ctl_at_line(ctl_type, beat_key, position)
                        } else if (channel != null) {
                            opus_manager.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)
                        } else {
                            opus_manager.cursor_select_ctl_at_global(ctl_type, beat, position)
                        }
                    },
                    onLongClick = {
                        val channel = line_info.channel.value
                        val line_offset = line_info.line_offset.value
                        if (ctl_type == null) {
                            val beat_key = BeatKey(channel!!, line_offset!!, beat)
                            opus_manager.cursor_select_range_next(beat_key)
                        } else if (line_offset != null) {
                            val beat_key = BeatKey(channel!!, line_offset, beat)
                            opus_manager.cursor_select_line_ctl_range_next(ctl_type, beat_key)
                        } else if (channel != null) {
                            opus_manager.cursor_select_channel_ctl_range_next(ctl_type, channel, beat)
                        } else {
                            opus_manager.cursor_select_global_ctl_range_next(ctl_type, beat)
                        }
                    }
                )
                .fillMaxHeight()
                .background(leaf_color),
            contentAlignment = Alignment.Center
        ) {
            if (leaf_selection == Colors.LeafSelection.Primary) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .padding(Dimensions.SelectionBorderPadding)
                        .dashed_border(
                            color = text_color,
                            shape = RectangleShape
                        )
                )
            } else {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .padding(Dimensions.HighlightBorderPadding)
                        .border(
                            width = 2.dp,
                            color = highlight_color
                        )
                )
            }

            if (zoom * leaf_data.weight.floatValue >= 1F) {
                when (event) {
                    is AbsoluteNoteEvent -> {
                        val octave = event.note / radix
                        val offset = event.note % radix
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.Center) {
                                Spacer(Modifier.weight(4F))
                                ProvideTextStyle(Typography.Leaf.Octave) {
                                    Text("$octave")
                                }
                                Spacer(Modifier.weight(1F))
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                ProvideTextStyle(Typography.Leaf.Offset) {
                                    Text("$offset")
                                }
                            }
                        }
                    }

                    is RelativeNoteEvent -> {
                        val octave = abs(event.offset) / radix
                        val offset = abs(event.offset) % radix
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                                    Text(
                                        text = if (event.offset > 0) "+" else "-",
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                                ProvideTextStyle(Typography.Leaf.Octave) {
                                    Text(
                                        text = "$octave",
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(1.dp))
                            ProvideTextStyle(Typography.Leaf.Offset) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("$offset")
                                }
                            }
                        }
                    }

                    is PercussionEvent -> Icon(
                        modifier = Modifier.padding(8.dp),
                        painter = painterResource(R.drawable.percussion_indicator),
                        contentDescription = ""
                    )

                    else -> {
                        ProvideTextStyle(Typography.EffectLeaf) {
                            when (event) {
                                is OpusVolumeEvent -> Text("${(event.value * 100F).toInt()}%", color = text_color)
                                is OpusPanEvent -> {
                                    Text(
                                        text = if (event.value > 0) {
                                            "<${(abs(event.value) * 10).roundToInt()}"
                                        } else if (event.value < 0) {
                                            "${(abs(event.value) * 10).roundToInt()}>"
                                        } else {
                                            "-0-"
                                        },
                                        color = text_color
                                    )
                                }

                                is DelayEvent -> {
                                    if (event.echo == 0 || event.fade == 0F) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(
                                                color = text_color,
                                                radius = (size.height * .1F),
                                                center = Offset(size.width / 2F, size.height / 2F)
                                            )
                                        }
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(Dimensions.LeafBaseWidth)
                                            ) {
                                                drawLine(
                                                    start = Offset((.1F * size.width), (.65F * size.height)),
                                                    end = Offset((size.width * .9F), (.35F * size.height)),
                                                    color = text_color,
                                                    strokeWidth = 1F
                                                )
                                            }
                                            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                                                Row(horizontalArrangement = Arrangement.Center) {
                                                    Column(
                                                        verticalArrangement = Arrangement.Top,
                                                        modifier = Modifier.fillMaxHeight(),
                                                        content = {
                                                            Text("${event.numerator}")
                                                        }
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Column(
                                                        verticalArrangement = Arrangement.Bottom,
                                                        modifier = Modifier.fillMaxHeight(),
                                                        content = {
                                                            Text("${event.denominator}")
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                is OpusTempoEvent -> Text(
                                    "${event.value.roundToInt()}",
                                    color = text_color,
                                    style = Typography.EffectLeaf
                                )

                                is OpusVelocityEvent -> {
                                    var label_string = "${(event.value * 100F).toInt()}"
                                    if (event.slide != null) {
                                        label_string += "^"
                                    }
                                    Text(
                                        label_string,
                                        color = text_color,
                                        style = Typography.EffectLeaf
                                    )
                                }

                                null -> {}
                            }
                        }
                    }
                }
            }
            TableLine(MaterialTheme.colorScheme.onBackground)
        }
    }
}