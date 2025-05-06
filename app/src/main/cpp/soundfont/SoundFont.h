//
// This Code is currently unused, but will be

#ifndef PAGAN_SOUNDFONT_H
#define PAGAN_SOUNDFONT_H

#include "Riff/Riff.h"
#include "Sample.h"
#include "Preset.h"
#include "Instrument.h"
#include <string>
#include <set>
#include <unordered_map>
#include <utility>

class NoIROMDeclared: public std::exception {
    public:
        NoIROMDeclared() {}
        const char* what() const throw() {
            return "No IROM Declared";
        }
};
class InvalidSampleIdPosition: public std::exception {
    public:
        InvalidSampleIdPosition() {}
        const char* what() const throw() {
            return "Invalid Sample Id Position";
        }
};

class SoundFont: public Riff {
    public:
        // Mandatory INFO
        std::tuple<int, int> ifil;
        std::string isng;
        std::string inam;
        std::unordered_map<std::string, std::vector<char>> pdta_chunks;

        //Optional INFO
        std::optional<std::string> irom = std::nullopt;
        std::optional<std::tuple<int, int>> iver = std::nullopt;
        std::optional<std::string> icrd = std::nullopt;
        std::optional<std::string> ieng = std::nullopt;
        std::optional<std::string> iprd = std::nullopt;
        std::optional<std::string> icop = std::nullopt;
        std::optional<std::string> icmt = std::nullopt;
        std::optional<std::string> isft = std::nullopt;

        explicit SoundFont(std::string path): Riff(path) {
            this->ifil = {0, 0};
            this->isng = "EMU8000";
            this->inam = "";
            if (this->type_cc != "sfbk") {
                // TODO: Throw Error
            }

            std::ifstream stream(this->path);
            char* info_chunk = this->get_chunk_data(&stream, &this->list_chunks[0]);
            int info_offset = this->list_chunks[0].index;

            for (const auto& header: this->sub_chunks[0]) {
                int header_offset = header.index + 8 - info_offset - 12;
                if (header.tag == "ifil") {
                    this->ifil = {
                        info_chunk[header_offset + 0] + (info_chunk[header_offset + 1] * 256),
                        info_chunk[header_offset + 2] + (info_chunk[header_offset + 3] * 256)
                    };
                } else if (header.tag == "isng") {
                    this->isng = "";
                    for (int j = 0; j < header.size; j++) {
                        this->isng += info_chunk[header_offset + j];
                    }
                } else if (header.tag == "INAM") {
                    this->inam = "";
                    for (int j = 0; j < header.size; j++) {
                        this->inam += info_chunk[header_offset + j];
                    }
                } else if (header.tag == "irom") {
                    std::string tmp_irom;
                    for (int j = 0; j < header.size; j++) {
                        tmp_irom += info_chunk[header_offset + j];
                    }
                    this->irom = tmp_irom;
                } else if (header.tag == "iver") {
                    this->iver = {
                        info_chunk[header_offset + 0] + (info_chunk[header_offset + 1] * 256),
                        info_chunk[header_offset + 2] + (info_chunk[header_offset + 3] * 256)
                    };
                } else if (header.tag == "ICRD") {
                    std::string tmp_icrd;
                    for (int j = 0; j < header.size; j++) {
                        tmp_icrd += info_chunk[header_offset + j];
                    }
                    this->icrd = tmp_icrd;
                } else if (header.tag == "IENG") {
                    std::string tmp_ieng;
                    for (int j = 0; j < header.size; j++) {
                        tmp_ieng += info_chunk[header_offset + j];
                    }
                    this->ieng = tmp_ieng;
                } else if (header.tag == "IPRD") {
                    std::string tmp_iprd;
                    for (int j = 0; j < header.size; j++) {
                        tmp_iprd += info_chunk[header_offset + j];
                    }
                    this->iprd = tmp_iprd;
                } else if (header.tag == "ICOP") {
                    std::string tmp_icop;
                    for (int j = 0; j < header.size; j++) {
                        tmp_icop += info_chunk[header_offset + j];
                    }
                    this->icop = tmp_icop;
                } else if (header.tag == "ICMT") {
                    std::string tmp_icmt;
                    for (int j = 0; j < header.size; j++) {
                        tmp_icmt += info_chunk[header_offset + j];
                    }
                    this->icmt = tmp_icmt;
                } else if (header.tag == "ISFT") {
                    std::string tmp_isft;
                    for (int j = 0; j < header.size; j++) {
                        tmp_isft += info_chunk[header_offset + j];
                    }
                    this->isft = tmp_isft;
                } else {
                }
            }

            char* pdta_chunk = this->get_chunk_data(&stream, &this->list_chunks[2]);
            std::unordered_map<std::string, std::vector<char>> pdta_chunks;

            int pdta_offset = this->list_chunks[2].index;
            for (auto header: this->sub_chunks[2]) {
                int offset = header.index + 8 - pdta_offset - 12;
                std::vector<char> working_vector;
                working_vector.reserve(header.size);
                for (int j = 0; j < header.size; j++) {
                    working_vector.push_back(pdta_chunk[offset + j]);
                }
                pdta_chunks[header.tag] = working_vector;
            }

            stream.close();
        }

