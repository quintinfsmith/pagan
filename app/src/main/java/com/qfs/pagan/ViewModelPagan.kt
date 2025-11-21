package com.qfs.pagan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.pagan.projectmanager.ProjectManager
import kotlin.properties.Delegates.observable

class ViewModelPagan: ViewModel() {
    var dialog_queue: MutableState<DialogChain?> = mutableStateOf(null)
    var project_manager: ProjectManager? = null

    var configuration = PaganConfiguration()
    var configuration_path: String? = null

    fun create_dialog(dialog_callback: (() -> Unit) -> (@Composable (() -> Unit))) {
        this.dialog_queue.value = DialogChain(
            parent = this.dialog_queue.value,
            dialog = dialog_callback {
                this.dialog_queue.value?.let {
                    this.dialog_queue.value = it.parent
                }
            }
        )
    }

    internal fun save_configuration() {
        this.configuration_path?.let {
            this.configuration.save(it)
        }
    }
}