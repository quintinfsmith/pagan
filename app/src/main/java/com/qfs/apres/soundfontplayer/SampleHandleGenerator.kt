package com.qfs.apres.soundfontplayer
import com.qfs.apres.event.NoteOn
import com.qfs.apres.soundfont.InstrumentSample
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.PresetInstrument
import kotlin.math.abs
import kotlin.math.pow

class SampleHandleGenerator(var sample_rate: Int, var buffer_size: Int) {
    // Hash ignores velocity since velocity isn't baked into sample data
    data class MapKey(
        var note: Int,
        var sample: Int,
        var instrument: Int,
        var preset: Int
    )

    var sample_data_map = HashMap<MapKey, SampleHandle>()

    fun get(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        val map_key = this.cache_new(event, sample, instrument, preset)
        return SampleHandle(this.sample_data_map[map_key]!!)
    }

    fun cache_new(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): MapKey {
        val map_key = MapKey(event.get_note(), sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(map_key)) {
            this.sample_data_map[map_key] = this.generate_new(event, sample, instrument, preset)
        }
        return map_key
    }

    fun generate_new(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        var pitch_shift = 1F
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            // Kludge: modulators arent implemented yet, so this is still needed for tuning
            val mod_env_pitch = (sample.mod_env_pitch ?: instrument.mod_env_pitch ?: preset.global_zone?.mod_env_pitch ?: 0).toFloat()
            var tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            tuning_semi += (tuning_cent + mod_env_pitch) / 100F
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val required_pitch = 2F.pow((event.get_note().toFloat() + tuning_semi) / 12F)
            pitch_shift = required_pitch / original_pitch
        }

        if (sample.sample!!.sampleRate != this.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toFloat() / this.sample_rate.toFloat())
        }

        val data = sample.sample!!.data!!

        val attenuation: Double = preset.global_zone?.attenuation
            ?: instrument.instrument?.global_sample?.attenuation
            ?: instrument.attenuation
            ?: sample.attenuation
            ?: 0.0

        val vol_env_delay: Double = preset.global_zone?.vol_env_delay
            ?: instrument.instrument?.global_sample?.vol_env_delay
            ?: instrument.vol_env_delay
            ?: sample.vol_env_delay
            ?: 0.0

        val vol_env_attack: Double = preset.global_zone?.vol_env_attack
            ?: instrument.instrument?.global_sample?.vol_env_attack
            ?: instrument.vol_env_attack
            ?: sample.vol_env_attack
            ?: 0.0

        val vol_env_hold: Double = preset.global_zone?.vol_env_hold
            ?: instrument.instrument?.global_sample?.vol_env_hold
            ?: instrument.vol_env_hold
            ?: sample.vol_env_hold
            ?: 0.0

        val vol_env_decay: Double = preset.global_zone?.vol_env_decay
            ?: instrument.instrument?.global_sample?.vol_env_decay
            ?: instrument.vol_env_decay
            ?: sample.vol_env_decay
            ?: 0.0

        val vol_env_release: Double = preset.global_zone?.vol_env_release
            ?: instrument.instrument?.global_sample?.vol_env_release
            ?: instrument.vol_env_release
            ?: sample.vol_env_release
            ?: 0.0

        // TODO: Commenting out for now. lfo's aren't current being used
        //val freq_mod_lfo: Double? = preset.global_zone?.mod_lfo_freq
        //    ?: instrument.instrument?.global_sample?.mod_lfo_freq
        //    ?: instrument.mod_lfo_freq
        //    ?: sample.mod_lfo_freq
        //val lfo_data: ShortArray? = if (freq_mod_lfo is Double) {
        //    //val lfo_vol: Int = preset.global_zone?.mod_lfo_volume
        //    //    ?: instrument.instrument?.global_sample?.mod_lfo_volume
        //    //    ?: instrument.mod_lfo_volume
        //    //    ?: sample.mod_lfo_volume
        //    //    ?: 0
        //    val level = .2

        //    val wave_length = this.sample_rate.toDouble() / freq_mod_lfo
        //    ShortArray(wave_length.toInt()) { i: Int ->
        //        val p = (i.toDouble() / wave_length)
        //        (sin(p * PI) * level * 0x7FFF.toDouble()).toInt().toShort()
        //    }
        //} else {
        //    null
        //}


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

        return SampleHandle(
            data = data,
            //lfo_data = lfo_data,
            lfo_data = null,
            pan = (sample.pan ?: instrument.pan ?: preset.global_zone?.pan ?: 0.0) * 100.0/ 500.0,
            pitch_shift = pitch_shift,
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
            attack_frame_count = ((this.sample_rate.toDouble() * vol_env_attack )).toInt(),
            hold_frame_count = ((this.sample_rate.toDouble() * vol_env_hold )).toInt(),
            decay_frame_count = ((this.sample_rate.toDouble() * vol_env_decay )).toInt(),
            release_size = ((this.sample_rate.toDouble() * vol_env_release)).toFloat(),
            max_values = max_values_floats,
            //filter_cutoff = sample.filter_cutoff ?: instrument.filter_cutoff
            //filter_cutoff = (20 .. 20000).random()
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
