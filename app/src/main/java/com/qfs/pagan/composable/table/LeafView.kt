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

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.Values
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.table.leaf_inset
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.InvalidOverwriteCall
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RangeOverflow
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.FilterEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Colors.LeafSelection
import com.qfs.pagan.ui.theme.Colors.LeafState
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
    )

    val context = LocalContext.current


    ProvideContentColorTextStyle(contentColor = text_color) {
        Row(modifier) {
            Box(
                Modifier
                    .LeafViewWrapModifier(leaf_selection, leaf_state, leaf_color)
                    .fillMaxHeight()
                    .weight(1F)
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
                                        opus_manager.move_effect_to_line_beat(
                                            beat_key,
                                            move_mode
                                        )
                                    } else if (channel != null) {
                                        opus_manager.move_effect_to_channel_beat(
                                            channel,
                                            beat,
                                            move_mode
                                        )
                                    } else {
                                        opus_manager.move_effect_to_global_beat(beat, move_mode)
                                    }
                                } catch (_: RangeOverflow) {
                                    Toast.makeText(
                                        context,
                                        R.string.range_overflow,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (_: MixedInstrumentException) {
                                    Toast.makeText(
                                        context,
                                        R.string.feedback_mixed_copy,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (_: InvalidOverwriteCall) {

                                }
                            } else if (ctl_type == null) {
                                val beat_key = BeatKey(channel!!, line_offset!!, beat)
                                opus_manager.cursor_select(beat_key, position)

                                val tree = opus_manager.get_tree() ?: return@combinedClickable
                                if (vm_state.soundfont_ready.value && tree.has_event()) {
                                    val note = if (opus_manager.is_percussion(channel)) {
                                        opus_manager.get_percussion_instrument(
                                            channel,
                                            line_offset
                                        )
                                    } else {
                                        opus_manager.get_absolute_value(beat_key, position)
                                            ?: return@combinedClickable
                                    }

                                    controller_model.play_event(
                                        beat_key.channel,
                                        note
                                    )
                                }
                            } else if (line_offset != null) {
                                val beat_key = BeatKey(channel!!, line_offset, beat)
                                opus_manager.cursor_select_ctl_at_line(
                                    ctl_type,
                                    beat_key,
                                    position
                                )
                            } else if (channel != null) {
                                opus_manager.cursor_select_ctl_at_channel(
                                    ctl_type,
                                    channel,
                                    beat,
                                    position
                                )
                            } else {
                                opus_manager.cursor_select_ctl_at_global(
                                    ctl_type,
                                    beat,
                                    position
                                )
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
                                opus_manager.cursor_select_line_ctl_range_next(
                                    ctl_type,
                                    beat_key
                                )
                            } else if (channel != null) {
                                opus_manager.cursor_select_channel_ctl_range_next(
                                    ctl_type,
                                    channel,
                                    beat
                                )
                            } else {
                                opus_manager.cursor_select_global_ctl_range_next(ctl_type, beat)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                LeafViewWrap(leaf_selection, leaf_state) {
                    if (zoom * leaf_data.weight.floatValue >= 1F) {
                        val radix = vm_state.radix.value
                        when (event) {
                            is AbsoluteNoteEvent -> LeafViewAbsoluteNoteEvent(event, radix)
                            is RelativeNoteEvent -> LeafViewRelativeNoteEvent(event, radix)
                            is PercussionEvent -> LeafViewPercussionEvent(event)
                            is OpusVolumeEvent -> LeafViewOpusVolumeEvent(event)
                            is OpusPanEvent -> LeafViewOpusPanEvent(event)
                            is DelayEvent -> LeafViewDelayEvent(event)
                            is OpusTempoEvent -> LeafViewOpusTempoEvent(event)
                            is OpusVelocityEvent -> LeafViewOpusVelocityEvent(event)
                            is FilterEvent -> LeafViewFilter(event)
                            null -> {}
                        }
                    }
                }
            }
            TableLine(Colors.active_color_scheme.table_line)
        }
    }
}

@Composable
fun Modifier.LeafViewWrapModifier(
    leaf_selection: LeafSelection,
    leaf_state: LeafState,
    leaf_color: Color,
): Modifier {
    return this
        .background(leaf_color)
        .leaf_inset(leaf_state, leaf_selection)
}
@Composable
fun BoxScope.LeafViewWrap(
    leaf_selection: LeafSelection,
    leaf_state: LeafState,
    content: @Composable (BoxScope.() -> Unit)
) {
    content()
    if (leaf_selection == LeafSelection.Primary) {
        Spacer(
            Modifier
                .fillMaxSize()
                .padding(Dimensions.SelectionBorderPadding)
                .border(
                    width = 1.dp,
                    color = LocalContentColor.current,
                    shape = RectangleShape
                )
        )
    }
}

@Composable
fun LeafViewAbsoluteNoteEvent(event: AbsoluteNoteEvent, radix: Int) {
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

@Composable
fun LeafViewRelativeNoteEvent(event: RelativeNoteEvent, radix: Int) {
    val octave = abs(event.offset) / radix
    val offset = abs(event.offset) % radix
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            ProvideTextStyle(Typography.Leaf.RelativePrefix) {
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

@Composable
fun LeafViewPercussionEvent(event: PercussionEvent) {
    Icon(
        modifier = Modifier.padding(8.dp),
        painter = painterResource(R.drawable.percussion_indicator),
        contentDescription = ""
    )
}
@Composable
fun LeafViewOpusVolumeEvent(event: OpusVolumeEvent) {
    Text(
        "${(event.value * 100F).roundToInt()}%",
        style = Typography.EffectLeaf
    )
}

@Composable
fun LeafViewOpusPanEvent(event: OpusPanEvent) {
    Text(
        text = if (event.value > 0) {
            "<${(abs(event.value) * 10).roundToInt()}"
        } else if (event.value < 0) {
            "${(abs(event.value) * 10).roundToInt()}>"
        } else {
            "-0-"
        },
        style = Typography.EffectLeaf
    )
}

@Composable
fun LeafViewDelayEvent(event: DelayEvent) {
    ProvideTextStyle(Typography.EffectLeaf) {
        val text_color = LocalContentColor.current
        if (event.echo == 0 || event.fade == 0F) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = text_color,
                    radius = (size.height * .1F),
                    center = Offset(
                        size.width / 2F,
                        size.height / 2F
                    )
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
                        start = Offset(
                            (.1F * size.width),
                            (.5F * size.height)
                        ),
                        end = Offset(
                            (size.width * .9F),
                            (.5F * size.height)
                        ),
                        color = text_color,
                        strokeWidth = 1F
                    )
                }
                ProvideTextStyle(Typography.EffectLeafDelay) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .fillMaxHeight(),
                        content = {
                            Text(
                                "${event.numerator}",
                                maxLines = 1,
                                lineHeight = 1.em
                            )
                            Text(
                                "${event.denominator}",
                                maxLines = 1,
                                lineHeight = 1.em
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LeafViewOpusTempoEvent(event: OpusTempoEvent) {
    val text_color = LocalContentColor.current
    Text(
        "${event.value.roundToInt()}",
        color = text_color,
        style = Typography.EffectLeaf
    )
}

@Composable
fun LeafViewOpusVelocityEvent(event: OpusVelocityEvent) {
    var label_string = "${(event.value * 100F).toInt()}"
    if (event.slide != null) {
        label_string += "^"
    }
    Text(
        label_string,
        style = Typography.EffectLeaf
    )
}
@Composable
fun LeafViewFilter(event: FilterEvent) {
    ProvideTextStyle(Typography.EffectLeaf) {
        if ((event is HighPassEvent && event.filter_cutoff <= Values.LowPassMinimum) || (event is LowPassEvent && event.filter_cutoff >= Values.LowPassMaximum)) {
            val text_color = LocalContentColor.current
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = text_color,
                    radius = (size.height * .1F),
                    center = Offset(
                        size.width / 2F,
                        size.height / 2F
                    )
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (event.filter_cutoff >= 1000F) {
                        stringResource(
                            R.string.low_pass_event_text_top,
                            event.filter_cutoff / 1000F
                        )
                    } else {
                        "${event.filter_cutoff.roundToInt()}"
                    },
                    textAlign = TextAlign.Center,
                    style = Typography.EffectLeafFilterTop
                )
                Text(
                    if (event.filter_cutoff >= 1000F) {
                        R.string.khz
                    } else {
                        R.string.hz
                    },
                    textAlign = TextAlign.Center,
                    style = Typography.EffectLeafFilterKhz
                )
            }
        }
    }
}

@Composable
fun Modifier.inset(): Modifier {
    return this.drawWithCache {
        val shadow_width = 10F
        val hbrush = Brush.horizontalGradient(
            0F to Color(0x44000000),
            1F to Color.Transparent,
            startX = 0F,
            endX = shadow_width
        )

        val vbrush = Brush.verticalGradient(
            0F to Color(0x44000000),
            1F to Color.Transparent,
            startY = 0F,
            endY = shadow_width
        )

        val vpath = Path()
        vpath.moveTo(0F,0F)
        vpath.lineTo(shadow_width, shadow_width)
        vpath.lineTo(this.size.width, shadow_width)
        vpath.lineTo(this.size.width, 0F)
        vpath.close()

        val hpath = Path()
        hpath.moveTo(0F,0F)
        hpath.lineTo(shadow_width, shadow_width)
        hpath.lineTo(shadow_width, this.size.height)
        hpath.lineTo(0F, this.size.height)
        hpath.close()

        onDrawWithContent {
            drawContent()
            drawPath(vpath, vbrush)
            drawPath(hpath, hbrush)
        }
    }
}

@Composable
fun Modifier.outset(): Modifier {
    return this.drawWithCache {
        val highlight_width = 4F
        val top_path = Path()
        top_path.moveTo(0F,0F)
        top_path.lineTo(highlight_width, highlight_width)
        top_path.lineTo(this.size.width - highlight_width, highlight_width)
        top_path.lineTo(this.size.width, 0F)
        top_path.close()

        val start_path = Path()
        start_path.moveTo(0F,0F)
        start_path.lineTo(highlight_width, highlight_width)
        start_path.lineTo(highlight_width, this.size.height - highlight_width)
        start_path.lineTo(0F, this.size.height)
        start_path.close()


        val top_brush = Brush.verticalGradient(
            0F to Color(0x88FFFFFF),
            1F to Color.Transparent,
            startY = 0F,
            endY = highlight_width
        )
        val start_brush = Brush.horizontalGradient(
            0F to Color(0x88FFFFFF),
            1F to Color.Transparent,
            startX = 0F,
            endX = highlight_width
        )
        val bottom_path = Path()
        bottom_path.moveTo(0F,this.size.height)
        bottom_path.lineTo(highlight_width, this.size.height - highlight_width)
        bottom_path.lineTo(this.size.width - highlight_width, this.size.height - highlight_width)
        bottom_path.lineTo(this.size.width, this.size.height)
        bottom_path.close()

        val end_path = Path()
        end_path.moveTo(this.size.width - highlight_width, highlight_width)
        end_path.lineTo(this.size.width - highlight_width, this.size.height - highlight_width)
        end_path.lineTo(this.size.width, this.size.height)
        end_path.lineTo(this.size.width,0F)
        end_path.close()

        val bottom_brush = Brush.verticalGradient(
            0F to Color.Transparent,
            1F to Color(0x44000000),
            startY = this.size.height - highlight_width,
            endY = this.size.height
        )
        val end_brush = Brush.horizontalGradient(
            0F to Color.Transparent,
            1F to Color(0x44000000),
            startX = this.size.width - highlight_width,
            endX = this.size.width
        )
        onDrawWithContent {
            drawContent()
            drawPath(top_path, top_brush)
            drawPath(start_path, start_brush)
            drawPath(bottom_path, bottom_brush)
            drawPath(end_path, end_brush)
        }
    }
}

@Composable
fun Modifier.leaf_inset(leaf_state: Colors.LeafState, selection: Colors.LeafSelection): Modifier {
    return if (leaf_state == LeafState.Empty && selection != LeafSelection.Primary) {
        this.inset()
    } else {
        this
    }
}

