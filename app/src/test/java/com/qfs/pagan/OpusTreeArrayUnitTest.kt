package com.qfs.pagan

import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusTreeArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpusTreeArrayUnitTest {
    @Test
    fun test_init() {
        val array = OpusTreeArray<OpusEvent>()
        array.insert_beat(0)
        assertEquals(
            1,
            array.beat_count()
        )

    }

}