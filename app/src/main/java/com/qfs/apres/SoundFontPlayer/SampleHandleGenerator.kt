package com.qfs.apres.SoundFontPlayer

import com.qfs.apres.InstrumentSample
import com.qfs.apres.Preset
import com.qfs.apres.PresetInstrument
import com.qfs.apres.riffreader.toUInt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

class SampleHandleGenerator {
    // Hash ignores velocity since velocity isn't baked into sample data
    data class MapKey(
        var note: Int,
        var sample: Int,
        var instrument: Int,
        var preset: Int
    )

    private var sample_data_map = HashMap<MapKey, SampleHandle>()

    fun get(event: SoundFontPlayer.TSNoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        val mapkey = MapKey(event.note, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(mapkey)) {
            this.sample_data_map[mapkey] = this.generate_new(event, sample, instrument, preset)
        }
        return SampleHandle(event, this.sample_data_map[mapkey]!!)
    }

    fun cache_new(event: SoundFontPlayer.TSNoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset) {
        val mapkey = MapKey(event.note, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(mapkey)) {
            this.sample_data_map[mapkey] = this.generate_new(event, sample, instrument, preset)
        }
    }

    private fun generate_new(event: SoundFontPlayer.TSNoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        var pitch_shift = 1F
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            val tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            val requiredPitch = 2F.pow((event.note.toFloat() + (tuning_semi + (tuning_cent / 1200))) / 12F)
            pitch_shift = requiredPitch / original_pitch
        }

        if (sample.sample!!.sampleRate != AudioTrackHandle.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toFloat() / AudioTrackHandle.sample_rate.toFloat())
        }

        val data = this.resample(sample.sample!!.data!!, pitch_shift)

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

        val release_mask_size = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_release)).toInt()

        val divisions = ceil(data.size.toFloat() / (AudioTrackHandle.buffer_size_in_bytes.toFloat() / 2F)).toInt() * 2
        val maximum_map = Array(divisions) { 0 }
        for (i in 0 until data.size / 2) {
            val a = toUInt(data[(2 * i)])
            val b = toUInt(data[(2 * i) + 1]) * 256
            val frame = (a + b).toShort().toInt()
            val mapped_position = (i * divisions / (data.size / 2))
            maximum_map[mapped_position] = max(abs(frame), maximum_map[mapped_position])
        }

        return SampleHandle(
            event.timestamp,
            data = data,
            stereo_mode = sample.sample!!.sampleType,
            loop_points = if (sample.sampleMode != null && sample.sampleMode!! and 1 == 1) {
                // Need to be even due to 2-byte words in sample
                var new_start = (sample.sample!!.loopStart.toFloat() / pitch_shift).toInt()
                if (new_start % 2 == 1) {
                    new_start -= 1
                }

                var new_end = (sample.sample!!.loopEnd.toFloat() / pitch_shift).toInt()
                if (new_end % 2 == 1) {
                    new_end -= 1
                }

                Pair(new_start, new_end)

            } else {
                null
            },
            delay_frames = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_delay)).toInt(),
            attack_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_attack ) * 2.0).toInt(),
            hold_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_hold ) * 2.0).toInt(),
            decay_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_decay ) * 2.0).toInt(),
            release_mask = Array(release_mask_size) {
                    i -> (release_mask_size - i - 1).toDouble() / release_mask_size.toDouble()
            },
            maximum_map = maximum_map
        )
    }

    fun resample(sample_data: ByteArray, pitch_shift: Float): ByteArray {
        // TODO: This is VERY Niave. Look into actual resampling algorithms
        var new_size = (sample_data.size / pitch_shift).toInt()
        if (new_size % 2 == 1) {
            new_size -= 1
        }

        val new_sample = ByteArray(new_size) { 0 }

        for (i in 0 until new_size / 2) {
            var i_offset = ((i * 2).toFloat() * pitch_shift).toInt()
            if (i_offset % 2 == 1) {
                i_offset -= 1
            }
            new_sample[i * 2] = sample_data[i_offset]
            new_sample[(i * 2) + 1] = sample_data[i_offset + 1]
        }

        return new_sample
    }

    fun clear_cache() {
        this.sample_data_map.clear()
    }
}
