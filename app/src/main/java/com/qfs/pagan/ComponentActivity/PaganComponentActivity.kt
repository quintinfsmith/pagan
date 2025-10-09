package com.qfs.pagan.ComponentActivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ViewModelPagan
import com.qfs.pagan.composable.ScaffoldWithTopBar
import kotlin.getValue

abstract class PaganComponentActivity: ComponentActivity() {
    companion object {
        val WIDTH_XL = 800.dp
        val WIDTH_L = 600.dp
        val WIDTH_M = 480.dp
        val WIDTH_S = 380.dp
    }

    @Composable
    abstract fun LayoutXLarge()
    @Composable
    abstract fun LayoutLarge()
    @Composable
    abstract fun LayoutMedium()
    @Composable
    abstract fun LayoutSmall()
    @Composable
    abstract fun LayoutXSmall()

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
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(it)
                ) {
                    if (this.minWidth >= WIDTH_XL) LayoutXLarge()
                    else if (this.minWidth > WIDTH_L) LayoutLarge()
                    else if (this.minWidth > WIDTH_M) LayoutMedium()
                    else if (this.minWidth > WIDTH_S) LayoutSmall()
                    else LayoutXSmall()
                }
            }
        }
    }
}