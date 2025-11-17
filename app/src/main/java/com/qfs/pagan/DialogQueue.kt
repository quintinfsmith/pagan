package com.qfs.pagan

import androidx.compose.runtime.Composable

class DialogQueue {
    class Entry(val key: Int, var dialog: @Composable (() -> Unit))
    var key_gen = 0
    val dialogs = mutableListOf<Entry>()

    fun new_dialog(callback: (DialogQueue, Int) -> (@Composable () -> Unit)) {
        val new_key = this.key_gen++
        this.dialogs.add(Entry(new_key, callback(this, new_key)))
    }

    fun remove(key: Int) {
        var i = 0
        while (i < this.dialogs.size) {
            if (this.dialogs[i].key == key) {
                this.dialogs.removeAt(i)
                break
            }
            i++
        }
    }
}