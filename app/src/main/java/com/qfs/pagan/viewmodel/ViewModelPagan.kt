package com.qfs.pagan.viewmodel

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.qfs.pagan.ComponentActivity.PaganComponentActivity.Companion.SIZE_L
import com.qfs.pagan.ComponentActivity.PaganComponentActivity.Companion.SIZE_M
import com.qfs.pagan.ComponentActivity.PaganComponentActivity.Companion.SIZE_XL
import com.qfs.pagan.DialogChain
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.SortableMenu
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.projectmanager.ProjectManager

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

    enum class LayoutSize {
        SmallPortrait,
        MediumPortrait,
        LargePortrait,
        XLargePortrait,
        SmallLandscape,
        MediumLandscape,
        LargeLandscape,
        XLargeLandscape
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
    val soundfont_name: MutableState<String?> = mutableStateOf(this.configuration.soundfont)
    val requires_soundfont: MutableState<Boolean> = mutableStateOf(false)
    val has_saved_project: MutableState<Boolean> = mutableStateOf(false)
    val soundfont_directory = mutableStateOf<Uri?>(null)
    val project_directory = mutableStateOf<Uri?>(null)
    val night_mode = mutableStateOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun set_layout_size(width: Dp, height: Dp) {
        this.active_layout_size.value = if (width >= height) {
            if (width >= SIZE_XL.first && height >= SIZE_XL.second) LayoutSize.XLargeLandscape
            else if (width >= SIZE_L.first && height >= SIZE_L.second) LayoutSize.LargeLandscape
            else if (width >= SIZE_M.first && height >= SIZE_M.second) LayoutSize.MediumLandscape
            else LayoutSize.SmallLandscape
        } else {
            if (width >= SIZE_XL.second && height >= SIZE_XL.first) LayoutSize.LargePortrait
             else if (width >= SIZE_L.second && height >= SIZE_L.first) LayoutSize.LargePortrait
             else if (width >= SIZE_M.second && height >= SIZE_M.first) LayoutSize.MediumPortrait
             else LayoutSize.SmallPortrait
        }
        println("- - - - - - ${this.active_layout_size} - - - - - - - ")
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
        this.configuration = try {
            PaganConfiguration.Companion.from_path(this.configuration_path!!)
        } catch (e: Exception) {
            this.configuration
        }

        // Update MutableStates
        this.soundfont_name.value = this.configuration.soundfont
        this.project_directory.value = this.configuration.project_directory
        this.soundfont_directory.value = this.configuration.soundfont_directory
        this.night_mode.value = this.configuration.night_mode
    }

    fun reload_config() {
        this.load_config(this.configuration_path ?: return)
    }
    fun set_project_manager(project_manager: ProjectManager) {
        this.project_manager = project_manager
        this.has_saved_project.value = this.project_manager?.has_projects_saved() ?: false
    }

    fun create_small_dialog(level: Int = 0, dialog_callback: (() -> Unit) -> (@Composable (ColumnScope.() -> Unit))) {
        this.create_dialog(level, DialogSize.Small, dialog_callback)
    }

    fun create_medium_dialog(level: Int = 0, dialog_callback: (() -> Unit) -> (@Composable (ColumnScope.() -> Unit))) {
        this.create_dialog(level, DialogSize.Medium, dialog_callback)
    }

    fun create_dialog(level: Int = 0, size: DialogSize = DialogSize.Unbounded, dialog_callback: (() -> Unit) -> (@Composable (ColumnScope.() -> Unit))) {
        // Use level to block Dup dialogs. set it to allow for dialogs opened from other dialogs
        if (this.dialog_queue.value?.level == level) return
        this.dialog_queue.value = DialogChain(
            parent = this.dialog_queue.value,
            size = size,
            dialog = dialog_callback {
                this.dialog_queue.value?.let {
                    this.dialog_queue.value = it.parent
                }
            },
            level = level
        )
    }

    fun <T> unsortable_list_dialog(title: Int, options: List<Pair<T, @Composable RowScope.() -> Unit>>, default_value: T? = null, callback: (T) -> Unit) {
        this.create_medium_dialog { close ->
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
        selected_sort: Int = -1,
        default_value: T? = null,
        content: (@Composable RowScope.() -> Unit)? = null,
        onLongClick: (T) -> Unit = {},
        onClick: (T) -> Unit
    ) {
        this.create_medium_dialog { close ->
            @Composable {
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    DialogSTitle(title)
                    content?.let { Row(content = it) }
                    SortableMenu(
                        modifier = Modifier.weight(1F, fill=false),
                        default_menu = default_menu,
                        sort_row_padding = PaddingValues(bottom = dimensionResource(R.dimen.dialog_bar_padding_vertical)),
                        sort_options = sort_options,
                        selected_sort = selected_sort,
                        onLongClick = onLongClick,
                        onClick = {
                            close()
                            onClick(it)
                        }
                    )
                    DialogBar(neutral = close)
                }
            }
        }
    }

    internal fun save_configuration() {
        this.configuration_path?.let {
            this.configuration.save(it)
        }
    }

    internal fun coerce_relative_soundfont_path(soundfont_uri: Uri): String? {
        return ViewModelPagan.coerce_relative_path(soundfont_uri, this.configuration.soundfont_directory)
    }

    fun set_soundfont_uri(uri: Uri) {
        this.configuration.soundfont = this.coerce_relative_soundfont_path(uri)
        this.soundfont_name.value = this.configuration.soundfont
    }
    
}
