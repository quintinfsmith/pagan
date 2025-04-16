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
                        this->reverb = (float)generator.get_int() / 10;
                        break;
                    }
                    case GeneratorOperation::Pan: {
                        this->pan = (float)generator.get_int_signed() / 10;
                        break;
                    }
                    case GeneratorOperation::ModLFODelay: {
                        this->mod_lfo_delay = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::ModLFOFrequency: {
                        this->mod_lfo_freq = generator.get_timecent() * 8.176;
                        break;
                    }
                    case GeneratorOperation::VibLFODelay: {
                        this->vib_lfo_delay = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::VibLFOFrequency: {
                        this->vib_lfo_freq = generator.get_timecent() * 8.176;
                        break;
                    }
                    case GeneratorOperation::ModEnvDelay: {
                        this->mod_env_delay = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::ModEnvAttack: {
                        this->mod_env_attack = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::ModEnvHold: {
                        this->mod_env_hold = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::ModEnvDecay: {
                        this->mod_env_decay = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::ModEnvSustain: {
                        this->mod_env_sustain = (float)min(1000, max(generator.get_int_signed(), 0)) / 10;
                        break;
                    }
                    case GeneratorOperation::ModEnvRelease: {
                        this->mod_env_release = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::KeyModEnvHold: {
                        this->key_mod_env_hold = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::KeyModEnvDecay: {
                        this->key_mod_env_decay = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::VolEnvDelay: {
                        this->vol_env_delay = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::VolEnvAttack: {
                        this->vol_env_attack = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::VolEnvHold: {
                        this->vol_env_hold = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::VolEnvDecay: {
                        this->vol_env_decay = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::VolEnvSustain: {
                        this->vol_env_sustain = (float)min(1000, max(generator.get_int_signed(), 0)) / 10;
                        break;
                    }
                    case GeneratorOperation::VolEnvRelease: {
                        this->vol_env_release = generator.get_timecent();
                        break;
                    }
                    case GeneratorOperation::KeyVolEnvHold: {
                        this->key_vol_env_hold = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::KeyVolEnvDecay: {
                        this->key_vol_env_decay = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::KeyRange: {
                        this->key_range = generator.get_pair();
                        break;
                    }
                    case GeneratorOperation::VelocityRange: {
                        this->velocity_range = generator.get_pair();
                        break;
                    }
                    case GeneratorOperation::Attenuation: {
                        // The spec appears to indicate a value range of 0 -> 1440 centibels,
                        // but looking at the fluid font, it has some samples with negative attenuation
                        // I'll treat the data type as signed, but still use the absolute value since that sounds right
                        // when I listen to the samples
                        this->attenuation = std::abs((float)generator.get_int_signed() / 10);
                        break;
                    }
                    case GeneratorOperation::TuningFine: {
                        this->tuning_cent = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::TuningCoarse: {
                        this->tuning_semi = generator.get_int();
                        break;
                    }
                    case GeneratorOperation::ScaleTuning: {
                        this->scale_tuning = generator.get_int();
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
