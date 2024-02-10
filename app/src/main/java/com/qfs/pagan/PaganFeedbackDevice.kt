package com.qfs.pagan

import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class PaganFeedbackDevice(var sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(ImmediateFrameMap(), sample_handle_manager.sample_rate, sample_handle_manager.buffer_size), VirtualMidiOutputDevice {
    class ImmediateFrameMap: FrameMap {
        val handles = mutableSetOf<SampleHandle>()
        val mutex = Mutex()
        var max_frame = -1
        override fun get_new_handles(frame: Int): Set<SampleHandle> {
            val output = this.handles.toSet()
            runBlocking {
                this@ImmediateFrameMap.mutex.withLock {
                    this@ImmediateFrameMap.handles.clear()
                }
            }
            for (handle in output) {
                this.max_frame =
                    max(frame + handle.release_frame!! + handle.frame_count_release, this.max_frame)
            }
            return output
        }

        override fun get_size(): Int {
            return this.max_frame + 1
        }

        override fun get_beat_frames(): List<Int> {
            return listOf()
        }

        override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
            return setOf()
        }

        fun add(handle: SampleHandle) {
            runBlocking {
                this@ImmediateFrameMap.mutex.withLock {
                    this@ImmediateFrameMap.handles.add(handle)
                }
            }
        }
    }

    init {
        this.fill_buffer_cache = false
        this.minimum_buffer_cache_size = this.buffer_size
        this.buffer_cache_size_limit = this.buffer_size
        //this.wave_generator.timeout = 1
    }
    override fun on_stop() {
        (this.sample_frame_map as ImmediateFrameMap).max_frame = 0
    }

    fun new_event(event: NoteOn, duration_millis: Int) {
        val handles = this.sample_handle_manager.gen_sample_handles(event)
        for (handle in handles) {
            handle.release_frame = duration_millis * this.sample_rate / 1000
            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }
        this.play()
    }

    fun new_event(event: NoteOn79, duration_millis: Int) {
        val handles = this.sample_handle_manager.gen_sample_handles(event)

        var kill_frame = 0
        for (handle in handles) {
            handle.release_frame = duration_millis * this.sample_rate / 1000
            kill_frame = max(kill_frame, handle.release_frame!! + handle.frame_count_release)
            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }

        if (this.is_playing) {
            this.set_kill_frame(kill_frame + this.wave_generator.frame)
        } else {
            this.play(0, kill_frame)
        }
    }

}