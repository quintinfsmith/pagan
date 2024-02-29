package com.qfs.apres.soundfontplayer
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.InstrumentDirective
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.SampleDirective
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SampleHandleGenerator(var sample_rate: Int, var buffer_size: Int) {
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

    fun get(event: NoteOn, sample_direcctive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective, linked_handle_count: Int = 1): SampleHandle {
        // set the key index to some hash of the note to allow for indexing byte note AS WELL as indexing by index
        val map_key = this.cache_or_create_new(event.get_note(), 0, sample_direcctive, global_sample_directive, instrument_directive, global_instrument_directive, linked_handle_count)
        return SampleHandle.copy(this.sample_data_map[map_key]!!)
    }

    fun get(event: NoteOn79, sample_directive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective, linked_handle_count: Int = 1): SampleHandle {
        val map_key = this.cache_or_create_new(event.note, event.bend, sample_directive, global_sample_directive, instrument_directive, global_instrument_directive, linked_handle_count)
        return SampleHandle.copy(this.sample_data_map[map_key]!!)
    }

    fun cache_or_create_new(note: Int, bend: Int, sample_directive: SampleDirective, global_sample_directive: SampleDirective, instrument_directive: InstrumentDirective, global_instrument_directive: InstrumentDirective, linked_handle_count: Int = 1): MapKey {
        val map_key = MapKey(note, bend, sample_directive.hashCode(), instrument_directive.hashCode(), global_instrument_directive.hashCode())
        if (!sample_data_map.contains(map_key)) {
            this.sample_data_map[map_key] = this.generate_new(note, bend, sample_directive, global_sample_directive, instrument_directive, global_instrument_directive, linked_handle_count)
        }

        return map_key
    }

    fun generate_new(note: Int, bend: Int, sample: SampleDirective, global_sample_directive: SampleDirective, instrument: InstrumentDirective, global_instrument_directive: InstrumentDirective, linked_handle_count: Int = 1): SampleHandle {
        var pitch_shift = 1F

        val original_note = sample.root_key ?: sample.sample!!.originalPitch

        // TODO: Why did I do this check? I vaguely remember needing it but I need a note
        val target_pitch = if (original_note != 255) {
            val tuning_cent: Int = (sample.tuning_cent ?: global_sample_directive.tuning_cent ?: 0 ) + (instrument.tuning_cent ?: 0) + (global_instrument_directive.tuning_cent ?: 0)
            var tuning_semi: Float = ((sample.tuning_semi ?: global_sample_directive.tuning_semi ?: 0 )
                + (instrument.tuning_semi ?: 0)
                + (global_instrument_directive.tuning_semi ?: 0)).toFloat()

            // Skip tuning if we can
            if (tuning_cent == 0 && tuning_semi == 0F && note == original_note && bend == 0) {
                1F
            } else {
                tuning_semi += tuning_cent / 100F
                val original_pitch = (2F).pow(original_note.toFloat() / 12F)
                val required_pitch = (2F).pow((note.toFloat() + tuning_semi + (bend.toFloat() / 512F)) / 12F)
                pitch_shift = required_pitch / original_pitch
                required_pitch
            }
        } else {
            1F
        }

        if (sample.sample!!.sampleRate != this.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toFloat() / this.sample_rate.toFloat())
        }

        val data = sample.sample!!.data!!
        val initial_attenuation: Float = (sample.attenuation ?: global_sample_directive.attenuation ?: 0F) + (instrument.attenuation ?: 0F) + (global_instrument_directive.attenuation ?: 0F)
        val vol_env_sustain: Float = (sample.vol_env_sustain ?: global_sample_directive.vol_env_sustain ?: 0F) + (instrument.vol_env_sustain ?: 0F) + (global_instrument_directive.vol_env_sustain ?: 0F)
        val volume_envelope = SampleHandle.VolumeEnvelope(
            sample_rate = this.sample_rate,
            delay = (sample.vol_env_delay ?: global_sample_directive.vol_env_delay ?: 0F )
                * (instrument.vol_env_delay ?: 1F)
                * (global_instrument_directive.vol_env_delay ?: 1F),
            attack = (sample.vol_env_attack ?: global_sample_directive.vol_env_attack ?: 0F )
                * (instrument.vol_env_attack ?: 1F)
                * (global_instrument_directive.vol_env_attack ?: 1F),
            hold = (sample.vol_env_hold ?: global_sample_directive.vol_env_hold ?: 0F )
                * (instrument.vol_env_hold ?: 1F)
                * (global_instrument_directive.vol_env_hold ?: 1F),
            decay = (sample.vol_env_decay ?: global_sample_directive.vol_env_decay ?: 0F )
                * (instrument.vol_env_decay ?: 1F)
                * (global_instrument_directive.vol_env_decay ?: 1F),
            release = (sample.vol_env_release ?: global_sample_directive.vol_env_release ?: 0F )
                * (instrument.vol_env_release ?: 1F)
                * (global_instrument_directive.vol_env_release ?: 1F),
            sustain_attenuation = 1F - (max(0F, min(vol_env_sustain, 1000F)) / 100F)
        )

        val mod_env_sustain = (sample.mod_env_sustain ?: global_sample_directive.mod_env_sustain ?: 0F) * (instrument.mod_env_sustain ?: 0F) * (global_instrument_directive.mod_env_sustain ?: 0F)
        val modulation_envelope = SampleHandle.ModulationEnvelope(
            sample_rate = this.sample_rate,
            delay = (sample.mod_env_delay ?: global_sample_directive.mod_env_delay ?: 0F )
                * (instrument.mod_env_delay ?: 1F)
                * (global_instrument_directive.mod_env_delay ?: 1F),
            attack = (sample.mod_env_attack ?: global_sample_directive.mod_env_attack ?: 0F )
                * (instrument.mod_env_attack ?: 1F)
                * (global_instrument_directive.mod_env_attack ?: 1F),
            hold = (sample.mod_env_hold ?: global_sample_directive.mod_env_hold ?: 0F)
                * (instrument.mod_env_hold ?: 1F)
                * (global_instrument_directive.mod_env_hold ?: 1F),
            decay = (sample.mod_env_decay ?: global_sample_directive.mod_env_decay ?: 0F)
                * (instrument.mod_env_decay ?: 1F)
                * (global_instrument_directive.mod_env_decay ?: 1F),
            release = (sample.mod_env_release ?: global_sample_directive.mod_env_release ?: 0F)
                * (instrument.mod_env_release ?: 1F)
                * (global_instrument_directive.mod_env_release ?: 1F),
            sustain_attenuation = 1F - (max(0F, min(mod_env_sustain, 1000F)) / 100F)
        )

        val mod_lfo_freq: Float = (sample.mod_lfo_freq ?: global_sample_directive.mod_lfo_freq ?: 0F) * (instrument.mod_lfo_freq ?: 1F) * (global_instrument_directive.mod_lfo_freq ?: 1F)
        val mod_lfo_delay: Float = (sample.mod_lfo_delay ?: global_sample_directive.mod_lfo_delay ?: 0F) * (instrument.mod_lfo_delay ?: 1F) * (global_instrument_directive.mod_lfo_delay ?: 1F)
        val mod_lfo_to_volume: Float = (sample.mod_lfo_to_volume ?: global_sample_directive.mod_lfo_to_volume ?: 0F ) + (instrument.mod_lfo_to_volume ?: 0F) + (global_instrument_directive.mod_lfo_to_volume ?: 0F)
        val mod_lfo_pitch: Int = (sample.mod_lfo_pitch ?: global_sample_directive.mod_lfo_pitch ?: 0 ) + (instrument.mod_lfo_pitch ?: 0) + (global_instrument_directive.mod_lfo_pitch ?: 0)
        val mod_lfo_filter: Int = (sample.mod_lfo_filter ?: global_sample_directive.mod_lfo_filter ?: 0 ) + (instrument.mod_lfo_filter ?: 0) + (global_instrument_directive.mod_lfo_filter ?: 0)
        val filter_cutoff: Float = (sample.filter_cutoff ?: global_sample_directive.filter_cutoff ?: 13500F ) + (instrument.filter_cutoff ?: 0F) + (global_instrument_directive.filter_cutoff ?: 0F)
        this.generated += 1

        return SampleHandle(
            data = data,
            sample_rate = sample_rate,
            pan = (sample.pan ?: instrument.pan ?: global_instrument_directive.pan ?: 0F) * 100F / 500F,
            pitch_shift = pitch_shift,
            initial_attenuation = 1F - (initial_attenuation / 100F),
            stereo_mode = sample.sample!!.sampleType,
            loop_points = if (sample.sampleMode != null && sample.sampleMode!! and 1 == 1) {
                val start = (sample.sample!!.loopStart.toFloat() / pitch_shift)
                val size = (sample.sample!!.loopEnd - sample.sample!!.loopStart).toFloat() / pitch_shift
                Pair(start.toInt(), (start + size).toInt())
            } else {
                null
            },
            modulation_lfo = SampleHandle.LFO(
                sample_rate = this.sample_rate,
                frequency = mod_lfo_freq,
                volume = mod_lfo_to_volume,
                pitch = target_pitch.pow((1200F + mod_lfo_pitch.toFloat()) / 1200F) / target_pitch,
                filter = mod_lfo_filter,
                delay = mod_lfo_delay
            ),
            volume_envelope =  volume_envelope,
            modulation_envelope = modulation_envelope,
            filter_cutoff = filter_cutoff,
            linked_handle_count = linked_handle_count
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
