//
// Created by pent on 8/12/25.
//

#ifndef PAGAN_WAVEGENERATORCACHE_H
#define PAGAN_WAVEGENERATORCACHE_H
#include <unordered_map>


class DelayHandle {
    public:
        std::vector<float> frames_left;
        int get_position_left = 0;
        std::vector<float> frames_right;
        int get_position_right = 0;

        void put_right(int i, float value, float fade) {
            int original_size = this->frames_right.size();
            if (i >= original_size) {
                this->frames_right.resize(i + 1);
                for (int x = original_size; x <= i; x++) {
                    this->frames_right[x] = 0;
                }
            }
            this->frames_right[i] += (value * fade);
        }

        float get_right() {
            return this->frames_right[this->get_position_right++];
        }

        void put_left(int i, float value, float fade) {
            int original_size = this->frames_left.size();
            if (i >= original_size) {
                this->frames_left.resize(i + 1);
                for (int x = original_size; x <= i; x++) {
                    this->frames_left[x] = 0;
                }
            }
            this->frames_left[i] += (value * fade);
        }

        float get_left() {
            return this->frames_left[this->get_position_left++];
        }
};

class WaveGeneratorCache {
    public:
        int* delay_keys;
        DelayHandle* delays;
        int size = 0;
        ~WaveGeneratorCache() {
        }
        bool has_key(int key) {
            for (int i = 0; i < this->size; i++) {
                if (this->delay_keys[i] == key) {
                    return true;
                }
            }
            return false;
        }
        void new_delay_handle(int key) {
            this->delay_keys[this->size] = key;
            this->delays[this->size] = DelayHandle();
            this->size++;
        }
        DelayHandle* get_delay_handle(int key) const {
            for (int i = 0; i < this->size; i++) {
                if (this->delay_keys[i] == key) {
                    return &this->delays[i];
                }
            }
            return nullptr;
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
    if (!generator_cache->has_key(effect_buffer->buffer_id)) {
        __android_log_print(ANDROID_LOG_DEBUG, "", "fff %d", "UNT");
        generator_cache->new_delay_handle(effect_buffer->buffer_id);
    }

    DelayHandle* delay_handle = generator_cache->get_delay_handle(effect_buffer->buffer_id);

    for (int i = 0; i < frame_count; i++) {
        float* frame = effect_buffer->get_next();
        float next_frame = frame[0];
        float repeat_count = frame[1];
        float fade = frame[2];
        delay_handle->put_left(next_frame, frame[i], fade);
        delay_handle->put_right(next_frame, frame[i + frame_count], fade);

        working_array[i] += delay_handle->get_left();
        working_array[i + frame_count] += delay_handle->get_right();
    }
}


#endif //PAGAN_WAVEGENERATORCACHE_H
