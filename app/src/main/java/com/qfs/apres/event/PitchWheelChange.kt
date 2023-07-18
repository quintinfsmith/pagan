package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

data class PitchWheelChange(var channel: Int, var value: Float): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val unsigned_value = this.get_unsigned_value()
        val least = unsigned_value and 0x007F
        val most = (unsigned_value shr 8) and 0x007F
        return byteArrayOf(
            (0xE0 or this.channel).toByte(),
            least.toByte(),
            most.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
    fun get_value(): Float {
        return this.value
    }
    fun set_value(value: Float) {
        this.value = value
    }

    fun get_unsigned_value(): Int {
        return if (this.value == 0.toFloat()) {
            0x2000
        } else if (this.value < 0) {
            ((1.toFloat() + this.value) * 0x2000.toFloat()).toInt()
        } else {
            (this.value * 0x1FFF.toFloat()).toInt() + 0x2000
        }
    }
}