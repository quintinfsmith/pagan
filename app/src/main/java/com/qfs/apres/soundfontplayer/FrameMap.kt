package com.qfs.apres.soundfontplayer

interface FrameMap {
    /* Get handles that start at frame */
    fun get_new_handles(frame: Int): Set<Pair<SampleHandle, IntArray>>? //handle, merge keys
    /* Get handles that are already playing during frame */
    fun get_active_handles(frame: Int): Set<Pair<Int, Pair<SampleHandle, IntArray>>> // first_frame::(handle::mergekeys)
    //fun get_marked_frames(): Array<Int>
    fun get_marked_frame(i: Int): Int?
    fun has_frames_remaining(frame: Int): Boolean
    fun get_size(): Int
    fun get_effect_buffers(): List<Triple<Int, Int, ProfileBuffer>>
    fun destroy() {
        for ((_, _, buffer) in this.get_effect_buffers()) {
            buffer.destroy()
        }
    }
}
