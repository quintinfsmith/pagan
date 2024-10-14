package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.Generator
import com.qfs.apres.soundfont.InstrumentDirective
import com.qfs.apres.soundfont.Modulator
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.SampleDirective
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SampleHandleGenerator(var sample_rate: Int, var buffer_size: Int, var ignore_envelopes: Boolean = false, var ignore_lfo: Boolean = false) {
    // Hash ignores velocity since velocity isn't baked into sample data
    data class MapKey(
        var note: Int,
        var bend: Int,
        var sample: Int,
        var instrument: Int,
        var preset: Int
    )

    var sample_data_map = HashMap<MapKey, SampleHandle>()
    var generated = 0

    fun clear() {
        this.sample_data_map.clear()
    }

    fun get(event: NoteOn, sample_directive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective): SampleHandle {
        // set the key index to some hash of the note to allow for indexing byte note AS WELL as indexing by index
        val map_key = this.cache_or_create_new(event.get_note(), 0, sample_directive, global_sample_directive, instrument_directive, global_instrument_directive)
        return SampleHandle.copy(this.sample_data_map[map_key]!!)
    }

    fun get(event: NoteOn79, sample_directive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective): SampleHandle {
        val map_key = this.cache_or_create_new(event.note, event.bend, sample_directive, global_sample_directive, instrument_directive, global_instrument_directive)
        return SampleHandle.copy(this.sample_data_map[map_key]!!)
    }

    fun cache_or_create_new(note: Int, bend: Int, sample_directive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective): MapKey {
        val map_key = MapKey(note, bend, sample_directive.hashCode(), instrument_directive.hashCode(), global_instrument_directive.hashCode())
        if (!sample_data_map.contains(map_key)) {
            this.sample_data_map[map_key] = this.generate_new(note, bend, sample_directive, global_sample_directive, instrument_directive, global_instrument_directive)
        }

        return map_key
    }

    fun generate_new(note: Int, bend: Int, sample_directive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective): SampleHandle {
        var pitch_shift = 1F

        val original_note = sample_directive.root_key ?: sample_directive.sample!!.originalPitch

        // 255 Means its an unpitched note and needs no correction.
        if (original_note != 255) {
            var tuning_cent: Int = (sample_directive.tuning_cent ?: global_sample_directive.tuning_cent ?: 0 ) + (instrument_directive.tuning_cent ?: 0) + (global_instrument_directive.tuning_cent ?: 0)
            var tuning_semi: Float = ((sample_directive.tuning_semi ?: global_sample_directive.tuning_semi ?: 0 )
                + (instrument_directive.tuning_semi ?: 0)
                + (global_instrument_directive.tuning_semi ?: 0)).toFloat()

            val pitch_correction = sample_directive.sample!!.pitchCorrection
            // Skip tuning if we can
            if (tuning_cent != 0 || tuning_semi != 0F || note != original_note || bend != 0 || pitch_correction != 0) {
                tuning_semi += tuning_cent.toFloat() / 100F
                val original_pitch = (2F).pow((original_note.toFloat() + (pitch_correction.toFloat() / 100F)) / 12F)
                val required_pitch = (2F).pow((note.toFloat() + tuning_semi + (bend.toFloat() / 512F)) / 12F)
                pitch_shift = required_pitch / original_pitch
            }
        }

        if (sample_directive.sample!!.sampleRate != this.sample_rate) {
            pitch_shift *= (sample_directive.sample!!.sampleRate.toFloat() / this.sample_rate.toFloat())
        }

        val data = sample_directive.sample!!.data!!
        val initial_attenuation: Float = (sample_directive.attenuation ?: global_sample_directive.attenuation ?: 0F) + (instrument_directive.attenuation ?: 0F) + (global_instrument_directive.attenuation ?: 0F)
        val vol_env_sustain: Float = (sample_directive.vol_env_sustain ?: global_sample_directive.vol_env_sustain ?: 0F) + (instrument_directive.vol_env_sustain ?: 0F) + (global_instrument_directive.vol_env_sustain ?: 0F)
        val volume_envelope = if (this.ignore_envelopes) {
            // Delay is still needed
            SampleHandle.VolumeEnvelope(
                sample_rate = this.sample_rate,
                delay = (sample_directive.vol_env_delay ?: global_sample_directive.vol_env_delay ?: 0F )
                    * (instrument_directive.vol_env_delay ?: 1F)
                    * (global_instrument_directive.vol_env_delay ?: 1F)
            )
        } else {
            SampleHandle.VolumeEnvelope(
                sample_rate = this.sample_rate,
                delay = (sample_directive.vol_env_delay ?: global_sample_directive.vol_env_delay ?: 0F )
                    * (instrument_directive.vol_env_delay ?: 1F)
                    * (global_instrument_directive.vol_env_delay ?: 1F),
                attack = (sample_directive.vol_env_attack ?: global_sample_directive.vol_env_attack ?: 0F )
                    * (instrument_directive.vol_env_attack ?: 1F)
                    * (global_instrument_directive.vol_env_attack ?: 1F),
                hold = (sample_directive.vol_env_hold ?: global_sample_directive.vol_env_hold ?: 0F )
                    * (instrument_directive.vol_env_hold ?: 1F)
                    * (global_instrument_directive.vol_env_hold ?: 1F),
                decay = (sample_directive.vol_env_decay ?: global_sample_directive.vol_env_decay ?: 0F )
                    * (instrument_directive.vol_env_decay ?: 1F)
                    * (global_instrument_directive.vol_env_decay ?: 1F),
                release = (sample_directive.vol_env_release ?: global_sample_directive.vol_env_release ?: 0F )
                    * (instrument_directive.vol_env_release ?: 1F)
                    * (global_instrument_directive.vol_env_release ?: 1F),
                sustain_attenuation = max(0F, min(vol_env_sustain, 1440F)) / 100F // Centibels -> bels
            )
        }

        val mod_env_sustain = (sample_directive.mod_env_sustain ?: global_sample_directive.mod_env_sustain ?: 0F) * (instrument_directive.mod_env_sustain ?: 0F) * (global_instrument_directive.mod_env_sustain ?: 0F)
        val modulation_envelope = if (this.ignore_envelopes) {
            SampleHandle.ModulationEnvelope(
                sample_rate = this.sample_rate
            )
        } else {
            SampleHandle.ModulationEnvelope(
                sample_rate = this.sample_rate,
                delay = (sample_directive.mod_env_delay ?: global_sample_directive.mod_env_delay ?: 0F )
                    * (instrument_directive.mod_env_delay ?: 1F)
                    * (global_instrument_directive.mod_env_delay ?: 1F),
                attack = (sample_directive.mod_env_attack ?: global_sample_directive.mod_env_attack ?: 0F )
                    * (instrument_directive.mod_env_attack ?: 1F)
                    * (global_instrument_directive.mod_env_attack ?: 1F),
                hold = (sample_directive.mod_env_hold ?: global_sample_directive.mod_env_hold ?: 0F)
                    * (instrument_directive.mod_env_hold ?: 1F)
                    * (global_instrument_directive.mod_env_hold ?: 1F),
                decay = (sample_directive.mod_env_decay ?: global_sample_directive.mod_env_decay ?: 0F)
                    * (instrument_directive.mod_env_decay ?: 1F)
                    * (global_instrument_directive.mod_env_decay ?: 1F),
                release = (sample_directive.mod_env_release ?: global_sample_directive.mod_env_release ?: 0F)
                    * (instrument_directive.mod_env_release ?: 1F)
                    * (global_instrument_directive.mod_env_release ?: 1F),
                sustain_attenuation = max(0F, min(mod_env_sustain, 1440F)) / 100F // Centibels -> bels
            )
        }

        val mod_lfo_freq: Float = (sample_directive.mod_lfo_freq ?: global_sample_directive.mod_lfo_freq ?: 0F) * (instrument_directive.mod_lfo_freq ?: 1F) * (global_instrument_directive.mod_lfo_freq ?: 1F)
        val mod_lfo_delay: Float = (sample_directive.mod_lfo_delay ?: global_sample_directive.mod_lfo_delay ?: 0F) * (instrument_directive.mod_lfo_delay ?: 1F) * (global_instrument_directive.mod_lfo_delay ?: 1F)
        val mod_lfo_to_volume: Float = (sample_directive.mod_lfo_to_volume ?: global_sample_directive.mod_lfo_to_volume ?: 0F ) + (instrument_directive.mod_lfo_to_volume ?: 0F) + (global_instrument_directive.mod_lfo_to_volume ?: 0F)
        val mod_lfo_pitch: Int = (sample_directive.mod_lfo_pitch ?: global_sample_directive.mod_lfo_pitch ?: 0 ) + (instrument_directive.mod_lfo_pitch ?: 0) + (global_instrument_directive.mod_lfo_pitch ?: 0)
        val mod_lfo_filter: Int = (sample_directive.mod_lfo_filter ?: global_sample_directive.mod_lfo_filter ?: 0 ) + (instrument_directive.mod_lfo_filter ?: 0) + (global_instrument_directive.mod_lfo_filter ?: 0)
        val filter_cutoff: Float = (sample_directive.filter_cutoff ?: global_sample_directive.filter_cutoff ?: 13500F ) * (instrument_directive.filter_cutoff ?: 1F) * (global_instrument_directive.filter_cutoff ?: 1F)
        this.generated += 1

        // TODO what is the priority order of global directives
        val new_modulators = HashMap<Generator.Operation, Set<Modulator>>()
        for ((key, modulators) in sample_directive.modulators) {
            if (!new_modulators.containsKey(key)) {
                new_modulators[key] = mutableSetOf()
            }
            new_modulators[key] = new_modulators[key]!! + modulators
        }

        return SampleHandle(
            data = data,
            sample_rate = sample_rate,
            pan = (sample_directive.pan ?: instrument_directive.pan ?: global_instrument_directive.pan ?: 0F) * 100F / 500F,
            pitch_shift = pitch_shift,
            initial_attenuation = max(0F, min(1440F, initial_attenuation)) / 100F, // Centibels -> bels
            stereo_mode = sample_directive.sample!!.sampleType,
            loop_points = if (sample_directive.sampleMode != null && (sample_directive.sampleMode!! and 1) == 1) {
                Pair(
                    sample_directive.sample!!.loopStart + (sample_directive.loopStartOffset ?: 0) + (global_sample_directive.loopStartOffset ?: 0),
                    sample_directive.sample!!.loopEnd + (sample_directive.loopEndOffset ?: 0) + (global_sample_directive.loopEndOffset ?: 0)
                )
            } else {
                null
            },
            modulation_lfo = if (this.ignore_lfo) {
                null
            } else {
                SampleHandle.LFO(
                    sample_rate = this.sample_rate,
                    frequency = mod_lfo_freq,
                    volume = mod_lfo_to_volume / 100F, // Centibels -> bels
                    pitch = 2F.pow(mod_lfo_pitch.toFloat() / 1200F),
                    filter = mod_lfo_filter,
                    delay = mod_lfo_delay
                )
            },
            volume_envelope = volume_envelope,
            modulation_envelope = modulation_envelope,
            filter_cutoff = filter_cutoff,
            modulators = new_modulators
        )
    }

    //fun resample(sample_data: ShortArray, pitch_shift: Float): ShortArray {
    //    // TODO: This is VERY Niave. Look into actual resampling algorithms
    //    val new_size = (sample_data.size / pitch_shift).toInt()
    //    return ShortArray(new_size) { i: Int ->
    //        val i_offset = (i.toFloat() * pitch_shift).toInt()
    //        sample_data[i_offset]
    //    }
    //}

    fun decache_sample_data(preset: Preset) {
        val to_remove = mutableListOf<MapKey>()
        for ((mapkey, _) in this.sample_data_map) {
            if (mapkey.preset == preset.hashCode()) {
                to_remove.add(mapkey)
            }
        }
        for (mapkey in to_remove) {
             this.sample_data_map.remove(mapkey)
        }

    }
}
