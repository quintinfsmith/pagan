//
// Created by pent on 4/19/25.
//

#ifndef PAGAN_SAMPLEHANDLE_H
#define PAGAN_SAMPLEHANDLE_H

#include <jni.h>
#include <vector>
#include <unordered_map>
#include "soundfont/PitchedBuffer.cpp"
#include <cmath>

class VolumeEnvelope {
    public:
        int sample_rate;
        float delay;
        float attack;
        float hold;
        float decay;
        float release;
        int frames_delay;
        int frames_attack;
        int frames_hold;
        int frames_decay;
        int frames_release;
        float sustain_attenuation;
        float true_sustain_attenuation;

        explicit VolumeEnvelope(
            int sample_rate,
            float delay,
            float attack,
            float hold,
            float decay,
            float release,
            float sustain_attenuation
        ) {
            this->delay = delay;
            this->attack = attack;
            this->hold = hold;
            this->decay = decay;
            this->release = release;
            this->sustain_attenuation = sustain_attenuation;
            this->set_sample_rate(sample_rate);
        }

        void set_sample_rate(int sample_rate) {
            this->sample_rate = sample_rate;
            float float_rate = (float)sample_rate;
            this->frames_delay = (int)(float_rate * this->delay);
            this->frames_hold = (int)(float_rate * this->hold);
            this->frames_attack = (int)(float_rate * this->attack);
            this->frames_hold = (int)(float_rate * this->hold);
            this->frames_decay = (int)(float_rate * this->decay);
            this->frames_release = (int)(float_rate * this->release);
        }

        void copy_to(VolumeEnvelope* other) {
            other->delay = this->delay;
            other->attack = this->attack;
            other->hold = this->hold;
            other->decay = this->decay;
            other->release = this->release;
            other->sustain_attenuation = this->sustain_attenuation;
            other->set_sample_rate(this->sample_rate);
        }
};

struct ProfileBufferFrame {
    int frame;
    float initial_value;
    float increment;
};

class vector;

class ProfileBuffer {
    public:
        std::vector<ProfileBufferFrame> frames;
        int current_frame;
        int current_index;
        float current_value;
        int next_frame_trigger;
        int start_frame;

        explicit ProfileBuffer(std::vector<ProfileBufferFrame> frames, int start_frame, bool skip_initial_set) {
            this->start_frame = start_frame;
            this->frames = frames;
            if (!skip_initial_set) {
                this->set_frame(0);
            }
        }

        float get_next() {
            ProfileBufferFrame bframe_data = this->frames[this->current_index];
            if (bframe_data.frame == this->current_index) {
                this->current_value = bframe_data.initial_value;
            } else {
                this->current_value += bframe_data.initial_value;
            }

            float output = this->current_value;
            this->_move_to_next_frame();
            return output;
        }

        void set_frame(int frame) {
            int original_frame = this->current_frame;
            this->current_frame = frame + this->start_frame;
            if (original_frame == this->current_frame) {
                return;
            } else if (original_frame < this->current_frame) {
                while (this->current_index < this->frames.size() - 1) {
                    if (this->frames[this->current_index + 1].frame <= this->current_frame) {
                        this->current_index++;
                    } else {
                        break;
                    }
                }
            } else {
                while (this->current_index > 0 && this->frames[this->current_index].frame > this->current_frame) {
                    this->current_index -= 1;
                }
            }

            if (this->current_index < this->frames.size() - 1) {
                this->next_frame_trigger = this->frames[this->current_index + 1].frame;
            } else {
                this->next_frame_trigger = -1;
            }

            int working_frame = this->current_frame - 1;
            ProfileBufferFrame frame_data = this->frames[this->current_index];
            this->current_value = frame_data.initial_value;
            if (frame_data.increment != 0) {
                this->current_value += (float)(working_frame - frame_data.frame) * frame_data.increment;
            }
        }

        void copy_to(ProfileBuffer* new_buffer) {
            std::vector<ProfileBufferFrame> frames_copy;
            frames_copy.reserve(this->frames.size());
            for (auto frame: this->frames) {
                frames_copy.push_back(
                    ProfileBufferFrame{
                        frame.frame,
                        frame.initial_value,
                        frame.increment
                    }
                );
            }
            new_buffer->frames = frames_copy;
            new_buffer->start_frame = start_frame;
            new_buffer->set_frame(0);
        }

    private:
        void _move_to_next_frame() {
            this->current_frame++;
            int working_frame = this->current_frame;
            if (working_frame == this->next_frame_trigger) {
                if (this->current_index == this->next_frame_trigger) {
                    this->next_frame_trigger -= 1;
                } else {
                    this->next_frame_trigger = this->frames[this->current_index++].frame;
                }
            }
        }
};

int SampleHandleUUIDGen = 0;

// TODO Modulations
// modulation_envelope, modulation_lfo, modulators
class SampleHandle {
    float RC;
    float smoothing_factor;
    float initial_frame_factor;
    float previous_frame;
    // int uuid;

