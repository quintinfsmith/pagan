//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_PROFILEBUFFERFRAME_H
#define PAGAN_PROFILEBUFFERFRAME_H

struct ProfileBufferFrame {
    int frame;
    int end;
    int data_width;
    float* initial_value;
    float* increment;

    public:
        ProfileBufferFrame(ProfileBufferFrame* original) {
            this->frame = original->frame;
            this->end = original->end;
            this->data_width = original->data_width;
            this->initial_value = (float*)malloc(sizeof(float) * original->data_width);
            this->increment = (float*)malloc(sizeof(float) * original->data_width);

            for (int j = 0; j < original->data_width; j++) {
                this->initial_value[j] = original->initial_value[j];
                this->increment[j] = original->increment[j];
            }
        }
        ProfileBufferFrame(int frame, int end, int data_width) {
            this->frame = frame;
            this->end = end;
            this->data_width = data_width;
            this->initial_value = (float*)malloc(sizeof(float) * data_width);
            this->increment = (float*)malloc(sizeof(float) * data_width);
        }
        ~ProfileBufferFrame() {
            delete this->initial_value;
            delete this->increment;
        }
};

#endif //PAGAN_PROFILEBUFFERFRAME_H
