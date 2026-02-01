package com.qfs.pagan

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.qfs.pagan.viewmodel.ViewModelPagan

class DialogChain(
    var parent: DialogChain? = null,
    val alignment: Alignment = Alignment.Center,
    val dialog: @Composable (ColumnScope.() -> Unit),
    val level: Int = 0
)