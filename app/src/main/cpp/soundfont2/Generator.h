//
// Created by pent on 4/16/25.
//

#ifndef PAGAN_GENERATOR_H
#define PAGAN_GENERATOR_H

#include <cmath>
#include <tuple>

class Generator {
    public:
        int sfGenOp;
        int shAmount;
        int wAmount;

        int get_int() {
            return this->shAmount + (this->wAmount * 256);
        }

        int get_int_signed() {
            int output;

            if ((this->wAmount & 0x80) == 0x80) {
                int w_amount = this->wAmount & 0x7F;
                int u = (w_amount * 256) + this->shAmount;
                output = 0 - ((u ^ 0x00007FFF) + 1);
            } else {
                output = this->shAmount + (this->wAmount * 256);
            }

            return output;
        }

        float get_timecent() {
            int signed_value = this->get_int_signed();

            float output;
            if (signed_value == -32768) {
                output = 0;
            } else {
                float p = (float)signed_value / 1200;
                output = pow(2, p);
            }

            return output;
        }

        std::tuple<int, int> get_pair() {
            return std::tuple {
                this->shAmount,
                this->wAmount
            };
        }

};

#endif //PAGAN_GENERATOR_H
