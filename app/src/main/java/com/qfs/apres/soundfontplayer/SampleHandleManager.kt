package com.qfs.apres.soundfontplayer

import android.media.AudioFormat
import android.media.AudioTrack
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont2.InstrumentDirective
import com.qfs.apres.soundfont2.Preset
import com.qfs.apres.soundfont2.SampleDirective
import com.qfs.apres.soundfont2.SoundFont
import kotlin.math.max

class SampleHandleManager(
        var soundfont: SoundFont,
        var sample_rate: Int,
        target_buffer_size: Int = 0,
        var sample_limit: Int? = null,
        ignore_lfo: Boolean = false
    ) {
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val preset_channel_map = HashMap<Int, Pair<Int, Int>>()
    private val sample_handle_generator: SampleHandleGenerator
    val buffer_size: Int

    init {
        val adj_target_buffer_size = max(
            target_buffer_size,
            AudioTrack.getMinBufferSize(
                this.sample_rate,
                AudioFormat.ENCODING_PCM_FLOAT,
                AudioFormat.CHANNEL_OUT_STEREO
            ) * 2 // too small causes clipping
        )

        this.buffer_size = adj_target_buffer_size

        this.sample_handle_generator = SampleHandleGenerator(
            this.sample_rate,
            this.buffer_size,
            ignore_lfo,
        )
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
                this.soundfont.get_preset(program, bank)
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

    fun gen_sample_handles(event: NoteOn79): Set<SampleHandle> {
        val preset = this.get_preset(event.channel) ?: return setOf()
        val output = mutableSetOf<SampleHandle>()
        val velocity = event.velocity shr 8
        val potential_instruments = preset.get_instruments(event.note, velocity)
        val sample_pairs = mutableListOf<Pair<SampleDirective, InstrumentDirective>>()
        for (p_instrument in potential_instruments) {
            val sample_directives = p_instrument.instrument?.get_samples(
                event.note,
                velocity
            )?.toList() ?: listOf()

            for (sample_directive in sample_directives) {
                if (sample_directive.sample == null) {
                    continue
                }
                sample_pairs.add(Pair(sample_directive, p_instrument))
            }
        }

        for ((sample,p_instrument) in sample_pairs) {
            val (new_handle, new_linked_handle) = this.sample_handle_generator.get(
                event,
                sample,
                p_instrument,
                preset
            ) ?: continue

            //new_handle.volume_profile = velocity.toFloat()  * .6F / 128.toFloat()
            output.add(new_handle)
            if (new_linked_handle != null) {
                output.add(new_linked_handle)
            }

            if (this.sample_limit != null && output.size >= this.sample_limit!!) {
                break
            }
        }

        return output
    }

    fun gen_sample_handles(event: NoteOn): Set<SampleHandle> {
        val preset = this.get_preset(event.channel) ?: return setOf()
        val output = mutableSetOf<SampleHandle>()
        val potential_instruments = preset.get_instruments(event.get_note(), event.get_velocity())

        val sample_pairs = mutableListOf<Pair<SampleDirective, InstrumentDirective>>()
        for (p_instrument in potential_instruments) {
            val sample_directives = p_instrument.instrument!!.get_samples(
                event.get_note(),
                event.get_velocity()
            ).toList()

            for (sample_directive in sample_directives) {
                if (sample_directive.sample == null) {
                    continue
                }
                sample_pairs.add(Pair(sample_directive, p_instrument))
            }
        }

        for ((sample_directive, p_instrument) in sample_pairs) {
            val (new_handle, new_linked_handle) = this.sample_handle_generator.get(
                event,
                sample_directive,
                //p_instrument.instrument?.global_zone ?: SampleDirective(),
                p_instrument,
                preset
            ) ?: continue
            //new_handle.volume_profile = (event.get_velocity().toFloat() * .6F / 128F)
            output.add(new_handle)
            if (new_linked_handle != null) {
                output.add(new_linked_handle)
            }

            if (this.sample_limit != null && output.size >= this.sample_limit!!) break
        }

        return output
    }

    private fun decache_unused_presets() {
        val loaded_preset_keys = this.loaded_presets.keys.toMutableSet()
        for ((_, key) in this.preset_channel_map) {
            if (loaded_preset_keys.contains(key)) {
                loaded_preset_keys.remove(key)
            }
        }

        for (key in loaded_preset_keys) {
            val preset = this.loaded_presets[key] ?: continue
            this.sample_handle_generator.decache_sample_data(preset)
            this.loaded_presets.remove(key)
        }
    }

    fun get_preset(channel: Int): Preset? {
        val key = this.get_channel_preset(channel)
        return this.loaded_presets[key]
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

    fun clear() {
        this.loaded_presets.clear()
        this.sample_handle_generator.clear()
    }

    fun get_samples_generated(): Int {
        return this.sample_handle_generator.generated
    }

    fun destroy() {
        this.sample_handle_generator.destroy()
    }
}
