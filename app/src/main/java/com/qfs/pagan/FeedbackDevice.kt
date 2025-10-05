package com.qfs.pagan

import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.ControllerEventData
import com.qfs.apres.soundfontplayer.EffectType
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.ProfileBuffer
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WaveGenerator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class FeedbackDevice(private var _sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(ImmediateFrameMap(), _sample_handle_manager.sample_rate, _sample_handle_manager.buffer_size, WaveGenerator.StereoMode.Mono), VirtualMidiOutputDevice {
    class ImmediateFrameMap: FrameMap {
        private val _handles = mutableSetOf<SampleHandle>()
        private val _mutex = Mutex()
        var max_frame = -1
        var volume = .6F
        val volume_event_data = ControllerEventData(
            listOf(ControllerEventData.IndexedProfileBufferFrame(
                first_frame = 0,
                last_frame = 0,
                value = floatArrayOf(this.volume),
                increment = floatArrayOf(0F)
            )),
            EffectType.Volume
        )
        override fun get_new_handles(frame: Int): Set<Pair<SampleHandle, IntArray>>? {
            if (this._handles.isEmpty()) return null

            val output = mutableSetOf<Pair<SampleHandle, IntArray>>()
            for (handle in this._handles) {
                this.max_frame = max(frame + handle.release_frame!! + handle.get_release_duration(), this.max_frame)
                output.add(Pair(handle, intArrayOf(handle.uuid)))
            }

            runBlocking {
                this@ImmediateFrameMap._mutex.withLock {
                    this@ImmediateFrameMap._handles.clear()
                }
            }

            return output
        }

        override fun get_marked_frame(i: Int): Int? {
            return null
        }

        override fun has_frames_remaining(frame: Int): Boolean {
            return this._handles.isNotEmpty()
        }

        override fun get_size(): Int {
            // Not used in feedback
            return 0
        }

        override fun get_effect_buffers(): List<Triple<Int, Int, ProfileBuffer>> {
            return listOf(
                Triple(0, 0, ProfileBuffer(this.volume_event_data))
            )
        }

        override fun get_active_handles(frame: Int): Set<Pair<Int, Pair<SampleHandle, IntArray>>> {
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
        for (handle in this._sample_handle_manager.gen_sample_handles(event)) {
            handle.release_frame = duration_millis * this.sample_rate / 1000

            // Remove release phase. can get noisy on things like tubular bells with long fade outs
            handle.volume_envelope.frames_release = 0
            handle.volume_envelope.release = 0F

            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }

        if (this.in_playable_state()) {
            this.play(0)
        }
    }

    fun destroy() {
        (this.sample_frame_map as ImmediateFrameMap).volume_event_data.destroy()
    }
}
