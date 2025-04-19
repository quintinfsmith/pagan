//
// Created by pent on 4/15/25.
//
#include <optional>
#ifndef PAGAN_SAMPLE_H
#define PAGAN_SAMPLE_H
#include "vector"

struct Sample {
    std::string name;
    int loop_start;
    int loop_end;
    int sample_rate;
    int original_pitch;
    int pitch_correction;
    int sample_type;
    int link_addr;
    int data_placeholder_start;
    int data_placeholder_end;
    std::vector<short>* data;
    public:
        void set_data(std::vector<short>* data) {
            this->data = data;
        }

};
#endif //PAGAN_SAMPLE_H
