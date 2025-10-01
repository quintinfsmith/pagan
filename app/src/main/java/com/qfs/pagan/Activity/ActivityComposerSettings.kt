package com.qfs.pagan.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp


class ActivityComposerSettings: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContent {
            ScaffoldWithTopBar()
        }
    }
}

@Composable
fun Bloop(msg: String) {
    Text(msg)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWithTopBar() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Top App Bar")
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
        }, content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .background(Color(0xff8d6e63)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    content = {
                        Text("Active Soundfont")
                        Button(
                            onClick = { println("Butts") },
                            content = { Text( "Active Soundfont" ) }
                        )
                    }
                )
                Row(
                    content = {
                        Text("Soundfont Directory")
                        Button(
                            onClick = {},
                            content = { Text("SF Directory") }
                        )
                    }
                )
                Row(
                    content = {
                        Text("Project Directory")
                        Button(
                            onClick = {},
                            content = { Text("Project Directory") }
                        )
                    }
                )
                Row(
                    content = {
                        Text("Playback Sample Rate")
                        Slider(
                            onValueChange = {},
                            value = 0f,
                            valueRange = 0f .. 100f,
                        )
                    }
                )

            }
        })
}