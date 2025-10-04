//
// Created by pent on 10/2/25.
//

#ifndef PAGAN_DELAYBUFFER_H
#define PAGAN_DELAYBUFFER_H
#include "EffectProfileBuffer.h"

class DelayedFrameValue {
    public:
        float left = 0;
        float right = 0;
        int count = 0;
        DelayedFrameValue* next = nullptr;
        DelayedFrameValue(float left, float right, int count, DelayedFrameValue* next) {
            this->left = left;
            this->right = right;
            this->count = count;
            this->next = next;
        }

        ~DelayedFrameValue() {
            if (this->next == nullptr) return;
            this->next->~DelayedFrameValue();
            free(this->next);
        }
    };

class DelayedFrame {
    DelayedFrame* next;
    public:
        DelayedFrameValue* value_chain;

        DelayedFrame() {
            this->next = nullptr;
            this->value_chain = nullptr;
        }

        ~DelayedFrame() {
            if (this->value_chain != nullptr) {
                this->value_chain->~DelayedFrameValue();
                free(this->value_chain);
            }
        }

        void set_next(DelayedFrame* next_frame) {
            this->next = next_frame;
        }

        DelayedFrame* get_next() {
            return this->next;
        }

        DelayedFrame* init_next() {
            this->next = (DelayedFrame*)malloc(sizeof(DelayedFrame));
            new (this->next) DelayedFrame();
            return this->next;
        }

        void get_values(float* pair) {
            DelayedFrameValue* working_ptr = this->value_chain;
            while (working_ptr != nullptr) {
                if (working_ptr->count > 0) {
                    pair[0] += working_ptr->left;
                    pair[1] += working_ptr->right;
                }
                working_ptr = working_ptr->next;
            }
        }

        void add_value(float left, float right, int repeat) {
            DelayedFrameValue* working_ptr = this->value_chain;
            auto* new_value = (DelayedFrameValue*)malloc(sizeof(DelayedFrameValue));
            new (new_value) DelayedFrameValue(left, right, repeat, working_ptr);
            this->value_chain = new_value;
        }

        // return number of values removed
        int decay(float decay, int echo_limit) {
            DelayedFrameValue* working_ptr = this->value_chain;
            while (working_ptr != nullptr) {
                if (working_ptr->count > 0) {
                    working_ptr->left *= decay;
                    working_ptr->right *= decay;
                    working_ptr->count -= 1;
                    if (working_ptr->count > echo_limit) {
                        working_ptr->count = echo_limit;
                    }
                }
                working_ptr = working_ptr->next;
            }

            int output = 0;
            // Prune values with count <= 0;
            // remove dead initial values first
            working_ptr = this->value_chain;
            while (working_ptr != nullptr && working_ptr->count == 0) {
                DelayedFrameValue* orig = working_ptr;
                working_ptr = working_ptr->next;

                orig->next = nullptr;
                orig->~DelayedFrameValue();
                free(orig);

                output++;
            }

            this->value_chain = working_ptr;

            if (working_ptr == nullptr) return output;

            // now remove dead values in chain
            while (working_ptr->next != nullptr) {
                if (working_ptr->next->count == 0) {
                    DelayedFrameValue* orig = working_ptr->next;
                    working_ptr->next = working_ptr->next->next;
                    orig->next = nullptr;

                    orig->~DelayedFrameValue();
                    free(orig);

                    output++;
                } else {
                    working_ptr = working_ptr->next;
                }
            }

            return output;
        }

        void destroy_chain() {
            // Since the chain is a loop, we set the first next to null to create a terminal condition
            DelayedFrame* working_ptr = this->get_next();
            this->next = nullptr;

            while (working_ptr != nullptr) {
                DelayedFrame *tmp = working_ptr->get_next();

                working_ptr->~DelayedFrame();
                free(working_ptr);

                working_ptr = tmp;
            }
        }
};

