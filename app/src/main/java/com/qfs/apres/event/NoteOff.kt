package com.qfs.apres.event

class NoteOff(channel: Int, note: Int, velocity: Int): ChannelVoiceMessage(0x80, channel, arrayOf(note, velocity)) {
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