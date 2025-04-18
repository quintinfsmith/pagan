//
// Created by pent on 4/17/25.
//

#ifndef PAGAN_PRESET_H
#define PAGAN_PRESET_H
#include <unordered_map>
#include "InstrumentDirective.h"

//companion object {
//        var next_uid: Int = 0
//        fun gen_uid(): Int {
//            return next_uid++
//        }
//}

class Preset {
    std::string name;
    int preset;
    int bank;
    //val uid = Preset.gen_uid()
    private:
        std::unordered_map<int, InstrumentDirective> instruments;
        InstrumentDirective global_zone;
        std::set<int> quick_instrument_ref_vel[128];
        std::set<int> quick_instrument_ref_key[128];
    public:
        void set_global_zone(InstrumentDirective new_zone) {
            this->global_zone = std::move(new_zone);
        }

        void add_instrument(InstrumentDirective new_instrument) {
            int uuid = 0; // TODO
            std::tuple<int, int> key_range;
            if (new_instrument.key_range.has_value()) {
                key_range = new_instrument.key_range.value();
            } else {
                key_range = { 0, 127 };
            }

            for (int i = std::get<0>(key_range); i < std::get<1>(key_range); i++) {
                this->quick_instrument_ref_key[i].insert(uuid);
            }

            std::tuple<int, int> velocity_range;
            if (new_instrument.velocity_range.has_value()) {
                velocity_range = new_instrument.velocity_range.value();
            } else {
                velocity_range = { 0, 127 };
            }

            for (int i = std::get<0>(velocity_range); i < std::get<1>(velocity_range); i++) {
                this->quick_instrument_ref_vel[i].insert(uuid);
            }

            this->instruments[uuid] = std::move(new_instrument);
        }

        std::set<InstrumentDirective*> get_instrument(int key, int velocity) {
            std::set<InstrumentDirective*> output = {};
            std::set<int> uuids;
            std::set_intersection(
                this->quick_instrument_ref_key[key].begin(),
                this->quick_instrument_ref_key[key].end(),
                this->quick_instrument_ref_vel[velocity].begin(),
                this->quick_instrument_ref_vel[velocity].end(),
                inserter(uuids, uuids.begin())
            );
            for (auto id: uuids) {
                output.insert(&this->instruments[id]);
            }

            return output;
        }
};

#endif //PAGAN_PRESET_H
