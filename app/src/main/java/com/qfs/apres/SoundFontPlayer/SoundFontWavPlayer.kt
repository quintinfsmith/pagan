package com.qfs.apres.SoundFontPlayer

import com.qfs.apres.BankSelect
import com.qfs.apres.MIDI
import com.qfs.apres.MIDIEvent
import com.qfs.apres.NoteOff
import com.qfs.apres.NoteOn
import com.qfs.apres.Preset
import com.qfs.apres.ProgramChange
import com.qfs.apres.SetTempo
import com.qfs.apres.SoundFont
import com.qfs.apres.VirtualMIDIDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ShortBuffer
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class SoundFontWavPlayer(var sound_font: SoundFont) {
    class PlaybackInterface() {
        var playing = true
        var stop_forced = false
        fun stop() {
            this.stop_forced = true
        }
    }
    class WaveGenerator(var player: SoundFontWavPlayer) {
        var frame = 0
        var active_sample_handles = HashMap<Pair<Int, Int>, MutableSet<SampleHandle>>()
        var midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
        var max_frame = 0
        var is_alive = true
        private var generate_ts: Long? = null

         fun parse_midi(midi: MIDI) {
            var frames_per_tick = ((500000 / midi.get_ppqn()) * AudioTrackHandle.sample_rate) / 1000000
            for ((tick, events) in midi.get_all_events_grouped()) {
                val tick_frame = (tick * frames_per_tick)
                this.midi_events_by_frame[tick_frame] = events.toMutableList()

                // Need to set Tempo
                for (event in events) {
                    when (event) {
                        is SetTempo -> {
                            frames_per_tick = ((event.get_uspqn() / midi.get_ppqn()) * AudioTrackHandle.sample_rate) / 1000000
                        }
                        is BankSelect -> {
                            this.player.select_bank(event.channel, event.value)
                        }
                        is ProgramChange -> {
                            this.player.change_program(event.channel, event.program)
                        }
                    }
                }
                //this.max_frame = max(tick_frame + AudioTrackHandle.sample_rate, this.max_frame)
                this.max_frame = max(tick_frame , this.max_frame)
            }
        }

        fun generate(): ShortArray {
            val buffer_array = ShortArray(AudioTrackHandle.buffer_size * 2)
            val buffer = ShortBuffer.wrap(buffer_array)
            this.generate_ts = System.currentTimeMillis()


            for (i in 0 until AudioTrackHandle.buffer_size) {
                var left_frame = 0
                var right_frame = 0
                val f = this.frame + i
                if (this.midi_events_by_frame.containsKey(f)) {
                    for (event in this.midi_events_by_frame[f]!!) {
                        when (event) {
                            is NoteOn -> {
                                val preset = this.get_preset(event.channel)
                                this.active_sample_handles[Pair(event.channel, event.note)] = this.player.gen_sample_handles(event, preset).toMutableSet()
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
                this.midi_events_by_frame.remove(f)

                val keys_to_pop = mutableSetOf<Pair<Int, Int>>()
                var overlap = 0
                for ((key, sample_handles) in this.active_sample_handles) {
                    overlap += 1
                    val to_kill = mutableSetOf<SampleHandle>()
                    for (sample_handle in sample_handles) {
                        // TODO: remove from active_sample_handles
                        val frame_value = sample_handle.get_next_frame()
                        if (frame_value == null) {
                            to_kill.add(sample_handle)
                            continue
                        }

                        // TODO: Implement ROM stereo modes
                        when (sample_handle.stereo_mode and 7) {
                            1 -> { // mono
                                left_frame += frame_value
                                right_frame += frame_value
                            }

                            2 -> { // right
                                right_frame += frame_value
                            }

                            4 -> { // left
                                left_frame += frame_value
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
                buffer.put(right_frame.toShort())
                buffer.put(left_frame.toShort())
            }

            this.frame +=  AudioTrackHandle.buffer_size
            this.generate_ts = null

            if (this.max_frame > 0 && this.frame >= this.max_frame && this.active_sample_handles.isEmpty()) {
                this.is_alive = false
            }

            return buffer_array
        }


        private fun get_preset(channel: Int): Preset {
            return this.player.get_preset(channel)
        }
    }

    private var active_audio_track_handle: AudioTrackHandle? = null
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()
    private val sample_handle_generator = SampleHandleGenerator()

    init {
        this.loaded_presets[Pair(0, 0)] = this.sound_font.get_preset(0, 0)
        this.loaded_presets[Pair(128, 0)] = this.sound_font.get_preset(0, 128)
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
                new_handle.current_volume = event.velocity.toDouble() * SampleHandle.MAXIMUM_VOLUME / 128.toDouble()
                output.add( new_handle )
            }
        }
        return output
    }

    private fun get_preset(channel: Int): Preset {
        return this.loaded_presets[this.get_channel_preset(channel)]!!
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

    fun select_bank(channel: Int, bank: Int) {
        // NOTE: Changing the bank doesn't trigger a preset change
        // That occurs in change_program()
        val program = if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!.second
        } else {
            0
        }
        this.preset_channel_map[channel] = Pair(bank, program)
    }

    fun change_program(channel: Int, program: Int) {
        val bank = if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!.first
        } else {
            0
        }
        val key = Pair(bank, program)
        if (this.loaded_presets[key] == null) {
            this.loaded_presets[key] = this.sound_font.get_preset(program, bank)
        }


        this.preset_channel_map[channel] = key
    }

    fun play(midi: MIDI, callback: (position: Float) -> Unit): PlaybackInterface {
        val audio_track_handle = AudioTrackHandle()
        this.active_audio_track_handle = audio_track_handle

        val wave_generator = WaveGenerator(this)
        wave_generator.parse_midi(midi)

        var playback_interface = PlaybackInterface()
        thread {
            this.controllable_play(
                playback_interface,
                audio_track_handle,
                wave_generator,
                callback
            )
        }

        return playback_interface
    }

    fun controllable_play(playback_interface: PlaybackInterface, audio_track_handle: AudioTrackHandle, wave_generator: WaveGenerator, callback: (position: Float) -> Unit) {
        val chunk_limit = 10
        val buffer_duration = AudioTrackHandle.buffer_size.toLong() * 1000.toLong() / AudioTrackHandle.sample_rate.toLong()
        val chunks = mutableListOf<Pair<ShortArray, Float>>()

        chunks.add(
            Pair(
                wave_generator.generate(),
                wave_generator.frame.toFloat() / wave_generator.max_frame.toFloat()
            )
        )

        audio_track_handle.play()
        val mutex = Mutex()
        val that = this
        runBlocking {
            var done_building_chunks = false
            launch(newSingleThreadContext("A")) {
                while ((chunks.isNotEmpty() || !done_building_chunks) && !playback_interface.stop_forced) {
                    val write_start_ts = System.currentTimeMillis()

                    try {
                        val (next_chunk, position) = mutex.withLock {
                            chunks.removeFirst()
                        }

                        audio_track_handle.write(next_chunk)
                        thread {
                            callback(position)
                        }
                    } catch (e: NoSuchElementException) {
                        if (done_building_chunks) {
                            break
                        }
                        audio_track_handle.pause()
                        withContext(Dispatchers.IO) {
                            while (playback_interface.playing && chunks.size < chunk_limit) {
                                Thread.sleep(10)
                            }
                        }
                        audio_track_handle.play()
                    }

                    val delta = System.currentTimeMillis() - write_start_ts
                    val sleep = buffer_duration - 10 - delta
                    if (sleep > 0) {
                        Thread.sleep(sleep)
                    }
                }
            }

            launch(newSingleThreadContext("B")) {
                while (playback_interface.playing && (wave_generator.max_frame == 0 || (wave_generator.is_alive)) && !playback_interface.stop_forced) {
                    if (chunks.size > chunk_limit) {
                        withContext(Dispatchers.IO) {
                            Thread.sleep(buffer_duration / 3)
                        }
                        continue
                    }

                    val chunk = wave_generator.generate()

                    // TODO: This won't be 100% accurate due to samples' vol_env_release
                    var generator_position = min(1F, wave_generator.frame.toFloat() / wave_generator.max_frame.toFloat())

                    mutex.withLock {
                        chunks.add(Pair(chunk, generator_position))
                    }
                }
                wave_generator.frame = 0
                playback_interface.playing = false

                done_building_chunks = true
            }
        }

        audio_track_handle.stop()
        wave_generator.is_alive = true
    }

    fun play_note(channel: Int, note: Int, velocity: Int, duration: Int) {
        var midi = MIDI()
        var ticks = max(1, (duration * 1000) / (500000 / midi.get_ppqn()))
        midi.insert_event(0, 0, NoteOn(channel, note, velocity))
        midi.insert_event(0, ticks, NoteOff(channel, note, 64))
        this.play(midi) {}
    }
}

