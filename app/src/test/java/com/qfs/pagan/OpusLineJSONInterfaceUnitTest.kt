package com.qfs.pagan

import androidx.compose.ui.graphics.Color
import com.qfs.pagan.structure.opusmanager.OpusLineJSONInterface
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.OpusLine
import com.qfs.pagan.structure.opusmanager.base.TunedInstrumentEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import org.junit.Assert.assertEquals
import org.junit.Test

class OpusLineJSONInterfaceUnitTest {
    @Test
    fun test_color() {
        val working_line = OpusLine(MutableList(4) { ReducibleTree<TunedInstrumentEvent>() })
        working_line.palette.event = Color(0xFFFF0000)
        val working_json_obj = OpusLineJSONInterface.to_json(working_line)
        assertEquals(
            "#FFFF0000".lowercase(),
            working_json_obj.get_hashmap("palette").get_string("event").lowercase()
        )
        val decoded = OpusColorPalette.from_json(working_json_obj.get_hashmap("palette"))
        assertEquals(
            working_line.palette,
            decoded
        )
    }
}