        std::vector<Sample> get_samples(int start_index) {
            std::vector<Sample> output;
            int working_index = start_index;
            while (working_index != 0) {
                Sample working_sample = this->get_sample(working_index);
                working_index = working_sample.link_addr;
                output.push_back(std::move(working_sample));
            }
            return output;
        }

        std::set<std::tuple<std::string, int, int>> get_available_presets() {
            std::set<std::tuple<std::string, int, int>> output;
            std::vector<char> phdr_bytes = this->pdta_chunks["phdr"];

            for (int index = 0; index < (phdr_bytes.size() / 38) - 1; index++) {
                int offset = index * 38;
                std::string phdr_name;
                for (int j = 0; j < 38; j++) {
                    char b = phdr_bytes[j + offset];
                    if (b == 0) {
                        break;
                    }
                    phdr_name += b;
                }

                int current_program = phdr_bytes[offset + 20] + (phdr_bytes[offset + 21] * 256);
                int current_bank = phdr_bytes[offset + 22] + (phdr_bytes[offset + 23] * 256);

                output.insert({
                    phdr_name,
                    current_program,
                    current_bank
                });
            }

            return output;
        }

        Preset get_preset(int preset_index, int preset_bank) {
            int pbag_entry_size = 4;
            std::vector<char> phdr_bytes = this->pdta_chunks["phdr"];
            std::vector<Preset> output;
            for (int index = 0; index < (phdr_bytes.size() / 38) - 1; index++) {
                int offset = index * 38;
                std::string phdr_name;
                for (int j = 0; j < 20; j++) {
                    char b = phdr_bytes[j + offset];
                    if (b == 0) {
                        break;
                    }
                    phdr_name += b;
                }

                int current_index = phdr_bytes[offset + 20] + (phdr_bytes[offset + 21] * 256);
                int current_bank = phdr_bytes[offset + 22] + (phdr_bytes[offset + 23] * 256);

                if (preset_index != current_index || preset_bank != current_bank) {
                    continue;
                }

                Preset preset = Preset {
                    phdr_name,
                    current_index,
                    current_bank
                };

                int w_preset_bag_index = phdr_bytes[offset + 24] + (phdr_bytes[offset + 25] * 256);
                int next_w_preset_bag_index = phdr_bytes[38 + offset + 24] + (phdr_bytes[38 + offset + 25] * 256);
                int zone_count = next_w_preset_bag_index - w_preset_bag_index;
                std::vector<std::tuple<int, int, int, int>> pbag_pairs;
                for (int j = 0; j < zone_count; j++) {
                    char pbag_bytes[pbag_entry_size * 2];
                    for (int k = 0; k < pbag_entry_size * 2; k++) {
                        pbag_bytes[k] = this->pdta_chunks["pbag"][((j + w_preset_bag_index) * pbag_entry_size) + k];
                    }

                    pbag_pairs.push_back(
                        {
                            pbag_bytes[0] + (pbag_bytes[1]  * 256),
                            pbag_bytes[2] + (pbag_bytes[3]  * 256),
                            pbag_bytes[4] + (pbag_bytes[5]  * 256),
                            pbag_bytes[6] + (pbag_bytes[6]  * 256)
                        }
                    );
                }

                for (auto pbags: pbag_pairs) {
                    std::vector<Generator> generators_to_use = this->get_preset_generators(
                        std::get<0>(pbags),
                        std::get<1>(pbags)
                    );
                    // TODO
                    //std::vector<Modulator> modulators_to_use = this->get_preset_modulators(
                    //        std::get<0>(pbags),
                    //        std::get<1>(pbags)
                    //);

                    this->generate_preset(&preset, generators_to_use);
                }

                std::vector<Sample> ordered_samples;
                for (auto pair: preset.instruments) {
                    InstrumentDirective* instrument_directive = &pair.second;
                    if (!instrument_directive->instrument.has_value()) {
                        continue;
                    }
                    Instrument* instrument = &(instrument_directive->instrument.value());

                    if (!instrument->samples.empty()) {
                        for (auto pair_b: instrument->samples) {
                            if (!pair_b.second.sample.has_value()) {
                                continue;
                            }
                            Sample sample = pair_b.second.sample.value();
                            ordered_samples.push_back(sample);
                        }
                    } else {
                        if (instrument->global_zone.sample.has_value()) {
                            ordered_samples.push_back(
                                instrument->global_zone.sample.value()
                            );
                        }
                    }
                }

                std::sort(
                    ordered_samples.begin(),
                    ordered_samples.end(),
                    [](Sample const a, Sample const b) {
                        return a.data_placeholder_start < b.data_placeholder_start;
                    }
                );

                std::ifstream stream(this->path);
                for (auto sample: ordered_samples) {
                    this->apply_sample_data(&stream, &sample);
                }

                output.push_back(preset);
                break;
            }


            return output[0];
        }

