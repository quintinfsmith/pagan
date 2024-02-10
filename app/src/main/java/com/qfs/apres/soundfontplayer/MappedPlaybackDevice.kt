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

    var cached_chunks = HashMap<Int, ShortArray>()

    open fun on_buffer() { }
    open fun on_buffer_done() { }
    open fun on_start() { }
    open fun on_stop() { }
    open fun on_cancelled() { }
    open fun on_beat(i :Int) { }

    fun cache_chunk(start_frame: Int) {
        this.cached_chunks[start_frame] = try {
            this.wave_generator.generate()
        } catch (e: WaveGenerator.EmptyException) {
            ShortArray(this.buffer_size * 2) { 0 }
        }
    }

    fun decache_range(range: IntRange) {
        for (key in range.intersect(this.cached_chunks.keys)) {
            this.cached_chunks.remove(key)
        }
    }

    fun purge_wave_generator() {
        this.wave_generator.clear()
    }

    fun start_playback(start_frame: Int = 0, kill_frame: Int? = null) {
        if (!this.is_playing && this.active_audio_track_handle == null) {
            this.active_audio_track_handle = AudioTrackHandle(this.sample_rate, this.buffer_size)
            //this._start_play_loop(start_frame, kill_frame)
            this.new_play_loop(start_frame, kill_frame)
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

        thread {
            val buffer_millis = this.BUFFER_NANO / 1_000_000
            var chunk: ShortArray? = null
            var ts: Long = System.currentTimeMillis()
            var flag_dead = false
            val empty_chunk = ShortArray(this.buffer_size * 2) { 0 }

            while (this.is_playing) {
                ts = System.currentTimeMillis()
                if (chunk != null) {
                    audio_track_handle.write(chunk)
                }

                chunk = try {
                    this@MappedPlaybackDevice.wave_generator.generate()
                } catch (e: WaveGenerator.EmptyException) {
                    empty_chunk
                } catch (e: WaveGenerator.DeadException) {
                    flag_dead = true
                    break
                }

                val duration = System.currentTimeMillis() - ts
                val real_delay = buffer_millis - duration

                if (real_delay > 0) {
                    Thread.sleep(real_delay)
                }
            }

            // Delay while write finishes
            val duration = System.currentTimeMillis() - ts
            val real_delay = buffer_millis - duration

            if (real_delay > 0) {
                Thread.sleep(real_delay)
            }

            if (this.beat_frames.isEmpty() && flag_dead) {
                this.kill()
                this.on_stop()
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

                    if (this.has_beats && this.current_beat < that.beat_frames.size) {
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
                    val frame_delay = if (audio_track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                        audio_track.notificationMarkerPosition - audio_track.playbackHeadPosition
                    } else {
                        return this._stop(audio_track)
                    }

                    if (this.current_beat < that.beat_frames.size - 1) {
                        this.current_beat += 1
                    } else {
                        return this._stop(audio_track)
                    }

                    that.on_beat(this.current_beat)

                    that.active_audio_track_handle?.set_next_notification_position(
                        if (that.wave_generator.kill_frame != null) {
                            min(that.wave_generator.kill_frame!!, that.beat_frames[this.current_beat]) - start_frame
                        } else {
                            that.beat_frames[this.current_beat] - start_frame + frame_delay
                        }
                    )
                }

                override fun onPeriodicNotification(audio_track: AudioTrack?) {
                    // COMMENTED: Kill frame isn't redundant but it is when it's grabbed from the wave_generator
                    //val kill_frame = this@MappedPlaybackDevice.wave_generator.kill_frame
                    //if (kill_frame != null && audio_track != null) {
                    //    if ((kill_frame - start_frame) <= audio_track.playbackHeadPosition) {
                    //        this._stop(audio_track)
                    //    }
                    //}
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