#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include <vector>
#include <opencl-c.h>
#include "Generator.h"
#include "GeneratorOperation.h"

#ifndef PAGAN_GENERATED_H
#define PAGAN_GENERATED_H

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

    public:
        void apply_generators(std::vector<Generator> generators) {
            for (auto & generator : generators) {
                switch (generator.sfGenOp) {
                    case GeneratorOperation::ModLFOPitch: {
                        this->mod_lfo_pitch = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::VibLFOPitch: {
                        this->vib_lfo_pitch = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::ModEnvPitch: {
                        this->mod_env_pitch = generator.get_int_signed();
                        break;
                    }
                    case GeneratorOperation::FilterCutoff: {
                        this->filter_cutoff = generator.get_timecent() * 8.176;
                        break;
                    }
                    case GeneratorOperation::FilterResonance: {
                        this->filter_resonance = (float)generator.get_int();
                        break;
                    }
                    case GeneratorOperation::ModLFOFilter: {
                        this->mod_lfo_filter = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::ModEnvFilter: {
                        this->mod_env_filter = generator.get_int_signed();
                        break;
                    }
                    case GeneratorOperation::ModLFOToVolume: {
                        this->mod_lfo_to_volume = (float)min(1000, max(generator.get_int_signed(), 0)) / 10;
                        break;
                    }
                    case GeneratorOperation::Chorus: {
                        this->chorus = (float)generator.get_int() / 10;
                        break;
                    }
                    case GeneratorOperation::Reverb: {
                        break;
                    }
                    case GeneratorOperation::Pan: {
                        break;
                    }
                    case GeneratorOperation::ModLFODelay: {
                        break;
                    }
                    case GeneratorOperation::ModLFOFrequency: {
                        break;
                    }
                    case GeneratorOperation::VibLFODelay: {
                        break;
                    }
                    case GeneratorOperation::VibLFOFrequency: {
                        break;
                    }
                    case GeneratorOperation::ModEnvDelay: {
                        break;
                    }
                    case GeneratorOperation::ModEnvAttack: {
                        break;
                    }
                    case GeneratorOperation::ModEnvHold: {
                        break;
                    }
                    case GeneratorOperation::ModEnvDecay: {
                        break;
                    }
                    case GeneratorOperation::ModEnvSustain: {
                        break;
                    }
                    case GeneratorOperation::ModEnvRelease: {
                        break;
                    }
                    case GeneratorOperation::KeyModEnvHold: {
                        break;
                    }
                    case GeneratorOperation::KeyModEnvDecay: {
                        break;
                    }
                    case GeneratorOperation::VolEnvDelay: {
                        break;
                    }
                    case GeneratorOperation::VolEnvAttack: {
                        break;
                    }
                    case GeneratorOperation::VolEnvHold: {
                        break;
                    }
                    case GeneratorOperation::VolEnvDecay: {
                        break;
                    }
                    case GeneratorOperation::VolEnvSustain: {
                        break;
                    }
                    case GeneratorOperation::VolEnvRelease: {
                        break;
                    }
                    case GeneratorOperation::KeyVolEnvHold: {
                        break;
                    }
                    case GeneratorOperation::KeyVolEnvDecay: {
                        break;
                    }
                    case GeneratorOperation::KeyRange: {
                        break;
                    }
                    case GeneratorOperation::VelocityRange: {
                        break;
                    }
                    case GeneratorOperation::Attenuation: {
                        break;
                    }
                    case GeneratorOperation::TuningFine: {
                        break;
                    }
                    case GeneratorOperation::TuningCoarse: {
                        break;
                    }
                    case GeneratorOperation::ScaleTuning: {
                        break;
                    }
                    default: {
                        break;
                    }
                }

            }
        }
};

#endif //PAGAN_GENERATED_H
