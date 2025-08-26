//
// Created by pent on 8/12/25.
// Work in Progress
//

#ifndef PAGAN_WAVEGENERATORCACHE_H
#define PAGAN_WAVEGENERATORCACHE_H
#include <unordered_map>


void apply_pan(EffectProfileBuffer* effect_buffer, float* working_array, int frames) {
    for (int i = 0; i < frames; i++) {
        float pan_value = effect_buffer->get_next()[0];
        working_array[i] *= 1 + pan_value;
        working_array[i + frames] *= (-1 + pan_value) * -1;
    }
}

void apply_volume(EffectProfileBuffer* effect_buffer, float* working_array, int frames) {
    for (int i = 0; i < frames; i++) {
        float volume = effect_buffer->get_next()[0];
        working_array[i] *= volume;
        working_array[i + frames] *= volume;
    }
}

void apply_bandpass(EffectProfileBuffer* effect_buffer, float* working_array, int frame_count) {
}

void apply_delay(EffectProfileBuffer* effect_buffer, float* working_array, int frame_count) {
   // if (!generator_cache->has_delay_handle(effect_buffer->buffer_id)) {
   //  //   generator_cache->new_delay_handle(effect_buffer->buffer_id);
   // }

    //DelayHandle* delay_handle = generator_cache->get_delay_handle(effect_buffer->buffer_id);

    //for (int i = 0; i < frame_count; i++) {
    //    float* frame = effect_buffer->get_next();
    //    float next_frame = frame[0];
    //    float repeat_count = frame[1];
    //    float fade = frame[2];
    //    delay_handle->put_left(next_frame, frame[i], fade);
    //    delay_handle->put_right(next_frame, frame[i + frame_count], fade);

    //    working_array[i] += delay_handle->get_left();
    //    working_array[i + frame_count] += delay_handle->get_right();
    //}
}


#endif //PAGAN_WAVEGENERATORCACHE_H
