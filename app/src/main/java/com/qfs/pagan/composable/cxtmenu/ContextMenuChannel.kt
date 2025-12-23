package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.viewmodel.ViewModelEditorState
import com.qfs.pagan.viewmodel.ViewModelPagan

@Composable
fun ToggleEffectsButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.show_hidden_channel_controller() },
        icon = R.drawable.icon_ctl,
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
        icon = R.drawable.icon_remove_channel,
        description = R.string.cd_remove_channel
    )


}

@Composable
fun AddKitButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_percussion_channel() },
        icon = R.drawable.icon_add_channel_kit,
        description = R.string.cd_insert_channel_percussion
    )
}

@Composable
fun AddChannelButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_channel() },
        icon = R.drawable.icon_add_channel,
        description = R.string.cd_insert_channel
    )
}

@Composable
fun MuteChannelButton(dispatcher: ActionTracker, active_channel: ViewModelEditorState.ChannelData) {
    IconCMenuButton(
        onClick = {
            if (active_channel.is_mute.value) {
                dispatcher.channel_unmute()
            } else {
                dispatcher.channel_mute()
            }
        },
        icon = if (!active_channel.is_mute.value) R.drawable.icon_unmute
        else R.drawable.icon_mute,
        description = R.string.cd_line_mute
    )
}

@Composable
fun SetPresetButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, channel_index: Int, active_channel: ViewModelEditorState.ChannelData) {
    TextCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.set_channel_preset(channel_index) },
        text = active_channel.active_name.value ?: stringResource(R.string.unavailable_preset, stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.second])
    )
}

@Composable
fun ContextMenuChannelPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: ViewModelPagan.LayoutSize) {
    when (layout) {
        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargePortrait,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                ToggleEffectsButton(dispatcher)
                Spacer(
                    Modifier
                        .width(dimensionResource(R.dimen.contextmenu_padding))
                        .weight(1F)
                )
                AdjustChannelButton(dispatcher)
                CMPadding()
                RemoveChannelButton(dispatcher)
                CMPadding()
                AddKitButton(dispatcher)
                CMPadding()
                AddChannelButton(dispatcher)
            }
        }

        ViewModelPagan.LayoutSize.SmallLandscape,
        ViewModelPagan.LayoutSize.MediumLandscape -> {
            Column(Modifier.width(dimensionResource(R.dimen.contextmenu_button_width))) {
                AddChannelButton(dispatcher)
                CMPadding()
                AddKitButton(dispatcher)
                CMPadding()
                AdjustChannelButton(dispatcher)
                CMPadding()
                RemoveChannelButton(dispatcher)
                Spacer(Modifier.weight(1F))

                ToggleEffectsButton(dispatcher)
            }
        }
    }
}

@Composable
fun ContextMenuChannelSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = try {
        ui_facade.channel_data[channel_index]
    } catch (e: Exception) {
        return
    }

    ContextMenuSecondaryRow(modifier) {
        MuteChannelButton(dispatcher, active_channel)
        CMPadding()
        SetPresetButton(
            modifier = Modifier
                .height(dimensionResource(R.dimen.contextmenu_button_height))
                .weight(1f),
            dispatcher,
            channel_index,
            active_channel
        )
    }
}
