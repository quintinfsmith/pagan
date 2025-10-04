//
// Created by pent on 10/2/25.
//

#ifndef PAGAN_LOWPASSBUFFER_H
#define PAGAN_LOWPASSBUFFER_H

#include "EffectProfileBuffer.h"

class LowPassBuffer: public EffectProfileBuffer {
    constexpr static float minimum_threshold = .0001;
    constexpr static int lag = 2;
    float previous_filtered_left[LowPassBuffer::lag]{};
    float previous_filtered_right[LowPassBuffer::lag]{};
    float previous_unfiltered_left[LowPassBuffer::lag]{};
    float previous_unfiltered_right[LowPassBuffer::lag]{};

    float working_sample_rate;
    float working_cutoff;
    float working_q;
    float a[LowPassBuffer::lag]{};
    float b[LowPassBuffer::lag]{};

    bool initial_apply = false;

    public:
        explicit LowPassBuffer(LowPassBuffer* original): EffectProfileBuffer(original) {
            for (int i = 0; i < LowPassBuffer::lag; i++) {
                this->previous_filtered_left[i] = original->previous_filtered_left[i];
                this->previous_filtered_right[i] = original->previous_filtered_right[i];
                this->previous_unfiltered_left[i] = original->previous_unfiltered_left[i];
                this->previous_unfiltered_right[i] = original->previous_unfiltered_right[i];
                this->a[i] = original->a[i];
                this->b[i] = original->b[i];
            }
            this->working_sample_rate = original->working_sample_rate;
            this->working_cutoff = original->working_cutoff;
            this->working_q = original->working_q;
            this->initial_apply = false;
        }

        LowPassBuffer(ControllerEventData* controller_event_data, int start_frame): EffectProfileBuffer(controller_event_data, start_frame) {

            for (int i = 0; i < LowPassBuffer::lag; i++) {
                this->previous_filtered_left[i] = 0;
                this->previous_filtered_right[i] = 0;
                this->previous_unfiltered_left[i] = 0;
                this->previous_unfiltered_right[i] = 0;
                this->a[i] = 0;
                this->b[i] = 0;
            }

            this->working_sample_rate = 0;
            this->working_cutoff = 0;
            this->working_q = 0;

            this->initial_apply = false;
        }

        void value_check() {
            float* next = this->get_next();
            float sample_rate = next[0];
            float cutoff = next[1];
            float q = next[2];
            if (this->working_sample_rate == sample_rate && this->working_cutoff == cutoff && this->working_q == q) return;
            this->working_sample_rate = sample_rate;
            this->working_cutoff = cutoff;
            this->working_q = q;

            if (cutoff == 0) return;

            float correction_factor = 1;
            float corrected_cutoff_freq = tan(M_PI * this->working_cutoff / sample_rate) / correction_factor;
            float k_0 = sqrt(2) * corrected_cutoff_freq;
            float k_1 = corrected_cutoff_freq * corrected_cutoff_freq;
            this->a[0] = k_1 / (1 + k_0 + k_1);
            this->a[1] = 2 * this->a[0];
            float k_2 = this->a[1] / k_1;
            this->b[0] = k_2 - this->a[1];
            this->b[1] = 1 - this->a[1] - k_2;
        }

        void apply(float* working_array, int array_size) override {
            this->initial_apply = true;
            for (int i = 0; i < array_size; i++) {
                this->value_check();
                if (this->working_cutoff == 0) continue;

                float input_left = working_array[i];
                float input_right = working_array[i + array_size];

                float output_left = (this->a[0] * input_left)
                    + (this->a[1] * this->previous_unfiltered_left[0])
                    + (this->a[0] * this->previous_unfiltered_left[1])
                    + (this->b[0] * this->previous_filtered_left[0])
                    + (this->b[1] * this->previous_filtered_left[1]);
                float output_right = (this->a[0] * input_right)
                    + (this->a[1] * this->previous_unfiltered_right[0])
                    + (this->a[0] * this->previous_unfiltered_right[1])
                    + (this->b[0] * this->previous_filtered_right[0])
                    + (this->b[1] * this->previous_filtered_right[1]);


                for (int j = LowPassBuffer::lag - 1; j > 0; j--) {
                    this->previous_unfiltered_left[j] = this->previous_unfiltered_left[j - 1];
                    this->previous_unfiltered_right[j] = this->previous_unfiltered_right[j - 1];
                    this->previous_filtered_left[j] = this->previous_filtered_left[j - 1];
                    this->previous_filtered_right[j] = this->previous_filtered_right[j - 1];
                }

                working_array[i] = output_left;
                working_array[i + array_size] = output_right;

                this->previous_unfiltered_left[0] = input_left;
                this->previous_unfiltered_right[0] = input_right;
                this->previous_filtered_left[0] = output_left;
                this->previous_filtered_right[0] = output_right;
            }
        }

        bool in_smoothing() const {
            return !this->initial_apply ||  fabs(this->previous_unfiltered_right[0]) > LowPassBuffer::minimum_threshold || fabs(this->previous_unfiltered_left[0]) > LowPassBuffer::minimum_threshold;
        }
};

#endif //PAGAN_LOWPASSBUFFER_H
