/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2026  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.EffectResourceMap
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.ColorPickerDialog
import com.qfs.pagan.composable.DialogMenu
import com.qfs.pagan.composable.LargeSpacer
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.SoundfontLoadingIndicator
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ShowEffectsButton(channel: Int, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, shape: Shape = Shapes.ContextMenuButtonPrimaryStart) {
    val menu_visibility = remember { mutableStateOf(false) }
    val multi_dialog_visibility = remember { mutableStateOf(false) }
    val selected_ctl_type = remember { mutableStateOf<EffectType?>(null) }

    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelEffects),
        onClick = { menu_visibility.value = !menu_visibility.value },
        icon = R.drawable.icon_ctl,
        shape = shape,
        description = R.string.cd_show_effect_controls
    )

    if (menu_visibility.value) {
        DialogMenu(
            menu_visibility,
            R.string.show_channel_controls,
            {
                val options = mutableListOf<Pair<EffectType, @Composable RowScope.() -> Unit>>( )
                for (ctl_type in OpusLayerInterface.channel_controller_domain) {
                    if (opus_manager.is_channel_ctl_visible(ctl_type, channel)) continue
                    options.add(
                        Pair(ctl_type) {
                            Icon(
                                modifier = Modifier.width(Dimensions.EffectDialogIconWidth),
                                painter = painterResource(EffectResourceMap[ctl_type].icon),
                                contentDescription = stringResource(EffectResourceMap[ctl_type].name)
                            )
                            Text(
                                EffectResourceMap[ctl_type].name,
                                Modifier.weight(1F)
                            )
                        }
                    )
                }
                options
            },
            long_click_callback = { ctl_type: EffectType ->
                selected_ctl_type.value = ctl_type
                multi_dialog_visibility.value = true
            },
            callback = { ctl_type ->
                opus_manager.set_channel_controller_visibility( ctl_type, channel, true)
            }
        )
    }

    if (multi_dialog_visibility.value) {
        val ctl_type = selected_ctl_type.value!!
        PaganDialog(multi_dialog_visibility) {
            Icon(
                modifier = Modifier.height(Dimensions.EffectDialogIconHeight),
                painter = painterResource(EffectResourceMap[ctl_type].icon),
                contentDescription = stringResource(EffectResourceMap[ctl_type].name)
            )
            LargeSpacer()
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    multi_dialog_visibility.value = false
                    menu_visibility.value = false
                    opus_manager.set_channel_controller_visibility(ctl_type, channel, true)
                },
                content = { Text(R.string.show_channel_controls_single) },
            )
            LargeSpacer()
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    multi_dialog_visibility.value = false
                    menu_visibility.value = false
                    opus_manager.set_all_channel_controller_visibility(ctl_type)
                },
                content = { Text(R.string.show_channel_controls_all) },
            )
            LargeSpacer()
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    multi_dialog_visibility.value = false
                },
                content = { Text(android.R.string.cancel) },
            )
        }
    }
}

@Composable
fun AdjustChannelButton(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier) {
    val menu_visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = modifier.testTag(TestTag.AdjustSelection),
        onClick = { menu_visibility.value = true },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )

    if (menu_visibility.value) {
        AdjustSelectionDialog(menu_visibility, vm_state.radix.value) { i ->
            opus_manager.offset_selection(i)
        }
    }
}

@Composable
fun DuplicateChannelButton(channel: Int, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier) {
    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelDuplicate),
        onClick = { opus_manager.duplicate_channel(channel) },
        icon = R.drawable.icon_ic_baseline_content_copy_24,
        description = R.string.duplicate_channel
    )
}

@Composable
fun RemoveChannelButton(channel: Int, opus_manager: OpusLayerInterface, is_percussion: Boolean, modifier: Modifier = Modifier) {
    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelRemove),
        onClick = { opus_manager.remove_channel(channel) },
        icon = if (is_percussion) {
            R.drawable.icon_subtract_bang
        } else {
            R.drawable.icon_subtract_circle
        },
        description = R.string.cd_remove_channel
    )
}

@Composable
fun AddKitButton(channel: Int, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier) {
    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelPercussionInsert),
        onClick = {
            opus_manager.new_channel(channel + 1, is_percussion = true)
        },
        icon = R.drawable.icon_add_bang,
        description = R.string.cd_insert_channel_percussion
    )
}

@Composable
fun AddChannelButton(channel: Int, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, shape: Shape = Shapes.ContextMenuButtonPrimaryStart) {
    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelInsert),
        onClick = {
            opus_manager.new_channel(channel + 1, is_percussion = false)
        },
        icon = R.drawable.icon_add_circle,
        shape = shape,
        description = R.string.cd_insert_channel
    )
}

@Composable
fun MuteChannelButton(
    channel: Int,
    opus_manager: OpusLayerInterface,
    active_channel: ViewModelEditorState.ChannelData,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelMute),
        onClick = {
            if (active_channel.is_mute.value) {
                opus_manager.unmute_channel(channel)
            } else {
                opus_manager.mute_channel(channel)
            }
        },
        shape = shape,
        icon = if (!active_channel.is_mute.value) R.drawable.icon_unmute
        else R.drawable.icon_mute,
        description = R.string.cd_line_mute
    )
}

