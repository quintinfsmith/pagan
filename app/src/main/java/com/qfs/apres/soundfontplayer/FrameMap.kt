package com.qfs.apres.soundfontplayer

interface FrameMap {
    fun get_new_handles(frame: Int): Set<SampleHandle>?
    fun get_beat_frames(): List<Int>
    fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>>
    fun get_size(): Int
}