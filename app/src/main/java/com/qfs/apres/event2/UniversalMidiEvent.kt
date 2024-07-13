package com.qfs.apres.event2

import com.qfs.apres.event.GeneralMIDIEvent

abstract interface UMPEvent: GeneralMIDIEvent

class NoteOn79(
    var index: Int = 0,
    var channel: Int,
    var note: Int,
    var velocity: Int,
    var bend: Int = 0, // 512ths of a semitone
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0x90 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x03.toByte(),
            ((this.velocity and 0xFF00) shr 8).toByte(),
            (this.velocity and 0x00FF).toByte(),
            (((this.note and 0x7F) shl 9) or (this.bend and 0x01FF)).toByte()
        )
    }
}

class NoteOff79(
    var index: Int = 0,
    var channel: Int,
    var note: Int,
    var velocity: Int,
    var bend: Int = 0, // 512ths of a semitone
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0x80 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x03.toByte(),
            ((this.velocity and 0xFF00) shr 8).toByte(),
            (this.velocity and 0x00FF).toByte(),
            (((this.note and 0x7F) shl 9) or (this.bend and 0x01FF)).toByte()
        )
    }
}
