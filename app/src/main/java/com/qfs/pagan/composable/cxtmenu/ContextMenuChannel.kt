package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ToggleEffectsButton(dispatcher: ActionTracker, shape: Shape = Shapes.ContextMenuButtonPrimaryStart) {
    IconCMenuButton(
        onClick = { dispatcher.show_hidden_channel_controller() },
        icon = R.drawable.icon_ctl,
        shape = shape,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun AdjustChannelButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun RemoveChannelButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.remove_channel() },
        icon = R.drawable.icon_subtract_circle,
        description = R.string.cd_remove_channel
    )

}

@Composable
fun AddKitButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_percussion_channel() },
        icon = R.drawable.icon_add_bang,
        description = R.string.cd_insert_channel_percussion
    )
}

@Composable
fun AddChannelButton(dispatcher: ActionTracker, shape: Shape = Shapes.ContextMenuButtonPrimaryStart) {
    IconCMenuButton(
        onClick = { dispatcher.insert_channel() },
        icon = R.drawable.icon_add_circle,
        shape = shape,
        description = R.string.cd_insert_channel
    )
}

@Composable
fun MuteChannelButton(
    dispatcher: ActionTracker,
    active_channel: ViewModelEditorState.ChannelData,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        onClick = {
            if (active_channel.is_mute.value) {
                dispatcher.channel_unmute()
            } else {
                dispatcher.channel_mute()
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
    ui_facade: ViewModelEditorState,
    dispatcher: ActionTracker,
    channel_index: Int,
    active_channel: ViewModelEditorState.ChannelData,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    TextCMenuButton(
        modifier = modifier,
        shape = shape,
        onClick = { dispatcher.set_channel_preset(channel_index) },
        text = active_channel.active_name.value ?: if (active_channel.instrument.value.first == 128) {
            if (ui_facade.soundfont_active.value) {
                stringResource(R.string.unavailable_kit)
            } else {
                stringResource(R.string.gm_kit)
            }
        } else if (ui_facade.soundfont_active.value) {
            stringResource(R.string.unavailable_preset, stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.second])
        } else {
            stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.second]
        }
    )
}

@Composable
fun ContextMenuChannelPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
    when (layout) {
        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargePortrait,
        LayoutSize.XLargePortrait,
        LayoutSize.LargeLandscape,
        LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                ToggleEffectsButton(dispatcher, Shapes.ContextMenuButtonPrimaryStart)
                Spacer(
                    Modifier
                        .width(Dimensions.ContextMenuPadding)
                        .weight(1F)
                )
                AdjustChannelButton(dispatcher)
                CMPadding()
                RemoveChannelButton(dispatcher)
                CMPadding()
                AddKitButton(dispatcher)
                CMPadding()
                AddChannelButton(dispatcher, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }

        LayoutSize.SmallLandscape,
        LayoutSize.MediumLandscape -> {
            Column {
                AddChannelButton(dispatcher, Shapes.ContextMenuButtonPrimaryStart)
                CMPadding()
                AddKitButton(dispatcher)
                CMPadding()
                AdjustChannelButton(dispatcher)
                CMPadding()
                RemoveChannelButton(dispatcher)
                Spacer(Modifier.weight(1F))

                ToggleEffectsButton(dispatcher, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
    }
}

@Composable
fun ContextMenuChannelSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize, modifier: Modifier = Modifier,) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = try {
        ui_facade.channel_data[channel_index]
    } catch (e: Exception) {
        return
    }

    ContextMenuSecondaryRow(modifier) {
        MuteChannelButton(
            dispatcher,
            active_channel,
            if (layout == LayoutSize.SmallLandscape || layout == LayoutSize.MediumLandscape) {
                Shapes.ContextMenuButtonPrimaryStart
            } else {
                Shapes.ContextMenuButtonPrimary
            }
        )
        CMPadding()
        SetPresetButton(
            modifier = Modifier
                .height(Dimensions.ContextMenuButtonHeight)
                .weight(1f),
            ui_facade,
            dispatcher,
            channel_index,
            active_channel,
            if (layout == LayoutSize.SmallLandscape || layout == LayoutSize.MediumLandscape) {
                Shapes.ContextMenuButtonPrimaryEnd
            } else {
                Shapes.ContextMenuButtonPrimary
            }
        )
    }
}