class DelayBuffer: public EffectProfileBuffer {
    float active_delay = 0;
    int active_fpb = 0; // Frames Per Beat
    int active_delay_in_frames = 0;
    int active_value_count = 0;

    DelayedFrame* active_input_frame;

    void delete_chain() {
        if (this->active_input_frame == nullptr) return;
        this->active_value_count = 0;

        this->active_input_frame->destroy_chain();
        this->active_input_frame = nullptr;
    }

    void create_chain() {
        if (this->active_delay_in_frames == 0) return;

        auto* working_ptr = (DelayedFrame*)malloc(sizeof(DelayedFrame));
        new (working_ptr) DelayedFrame();

        this->active_value_count = 0;
        this->active_input_frame = working_ptr;

        for (int i = 0; i < this->active_delay_in_frames - 1; i++) {
            working_ptr = working_ptr->init_next();
        }

        working_ptr->set_next(this->active_input_frame);
    }

    void set_chain_size(int next_fpb, float next_delay) {
        if (next_fpb == this->active_fpb && this->active_delay == next_delay) return;

        DelayedFrame* original_ptr = this->active_input_frame;
        int original_size = this->active_delay_in_frames;

        this->active_delay = next_delay;
        this->active_fpb = next_fpb;
        this->active_delay_in_frames = (int)(this->active_delay * (float)this->active_fpb);
        this->create_chain();

        if (original_ptr != nullptr) {
            DelayedFrame* working_ptr = this->active_input_frame;
            int count = min(original_size, this->active_delay_in_frames);
            for (int i = 0; i < count; i++) {
                DelayedFrameValue* node = original_ptr->value_chain;
                while (node != nullptr) {
                    working_ptr->add_value(node->left, node->right, node->count);
                    this->active_value_count += 1;
                    node = node->next;
                }

                original_ptr = original_ptr->get_next();
                working_ptr = working_ptr->get_next();
            }

            original_ptr->destroy_chain();
        }
    }

    void cycle() {
        this->active_input_frame = this->active_input_frame->get_next();
    }

public:
    DelayBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) {
        this->active_input_frame = nullptr;
        this->active_delay = 0;
        this->active_fpb = 0; // Frames Per Beat
        this->active_delay_in_frames = 0;
        this->active_value_count = 0;
    }

    explicit DelayBuffer(DelayBuffer* original): EffectProfileBuffer(original) {
        this->active_input_frame = original->active_input_frame;
        this->active_delay = original->active_delay;
        this->active_fpb = original->active_fpb; // Frames Per Beat
        this->active_delay_in_frames = original->active_delay_in_frames;
        this->active_value_count = original->active_value_count;
    }

    void apply(float* working_array, int array_size) override {
        for (int i = 0; i < array_size; i++) {
            float* frame_data = EffectProfileBuffer::get_next();

            int echo = frame_data[1];
            if (echo == 0) {
                this->delete_chain();
                continue;
            }

            float next_delay = frame_data[0];
            float fade = frame_data[2];
            int next_fpb = frame_data[3];

            this->set_chain_size(next_fpb, next_delay);

            if (this->active_input_frame == nullptr) continue;

            if (working_array[i] != 0 || working_array[i + array_size] != 0) {
                this->active_input_frame->add_value(working_array[i] * fade, working_array[i + array_size] * fade, echo);
                this->active_value_count++;
            }

            auto* output_frame = this->active_input_frame->get_next();
            float decay_value[2] = {0,0};
            output_frame->get_values(decay_value);
            this->active_value_count -= output_frame->decay(fade, echo);

            working_array[i] += decay_value[0];
            working_array[i + array_size] += decay_value[1];

            this->cycle();
        }
    }

    ~DelayBuffer() {
        if (this->active_input_frame == nullptr) return;
        this->active_input_frame->destroy_chain();
    }

    bool has_pending_echoes() {
        if (this->active_input_frame == nullptr) return false;
        return this->active_value_count != 0;
    }
};

#endif //PAGAN_DELAYBUFFER_H
