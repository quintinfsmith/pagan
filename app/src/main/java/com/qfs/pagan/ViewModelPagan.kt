package com.qfs.pagan

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.pagan.projectmanager.ProjectManager
import kotlin.properties.Delegates.observable

class ViewModelPagan: ViewModel() {
    var dialog_queue = mutableStateOf(DialogQueue())
    var project_manager: ProjectManager? = null

    var _title_callbacks = mutableListOf<(String?) -> Unit>()
    var title: String? by observable(null) { _, _, new_value ->
        this._title_callbacks.forEach { it(new_value) }
    }

    var configuration = PaganConfiguration()
    var configuration_path: String? = null

    internal fun save_configuration() {
        this.configuration_path?.let {
            this.configuration.save(it)
        }
    }
}