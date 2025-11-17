package com.qfs.pagan

import androidx.compose.runtime.Composable

class DialogChain(var parent: DialogChain? = null, val dialog: @Composable (() -> Unit))