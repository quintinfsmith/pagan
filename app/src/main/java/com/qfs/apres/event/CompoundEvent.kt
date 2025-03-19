package com.qfs.apres.event

abstract class CompoundEvent(
        var channel: Int,
        var value: Int,
        val controller: Int,
        private val controller_lsb: Int = 0x20 + controller // NRN/RN are the only events that arent 0x20 apart. So assume 0x20 unless specified
    ): MIDIEvent {

    override fun as_bytes(): ByteArray {
        val value_msb = (0xFF00 and this.value) shr 8
        val value_lsb = 0x00FF and this.value
        return byteArrayOf(
            (0xB0 or this.channel).toByte(),
            this.controller.toByte(),
            value_msb.toByte(),
            0x00.toByte(),
            (0xB0 or this.channel).toByte(),
            this.controller_lsb.toByte(),
            value_lsb.toByte()
        )
    }

}