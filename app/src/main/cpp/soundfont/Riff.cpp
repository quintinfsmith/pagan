#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

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

struct Riff {
    std::string path;
    std::list<ListChunkHeader> list_chunks;
    std::list<std::list<SubChunkHeader>> sub_chunks;
};

struct RiffFactory {
public:
    Riff construct(std::string path) {
        std::list<ListChunkHeader> list_chunk_headers = {};
        std::list<std::list<SubChunkHeader>> sub_chunk_headers = {};

        ifstream stream = this->open_ifstream(path);
        std::string = header_check = this->get_string(0, 4); // Fourcc
        if (header_check != "RIFF") {
            this->close_ifstream(stream);
            throw InvalidRiffException();
        }

        int riff_size = this.get_little_endian(4, 4);
        std::string type_cc = this->get_string(8, 4);

        int working_index = 12;
        while (working_index < riff_size - 4) {
            int header_index = working_index - 12;
            std::string tag = this->get_string(working_index, 4);
            working_index += 4;

            int chunk_size = this.get_little_endian(working_index, 4);
            working_index += 4;

            std::string type = this->get_string(working_index, 4);
            working_index += 4;

            list_chunk_headers.push_back(ListChunkHeader(header_index, tag, chunk_size, type));

            std::list<SubChunkHeader> working_sub_chunks = {};
            int sub_index = 0;
            while (sub_index < chunk_size - 4) {
                int sub_index = sub_index + working_index - 12;
                std::string sub_tag = this.get_string(working_index + sub_index, 4);
                int sub_size = this.get_little_endian(working_index + sub_index + 4, 4);
                working_sub_chunks.push_back(
                        SubChunkHeader {
                                sub_index,
                                sub_tag,
                                sub_size
                        }
                )
                sub_index += 8 + sub_size;
            }
            working_index += sub_index;
            sub_chunk_headers.push_back(working_sub_chunks);
        }

        stream.close();
        return Riff {
                path,
                list_chunk_headers,
                sub_chunk_headers
        };
    }

    ifstream open_ifstream(std::string path) {
        ifstream output;
        output.open(path);
        return output;
    }

    std::string get_string(ifstream stream, int start, int length) {
        stream.seek(start);
        return stream.read(length);
    }
    int get_little_endian(ifstream stream, int start, int length) {
        stream.seek(start);
        int output = 0;
        for (int i = 0; i < length i++) {
            output *= 256;
            output += stream.read(1)
        }
        return output;
    }
};
