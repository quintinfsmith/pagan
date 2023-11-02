package com.qfs.apres.soundfontplayer

import android.media.AudioFormat
import android.media.AudioTrack
import com.qfs.apres.event.NoteOn
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.SoundFont
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SampleHandleManager(
    var soundfont: SoundFont,
    var sample_rate: Int,
    val buffer_size: Int = AudioTrack.getMinBufferSize(
        sample_rate,
        AudioFormat.ENCODING_PCM_16BIT,
        AudioFormat.CHANNEL_OUT_STEREO
    )) {
    private val loaded_presets_mutex = Mutex()
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()
    private val preset_channel_map_mutex = Mutex()
    private val sample_handle_generator = SampleHandleGenerator(sample_rate, buffer_size)

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
        runBlocking {
            this@SampleHandleManager.loaded_presets_mutex.withLock {
                if (this@SampleHandleManager.loaded_presets[key] == null) {
                    this@SampleHandleManager.loaded_presets[key] = try {
                        this@SampleHandleManager.soundfont.get_preset(program, bank)
                    } catch (e: SoundFont.InvalidPresetIndex) {
                        if (channel == 9) {
                            if (Pair(bank, 0) in this@SampleHandleManager.loaded_presets) {
                                this@SampleHandleManager.loaded_presets[Pair(bank, 0)]!!
                            } else {
                                return@withLock
                            }
                        } else {
                            if (Pair(0, program) in this@SampleHandleManager.loaded_presets) {
                                this@SampleHandleManager.loaded_presets[Pair(0, program)]!!
                            } else if (Pair(0, 0) in this@SampleHandleManager.loaded_presets) {
                                this@SampleHandleManager.loaded_presets[Pair(0, 0)]!!
                            } else {
                                return@withLock
                            }
                        }
                    }
                }
            }
            this@SampleHandleManager.preset_channel_map_mutex.withLock {
                this@SampleHandleManager.preset_channel_map[channel] = key
            }
        }
        this.decache_unused_presets()
    }

    fun gen_sample_handles(event: NoteOn, preset: Preset): Set<SampleHandle> {
        val output = mutableSetOf<SampleHandle>()
        val potential_instruments = preset.get_instruments(event.get_note(), event.get_velocity())

        for (p_instrument in potential_instruments) {
            val samples = p_instrument.instrument!!.get_samples(
                event.get_note(),
                event.get_velocity()
            ).toList()

            for (sample in samples) {
                val new_handle = this.sample_handle_generator.get(event, sample, p_instrument, preset)
                new_handle.current_volume = (event.get_velocity().toDouble() / 128.toDouble()) * SampleHandle.MAXIMUM_VOLUME
                output.add( new_handle )
            }
        }

        return output
    }

    private fun decache_unused_presets() {
        runBlocking {
            val loaded_preset_keys = this@SampleHandleManager.loaded_presets_mutex.withLock {
                this@SampleHandleManager.loaded_presets.keys.toMutableSet()
            }
            this@SampleHandleManager.preset_channel_map_mutex.withLock {
                for ((_, key) in this@SampleHandleManager.preset_channel_map) {
                    if (loaded_preset_keys.contains(key)) {
                        loaded_preset_keys.remove(key)
                    }
                }
            }

            this@SampleHandleManager.loaded_presets_mutex.withLock {
                for (key in loaded_preset_keys) {
                    val preset = this@SampleHandleManager.loaded_presets[key] ?: continue
                    this@SampleHandleManager.sample_handle_generator.decache_sample_data(preset)
                    this@SampleHandleManager.loaded_presets.remove(key)
                }
            }
        }
    }

    fun get_preset(channel: Int): Preset? {
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
}