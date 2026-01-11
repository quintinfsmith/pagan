package com.qfs.pagan.ComponentActivity

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel

class TestActivity: PaganComponentActivity() {
    class TestClass(var bloop: IntArray, var text: String = "TEST")
    class TestModel: ViewModel() {
        var t = TestClass(IntArray(4))
    }

    var test_model = TestModel()

    @Composable
    override fun Drawer(modifier: Modifier) { }
    override fun on_back_press_check(): Boolean {
        TODO("Not yet implemented")
    }

    @Composable
    fun Layout(modifier: Modifier = Modifier) {
        Button(onClick = {
            test_model.t.text = "Toast"
        }) {
            Text(test_model.t.text)
        }
    }

    @Composable
    override fun LayoutXLargePortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutLargePortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutMediumPortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutSmallPortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutXLargeLandscape(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutLargeLandscape(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutMediumLandscape(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutSmallLandscape(modifier: Modifier) = Layout(modifier)
}