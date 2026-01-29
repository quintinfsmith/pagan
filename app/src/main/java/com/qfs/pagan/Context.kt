package com.qfs.pagan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.find_activity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.find_activity()
        else -> null
    }
}

