package com.qfs.apres.event

data class NoteOff(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x80 or this.channel).toByte(),
            this.note.toByte(),
            this.velocity.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }

    fun get_note(): Int {
        return this.note
    }

    fun get_velocity(): Int {
        return this.velocity
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun set_note(note: Int) {
        this.note = note
    }

    fun set_velocity(velocity: Int) {
        this.velocity = velocity
    }
}