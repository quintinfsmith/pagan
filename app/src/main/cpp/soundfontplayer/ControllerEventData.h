//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_CONTROLLEREVENTDATA_H
#define PAGAN_CONTROLLEREVENTDATA_H
#include "ProfileBufferFrame.h"
#include <vector>

class ControllerEventData {
public:
    ProfileBufferFrame** frames;
    int frame_count;

    explicit ControllerEventData(std::vector<ProfileBufferFrame> frames) {
        this->frame_count = frames.size();
        this->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * this->frame_count);

        for (auto & frame : frames) {
            ProfileBufferFrame* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
            ptr->frame = frame.frame;
            ptr->initial_value = frame.initial_value;
            ptr->increment = frame.increment;
        }
    }

    ~ControllerEventData() {
        for (int i = 0; i < this->frame_count; i++) {
            delete this->frames[i];
        }
        delete this->frames;
    }

    void copy_to(ControllerEventData* new_buffer) const {
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
        } else {
            new_buffer->frames = nullptr;
            new_buffer->frame_count = 0;
        }
    }
};



#endif //PAGAN_CONTROLLEREVENTDATA_H
