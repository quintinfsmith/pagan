package com.qfs.pagan.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


class ActivitySettingsComposer: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Test("Butt")
        }
    }
}

@Composable
fun Test(title: String, modifier: Modifier = Modifier) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TopAppBar { Text(title) } },
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {

            Text("Bottom app bar padding:  $paddingValues")

            repeat(50) {
                Text(it.toString())
            }
        }
    }
}