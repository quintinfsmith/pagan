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

int PROFILE_BUFFER_ID_GEN = 0;
class EffectProfileBuffer {
public:
    ControllerEventData* data;
    int current_frame;
    int current_index;
    int data_width;
    float* current_value;
    int buffer_id;
    int type;

    ~EffectProfileBuffer() {
        delete this->current_value;
    }


    void init(ControllerEventData* controller_event_data, int current_frame) {
        this->buffer_id = PROFILE_BUFFER_ID_GEN++;

        this->data = controller_event_data;
        this->current_frame = current_frame;
        this->current_index = 0;
        this->data_width = this->data->frames[0]->data_width;
        this->current_value = (float*)malloc(sizeof(float) * this->data_width);
        for (int i = 0; i < this->data_width; i++) {
            this->current_value[i] = 0;
        }
        this->set_frame(0);
    }

    float* get_next() {
        float* output = this->current_value;
        this->_move_to_next_frame();
        return output;
    }

    void set_frame(int frame) {
        if (this->data == nullptr || this->data->frames == nullptr) {
            return;
        }
        // First set the working frame
        this->current_frame = frame;

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

    void copy_to(EffectProfileBuffer* new_buffer) {
        new_buffer->data = this->data;
        new_buffer->current_index = this->current_index;
        new_buffer->data_width = this->data_width;
        for (int i = 0; i < this->data_width; i++) {
            new_buffer->current_value[i] = this->current_value[i];
        }
        new_buffer->current_frame = this->current_frame;
        new_buffer->set_frame(0);
        this->copy_from(new_buffer);
    }
    void drain(int count) {
        this->set_frame(count + this->current_frame);
    }


private:
    virtual void copy_from(EffectProfileBuffer* original) = 0;
    void _move_to_next_frame() {
        this->current_frame++;
        if (this->current_index >= this->data->frame_count) {
            // Nothing to be done
        } else {
            auto bframe = this->data->frames[this->current_index];
            bool frame_changed = false;
            while (this->current_frame > bframe->end) {
                this->current_index++;
                if (this->current_index >= this->data->frame_count) {
                    return;
                }

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

class EqualizerBuffer: public EffectProfileBuffer {
    int type = TYPE_EQUALIZER;
    float* working_eq;
    void copy_from(EffectProfileBuffer* original) override {}
    public:
        void apply(float* working_array, int array_size) {
            int i = 0;
            float *working_input_left;
            float *working_input_right;
            int working_input_size = 0;
            for (int x = 0; x < array_size; x++) {
                float* next = this->get_next();
                if (next != this->working_eq) {
                    this->working_eq = next;

                    int padded_size = 2;
                    while (padded_size < working_input_size) {
                        padded_size *= 2;
                    }

                    Complex* transformed_left = fft(working_input_left, working_input_size, padded_size);
                    Complex* transformed_right = fft(working_input_right, working_input_size, padded_size);
                    int size = (int)this->working_eq[0];

                    for (int y = 0; y < size; y++) {
                        float value = working_eq[(y * 3)];
                        int first = (int)this->working_eq[(y * 3) + 1];
                        int last = (int)this->working_eq[(y * 3) + 2];
                        for (int z = first; z < last; z++) {
                            transformed_left[z].real *= value;
                            transformed_right[z].real *= value;
                        }
                    }

                    Complex* reverted_left = ifft(transformed_left, working_input_size);
                    Complex* reverted_right = ifft(transformed_right, working_input_size);
                    for (int y = 0; y < working_input_size; y++) {
                        working_array[y + i] = reverted_left[y].real;
                        working_array[y + array_size + i] = reverted_right[y].real;
                    }

                    delete transformed_left;
                    delete reverted_left;
                    delete transformed_right;
                    delete reverted_right;

                    working_input_size = 0;
                    i = x;
                } else {
                    working_input_left[working_input_size] = working_array[x];
                    working_input_right[working_input_size++] = working_array[x + array_size];
                }
            }
        }
};

class VolumeBuffer: public EffectProfileBuffer {
    int type = TYPE_VOLUME;
    void copy_from(EffectProfileBuffer* original) override {}
    public:
        void apply(float* working_array, int array_size) {
            for (int i = 0; i < array_size; i++) {
                float volume = this->get_next()[0];
                working_array[i] *= volume;
                working_array[i + array_size] *= volume;
            }
        }
};

class PanBuffer: public EffectProfileBuffer {
    int type = TYPE_PAN;
    void copy_from(EffectProfileBuffer* original) override {}
    public:
        void apply(float* working_array, int array_size) {
            for (int i = 0; i < array_size; i++) {
                float pan_value = this->get_next()[0];
                working_array[i] *= 1 + pan_value;
                working_array[i + array_size] *= (-1 + pan_value) * -1;
            }
        }
};

class DelayedFrame {
    DelayedFrame* next;
    public:
        float value_left = 0;
        float value_right = 0;

        // Will likely cause error
        ~DelayedFrame() {
            if (this->next != nullptr) {
                delete this->next;
                this->next = nullptr;
            }
        }

        DelayedFrame() {
            this->value_left = 0;
            this->value_right = 0;
            this->next = nullptr;
        }

        void set_next(DelayedFrame* next_frame) {
            this->next = next_frame;
        }

        DelayedFrame* get_next() {
            return this->next;
        }
        DelayedFrame* init_next() {
            this->next = (DelayedFrame*)malloc(sizeof(DelayedFrame));
            return this->next;
        }
};

class DelayBuffer: public EffectProfileBuffer {
    int type = TYPE_DELAY;
    float active_delay = 0;
    int active_fpb = 0; // Frames Per Beat
    int active_delay_in_frames = 0;

    DelayedFrame* active_input_frame;

    // void delete_chain() {
    //     DelayedFrame* working_ptr = this->active_input_frame->get_next();
    //     while (working_ptr->get_next() != this->active_input_frame) {
    //         DelayedFrame* prev_ptr = working_ptr;
    //         working_ptr = working_ptr->get_next();
    //         delete prev_ptr;
    //     }
    //     delete working_ptr;
    //     this->active_input_frame = nullptr;
    //     this->active_output_frame = nullptr;
    // }

    void create_chain() {
        if (this->active_delay_in_frames == 0) return;

        auto* working_ptr = (DelayedFrame*)malloc(sizeof(DelayedFrame));
        this->active_input_frame = working_ptr;

        for (int i = 0; i < this->active_delay_in_frames - 1; i++) {
            working_ptr = working_ptr->init_next();
        }

        working_ptr->set_next(this->active_input_frame);
    }

    void adjust_chain_size(int next_fpb, float next_delay) {

        if (next_fpb == this->active_fpb && this->active_delay == next_delay) return;
        // TODO: VV -- Adjust Countdowns Here -- VV

        DelayedFrame* original_ptr = this->active_input_frame;
        int original_delay = this->active_delay_in_frames;

        this->active_delay = next_delay;
        this->active_fpb = next_fpb;
        this->active_delay_in_frames = (int)(this->active_delay * (float)this->active_fpb);

        this->create_chain();
    }

    void cycle() {
        this->active_input_frame = this->active_input_frame->get_next();
    }

    void copy_from(EffectProfileBuffer* original) override {}
    public:
        void apply(float* working_array, int array_size) {
            for (int i = 0; i < array_size; i++) {
                float* frame_data = EffectProfileBuffer::get_next();

                //__android_log_print(ANDROID_LOG_DEBUG, "??", "::- %f, %f, %f, %f", frame_data[0], frame_data[1], frame_data[2], frame_data[3]);
                int repeat = frame_data[1];
                if (repeat == 0) continue;

                float next_delay = frame_data[0];
                float decay = frame_data[2];
                int next_fpb = frame_data[3];

                this->adjust_chain_size(next_fpb, next_delay);

                if (this->active_input_frame == nullptr) continue;

                this->active_input_frame->value_left += working_array[i] * decay;
                this->active_input_frame->value_right += working_array[i + array_size] * decay;

                auto* output_frame = this->active_input_frame->get_next();
                working_array[i] += output_frame->value_left;
                working_array[i + array_size] += output_frame->value_right;

                this->cycle();
            }
        }

};

#endif //PAGAN_EFFECTPROFILEBUFFER_H