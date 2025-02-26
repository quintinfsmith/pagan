package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.pagan.opusmanager.OpusLineAbstract
import kotlin.math.max

class OpusLineFrameTracker<T: OpusLineAbstract<*>>(var opus_line: T) {
    class IndefiniteNoteException: Exception()

    var generated_frames = Array(0) { 0F }
    val size: Int
        get() = this.generated_frames.size

    var handles: HashMap<Int, SampleHandle> = hashMapOf()
    var handle_start_map: HashMap<Int, MutableSet<Int>> = hashMapOf() // Frame::[Uuid]
    var handle_end_map: HashMap<Int, MutableSet<Int>> = hashMapOf() // frame:: [uuid]

    fun invalidate(from_frame: Int = 0, to_frame: Int = this.size) {
        for (i in from_frame until to_frame) {
            this.generated_frames[i] = 0F
        }
    }

    fun remove_handles_at_frame(frame: Int) {
        val handles = this.handle_start_map.remove(frame) ?: return
        for (handle_id in handles) {
            var handle = this.handles.remove(handle_id) ?: continue
            val id_set = this.handle_end_map[frame + handle.release_frame!! + handle.volume_envelope.frames_release]!!.remove(handle_id)

        }

    }

    fun add_handles(frame: Int, handles: Set<SampleHandle>) {
        if (!this.handle_start_map.containsKey(frame)) {
            this.handle_start_map[frame] = mutableSetOf()
        }
        for (handle in handles) {
            this.handle_start_map[frame]!!.add(handle.uuid)
        }

        var max_end_frame = 0
        for (handle in handles) {
            if (handle.release_frame == null) {
                throw IndefiniteNoteException()
            }

            val end_frame = frame + handle.release_frame!! + handle.volume_envelope.frames_release
            if (!this.handle_end_map.containsKey(end_frame)) {
                this.handle_end_map[end_frame] = mutableSetOf()
            }

            this.handle_end_map[end_frame]!!.add(handle.uuid)

            max_end_frame = max(max_end_frame, end_frame)
        }

        if (max_end_frame > this.size) {
            this.add_frames(max_end_frame - this.size, this.size)
        }

        this.invalidate(frame, max_end_frame)
    }

    private fun add_frames(count: Int, position: Int) {
        val new_frames = Array(count + this.size) { i: Int ->
            if (i <= position) {
                this.generated_frames[i]
            } else if (i > position + count) {
                this.generated_frames[i - count]
            } else {
                0F
            }
        }

        this.generated_frames = new_frames
    }

}