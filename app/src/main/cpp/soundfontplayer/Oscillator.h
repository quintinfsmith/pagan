//
// Created by pent on 5/31/25.
//

#ifndef PAGAN_OSCILLATOR_H
#define PAGAN_OSCILLATOR_H

class Oscillator {
    int frame = 0;
    public:
        int period;
        int sample_rate;
        float frequency;
        Oscillator(int sample_rate, float frequency) {
            this->sample_rate = sample_rate;
            this->frequency = frequency;
            this->frame = 0;
            this->period = sample_rate / frequency;
        }

        float next() {
            int frame = this->frame;
            this->frame = (this->frame + 1) % (2 * this->period);
            // TODO: Make this Triangular
            return sinf( ((float)frame / (float)this->period) * M_PI_2);
        }
};


#endif //PAGAN_OSCILLATOR_H
