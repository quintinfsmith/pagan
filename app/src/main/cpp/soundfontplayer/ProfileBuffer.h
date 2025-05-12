//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_PROFILEBUFFER_H
#define PAGAN_PROFILEBUFFER_H
#include "ProfileBufferFrame.h"
#include <vector>
#include "ControllerEventData.h"

class ProfileBuffer {
public:
    ControllerEventData* data;
    int current_frame;
    int current_index;
    float current_value;
    int next_frame_trigger;
    int start_frame;

    ~ProfileBuffer() = default;

    float get_next() {
        ProfileBufferFrame* bframe_data = this->data->frames[this->current_index];
        if (bframe_data->frame == this->current_frame) {
            this->current_value = bframe_data->initial_value;
        } else {
            this->current_value += bframe_data->increment;
        }

        float output = this->current_value;
        this->_move_to_next_frame();
        return output;
    }

    void set_frame(int frame) {
        if (this->data->frames == nullptr) {
            return;
        }
        // First set the working frame
        this->current_frame = frame + this->start_frame;

        // Find the active event
        this->current_index = -1;
        while (this->current_index < this->data->frame_count - 1) {
            if (this->data->frames[this->current_index + 1]->frame <= this->current_frame) {
                this->current_index++;
            } else {
                break;
            }
        }
        if (this->current_index == -1) {
            this->current_index = 0;
        }

        // Set the next frame trigger
        if (this->current_index < this->data->frame_count - 1) {
            this->next_frame_trigger = this->data->frames[this->current_index + 1]->frame;
        } else {
            this->next_frame_trigger = -1;
        }

        // Set the active value
        ProfileBufferFrame* frame_data = this->data->frames[this->current_index];
        this->current_value = frame_data->initial_value;
        if (frame_data->increment != 0) {
            this->current_value += (float)(this->current_frame - frame_data->frame) * frame_data->increment;
        }
    }

    void copy_to(ProfileBuffer* new_buffer) const {
        new_buffer->data = this->data;
        new_buffer->start_frame = this->start_frame;
        new_buffer->next_frame_trigger = this->next_frame_trigger;
        new_buffer->current_index = this->current_index;
        new_buffer->current_frame = this->current_frame;
        new_buffer->current_value = this->current_value;
        new_buffer->set_frame(0);
    }

private:
    void _move_to_next_frame() {
        this->current_frame++;
        int working_frame = this->current_frame;
        if (working_frame == this->next_frame_trigger) {
            if (this->current_index >= this->data->frame_count - 1) {
                this->next_frame_trigger = -1;
            } else {
                this->next_frame_trigger = this->data->frames[this->current_index++]->frame;
            }
        }
    }
};

#endif //PAGAN_PROFILEBUFFER_H
