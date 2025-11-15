package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.qfs.pagan.OpusLayerInterface as OpusManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ContextMenuLinePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
@Composable
fun ContextMenuLineSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}

@Composable
fun ContextMenuColumnPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
@Composable
fun ContextMenuColumnSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}

@Composable
fun ContextMenuSinglePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val active_event = ui_facade.active_event.value
    Column() {
        Row() {
            Button(onClick = {
                dispatcher.split(2)
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_split),
                    contentDescription = stringResource(R.string.btn_split)
                )
            }
            Button(onClick = {
                dispatcher.insert_leaf(1)
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_insert),
                    contentDescription = stringResource(R.string.btn_insert)
                )
            }
            Button(onClick = {
                dispatcher.remove_at_cursor()
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_remove),
                    contentDescription = stringResource(R.string.btn_remove)
                )
            }
            Button(onClick = {
                dispatcher.set_duration()
            }) {
                Text("x${active_event?.duration ?: 1}")
            }
            Button(onClick = {
                dispatcher.unset()
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_unset),
                    contentDescription = stringResource(R.string.btn_unset)
                )
            }
        }
    }
    Text("SINGLE PRIMARY")
}
@Composable
fun ContextMenuSingleSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    Text("SINGLE SECONDARY")
}

@Composable
fun ContextMenuRangePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
@Composable
fun ContextMenuRangeSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}

@Composable
fun ContextMenuChannelPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
@Composable
fun ContextMenuChannelSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
