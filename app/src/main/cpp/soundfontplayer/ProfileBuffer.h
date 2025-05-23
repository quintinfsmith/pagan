//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_PROFILEBUFFER_H
#define PAGAN_PROFILEBUFFER_H
#include "ProfileBufferFrame.h"
#include <vector>
#include <android/log.h>
#include "ControllerEventData.h"

class ProfileBuffer {
public:
    ControllerEventData* data;
    int current_frame;
    int current_index;
    float current_value;

    ~ProfileBuffer() = default;

    float get_next() {
        float output = this->current_value;
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
            this->current_value = bframe_data->initial_value + ((bframe_data->end - bframe_data->frame) * bframe_data->increment);
        } else {
            ProfileBufferFrame* bframe_data = this->data->frames[this->current_index];
            if (this->current_frame >= bframe_data->frame) {
                this->current_value = bframe_data->initial_value + ((this->current_frame - bframe_data->frame) * bframe_data->increment);
            } else if (this->current_index > 0) {
                bframe_data = this->data->frames[this->current_index - 1];
                this->current_value = bframe_data->initial_value + ((bframe_data->end - bframe_data->frame) * bframe_data->increment);
            } else {
                this->current_value = 0; // Shouldn't be reachable
            }
        }
        __android_log_write(ANDROID_LOG_DEBUG, "", (std::to_string(this->current_index) + " " + std::to_string(this->data->frame_count) + " " + std::to_string(frame)).c_str());
    }

    void copy_to(ProfileBuffer* new_buffer) const {
        new_buffer->data = this->data;
        new_buffer->current_index = this->current_index;
        new_buffer->current_value = this->current_value;
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
            if (this->current_frame > bframe->end) {
                this->current_index++;
                if (this->current_index >= this->data->frame_count) {
                    return;
                }

                bframe = this->data->frames[this->current_index];
            }

            if (this->current_frame == bframe->frame) {
                this->current_value = bframe->initial_value;
            } else if (this->current_frame > bframe->frame) {
                this->current_value += bframe->increment;
            }
        }
    }
};

#endif //PAGAN_PROFILEBUFFER_H
