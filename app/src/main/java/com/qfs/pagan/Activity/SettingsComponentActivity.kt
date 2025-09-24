package com.qfs.pagan.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class SettingsComponentActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setContent {
            Blargh("BOOP")
        }
    }
}

@Composable
fun Blargh(text: String, modifier: Modifier = Modifier) {
    Box() {
        BasicText(
            text = text,
            color = { Color.Red }
        )
    }
}