//
// Created by pent on 10/2/25.
//

#ifndef PAGAN_VOLUMEBUFFER_H
#define PAGAN_VOLUMEBUFFER_H

#include "EffectProfileBuffer.h"

class VolumeBuffer: public EffectProfileBuffer {
    public:
        VolumeBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) { }
        explicit VolumeBuffer(VolumeBuffer* original): EffectProfileBuffer(original) { }
        void apply(float* working_array, int array_size) override {
            for (int i = 0; i < array_size; i++) {
                float volume = this->get_next()[0];
                working_array[i] *= volume;
                working_array[i + array_size] *= volume;
            }
        }
};

#endif //PAGAN_VOLUMEBUFFER_H
