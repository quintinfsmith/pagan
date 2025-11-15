package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.qfs.pagan.OpusLayerInterface as OpusManager
import androidx.compose.runtime.Composable
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ContextMenuLinePrimary(ui_facade: UIFacade, opus_manager: OpusManager) {}
@Composable
fun ContextMenuLineSecondary(ui_facade: UIFacade, opus_manager: OpusManager) {}

@Composable
fun ContextMenuColumnPrimary(ui_facade: UIFacade, opus_manager: OpusManager) {}
@Composable
fun ContextMenuColumnSecondary(ui_facade: UIFacade, opus_manager: OpusManager) {}

@Composable
fun ContextMenuSinglePrimary(ui_facade: UIFacade, opus_manager: OpusManager) {
    val active_event = ui_facade.active_event.value
    Column() {
        Row() {
            Button(onClick = {
                opus_manager.split_tree_at_cursor(2)
            }) { Text("/") }
            Button(onClick = {}) { Text("+") }
            Button(onClick = {}) { Text("-") }
            Button(onClick = {}) { Text("x") }
            Button(onClick = {}) { Text("C")}
        }
    }
    Text("SINGLE PRIMARY")
}
@Composable
fun ContextMenuSingleSecondary(ui_facade: UIFacade, opus_manager: OpusManager) {
    Text("SINGLE SECONDARY")
}

@Composable
fun ContextMenuRangePrimary(ui_facade: UIFacade, opus_manager: OpusManager) {}
@Composable
fun ContextMenuRangeSecondary(ui_facade: UIFacade, opus_manager: OpusManager) {}

@Composable
fun ContextMenuChannelPrimary(ui_facade: UIFacade, opus_manager: OpusManager) {}
@Composable
fun ContextMenuChannelSecondary(ui_facade: UIFacade, opus_manager: OpusManager) {}
