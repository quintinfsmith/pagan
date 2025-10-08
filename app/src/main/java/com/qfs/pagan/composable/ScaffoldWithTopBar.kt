package com.qfs.pagan.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWithTopBar(title: String, content: @Composable (PaddingValues) -> Unit) {
    PaganTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = title)
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            //Icon(Icons.Filled.ArrowBack, "backIcon")
                        }
                    },
                    //backgroundColor = MaterialTheme.colors.primary,
                    //contentColor = Color.White,
                    //elevation = 10.dp
                )
            },
            content = content
        )
    }
}
