package com.qfs.apres.soundfontplayer

import android.media.AudioTrack
import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOn79
import kotlin.concurrent.thread
import kotlin.math.min

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class FiniteMidiDevice(var sample_handle_manager: SampleHandleManager) {
    internal var wave_generator = WaveGenerator(sample_handle_manager)
    internal var active_audio_track_handle: AudioTrackHandle? = null

    var BUFFER_NANO = sample_handle_manager.buffer_size.toLong() * 1_000_000_000.toLong() / sample_handle_manager.sample_rate.toLong()
    var play_queued = false
    var is_playing = false
    var play_cancelled = false // need a away to cancel between parsing and playing
    var fill_buffer_cache = true

    var approximate_frame_count: Int = 0
    var beat_delays = mutableListOf<Int>()
    // precache 3 seconds when buffering
    val minimum_buffer_cache_size: Int = this.sample_handle_manager.sample_rate * 3 / this.sample_handle_manager.buffer_size
    // allow up to 1 minute to be cached during playback
    val buffer_cache_size_limit: Int = this.sample_handle_manager.sample_rate * 60 / this.sample_handle_manager.buffer_size

    fun start_playback() {
        if (!this.is_playing && this.active_audio_track_handle == null) {
            this.active_audio_track_handle = AudioTrackHandle(sample_handle_manager.sample_rate, sample_handle_manager.buffer_size)
            this._start_play_loop()
        }
    }

    fun in_playable_state(): Boolean {
        return !this.is_playing && this.active_audio_track_handle == null && !this.play_queued
    }

    private fun _start_play_loop() {
        /*
            This loop will attempt to play a chunk, and while it's playing, generate the next chunk.
            Any time the generate() call takes longer than the buffer takes to play and empties the queue,
                the number of chunks in queue is increased, and the chunk queue is refilled.
            Every time this happens is counted and considered in get_delay() call.
         */
        val audio_track_handle = this.active_audio_track_handle!!
        this.play_queued = false
        this.is_playing = true

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
                    var notification_index = 0
                    init {
                        if (this@FiniteMidiDevice.play_cancelled) {
                            throw java.lang.IllegalStateException()
                        } else {
                            this@FiniteMidiDevice.on_start()
                            this@FiniteMidiDevice.on_beat(this.notification_index)
                            this@FiniteMidiDevice.active_audio_track_handle?.offset_next_notification_position(
                                this@FiniteMidiDevice.pop_next_beat_delay() ?: 0
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
                        var frame_delay = if (audio_track != null) {
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

                        var next_beat_delay = 0
                        if (!kill_flag) {
                            while (frame_delay <= 0) {
                                val next_delay = this@FiniteMidiDevice.pop_next_beat_delay()

                                if (next_delay == null) {
                                    kill_flag = true
                                    break
                                }

                                next_beat_delay += next_delay
                                frame_delay += next_delay
                                this.notification_index += 1
                            }
                        }

                        if (kill_flag) {
                            if (audio_track?.state != AudioTrack.STATE_UNINITIALIZED) {
                                audio_track?.stop()
                            }
                            this@FiniteMidiDevice.active_audio_track_handle = null
                            this@FiniteMidiDevice.is_playing = false
                            this@FiniteMidiDevice.on_stop()
                        } else {
                            this@FiniteMidiDevice.on_beat(this.notification_index)
                            val target_next_position =
                                audio_track!!.notificationMarkerPosition + next_beat_delay
                            val next_position = if (final_frame != null) {
                                min(final_frame!!, target_next_position)
                            } else {
                                target_next_position
                            }

                            this@FiniteMidiDevice.active_audio_track_handle?.set_next_notification_position(
                                next_position
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

    open fun on_buffer() { }
    open fun on_buffer_done() { }
    open fun on_start() { }
    open fun on_stop() { }
    open fun on_cancelled() { }
    open fun on_beat(i :Int) { }

    internal fun parse_midi(midi: Midi) {
        this.beat_delays.clear()
        var start_frame = this.wave_generator.frame
        var ticks_per_beat = (500_000 / midi.get_ppqn())
        var frames_per_tick = (ticks_per_beat * this.sample_handle_manager.sample_rate) / 1_000_000
        var last_tick = 0
        for ((tick, events) in midi.get_all_events_grouped()) {
            last_tick = tick
            val tick_frame = (tick * frames_per_tick) + start_frame
            this.wave_generator.place_events(events, tick_frame)

            // Need to handle some functions so the sample handles are created before the playback
            // & Need to set Tempo
            for (event in events) {
                when (event) {
                    is ProgramChange -> {
                        this.sample_handle_manager.change_program(event.channel, event.get_program())
                    }
                    is BankSelect -> {
                        this.sample_handle_manager.select_bank(event.channel, event.value)
                    }
                    is NoteOn -> {
                        this.sample_handle_manager.gen_sample_handles(event)
                    }
                    is NoteOn79 -> {
                        this.sample_handle_manager.gen_sample_handles(event)
                    }
                    is SetTempo -> {
                        ticks_per_beat = (event.get_uspqn() / midi.get_ppqn())
                        frames_per_tick = (ticks_per_beat * this.sample_handle_manager.sample_rate) / 1_000_000
                    }
                    is SongPositionPointer -> {
                        this.beat_delays.add(midi.get_ppqn() * frames_per_tick)
                    }
                }
            }
        }

        val tick_frame = ((last_tick) * frames_per_tick) + start_frame
        this.approximate_frame_count = tick_frame
    }

    fun peek_next_beat_delay(): Int? {
        return if (this.beat_delays.isEmpty()) {
            null
        } else {
            this.beat_delays.removeFirst()
        }
    }

    fun pop_next_beat_delay(): Int? {
        return if (this.beat_delays.isEmpty()) {
            null
        } else {
            this.beat_delays.removeFirst()
        }
    }

    fun play_midi(midi: Midi) {
        this.play_cancelled = false
        this.play_queued = true
        this.parse_midi(midi)
        this.start_playback()
    }
}
