package com.qfs.apres.soundfontplayer

import kotlinx.coroutines.sync.Mutex
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
    private var active_audio_track_handle: AudioTrackHandle? = null
    internal var stop_request = StopRequest.Neutral
    var SAMPLE_RATE_NANO = sample_handle_manager.sample_rate.toFloat() / 1_000_000_000F
    var BUFFER_NANO = sample_handle_manager.buffer_size.toLong() * 1_000_000_000.toLong() / sample_handle_manager.sample_rate.toLong()

    fun start_playback() {
        if (this.stop_request == StopRequest.Neutral) {
            this.stop_request = StopRequest.Play
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
        var chunk_mutex = Mutex()
        thread {
            this.wave_generator.timestamp = System.nanoTime()

            while (this.stop_request != StopRequest.Kill) {
                val chunk = try {
                    this@MidiPlaybackDevice.wave_generator.generate(this@MidiPlaybackDevice.sample_handle_manager.buffer_size)
                } catch (e: WaveGenerator.KilledException) {
                    break
                } catch (e: WaveGenerator.DeadException) {
                    break
                }

                audio_track_handle.write(chunk)
            }

            this@MidiPlaybackDevice.wave_generator.clear()
            audio_track_handle.stop()

            this.active_audio_track_handle = null
            this.stop_request = StopRequest.Neutral
            this.on_stop()
        }
        this.on_start()
    }

    private fun stop() {
        if (this.stop_request != StopRequest.Neutral) {
            this.stop_request = StopRequest.Stop
        }
    }

    fun kill() {
        if (this.stop_request != StopRequest.Neutral) {
            this.stop_request = StopRequest.Kill
        }
    }

    fun is_playing(): Boolean {
        return this.stop_request == StopRequest.Play
    }

    open fun on_start() { }
    open fun on_stop() { }
}
