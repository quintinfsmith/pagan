package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

data class SongPositionPointer(var beat: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val least = this.beat and 0x007F
        val most = (this.beat shr 8) and 0x007F

        return byteArrayOf(
            0xF2.toByte(),
            least.toByte(),
            most.toByte()
        )
    }

    fun get_beat(): Int {
        return this.beat
    }
    fun set_beat(beat: Int) {
        this.beat = beat
    }
}