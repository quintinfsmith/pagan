//
// Created by pent on 8/12/25.
//

#ifndef PAGAN_WAVEGENERATORCACHE_H
#define PAGAN_WAVEGENERATORCACHE_H
#include <unordered_map>

// TODO: Double check left/right offset
class DelayHandle {};
//class DelayHandle {
//    public:
//        float* frames;
//        int size = 0;
//        int position = 0;
//
//        void put_frame(int i, float left_value, float right_value, float fade, int offset = 0) {
//            if (i >= this->size) {
//                for (int x = this->size; x <= i; x++) {
//                    this->frames[2*x] = 0;
//                    this->frames[(2*x) + 1] = 0;
//                }
//                this->size = i + 1;
//            }
//
//            this->frames[(i / 2)] += (right_value * fade);
//            this->frames[(i / 2) + 1] += (left_value * fade);
//        }
//
//        std::tuple<float, float> get_frame() {
//            int frame = this->position++ / 2;
//            return std::tuple(
//                this->frames[frame],
//                this->frames[frame + 1]
//            );
//        }
//};

class BandPassFilter {
    float high;
    float low;
};

class WaveGeneratorCache {
    std::unordered_map<int, BandPassFilter*>* current_band_pass;
    std::unordered_map<int, DelayHandle*>* current_delay_handle;
    public:
        void init() {
            this->current_band_pass = (std::unordered_map<int, BandPassFilter*>*)malloc(sizeof(std::unordered_map<int, BandPassFilter*>));
            this->current_delay_handle = (std::unordered_map<int, DelayHandle*>*)malloc(sizeof(std::unordered_map<int, DelayHandle*>));
        }
        ~WaveGeneratorCache() {
            delete this->current_band_pass;
            delete this->current_delay_handle;
        }

        bool has_delay_handle(int key) {
            __android_log_print(ANDROID_LOG_DEBUG, "", "UNTXX%ld", (long)this);
            __android_log_print(ANDROID_LOG_DEBUG, "", "UTxxxXX - %ld -", (long)this->current_delay_handle);
            if (this->current_delay_handle == nullptr) {
                __android_log_print(ANDROID_LOG_DEBUG, "", "UNTXX%d", 0);
                this->current_delay_handle = (std::unordered_map<int, DelayHandle*>*)malloc(sizeof(std::unordered_map<int, DelayHandle*>));
            }
            return this->current_delay_handle->find(key) != this->current_delay_handle->end();
        }
        //int* delay_keys;
        //DelayHandle* delays;
        //int size = 0;


        //bool has_key(int key) {
        //    for (int i = 0; i < this->size; i++) {
        //        if (this->delay_keys[i] == key) {
        //            return true;
        //        }
        //    }
        //    return false;
        //}

        //void new_delay_handle(int key) {
        //    this->delay_keys[this->size] = key;
        //    this->delays[this->size] = DelayHandle();
        //    this->size++;
        //}

        //DelayHandle* get_delay_handle(int key) {
        //    for (int i = 0; i < this->size; i++) {
        //        if (this->delay_keys[i] == key) {
        //            return &this->delays[i];
        //        }
        //    }
        //    return nullptr;
        //}
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

void apply_bandpass(WaveGeneratorCache* generator_cache, ProfileBuffer* effect_buffer, float* working_array, int frame_count) {
}

void apply_delay(WaveGeneratorCache* generator_cache, ProfileBuffer* effect_buffer, float* working_array, int frame_count) {
    __android_log_print(ANDROID_LOG_DEBUG, "", "UNTXX%d", 1);
    if (!generator_cache->has_delay_handle(effect_buffer->buffer_id)) {
        __android_log_print(ANDROID_LOG_DEBUG, "", "UNTXX%d", 2);
     //   generator_cache->new_delay_handle(effect_buffer->buffer_id);
    }

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
