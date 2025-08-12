package com.qfs.apres.soundfontplayer

import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.min

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
abstract class MappedPlaybackDevice(var sample_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int, stereo_mode: WaveGenerator.StereoMode = WaveGenerator.StereoMode.Stereo) {
    internal var wave_generator = WaveGenerator(this.sample_frame_map, this.sample_rate, this.buffer_size, stereo_mode)
    internal var active_audio_track_handle: AudioTrackHandle? = null

    private var BUFFER_NANO = this.buffer_size.toLong() * 1_000_000_000.toLong() / this.sample_rate.toLong()
    var play_queued = false
    var is_playing = false
    var play_cancelled = false // need a away to cancel between parsing and playing


    abstract fun on_buffer()
    abstract fun on_buffer_done()
    abstract fun on_start()
    abstract fun on_stop()
    abstract fun on_cancelled()
    abstract fun on_mark(i :Int)


    fun purge_wave_generator() {
        this.wave_generator.clear()
    }

    fun start_playback(start_frame: Int = 0) {
        if (!this.is_playing && this.active_audio_track_handle == null) {
            this.active_audio_track_handle = AudioTrackHandle(this.sample_rate, this.buffer_size)
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
        this.wave_generator.set_position(start_frame, true)
        thread {
            val buffer_millis = this.BUFFER_NANO / 1_000_000
            var ts: Long = System.currentTimeMillis()
            val empty_chunk = FloatArray(this.buffer_size * 2)
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
                    break
                }
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

            this.on_start()

            audio_track_handle.play(object : AudioTrack.OnPlaybackPositionUpdateListener {
                var current_mark_index = 0
                init {
                    val track_handle = this@MappedPlaybackDevice.active_audio_track_handle
                    this@MappedPlaybackDevice.sample_frame_map.get_marked_frame(0)?.let { _ ->
                        var i = 0
                        var beat_frame = -1
                        while (true) {
                            beat_frame = this@MappedPlaybackDevice.sample_frame_map.get_marked_frame(i) ?: break
                            if (beat_frame >= start_frame) {
                                break
                            }
                            i++
                        }

                        this.current_mark_index = i

                        if (beat_frame == start_frame) {
                            this@MappedPlaybackDevice.on_mark(this.current_mark_index++)
                        }

                        this@MappedPlaybackDevice.sample_frame_map.get_marked_frame(this.current_mark_index)?.let { next_frame ->
                            track_handle?.set_next_notification_position(
                                next_frame - start_frame
                            )
                        }
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

                    if (that.wave_generator.kill_frame == null) {
                        while (true) {
                            val beat_frame = this@MappedPlaybackDevice.sample_frame_map.get_marked_frame(this.current_mark_index) ?: break
                            if (beat_frame - start_frame + frame_delay >= 1) {
                                break
                            }
                            this.current_mark_index += 1
                        }

                        if (this@MappedPlaybackDevice.sample_frame_map.get_marked_frame(this.current_mark_index) == null) {
                            return
                        }
                    }


                    that.on_mark(this.current_mark_index++)

                    val frame = this@MappedPlaybackDevice.sample_frame_map.get_marked_frame(this.current_mark_index) ?: return

                    that.active_audio_track_handle?.set_next_notification_position(
                        if (that.wave_generator.kill_frame != null) {
                            min(that.wave_generator.kill_frame!!, frame) - start_frame
                        } else {
                            frame - start_frame + frame_delay
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

    fun destroy() {
        this.wave_generator.destroy()
    }
}
