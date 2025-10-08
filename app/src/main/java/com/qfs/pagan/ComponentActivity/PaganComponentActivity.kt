package com.qfs.pagan.ComponentActivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.composable.ScaffoldWithTopBar

abstract class PaganComponentActivity: ComponentActivity() {
    companion object {
        val WIDTH_XL = 800.dp
        val WIDTH_L = 600.dp
        val WIDTH_M = 480.dp
        val WIDTH_S = 380.dp
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContent {
            ScaffoldWithTopBar("Bums") {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(it)
                ) {
                    if (this.minWidth >= WIDTH_XL) {
                        this@PaganComponentActivity.LayoutXLarge()
                    } else if (this.minWidth > WIDTH_L) {
                        this@PaganComponentActivity.LayoutLarge()
                    } else if (this.minWidth > WIDTH_M) {
                        this@PaganComponentActivity.LayoutMedium()
                    } else if (this.minWidth > WIDTH_S) {
                        this@PaganComponentActivity.LayoutSmall()
                    } else {
                        this@PaganComponentActivity.LayoutXSmall()
                    }
                }
            }
        }
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



}