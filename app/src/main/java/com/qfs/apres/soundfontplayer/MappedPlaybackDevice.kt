package com.qfs.apres.soundfontplayer

import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.min

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class MappedPlaybackDevice(var sample_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int) {
    internal var wave_generator = WaveGenerator(this.sample_frame_map, sample_rate, buffer_size)
    internal var active_audio_track_handle: AudioTrackHandle? = null

    var BUFFER_NANO = this.buffer_size.toLong() * 1_000_000_000.toLong() / this.sample_rate.toLong()
    var play_queued = false
    var is_playing = false
    var play_cancelled = false // need a away to cancel between parsing and playing
    var fill_buffer_cache = true
    val beat_frames = mutableListOf<Int>()

    // precache 3 seconds when buffering
    var minimum_buffer_cache_size: Int = this.sample_rate * 3 / this.buffer_size
    // allow up to 1 minute to be cached during playback
    var buffer_cache_size_limit: Int = this.sample_rate * 60 / this.buffer_size

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

    fun purge_wave_generator() {
        this.wave_generator.clear()
    }

    fun start_playback(start_frame: Int = 0, kill_frame: Int? = null) {
        if (!this.is_playing && this.active_audio_track_handle == null) {
            this.active_audio_track_handle = AudioTrackHandle(this.sample_rate, this.buffer_size)
            this._start_play_loop(start_frame, kill_frame)
        }
    }

    fun in_playable_state(): Boolean {
        return !this.is_playing && this.active_audio_track_handle == null && !this.play_queued
    }

    private fun new_play_loop(start_frame: Int = 0, kill_frame: Int? = null) {
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
        if (kill_frame != null) {
            this.wave_generator.kill_frame = kill_frame
        }
        thread {
            val buffer_millis = this.BUFFER_NANO / 1_000_000
            val wait_delay = if (this.fill_buffer_cache) {
                buffer_millis / 10
            } else {
                buffer_millis
            }
            while (this.is_playing) {
                audio_track_handle.write(
                    try {
                        this.wave_generator.generate(this.buffer_size)
                    } catch (e: WaveGenerator.KilledException) {
                        break
                    } catch (e: WaveGenerator.EmptyException) {
                        ShortArray(this.buffer_size * 2) { 0 }
                    } catch (e: WaveGenerator.DeadException) {
                        break
                    }
                )
            }
            this.wave_generator.clear()
        }

        try {
            if (this.play_cancelled) {
                throw java.lang.IllegalStateException()
            }

            audio_track_handle.play(object : AudioTrack.OnPlaybackPositionUpdateListener {
                var current_beat = 0
                var has_beats: Boolean
                init {
                    val that = this@MappedPlaybackDevice

                    if (that.play_cancelled) {
                        throw java.lang.IllegalStateException()
                    }

                    this.has_beats = that.beat_frames.isNotEmpty()

                    if (this.has_beats) {
                        while (this.current_beat < that.beat_frames.size && that.beat_frames[this.current_beat++] <= start_frame) { }
                    }

                    that.on_start()

                    if (this.has_beats) {
                        that.on_beat(this.current_beat)
                        that.active_audio_track_handle?.set_next_notification_position(
                            that.beat_frames[this.current_beat] - start_frame
                        )
                    }
                }

                override fun onMarkerReached(audio_track: AudioTrack?) {
                    if (audio_track == null) {
                        return
                    }
                    val that = this@MappedPlaybackDevice

                    /*
                     On Slower devices, the MarkerReached Callback can take a bit to fire,
                      Therefore we need to try to compensate for that and check the position it was
                      fired at vs the current position
                     */
                    val frame_delay = if (audio_track.playState == AudioTrack.PLAYSTATE_STOPPED) {
                        return this._stop(audio_track)
                    } else {
                        audio_track.notificationMarkerPosition - audio_track.playbackHeadPosition
                    }

                    if (this.current_beat < that.beat_frames.size - 1) {
                        this.current_beat += 1
                    } else {
                        return this._stop(audio_track)
                    }

                    that.on_beat(this.current_beat)

                    that.active_audio_track_handle?.set_next_notification_position(
                        if (kill_frame != null) {
                            min(kill_frame!!, that.beat_frames[this.current_beat]) - start_frame
                        } else {
                            that.beat_frames[this.current_beat] - start_frame + frame_delay
                        }
                    )
                }

                override fun onPeriodicNotification(audio_track: AudioTrack?) {
                    if (kill_frame != null && audio_track != null) {
                        if ((kill_frame - start_frame) <= audio_track.playbackHeadPosition) {
                            this._stop(audio_track)
                        }
                    }
                }

                private fun _stop(audio_track: AudioTrack?) {
                    val that = this@MappedPlaybackDevice
                    if (audio_track?.state != AudioTrack.STATE_UNINITIALIZED) {
                        audio_track?.stop()
                    }
                    that.active_audio_track_handle = null
                    that.is_playing = false
                    that.on_stop()
                }

            })

        } catch (e: IllegalStateException) {
            this.play_cancelled = false
            this.is_playing = false
            this.on_cancelled()
        }

        if (this.beat_frames.isEmpty()) {
            this.kill()
        }

    }

    private fun _start_play_loop(start_frame: Int = 0, kill_frame: Int? = null) {
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
        if (kill_frame != null) {
            this.wave_generator.kill_frame = kill_frame
        }

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

                    val start_ts = System.currentTimeMillis()
                    val chunk = try {
                        this.wave_generator.generate(this.buffer_size)
                    } catch (e: WaveGenerator.KilledException) {
                        break
                    } catch (e: WaveGenerator.EmptyException) {
                        ShortArray(this.buffer_size * 2) { 0 }
                    } catch (e: WaveGenerator.DeadException) {
                        break
                    }

                    chunks.add(chunk)
                    if (this.fill_buffer_cache) {
                        val delay = wait_delay - (System.currentTimeMillis() - start_ts)
                        if (delay > 0) {
                            Thread.sleep(delay)
                        }
                    }
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
                        if (this@MappedPlaybackDevice.beat_frames.isNotEmpty()) {
                            while (this.current_beat < this@MappedPlaybackDevice.beat_frames.size && this@MappedPlaybackDevice.beat_frames[this.current_beat] <= start_frame) {
                                this.current_beat += 1
                            }
                        }

                        if (this@MappedPlaybackDevice.play_cancelled) {
                            throw java.lang.IllegalStateException()
                        } else {
                            this@MappedPlaybackDevice.on_start()
                            if (this@MappedPlaybackDevice.beat_frames.isNotEmpty()) {
                                this@MappedPlaybackDevice.on_beat(this.current_beat)
                                this@MappedPlaybackDevice.active_audio_track_handle?.set_next_notification_position(
                                    this@MappedPlaybackDevice.beat_frames[this.current_beat] - start_frame
                                )
                            }
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
                            if (this.current_beat < this@MappedPlaybackDevice.beat_frames.size - 1) {
                                this.current_beat += 1
                            } else {
                                kill_flag = true
                            }
                        }

                        if (kill_flag) {
                            if (audio_track?.state != AudioTrack.STATE_UNINITIALIZED) {
                                audio_track?.stop()
                            }
                            this@MappedPlaybackDevice.active_audio_track_handle = null
                            this@MappedPlaybackDevice.is_playing = false
                            this@MappedPlaybackDevice.on_stop()
                        } else {
                            this@MappedPlaybackDevice.on_beat(this.current_beat)

                            val next_position = if (final_frame != null) {
                                min(final_frame!!, this@MappedPlaybackDevice.beat_frames[this.current_beat])
                            } else {
                                this@MappedPlaybackDevice.beat_frames[this.current_beat]
                            }

                            this@MappedPlaybackDevice.active_audio_track_handle?.set_next_notification_position(
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

            if (this.beat_frames.isEmpty()) {
                this.kill()
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

    fun play(start_frame: Int = 0, kill_frame: Int? = null) {
        this.play_cancelled = false
        this.play_queued = true
        this.start_playback(start_frame, kill_frame)
    }

    fun pop_next_beat_delay(): Int? {
        return if (this.beat_frames.isEmpty()) {
            null
        } else {
            this.beat_frames.removeFirst()
        }
    }

    fun set_kill_frame(frame: Int) {
        this.wave_generator.kill_frame = frame
    }

    fun setup_beat_frames() {
        this.beat_frames.clear()
        this.beat_frames.addAll(this.sample_frame_map.get_beat_frames())
    }

}