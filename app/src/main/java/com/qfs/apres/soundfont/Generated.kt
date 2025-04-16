package com.qfs.apres.soundfont

import com.qfs.apres.soundfont.Generator.Operation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class Generated {
    companion object {
        var next_uid = 0
        fun gen_uid(): Int {
            return next_uid++
        }
    }
    val uid = Generated.gen_uid()

    var key_range: Pair<Int, Int>? = null
    var velocity_range: Pair<Int, Int>? = null
    var attenuation: Float? = null
    var pan: Float? = null
    var tuning_semi: Int? = null
    var tuning_cent: Int? = null
    var scale_tuning: Int? = null
    var filter_cutoff: Float? = null
    var filter_resonance: Float? = null
    var vol_env_delay: Float? = null
    var vol_env_attack: Float? = null
    var vol_env_hold: Float? = null
    var vol_env_decay: Float? = null
    var vol_env_sustain: Float? = null
    var vol_env_release: Float? = null
    var key_vol_env_hold: Int? = null
    var key_vol_env_decay: Int? = null
    var mod_env_attack: Float? = null
    var mod_env_hold: Float? = null
    var mod_env_delay: Float? = null
    var mod_env_decay: Float? = null
    var mod_env_sustain: Float? = null
    var mod_env_release: Float? = null
    var mod_env_pitch: Int? = null
    var mod_env_filter: Int? = null
    var key_mod_env_hold: Int? = null
    var key_mod_env_decay: Int? = null
    var mod_lfo_delay: Float? = null
    var mod_lfo_freq: Float? = null
    var mod_lfo_pitch: Int? = null
    var mod_lfo_filter: Int? = null
    var mod_lfo_to_volume: Float? = null
    var vib_lfo_delay: Float? = null
    var vib_lfo_freq: Float? = null
    var vib_lfo_pitch: Int? = null
    var chorus: Float? = null
    var reverb: Float? = null
    abstract fun apply_generator(generator: Generator)

    fun apply_generators(generators: List<Generator>) {
        for (i in generators.indices) {
            val generator = generators[i]
            when (generator.get_operation()) {
                Operation.ModLFOPitch -> {
                    this.mod_lfo_pitch = generator.asInt()
                }

                Operation.VibLFOPitch -> {
                    this.vib_lfo_pitch = generator.asInt()
                }

                Operation.ModEnvPitch -> {
                    this.mod_env_pitch = generator.asIntSigned()
                }

                Operation.FilterCutoff -> {
                    this.filter_cutoff = generator.asTimecent() * 8.176f
                }

                Operation.FilterResonance -> {
                    this.filter_resonance = generator.asInt().toFloat()
                }

                Operation.ModLFOFilter -> {
                    this.mod_lfo_filter = generator.asInt()
                }

                Operation.ModEnvFilter -> {
                    this.mod_env_filter = generator.asIntSigned()
                }

                Operation.ModLFOToVolume -> {
                    this.mod_lfo_to_volume =
                        min(1000, max(generator.asIntSigned(), 0)).toFloat() / 10F
                }

                Operation.Chorus -> {
                    this.chorus = generator.asInt().toFloat() / 10F
                }

                Operation.Reverb -> {
                    this.reverb = (generator.asInt().toFloat()) / 10F
                }

                Operation.Pan -> {
                    this.pan = (generator.asIntSigned().toFloat()) / 10F
                }

                Operation.ModLFODelay -> {
                    this.mod_lfo_delay = generator.asTimecent()
                }

                Operation.ModLFOFrequency -> {
                    this.mod_lfo_freq = generator.asTimecent() * 8.176F
                }

                Operation.VibLFODelay -> {
                    this.vib_lfo_delay = generator.asTimecent()
                }

                Operation.VibLFOFrequency -> {
                    this.vib_lfo_freq = generator.asTimecent() * 8.176F
                }

                Operation.ModEnvDelay -> {
                    this.mod_env_delay = generator.asTimecent()
                }

                Operation.ModEnvAttack -> {
                    this.mod_env_attack = generator.asTimecent()
                }

                Operation.ModEnvHold -> {
                    this.mod_env_hold = generator.asTimecent()
                }

                Operation.ModEnvDecay -> {
                    this.mod_env_decay = generator.asTimecent()
                }

                Operation.ModEnvSustain -> {
                    this.mod_env_sustain =
                        min(1000, max(generator.asIntSigned(), 0)).toFloat() / 10F
                }

                Operation.ModEnvRelease -> {
                    this.mod_env_release = generator.asTimecent()
                }

                Operation.KeyModEnvHold -> {
                    this.key_mod_env_hold = generator.asInt()
                }

                Operation.KeyModEnvDecay -> {
                    this.key_mod_env_decay = generator.asInt()
                }

                Operation.VolEnvDelay -> {
                    this.vol_env_delay = generator.asTimecent()
                }

                Operation.VolEnvAttack -> {
                    this.vol_env_attack = generator.asTimecent()
                }

                Operation.VolEnvHold -> {
                    this.vol_env_hold = generator.asTimecent()
                }

                Operation.VolEnvDecay -> {
                    this.vol_env_decay = generator.asTimecent()
                }

                Operation.VolEnvSustain -> {
                    this.vol_env_sustain =
                        min(1000, max(generator.asIntSigned(), 0)).toFloat() / 10F
                }

                Operation.VolEnvRelease -> {
                    this.vol_env_release = generator.asTimecent()
                }

                Operation.KeyVolEnvHold -> {
                    this.key_vol_env_hold = generator.asInt()
                }

                Operation.KeyVolEnvDecay -> {
                    this.key_vol_env_decay = generator.asInt()
                }

                Operation.KeyRange -> {
                    this.key_range = generator.asPair()
                }

                Operation.VelocityRange -> {
                    this.velocity_range = generator.asPair()
                }

                Operation.Attenuation -> {
                    // The spec appears to indicate a value range of 0 -> 1440 centibels,
                    // but looking at the fluid font, it has some samples with negative attenuation
                    // I'll treat the data type as signed, but still use the absolute value since that sounds right
                    // when I listen to the samples
                    this.attenuation = abs(generator.asIntSigned().toFloat() / 10)
                }

                Operation.TuningFine -> {
                    this.tuning_cent = generator.asIntSigned()
                }

                Operation.TuningCoarse -> {
                    this.tuning_semi = generator.asIntSigned()
                }

                Operation.ScaleTuning -> {
                    this.scale_tuning = generator.asInt()
                }

                else -> this.apply_generator(generator)
            }
        }
    }
}