        Instrument get_instrument(int instrument_index) {
            int ibag_entry_size = 4;
            std::vector<char> inst_bytes = this->pdta_chunks["inst"];

            int offset = instrument_index * 22;
            std::string inst_name;
            for (int j = 0; j < 20; j++) {
                char b = inst_bytes[offset +j];
                if (b == 0) {
                    break;
                }
                inst_name += b;
            }

            int first_ibag_index = inst_bytes[offset + 20] + (inst_bytes[offset + 21] * 256);
            int next_first_ibag_index = inst_bytes[22 + offset + 20] + (inst_bytes[22 + offset + 21] * 256);
            int zone_count = next_first_ibag_index - first_ibag_index;

            Instrument instrument = Instrument(inst_name);
            for (int j = 0; j < zone_count; j++) {
                char ibag_bytes[ibag_entry_size];
                for (int k = 0; k < ibag_entry_size; k++) {
                    this->pdta_chunks["ibag"][(ibag_entry_size * (first_ibag_index + j)) + k];
                }

                char next_ibag_bytes[ibag_entry_size];
                for (int k = 0; k < ibag_entry_size; k++) {
                    this->pdta_chunks["ibag"][(ibag_entry_size * (first_ibag_index + j + 1)) + k];
                }

                std::tuple<int, int> ibag = {
                    ibag_bytes[0] + (ibag_bytes[1] * 256),
                    ibag_bytes[2] + (ibag_bytes[3] * 256)
                };

                std::tuple<int, int> next_ibag = {
                    next_ibag_bytes[0] + (next_ibag_bytes[1] * 256),
                    next_ibag_bytes[2] + (next_ibag_bytes[3] * 256)
                };

                std::vector<Generator> generators = this->get_instrument_generators(
                    get<0>(ibag),
                    get<0>(next_ibag)
                );

                // TODO MODULATORS

                this->generate_instrument(&instrument, generators);
            }

            return instrument;
        }

        void apply_sample_data(std::ifstream* stream, Sample* sample) {
            switch (sample->sample_type) {
                case 0x8001:
                case 0x8002:
                case 0x8004:
                case 0x8008: {
                    if (!this->irom.has_value()) {
                        throw NoIROMDeclared();
                    }
                    // TODO
                    //this->read_rom_hook(
                    //    sample->data_placeholder_start,
                    //    sample->data_placeholder_end,
                    //);
                }
                default: {
                    sample->set_data(
                        this->get_sample_data(
                            stream,
                            sample->data_placeholder_start,
                            sample->data_placeholder_end
                        )
                    );
                }
            }
        }

        std::vector<short>* get_sample_data(std::ifstream* stream, int start, int end) {
            // TODO: SM24
            const std::tuple<int, int> key = {start, end};

            if (this->sample_data_cache.count(start) > 0 && this->sample_data_cache[start].count(end) > 0) {
                this->sample_data_cache[start][end] = {
                    std::get<0>(this->sample_data_cache[start][end]) + 1,
                    std::get<1>(this->sample_data_cache[start][end])
                };
                return &get<1>(this->sample_data_cache[start][end]);
            }

            int smpl_size = 2 * (end - start);
            char* smpl = this->get_sub_chunk_data(
                stream,
                &this->sub_chunks[1][0],
                start * 2,
                smpl_size
            );

            std::vector<short> output;
            output.reserve(smpl_size);
            for (int i = 0; i < smpl_size / 2; i++) {
                output.push_back(
                    ((short)smpl[i * 2] * 256) + (short)smpl[(i * 2) + 1]
                );
            }

            if (this->sample_data_cache.count(start) == 0) {
                this->sample_data_cache[start] = {};
            }
            if (this->sample_data_cache[start].count(end) == 0) {
                this->sample_data_cache[start][end] = {0, std::move(output)};
            }

            return &get<1>(this->sample_data_cache[start][end]);
        }



