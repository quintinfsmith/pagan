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
    override fun RowScope.TopBar() {}

    @Composable
    override fun Drawer(modifier: Modifier) {
    }

    @Composable
    fun Layout() {
        Button(onClick = {
            test_model.t.text = "Toast"
        }) {
            Text(test_model.t.text)
        }
    }
    @Composable
    override fun LayoutXLargePortrait() = Layout()
    @Composable
    override fun LayoutLargePortrait() = Layout()
    @Composable
    override fun LayoutMediumPortrait() = Layout()
    @Composable
    override fun LayoutSmallPortrait() = Layout()
    @Composable
    override fun LayoutXLargeLandscape() = Layout()
    @Composable
    override fun LayoutLargeLandscape() = Layout()
    @Composable
    override fun LayoutMediumLandscape() = Layout()
    @Composable
    override fun LayoutSmallLandscape() = Layout()
}