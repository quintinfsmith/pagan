package com.qfs.apres.soundfontplayer

interface FrameMap {
    fun get_new_handles(frame: Int): List<Set<SampleHandle>>?
    fun get_track_priority(track: Int): Float
    fun get_beat_frames(): HashMap<Int, IntRange>
    fun get_active_handles(frame: Int): List<Set<Pair<Int, SampleHandle>>>
    fun get_size(): Int
}