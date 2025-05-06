//
// Created by pent on 4/17/25.
//

#ifndef PAGAN_INSTRUMENTDIRECTIVE_H
#define PAGAN_INSTRUMENTDIRECTIVE_H

#include "Generated.h"
#include "Instrument.h"
#include <unordered_map>

class InstrumentDirective: public Generated {
    //unordered_map<int, std::set<Modulator>> modulators;
    public:
        std::optional<Instrument> instrument;
        // void add_modulator(Modulator modulator) {
        // }

        void apply_generator(Generator generator) {
            return; // 0x29 is handled in SoundFont
        }
};


#endif //PAGAN_INSTRUMENTDIRECTIVE_H
