package com.qfs.pagan.viewmodel

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.pagan.DialogChain
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.projectmanager.ProjectManager

class ViewModelPagan: ViewModel() {
    companion object {
        fun coerce_relative_path(descendant: Uri, ancestor: Uri?): String? {
            if (ancestor == null) return null

            val parent_segments = ancestor.pathSegments
            val child_segments = descendant.pathSegments

            if (parent_segments.size >= child_segments.size || child_segments.subList(0, parent_segments.size) != parent_segments) return null

            val split_child_path = child_segments.last().split("/")
            val split_parent_path = parent_segments.last().split("/")
            val relative_path = split_child_path.subList(split_parent_path.size, split_child_path.size)

            return relative_path.joinToString("/")
        }
    }
    var dialog_queue: MutableState<DialogChain?> = mutableStateOf(null)
    var project_manager: ProjectManager? = null

    var configuration_path: String? = null
    var configuration = PaganConfiguration()

    val soundfont_name: MutableState<String?> = mutableStateOf(this.configuration.soundfont)
    val requires_soundfont: MutableState<Boolean> = mutableStateOf(false)
    val has_saved_project: MutableState<Boolean> = mutableStateOf(false)

    fun delete_project(uri: Uri) {
        this.project_manager?.delete(project)
        this.has_saved_project = this.project_manager?.has_projects_saved()
    }

    fun load_config(path: String) {
        this.configuration_path = path
        this.configuration = try {
            PaganConfiguration.Companion.from_path(this.configuration_path!!)
        } catch (e: Exception) {
            this.configuration
        }
        this.soundfont_name.value = this.configuration.soundfont
    }

    fun reload_config() {
        this.load_config(this.configuration_path ?: return)
    }
    fun set_project_manager(project_manager: ProjectManager) {
        this.project_manager = project_manager
        this.has_saved_project.value = this.project_manager.has_projects_saved()
    }

    fun create_dialog(level: Int = 0, dialog_callback: (() -> Unit) -> (@Composable (() -> Unit))) {
        // Use level to block Dup dialogs. set it to allow for dialogs opened from other dialogs
        if (this.dialog_queue.value?.level == level) return
        this.dialog_queue.value = DialogChain(
            parent = this.dialog_queue.value,
            dialog = dialog_callback {
                this.dialog_queue.value?.let {
                    this.dialog_queue.value = it.parent
                }
            },
            level = level
        )
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