    int working_frame;
    std::optional<int> release_frame;
    std::optional<int> kill_frame;
    bool is_dead;
    int active_buffer;

    public:
        int uuid;
        jshort* data;
        int data_size;
        jfloat sample_rate;
        jfloat initial_attenuation;
        std::optional<std::tuple<int, int>>  loop_points;
        int stereo_mode;
        VolumeEnvelope* volume_envelope;
        float pitch_shift;
        float filter_cutoff;
        float pan;
        ProfileBuffer* volume_profile;
        ProfileBuffer* pan_profile;
        std::vector<PitchedBuffer> data_buffers;

        explicit SampleHandle(
            jshort* data,
            int data_size,
            jfloat sample_rate,
            jfloat initial_attenuation,
            std::optional<std::tuple<int, int>> loop_points,
            int stereo_mode,
            VolumeEnvelope* volume_envelope,
            float pitch_shift,
            float filter_cutoff,
            float pan,
            ProfileBuffer* volume_profile,
            ProfileBuffer* pan_profile,
            std::optional<std::vector<PitchedBuffer>> data_buffers
        ) {
            this->uuid = SampleHandleUUIDGen++;
            this->data = data;
            this->data_size = data_size;
            this->sample_rate = sample_rate;
            this->initial_attenuation = initial_attenuation;
            this->loop_points = loop_points;
            this->stereo_mode = stereo_mode;
            this->volume_envelope = volume_envelope;
            this->pitch_shift = pitch_shift;
            this->filter_cutoff = filter_cutoff;
            this->pan = pan;
            this->volume_profile = volume_profile;
            this->pan_profile = pan_profile;

            this->secondary_setup(data_buffers);
        }

        void secondary_setup(std::optional<std::vector<PitchedBuffer>> data_buffers) {
            this->RC = 1 / (this->filter_cutoff * 2 * M_PI);
            this->initial_frame_factor = 1 / pow(10, this->initial_attenuation);
            this->previous_frame = 0;
            this->working_frame = 0;
            this->release_frame.reset();
            this->kill_frame.reset();
            this->is_dead = false;
            this->active_buffer = 0;

            this->data_buffers.clear();
            if (data_buffers.has_value()) {
                for (auto buffer: data_buffers.value()) {
                    this->data_buffers.push_back(PitchedBuffer {
                        buffer.data,
                        buffer.data_size,
                        buffer.pitch,
                        buffer.start,
                        buffer.end,
                        buffer.is_loop,
                        buffer.virtual_position,
                        buffer.pitch_adjustment,
                        buffer.virtual_size,
                        buffer.adjusted_pitch
                    });
                }
            } else if (this->loop_points.has_value() && std::get<0>(this->loop_points.value()) != std::get<1>(this->loop_points.value())) {
                this->data_buffers.push_back(
                  PitchedBuffer(
                      this->data,
                      this->data_size,
                      this->pitch_shift,
                      0,
                      std::get<0>(this->loop_points.value()),
                      true
                  )
                );
                this->data_buffers.push_back(
                  PitchedBuffer(
                      this->data, this->data_size,
                      this->pitch_shift,
                      std::get<0>(this->loop_points.value()),
                      std::get<1>(this->loop_points.value()),
                      true
                  )
                );
                this->data_buffers.push_back(
                    PitchedBuffer(
                        this->data, this->data_size,
                        this->pitch_shift,
                        std::get<1>(this->loop_points.value()),
                        this->data_size,
                        true
                    )
                );

            } else {
                this->data_buffers.push_back(
                    PitchedBuffer (
                        this->data,
                        this->data_size,
                        this->pitch_shift,
                        0,
                        this->data_size,
                        false
                    )
                );
            }
        }

        ~SampleHandle() = default;

