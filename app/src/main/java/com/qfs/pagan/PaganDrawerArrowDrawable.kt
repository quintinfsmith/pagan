package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable

class PaganDrawerArrowDrawable(var context: Context) : DrawerArrowDrawable(context) {
    init {
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }
        this.color = context.view_model.color_map[ColorMap.Palette.Background]
    }
}