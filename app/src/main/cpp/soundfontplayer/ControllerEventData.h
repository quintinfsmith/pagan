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
    int type;
    int frame_count;

    explicit ControllerEventData(std::vector<ProfileBufferFrame> frames, int type) {
        this->type = type;
        this->frame_count = frames.size();
        this->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * this->frame_count);

        for (auto & frame : frames) {
            ProfileBufferFrame* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
            ptr->data_width = frame.data_width;
            ptr->frame = frame.frame;
            ptr->end = frame.end;

            ptr->initial_value = (float*)malloc(sizeof(float) * frame.data_width);
            ptr->increment = (float*)malloc(sizeof(float) * frame.data_width);
            for (int i = 0; i < frame.data_width; i++) {
                ptr->initial_value[i] = frame.initial_value[i];
                ptr->increment[i] = frame.increment[i];
            }
        }
    }

    ~ControllerEventData() {
        for (int i = 0; i < this->frame_count; i++) {
            delete this->frames[i];
        }
        delete this->frames;
    }

    void copy_to(ControllerEventData* new_buffer) const {
        new_buffer->type = this->type;
        if (this->frames != nullptr) {
            new_buffer->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * this->frame_count);
            for (int i = 0; i < this->frame_count; i++) {
                ProfileBufferFrame* frame = this->frames[i];
                auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
                ptr->frame = frame->frame;
                ptr->end = frame->end;
                ptr->data_width = frame->data_width;

                ptr->initial_value = (float*)malloc(sizeof(float) * frame->data_width);
                ptr->increment = (float*)malloc(sizeof(float) * frame->data_width);
                for (int j = 0; j < ptr->data_width; j++) {
                    ptr->initial_value[j] = frame->initial_value[(i * frame->data_width) + j];
                    ptr->increment[j] = frame->increment[(i * frame->data_width) + j];
                }
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
