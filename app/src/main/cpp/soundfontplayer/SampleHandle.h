//
// Created by pent on 4/19/25.
//

#ifndef PAGAN_SAMPLEHANDLE_H
#define PAGAN_SAMPLEHANDLE_H
#include <string>
#include <sstream>
#include <unordered_map>
#include "PitchedBuffer.h"
#include "EffectProfileBuffer.h"
#include "VolumeEnvelope.h"
#include <cmath>
#include "Oscillator.h"

class NoFrameDataException: public std::exception {};

int SampleHandleUUIDGen = 0;
float MAX_VOLUME = 1 / 1.27;

// Values correspond with values defined in EffectType.kt
const int TYPE_VOLUME = 1;
const int TYPE_REVERB = 2;
const int TYPE_PAN = 5;
const int TYPE_DELAY = 3;
const int TYPE_EQUALIZER = 6;

// TODO Modulations
// modulation_envelope, modulation_lfo, modulators
class SampleHandle {
    float RC;
    float initial_frame_factor;
    // int uuid;

    public:
        int uuid;
        SampleData* data;
        jint sample_rate;
        jfloat initial_attenuation;
        int loop_start;
        int loop_end;
        int stereo_mode;
        VolumeEnvelope* volume_envelope;
        float pitch_shift;
        float filter_cutoff;
        float pan;
        PitchedBuffer** data_buffers;
        int buffer_count;
        float smoothing_factor;

        int working_frame;
        int release_frame;
        int kill_frame;
        bool is_dead;
        int active_buffer;
        float previous_value = 0;

        Oscillator* vibrato_oscillator;
        int vibrato_delay;
        float vibrato_pitch;

        explicit SampleHandle(
                SampleData* data,
                jfloat sample_rate,
                jfloat initial_attenuation,
                jint loop_start,
                jint loop_end,
                int stereo_mode,
                VolumeEnvelope* volume_envelope,
                float pitch_shift,
                float filter_cutoff,
                float pan,
                PitchedBuffer** data_buffers,
                int buffer_count,
                Oscillator* vibrato_oscillator,
                float vibrato_delay,
                float vibrato_pitch
        ) {
            this->uuid = SampleHandleUUIDGen++;
            this->data = data;
            this->previous_value = 0;
            this->sample_rate = sample_rate;
            this->initial_attenuation = initial_attenuation;
            this->loop_end = loop_end;
            this->loop_start = loop_start;
            this->stereo_mode = stereo_mode;
            this->volume_envelope = volume_envelope;
            this->pitch_shift = pitch_shift;
            this->filter_cutoff = filter_cutoff;
            this->pan = pan;
            this->vibrato_oscillator = vibrato_oscillator;
            this->vibrato_delay = (int)(vibrato_delay * this->sample_rate);
            this->vibrato_pitch = vibrato_pitch;

            this->secondary_setup(data_buffers, buffer_count);
        }

        void secondary_setup(PitchedBuffer** input_buffers, int count) {
            this->RC = 1.0 / (this->filter_cutoff * M_2_PI);
            float dt =  1.0 / this->sample_rate;
            this->smoothing_factor = dt / (this->RC + dt);

            this->initial_frame_factor = 1 / std::pow(10, this->initial_attenuation);
            this->working_frame = 0;
            this->release_frame = -1;
            this->kill_frame = -1;
            this->is_dead = false;
            this->active_buffer = 0;

            if (count > 0) {
                this->buffer_count = count;
                this->data_buffers = (PitchedBuffer**)malloc(sizeof(PitchedBuffer*) * count);
                for (int i = 0; i < count; i++) {
                    PitchedBuffer* buffer = input_buffers[i];
                    auto* ptr = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
                    buffer->copy_to(ptr);
                    this->data_buffers[i] = ptr;
                }
            } else if (this->loop_start > -1 && this->loop_start != this->loop_end) {
                this->data_buffers = (PitchedBuffer**)malloc(sizeof(PitchedBuffer*) * 3);
                auto* ptr = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
                ptr->virtual_position = 0;
                ptr->real_position_preradix = 0;
                ptr->real_position_postradix = 0;
                ptr->data = this->data;
                ptr->default_pitch = this->pitch_shift;
                ptr->start = 0;
                ptr->end = this->loop_start;
                ptr->is_loop = false;
                ptr->repitch(1);
                this->data_buffers[0] = ptr;

                ptr = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
                ptr->virtual_position = 0;
                ptr->real_position_preradix = 0;
                ptr->real_position_postradix = 0;
                ptr->data = this->data;
                ptr->default_pitch = this->pitch_shift;
                ptr->start = this->loop_start;
                ptr->end = this->loop_end;
                ptr->is_loop = true;
                ptr->repitch(1);
                this->data_buffers[1] = ptr;

                ptr = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
                ptr->virtual_position = 0;
                ptr->real_position_preradix = 0;
                ptr->real_position_postradix = 0;
                ptr->data = this->data;
                ptr->default_pitch = this->pitch_shift;
                ptr->start = this->loop_end;
                ptr->end = this->data->size;
                ptr->repitch(1);
                this->data_buffers[2] = ptr;

                this->buffer_count = 3;
            } else {
                this->data_buffers = (PitchedBuffer**)malloc(sizeof(PitchedBuffer*));
                auto* ptr = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));

