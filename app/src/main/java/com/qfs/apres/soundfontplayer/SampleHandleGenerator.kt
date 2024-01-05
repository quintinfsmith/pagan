package com.qfs.apres.soundfontplayer
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.InstrumentSample
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.PresetInstrument
import kotlin.math.abs
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

    fun clear() {
        this.sample_data_map.clear()
    }

    fun get(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        // set the key index to some hash of the note to allow for indexing byte note AS WELL as indexing by index
        val map_key = this.cache_new(event.get_note(), 0, sample, instrument, preset)
        return SampleHandle(this.sample_data_map[map_key]!!)
    }

    fun get(event: NoteOn79, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        val map_key = this.cache_new(event.note, event.bend, sample, instrument, preset)
        return SampleHandle(this.sample_data_map[map_key]!!)
    }

    fun cache_new(note: Int, bend: Int, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): MapKey {
        val map_key = MapKey(note, bend, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(map_key)) {
            this.sample_data_map[map_key] = this.generate_new(note, bend, sample, instrument, preset)
        }
        return map_key
    }

    fun generate_new(note: Int, bend: Int, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        var pitch_shift = 1F
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        // TODO: Why did I do this check? I vaguely remember needing it but I need a note
        if (original_note != 255) {
            val tuning_cent: Int = (sample.tuning_cent
                ?: instrument.instrument?.global_sample?.tuning_cent
                ?: 0
            ) + (instrument.tuning_cent ?: 0) + (preset.global_zone?.tuning_cent ?: 0)

            // Kludge: modulators arent implemented yet, so this is still needed for tuning
            val mod_env_pitch: Float = ((sample.mod_env_pitch
                ?: instrument.instrument?.global_sample?.mod_env_pitch
                ?: 0
            ) + (instrument.mod_env_pitch ?: 0) + (preset.global_zone?.mod_env_pitch ?: 0)).toFloat()

            var tuning_semi: Float = ((sample.tuning_semi
                ?: instrument.instrument?.global_sample?.tuning_semi
                ?: 0
            ) + (instrument.tuning_semi ?: 0) + (preset.global_zone?.tuning_semi ?: 0)).toFloat()

            tuning_semi += (tuning_cent + mod_env_pitch) / 100F

            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val required_pitch = 2F.pow((note.toFloat() + tuning_semi + (bend.toFloat() / 512F)) / 12F)
            pitch_shift = required_pitch / original_pitch
        }

        if (sample.sample!!.sampleRate != this.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toFloat() / this.sample_rate.toFloat())
        }

        val data = sample.sample!!.data!!

        val attenuation: Double = (sample.attenuation
            ?: instrument.instrument?.global_sample?.attenuation
            ?: 0.0
        ) + (instrument.attenuation ?: 0.0) + (preset.global_zone?.attenuation ?: 0.0)

        val vol_env_delay: Double = (sample.vol_env_delay
            ?: instrument.instrument?.global_sample?.vol_env_delay
            ?: 0.0
        ) + (instrument.vol_env_delay ?: 0.0) + (preset.global_zone?.vol_env_delay ?: 0.0)

        val vol_env_attack: Double = (sample.vol_env_attack
            ?: instrument.instrument?.global_sample?.vol_env_attack
            ?: -12000.0
        ) + (instrument.vol_env_attack ?: 0.0) + (preset.global_zone?.vol_env_attack ?: 0.0)
        val vol_env_hold: Double = (sample.vol_env_hold
            ?: instrument.instrument?.global_sample?.vol_env_hold
            ?: -12000.0
        ) + (instrument.vol_env_hold ?: 0.0) + (preset.global_zone?.vol_env_hold ?: 0.0)

        val vol_env_decay: Double = (sample.vol_env_decay
            ?: instrument.instrument?.global_sample?.vol_env_decay
            ?: -12000.0
        ) + (instrument.vol_env_decay ?: 0.0) + (preset.global_zone?.vol_env_decay ?: 0.0)

        val vol_env_release: Double = (sample.vol_env_release
            ?: instrument.instrument?.global_sample?.vol_env_release
            ?: -12000.0
        ) + (instrument.vol_env_release ?: 0.0) + (preset.global_zone?.vol_env_release ?: 0.0)

        val vol_env_sustain: Double = (sample.vol_env_sustain
            ?: instrument.instrument?.global_sample?.vol_env_sustain
            ?: 0.0
        ) + (instrument.vol_env_sustain ?: 0.0) + (preset.global_zone?.vol_env_sustain ?: 0.0)

        val mod_lfo_freq: Double = (sample.mod_lfo_freq
            ?: instrument.instrument?.global_sample?.mod_lfo_freq
            ?: 0.0
        ) + (instrument.mod_lfo_freq ?: 0.0) + (preset.global_zone?.mod_lfo_freq ?: 0.0)
        val mod_lfo_volume: Int = (sample.mod_lfo_volume
            ?: instrument.instrument?.global_sample?.mod_lfo_volume
            ?: 0
        ) + (instrument.mod_lfo_volume ?: 0) + (preset.global_zone?.mod_lfo_volume ?: 0)

        val max_values = mutableListOf<Short>()
        data.forEachIndexed { i: Int, frame: Short ->
            val d = i / (this@SampleHandleGenerator.buffer_size / 3)
            while (max_values.size <= d) {
                max_values.add(0)
            }

            val abs_frame = abs(frame.toInt())
            if (abs_frame > max_values[d]) {
                max_values[d] = abs_frame.toShort()
            }
        }

        val max_values_floats = Array(max_values.size) {
            max_values[it].toFloat() / Short.MAX_VALUE.toFloat()
        }

        val filter_cutoff: Double = (sample.filter_cutoff
            ?: instrument.instrument?.global_sample?.filter_cutoff
            ?: 13500.0
        ) + (instrument.filter_cutoff ?: 0.0) + (preset.global_zone?.filter_cutoff ?: 0.0)

        return SampleHandle(
            data = data,
            sample_rate = sample_rate,
            pan = (sample.pan ?: instrument.pan ?: preset.global_zone?.pan ?: 0.0) * 100.0/ 500.0,
            pitch_shift = pitch_shift,
            sustain_attenuation = (10.0).pow(vol_env_sustain / -20.0).toFloat(),
            attenuation = (10.0).pow(attenuation / -20.0).toFloat(),
            stereo_mode = sample.sample!!.sampleType,
            loop_points = if (sample.sampleMode != null && sample.sampleMode!! and 1 == 1) {
                val start = (sample.sample!!.loopStart.toFloat() / pitch_shift)
                val size = (sample.sample!!.loopEnd - sample.sample!!.loopStart).toFloat() / pitch_shift

                Pair( start.toInt(), (start + size).toInt() )
            } else {
                null
            },
            delay_frames = ((this.sample_rate.toDouble() * vol_env_delay)).toInt(),
            attack_frame_count = (this.sample_rate.toDouble() * vol_env_attack ),
            hold_frame_count = ((this.sample_rate.toDouble() * vol_env_hold )).toInt(),
            decay_frame_count = (this.sample_rate.toDouble() * vol_env_decay),
            release_size = ((this.sample_rate.toDouble() * vol_env_release)).toFloat(),
            max_values = max_values_floats,
            filter_cutoff = filter_cutoff
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
