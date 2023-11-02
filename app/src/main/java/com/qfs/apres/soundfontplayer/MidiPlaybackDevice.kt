package com.qfs.apres.soundfontplayer

import kotlin.concurrent.thread
import kotlin.math.max

open class MidiPlaybackDevice(
        var sample_handle_manager: SampleHandleManager,
        // (in frames) how many x the buffer size should be delayed
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


    var buffer_delay = 0
    internal var wave_generator = WaveGenerator(sample_handle_manager)
    private var active_audio_track_handle: AudioTrackHandle? = null
    internal var stop_request = StopRequest.Neutral
    private var play_drift = 0
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
        thread {
            this.wave_generator.timestamp = System.nanoTime()
            val chunks: MutableList<Pair<ShortArray, List<Pair<Int, Int>>>> = mutableListOf()

            this.play_drift = 0
            var flag_writing = true
            var flag_no_more_chunks = false
            var pause_write = true

            thread {
                while (!flag_no_more_chunks && this.stop_request != StopRequest.Kill) {
                    if (chunks.size < this.cache_size_limit) {
                        try {
                            val chunk = this.wave_generator.generate(this.sample_handle_manager.buffer_size)
                            chunks.add(chunk)
                        } catch (e: WaveGenerator.KilledException) {
                            flag_no_more_chunks = true
                            break
                        } catch (e: WaveGenerator.DeadException) {
                            flag_no_more_chunks = true
                            break
                        }
                    } else if (!pause_write) {
                        val sleep = BUFFER_NANO  / 2
                        if (sleep > 0) {
                            Thread.sleep(sleep / 1_000_000, (sleep % 1_000_000).toInt())
                        }
                    } else {
                        pause_write = false
                    }
                }
            }

            while (flag_writing && this.stop_request != StopRequest.Kill) {
                val write_start_ts = System.nanoTime()
                if (!pause_write) {
                    if (chunks.isNotEmpty()) {
                        val (chunk, pointers) = chunks.removeAt(0)
                        thread {
                            for ((millis, beat) in pointers) {
                                thread {
                                    Thread.sleep(millis.toLong())
                                    this.on_beat_signal(beat)
                                }
                            }
                        }
                        audio_track_handle.write(chunk)
                    } else if (!flag_no_more_chunks) {
                        this.play_drift += (BUFFER_NANO / 1_000_000).toInt()
                        pause_write = true
                    } else {
                        flag_writing = false
                        break
                    }
                } else {
                    this.play_drift += (BUFFER_NANO / 1_000_000).toInt()
                }

                val delta = System.nanoTime() - write_start_ts
                val sleep = BUFFER_NANO - delta - 1
                if (sleep > 0) {
                    Thread.sleep(sleep / 1_000_000, (sleep % 1_000_000).toInt())
                }
            }

            audio_track_handle.stop()
            this.wave_generator.clear()
            this.active_audio_track_handle = null
            this.stop_request = StopRequest.Neutral
            this.play_drift = 0
            this.on_stop()
        }
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

    fun get_delay(): Long {
        return (((this.buffer_delay * this.sample_handle_manager.buffer_size).toLong() * 1_000.toLong()) / this.sample_handle_manager.sample_rate.toLong()) + this.play_drift
    }

    open fun on_stop() { }
    open fun on_beat_signal(beat: Int) { }
}
