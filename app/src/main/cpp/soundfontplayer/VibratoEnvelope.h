//
// Created by pent on 5/31/25.
//

#ifndef PAGAN_VIBRATOENVELOPE_H
#define PAGAN_VIBRATOENVELOPE_H


class VibratoEnvelope {
    float frequency;
    float delay;
    float pitch;
    public:
        VibratoEnvelope(float frequency, float delay, float pitch) {
            this->pitch = pitch;
            this->frequency = frequency;
            this->delay = delay;
        }
};


#endif //PAGAN_VIBRATOENVELOPE_H
