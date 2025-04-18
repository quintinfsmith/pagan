//
// Created by pent on 4/17/25.
//

#ifndef PAGAN_INSTRUMENT_H
#define PAGAN_INSTRUMENT_H
#include <unordered_map>
#include <set>
#include <utility>
#include "SampleDirective.h"

class Instrument {
    std::string name;
    public:
        void set_global_zone(SampleDirective new_zone) {
            this->global_zone = std::move(new_zone);
        }

        void add_sample(SampleDirective new_sample) {
            int uuid = 0; // TODO: GEn UUID

            std::tuple<int, int> key_range;
            if (new_sample.key_range.has_value()) {
                key_range = new_sample.key_range.value();
            } else {
                key_range = {0, 127};
            }

            for (int i = std::get<0>(key_range); i < std::get<1>(key_range); i++) {
                this->quick_ref_key[i].insert(uuid);
            }

            std::tuple<int, int> velocity_range;
            if (new_sample.velocity_range.has_value()) {
                velocity_range = new_sample.velocity_range.value();
            } else {
                velocity_range = {0, 127};
            }

            for (int i = std::get<0>(velocity_range); i < std::get<1>(velocity_range); i++) {
                this->quick_ref_vel[i].insert(uuid);
            }

            this->samples[uuid] = std::move(new_sample);
        }

        std::set<SampleDirective*> get_samples(int key, int velocity) {
            std::set<SampleDirective*> output = {};
            if (this->samples.empty()) {
                std::tuple<int, int> key_range;
                if (this->global_zone.key_range.has_value()) {
                    key_range = this->global_zone.key_range.value();
                } else {
                    key_range = {0, 127};
                }

                std::tuple<int, int> velocity_range;
                if (this->global_zone.velocity_range.has_value()) {
                    velocity_range = this->global_zone.velocity_range.value();
                } else {
                    velocity_range = {0, 127};
                }

                if (std::get<0>(key_range) < key && std::get<1>(key_range) >= key) {
                    output.insert(&this->global_zone);
                }
            } else {
                std::set<int> uuids;
                std::set_intersection(
                    this->quick_ref_key[key].begin(),
                    this->quick_ref_key[key].end(),
                    this->quick_ref_vel[velocity].begin(),
                    this->quick_ref_vel[velocity].end(),
                    inserter(uuids, uuids.begin())
                );
                for (auto id: uuids) {
                    output.insert(&this->samples[id]);
                }
            }
            return output;
        }

    private:
        std::unordered_map<int, SampleDirective> samples = {};
        SampleDirective global_zone = SampleDirective();
        std::set<int> quick_ref_vel[128];
        std::set<int> quick_ref_key[128];

};

#endif //PAGAN_INSTRUMENT_H
