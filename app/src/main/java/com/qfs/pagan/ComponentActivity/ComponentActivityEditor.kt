package com.qfs.pagan.ComponentActivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.uibill.UIChangeBill
import com.qfs.pagan.viewmodel.ViewModelEditor

class ComponentActivityEditor: PaganComponentActivity() {
    val model_editor: ViewModelEditor by this.viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    @Composable
    fun ContextMenuPrimary(ui_facade: UIChangeBill) {
        val cursor = ui_facade.active_cursor
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLinePrimary(ui_facade)
            CursorMode.Column -> ContextMenuColumnPrimary(ui_facade)
            CursorMode.Single -> ContextMenuSinglePrimary(ui_facade)
            CursorMode.Range -> ContextMenuRangePrimary(ui_facade)
            CursorMode.Channel -> ContextMenuChannelPrimary(ui_facade)
            CursorMode.Unset,
            null -> TODO()
        }
    }
    @Composable
    fun ContextMenuSecondary(ui_facade: UIChangeBill) {
        val cursor = ui_facade.active_cursor
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLineSecondary(ui_facade)
            CursorMode.Column -> ContextMenuColumnSecondary(ui_facade)
            CursorMode.Single -> ContextMenuSingleSecondary(ui_facade)
            CursorMode.Range -> ContextMenuRangeSecondary(ui_facade)
            CursorMode.Channel -> ContextMenuChannelSecondary(ui_facade)
            CursorMode.Unset,
            null -> TODO()
        }
    }

    @Composable
    fun MainTable(ui_facade: UIChangeBill) {
        Column() {
            ui_facade.cell_map.forEach { line ->
                Row() {
                    line.forEach { cell ->
                        Column() {
                            CellView(cell)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CellView(cell: ReducibleTree<out OpusEvent>) {
        Row {
            composable_traverse(cell) { tree, event ->
                if (tree.is_leaf()) {
                    Text("C")
                }
            }
        }
    }

    @Composable
    fun <T> composable_traverse(tree: ReducibleTree<T>, callback: @Composable (ReducibleTree<T>, T?) -> Unit) {
        if (! tree.is_leaf()) {
            for ((_, child) in tree.divisions) {
                composable_traverse(child, callback)
            }
        }
        callback(tree, tree.event)
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
                MainTable(ui_facade)
            }
            if (ui_facade.active_cursor != null && ui_facade.active_cursor!!.type != CursorMode.Unset) {
                Box(Modifier) {
                    Column {
                        Row { ContextMenuPrimary(ui_facade) }
                        Row { ContextMenuSecondary(ui_facade) }
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