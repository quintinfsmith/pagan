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

    ControllerEventData(int type, int array_length, int data_width, int* indices, int* end_indices, const float* values, const float* increments) {
        this->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * array_length);
        this->frame_count = array_length;
        this->type = type;



        for (int i = 0; i < array_length; i++) {
            auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
            new (ptr) ProfileBufferFrame((int)indices[i], (int)end_indices[i], data_width);
            for (int j = 0; j < data_width; j++) {
                ptr->initial_value[j] = (float)values[(i * data_width) + j];
                ptr->increment[j] = (float)increments[(i * data_width) + j];
            }

            this->frames[i] = ptr;
        }

    }

    explicit ControllerEventData(ControllerEventData* original) {
        this->type = original->type;
        if (original->frames != nullptr) {
            this->frames = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * original->frame_count);
            for (int i = 0; i < original->frame_count; i++) {
                auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
                new (ptr) ProfileBufferFrame(original->frames[i]);
                this->frames[i] = ptr;
            }

            this->frame_count = original->frame_count;
        } else {
            this->frames = nullptr;
            this->frame_count = 0;
        }
    }

    ~ControllerEventData() {
        for (int i = 0; i < this->frame_count; i++) {
            this->frames[i]->~ProfileBufferFrame();
            free(this->frames[i]);
        }
        free(this->frames);
    }
};



#endif //PAGAN_CONTROLLEREVENTDATA_H
