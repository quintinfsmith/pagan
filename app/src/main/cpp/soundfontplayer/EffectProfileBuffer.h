//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_EFFECTPROFILEBUFFER_H
#define PAGAN_EFFECTPROFILEBUFFER_H
#include "ProfileBufferFrame.h"
#include <vector>
#include "ControllerEventData.h"
#include "SampleHandle.h"
#include "Complex.h"

class EffectProfileBuffer {
    public:
        ControllerEventData* data;
        int current_frame;
        int current_index;
        int data_width;
        float* current_value{};

        virtual void apply(float* working_array, int array_size) {};

        explicit EffectProfileBuffer(EffectProfileBuffer* original) {
            this->data = original->data;
            this->current_index = original->current_index;
            this->data_width = original->data_width;
            this->current_value = (float*)malloc(sizeof(float) * this->data_width);
            for (int i = 0; i < original->data_width; i++) {
                this->current_value[i] = original->current_value[i];
            }
            this->current_frame = original->current_frame;
            this->set_frame(0);
        }

        EffectProfileBuffer(ControllerEventData* controller_event_data, int start_frame) {
            this->data = controller_event_data;
            this->current_frame = start_frame;
            this->current_index = 0;
            this->data_width = this->data->frames[0]->data_width;
            this->current_value = (float*)malloc(sizeof(float) * this->data_width);
            for (int i = 0; i < this->data_width; i++) {
                this->current_value[i] = 0;
            }
            this->set_frame(0);
        }

        ~EffectProfileBuffer() {
            free(this->current_value);
        }

        float* get_next() {
            float* output = this->current_value;
            this->_move_to_next_frame();
            return output;
        }

        void set_frame(int frame) {
            if (this->data == nullptr || this->data->frames == nullptr) return;

            // First set the working frame
            if (this->data->actual_size > 0) {
                this->current_frame = frame % this->data->actual_size;
            } else {
                this->current_frame = frame;
            }

            // Find the active event
            this->current_index = 0;
            while (this->current_index < this->data->frame_count && this->current_frame > this->data->frames[this->current_index]->end) {
                this->current_index++;
            }

            if (this->current_index >= this->data->frame_count) {
                ProfileBufferFrame* bframe_data = this->data->frames[this->data->frame_count - 1];
                auto frame_diff = (float)(bframe_data->end - bframe_data->frame);
                for (int i = 0; i < this->data_width; i++) {
                    this->current_value[i] = bframe_data->initial_value[i] + (frame_diff * bframe_data->increment[i]);
                }
            } else {
                ProfileBufferFrame* bframe_data = this->data->frames[this->current_index];
                if (this->current_frame >= bframe_data->frame) {
                    auto frame_diff = (float)(this->current_frame - bframe_data->frame);
                    for (int i = 0; i < this->data_width; i++) {
                        this->current_value[i] = bframe_data->initial_value[i] + (frame_diff * bframe_data->increment[i]);
                    }
                } else if (this->current_index > 0) {
                    bframe_data = this->data->frames[this->current_index - 1];
                    auto frame_diff = (float)(bframe_data->end - bframe_data->frame);
                    for (int i = 0; i < this->data_width; i++) {
                        this->current_value[i] = bframe_data->initial_value[i] + (frame_diff * bframe_data->increment[i]);
                    }
                } else {
                    for (int i = 0; i < this->data_width; i++) {
                        this->current_value[i] = 0; // Shouldn't be reachable
                    }
                }
            }
        }

        void drain(int count) {
            this->set_frame(count + this->current_frame);
        }

    private:
        void _move_to_next_frame() {
            this->current_frame++;

            if (this->data->actual_size > 0 && this->current_frame > this->data->actual_size) {
                this->set_frame(this->current_frame % this->data->actual_size);
                return;
            }

            if (this->current_index >= this->data->frame_count) {
                // Nothing to be done
            } else {
                auto bframe = this->data->frames[this->current_index];
                bool frame_changed = false;
                while (this->current_frame > bframe->end) {
                    this->current_index++;
                    if (this->current_index >= this->data->frame_count) return;

                    bframe = this->data->frames[this->current_index];
                    frame_changed = true;
                }

                if (frame_changed) {
                    if (bframe->frame <= this->current_frame) {
                        float diff = (float)this->current_frame - (float)bframe->frame;
                        for (int i = 0; i < this->data_width; i++) {
                            this->current_value[i] = bframe->initial_value[i] + (bframe->increment[i] * diff);
                        }
                    }
                } else if (this->current_frame == bframe->frame) {
                    for (int i = 0; i < this->data_width; i++) {
                        this->current_value[i] = bframe->initial_value[i];
                    }
                } else if (this->current_frame > bframe->frame) {
                    for (int i = 0; i < this->data_width; i++) {
                        this->current_value[i] += bframe->increment[i];
                    }
                }
            }
        }
};

// class FrequencyDomainBuffer: public EffectProfileBuffer {
// public:
//     FrequencyDomainBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) { }
//     FrequencyDomainBuffer(FrequencyDomainBuffer* original): EffectProfileBuffer(original) {}
//
//     virtual bool working_value_check(float* next_value) = 0;
//     virtual void apply_section(Complex* transformed_left, Complex* transformed_right, int size) {};
//     void setup_fft_and_apply(float* working_array, int array_size, int offset, float* working_input_left, float* working_input_right, int target_size) {
//         int padded_size = 2;
//         while (padded_size < target_size) {
//             padded_size *= 2;
//         }
//         Complex* transformed_left = fft(working_input_left, target_size, padded_size);
//         Complex* transformed_right = fft(working_input_right, target_size, padded_size);
//         this->apply_section(transformed_left, transformed_right, target_size);
//
//         Complex* reverted_left = ifft(transformed_left, padded_size);
//         Complex* reverted_right = ifft(transformed_right, padded_size);
//         for (int y = 0; y < target_size; y++) {
//             working_array[y + offset] = reverted_left[y].real;
//             working_array[y + array_size + offset] = reverted_right[y].real;
//         }
//
//         free(transformed_left);
//         free(reverted_left);
//         free(transformed_right);
//         free(reverted_right);
//     }
//
//     void apply(float* working_array, int array_size) override {
//         int i = 0;
//         float working_input_left[array_size];
//         float working_input_right[array_size];
//         int working_input_size = 0;
//         for (int x = 0; x < array_size; x++) {
//             if (this->working_value_check(this->get_next())) {
//                 this->setup_fft_and_apply(working_array, array_size, i, working_input_left, working_input_right, working_input_size);
//                 working_input_size = 0;
//                 i = x;
//             } else {
//                 working_input_left[working_input_size] = working_array[x];
//                 working_input_right[working_input_size++] = working_array[x + array_size];
//             }
//         }
//         if (working_input_size > 0) {
//             this->setup_fft_and_apply(working_array, array_size, i, working_input_left, working_input_right, working_input_size);
//         }
//     }
// };

#endif //PAGAN_EFFECTPROFILEBUFFER_H
