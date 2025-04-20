//
// Created by pent on 4/15/25.
//
#ifndef PAGAN_SAMPLE_H
#define PAGAN_SAMPLE_H
#include <jni.h>
#include <optional>
#include <vector>

struct Sample {
    jstring name;
    int loop_start;
    int loop_end;
    int sample_rate;
    int original_pitch;
    int pitch_correction;
    int sample_type;
    int link_addr;
    int data_placeholder_start;
    int data_placeholder_end;
    jshortArray data;
    public:
        void set_data(jshortArray data) {
            this->data = data;
        }

};
#endif //PAGAN_SAMPLE_H
