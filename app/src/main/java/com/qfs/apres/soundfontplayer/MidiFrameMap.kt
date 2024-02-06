package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi

class MidiFrameMap: FrameMap {
    private val frames = HashMap<Int, SampleHandle>()

    fun clear() {
        this.frames.clear()
    }

    fun parse_midi(midi: Midi) {

    }

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        TODO("Not yet implemented")
    }

    override fun get_beat_frames(): List<Int> {
        TODO("Not yet implemented")
    }

    override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
        TODO("Not yet implemented")
    }
}