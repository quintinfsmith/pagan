package com.qfs.apres.event

data class SongSelect(var song: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF3.toByte(),
            (this.song and 0xFF).toByte()
        )
    }

    fun set_song(song: Int) {
        this.song = song
    }
    fun get_song(): Int {
        return song
    }
}