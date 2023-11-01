package com.qfs.apres.event

class NoteOn(channel: Int, note: Int, velocity: Int, index: Int? = null, ): ChannelVoiceMessage(0x90, channel, arrayOf(note, velocity)) {
    fun get_note(): Int {
        return this.get_data(0)
    }

    fun get_velocity(): Int {
        return this.get_data(1)
    }

    fun set_note(note: Int) {
        this.set_data(0, note)
    }

    fun set_velocity(velocity: Int) {
        this.set_data(1, velocity)
    }
}

