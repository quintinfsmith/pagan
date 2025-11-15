package com.qfs.pagan.ComponentActivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.cxtmenu.ContextMenuChannelPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuChannelSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuColumnPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuColumnSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLinePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLineSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuRangePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuRangeSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuSinglePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuSingleSecondary
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.uibill.UIFacade
import com.qfs.pagan.viewmodel.ViewModelEditor

class ComponentActivityEditor: PaganComponentActivity() {
    val model_editor: ViewModelEditor by this.viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        this.model_editor.opus_manager.project_change_new()
        super.onCreate(savedInstanceState)
    }

    @Composable
    fun ContextMenuPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
        val cursor = ui_facade.active_cursor.value
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLinePrimary(ui_facade, dispatcher)
            CursorMode.Column -> ContextMenuColumnPrimary(ui_facade, dispatcher)
            CursorMode.Single -> ContextMenuSinglePrimary(ui_facade, dispatcher)
            CursorMode.Range -> ContextMenuRangePrimary(ui_facade, dispatcher)
            CursorMode.Channel -> ContextMenuChannelPrimary(ui_facade, dispatcher)
            CursorMode.Unset,
            null -> Text("TODO")
        }
    }
    @Composable
    fun ContextMenuSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
        val cursor = ui_facade.active_cursor.value
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLineSecondary(ui_facade, dispatcher)
            CursorMode.Column -> ContextMenuColumnSecondary(ui_facade, dispatcher)
            CursorMode.Single -> ContextMenuSingleSecondary(ui_facade, dispatcher)
            CursorMode.Range -> ContextMenuRangeSecondary(ui_facade, dispatcher)
            CursorMode.Channel -> ContextMenuChannelSecondary(ui_facade, dispatcher)
            CursorMode.Unset,
            null -> Text("TODO")
        }
    }

    @Composable
    fun MainTable(ui_facade: UIFacade, length: MutableState<Int>) {
        val line_height = dimensionResource(R.dimen.line_height)
        val ctl_line_height = dimensionResource(R.dimen.ctl_line_height)
        val leaf_width = dimensionResource(R.dimen.base_leaf_width)
        val column_widths = Array(ui_facade.beat_count.value) { i ->
            Array(ui_facade.cell_map.size) { j -> ui_facade.cell_map[j][i].value.weighted_size }.max()
        }
        Column() {
            for ((y, line) in ui_facade.cell_map.enumerate()) {
                val use_height = if (ui_facade.line_data[y].ctl_type != null) {
                    ctl_line_height
                } else {
                    line_height
                }
                Row(Modifier.height(use_height)) {
                    for (x in 0 until length.value) {
                        Column(Modifier.width(leaf_width * column_widths[x])) {
                            CellView(ui_facade, y, x)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun <T: OpusEvent> LeafView(ui_facade: UIFacade, y: Int, x: Int, path: List<Int>, event: T?, modifier: Modifier = Modifier) {
        val cursor = ui_facade.active_cursor.value
        val leaf_selected = cursor != null && ui_facade.is_leaf_selected(cursor, y, x, path)
        val background_color = if (leaf_selected) {
            when (event) {
                is InstrumentEvent -> colorResource(R.color.leaf_selected)
                is EffectEvent -> colorResource(R.color.ctl_leaf_selected)
                else -> colorResource(R.color.leaf_empty_selected)
            }
        } else {
            when (event) {
                is InstrumentEvent -> colorResource(R.color.leaf_main)
                is EffectEvent -> colorResource(R.color.ctl_leaf)
                else -> Color.Transparent
            }
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .dropShadow(
                    shape = RoundedCornerShape(8.dp),
                    shadow = Shadow(
                        radius = 4.dp,
                        spread = 6.dp,
                        color = Color(0x40000000),
                        offset = DpOffset(x = 4.dp, 4.dp)
                    )
                )
                .padding(1.dp)
                .background(color = background_color)
                .fillMaxSize()
        ) {
            when (event) {
                is AbsoluteNoteEvent -> {
                    val octave = event.note / ui_facade.radix.value
                    val offset = event.note % ui_facade.radix.value
                    Text(
                        AnnotatedString.fromHtml("<sub>$octave</sub>$offset")
                    )
                }
                is RelativeNoteEvent -> {}
                is PercussionEvent -> Text(stringResource(R.string.percussion_label))
                is OpusVolumeEvent -> {
                    Text("${event.value}")
                }
                is OpusPanEvent -> {}
                is DelayEvent -> {}
                is OpusTempoEvent -> {}
                is OpusVelocityEvent -> {}
                null -> {}
            }
        }
    }

    @Composable
    fun CellView(ui_facade: UIFacade, y: Int, x: Int, modifier: Modifier = Modifier) {
        val dispatcher = this.model_editor.action_interface
        val cell = ui_facade.cell_map[y][x].value
        val line_info = ui_facade.line_data[y]
        Row(modifier.fillMaxSize()) {
            composable_traverse(cell, listOf()) { tree, path, event ->
                if (tree.is_leaf()) {
                    this@ComponentActivityEditor.LeafView(
                        ui_facade,y, x, path, event,
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    if (line_info.ctl_type == null) {
                                        dispatcher.cursor_select(BeatKey(line_info.channel!!, line_info.line_offset!!, x), path)
                                    } else if (line_info.line_offset != null) {
                                        dispatcher.cursor_select_ctl_at_line(line_info.ctl_type!!, BeatKey(line_info.channel!!, line_info.line_offset!!, x), path)
                                    } else if (line_info.channel != null) {
                                        dispatcher.cursor_select_ctl_at_channel(line_info.ctl_type!!, line_info.channel!!, x, path)
                                    } else {
                                        dispatcher.cursor_select_ctl_at_global(line_info.ctl_type!!, x, path)
                                    }
                                },
                                onLongClick = {
                                    if (line_info.ctl_type == null) {
                                        dispatcher.cursor_select_range_next(BeatKey(line_info.channel!!, line_info.line_offset!!, x))
                                    } else if (line_info.line_offset != null) {
                                        dispatcher.cursor_select_line_ctl_range_next(line_info.ctl_type!!, BeatKey(line_info.channel!!, line_info.line_offset!!, x))
                                    } else if (line_info.channel != null) {
                                        dispatcher.cursor_select_channel_ctl_range_next(line_info.ctl_type!!, line_info.channel!!, x)
                                    } else {
                                        dispatcher.cursor_select_global_ctl_range_next(line_info.ctl_type!!, x)
                                    }
                                }
                            )
                            .weight(tree.weighted_size.toFloat())
                    )
                }
            }
        }
    }

    @Composable
    fun <T> composable_traverse(tree: ReducibleTree<T>, path: List<Int>, callback: @Composable (ReducibleTree<T>, List<Int>, T?) -> Unit) {
        if (! tree.is_leaf()) {
            for ((i, child) in tree.divisions) {
                this.composable_traverse(child, path + listOf(i), callback)
            }
        }
        callback(tree, path, tree.event)
    }

    @Composable
    override fun LayoutXLargePortrait() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutLargePortrait() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutMediumPortrait() {
        val view_model = this.model_editor
        val ui_facade = model_editor.opus_manager.ui_facade
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.fillMaxSize()) {
                MainTable(ui_facade, ui_facade.beat_count)
            }
            if (ui_facade.active_cursor.value?.type != CursorMode.Unset) {
                Box(Modifier) {
                    Column {
                        Row { ContextMenuPrimary(ui_facade, view_model.action_interface) }
                        Row { ContextMenuSecondary(ui_facade, view_model.action_interface) }
                    }
                }
            }
        }
    }

    @Composable
    override fun LayoutSmallPortrait() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutXLargeLandscape() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutLargeLandscape() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutMediumLandscape() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutSmallLandscape() {
        TODO("Not yet implemented")
    }
}