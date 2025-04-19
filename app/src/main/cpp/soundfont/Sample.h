//
// Created by pent on 4/15/25.
//
#include <optional>
#ifndef PAGAN_SAMPLE_H
#define PAGAN_SAMPLE_H

struct Sample {
    std::string name;
    int loop_start;
    int loop_end;
    int sample_rate;
    int original_pitch;
    int pitch_correction;
    int sample_type;
    int data_placeholder_start;
    int data_placeholder_end;
    jshort* data;
};
#endif //PAGAN_SAMPLE_H
