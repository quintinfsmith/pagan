package com.qfs.apres.event2

import com.qfs.apres.event.GeneralMIDIEvent

abstract interface UMPEvent: GeneralMIDIEvent

abstract class SystemExclusive(
    var group: Int,
    var status: Int,
    var stream: Int
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val data = this.get_data_bytes()
        // TODO: Validate data size and status
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            ((this.status shl 4) and (data.size)).toByte(),
            *data
        )
    }
    abstract fun get_data_bytes(): ByteArray
}

// abstract class MidiCI(group: Int, stream: Int): SystemExclusive(group, 0xD, stream) {
//     
// }

class StartOfClip(): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF0.toByte(),
            0x20.toByte(),
            *(ByteArray(14) { 0.toByte() })
        )
    }
}

class EndOfClip(): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF0.toByte(),
            0x21.toByte(),
            *(ByteArray(14) { 0.toByte() })
        )
    }
}

class PolyPressure(
    var channel: Int,
    var index: Int,
    var pressure: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val pressure_int = (this.pressure * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xA0 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x0.toByte(),

            // Assuming little endian
            ((pressure_int shr 24) and 0xFF).toByte(),
            ((pressure_int shr 16) and 0xFF).toByte(),
            ((pressure_int shr 8) and 0xFF).toByte(),
            (pressure_int and 0xFF).toByte(),
        )
    }
}
class ChannelPressure(
    var channel: Int,
    var pressure: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val pressure_int = (this.pressure * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xD0 or (this.channel and 0x0F)).toByte(),
            0x00.toByte(),
            0x00.toByte(),
            // Assuming little endian
            ((pressure_int shr 24) and 0xFF).toByte(),
            ((pressure_int shr 16) and 0xFF).toByte(),
            ((pressure_int shr 8) and 0xFF).toByte(),
            (pressure_int and 0xFF).toByte(),
        )
    }
}

class ChannelPitchBend(
    var channel: Int,
    var bend: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val bend_int = (((this.bend + 1F) / 2F) * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xE0 or (this.channel and 0x0F)).toByte(),
            0x00.toByte(),
            ((bend_int shr 24) and 0xFF).toByte(),
            ((bend_int shr 16) and 0xFF).toByte(),
            ((bend_int shr 8) and 0xFF).toByte(),
            (bend_int and 0xFF).toByte()
        )
    }
}

class PerNotePitchBend(
    val index: Int = 0,
    var channel: Int,
    var bend: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val bend_int = (((this.bend + 1F) / 2F) * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0x60 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x00.toByte(),
            ((bend_int shr 24) and 0xFF).toByte(),
            ((bend_int shr 16) and 0xFF).toByte(),
            ((bend_int shr 8) and 0xFF).toByte(),
            (bend_int and 0xFF).toByte()
        )
    }
}

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

class ProgramChange(
    var index: Int,
    var channel: Int,
    var group: Int,
    var program: Int,
    var bank: Int,
    var bank_select: Boolean = false,
): UMPEvent {
    override fun as_bytes(): ByteArray {
        var option_flags = 0x00
        if (this.bank_select) {
            option_flags += 1
        }

        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xC0 or (this.channel and 0x0F)).toByte(),
            0x00.toByte(),
            option_flags.toByte(),
            (0x7F and this.program).toByte(),
            0x00.toByte(),
            ((this.bank and 0x3f80) shr 7).toByte(),
            (this.bank and 0x007F).toByte()
        )
    }
}

class ControlChange(
    var index: Int,
    var group: Int,
    var status: Int,
    var channel: Int,
    var value: Int
): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            ((this.status shl 4) or (this.channel and 0x0F)).toByte(),
            (this.index and 0xFF).toByte(),
            0x00.toByte(),
            ((this.value shr 24) and 0xFF).toByte(),
            ((this.value shr 16) and 0xFF).toByte(),
            ((this.value shr 8) and 0xFF).toByte(),
            (this.value and 0xFF).toByte()
        )
    }
}

