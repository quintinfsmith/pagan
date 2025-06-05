package com.qfs.pagan

import com.qfs.pagan.opusmanager.OpusLine
import com.qfs.pagan.opusmanager.OpusLineJSONInterface
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import org.junit.Assert.assertEquals
import org.junit.Test

class OpusLineJSONInterfaceUnitTest {
    @Test
    fun test_color() {
        val working_line = OpusLine(MutableList(4) { OpusTree<TunedInstrumentEvent>() })
        working_line.color = (255 * 256 * 256) + (0 * 256) + (0)
        val working_json_obj = OpusLineJSONInterface.to_json(working_line)
        assertEquals(
            "#FF0000".lowercase(),
            working_json_obj.get_string("color").lowercase()
        )
        val decoded_line = OpusLineJSONInterface.opus_line(working_json_obj, 4)
        assertEquals(
            working_line.color,
            decoded_line.color
        )
    }
}