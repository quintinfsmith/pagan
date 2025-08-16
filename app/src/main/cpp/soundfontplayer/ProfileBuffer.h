//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_PROFILEBUFFER_H
#define PAGAN_PROFILEBUFFER_H
#include "ProfileBufferFrame.h"
#include <vector>
#include "ControllerEventData.h"
int PROFILE_BUFFER_ID_GEN = 0;
class ProfileBuffer {
public:
    ControllerEventData* data;
    int current_frame;
    int current_index;
    int data_width;
    float* current_value;
    int buffer_id = 0;

    ~ProfileBuffer() {
        delete this->current_value;
    }

    void init_id() {
        this->buffer_id = PROFILE_BUFFER_ID_GEN++;
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

    void copy_to(ProfileBuffer* new_buffer) const {
        new_buffer->data = this->data;
        new_buffer->current_index = this->current_index;
        new_buffer->data_width = this->data_width;
        for (int i = 0; i < this->data_width; i++) {
            new_buffer->current_value[i] = this->current_value[i];
        }
        new_buffer->current_frame = this->current_frame;
        new_buffer->set_frame(0);
    }

    // TODO: This could probably be optimized but not a priority atm.
    void drain(int count) {
        this->set_frame(count + this->current_frame);
    }

private:
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

#endif //PAGAN_PROFILEBUFFER_H
