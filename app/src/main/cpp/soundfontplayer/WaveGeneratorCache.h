//
// Created by pent on 8/12/25.
//

#ifndef PAGAN_WAVEGENERATORCACHE_H
#define PAGAN_WAVEGENERATORCACHE_H


class WaveGeneratorCache {

};

void apply_pan(ProfileBuffer* effect_buffer, float* working_array, int frames) {
    for (int i = 0; i < frames; i++) {
        float pan_value = effect_buffer->get_next()[0];
        working_array[i] *= 1 + pan_value;
        working_array[i + frames] *= (-1 + pan_value) * -1;
    }
}

void apply_volume(ProfileBuffer* effect_buffer, float* working_array, int frames) {
    for (int i = 0; i < frames; i++) {
        float volume = effect_buffer->get_next()[0];
        working_array[i] *= volume;
        working_array[i + frames] *= volume;
    }
}

void apply_delay(ProfileBuffer* effect_buffer, float* working_array, int frames) {
    for (int i = 0; i < frames; i++) {
        float* frame = effect_buffer->get_next();
        float next_frame = frame[0];
        float fade = frame[1];
        // working_array[i] *= de;
        // working_array[i + frames] *= volume;
    }
}


#endif //PAGAN_WAVEGENERATORCACHE_H
