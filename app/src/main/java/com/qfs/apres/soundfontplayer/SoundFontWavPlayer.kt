package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.BankSelect
import com.qfs.apres.Midi
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.soundfont.SoundFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.BufferUnderflowException
import java.nio.IntBuffer
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class SoundFontWavPlayer(private var sound_font: SoundFont) {
    class PlaybackInterface() {
        var playing = true
        var stop_forced = false
        fun stop() {
            this.stop_forced = true
        }
    }
    class WaveGenerator(private var player: SoundFontWavPlayer) {
        var frame = 0
        private var active_sample_handles = HashMap<Pair<Int, Int>, MutableSet<SampleHandle>>()
        private var midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
        var max_frame = 0
        var is_alive = true
        private var generate_ts: Long? = null

        /*
            Get the max volume of the quietest active sample
         */

         fun parse_midi(midi: Midi) {
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
            val initial_array = IntArray(AudioTrackHandle.buffer_size * 2)
            val buffer = IntBuffer.wrap(initial_array)
            this.generate_ts = System.currentTimeMillis()


            var max_frame_value = 0
            for (i in 0 until AudioTrackHandle.buffer_size) {
                var left_frame = 0
                var right_frame = 0
                val f = this.frame + i
                if (this.midi_events_by_frame.containsKey(f)) {
                    for (event in this.midi_events_by_frame[f]!!) {
                        when (event) {
                            is NoteOn -> {
                                val preset = this.get_preset(event.channel) ?: continue
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
                        val pan = sample_handle.pan
                        when (sample_handle.stereo_mode and 7) {
                            1 -> { // mono
                                if (pan > 0) {
                                    left_frame += frame_value
                                    right_frame += (frame_value.toDouble() * (100 - pan) / 100.0).toInt().toShort()
                                } else if (pan < 0) {
                                    left_frame += (frame_value.toDouble() * (100 + pan) / 100.0).toInt().toShort()
                                    right_frame += frame_value
                                } else {
                                    left_frame += frame_value
                                    right_frame += frame_value
                                }
                            }

                            2 -> { // right
                                if (pan > 0) {
                                    right_frame += (frame_value.toDouble() * (100 - pan) / 100.0).toInt().toShort()
                                } else {
                                    right_frame += frame_value
                                }
                            }

                            4 -> { // left
                                if (pan < 0) {
                                    left_frame += (frame_value.toDouble() * (100 + pan) / 100.0).toInt().toShort()
                                } else {
                                    left_frame += frame_value
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

            this.frame +=  AudioTrackHandle.buffer_size
            this.generate_ts = null

            if (this.max_frame > 0 && this.frame >= this.max_frame && this.active_sample_handles.isEmpty()) {
                this.is_alive = false
            }

            return compressed_array
        }


        private fun get_preset(channel: Int): Preset? {
            return this.player.get_preset(channel)
        }
    }

    private var active_audio_track_handle: AudioTrackHandle? = null
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()
    private val sample_handle_generator = SampleHandleGenerator()

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


    fun play(midi: Midi, callback: (position: Float) -> Unit): PlaybackInterface {
        val audio_track_handle = AudioTrackHandle()
        this.active_audio_track_handle = audio_track_handle

        val wave_generator = WaveGenerator(this)
        wave_generator.parse_midi(midi)

        val playback_interface = PlaybackInterface()
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

    private fun controllable_play(playback_interface: PlaybackInterface, audio_track_handle: AudioTrackHandle, wave_generator: WaveGenerator, callback: (position: Float) -> Unit) {
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
                playback_interface.playing = false
            }

            launch(newSingleThreadContext("B")) {
                while (playback_interface.playing && (wave_generator.max_frame == 0 || (wave_generator.is_alive)) && !playback_interface.stop_forced) {
                    if (chunks.size > chunk_limit) {
                        withContext(Dispatchers.IO) {
                            Thread.sleep(buffer_duration / 3)
                        }
                        continue
                    }
                    val chunk = try {
                        wave_generator.generate()
                    } catch (e: BufferUnderflowException) {
                        break
                    }

                    // TODO: This won't be 100% accurate due to samples' vol_env_release
                    val generator_position = min(1F, wave_generator.frame.toFloat() / wave_generator.max_frame.toFloat())

                    mutex.withLock {
                        chunks.add(Pair(chunk, generator_position))
                    }
                }
                wave_generator.frame = 0

                done_building_chunks = true
            }
        }

        audio_track_handle.stop()
        wave_generator.is_alive = true
    }

    fun play_note(channel: Int, note: Int, velocity: Int, duration: Int) {
        val midi = Midi()
        val ticks = max(1, (duration * 1000) / (500000 / midi.get_ppqn()))
        midi.insert_event(0, 0, NoteOn(channel, note, velocity))
        midi.insert_event(0, ticks, NoteOff(channel, note, 64))
        this.play(midi) {}
    }
}

