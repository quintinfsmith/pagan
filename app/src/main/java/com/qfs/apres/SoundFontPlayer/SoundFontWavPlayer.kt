package com.qfs.apres.SoundFontPlayer

import android.util.Log
import com.qfs.apres.BankSelect
import com.qfs.apres.MIDI
import com.qfs.apres.MIDIEvent
import com.qfs.apres.NoteOff
import com.qfs.apres.NoteOn
import com.qfs.apres.Preset
import com.qfs.apres.ProgramChange
import com.qfs.apres.SetTempo
import com.qfs.apres.SoundFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ShortBuffer
import kotlin.math.max

class SoundFontWavPlayer(var sound_font: SoundFont) {
    var playing = false
    var stop_forced = false
    class WaveGenerator(var midi: MIDI, var sound_font: SoundFont) {
        private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
        private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()

        var frame = 0
        var active_sample_handles = HashMap<Pair<Int, Int>, MutableSet<SampleHandle>>()
        private val sample_handle_generator = SampleHandleGenerator()

        var midi_events_by_frame = HashMap<Int, List<MIDIEvent>>()
        var max_frame = 0

        init {
            this.loaded_presets[Pair(0, 0)] = this.sound_font.get_preset(0, 0)
            this.loaded_presets[Pair(128, 0)] = this.sound_font.get_preset(0,128)



            var frames_per_tick = ((500000 / this.midi.get_ppqn()) * AudioTrackHandle.sample_rate) / 1000000
            for ((tick, events) in this.midi.get_all_events_grouped()) {
                val tick_frame = (tick * frames_per_tick)
                this.midi_events_by_frame[tick_frame] = events

                // Need to set Tempo
                for (event in events) {
                    when (event) {
                        is SetTempo -> {
                            frames_per_tick = ((event.get_uspqn() / this.midi.get_ppqn()) * AudioTrackHandle.sample_rate) / 1000000
                        }
                        is BankSelect -> {
                            this.select_bank(event.channel, event.value)
                        }
                        is ProgramChange -> {
                            this.change_program(event.channel, event.program)
                        }
                    }
                }
                this.max_frame = max(tick_frame, this.max_frame)
            }
        }
        fun select_bank(channel: Int, bank: Int) {
            // NOTE: Changing the bank doesn't trigger a preset change
            // That occurs in change_program()
            var program = if (this.preset_channel_map.containsKey(channel)) {
                this.preset_channel_map[channel]!!.second
            } else {
                0
            }
            this.preset_channel_map[channel] = Pair(bank, program)
        }

        fun change_program(channel: Int, program: Int) {
            var bank = if (this.preset_channel_map.containsKey(channel)) {
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

        fun generate(): ShortArray {
            var buffer_array = ShortArray(AudioTrackHandle.buffer_size * 2)
            var buffer = ShortBuffer.wrap(buffer_array)

            for (i in 0 until AudioTrackHandle.buffer_size) {
                var left_frame = 0
                var right_frame = 0
                val f = i + this.frame
                if (this.midi_events_by_frame.containsKey(f)) {
                    for (event in this.midi_events_by_frame[f]!!) {
                        when (event) {
                            is NoteOn -> {
                                val preset = this.get_preset(event.channel)
                                this.active_sample_handles[Pair(event.channel, event.note)] = this.gen_sample_handles(event, preset).toMutableSet()
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

            this.frame += AudioTrackHandle.buffer_size

            return buffer_array
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
    }

    fun pause_playback() {
        this.stop_forced = true
    }

    fun play(midi: MIDI) {
        this.play(midi) {}
    }

    fun play(midi: MIDI, callback: (position: Float) -> Unit) {
        var audiotrackhandle = AudioTrackHandle()
        var wave_generator = WaveGenerator(midi, this.sound_font)
        var buffer_duration = AudioTrackHandle.buffer_size.toLong() * 1000.toLong() / AudioTrackHandle.sample_rate.toLong()
        var chunks = mutableListOf<ShortArray>()
        var chunk_limit = 10

        for (i in 0 until chunk_limit) {
            chunks.add(wave_generator.generate())
        }

        audiotrackhandle.play()
        var mutex = Mutex()
        var that = this
        this.playing = true
        runBlocking {
            var next_chunk = chunks.removeFirst()
            var done_building_chunks = false

            launch(newSingleThreadContext("A")) {
                while ((chunks.isNotEmpty() || !done_building_chunks) && !that.stop_forced) {
                    var write_start_ts = System.currentTimeMillis()
                    audiotrackhandle.write(next_chunk)

                    try {
                        next_chunk = mutex.withLock {
                            chunks.removeFirst()
                        }
                    } catch (e: NoSuchElementException) {
                        if (done_building_chunks) {
                            break
                        }
                        audiotrackhandle.pause()
                        withContext(Dispatchers.IO) {
                            while (that.playing && chunks.size < chunk_limit) {
                                Thread.sleep(10)
                            }
                        }
                        audiotrackhandle.play()
                    }

                    var delta = System.currentTimeMillis() - write_start_ts
                    var sleep = buffer_duration - 10 - delta
                    if (sleep > 0) {
                        Thread.sleep(sleep)
                    }
                }
            }

            launch(newSingleThreadContext("B")) {
                while (that.playing && wave_generator.max_frame > wave_generator.frame && !that.stop_forced) {
                    if (chunks.size > chunk_limit) {
                        continue
                    }

                    var chunk = wave_generator.generate()

                    mutex.withLock {
                        chunks.add(chunk)
                    }
                }
                done_building_chunks = true
            }
        }
        this.stop_forced = false
        this.playing = false

        audiotrackhandle.stop()
        Log.d("AAA", "stopped")
    }
}

