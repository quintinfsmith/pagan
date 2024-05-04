package com.qfs.apres.soundfontplayer

interface FrameMap {
    fun get_new_handles(frame: Int): Set<SampleHandle>?
    fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>>
    fun get_marked_frames(): Array<Int>
    fun has_handles_remaining(frame: Int): Boolean
    fun get_size(): Int
}