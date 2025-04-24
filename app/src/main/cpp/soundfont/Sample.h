//
// Created by pent on 4/15/25.
//
#ifndef PAGAN_SAMPLE_H
#define PAGAN_SAMPLE_H
#include <jni.h>
#include <optional>
#include <vector>

struct Sample {
    char *name;
    int loop_start;
    int loop_end;
    int sample_rate;
    int original_pitch;
    int pitch_correction;
    int sample_type;
    int data_placeholder_start;
    int data_placeholder_end;
    short* data;
    public:
        explicit Sample(
            char* name,
            int loop_start,
            int loop_end,
            int sample_rate,
            int original_pitch,
            int pitch_correction,
            int sample_type,
            int data_placeholder_start,
            int data_placeholder_end
        ) {
            this->name = name;
            this->loop_start = loop_start;
            this->loop_end = loop_end;
            this->sample_rate = sample_rate;
            this->original_pitch = original_pitch;
            this->pitch_correction = pitch_correction;
            this->sample_type = sample_type;
            this->data_placeholder_start = data_placeholder_start;
            this->data_placeholder_end = data_placeholder_end;
        }
        void set_data(short * data) {
            this->data = data;
        }

};
#endif //PAGAN_SAMPLE_H
