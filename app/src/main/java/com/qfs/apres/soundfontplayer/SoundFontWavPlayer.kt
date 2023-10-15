package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi
import com.qfs.apres.VirtualMidiDevice
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStart
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.SoundFont
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.IntBuffer
import kotlin.concurrent.thread
import kotlin.math.max

class SoundFontWavPlayer(private var sound_font: SoundFont): VirtualMidiDevice() {
    companion object {
        val SAMPLE_RATE_NANO = AudioTrackHandle.sample_rate.toFloat() / 1_000_000_000F
    }
    class WaveGenerator(private var player: SoundFontWavPlayer) {
        class KilledException: Exception()
        class DeadException: Exception()

        var frame = 0
        var timestamp: Long = System.nanoTime()
        private var active_sample_handles = HashMap<Pair<Int, Int>, MutableSet<SampleHandle>>()
        private var midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
        var max_frame = 0
        var event_mutex = Mutex()

        fun process_event(event: MIDIEvent) {
            val delta_nano = (System.nanoTime() - this.timestamp).toFloat()
            val frame = (SAMPLE_RATE_NANO * delta_nano).toInt() + (AudioTrackHandle.buffer_size * 2)

            runBlocking {
                this@WaveGenerator.event_mutex.withLock {
                    if (!this@WaveGenerator.midi_events_by_frame.containsKey(frame)) {
                        this@WaveGenerator.midi_events_by_frame[frame] = mutableListOf()
                    }
                    this@WaveGenerator.midi_events_by_frame[frame]!!.add(event)
                }
            }
        }

        fun generate(): ShortArray {
            val initial_array = IntArray(AudioTrackHandle.buffer_size * 2)
            val buffer = IntBuffer.wrap(initial_array)

            var max_frame_value = 0

            for (i in 0 until AudioTrackHandle.buffer_size) {
                var left_frame = 0
                var right_frame = 0
                val f = this.frame + i

                if (this.player.stop_request == StopRequest.Kill) {
                    for ((_, handles) in this.active_sample_handles) {
                        for (handle in handles) {
                            handle.release_note()
                        }
                    }
                    throw KilledException()
                } else if (this.midi_events_by_frame.containsKey(f)) {
                    for (event in this.midi_events_by_frame[f]!!) {
                        when (event) {
                            is NoteOn -> {
                                if (this.player.stop_request == StopRequest.Play) {
                                    val preset = this.get_preset(event.channel) ?: continue
                                    this.active_sample_handles[Pair(
                                        event.channel,
                                        event.note
                                    )] =
                                        this.player.gen_sample_handles(event, preset)
                                            .toMutableSet()
                                }
                            }

                            is NoteOff -> {
                                for (handle in this.active_sample_handles[Pair(
                                    event.channel,
                                    event.note
                                )] ?: continue) {
                                    handle.release_note()
                                }
                            }
                        }
                    }
                }

                runBlocking {
                    this@WaveGenerator.event_mutex.withLock {
                        this@WaveGenerator.midi_events_by_frame.remove(f)
                    }
                }

                val keys_to_pop = mutableSetOf<Pair<Int, Int>>()
                var overlap = 0
                for ((key, sample_handles) in this.active_sample_handles) {
                    overlap += 1
                    val to_kill = mutableSetOf<SampleHandle>()
                    for (sample_handle in sample_handles) {
                        val frame_value = sample_handle.get_next_frame()
                        if (frame_value == null) {
                            to_kill.add(sample_handle)
                            continue
                        }

                        // TODO: Implement ROM stereo modes
                        val pan = sample_handle.pan
                        when (sample_handle.stereo_mode and 7) {
                            1 -> { // mono
                                if (pan > 0) {
                                    left_frame += frame_value
                                    right_frame += (frame_value.toDouble() * (100 - pan) / 100.0).toInt()
                                        .toShort()
                                } else if (pan < 0) {
                                    left_frame += (frame_value.toDouble() * (100 + pan) / 100.0).toInt()
                                        .toShort()
                                    right_frame += frame_value
                                } else {
                                    left_frame += frame_value
                                    right_frame += frame_value
                                }
                            }

                            2 -> { // right
                                right_frame += if (pan > 0) {
                                    (frame_value.toDouble() * (100 - pan) / 100.0).toInt()
                                        .toShort()
                                } else {
                                    frame_value
                                }
                            }

                            4 -> { // left
                                left_frame += if (pan < 0) {
                                    (frame_value.toDouble() * (100 + pan) / 100.0).toInt()
                                        .toShort()
                                } else {
                                    frame_value
                                }
                            }

                            else -> {}
                        }
                    }

                    for (sample_handle in to_kill) {
                        this.active_sample_handles[key]!!.remove(sample_handle)
                    }
                    if (this.active_sample_handles[key]!!.isEmpty()) {
                        keys_to_pop.add(key)
                    }
                }

                for (key in keys_to_pop) {
                    this.active_sample_handles.remove(key)
                }

                max_frame_value = max(
                    max_frame_value,
                    kotlin.math.abs(right_frame)
                )

                max_frame_value = max(
                    max_frame_value,
                    kotlin.math.abs(left_frame)
                )

                buffer.put(right_frame)
                buffer.put(left_frame)
            }

            val mid = Short.MAX_VALUE / 2
            val compression_ratio = if (max_frame_value <= Short.MAX_VALUE) {
                1F
            } else {
                (Short.MAX_VALUE - mid).toFloat() / (max_frame_value - mid).toFloat()
            }

            val compressed_array = ShortArray(initial_array.size) { i: Int ->
                val v = initial_array[i]
                if (compression_ratio >= 1F || (0 - mid .. mid).contains(v)) {
                    v.toShort()
                } else if (v > mid) {
                    (mid + ((v - mid).toFloat() * compression_ratio).toInt()).toShort()
                } else {
                    (((v + mid).toFloat() * compression_ratio).toInt() - mid).toShort()
                }
            }

            this.frame += AudioTrackHandle.buffer_size

            if (this.max_frame > 0 && this.frame >= this.max_frame && this.active_sample_handles.isEmpty()) {
                throw DeadException()
            }

            return compressed_array
        }

