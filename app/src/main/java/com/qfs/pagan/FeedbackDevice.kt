package com.qfs.pagan

import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class FeedbackDevice(private var _sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(ImmediateFrameMap(), _sample_handle_manager.sample_rate, _sample_handle_manager.buffer_size), VirtualMidiOutputDevice {
    class ImmediateFrameMap: FrameMap {
        private val _handles = mutableSetOf<SampleHandle>()
        private val _mutex = Mutex()
        var max_frame = -1

        override fun get_new_handles(frame: Int): Set<SampleHandle>? {
            if (this._handles.isEmpty()) {
                return null
            }

            val output = this._handles.toSet()
            runBlocking {
                this@ImmediateFrameMap._mutex.withLock {
                    this@ImmediateFrameMap._handles.clear()
                }
            }

            for (handle in output) {
                this.max_frame = max(frame + handle.release_frame!! + handle.get_release_duration(), this.max_frame)
            }
            return output
        }

        override fun get_marked_frames(): Array<Int> {
            return arrayOf<Int>()
        }

        override fun has_handles_remaining(frame: Int): Boolean {
            return this._handles.isNotEmpty()
        }

        override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
            return setOf()
        }

        fun add(handle: SampleHandle) {
            runBlocking {
                this@ImmediateFrameMap._mutex.withLock {
                    this@ImmediateFrameMap._handles.add(handle)
                }
            }
        }
    }

    override fun on_buffer() { }
    override fun on_buffer_done() { }
    override fun on_start() { }

    override fun on_stop() {
        (this.sample_frame_map as ImmediateFrameMap).max_frame = 0
    }

    override fun on_cancelled() { }
    override fun on_mark(i: Int) { }

    //fun new_event(event: NoteOn, duration_millis: Int) {
    //    val handles = this.sample_handle_manager.gen_sample_handles(event)
    //    for (handle in handles) {
    //        handle.release_frame = duration_millis * this.sample_rate / 1000
    //        handle.volume = event.get_velocity().toFloat() * 0.2F / 128F
    //        (this.sample_frame_map as ImmediateFrameMap).add(handle)
    //    }
    //    this.play()
    //}

    fun new_event(event: NoteOn79, duration_millis: Int) {
        val handles = this._sample_handle_manager.gen_sample_handles(event)

        for (handle in handles) {
            handle.release_frame = duration_millis * this.sample_rate / 1000
            handle.volume = (event.velocity shr 8).toFloat() * 0.2F / 128F
            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }

        if (!this.is_playing) {
            this.play(0)
        }
    }

}