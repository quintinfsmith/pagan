#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

class PitchedBufferOverflow : public std::exception {};

struct PitchedBuffer {
    jshort* data;
    int data_size;
    float pitch;
    int max;
    int start;
    int end;
    bool is_loop;
    int virtual_position;
    float pitch_adjustment;
    int virtual_size;
    float adjusted_pitch;
public:
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
        return (this->virtual_position * this->adjusted_pitch) - this->start > this->end;
    }
};

