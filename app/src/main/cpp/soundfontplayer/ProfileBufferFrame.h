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

    ~ProfileBufferFrame() {
        delete this->initial_value;
        delete this->increment;
    }
};

#endif //PAGAN_PROFILEBUFFERFRAME_H
