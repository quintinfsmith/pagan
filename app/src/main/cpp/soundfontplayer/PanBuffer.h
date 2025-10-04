//
// Created by pent on 10/2/25.
//

#ifndef PAGAN_PANBUFFER_H
#define PAGAN_PANBUFFER_H
#include "EffectProfileBuffer.h"

class PanBuffer: public EffectProfileBuffer {
    public:
        PanBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) { }
        explicit PanBuffer(PanBuffer* original): EffectProfileBuffer(original) {}
        void apply(float* working_array, int array_size) override {
            for (int i = 0; i < array_size; i++) {
                float pan_value = this->get_next()[0];
                working_array[i] *= 1 + pan_value;
                working_array[i + array_size] *= (-1 + pan_value) * -1;
            }
        }
};

#endif //PAGAN_PANBUFFER_H
