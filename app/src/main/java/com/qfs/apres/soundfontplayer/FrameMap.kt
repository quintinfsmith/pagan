package com.qfs.apres.soundfontplayer

interface FrameMap {
    fun get_new_handles(frame: Int): Set<SampleHandle>?
    fun get_beat_frames(): List<Int>
}