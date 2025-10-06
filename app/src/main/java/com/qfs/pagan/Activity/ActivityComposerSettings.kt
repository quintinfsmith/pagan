package com.qfs.pagan.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


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
    val width_xl = 800.dp
    val width_l = 600.dp
    val width_m = 480.dp
    val width_s = 380.dp
    val width_xs = 320.dp

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
            BoxWithConstraints(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xff8d6e63))
            ) {
                if (minWidth < width_xl) {
                    Column {
                        SettingsSectionFirst()
                    }
                    Column {
                        SettingsSectionSecond()
                    }
                //} else if (minWidth < width_xl) {
                //} else if (minWidth < width_l) {
                //} else if (minWidth < width_m) {
                } else {
                    SettingsSectionFirst()
                    SettingsSectionSecond()
                }
            }
        })
}

@Composable
fun SettingsSectionFirst() {
    Text("Active Soundfont")
    Button(
        onClick = { println("Butts") },
        content = { Text( "Active Soundfont" ) }
    )
    Text("Soundfont Directory")
    Button(
        onClick = {},
        content = { Text("SF Directory") }
    )
    Text("Project Directory")
    Button(
        onClick = {},
        content = { Text("Project Directory") }
    )
}

@Composable
fun SettingsSectionSecond() {
    val options_playback = listOf( 4000, 8000, 22050, 44100, 48000)
    var slider_position by remember { mutableIntStateOf(0) }

    Text("Playback Sample Rate: ${options_playback[slider_position]}")
    Slider(
        value = slider_position.toFloat() / options_playback.size.toFloat(),
        onValueChange = { slider_position = (it * options_playback.size).toInt() },
    )
    Row {
        Text("Clip sample fade-out")
        Switch(
            checked = true,
            onCheckedChange = { }
        )
    }
    Row {
        Text("Relative Input Mode")
        Switch(
            checked = true,
            onCheckedChange = { }
        )
    }
    Row {
        Text("Associate soundfonts with projects")
        Switch(
            checked = true,
            onCheckedChange = { }
        )
    }
    Row {
        Text("Percussion as standard instrument")
        Switch(
            checked = true,
            onCheckedChange = { }
        )
    }
    var selectedIndex by remember { mutableIntStateOf(1) }
    val options = listOf("Landscape", "System", "Portrait")
    Text("Screen Orientation")
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { selectedIndex = index },
                selected = index == selectedIndex,
                label = { Text(label) }
            )
        }
    }

    var selectedIndexb by remember { mutableIntStateOf(1) }
    val optionsb = listOf("Night", "System", "Day")
    Text("Night Mode")
    SingleChoiceSegmentedButtonRow {
        optionsb.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = optionsb.size
                ),
                onClick = { selectedIndexb = index },
                selected = index == selectedIndexb,
                label = { Text(label) }
            )
        }
    }
}