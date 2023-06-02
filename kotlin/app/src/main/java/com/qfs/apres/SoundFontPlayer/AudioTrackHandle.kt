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
        val buffer_size_in_bytes: Int = AudioTrack.getMinBufferSize(
            sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        )
        val buffer_size_in_frames: Int = buffer_size_in_bytes / 4
        val base_delay_in_frames = buffer_size_in_frames * 2
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
    var sample_handles = HashMap<Int, SampleHandle>()
    private var sample_handles_mutex = Mutex()
    private var keygen: Int = 0

    var is_playing = false
    private var stop_forced = false
    private var stop_called_from_write = false // play *may*  be called between a //write_next_chunk() finding an incomplete chunk and finishing the call
    var last_buffer_start_ts: Long? = null

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
    fun genkey(): Int {
        val output = this.keygen

        this.keygen += 1
        if (maxkey <= this.keygen) {
            this.keygen = 0
        }

        return output
    }

    // join_delay exists to position the start of the note press when generating the wave.
    // since it's not likely that notes will only be pressed exactly in between loops
    fun get_join_delay(buffer_ts: Long?, target_ts: Long): Int {
        // target is this time at which the note was pressed
        // calc_delay is the time at which all required calculations were completed
        var join_delay = base_delay_in_frames
        if (buffer_ts != null) {
            val time_delay = (target_ts - buffer_ts).toInt()
            join_delay += (sample_rate * time_delay / 4000)
        }
        return join_delay
    }

    private fun add_sample_handle(handle: SampleHandle, join_delay: Int): Int? {
        if (this.stop_forced) {
            throw HandleStoppedException()
        }
        val that = this
        return runBlocking {
            that.sample_handles_mutex.withLock {
                val key = that.genkey()
                that.sample_handles[key] = handle
                handle.join_delay = join_delay
                key
            }
        }
    }

    fun add_sample_handles(handles: Set<SampleHandle>, join_delay: Int): Set<Int> {
        if (this.stop_forced) {
            throw HandleStoppedException()
        }
        val output = mutableSetOf<Int>()
        for (handle in handles) {
            output.add(this.add_sample_handle(handle, join_delay) ?: continue)
        }
        return output
    }

    fun queue_sample_handle_stop(key: Int, remove_delay: Int) {
        if (this.stop_forced) {
            throw HandleStoppedException()
        }
        val that = this
        return runBlocking {
            that.sample_handles_mutex.withLock {
                that.sample_handles[key]?.remove_delay = remove_delay
            }
        }
    }

    fun queue_sample_handles_stop(keys: Set<Int>, remove_delay: Int) {
        if (this.stop_forced) {
            throw HandleStoppedException()
        }
        for (key in keys) {
            this.queue_sample_handle_stop(key, remove_delay)
        }
    }

    fun queue_sample_handle_release(key: Int, release_delay: Int) {
        val that = this
        runBlocking {
            that.sample_handles_mutex.withLock {
                that.sample_handles[key]?.release_delay = release_delay
            }
        }
    }

    fun queue_sample_handles_release(keys: Set<Int>, release_delay: Int) {
        for (key in keys) {
            this.queue_sample_handle_release(key, release_delay)
        }
    }

    fun remove_sample_handle(key: Int): SampleHandle? {
        val that = this
        return runBlocking {
            that.sample_handles_mutex.withLock {
                that.sample_handles.remove(key)
            }
        }
    }

    fun remove_sample_handles(keys: Set<Int>): Set<SampleHandle> {
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

    fun kill_samples(keys: Set<Int>) {
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

    private fun write_next_chunk() {
        val use_bytes = ByteArray(buffer_size_in_bytes) { 0 }
        val kill_handles = mutableSetOf<Int>()
        var cut_point: Int? = null

        val that = this
        val sample_handles = runBlocking {
            that.sample_handles_mutex.withLock {
                that.sample_handles.toList()
            }
        }

        val short_max = Short.MAX_VALUE.toFloat()

        if (this.sample_handles.isEmpty()) {
            this.stop_called_from_write = true
        } else {
            var max_left = 0
            var max_right = 0
            for ((_, sample_handle) in sample_handles) {
                when (sample_handle.stereo_mode and 7) {
                    1 -> { // mono
                        val next_max = sample_handle.get_next_max(buffer_size_in_frames)
                        max_left += next_max
                        max_right += next_max
                    }
                    2 -> { // right
                        max_right += sample_handle.get_next_max(buffer_size_in_frames)
                    }
                    4 -> { // left
                        max_left += sample_handle.get_next_max(buffer_size_in_frames)
                    }
                    else -> {}
                }
            }

            val control_sample_left = IntArray(buffer_size_in_frames) { 0 }
            val control_sample_right = IntArray(buffer_size_in_frames) { 0 }
            for (x in 0 until buffer_size_in_frames) {
                var left = 0
                var right = 0
                for ((key, sample_handle) in sample_handles) {
                    if (key in kill_handles) {
                        continue
                    }
                    var frame_value = sample_handle.get_next_frame()

                    if (frame_value == null) {
                        kill_handles.add(key)
                        if (kill_handles.size == sample_handles.size && cut_point == null) {
                            this.stop_called_from_write = true
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

                control_sample_left[x] = left
                control_sample_right[x] = right
            }

            val gain_factor_left = if (max_left > Short.MAX_VALUE) {
                short_max / max_left.toFloat()
            } else {
                1F
            }

            val gain_factor_right = if (max_right > Short.MAX_VALUE) {
                short_max / max_right.toFloat()
            } else {
                1F
            }

            for (x in control_sample_left.indices) {
                val right = (control_sample_right[x] * gain_factor_right).toInt()
                val left = (control_sample_left[x] * gain_factor_left).toInt()

                use_bytes[(4 * x)] = (right and 0xFF).toByte()
                use_bytes[(4 * x) + 1] = ((right and 0xFF00) shr 8).toByte()

                use_bytes[(4 * x) + 2] = (left and 0xFF).toByte()
                use_bytes[(4 * x) + 3] = ((left and 0xFF00) shr 8).toByte()
            }

            if (this.audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
                try {
                    this.audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
                } catch (e: IllegalStateException) {
                    // Shouldn't need to do anything. the audio track was released and this should stop on its own
                }
            }
        }

        for (key in kill_handles) {
            this.remove_sample_handle(key)
        }

        if (this.stop_called_from_write) {
            this.is_playing = false
            // Clearing these since an abrupt stop means that remove/release delays prevent the
            // write loop from popping the handles when they finish
            this.sample_handles.clear()
        }
    }

    private fun write_loop() {
        this.audioTrack.play()
        // write an empty chunk to give a bit of a time buffer for finer
        // control of when to start playing samples
        //this.write_empty_chunk()
        while (this.is_playing) {
            this.last_buffer_start_ts = System.currentTimeMillis()
            this.write_next_chunk()
        }
        this.last_buffer_start_ts = null
        this.audioTrack.stop()
    }

    fun stop() {
        this.stop_forced = true
        this.is_playing = false
        this.stop_called_from_write = false
        this.last_buffer_start_ts = null
        this.sample_handles.clear()
        //this.audioTrack.stop()
    }

    fun enable_play() {
        this.stop_forced = false
    }

}

