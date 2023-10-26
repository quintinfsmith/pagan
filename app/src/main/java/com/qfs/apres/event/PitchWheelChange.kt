package com.qfs.apres.event

class PitchWheelChange(channel: Int, value: Double): ChannelVoiceMessage(0xE0, channel, PitchWheelChange.to_ints(value))  {
    companion object {
        fun to_unsigned(value: Double): Int {
            return if (value == 0.0) {
                0x2000
            } else if (value < 0) {
                ((1.0 + value) * 0x2000.toDouble()).toInt()
            } else {
                (value * 0x1FFF.toDouble()).toInt() + 0x2000
            }
        }
        fun to_ints(value: Double): Array<Int> {
            val unsigned_value = to_unsigned(value)
            val least = unsigned_value and 0x007F
            val most = (unsigned_value shr 8) and 0x007F
            return arrayOf(least, most)
        }
        fun from_bytes(msb: Int, lsb: Int): Double {
            val unsigned_value = ((msb and 0xFF) shl 8) + (lsb and 0xFF)
            return ((unsigned_value.toDouble() * 2.0) / 0x3FFF.toDouble()) - 1
        }
    }

    fun get_value(): Double {
        val msb = this.get_data(1)
        val lsb = this.get_data(0)
        return PitchWheelChange.from_bytes(msb, lsb)
    }

    fun set_value(value: Double) {
        var ints = PitchWheelChange.to_ints(value)
        this.set_data(0, ints[0])
        this.set_data(1, ints[1])
    }
}