package com.qfs.apres.soundfontplayer

import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStart
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlin.concurrent.thread

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
class ActiveMidiAudioPlayer(var sample_handle_manager: SampleHandleManager): VirtualMidiOutputDevice {
    internal var active_audio_track_handle: AudioTrackHandle? = null
    internal var wave_generator = WaveGenerator(sample_handle_manager)
    var SAMPLE_RATE_MILLIS = sample_handle_manager.sample_rate.toFloat() / 1_000F
    var buffer_delay = 2
    var is_playing = false
    var generate_timestamp: Long? = null

    override fun onNoteOn79(event: NoteOn79) {
        this.process_event(event)
        this.start_playback() // Only starts if not already started
    }
    override fun onNoteOn(event: NoteOn) {
        this.process_event(event)
        this.start_playback() // Only starts if not already started
    }

    override fun onNoteOff79(event: NoteOff79) {
        if (! this.is_playing) {
            return
        }
        this.process_event(event)
    }
    override fun onNoteOff(event: NoteOff) {
        if (! this.is_playing) {
            return
        }
        this.process_event(event)
    }

    override fun onMIDIStop(event: MIDIStop) {
        if (! this.is_playing) {
            return
        }
        this.kill()
        this.process_event(event)
    }

    override fun onMIDIStart(event: MIDIStart) {
        this.start_playback()
    }

    override fun onBankSelect(event: BankSelect) {
        this.process_event(event)
    }

    override fun onAllSoundOff(event: AllSoundOff) {
        this.process_event(event)
    }

    override fun onProgramChange(event: ProgramChange) {
        this.process_event(event)
    }

    private fun process_event(event: MIDIEvent) {
        val now = System.currentTimeMillis()
        var gts = this.generate_timestamp ?: now
        val delta = (now - gts).toFloat()
        val frame = (this.SAMPLE_RATE_MILLIS * delta).toInt() + (this.sample_handle_manager.buffer_size * this.buffer_delay)

        this.wave_generator.place_event(event, frame)
    }

    fun start_playback() {
        if (this.in_playable_state()) {
            this.active_audio_track_handle = AudioTrackHandle(sample_handle_manager.sample_rate, sample_handle_manager.buffer_size)
            this._start_play_loop()
        }
    }

    fun in_playable_state(): Boolean {
        return !this.is_playing && this.active_audio_track_handle == null
    }

    fun _start_play_loop() {
        this.is_playing = true
        thread {
            this.active_audio_track_handle = AudioTrackHandle(
                this.sample_handle_manager.sample_rate,
                this.sample_handle_manager.buffer_size
            )

            this.active_audio_track_handle?.play()

            this.generate_timestamp = System.currentTimeMillis()
            while (this.is_playing) {
                val chunk = try {
                    this.wave_generator.generate()
                } catch (e: WaveGenerator.EmptyException) {
                    ShortArray(this.sample_handle_manager.buffer_size * 2) { 0 }
                } catch (e: WaveGenerator.DeadException) {
                    break
                } catch (e: WaveGenerator.KilledException) {
                    break
                }
                this.active_audio_track_handle?.write(chunk)
            }
            this.generate_timestamp = null

            this.is_playing = false
            this.active_audio_track_handle?.stop()
            this.active_audio_track_handle = null
            this.wave_generator.clear()
        }
    }

    private fun stop() {
        this.active_audio_track_handle?.pause()
        this.is_playing = false
    }

    fun kill() {
        this.active_audio_track_handle?.pause()
        this.is_playing = false
    }
}
