//
// Created by pent on 4/22/25.
//

#ifndef PAGAN_PITCHEDBUFFER_H
#define PAGAN_PITCHEDBUFFER_H

#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include "../soundfont2/SampleData.h"

class PitchedBufferOverflow : public std::exception {};

struct PitchedBuffer {
    SampleData* data;
    float default_pitch;
    int start;
    int end;
    bool is_loop;
    float pitched_increment;
    float real_position_postradix;
    int real_position_preradix;
    int virtual_position; // position as viewed from the outside
    int virtual_size;
 public:
    explicit PitchedBuffer(PitchedBuffer* original) {
        this->data = original->data;
        this->default_pitch = original->default_pitch;
        this->start = original->start;
        this->end = original->end;
        this->is_loop = original->is_loop;
        this->virtual_position = original->virtual_position;
        this->pitched_increment = original->pitched_increment;
        this->real_position_preradix = original->real_position_preradix;
        this->real_position_postradix = original->real_position_postradix;
        this->virtual_size = original->virtual_size;
    }

    explicit PitchedBuffer(SampleData* data, float pitch, int start, int end, bool is_loop) {
        this->virtual_position = 0;
        this->real_position_preradix = 0;
        this->real_position_postradix = 0;
        this->data = data;
        this->default_pitch = pitch;
        this->pitched_increment = this->default_pitch;
        this->start = start;
        this->end = end;
        this->is_loop = is_loop;
        this->repitch(1);
    }

    // Pitched Buffer doesn't original source of data so don't destroy it here.
    //~PitchedBuffer() = default;

    void repitch(float new_pitch_adjustment) {
        this->pitched_increment = (this->default_pitch * new_pitch_adjustment);
        this->virtual_size = static_cast<int>(static_cast<float>(this->end + 1 - this->start) / this->pitched_increment);
    }

    float get() {
        int unpitched_position = this->real_position_preradix;
        this->virtual_position += 1;
        float temp = this->real_position_postradix + this->pitched_increment;
        this->real_position_preradix += std::floor(temp);
        this->real_position_postradix = temp - std::floor(temp);

        return (float)this->get_real_frame(unpitched_position) / (float)32768; //SHORT MAX
    }

    jshort get_real_frame(int unpitched_position) const {
        int range_size = this->end - this->start;

        int adj_i;
        if (this->is_loop && range_size > 0) {
            adj_i = unpitched_position % range_size;
        } else if (unpitched_position >= range_size) {
            throw PitchedBufferOverflow();
        } else {
            adj_i = unpitched_position;
        };
        adj_i = std::min(this->end - 1, this->start + adj_i);
        return this->data->data[adj_i];
    }

    bool is_overflowing() {
        return (int)((float)this->real_position_preradix) - this->start >= this->end;
    }

    // This should NOT be used if a smooth transition is required
    void set_position(int frame) {
        this->virtual_position = frame;
        this->real_position_preradix = frame * this->pitched_increment;
        this->real_position_postradix = (frame * this->pitched_increment) - this->real_position_preradix;
    }
};

#endif //PAGAN_PITCHEDBUFFER_H
