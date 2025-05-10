//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_PROFILEBUFFER_H
#define PAGAN_PROFILEBUFFER_H
#include "ProfileBufferFrame.h"
#include <vector>
#include <android/log.h>

class ProfileBuffer {
public:
    ProfileBufferFrame** frames;
    int frame_count;
    int current_frame;
    int current_index;
    float current_value;
    int next_frame_trigger;
    int start_frame;

    explicit ProfileBuffer(std::vector<ProfileBufferFrame> frames, int start_frame) {
        this->frame_count = frames.size();
        this->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * this->frame_count);

        for (auto & frame : frames) {
            ProfileBufferFrame* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
            ptr->frame = frame.frame;
            ptr->initial_value = frame.initial_value;
            ptr->increment = frame.increment;
        }

        this->start_frame = start_frame;
        this->set_frame(0);
    }

    ~ProfileBuffer() {
        for (int i = 0; i < this->frame_count; i++) {
            delete this->frames[i];
        }
        delete this->frames;
    }

    float get_next() {
        ProfileBufferFrame* bframe_data = this->frames[this->current_index];
        if (bframe_data->frame == this->current_index) {
            this->current_value = bframe_data->initial_value;
        } else {
            this->current_value += bframe_data->increment;
        }

        float output = this->current_value;

        this->_move_to_next_frame();
        return output;
    }

    void set_frame(int frame) {
        if (this->frames == nullptr) {
            return;
        }
        // First set the working frame
        this->current_frame = frame + this->start_frame;

        // Find the active event
        this->current_index = -1;
        while (this->current_index < this->frame_count - 1) {
            if (this->frames[this->current_index + 1]->frame <= this->current_frame) {
                this->current_index++;
            } else {
                break;
            }
        }

        // Set the next frame trigger
        if (this->current_index < this->frame_count - 1) {
            this->next_frame_trigger = this->frames[this->current_index + 1]->frame;
        } else {
            this->next_frame_trigger = -1;
        }

        // Set the active value
        ProfileBufferFrame* frame_data = this->frames[this->current_index];
        this->current_value = frame_data->initial_value;
        if (frame_data->increment != 0) {
            this->current_value += (float)(this->current_frame - frame_data->frame) * frame_data->increment;
        }
    }

    void copy_to(ProfileBuffer* new_buffer) const {
        if (this->frames != nullptr) {
            new_buffer->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * this->frame_count);
            for (int i = 0; i < this->frame_count; i++) {
                ProfileBufferFrame* frame = this->frames[i];
                auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
                ptr->frame = frame->frame;
                ptr->increment = frame->increment;
                ptr->initial_value = frame->initial_value;
                new_buffer->frames[i] = ptr;
            }
            new_buffer->frame_count = this->frame_count;
        }

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
            if (this->current_index >= this->frame_count - 1) {
                this->next_frame_trigger = -1;
            } else {
                int c = this->current_index;
                this->next_frame_trigger = this->frames[this->current_index++]->frame;
                __android_log_write(ANDROID_LOG_DEBUG, "---", (std::to_string(c) + " -- " + std::to_string(this->next_frame_trigger)).c_str());
            }
        }
    }
};



#endif //PAGAN_PROFILEBUFFER_H
