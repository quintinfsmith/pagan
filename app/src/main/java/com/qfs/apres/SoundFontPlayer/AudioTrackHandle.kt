package com.qfs.apres.SoundFontPlayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.thread

class AudioTrackHandle {
    class HandleStoppedException(): Exception()
    companion object {
        const val sample_rate = 44100
        val buffer_size: Int = AudioTrack.getMinBufferSize(
            sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        ) * 4
        //val buffer_size = sample_rate
        val buffer_size_in_bytes: Int = buffer_size * 4
        val base_delay_in_frames = buffer_size * 2
        private const val maxkey = 0xFFFFFFFF
    }

    private var audioTrack: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sample_rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
        )
        .setBufferSizeInBytes(buffer_size_in_bytes)
        .build()

    var sample_handles = HashMap<Long, SampleHandle>()
    private var sample_handles_mutex = Mutex()

    var is_playing = false
    private var stop_forced = false
    private var stop_called_from_write = false // play *may*  be called between a //write_next_chunk() finding an incomplete chunk and finishing the call
    var write_loop_ts: Long? = null
    var current_frame: Long? = null


    fun play() {
        if (this.is_playing || this.stop_forced) {
            return
        }
        this.stop_called_from_write = false
        this.is_playing = true

        thread {
            this.write_loop()
        }
    }

    // Generate a sample handle key
    fun genkey(): Long {
        return System.nanoTime()
    }

    // join_delay exists to position the start of the note press when generating the wave.
    // since it's not likely that notes will only be pressed exactly in between loops

    private fun add_sample_handle(handle: SampleHandle): Long {
        if (this.stop_forced) {
            throw HandleStoppedException()
        }

        val that = this
        return runBlocking {
            that.sample_handles_mutex.withLock {
                val key = that.genkey()

                //////////////////////////////////
                val current_frame = that.current_frame ?: 0
                val delta = if (that.write_loop_ts == null) {
                    // handle.timestamp - System.currentTimeMillis()
                    0
                } else {
                    handle.timestamp - that.write_loop_ts!!
                }

                val delta_in_frames = delta * (AudioTrackHandle.sample_rate / 1000)
                val delay_in_frames = (delta_in_frames - current_frame) + (AudioTrackHandle.base_delay_in_frames * 2)
                handle.join_delay = delay_in_frames.toInt()
                /////////////////////////////////////
                that.sample_handles[key] = handle

                key
            }
        }
    }

    fun add_sample_handles(handles: Set<SampleHandle>): Set<Long> {
        if (this.stop_forced) {
            throw HandleStoppedException()
        }
        val output = mutableSetOf<Long>()
        for (handle in handles) {
            output.add(this.add_sample_handle(handle) ?: continue)
        }
        return output
    }

    //fun queue_sample_handle_stop(key: Int, remove_delay: Int) {
    //    if (this.stop_forced) {
    //        throw HandleStoppedException()
    //    }
    //    val that = this
    //    return runBlocking {
    //        that.sample_handles_mutex.withLock {
    //            that.sample_handles[key]?.remove_delay = remove_delay
    //        }
    //    }
    //}

    //fun queue_sample_handles_stop(keys: Set<Int>, remove_delay: Int) {
    //    if (this.stop_forced) {
    //        throw HandleStoppedException()
    //    }
    //    for (key in keys) {
    //        this.queue_sample_handle_stop(key, remove_delay)
    //    }
    //}

    fun queue_sample_handle_release(key: Long) {
        val that = this
        runBlocking {
            that.sample_handles_mutex.withLock {
                val handle= that.sample_handles[key]?: return@withLock
                handle.set_release_delay(System.currentTimeMillis())
            }
        }
    }

    fun queue_sample_handles_release(keys: Set<Long>) {
        for (key in keys) {
            this.queue_sample_handle_release(key)
        }
    }

    fun remove_sample_handle(key: Long): SampleHandle? {
        val that = this
        return runBlocking {
            that.sample_handles_mutex.withLock {
                var output = that.sample_handles.remove(key)
                output
            }
        }
    }

    fun remove_sample_handles(keys: Set<Long>): Set<SampleHandle> {
        val that = this
        return runBlocking {
            that.sample_handles_mutex.withLock {
                val output = mutableSetOf<SampleHandle>()
                for (key in keys) {
                    output.add(that.sample_handles.remove(key) ?: continue)
                }
                output
            }
        }
    }

    fun kill_samples(keys: Set<Long>) {
        val that = this
        runBlocking {
            that.sample_handles_mutex.withLock {
                for (key in keys) {
                    that.sample_handles[key]?.kill_note()
                }
            }
        }
    }

    private fun write_empty_chunk() {
        val use_bytes = ByteArray(buffer_size_in_bytes) { 0 }
        this.audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
    }

    private fun write_next_chunk(): ByteArray {
        var use_bytes = ByteArray(buffer_size_in_bytes) { 0 }
        val kill_handles = mutableSetOf<Long>()
        var cut_point: Int? = null

        val that = this
        val control_sample_left = IntArray(AudioTrackHandle.buffer_size) { 0 }
        val control_sample_right = IntArray(AudioTrackHandle.buffer_size) { 0 }
        var has_sample = false
        for (x in 0 until AudioTrackHandle.buffer_size) {
            var left = 0
            var right = 0
            runBlocking {
                that.sample_handles_mutex.withLock {
                    for ((key, sample_handle) in that.sample_handles) {
                        if (key in kill_handles) {
                            continue
                        }
                        has_sample = true
                        val frame_value = sample_handle.get_next_frame()

                        if (frame_value == null) {
                            kill_handles.add(key)
                            if (kill_handles.size == sample_handles.size && cut_point == null) {
                                that.stop_called_from_write = true
                                cut_point = x
                            }
                            continue
                        }

                        // TODO: Implement ROM stereo modes
                        when (sample_handle.stereo_mode and 7) {
                            1 -> { // mono
                                left += frame_value
                                right += frame_value
                            }

                            2 -> { // right
                                right += frame_value
                            }

                            4 -> { // left
                                left += frame_value
                            }

                            else -> {}
                        }
                    }
                }
            }

            control_sample_left[x] = left
            control_sample_right[x] = right

            this.current_frame = (this.current_frame ?: 0) + 1
        }

        if (has_sample) {
            for (x in control_sample_left.indices) {
                val left = (control_sample_left[x] * .8F).toInt()
                val right = (control_sample_right[x] * .8F).toInt()

                use_bytes[(4 * x)] = (right and 0xFF).toByte()
                use_bytes[(4 * x) + 1] = ((right and 0xFF00) shr 8).toByte()

                use_bytes[(4 * x) + 2] = (left and 0xFF).toByte()
                use_bytes[(4 * x) + 3] = ((left and 0xFF00) shr 8).toByte()
            }
        } else {
            this.stop_called_from_write = true
        }

        this.remove_sample_handles(kill_handles)
        if (this.stop_called_from_write) {
            this.is_playing = false
            // Clearing these since an abrupt stop means that remove/release delays prevent the
            // write loop from popping the handles when they finish
            this.sample_handles.clear()
        }

        return use_bytes
    }

    private fun write_loop() {
        this.audioTrack.play()
        // write an empty chunk to give a bit of a time buffer for finer
        // control of when to start playing samples
        //this.write_empty_chunk()
        this.write_loop_ts = System.currentTimeMillis()
        this.current_frame = 0
        while (this.is_playing) {
            val use_bytes = this.write_next_chunk()
            if (this.audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
                try {
                    this.audioTrack.write(
                        use_bytes,
                        0,
                        use_bytes.size,
                        AudioTrack.WRITE_BLOCKING
                    )
                } catch (e: IllegalStateException) {
                    // Shouldn't need to do anything. the audio track was released and this should stop on its own
                }
            }
        }
        this.write_loop_ts = null
        this.current_frame = null
        this.audioTrack.stop()
    }

    fun stop() {
        this.stop_forced = true
        this.is_playing = false
        this.stop_called_from_write = false
        this.write_loop_ts = null
        this.current_frame = null
        this.sample_handles.clear()
        //this.audioTrack.stop()
    }

    fun enable_play() {
        this.stop_forced = false
    }

}

