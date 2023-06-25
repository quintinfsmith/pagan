package com.qfs.apres.SoundFontPlayer

import com.qfs.apres.MIDI
import com.qfs.apres.NoteOff
import com.qfs.apres.NoteOn
import com.qfs.apres.Preset
import com.qfs.apres.SoundFont
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SoundFontPlayer(var sound_font: SoundFont) {
    data class TSNoteOn(var channel: Int, var note: Int, var velocity: Int) {
        var timestamp = System.currentTimeMillis()
        constructor(event: NoteOn): this(
            event.channel,
            event.note,
            event.velocity
        )
    }
    private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    val audio_track_handle = AudioTrackHandle()
    private val active_handle_keys = HashMap<Pair<Int, Int>, Set<Long>>()
    private val active_handle_mutex = Mutex()
    private val sample_handle_generator = SampleHandleGenerator()

    val active_note_map = mutableSetOf<Pair<Int, Int>>()

    init {
        this.loaded_presets[Pair(0, 0)] = this.sound_font.get_preset(0, 0)
        this.loaded_presets[Pair(128, 0)] = this.sound_font.get_preset(0,128)
    }

    fun press_note(event: NoteOn) {
        val key = Pair(event.channel, event.note)
        val ts_event = TSNoteOn(event)
        if (this.active_note_map.contains(key)) {
            return
        }
        this.active_note_map.add(key)
        val preset = this.get_preset(event.channel)
        val that = this
        runBlocking {
            that.active_handle_mutex.withLock {
                if (! that.audio_track_handle.is_playing) {
                    that.audio_track_handle.play()
                }

                // Get Join delay BEFORE generating sample
                val sample_handles = that.gen_sample_handles(ts_event, preset)
                val existing_keys = that.active_handle_keys[key]?.toSet()
                if (existing_keys != null) {
                    that.attempt {
                        that.audio_track_handle.queue_sample_handles_release(
                            existing_keys
                        )
                    }
                }

                that.attempt {
                    var keys = that.audio_track_handle.add_sample_handles( sample_handles )
                    that.active_handle_keys[key] = keys
                }
            }
        }

    }

    fun release_note(event: NoteOff) {
        val key = Pair(event.channel, event.note)
        if (!this.active_note_map.contains(key)) {
            return
        }
        this.active_note_map.remove(key)
        val that = this
        runBlocking {
            that.active_handle_mutex.withLock {
                val keys = that.active_handle_keys[key] ?: return@withLock
                that.active_handle_keys.remove(key)
                that.attempt {
                    that.audio_track_handle.queue_sample_handles_release( keys.toSet() )
                }
            }
        }
    }

    fun kill_note(note: Int, channel: Int) {
        this.attempt {
            val keys = this.active_handle_keys[Pair(channel, note)] ?: return@attempt
            this.audio_track_handle.remove_sample_handles(keys.toSet())
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

    fun kill_channel_sound(channel: Int) {
        val that = this
        val keys = runBlocking {
            that.active_handle_mutex.withLock {
                val output = mutableSetOf<Long>()
                for ((_, output_keys) in that.active_handle_keys.filterKeys { k -> k.second == channel }) {
                    output.union(output_keys)
                }
                output
            }
        }
        this.audio_track_handle.kill_samples(keys)
    }

    //Private Functions//////////////////////
    private fun get_channel_preset(channel: Int): Pair<Int, Int> {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else if (channel == 9) {
            Pair(128, 0)
        } else {
            Pair(0,0)
        }
    }

    private fun gen_sample_handles(event: TSNoteOn, preset: Preset): Set<SampleHandle> {
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

    fun precache_midi(midi: MIDI) {
        for ((_, events) in midi.get_all_events_grouped()) {
            for (event in events) {
                if (event is NoteOn) {
                    val tsevent = TSNoteOn(event)
                    val preset = this.get_preset(event.channel)
                    val potential_instruments = preset.get_instruments(event.note, event.velocity)
                    for (p_instrument in potential_instruments) {
                        val samples = p_instrument.instrument!!.get_samples(
                            event.note,
                            event.velocity
                        ).toList()
                        for (sample in samples) {
                            this.sample_handle_generator.cache_new(tsevent, sample, p_instrument, preset)
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

    fun <T> attempt(callback: () -> T): T? {
        return try {
            callback()
        } catch (e: AudioTrackHandle.HandleStoppedException) {
            null
        }
    }
}