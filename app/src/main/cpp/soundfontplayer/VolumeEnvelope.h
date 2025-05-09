//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_VOLUMEENVELOPE_H
#define PAGAN_VOLUMEENVELOPE_H
#include <cmath>

class VolumeEnvelope {
public:
    int sample_rate;
    float delay;
    float attack;
    float hold;
    float decay;
    float release;
    int frames_delay;
    int frames_attack;
    int frames_hold;
    int frames_decay;
    int frames_release;
    float sustain_attenuation;
    float true_sustain_attenuation;

    explicit VolumeEnvelope(
            int sample_rate,
            float delay,
            float attack,
            float hold,
            float decay,
            float release,
            float sustain_attenuation
    ) {
        this->delay = delay;
        this->attack = attack;
        this->hold = hold;
        this->decay = decay;
        this->release = release;
        this->sustain_attenuation = sustain_attenuation;
        this->true_sustain_attenuation = pow((float)10, this->sustain_attenuation);
        this->set_sample_rate(sample_rate);
    }

    void set_sample_rate(int sample_rate) {
        this->sample_rate = sample_rate;
        auto float_rate = (float)sample_rate;
        this->frames_delay = (int)(float_rate * this->delay);
        this->frames_hold = (int)(float_rate * this->hold);
        this->frames_attack = (int)(float_rate * this->attack);
        this->frames_hold = (int)(float_rate * this->hold);
        this->frames_decay = (int)(float_rate * this->decay);
        this->frames_release = (int)(float_rate * this->release);
    }

    void copy_to(VolumeEnvelope* other) {
        other->delay = this->delay;
        other->attack = this->attack;
        other->hold = this->hold;
        other->decay = this->decay;
        other->release = this->release;
        other->sustain_attenuation = this->sustain_attenuation;
        other->true_sustain_attenuation = pow((float)10, this->sustain_attenuation);
        other->set_sample_rate(this->sample_rate);
    }
};


#endif //PAGAN_VOLUMEENVELOPE_H
