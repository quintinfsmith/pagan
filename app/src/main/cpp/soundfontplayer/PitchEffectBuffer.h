#ifndef PAGAN_PITCHEFFECTBUFFER_H
#define PAGAN_PITCHEFFECTBUFFER_H
#include "EffectProfileBuffer.h"

class PitchEffectBuffer: public EffectProfileBuffer {
    public:
        PitchEffectBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) { }
        explicit PitchEffectBuffer(PitchEffectBuffer* original): EffectProfileBuffer(original) {}
        void apply(float* working_array, int array_size) override { }
        float peek() {
            return this->current_value[0];
        }
};

#endif //PAGAN_PITCHEFFECTBUFFER_H
