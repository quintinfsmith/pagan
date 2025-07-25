#ifndef PAGAN_SAMPLEDIRECTIVE_H
#define PAGAN_SAMPLEDIRECTIVE_H

#include "Generated.h"
#include "Sample.h"
#include "GeneratorOperation.h"

class SampleDirective : public Generated {
    public:
        std::optional<Sample> sample;
        std::optional<int> sampleStartOffset;
        std::optional<int> sampleEndOffset;
        std::optional<int> loopStartOffset;
        std::optional<int> loopEndOffset;
        std::optional<int> sampleMode;
        std::optional<int> root_key;
        std::optional<int> exclusive_class;
        std::optional<int> keynum;
        std::optional<int> velocity;
        // val modulators = HashMap<Generator.Operation, MutableSet<Modulator>>()
        void apply_generator(Generator* generator) override {
            switch(generator->sfGenOp) {
                case 0x00: {
                    if (!this->sampleStartOffset.has_value()) {
                        this->sampleStartOffset = 0;
                    }
                    this->sampleStartOffset = this->sampleStartOffset.value() + generator->get_int_signed();
                    break;
                }
                case 0x01: {
                    this->sampleEndOffset = generator->get_int_signed();
                    break;
                }
                case 0x02: {
                    this->loopStartOffset = generator->get_int_signed();
                    break;
                }
                case 0x03: {
                    this->loopEndOffset = generator->get_int_signed();
                    break;
                }
                case 0x04: {
                    if (!this->sampleStartOffset.has_value()) {
                        this->sampleStartOffset = 0;
                    }
                    this->sampleStartOffset = this->sampleStartOffset.value() + (generator->get_int_signed() * 32768);
                    break;
                }
                case 0x0C: {
                    if (!this->sampleEndOffset.has_value()) {
                        this->sampleEndOffset = 0;
                    }
                    this->sampleEndOffset = this->sampleEndOffset.value() + (generator->get_int_signed() * 32768);
                    break;
                }
                case 0x2D: {
                    if (!this->loopStartOffset.has_value()) {
                        this->loopStartOffset = 0;
                    }
                    this->loopStartOffset = this->loopStartOffset.value() + (generator->get_int_signed() * 32768);
                    break;
                }
                case 0x2E: {
                    this->keynum = generator->get_int();
                    break;
                }
                case 0x2F: {
                    this->velocity = generator->get_int();
                    break;
                }
                case 0x32: {
                    if (!this->loopEndOffset.has_value()) {
                        this->loopEndOffset = 0;
                    }
                    this->loopEndOffset = this->loopEndOffset.value() + (generator->get_int_signed() * 32768);
                    break;
                }
                case 0x36: {
                    this->sampleMode = generator->get_int();
                    break;
                }
                case 0x39: {
                    this->exclusive_class = generator->get_int();
                    break;
                }
                case 0x3A: {
                    this->root_key = generator->get_int();
                    break;
                }
                default:
                    // ignore unknown
                    break;
            }
        }

};

#endif //PAGAN_SAMPLEDIRECTIVE_H
