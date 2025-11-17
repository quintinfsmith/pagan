package com.qfs.pagan.ComponentActivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qfs.pagan.ViewModelPagan
import com.qfs.pagan.composable.ScaffoldWithTopBar

abstract class PaganComponentActivity: ComponentActivity() {
    companion object {
        val SIZE_XL = Pair(960.dp, 720.dp)
        val SIZE_L = Pair(640.dp, 480.dp)
        val SIZE_M = Pair(470.dp, 320.dp)
        val SIZE_S = Pair(426.dp, 320.dp)
    }

    @Composable
    abstract fun LayoutXLargePortrait()
    @Composable
    abstract fun LayoutLargePortrait()
    @Composable
    abstract fun LayoutMediumPortrait()
    @Composable
    abstract fun LayoutSmallPortrait()
    @Composable
    abstract fun LayoutXLargeLandscape()
    @Composable
    abstract fun LayoutLargeLandscape()
    @Composable
    abstract fun LayoutMediumLandscape()
    @Composable
    abstract fun LayoutSmallLandscape()

    val view_model: ViewModelPagan by this.viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view_model = this.view_model

        view_model.configuration.callbacks_force_orientation.add {
            this.requestedOrientation = it
        }

        this.setContent {
            var title by remember { mutableStateOf(view_model.title ?: "") }
            view_model._title_callbacks.add { title = it ?: "" }

            // Allow night mode mutability
            val night_mode = remember { mutableIntStateOf(view_model.configuration.night_mode) }
            view_model.configuration.callbacks_night_mode.add { night_mode.intValue = it }
            ScaffoldWithTopBar(title, night_mode) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(it)
                ) {
                    for (entry in view_model.dialog_queue.value.dialogs) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Dialog(onDismissRequest = {}) {
                                Card {
                                    entry.dialog()
                                }
                            }
                        }
                    }

                    if (this.maxWidth >= this.maxHeight) {
                        if (this.maxWidth >= SIZE_XL.first && this.maxHeight >= SIZE_XL.second) {
                            println("LANDSCAPE XL")
                            LayoutXLargeLandscape()
                        } else if (this.maxWidth >= SIZE_L.first && this.maxHeight >= SIZE_L.second) {
                            println("LANDSCAPE L")
                            LayoutLargeLandscape()
                        } else if (this.maxWidth >= SIZE_M.first && this.maxHeight >= SIZE_M.second) {
                            println("LANDSCAPE M")
                            LayoutMediumLandscape()
                        } else {
                            println("LANDSCAPE S")
                            LayoutSmallLandscape()
                        }
                    } else {
                        if (this.maxWidth >= SIZE_XL.second && this.maxHeight >= SIZE_XL.first) {
                            println("PORTRAIT XL")
                            LayoutXLargePortrait()
                        } else if (this.maxWidth >= SIZE_L.second && this.maxHeight >= SIZE_L.first) {
                            println("PORTRAIT L")
                            LayoutLargePortrait()
                        } else if (this.maxWidth >= SIZE_M.second && this.maxHeight >= SIZE_M.first) {
                            println("PORTRAIT M")
                            LayoutMediumPortrait()
                        } else {
                            println("PORTRAIT S")
                            LayoutSmallPortrait()
                        }
                    }
                }
            }
        }
    }
}