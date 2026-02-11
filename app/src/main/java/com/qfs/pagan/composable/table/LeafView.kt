package com.qfs.pagan.composable.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.composable.is_light
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LeafView(
    channel_data: ViewModelEditorState.ChannelData?,
    line_data: ViewModelEditorState.LineData,
    leaf_data: ViewModelEditorState.LeafData,
    radix: Int,
    modifier: Modifier = Modifier
) {
    val event = leaf_data.event.value
    val leaf_state = if (leaf_data.is_spillover.value) Colors.LeafState.Spill
    else if (event != null) Colors.LeafState.Active
    else Colors.LeafState.Empty

    val leaf_selection = if (leaf_data.is_selected.value) Colors.LeafSelection.Primary
    else if (leaf_data.is_secondary.value) Colors.LeafSelection.Secondary
    else Colors.LeafSelection.Unselected

    val (leaf_color, text_color) = Colors.get_leaf_color(
        line_data.palette.value ?: OpusColorPalette(),
        channel_data?.palette?.value ?: OpusColorPalette(),
        leaf_state,
        leaf_selection,
        line_data.ctl_type.value != null,
        line_data.is_mute.value || channel_data?.is_mute?.value == true,
        !MaterialTheme.colorScheme.is_light()
    )

    ProvideContentColorTextStyle(contentColor = text_color) {
        Box(
            modifier
                .fillMaxHeight()
                .background(color = leaf_color),
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
            }
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
                                    text = if (event.value < 0) {
                                        "<${(abs(event.value) * 10).roundToInt()}"
                                    } else if (event.value > 0) {
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

                            is OpusVelocityEvent -> Text(
                                "${(event.value * 100F).toInt()}%",
                                color = text_color,
                                style = Typography.EffectLeaf
                            )

                            null -> {}
                        }
                    }
                }
            }
            TableLine(MaterialTheme.colorScheme.onBackground)
        }
    }
}