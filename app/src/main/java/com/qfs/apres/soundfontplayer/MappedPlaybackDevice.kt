package com.qfs.apres.soundfontplayer

import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.min

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class MappedPlaybackDevice(var sample_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int, stereo_mode: WaveGenerator.StereoMode = WaveGenerator.StereoMode.Stereo) {
    internal var wave_generator = WaveGenerator(this.sample_frame_map, sample_rate, buffer_size, stereo_mode)
    internal var active_audio_track_handle: AudioTrackHandle? = null

    private var BUFFER_NANO = this.buffer_size.toLong() * 1_000_000_000.toLong() / this.sample_rate.toLong()
    var play_queued = false
    var is_playing = false
    var play_cancelled = false // need a away to cancel between parsing and playing
    val beat_frames = HashMap<Int, IntRange>()


    open fun on_buffer() { }
    open fun on_buffer_done() { }
    open fun on_start() { }
    open fun on_stop() { }
    open fun on_cancelled() { }
    open fun on_beat(i :Int) { }


    fun purge_wave_generator() {
        this.wave_generator.clear()
    }

    fun start_playback(start_frame: Int = 0) {
        if (!this.is_playing && this.active_audio_track_handle == null) {
            this.active_audio_track_handle = AudioTrackHandle(this.sample_rate, this.buffer_size)
            //this._start_play_loop(start_frame, kill_frame)
            this.play_loop(start_frame)
        }
    }

    fun in_playable_state(): Boolean {
        return !this.is_playing && this.active_audio_track_handle == null && !this.play_queued
    }

    private fun play_loop(start_frame: Int = 0) {
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
        this.wave_generator.set_position(start_frame, true)

        thread {
            val buffer_millis = this.BUFFER_NANO / 1_000_000
            var ts: Long = System.currentTimeMillis()
            var flag_dead = false
            val empty_chunk = FloatArray(this.buffer_size * 2) { 0f }
            var chunk: FloatArray? = null

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

               // val duration = System.currentTimeMillis() - ts
               // val real_delay = buffer_millis - duration
            }

            // Delay while write finishes
            val duration = System.currentTimeMillis() - ts
            val real_delay = buffer_millis - duration

            if (real_delay > 0) {
                Thread.sleep(real_delay)
            }

            this.kill()
            this.on_stop()
            this.wave_generator.clear()
        }

        try {
            if (this.play_cancelled) {
                throw java.lang.IllegalStateException()
            }

            audio_track_handle.play(object : AudioTrack.OnPlaybackPositionUpdateListener {
                var current_beat = -1
                var has_beats: Boolean
                init {
                    val that = this@MappedPlaybackDevice

                    if (that.play_cancelled) {
                        throw java.lang.IllegalStateException()
                    }

                    that.on_start()

                    this.has_beats = that.beat_frames.isNotEmpty()

                    if (this.has_beats) {
                        this.current_beat = that.beat_frames.keys.min()
                        for ((k, range) in that.beat_frames) {
                            if (range.contains(start_frame)) {
                                this.current_beat = k
                                break
                            }
                        }

                        //that.on_beat(this.current_beat)

                        that.active_audio_track_handle?.set_next_notification_position(
                            that.beat_frames[this.current_beat]!!.last - start_frame
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

                    this.current_beat += 1

                    if (this.current_beat > that.beat_frames.keys.max()) {
                        return this._stop(audio_track)
                    }

                    that.on_beat(this.current_beat)


                    that.active_audio_track_handle?.set_next_notification_position(
                        if (that.wave_generator.kill_frame != null) {
                            min(that.wave_generator.kill_frame!!, that.beat_frames[this.current_beat]!!.last) - start_frame
                        } else {
                            that.beat_frames[this.current_beat]!!.last - start_frame + frame_delay
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
        this.active_audio_track_handle = null
        this.play_cancelled = true
    }

    fun play(start_frame: Int = 0) {
        this.play_cancelled = false
        this.play_queued = true
        this.start_playback(start_frame)
    }

    fun set_kill_frame(frame: Int) {
        this.wave_generator.kill_frame = frame
    }

    fun setup_beat_frames() {
        this.beat_frames.clear()
        for ((k, v) in this.sample_frame_map.get_beat_frames()) {
            this.beat_frames[k] = v
        }
    }
}