@Composable
fun SetPresetButton(
    modifier: Modifier = Modifier,
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface,
    channel_index: Int,
    active_channel: ViewModelEditorState.ChannelData,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    if (vm_state.soundfont_ready.value) {
        TextCMenuButton(
            modifier = modifier.testTag(TestTag.ChannelPreset),
            shape = shape,
            onClick = {
                opus_manager.set_channel_preset(channel_index)
            },
            text = if (vm_state.use_midi_playback.value || active_channel.active_name.value == null) {
                if (active_channel.instrument.value.bank == 128) {
                    if (vm_state.soundfont_active.value != null && !vm_state.use_midi_playback.value) {
                        stringResource(R.string.unavailable_kit)
                    } else {
                        stringResource(R.string.gm_kit)
                    }
                } else if (vm_state.soundfont_active.value != null && !vm_state.use_midi_playback.value) {
                    stringResource(
                        R.string.unavailable_preset,
                        stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.program]
                    )
                } else {
                    stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.program]
                }
            } else {
                active_channel.active_name.value!!
            }
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            SoundfontLoadingIndicator()
        }
    }
}

@Composable
fun SetChannelColorButton(
    modifier: Modifier = Modifier,
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface,
    channel_index: Int,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    val visibility = remember { mutableStateOf(false) }

    IconCMenuButton(
        modifier = modifier.testTag(TestTag.ChannelColor),
        onClick = { visibility.value = true },
        shape = shape,
        icon = R.drawable.icon_palette,
        description = R.string.cd_line_mute
    )

    if (visibility.value) {
        val default_color = opus_manager.get_channel(channel_index).palette.event
            ?: Colors.LEAF_COLOR
        ColorPickerDialog(default_color, visibility) { new_color ->
            opus_manager.set_channel_event_color(channel_index, new_color)
        }
    }
}

@Composable
fun ContextMenuChannelPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    if (cursor.type != CursorMode.Channel) return
    val channel_index = cursor.ints[0]
    val active_channel = vm_state.channel_data[channel_index]
    val is_percussion = active_channel.percussion.value
    when (layout) {
        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargePortrait,
        LayoutSize.XLargePortrait,
        LayoutSize.LargeLandscape,
        LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                ShowEffectsButton(
                    channel_index,
                    opus_manager,
                    Modifier,
                    Shapes.ContextMenuButtonPrimaryStart
                )
                Spacer(
                    Modifier
                        .width(Dimensions.ContextMenuPadding)
                        .weight(1F)
                )
                AdjustChannelButton(vm_state, opus_manager)
                ContextMenuSpacer()
                DuplicateChannelButton(channel_index, opus_manager)
                ContextMenuSpacer()
                RemoveChannelButton(channel_index, opus_manager, is_percussion)
                ContextMenuSpacer()
                AddKitButton(channel_index, opus_manager)
                ContextMenuSpacer()
                AddChannelButton(
                    channel_index,
                    opus_manager,
                    Modifier,
                    Shapes.ContextMenuButtonPrimaryEnd
                )
            }
        }

        LayoutSize.SmallLandscape,
        LayoutSize.MediumLandscape -> {
            Column(
                modifier = modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AddChannelButton(
                    channel_index,
                    opus_manager,
                    Modifier,
                    Shapes.ContextMenuButtonPrimaryStart
                )
                AddKitButton(channel_index, opus_manager)
                DuplicateChannelButton(channel_index, opus_manager)
                AdjustChannelButton(vm_state, opus_manager)
                RemoveChannelButton(channel_index, opus_manager, is_percussion)
                ShowEffectsButton(
                    channel_index,
                    opus_manager,
                    Modifier.weight(1F, fill = false),
                    Shapes.ContextMenuButtonPrimaryBottom
                )
            }
        }
    }
}

@Composable
fun ContextMenuChannelSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize, modifier: Modifier = Modifier,) {
    val cursor = vm_state.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = try {
        vm_state.channel_data[channel_index]
    } catch (e: Exception) {
        return
    }

    ContextMenuSecondaryRow(modifier) {
        MuteChannelButton(
            channel_index,
            opus_manager,
            active_channel,
            Modifier,
            if (layout == LayoutSize.SmallLandscape || layout == LayoutSize.MediumLandscape) {
                Shapes.ContextMenuButtonPrimaryStart
            } else {
                Shapes.ContextMenuButtonPrimary
            }
        )
        ContextMenuSpacer()
        SetPresetButton(
            modifier = Modifier
                .height(Dimensions.ContextMenuButtonHeight)
                .weight(1f),
            vm_state,
            opus_manager,
            channel_index,
            active_channel,
            Shapes.ContextMenuButtonPrimary
        )
        ContextMenuSpacer()
        SetChannelColorButton(
            Modifier,
            vm_state,
            opus_manager,
            channel_index,
            shape = Shapes.ContextMenuSecondaryButtonEnd
        )

    }
}