        void set_release_frame(int frame) {
            this->release_frame = frame;
        }
        void set_working_frame(int frame) {
            this->working_frame = frame;
            if (this->kill_frame.has_value() && this->working_frame >= this->kill_frame.value()) {
                this->is_dead = true;
                return;
            }

            if (this->release_frame.has_value() && this->working_frame >= this->release_frame.value() + this->volume_envelope->frames_release) {
                this->is_dead = true;
                return;
            }

            this->volume_profile->set_frame(frame);
            this->pan_profile->set_frame(frame);

            try {
                if (!release_frame.has_value() || release_frame.value() > frame) {
                    if (!this->loop_points.has_value() || frame < this->data_buffers[0].virtual_size) {
                        this->data_buffers[0].set_position(frame);
                        this->active_buffer = 0;
                    } else {
                        this->data_buffers[1].set_position((frame - this->data_buffers[0].virtual_size));
                        this->active_buffer = 1;
                    }
                } else if (this->loop_points.has_value() && std::get<0>(this->loop_points.value()) < std::get<1>(this->loop_points.value())) {
                    if (frame < this->data_buffers[0].virtual_size) {
                        this->data_buffers[0].set_position(frame);
                        this->active_buffer = 0;
                    } else if (frame < this->data_buffers[1].virtual_size) {
                        this->data_buffers[1].set_position(frame - this->data_buffers[0].virtual_size);
                        this->active_buffer = 1;
                    } else {
                        int remainder = frame - this->release_frame.value();
                        int loop_size = std::get<1>(this->loop_points.value()) - std::get<0>(this->loop_points.value());
                        if (remainder < loop_size) {
                            this->data_buffers[1].set_position(remainder);
                            this->active_buffer = 1;
                        } else {
                            int loop_count = (this->release_frame.value() - std::get<0>(this->loop_points.value())) / loop_size;
                            this->data_buffers[2].set_position(frame - this->data_buffers[0].virtual_size - (loop_count * this->data_buffers[1].virtual_size));
                            this->active_buffer = 2;
                        }
                    }
                } else {
                    this->data_buffers[0].set_position(frame);
                    this->active_buffer = 0;
                }
                this->is_dead = false;
            } catch (PitchedBufferOverflow) {
                this->is_dead = true;
            }
        }

        int get_release_duration() {
            return this->volume_envelope->frames_release;
        }

        PitchedBuffer* get_active_data_buffer() {
            return &this->data_buffers[this->active_buffer];
        }

        std::tuple<float, float> get_next_balance() {
            // TODO
        }

        std::optional<std::tuple<float, float>> get_next_frame() {
            if (this->is_dead) {
                return std::nullopt;
            }

            bool is_pressed = !this->release_frame.has_value() || (this->working_frame < this->release_frame.value());
            if (this->working_frame < this->volume_envelope->frames_delay) {
                this->working_frame += 1;
                this->previous_frame = 0;
                this->volume_profile->get_next();
                return std::make_tuple(0, 0);
            }

            float frame_factor = this->initial_frame_factor;
            if (this->working_frame - this->volume_envelope->frames_delay < this->volume_envelope->frames_attack) {
                float r = (this->working_frame - (float)this->volume_envelope->frames_delay) / (float)this->volume_envelope->frames_attack;
                frame_factor *= r;
            } else if (this->working_frame - this->volume_envelope->frames_attack - this->volume_envelope->frames_delay < this->volume_envelope->frames_hold) {
                // PASS
            } else if (this->volume_envelope->sustain_attenuation > 0) {
                int relative_frame = this->working_frame - this->volume_envelope->frames_delay - this->volume_envelope->frames_attack;
                if (relative_frame < this->volume_envelope->frames_decay) {
                    float r = ((float)relative_frame / (float)this->volume_envelope->frames_decay);
                    frame_factor /= pow(10, r * this->volume_envelope->sustain_attenuation);
                } else {
                    frame_factor /= this->volume_envelope->true_sustain_attenuation;
                }
            }

            if (this->get_active_data_buffer()->is_overflowing()) {
                if (!is_pressed || !this->loop_points.has_value()) {
                    if (this->active_buffer < this->data_buffers.size() - 1) {
                        this->active_buffer += 1;
                    } else {
                        this->is_dead = true;
                        return std::nullopt;
                    }
                } else if (this->active_buffer == 0) {
                    this->active_buffer += 1;
                    this->get_active_data_buffer()->set_position(0);
                }
            }

            if (!is_pressed) {
                int pos = 0;
                for (auto buffer: this->data_buffers) {
                    pos += buffer.virtual_size;
                }
                int release_frame_count = fmin(this->volume_envelope->frames_release, pos);

                int current_position_release = this->working_frame - this->release_frame.value();
                if (current_position_release < release_frame_count) {
                    frame_factor *= 1 - ((float)current_position_release / (float)release_frame_count);
                } else {
                    this->is_dead = true;
                    return std::nullopt;
                }
            }

            float use_volume = this->volume_profile->get_next();
            this->working_frame += 1;

            if (this->active_buffer >= this->data_buffers.size()) {
                this->is_dead = true;
                return std::nullopt;
            }

            float frame_value;
            try {
                frame_value = this->get_active_data_buffer()->get();
            } catch (PitchedBufferOverflow) {
                return std::nullopt;
            }

            return std::make_tuple(frame_value, frame_factor * use_volume);
        }

        void release_note() {
            this->set_release_frame(this->working_frame);
        }

        void set_kill_frame(int f) {
            this->kill_frame = f;
        }

        bool is_pressed() {
            return !this->release_frame.has_value() || this->release_frame.value() < this->working_frame;
        }

        std::optional<int> get_duration() {
            if (this->release_frame.has_value()) {
                return this->release_frame.value() + this->get_release_duration();
            } else {
                return std::nullopt;
            }
        }

        void repitch(float adjustment) {
            for (auto buffer: this->data_buffers) {
                buffer.repitch(adjustment);
            }
        }
};

#endif //PAGAN_SAMPLEHANDLE_H