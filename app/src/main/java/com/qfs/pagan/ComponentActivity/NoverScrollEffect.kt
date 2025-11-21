package com.qfs.pagan.ComponentActivity

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope

class NoverScrollEffect(val scope: CoroutineScope) : OverscrollEffect {
    override val isInProgress: Boolean
        get() = false

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        return Offset(0f, 0f)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) { }
}