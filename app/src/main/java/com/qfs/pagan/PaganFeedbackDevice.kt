package com.qfs.pagan

import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.NoteOn
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PaganFeedbackDevice(var sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(ImmediateFrameMap(), sample_handle_manager.sample_rate, sample_handle_manager.buffer_size), VirtualMidiOutputDevice {
    class ImmediateFrameMap: FrameMap {
        val handles = mutableSetOf<SampleHandle>()
        val mutex = Mutex()
        override fun get_new_handles(frame: Int): Set<SampleHandle> {
            val output = this.handles.toSet()
            runBlocking {
                this@ImmediateFrameMap.mutex.withLock {
                    this@ImmediateFrameMap.handles.clear()
                }
            }
            return output
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

    fun new_event(event: NoteOn, duration_millis: Int) {
        val handles = this.sample_handle_manager.gen_sample_handles(event)
        for (handle in handles) {
            handle.release_frame = duration_millis * this.sample_rate / 1000
            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }
    }
}