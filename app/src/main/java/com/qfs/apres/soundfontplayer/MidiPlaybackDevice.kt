package com.qfs.apres.soundfontplayer

import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.max

open class MidiPlaybackDevice(
        var sample_handle_manager: SampleHandleManager,
        private var cache_size_limit: Int = 10) {

    enum class StopRequest() {
        Play,
        Stop,
        Kill,
        Neutral
    }

    companion object {
        /*
            Not in use right now. a half-assed attempt to estimate a decent sample rate
         */
        fun calc_sample_rate(): Int {
            val target_nano = 70_000_000.toLong()
            val c = 6
            var factor = 0f

            for (i in 0 until c) {
                val start_ts = System.nanoTime()
                var x = 0
                for (j in 0 until 100_000_000) {
                    x += 1
                }

                val delta = max(System.nanoTime() - start_ts, target_nano)
                factor += target_nano.toFloat() / delta.toFloat()
            }

            factor /= c

            var output = (44100.toFloat() * factor).toInt()

            return max(output, 11025)
        }
    }

    var buffer_delay = 1
    internal var wave_generator = WaveGenerator(sample_handle_manager)
    internal var active_audio_track_handle: AudioTrackHandle? = null
    var frames_per_beat = 0

    var SAMPLE_RATE_NANO = sample_handle_manager.sample_rate.toFloat() / 1_000_000_000F
    var BUFFER_NANO = sample_handle_manager.buffer_size.toLong() * 1_000_000_000.toLong() / sample_handle_manager.sample_rate.toLong()
    var is_playing = false
    var is_stopping = false

    fun start_playback() {
        if (!this.is_playing && !this.is_stopping) {
            this.active_audio_track_handle = AudioTrackHandle(sample_handle_manager.sample_rate, sample_handle_manager.buffer_size)
            this._start_play_loop()
        }
    }

    private fun _start_play_loop() {
        /*
            This loop will attempt to play a chunk, and while it's playing, generate the next chunk.
            Any time the generate() call takes longer than the buffer takes to play and empties the queue,
                the number of chunks in queue is increased, and the chunk queue is refilled.
            Every time this happens is counted and considered in get_delay() call.
         */
        val audio_track_handle = this.active_audio_track_handle!!
        this.is_playing = true
        thread {
            var buffer_millis = this.BUFFER_NANO / 1_000_000
            var chunks = mutableListOf<ShortArray>()
            var building_chunks = true
            thread {
                while (this.is_playing) {
                    if (chunks.size >= 4) {
                        Thread.sleep(buffer_millis)
                        continue
                    }

                    val chunk = try {
                        this.wave_generator.generate(this.sample_handle_manager.buffer_size)
                    } catch (e: WaveGenerator.KilledException) {
                        break
                    } catch (e: WaveGenerator.DeadException) {
                        break
                    }

                    chunks.add(chunk)
                }
                building_chunks = false
                this.wave_generator.clear()
                this.is_stopping = true
            }

            audio_track_handle.play(object : AudioTrack.OnPlaybackPositionUpdateListener {
                var notification_index = 0
                init {
                    this@MidiPlaybackDevice.on_start()
                    //this@MidiPlaybackDevice.active_audio_track_handle?.offset_next_notification_position(
                    //    this@MidiPlaybackDevice.buffer_delay * this@MidiPlaybackDevice.sample_handle_manager.buffer_size
                    //)
                    this@MidiPlaybackDevice.on_beat(this.notification_index++)
                    this@MidiPlaybackDevice.active_audio_track_handle?.offset_next_notification_position(this@MidiPlaybackDevice.get_next_beat_delay() ?: 0)
                }
                override fun onMarkerReached(p0: AudioTrack?) {
                    this@MidiPlaybackDevice.on_beat(this.notification_index++)
                    this@MidiPlaybackDevice.active_audio_track_handle?.offset_next_notification_position(this@MidiPlaybackDevice.get_next_beat_delay() ?: 0)
                }
                override fun onPeriodicNotification(p0: AudioTrack?) { }
            })

            while (this.is_playing && (building_chunks || chunks.isNotEmpty())) {
                if (chunks.isEmpty()) {
                    Thread.sleep(buffer_millis / 2)
                } else {
                    audio_track_handle.write(chunks.removeFirst())
                }
            }

            this.on_stop()

            audio_track_handle.stop()
            this.is_stopping = false
            this.active_audio_track_handle = null
        }
    }

    private fun stop() {
        this.is_playing = false
        this.is_stopping = true
    }

    fun kill() {
        this.is_playing = false
        this.is_stopping = true
    }

    open fun get_next_beat_delay(): Int? {
        return this.frames_per_beat
    }

    open fun on_write() { }
    open fun on_start() { }
    open fun on_stop() { }
    open fun on_beat(i :Int) { }
}
