//
// Created by pent on 4/15/25.
//

#include "Generated.h"
class Generated {
    std::optional<std::tuple<int, int>> key_range;
    std::optional<std::tuple<int, int>> velocity_range;
    std::optional<float> attenuation;
    std::optional<float> pan;
    std::optional<int> tuning_semi;
    std::optional<int> tuning_cent;
    std::optional<int> scale_tuning;
    std::optional<float> filter_cutoff;
    std::optional<float> filter_resonance;
    std::optional<float> vol_env_delay;
    std::optional<float> vol_env_attack;
    std::optional<float> vol_env_hold;
    std::optional<float> vol_env_decay;
    std::optional<float> vol_env_sustain;
    std::optional<float> vol_env_release;

    std::optional<int> key_vol_env_hold;
    std::optional<int> key_vol_env_decay;
    std::optional<float> mod_env_attack;
    std::optional<float> mod_env_hold;
    std::optional<float> mod_env_delay;
    std::optional<float> mod_env_decay;
    std::optional<float> mod_env_sustain;
    std::optional<float> mod_env_release;
    std::optional<int> mod_env_pitch;
    std::optional<int> mod_env_filter;
    std::optional<int> key_mod_env_hold;
    std::optional<int> key_mod_env_decay;

    std::optional<float> mod_lfo_delay;
    std::optional<float> mod_lfo_freq;
    std::optional<int> mod_lfo_pitch;
    std::optional<int> mod_lfo_filter;
    std::optional<float> mod_lfo_to_volume;

    std::optional<float> vib_lfo_delay;
    std::optional<float> vib_lfo_freq;
    std::optional<int> vib_lfo_pitch;

    std::optional<float> chorus;
    std::optional<float> reverb;
};
