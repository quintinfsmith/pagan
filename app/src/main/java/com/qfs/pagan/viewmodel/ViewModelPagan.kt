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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
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
    val requires_soundfont: MutableState<Boolean> = mutableStateOf(false)
    val has_saved_project: MutableState<Boolean> = mutableStateOf(false)
    val has_backup_saved: MutableState<Boolean> = mutableStateOf(false)
    val dialog_states = HashMap<String, MutableState<Boolean>>()
    val project_list = mutableStateOf<List<ProjectManager.CachedProjectData>>(listOf())

    fun get_dialog_state(key: String): MutableState<Boolean> {
        if (!this.dialog_states.contains(key)) {
            this.dialog_states[key] = mutableStateOf(false)
        }
        return this.dialog_states[key]!!
    }

    fun set_layout_size(width: Dp, height: Dp) {
        this.active_layout_size.value = Dimensions.set_active_layout_size(width, height)
    }

    fun get_layout_size(): LayoutSize {
        return this.active_layout_size.value
    }

    fun delete_project(uri: Uri) {
        this.project_manager?.delete(uri)
        this.project_list.value = this.project_manager?.get_project_list() ?: listOf()
        this.has_saved_project.value = this.project_manager?.has_projects_saved() ?: false
        this.has_backup_saved.value = this.project_manager?.has_backup_saved() == true
    }

    fun load_config(path: String) {
        this.configuration_path = path
        try {
            this.configuration.update_from_path(path)
        } catch (e: Exception) {
            // pass
        }
    }

    fun set_config(config: PaganConfiguration) {
        this.configuration.from_other(config)
        this.has_saved_project.value = this.project_manager?.has_projects_saved() ?: false
        this.has_backup_saved.value = this.project_manager?.has_backup_saved() == true
    }

    fun set_project_manager(project_manager: ProjectManager) {
        this.project_manager = project_manager
        this.project_list.value = this.project_manager?.get_project_list() ?: listOf()
        this.has_saved_project.value = this.project_manager?.has_projects_saved() ?: false
        this.has_backup_saved.value = this.project_manager?.has_backup_saved() == true
    }

    internal fun save_configuration() {
        this.configuration_path?.let {
            this.configuration.save(it)
        }
    }

    internal fun coerce_relative_soundfont_path(soundfont_uri: Uri): String? {
        return ViewModelPagan.coerce_relative_path(soundfont_uri, this.configuration.soundfont_directory.value)
    }

    fun remove_soundfont(index: Int) {
        this.configuration.soundfonts.value = this.configuration.soundfonts.value.sliceArray(0 until index) + this.configuration.soundfonts.value.sliceArray(index + 1 until this.configuration.soundfonts.value.size)
    }
    fun add_soundfont_uri(uri: Uri) {
        this.coerce_relative_soundfont_path(uri)?.let { path ->
            this.configuration.soundfonts.value += arrayOf(mutableStateOf(path))
        }
    }
    fun set_soundfont_uri(uri: Uri, index: Int) {
        this.coerce_relative_soundfont_path(uri)?.let { path ->
            this.configuration.soundfonts.value[index].value = path
        }
    }


    fun scan_and_update_project_list(full_refresh: Boolean = false) {
        this.project_manager?.scan_and_update_project_list(full_refresh)
        this.project_list.value = this.project_manager?.get_project_list() ?: listOf()
    }

    fun change_project_path(new_uri: Uri, active_project_uri: Uri? = null) {
        this.project_manager?.change_project_path(new_uri, active_project_uri)
        this.scan_and_update_project_list(true)
    }

    fun save(opus_manager: OpusLayerInterface, active_project: Uri?): Uri? {
        val project_manager = this.project_manager ?: return null

        val uri = project_manager.save(opus_manager, active_project)
        this.has_saved_project.value = true
        this.project_list.value = project_manager.get_project_list()

        return uri
    }
}
