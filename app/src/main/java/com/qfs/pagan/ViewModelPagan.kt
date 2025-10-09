package com.qfs.pagan

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.qfs.pagan.PaganConfiguration.MoveMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates

class ViewModelPagan: ViewModel() {
    var _title_callbacks = mutableListOf<(String?) -> Unit>()
    var title: String? by Delegates.observable(null) { _, _, new_value ->
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