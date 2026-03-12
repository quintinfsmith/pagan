//
// Created by pent on 10/2/25.
//

#ifndef PAGAN_PITCHEFFECTBUFFER_H
#define PAGAN_PITCHEFFECTBUFFER_H
#include "EffectProfileBuffer.h"

class PitchEffectBuffer: public EffectProfileBuffer {
    public:
        PitchEffectBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) { }
        explicit PitchEffectBuffer(PitchEffectBuffer* original): EffectProfileBuffer(original) {}
        void apply(float* working_array, int array_size) override {
            for (int i = 0; i < array_size; i++) {
                float pan_value = this->get_next()[0];
                working_array[i] *= 1 + pan_value;
                working_array[i + array_size] *= (-1 + pan_value) * -1;
            }
        }
};

#endif //PAGAN_PANBUFFER_H
