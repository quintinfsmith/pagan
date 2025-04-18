#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include <fstream>
#include <utility>
#include <vector>
#include <string>


class InvalidRiffException: public std::exception {};
struct ListChunkHeader {
    int index;
    std::string tag;
    int size;
    std::string type;
};

struct SubChunkHeader {
    int index;
    std::string tag;
    int size;
};

class Riff {
    std::string path;
    std::vector<ListChunkHeader> list_chunks;
    std::vector<std::vector<SubChunkHeader>> sub_chunks;

    public:
        explicit Riff(std::string _path) {
            this->path = std::move(_path);
            this->list_chunks = {};
            this->sub_chunks = {};

            std::ifstream stream(path);

            std::string header_check = Riff::get_string(&stream, 0, 4); // Fourcc
            if (header_check != "RIFF") {
                stream.close();
                throw InvalidRiffException();
            }

            int riff_size = Riff::get_little_endian(&stream, 4, 4);
            std::string type_cc = Riff::get_string(&stream, 8, 4);

            int working_index = 12;
            while (working_index < riff_size - 4) {
                int header_index = working_index - 12;
                std::string tag = Riff::get_string(&stream, working_index, 4);
                working_index += 4;

                int chunk_size = Riff::get_little_endian(&stream, working_index, 4);
                working_index += 4;

                std::string type = Riff::get_string(&stream, working_index, 4);
                working_index += 4;

                list_chunks.push_back(ListChunkHeader {header_index, tag, chunk_size, type });

                std::vector<SubChunkHeader> working_sub_chunks = {};
                int sub_index = 0;
                while (sub_index < chunk_size - 4) {
                    SubChunkHeader chunkheader = SubChunkHeader {
                        sub_index + working_index - 12,
                        this->get_string(&stream, working_index + sub_index, 4),
                        this->get_little_endian(&stream, working_index + sub_index + 4, 4)
                    };
                    working_sub_chunks.push_back(chunkheader);
                    sub_index += 8 + chunkheader.size;
                }

                working_index += sub_index;
                sub_chunks.push_back(working_sub_chunks);
            }

            stream.close();
        }

        static std::string get_string(std::ifstream* stream, int start, int length) {
            stream->seekg(start); char output[length];
            stream->read(output, length);
            return output;
        }

        static int get_little_endian(std::ifstream* stream, int start, int length) {
            stream->seekg(start);
            int output = 0;
            char v[1];
            for (int i = 0; i < length; i++) {
                output *= 256;
                stream->read(v, 1);
                output += int(v[0]);
            }
            return output;
        }

        static char* get_bytes(std::ifstream* stream, int start, int length) {
            char output[length];
            stream->seekg(start);
            stream->read(output, length);
            return output;
        }

        static char* get_chunk_data(std::ifstream* stream, ListChunkHeader* header) {
            return Riff::get_bytes(stream, header->index + 24, header->size);
        }

        static char* get_sub_chunk_data(std::ifstream* stream, SubChunkHeader* header, std::optional<int> inner_offset, std::optional<int> cropped_size) {
            int offset = header->index + 8;
            int size = header->size;
            if (inner_offset.has_value()) {
                size -= inner_offset.value();
                offset += inner_offset.value();
            }

            if (cropped_size.has_value() and cropped_size.value() <= size) {
                size = cropped_size.value();
            }

            return Riff::get_bytes(stream, offset, size);
        }
};