                ptr->virtual_position = 0;
                ptr->real_position_preradix = 0;
                ptr->real_position_postradix = 0;
                ptr->data = this->data;
                ptr->default_pitch = this->pitch_shift;
                ptr->start = 0;
                ptr->end = this->data->size;
                ptr->is_loop = false;
                ptr->repitch(1);
                this->data_buffers[0] = ptr;
                this->buffer_count = 1;
            }
        }

        ~SampleHandle() {
            for (int i = 0; i < this->buffer_count; i++) {
                delete this->data_buffers[i];
            }
            delete[] this->data_buffers;
            delete this->volume_envelope;
        };

        void set_release_frame(int frame) {
            this->release_frame = frame;
        }

        void set_working_frame(int frame) {
            this->previous_value = 0;
            this->working_frame = frame;
            if (this->kill_frame > -1 && this->working_frame >= this->kill_frame) {
                this->is_dead = true;
                return;
            }

            if (this->release_frame > -1 && this->working_frame >= this->release_frame + this->volume_envelope->frames_release) {
                this->is_dead = true;
                return;
            }

            try {
                if (release_frame == -1 || release_frame > frame) {
                    if (this->loop_start == -1 || frame < this->data_buffers[0]->virtual_size) {
                        this->data_buffers[0]->set_position(frame);
                        this->active_buffer = 0;
                    } else {
                        this->data_buffers[1]->set_position((frame - this->data_buffers[0]->virtual_size));
                        this->active_buffer = 1;
                    }
                } else if (this->loop_start > -1 && this->loop_start < this->loop_end) {
                    if (frame < this->data_buffers[0]->virtual_size) {
                        this->data_buffers[0]->set_position(frame);
                        this->active_buffer = 0;
                    } else if (frame < this->data_buffers[1]->virtual_size) {
                        this->data_buffers[1]->set_position(frame - this->data_buffers[0]->virtual_size);
                        this->active_buffer = 1;
                    } else {
                        int remainder = frame - this->release_frame;
                        int loop_size = this->loop_end - this->loop_start;
                        if (remainder < loop_size) {
                            this->data_buffers[1]->set_position(remainder);
                            this->active_buffer = 1;
                        } else {
                            int loop_count = (this->release_frame - this->loop_start) / loop_size;
                            this->data_buffers[2]->set_position(frame - this->data_buffers[0]->virtual_size - (loop_count * this->data_buffers[1]->virtual_size));
                            this->active_buffer = 2;
                        }
                    }
                } else {
                    this->data_buffers[0]->set_position(frame);
                    this->active_buffer = 0;
                }
                this->is_dead = false;
            } catch (PitchedBufferOverflow& e) {
                this->is_dead = true;
            }
        }

        int get_release_duration() const {
            return this->volume_envelope->frames_release;
        }

        PitchedBuffer* get_active_data_buffer() const {
            return this->data_buffers[this->active_buffer];
        }

        // TODO: Calculate on init
        std::tuple<float, float> get_balance() {
            std::tuple<float, float> output;
            switch (this->stereo_mode & 0x000F) {
                case 0x01: {
                    output = std::make_tuple(
                        1 + this->pan,
                        -1 * (-1 + this->pan)
                    );
                    break;
                }

                case 0x02: {
                    output = std::make_tuple(
                        1 + this->pan,
                        0
                    );
                    break;
                }

                case 0x04: {
                    output = std::make_tuple(
                        0,
                        -1 * (-1 + this->pan)
                    );
                    break;
                }

                default: {
                    // TODO: LINKED, but treat as mono for now
                    output = std::make_tuple(
                            1 + this->pan,
                            -1 * (-1 + this->pan)
                    );
                    break;
                }
            }

            return output;
        }

        // Instead of multiplexing the channels, alternating between left and right
        // The 2 channels are split length-wise. the first entire half is the right channel, followed by the left
        // This is so its easier to FFT later in the process.
        void get_next_frames(float* buffer, int target_size, int left_padding) {
            int actual_size = target_size;
            std::tuple<float, float> working_pan = this->get_balance();

            // No need to smooth the left padding since the handle won't start, then have a gap, then continue
            for (int i = 0; i < left_padding; i++) {
                buffer[i] = 0;
                buffer[i + target_size] = 0;
            }
            for (int i = left_padding; i < target_size; i++) {
                float frame;
                try {
                    frame = this->get_next_frame();
                } catch (NoFrameDataException &e) {
                    actual_size = i;
                    break;
                }

                float v = this->previous_value + (this->smoothing_factor * (frame - this->previous_value));
                buffer[i] = v * std::get<0>(working_pan);
                buffer[i + target_size] = v * std::get<1>(working_pan);

                this->previous_value = v;
            }

            // Need to smooth into silence
            for (int i = actual_size; i < target_size; i++) {
                if (this->previous_value != 0) {
                    float v = this->previous_value + (this->smoothing_factor * (0 - this->previous_value));
                    buffer[i] = v * std::get<0>(working_pan);
                    buffer[i + target_size] = v * std::get<1>(working_pan);
                    this->previous_value = v;
                } else {
                    buffer[i] = 0;
                    buffer[i + target_size] = 0;
                }
            }
        }

        float get_next_frame() {
            if (this->is_dead) {
                throw NoFrameDataException();
            }

            bool is_pressed = this->is_pressed();
            if (this->working_frame < this->volume_envelope->frames_delay) {
                this->working_frame += 1;
                return 0;
            }

            float frame_factor = this->initial_frame_factor;
            if (this->working_frame - this->volume_envelope->frames_delay < this->volume_envelope->frames_attack) {
                float r = ((float)this->working_frame - (float)this->volume_envelope->frames_delay) / (float)this->volume_envelope->frames_attack;
                frame_factor *= r;
            } else if (this->working_frame - this->volume_envelope->frames_attack - this->volume_envelope->frames_delay < this->volume_envelope->frames_hold) {
                // PASS
            } else if (this->volume_envelope->sustain_attenuation > 0) {
                int relative_frame = this->working_frame - this->volume_envelope->frames_delay - this->volume_envelope->frames_attack;
                if (relative_frame < this->volume_envelope->frames_decay) {
                    float r = ((float)relative_frame / (float)this->volume_envelope->frames_decay);
                    frame_factor /= std::pow((float)10, r * this->volume_envelope->sustain_attenuation);
                } else {
                    frame_factor /= this->volume_envelope->true_sustain_attenuation;
                }
            }

            if (this->get_active_data_buffer()->is_overflowing()) {
                if (!is_pressed || this->loop_start == -1) {
                    if (this->active_buffer < this->buffer_count - 1) {
                        this->active_buffer += 1;
                    } else {
                        this->is_dead = true;
                        throw NoFrameDataException();
                    }
                } else if (this->active_buffer == 0) {
                    this->active_buffer += 1;
                    this->get_active_data_buffer()->set_position(0);
                }
            }

            if (!is_pressed) {
                int pos = 0;
                for (int i = 0; i < this->buffer_count; i++) {
                    pos += this->data_buffers[i]->virtual_size;
                }
                int release_frame_count = std::min(this->volume_envelope->frames_release, pos - this->release_frame);
                int current_position_release = this->working_frame - this->release_frame;
                if (current_position_release < release_frame_count) {
                    frame_factor *= 1 - ((float)current_position_release / (float)release_frame_count);
                } else {
                    this->is_dead = true;
                    throw NoFrameDataException();
                }
            }

            this->working_frame += 1;
            if (this->active_buffer >= this->buffer_count) {
                this->is_dead = true;
                throw NoFrameDataException();
            }

            float frame_value;
            try {
                frame_value = this->get_active_data_buffer()->get();
            } catch (PitchedBufferOverflow& e) {
                this->is_dead = true;
                throw NoFrameDataException();
            }

            if (this->vibrato_oscillator != nullptr && this->vibrato_delay < this->working_frame) {
                this->get_active_data_buffer()->repitch(1 + ((this->vibrato_pitch - 1) * this->vibrato_oscillator->next()));
            }

            return frame_value * frame_factor * MAX_VOLUME;
        }

        void release_note() {
            this->set_release_frame(this->working_frame);
        }

        void set_kill_frame(int f) {
            this->kill_frame = f;
        }

        bool is_pressed() {
            return this->release_frame == -1 || this->release_frame > this->working_frame;
        }

        int get_duration() {
            if (this->release_frame > -1) {
                return this->release_frame + this->get_release_duration();
            } else {
                return -1;
            }
        }

        void repitch(float adjustment) const {
            for (int i = 0; i < this->buffer_count; i++) {
                this->data_buffers[i]->repitch(adjustment);
            }
        }
};

#endif //PAGAN_SAMPLEHANDLE_H