package com.qfs.apres.soundfontplayer
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.SampleDirective
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.InstrumentDirective
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

    fun get(event: NoteOn, sample: SampleDirective, instrument: InstrumentDirective, preset: Preset, linked_handle_count: Int = 1): SampleHandle {
        // set the key index to some hash of the note to allow for indexing byte note AS WELL as indexing by index
        val map_key = this.cache_new(event.get_note(), 0, sample, instrument, preset, linked_handle_count)
        return SampleHandle(this.sample_data_map[map_key]!!)
    }

    fun get(event: NoteOn79, sample: SampleDirective, instrument: InstrumentDirective, preset: Preset, linked_handle_count: Int = 1): SampleHandle {
        val map_key = this.cache_new(event.note, event.bend, sample, instrument, preset, linked_handle_count)
        return SampleHandle(this.sample_data_map[map_key]!!)
    }

    fun cache_new(note: Int, bend: Int, sample: SampleDirective, instrument: InstrumentDirective, preset: Preset, linked_handle_count: Int = 1): MapKey {
        val map_key = MapKey(note, bend, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(map_key)) {
            this.sample_data_map[map_key] = this.generate_new(note, bend, sample, instrument, preset, linked_handle_count)
        }
        return map_key
    }

    fun generate_new(note: Int, bend: Int, sample: SampleDirective, instrument: InstrumentDirective, preset: Preset, linked_handle_count: Int = 1): SampleHandle {
        var pitch_shift = 1.0
        val original_note = sample.root_key ?: sample.sample!!.originalPitch

        // TODO: Why did I do this check? I vaguely remember needing it but I need a note
        val target_pitch = if (original_note != 255) {
            val tuning_cent: Int = (sample.tuning_cent ?: instrument.instrument?.global_zone?.tuning_cent ?: 0 ) + (instrument.tuning_cent ?: 0) + (preset.global_zone?.tuning_cent ?: 0)
            var tuning_semi: Double = ((sample.tuning_semi ?: instrument.instrument?.global_zone?.tuning_semi ?: 0 )
                + (instrument.tuning_semi ?: 0)
                + (preset.global_zone?.tuning_semi ?: 0)).toDouble()

            tuning_semi += tuning_cent / 100.0

            val original_pitch = (2.0).pow(original_note.toFloat() / 12.0)
            val required_pitch = (2.0).pow((note.toFloat() + tuning_semi + (bend.toFloat() / 512.0)) / 12.0)
            pitch_shift = required_pitch / original_pitch
            required_pitch
        } else {
            1.0
        }

        if (sample.sample!!.sampleRate != this.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toDouble() / this.sample_rate.toDouble())
        }

        val data = sample.sample!!.data!!
        val initial_attenuation: Double = (sample.attenuation ?: instrument.instrument?.global_zone?.attenuation ?: 0.0) + (instrument.attenuation ?: 0.0) + (preset.global_zone?.attenuation ?: 0.0)
        val vol_env_sustain: Double = (sample.vol_env_sustain ?: instrument.instrument?.global_zone?.vol_env_sustain ?: 0.0) + (instrument.vol_env_sustain ?: 0.0) + (preset.global_zone?.vol_env_sustain ?: 0.0)
        val volume_envelope = SampleHandle.VolumeEnvelope(
            sample_rate = this.sample_rate,
            delay = (sample.vol_env_delay ?: instrument.instrument?.global_zone?.vol_env_delay ?: 0.0 )
                * (instrument.vol_env_delay ?: 1.0)
                * (preset.global_zone?.vol_env_delay ?: 1.0),
            attack = (sample.vol_env_attack ?: instrument.instrument?.global_zone?.vol_env_attack ?: 0.0 )
                * (instrument.vol_env_attack ?: 1.0)
                * (preset.global_zone?.vol_env_attack ?: 1.0),
            hold = (sample.vol_env_hold ?: instrument.instrument?.global_zone?.vol_env_hold ?: 0.0 )
                * (instrument.vol_env_hold ?: 1.0)
                * (preset.global_zone?.vol_env_hold ?: 1.0),
            decay = (sample.vol_env_decay ?: instrument.instrument?.global_zone?.vol_env_decay ?: 0.0 )
                * (instrument.vol_env_decay ?: 1.0)
                * (preset.global_zone?.vol_env_decay ?: 1.0),
            release = (sample.vol_env_release ?: instrument.instrument?.global_zone?.vol_env_release ?: 0.0 )
                * (instrument.vol_env_release ?: 1.0)
                * (preset.global_zone?.vol_env_release ?: 1.0),
            sustain_attenuation = 1.0 - (max(0.0, min(vol_env_sustain, 1000.0)) / 100.0)
        )

        val mod_env_sustain = (sample.mod_env_sustain ?: instrument.instrument?.global_zone?.mod_env_sustain ?: 0.0) * (instrument.mod_env_sustain ?: 0.0) * (preset.global_zone?.mod_env_sustain ?: 0.0)
        val modulation_envelope = SampleHandle.ModulationEnvelope(
            sample_rate = this.sample_rate,
            delay = (sample.mod_env_delay ?: instrument.instrument?.global_zone?.mod_env_delay ?: 0.0 )
                * (instrument.mod_env_delay ?: 1.0)
                * (preset.global_zone?.mod_env_delay ?: 1.0),
            attack = (sample.mod_env_attack ?: instrument.instrument?.global_zone?.mod_env_attack ?: 0.0 )
                * (instrument.mod_env_attack ?: 1.0)
                * (preset.global_zone?.mod_env_attack ?: 1.0),
            hold = (sample.mod_env_hold ?: instrument.instrument?.global_zone?.mod_env_hold ?: 0.0)
                * (instrument.mod_env_hold ?: 1.0)
                * (preset.global_zone?.mod_env_hold ?: 1.0),
            decay = (sample.mod_env_decay ?: instrument.instrument?.global_zone?.mod_env_decay ?: 0.0)
                * (instrument.mod_env_decay ?: 1.0)
                * (preset.global_zone?.mod_env_decay ?: 1.0),
            release = (sample.mod_env_release ?: instrument.instrument?.global_zone?.mod_env_release ?: 0.0)
                * (instrument.mod_env_release ?: 1.0)
                * (preset.global_zone?.mod_env_release ?: 1.0),
            sustain_attenuation = 1.0 - (max(0.0, min(mod_env_sustain, 1000.0)) / 100.0)
        )

        val mod_lfo_freq: Double = (sample.mod_lfo_freq ?: instrument.instrument?.global_zone?.mod_lfo_freq ?: 0.0) * (instrument.mod_lfo_freq ?: 1.0) * (preset.global_zone?.mod_lfo_freq ?: 1.0)
        val mod_lfo_delay: Double = (sample.mod_lfo_delay ?: instrument.instrument?.global_zone?.mod_lfo_delay ?: 0.0) * (instrument.mod_lfo_delay ?: 1.0) * (preset.global_zone?.mod_lfo_delay ?: 1.0)
        val mod_lfo_to_volume: Double = (sample.mod_lfo_to_volume ?: instrument.instrument?.global_zone?.mod_lfo_to_volume ?: 0.0 ) + (instrument.mod_lfo_to_volume ?: 0.0) + (preset.global_zone?.mod_lfo_to_volume ?: 0.0)
        val mod_lfo_pitch: Int = (sample.mod_lfo_pitch ?: instrument.instrument?.global_zone?.mod_lfo_pitch ?: 0 ) + (instrument.mod_lfo_pitch ?: 0) + (preset.global_zone?.mod_lfo_pitch ?: 0)
        val mod_lfo_filter: Int = (sample.mod_lfo_filter ?: instrument.instrument?.global_zone?.mod_lfo_filter ?: 0 ) + (instrument.mod_lfo_filter ?: 0) + (preset.global_zone?.mod_lfo_filter ?: 0)

        val filter_cutoff: Double = (sample.filter_cutoff ?: instrument.instrument?.global_zone?.filter_cutoff ?: 13500.0 ) + (instrument.filter_cutoff ?: 0.0) + (preset.global_zone?.filter_cutoff ?: 0.0)
        this.generated += 1

        return SampleHandle(
            data = data,
            sample_rate = sample_rate,
            pan = (sample.pan ?: instrument.pan ?: preset.global_zone?.pan ?: 0.0) * 100.0 / 500.0,
            pitch_shift = pitch_shift,
            initial_attenuation = 1.0 - (initial_attenuation / 100.0),
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
                pitch = target_pitch.pow((1200.0 + mod_lfo_pitch.toDouble()) / 1200.0) / target_pitch,
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
