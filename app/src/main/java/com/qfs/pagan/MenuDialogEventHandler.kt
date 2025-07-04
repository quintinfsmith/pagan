package com.qfs.pagan

import android.app.AlertDialog

/**
 * Event Handler for the PopupMenuRecycler. Allows control over long clicks as well as clicks.
 */
abstract class MenuDialogEventHandler<T> {
    var dialog: AlertDialog? = null
    fun do_submit(index: Int, value: T) {
        this.dialog?.dismiss()
        this.on_submit(index, value)
    }

    abstract fun on_submit(index: Int, value: T)
    open fun on_long_click_item(index: Int, value: T): Boolean {
        return false
    }
}