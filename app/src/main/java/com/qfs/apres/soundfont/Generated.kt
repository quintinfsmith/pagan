package com.qfs.apres.soundfont

abstract class Generated {
    var key_range: Pair<Int, Int>? = null
    var velocity_range: Pair<Int, Int>? = null
    var attenuation: Double? = null
    var pan: Double? = null
    var tuning_semi: Int? = null
    var tuning_cent: Int? = null
    var scale_tuning: Int? = null
    var filter_cutoff: Double? = null
    var filter_resonance: Double? = null
    var vol_env_delay: Double? = null
    var vol_env_attack: Double? = null
    var vol_env_hold: Double? = null
    var vol_env_decay: Double? = null
    var vol_env_sustain: Double? = null
    var vol_env_release: Double? = null
    var key_vol_env_hold: Int? = null
    var key_vol_env_decay: Int? = null
    var mod_env_attack: Double? = null
    var mod_env_hold: Double? = null
    var mod_env_delay: Double? = null
    var mod_env_decay: Double? = null
    var mod_env_sustain: Double? = null
    var mod_env_release: Double? = null
    var mod_env_pitch: Int? = null
    var mod_env_filter: Int? = null
    var key_mod_env_hold: Int? = null
    var key_mod_env_decay: Int? = null
    var mod_lfo_delay: Double? = null
    var mod_lfo_freq: Double? = null
    var mod_lfo_pitch: Int? = null
    var mod_lfo_filter: Int? = null
    var mod_lfo_volume: Int? = null
    var vib_lfo_delay: Double? = null
    var vib_lfo_freq: Double? = null
    var vib_lfo_pitch: Int? = null
    var chorus: Double? = null
    var reverb: Double? = null
}