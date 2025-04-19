//
// Created by pent on 4/18/25.
//

#ifndef PAGAN_SOUNDFONT_H
#define PAGAN_SOUNDFONT_H

#include "Riff.cpp"
#include "Sample.h"
#include <string>
#include <unordered_map>
#include <utility>

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

        Sample get_sample(int sample_index, bool build_linked) {
            std::vector<char> shdr_bytes = this->pdta_chunks["shdr"];
            int offset = sample_index * 46;

            std::string sample_name;
            for (int j = 0; j < 20; j++) {
                int b = shdr_bytes[offset + j];
                if (b == 0) {
                    break;
                }
                sample_name += b;
            }

            int start = shdr_bytes[offset + 20] + (shdr_bytes[offset + 21] * 256) + (shdr_bytes[offset + 22] * 65536) + (shdr_bytes[offset + 23] * 16777216);
            int end = shdr_bytes[offset + 24] + (shdr_bytes[offset + 25] * 256) + (shdr_bytes[offset + 26] * 65536) + (shdr_bytes[offset + 27] * 16777216);

            int sample_type = shdr_bytes[offset + 44] + (shdr_bytes[offset + 45] * 256);

            std::optional<Sample> linked_sample = std::nullopt;
            if (build_linked && (sample_type == 0x002 || sample_type == 0x004 || sample_type == 0x008)) {
                int linked_addr = shdr_bytes[offset + 42] + (shdr_bytes[offset + 43] * 256);
                if (linked_addr != 0) {
                    linked_sample = this->get_sample(linked_addr, false);
                }
            }

            return Sample{
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
                linked_sample,
                sample_type,
                start,
                end
            }
        }
};


#endif //PAGAN_SOUNDFONT_H