        private fun get_preset(channel: Int): Preset? {
            return this.player.get_preset(channel)
        }
    }

    enum class StopRequest() {
        Play,
        Stop,
        Kill,
        Neutral
    }

    private var active_wave_generator: WaveGenerator? = null
    private var active_audio_track_handle: AudioTrackHandle? = null
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()
    private val sample_handle_generator = SampleHandleGenerator()
    private var stop_request = StopRequest.Neutral

    override fun onNoteOn(event: NoteOn) {
        this.start_playback() // Only starts if not already started
        this.active_wave_generator?.process_event(event) ?: return
    }

    override fun onNoteOff(event: NoteOff) {
        this.active_wave_generator?.process_event(event) ?: return
    }

    override fun onMIDIStop(event: MIDIStop) {
        this.stop()
        this.active_wave_generator?.process_event(event) ?: return
    }

    override fun onMIDIStart(event: MIDIStart) {
        this.start_playback()
    }

    override fun onBankSelect(event: BankSelect) {
        val channel = event.channel
        val bank = event.value
        // NOTE: Changing the bank doesn't trigger a preset change
        // That occurs in change_program()
        val program = if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!.second
        } else {
            0
        }
        this.preset_channel_map[channel] = Pair(bank, program)
    }

    override fun onAllSoundOff(event: AllSoundOff) {
        this.kill()
    }

    override fun onProgramChange(event: ProgramChange) {
        val channel = event.channel
        val program = event.program

        val bank = if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!.first
        } else {
            0
        }

        val key = Pair(bank, program)
        if (this.loaded_presets[key] == null) {
            this.loaded_presets[key] = try {
                this.sound_font.get_preset(program, bank)
            } catch (e: SoundFont.InvalidPresetIndex) {
                if (channel == 9) {
                    if (Pair(bank, 0) in this.loaded_presets) {
                        this.loaded_presets[Pair(bank, 0)]!!
                    } else {
                        return
                    }
                } else {
                    if (Pair(0, program) in this.loaded_presets) {
                        this.loaded_presets[Pair(0, program)]!!
                    } else if (Pair(0, 0) in this.loaded_presets) {
                        this.loaded_presets[Pair(0, 0)]!!
                    } else {
                        return
                    }
                }
            }
        }

        this.preset_channel_map[channel] = key
        this.decache_unused_presets()
    }

    fun start_playback() {
        if (this.active_wave_generator == null && this.stop_request == StopRequest.Neutral) {
            this.stop_request = StopRequest.Play
            val audio_track_handle = AudioTrackHandle()
            this.active_audio_track_handle = audio_track_handle
            this.active_wave_generator = WaveGenerator(this)

            this._start_play_loop()
        }
    }

    private fun gen_sample_handles(event: NoteOn, preset: Preset): Set<SampleHandle> {
        val output = mutableSetOf<SampleHandle>()
        val potential_instruments = preset.get_instruments(event.note, event.velocity)

        for (p_instrument in potential_instruments) {
            val samples = p_instrument.instrument!!.get_samples(
                event.note,
                event.velocity
            ).toList()

            for (sample in samples) {
                val new_handle = this.sample_handle_generator.get(event, sample, p_instrument, preset)
                new_handle.current_volume = (event.velocity.toDouble() / 128.toDouble()) * SampleHandle.MAXIMUM_VOLUME
                output.add( new_handle )
            }
        }

        return output
    }

    private fun get_preset(channel: Int): Preset? {
        return this.loaded_presets[this.get_channel_preset(channel)]
    }

    private fun get_channel_preset(channel: Int): Pair<Int, Int> {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else if (channel == 9) {
            Pair(128, 0)
        } else {
            Pair(0,0)
        }
    }

    private fun decache_unused_presets() {
        val loaded_preset_keys = this.loaded_presets.keys.toMutableSet()
        for ((_, key) in this.preset_channel_map) {
            if (loaded_preset_keys.contains(key)) {
                loaded_preset_keys.remove(key)
            }
        }

        for (key in loaded_preset_keys) {
            val preset = this.loaded_presets[key]!!
            this.sample_handle_generator.decache_sample_data(preset)
            this.loaded_presets.remove(key)
        }
    }

    private fun _start_play_loop() {
        val buffer_duration = AudioTrackHandle.buffer_size.toLong() * 1000.toLong() / AudioTrackHandle.sample_rate.toLong()
        val audio_track_handle = this.active_audio_track_handle!!
        val wave_generator = this.active_wave_generator!!

        this.active_wave_generator?.timestamp = System.nanoTime()
        // TODO: At the moment, an array of chunks is unnecessary, but I'd like to have a variable
        //  delay for pieces with more lines/devices with less power
        val chunks: MutableList<ShortArray> = mutableListOf( )

        audio_track_handle.play()
        thread {
            while (true) {
                val write_start_ts = System.currentTimeMillis()
                if (chunks.isNotEmpty()) {
                    val chunk = chunks.removeAt(0)
                    audio_track_handle.write(chunk)
                }

                try {
                    chunks.add(wave_generator.generate())
                } catch (e: WaveGenerator.KilledException) {
                    break
                } catch (e: WaveGenerator.DeadException) {
                    break
                }

                val delta = System.currentTimeMillis() - write_start_ts
                val sleep = buffer_duration - 10 - delta
                if (sleep > 0) {
                    Thread.sleep(sleep)
                }
            }

            audio_track_handle.stop()

            this.active_wave_generator = null
            this.active_audio_track_handle = null
            this.stop_request = StopRequest.Neutral
        }
    }

    private fun stop() {
        this.stop_request = StopRequest.Stop
    }

    private fun kill() {
        this.stop_request = StopRequest.Kill
    }

    fun listening(): Boolean {
        return this.stop_request == StopRequest.Play
    }
}

