package com.qfs.pagan

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

class DialogChain(var parent: DialogChain? = null, val dialog: @Composable (ColumnScope.() -> Unit), val level: Int = 0)