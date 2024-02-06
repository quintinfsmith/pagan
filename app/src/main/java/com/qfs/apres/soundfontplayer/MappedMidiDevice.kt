package com.qfs.apres.soundfontplayer

import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.min

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class MappedMidiDevice(var sample_handle_manager: SampleHandleManager, var midi_frame_map: FrameMap) {
    internal var wave_generator = WaveGenerator(sample_handle_manager, this.midi_frame_map)
    internal var active_audio_track_handle: AudioTrackHandle? = null

    var BUFFER_NANO = sample_handle_manager.buffer_size.toLong() * 1_000_000_000.toLong() / sample_handle_manager.sample_rate.toLong()
    var play_queued = false
    var is_playing = false
    var play_cancelled = false // need a away to cancel between parsing and playing
    var fill_buffer_cache = true
    val beat_frames = mutableListOf<Int>()

    // precache 3 seconds when buffering
    val minimum_buffer_cache_size: Int = this.sample_handle_manager.sample_rate * 3 / this.sample_handle_manager.buffer_size
    // allow up to 1 minute to be cached during playback
    val buffer_cache_size_limit: Int = this.sample_handle_manager.sample_rate * 60 / this.sample_handle_manager.buffer_size

    open fun on_buffer() { }
    open fun on_buffer_done() { }
    open fun on_start() { }
    open fun on_stop() { }
    open fun on_cancelled() { }
    open fun on_beat(i :Int) { }

    fun cache_chunk(start_frame: Int) {
        this.wave_generator.cache_chunk(start_frame)
    }

    fun decache_range(range: IntRange) {
        this.wave_generator.decache_range(range)
    }

    fun start_playback(start_frame: Int = 0) {
        if (!this.is_playing && this.active_audio_track_handle == null) {
            this.active_audio_track_handle = AudioTrackHandle(sample_handle_manager.sample_rate, sample_handle_manager.buffer_size)
            this._start_play_loop(start_frame)
        }
    }

    fun in_playable_state(): Boolean {
        return !this.is_playing && this.active_audio_track_handle == null && !this.play_queued
    }

    private fun _start_play_loop(start_frame: Int = 0) {
        /*
            This loop will attempt to play a chunk, and while it's playing, generate the next chunk.
            Any time the generate() call takes longer than the buffer takes to play and empties the queue,
                the number of chunks in queue is increased, and the chunk queue is refilled.
            Every time this happens is counted and considered in get_delay() call.
         */
        val audio_track_handle = this.active_audio_track_handle!!
        this.play_queued = false
        this.is_playing = true

        this.setup_beat_frames()
        this.wave_generator.clear()
        this.wave_generator.frame = start_frame

        thread {
            val buffer_millis = this.BUFFER_NANO / 1_000_000
            val chunks = mutableListOf<ShortArray>()
            var building_chunks = true
            var final_frame: Int? = null
            val wait_delay = if (this.fill_buffer_cache) {
                buffer_millis / 10
            } else {
                buffer_millis
            }
            var fill_flagged = false

            thread {
                while (this.is_playing) {
                    if (chunks.size >= this.minimum_buffer_cache_size) {
                        if (fill_flagged) {
                            fill_flagged = false
                            this.on_buffer_done()
                        }
                        if (chunks.size >= this.buffer_cache_size_limit) {
                            Thread.sleep(wait_delay)
                            continue
                        }
                    }

                    val chunk = try {
                        this.wave_generator.generate(this.sample_handle_manager.buffer_size)
                    } catch (e: WaveGenerator.KilledException) {
                        break
                    } catch (e: WaveGenerator.EmptyException) {
                        ShortArray(this.sample_handle_manager.buffer_size * 2) { 0 }
                    } catch (e: WaveGenerator.DeadException) {
                        break
                    }

                    chunks.add(chunk)
                }

                final_frame = this.wave_generator.frame

                building_chunks = false
                this.wave_generator.clear()
            }

            try {
                if (this.play_cancelled) {
                    throw java.lang.IllegalStateException()
                }

                audio_track_handle.play(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    var current_beat = 0
                    init {
                        while (this.current_beat < this@MappedMidiDevice.beat_frames.size && this@MappedMidiDevice.beat_frames[this.current_beat] <= start_frame) {
                            this.current_beat += 1
                        }

                        if (this@MappedMidiDevice.play_cancelled) {
                            throw java.lang.IllegalStateException()
                        } else {
                            this@MappedMidiDevice.on_start()

                            this@MappedMidiDevice.on_beat(this.current_beat)
                            this@MappedMidiDevice.active_audio_track_handle?.set_next_notification_position(
                                this@MappedMidiDevice.beat_frames[this.current_beat] - start_frame
                            )
                        }
                    }

                    override fun onMarkerReached(audio_track: AudioTrack?) {
                        var kill_flag = false
                        /*
                         On Slower devices, the MarkerReached Callback can take a bit to fire,
                          Therefore we need to try to compensate for that and check the position it was
                          fired at vs the current position
                         */
                        val frame_delay = if (audio_track != null) {
                            if (audio_track.playState == AudioTrack.PLAYSTATE_STOPPED) {
                                kill_flag = true
                                0
                            } else {
                                audio_track.notificationMarkerPosition - audio_track.playbackHeadPosition
                            }
                        } else {
                            kill_flag = true
                            0
                        }

                        if (!kill_flag) {
                            if (this.current_beat < this@MappedMidiDevice.beat_frames.size - 1) {
                                this.current_beat += 1
                            } else {
                                kill_flag = true
                            }
                        }

                        if (kill_flag) {
                            if (audio_track?.state != AudioTrack.STATE_UNINITIALIZED) {
                                audio_track?.stop()
                            }
                            this@MappedMidiDevice.active_audio_track_handle = null
                            this@MappedMidiDevice.is_playing = false
                            this@MappedMidiDevice.on_stop()
                        } else {
                            this@MappedMidiDevice.on_beat(this.current_beat)

                            val next_position = if (final_frame != null) {
                                min(final_frame!!, this@MappedMidiDevice.beat_frames[this.current_beat])
                            } else {
                                this@MappedMidiDevice.beat_frames[this.current_beat]
                            }

                            this@MappedMidiDevice.active_audio_track_handle?.set_next_notification_position(
                                next_position - start_frame + frame_delay
                            )
                        }
                    }
                    override fun onPeriodicNotification(p0: AudioTrack?) {}
                })
            } catch (e: IllegalStateException) {
                this.play_cancelled = false
                this.is_playing = false
                this.on_cancelled()
            }

            while (building_chunks || chunks.isNotEmpty()) {
                if (chunks.isEmpty()) {
                    if (!fill_flagged) {
                        fill_flagged = true
                        if (this.is_playing) {
                            this.on_buffer()
                        }
                    }
                    Thread.sleep(wait_delay)
                } else if (fill_flagged && building_chunks) {
                    Thread.sleep(wait_delay)
                } else {
                    val chunk = chunks.removeFirst()
                    audio_track_handle.write(chunk)
                }
            }
        }
    }

    fun kill() {
        this.is_playing = false
        this.active_audio_track_handle?.stop()
        this.on_stop()
        this.active_audio_track_handle = null
        this.play_cancelled = true
    }

    fun play(start_frame: Int = 0) {
        this.play_cancelled = false
        this.play_queued = true
        this.start_playback(start_frame)
    }

    fun pop_next_beat_delay(): Int? {
        return if (this.beat_frames.isEmpty()) {
            null
        } else {
            this.beat_frames.removeFirst()
        }
    }

    fun setup_beat_frames() {
        this.beat_frames.clear()
        this.beat_frames.addAll(this.midi_frame_map.get_beat_frames())
    }

}