package com.qfs.apres.event

abstract class CompoundEvent(var channel: Int, var value: Int): MIDIEvent {
    abstract val controller: Int
    open val controller_lsb: Int? = null// NRN/RN are the only events that arent 0x20 apart. So assume 0x20 unless specified
    override fun as_bytes(): ByteArray {
        val controller_lsb: Int = if (this.controller_lsb == null) {
            this.controller + 0x20
        } else {
            this.controller_lsb!!
        }
        val value_msb = 0xFF00 and this.value
        val value_lsb = 0x00FF and this.value
        return byteArrayOf(
            (0xB0 or this.channel).toByte(),
            this.controller.toByte(),
            value_msb.toByte(),
            0x00.toByte(),
            (0xB0 or this.channel).toByte(),
            controller_lsb.toByte(),
            value_lsb.toByte()
        )
    }

}