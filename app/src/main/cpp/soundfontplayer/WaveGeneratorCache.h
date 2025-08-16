//
// Created by pent on 8/12/25.
//

#ifndef PAGAN_WAVEGENERATORCACHE_H
#define PAGAN_WAVEGENERATORCACHE_H
#include <unordered_map>


class DelayHandle {
    public:
        float* frames_left;
        int set_position_left = 0;
        int get_position_left = 0;
        float* frames_right;
        int set_position_right = 0;
        int get_position_right = 0;

        void put_right(int i, float value, float fade) {
            while (this->set_position_right <= i) {
                this->frames_right[this->set_position_right++] = 0;
            }
            this->frames_right[i] += (value * fade);
        }
        float get_right() {
            return this->frames_right[this->get_position_right++];
        }

        void put_left(int i, float value, float fade) {
            while (this->set_position_left <= i) {
                this->frames_left[this->set_position_left++] = 0;
            }
            this->frames_left[i] += (value * fade);
        }
        float get_left() {
            return this->frames_left[this->get_position_left++];
        }
};

class WaveGeneratorCache {
    public:
        std::unordered_map<int, DelayHandle*> delays;
        ~WaveGeneratorCache() {
            for (auto it = this->delays.begin(); it != this->delays.end(); it++) {
                delete it->second;
            }
        }
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

void apply_delay(WaveGeneratorCache* generator_cache, ProfileBuffer* effect_buffer, float* working_array, int frame_count) {
    if (generator_cache->delays.find(effect_buffer->buffer_id) == generator_cache->delays.end()) {
        generator_cache->delays[effect_buffer->buffer_id] = (DelayHandle*)malloc(sizeof(DelayHandle));
        generator_cache->delays[effect_buffer->buffer_id]->get_position_left = 0;
        generator_cache->delays[effect_buffer->buffer_id]->set_position_left = 0;
    }

    DelayHandle* delay_handle = generator_cache->delays[effect_buffer->buffer_id];

    for (int i = 0; i < frame_count; i++) {
        float* frame = effect_buffer->get_next();
        float next_frame = frame[0];
        float fade = frame[1];
        delay_handle->put_left(next_frame, frame[i], fade);
        delay_handle->put_right(next_frame, frame[i + frame_count], fade);

        working_array[i] += delay_handle->get_left();
        working_array[i + frame_count] += delay_handle->get_right();
    }
}


#endif //PAGAN_WAVEGENERATORCACHE_H
