package com.qfs.pagan.apres.SoundFontPlayer

import com.qfs.pagan.apres.MIDI
import com.qfs.pagan.apres.NoteOff
import com.qfs.pagan.apres.NoteOn
import com.qfs.pagan.apres.Preset
import com.qfs.pagan.apres.SoundFont
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SoundFontPlayer(var sound_font: SoundFont) {
    private val preset_channel_map = HashMap<Int, Int>()
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val audio_track_handle = AudioTrackHandle()
    private val active_handle_keys = HashMap<Pair<Int, Int>, Set<Int>>()
    private val active_handle_mutex = Mutex()
    private val sample_handle_generator = SampleHandleGenerator()

    init {
        this.loaded_presets[Pair(0, 0)] = this.sound_font.get_preset(0, 0)
        this.loaded_presets[Pair(128, 0)] = this.sound_font.get_preset(0,128)
    }

    fun press_note(event: NoteOn) {
        val preset = this.get_preset(event.channel)
        val first_ts = System.currentTimeMillis()
        val that = this
        runBlocking {
            that.active_handle_mutex.withLock {
                that.audio_track_handle.kill_mutex.withLock {
                    // Get Join delay BEFORE generating sample
                    val key = Pair(event.note, event.channel)
                    val sample_handles = that.gen_sample_handles(event, preset)
                    val second_ts = System.currentTimeMillis()
                    val join_delay = that.audio_track_handle.get_join_delay(first_ts, second_ts)
                    if (that.active_handle_keys.containsKey(key)) {
                        that.audio_track_handle.queue_sample_handles_stop(
                            that.active_handle_keys[key]!!,
                            join_delay
                        )
                    }

                    that.active_handle_keys[key] = that.audio_track_handle.add_sample_handles(
                        sample_handles,
                        join_delay
                    )
                }
            }
        }
        this.audio_track_handle.play()
    }

    fun release_note(event: NoteOff) {
        val keys = this.active_handle_keys[Pair(event.note, event.channel)] ?: return
        this.audio_track_handle.queue_sample_handles_release(keys, AudioTrackHandle.base_delay_in_frames)
        this.active_handle_keys.remove(Pair(event.note, event.channel))
    }

    fun kill_note(note: Int, channel: Int) {
        val keys = this.active_handle_keys[Pair(note, channel)] ?: return
        this.audio_track_handle.remove_sample_handles(keys)
    }


    fun change_program(channel: Int, program: Int) {
        if (channel == 9) {
            return
        }

        val key = Pair(0, program)
        if (this.loaded_presets[key] == null) {
            this.loaded_presets[key] = this.sound_font.get_preset(program, 0)
        }

        this.preset_channel_map[channel] = program
    }

    fun kill_channel_sound(channel: Int) {
        val to_kill = mutableListOf<Int>()
        val that = this
        val keys = runBlocking {
            that.active_handle_mutex.withLock {
                val output = mutableSetOf<Int>()
                for ((input_key, output_keys) in that.active_handle_keys.filterKeys { k -> k.second == channel }) {
                    output.union(output_keys)
                }
                output
            }
        }
        this.audio_track_handle.kill_samples(keys)
    }

    //Private Functions//////////////////////
    private fun get_channel_preset(channel: Int): Int {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else {
            0
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
                new_handle.current_volume = event.velocity.toDouble() / 128.toDouble()
                output.add( new_handle )
            }
        }
        return output
    }

    private fun get_preset(channel: Int): Preset {
        // TODO: Handle Bank
        val bank = if (channel == 9) {
            128
        } else {
            0
        }
        return this.loaded_presets[Pair(bank, this.get_channel_preset(channel))]!!
    }

    fun precache_midi(midi: MIDI) {
        for ((_, events) in midi.get_all_events_grouped()) {
            for (event in events) {
                if (event is NoteOn) {
                    val preset = this.get_preset(event.channel)
                    val potential_instruments = preset.get_instruments(event.note, event.velocity)
                    for (p_instrument in potential_instruments) {
                        val samples = p_instrument.instrument!!.get_samples(
                            event.note,
                            event.velocity
                        ).toList()
                        for (sample in samples) {
                            this.sample_handle_generator.cache_new(event, sample, p_instrument, preset)
                        }
                    }
                }
            }
        }
    }

    fun clear_sample_cache() {
        this.sample_handle_generator.clear_cache()
    }

    fun stop() {
        val that = this
        runBlocking {
            that.active_handle_mutex.withLock {
                that.active_handle_keys.clear()
            }
        }
        this.audio_track_handle.stop()
    }
    fun enable_play() {
        this.audio_track_handle.enable_play()
    }
}