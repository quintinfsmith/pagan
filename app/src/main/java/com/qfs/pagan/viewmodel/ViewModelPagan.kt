/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.viewmodel

import android.net.Uri
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.qfs.pagan.DialogChain
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.SortableMenu
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.projectmanager.ProjectManager
import com.qfs.pagan.ui.theme.Dimensions

class ViewModelPagan: ViewModel() {
    companion object {
        fun coerce_relative_path(descendant: Uri, ancestor: Uri?): String? {
            if (ancestor == null) return null

            val parent_segments = ancestor.pathSegments
            val child_segments = descendant.pathSegments

            if (parent_segments.size >= child_segments.size || child_segments.subList(
                    0,
                    parent_segments.size
                ) != parent_segments
            ) return null

            val split_child_path = child_segments.last().split("/")
            val split_parent_path = parent_segments.last().split("/")
            val relative_path = split_child_path.subList(split_parent_path.size, split_child_path.size)

            return relative_path.joinToString("/")
        }
    }

    enum class DialogSize {
        Unbounded,
        Small, // Single purpose. Eg, number input dialogs
        Medium,
    }

    val active_layout_size: MutableState<LayoutSize> = mutableStateOf(LayoutSize.SmallPortrait)
    var project_manager: ProjectManager? = null
    var configuration_path: String? = null
    var configuration = PaganConfiguration()

    // MutableStates
    var dialog_queue: MutableState<DialogChain?> = mutableStateOf(null)
    val requires_soundfont: MutableState<Boolean> = mutableStateOf(false)
    val has_saved_project: MutableState<Boolean> = mutableStateOf(false)

    fun set_layout_size(width: Dp, height: Dp) {
        this.active_layout_size.value = Dimensions.set_active_layout_dimensions(width, height)
    }

    fun get_layout_size(): LayoutSize {
        return this.active_layout_size.value
    }

    fun delete_project(uri: Uri) {
        this.project_manager?.delete(uri)
        this.has_saved_project.value = this.project_manager?.has_projects_saved() ?: false
    }

    fun load_config(path: String) {
        this.configuration_path = path
        try {
            this.configuration.update_from_path(path)
        } catch (e: Exception) {
            // pass
        }
    }

    fun reload_config() {
        this.load_config(this.configuration_path ?: return)
    }
    fun set_project_manager(project_manager: ProjectManager) {
        this.project_manager = project_manager
        this.has_saved_project.value = this.project_manager?.has_projects_saved() ?: false
    }

    fun create_dialog(level: Int = 0, alignment: Alignment = Alignment.Center, dialog_callback: (() -> Unit) -> (@Composable (ColumnScope.() -> Unit))) {
        // Use level to block Dup dialogs. set it to allow for dialogs opened from other dialogs
        if (this.dialog_queue.value?.level == level) return
        this.dialog_queue.value = DialogChain(
            parent = this.dialog_queue.value,
            alignment = alignment,
            dialog = dialog_callback {
                this.dialog_queue.value?.let {
                    this.dialog_queue.value = it.parent
                }
            },
            level = level
        )
    }

    fun <T> unsortable_list_dialog(title: Int, options: List<Pair<T, @Composable RowScope.() -> Unit>>, default_value: T? = null, callback: (T) -> Unit) {
        this.create_dialog { close ->
            @Composable {
                DialogSTitle(title)
                UnSortableMenu(Modifier.weight(1F, fill=false), options, default_value) {
                    close()
                    callback(it)
                }
                DialogBar(neutral = close)
            }
        }
    }

    fun <T> sortable_list_dialog(
        title: Int,
        default_menu: List<Pair<T, @Composable RowScope.() -> Unit>>,
        sort_options: List<Pair<Int, (Int, Int) -> Int>>,
        selected_sort: MutableIntState = mutableIntStateOf(-1),
        default_value: T? = null,
        content: (@Composable RowScope.() -> Unit)? = null,
        other: (@Composable (() -> Unit, Int) -> Unit)? = null,
        onLongClick: (T, (() -> Unit)) -> Unit = {_, _ -> },
        onClick: (T) -> Unit
    ) {
        this.create_dialog { close ->
            @Composable {
                SortableMenu(
                    modifier = Modifier
                        .weight(1F, fill = false)
                        .fillMaxWidth(),
                    title_content = {
                        DialogSTitle(title)
                        content?.let { Row(content = it) }
                    },
                    default_menu = default_menu,
                    sort_row_padding = PaddingValues(
                        bottom = Dimensions.DialogBarPaddingVertical,
                    ),
                    sort_options = sort_options,
                    active_sort_option = selected_sort,
                    onLongClick = {
                        onLongClick(it, close)
                    },
                    other = {
                        other?.let { it(close, selected_sort.value) }
                    },
                    default_value = default_value,
                    onClick = {
                        close()
                        onClick(it)
                    }
                )
                DialogBar(neutral = close)
            }
        }
    }

    internal fun save_configuration() {
        this.configuration_path?.let {
            this.configuration.save(it)
        }
    }

    internal fun coerce_relative_soundfont_path(soundfont_uri: Uri): String? {
        return ViewModelPagan.coerce_relative_path(soundfont_uri, this.configuration.soundfont_directory.value)
    }

    fun set_soundfont_uri(uri: Uri) {
        this.configuration.soundfont.value = this.coerce_relative_soundfont_path(uri)
    }
}
