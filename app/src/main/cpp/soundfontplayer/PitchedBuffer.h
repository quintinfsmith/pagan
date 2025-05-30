//
// Created by pent on 4/22/25.
//

#ifndef PAGAN_PITCHEDBUFFER_H
#define PAGAN_PITCHEDBUFFER_H

#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include "../soundfont/SampleData.h"

class PitchedBufferOverflow : public std::exception {};

struct PitchedBuffer {
    SampleData* data;
    float pitch;
    int start;
    int end;
    bool is_loop;
    int virtual_position; // position as viewed from the outside
    int internal_position; // same ratio as virtual position but readjusted on every repitch
    float pitch_adjustment;
    int virtual_size;
    float adjusted_pitch;

public:
    explicit PitchedBuffer(
        SampleData* data,
        float pitch,
        int start,
        int end,
        bool is_loop,
        int virtual_position,
        float pitch_adjustment,
        int virtual_size,
        float adjusted_pitch
    ) {
        this->data = data;
        this->pitch = pitch;
        this->start = start;
        this->end = end;
        this->is_loop = is_loop;
        this->virtual_position = virtual_position;
        this->internal_position = virtual_position;
        this->pitch_adjustment = pitch_adjustment;
        this->virtual_size = virtual_size;
        this->adjusted_pitch = adjusted_pitch;
    }

    explicit PitchedBuffer(SampleData* data, float pitch, int start, int end, bool is_loop) {
        this->virtual_position = 0;
        this->internal_position = virtual_position;
        this->data = data;
        this->pitch = pitch;
        this->start = start;
        this->end = end;
        this->is_loop = is_loop;

        this->initialize_pitch();
    }

    // Pitched Buffer doesn't original source of data so don't destroy it here.
    //~PitchedBuffer() = default;

    void repitch(float new_pitch_adjustment) {
        this->pitch_adjustment = new_pitch_adjustment;
        this->adjusted_pitch = this->pitch * this->pitch_adjustment;

        this->virtual_size = static_cast<int>(static_cast<float>(this->end + 1 - this->start) / this->adjusted_pitch);
        this->internal_position /= this->adjusted_pitch;
    }

    void initialize_pitch() {
        this->pitch_adjustment = 1;
        this->adjusted_pitch = this->pitch;
        this->virtual_size = static_cast<int>(static_cast<float>(this->end + 1 - this->start) / this->adjusted_pitch);
    }

    float get() {
        int unpitched_position = (int)((float)this->internal_position * this->adjusted_pitch);
        this->virtual_position += 1;
        this->internal_position += 1;

        float output = (float)this->get_real_frame(unpitched_position) / (float)32768; //SHORT MAX
        return output;
    }

    jshort get_real_frame(int unpitched_position) const {
        int range_size = this->end - this->start;

        int adj_i;
        if (this->is_loop) {
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
        return (int)((float)this->internal_position * this->adjusted_pitch) - this->start >= this->end;
    }

    // This should NOT be used if a smooth transition is required
    void set_position(int frame) {
        this->virtual_position = frame;
        this->internal_position = frame;
    }

    void copy_to(PitchedBuffer* new_buffer) const {
        new_buffer->data = this->data;
        new_buffer->pitch = this->pitch;
        new_buffer->start = this->start;
        new_buffer->end = this->end;
        new_buffer->is_loop = this->is_loop;
        new_buffer->virtual_position = this->virtual_position;
        new_buffer->internal_position = this->internal_position;
        new_buffer->pitch_adjustment = this->pitch_adjustment;
        new_buffer->virtual_size = this->virtual_size;
        new_buffer->adjusted_pitch = this->adjusted_pitch;
    }
};

#endif //PAGAN_PITCHEDBUFFER_H
