package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

data class ProgramChange(var channel: Int, var program: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xC0 or this.channel).toByte(),
            this.program.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun get_program(): Int {
        return this.program
    }
    fun set_program(program: Int) {
        this.program = program
    }
}