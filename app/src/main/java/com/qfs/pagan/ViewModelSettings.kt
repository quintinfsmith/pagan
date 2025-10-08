package com.qfs.pagan

import androidx.lifecycle.ViewModel

class ViewModelSettings: ViewModel() {
    var configuration = PaganConfiguration()
    var configuration_path: String? = null

    internal fun save_configuration() {
        this.configuration_path?.let {
            this.configuration.save(it)
        }
    }
}