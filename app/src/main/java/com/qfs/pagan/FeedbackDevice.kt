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

class FeedbackDevice(var sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(ImmediateFrameMap(), sample_handle_manager.sample_rate, sample_handle_manager.buffer_size), VirtualMidiOutputDevice {
    var queued_kill_time: Long? = null
    val kill_mutex = Mutex()
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
                this.max_frame = max(frame + handle.release_frame!! + handle.get_release_duration(), this.max_frame)
            }
            return output
        }

        override fun get_size(): Int {
            return this.max_frame + 1
        }

        override fun get_beat_frames(): HashMap<Int, IntRange> {
            return HashMap()
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

    fun set_kill_time(kill_time: Long?) {
        runBlocking {
            this@FeedbackDevice.kill_mutex.withLock {
                this@FeedbackDevice.queued_kill_time = kill_time
            }
        }
    }

    fun queue_kill(millis: Int) {
        val working_kill_time = millis + System.currentTimeMillis()
        this.set_kill_time(working_kill_time)

        Thread.sleep(millis.toLong())

        if (working_kill_time == this@FeedbackDevice.queued_kill_time) {
            this.kill()
            this.set_kill_time(null)
        }
    }

    override fun on_stop() {
        (this.sample_frame_map as ImmediateFrameMap).max_frame = 0
    }

    fun new_event(event: NoteOn, duration_millis: Int) {
        val handles = this.sample_handle_manager.gen_sample_handles(event)
        for (handle in handles) {
            handle.release_frame = duration_millis * this.sample_rate / 1000
            handle.volume = event.get_velocity().toFloat() * 0.3F / 128F
            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }
        this.play()
    }

    fun new_event(event: NoteOn79, duration_millis: Int) {
        val handles = this.sample_handle_manager.gen_sample_handles(event)

        for (handle in handles) {
            handle.release_frame = duration_millis * this.sample_rate / 1000
            handle.volume = (event.velocity shr 8).toFloat() * 0.3F / 128F
            (this.sample_frame_map as ImmediateFrameMap).add(handle)
        }

        if (!this.is_playing) {
            this.play(0)
        }
    }

}