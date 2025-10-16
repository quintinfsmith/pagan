package com.qfs.pagan

import androidx.lifecycle.ViewModel
import kotlin.properties.Delegates.observable

class ViewModelPagan: ViewModel() {
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