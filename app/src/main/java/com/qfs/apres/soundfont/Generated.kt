package com.qfs.apres.soundfont

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
}