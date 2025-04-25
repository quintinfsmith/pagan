//
// Created by pent on 4/22/25.
//

#ifndef PAGAN_PITCHEDBUFFER_H
#define PAGAN_PITCHEDBUFFER_H

#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

class PitchedBufferOverflow : public std::exception {};

struct PitchedBuffer {
    jshort* data;
    int data_size;
    float pitch;
    int start;
    int end;
    bool is_loop;
    int virtual_position;
    float pitch_adjustment;
    int virtual_size;
    float adjusted_pitch;

public:
    explicit PitchedBuffer(
        jshort* data,
        int data_size,
        float pitch,
        int start,
        int end,
        bool is_loop,
        int virtual_position,
        float pitch_adjustment,
        int virtual_size,
        float adjusted_pitch
    ) {
        this->virtual_position = 0;
        this->data = data;
        this->data_size = data_size;
        this->pitch = pitch;
        this->start = start;
        this->end = end;
        this->is_loop = is_loop;
        this->virtual_position = virtual_position;
        this->pitch_adjustment = pitch_adjustment;
        this->virtual_size = virtual_size;
        this->adjusted_pitch = adjusted_pitch;
    }

    explicit PitchedBuffer(jshort* data, int data_size, float pitch, int start, int end, bool is_loop) {
        this->virtual_position = 0;
        this->data = data;
        this->data_size = data_size;
        this->pitch = pitch;
        this->start = start;
        this->end = end;
        this->is_loop = is_loop;

        this->repitch(1);
    }
    ~PitchedBuffer() = default;
    void repitch(float new_pitch_adjustment) {
        this->pitch_adjustment = new_pitch_adjustment;
        this->adjusted_pitch = this->pitch * this->pitch_adjustment;
        this->virtual_size = static_cast<int>(static_cast<float>(this->end + 1 - this->start) / this->adjusted_pitch);
    }

    float get() {
        float working_pitch = this->pitch * this->adjusted_pitch;
        int unpitched_position = this->virtual_position++ * working_pitch;
        uint16_t output = this->get_real_frame(unpitched_position);
        return static_cast<float>(output) / static_cast<float>(65535); // SHORT MAX
    }

    uint16_t get_real_frame(int unpitched_position) {
        int range_size = this->end + 1 - this->start;

        int adj_i;
        if (this->is_loop) {
            adj_i = unpitched_position % range_size;
        } else if (unpitched_position >= range_size) {
            throw PitchedBufferOverflow();
        } else {
            adj_i = unpitched_position;
        };

        return this->data[adj_i];
    }

    bool is_overflowing() {
        return (int)((float)this->virtual_position * this->adjusted_pitch) - this->start > this->end;
    }

    void set_position(int frame) {
        this->virtual_position = frame;
    }

    void copy_to(PitchedBuffer* new_buffer) const {
        new_buffer->data = this->data;
        new_buffer->data_size = this->data_size;
        new_buffer->pitch = this->pitch;
        new_buffer->start = this->start;
        new_buffer->end = this->end;
        new_buffer->is_loop = this->is_loop;
        new_buffer->virtual_position = this->virtual_position;
        new_buffer->pitch_adjustment = this->pitch_adjustment;
        new_buffer->virtual_size = this->virtual_size;
        new_buffer->adjusted_pitch = this->adjusted_pitch;
    }

};
#endif //PAGAN_PITCHEDBUFFER_H