    private:
        std::unordered_map<int, std::unordered_map<int, std::tuple<int, std::vector<short>>>> sample_data_cache;
        Sample get_sample(int sample_index) {
            std::vector<char> shdr_bytes = this->pdta_chunks["shdr"];
            int offset = sample_index * 46;

            std::string sample_name;
            for (int j = 0; j < 20; j++) {
                char b = shdr_bytes[offset + j];
                if (b == 0) {
                    break;
                }
                sample_name += b;
            }

            int start = shdr_bytes[offset + 20] + (shdr_bytes[offset + 21] * 256) + (shdr_bytes[offset + 22] * 65536) + (shdr_bytes[offset + 23] * 16777216);
            int end = shdr_bytes[offset + 24] + (shdr_bytes[offset + 25] * 256) + (shdr_bytes[offset + 26] * 65536) + (shdr_bytes[offset + 27] * 16777216);

            int sample_type = shdr_bytes[offset + 44] + (shdr_bytes[offset + 45] * 256);

            int link_addr;
            if (sample_type == 0x002 || sample_type == 0x004 || sample_type == 0x008) {
                link_addr = shdr_bytes[offset + 42] + (shdr_bytes[offset + 43] * 256);
            } else {
                link_addr = 0;
            }

            return Sample {
                sample_name,
                shdr_bytes[offset + 28]
                + (shdr_bytes[offset + 29] * 256)
                + (shdr_bytes[offset + 30] * 65536)
                + (shdr_bytes[offset + 31] * 16777216)
                - start,
                shdr_bytes[offset + 32]
                + (shdr_bytes[offset + 33] * 256)
                + (shdr_bytes[offset + 34] * 65536)
                + (shdr_bytes[offset + 35] * 16777216)
                - start,
                shdr_bytes[offset + 36]
                + (shdr_bytes[offset + 37] * 256)
                + (shdr_bytes[offset + 38] * 65536)
                + (shdr_bytes[offset + 39] * 16777216)
                - start,
                shdr_bytes[offset + 40],
                shdr_bytes[offset + 41],
                sample_type,
                link_addr,
                start,
                end
            };
        }

        std::vector<Generator> get_preset_generators(int from_index, int to_index) {
            int array_size = (to_index - from_index) * 4;
            char bytes[array_size];
            for (int i = 0; i < array_size; i++) {
                bytes[i] = this->pdta_chunks["pgen"][i + (from_index * 4)];
            }

            std::vector<Generator> output;
            output.reserve((to_index - from_index));
            for (int i = 0; i < (to_index - from_index); i++) {
                int offset = i * 4;
                output.push_back(
                    Generator {
                        bytes[offset + 0] + (bytes[offset + 1] * 256),
                        bytes[offset + 2],
                        bytes[offset + 3]
                    }
                );
            }

            return output;
        }

        std::vector<Generator> get_instrument_generators(int from_index, int to_index) {
            int array_size = (to_index - from_index) * 4;
            char bytes[(to_index - from_index) * 4];
            for (int i = 0; i < array_size; i++) {
                bytes[i] = this->pdta_chunks["igen"][i + (from_index * 4)];
            }

            std::vector<Generator> output;
            output.reserve((to_index - from_index));

            for (int i = 0; i < (to_index - from_index); i++) {
                int offset = i * 4;
                output.push_back(
                        Generator {
                                bytes[offset + 0] + (bytes[offset + 1] * 256),
                                bytes[offset + 2],
                                bytes[offset + 3]
                        }
                );
            }

            return output;
        }

        void generate_instrument(Instrument* instrument, std::vector<Generator> generators) {
            bool is_global = generators.empty() || generators[generators.size() - 1].sfGenOp != 0x35;

            SampleDirective working_directive = SampleDirective();
            working_directive.apply_generators(generators);
            if (generators[generators.size() - 1].sfGenOp != 0x35) {
                throw InvalidSampleIdPosition();
            }
            working_directive.sample = this->get_sample(generators[generators.size() - 1].get_int());

            if (is_global) {
                instrument->set_global_zone(working_directive);
            } else {
                instrument->add_sample(working_directive);
            }
        }

    void generate_preset(Preset* preset, std::vector<Generator> generators) {
        bool is_global = generators.empty() || generators[generators.size() - 1].sfGenOp != 0x29;

        InstrumentDirective working_directive = InstrumentDirective();
        working_directive.apply_generators(generators);

        for (auto generator: generators) {
            if (generator.sfGenOp == 0x29) {
                working_directive.instrument = this->get_instrument(generator.get_int());
                break;
            }
        }

        if (is_global) {
            preset->set_global_zone(working_directive);
        } else {
            preset->add_instrument(working_directive);
        }
    }

};


#endif //PAGAN_SOUNDFONT_H
