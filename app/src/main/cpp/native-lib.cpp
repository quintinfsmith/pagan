#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

class PitchedBufferOverflow : public std::exception {};

struct PitchedBuffer {
    jshort* data;
    int data_size;
    float pitch;
    int max;
    int start;
    int end;
    bool is_loop;
    int virtual_position;
    float pitch_adjustment;
    int virtual_size;
    float adjusted_pitch;
    public:
        void repitch(float new_pitch_adjustment) {
            this->pitch_adjustment = new_pitch_adjustment;
            this->adjusted_pitch = this->pitch * this->pitch_adjustment;
            this->virtual_size = static_cast<int>(static_cast<float>(this->end + 1 - this->start) / this->adjusted_pitch);
        }

        float get() {
            float working_pitch = this->pitch * this->adjusted_pitch;
            int unpitched_position = this->virtual_position++ * working_pitch;
            uint16_t output = this->get_real_frame(unpitched_position);
            return static_cast<float>(output) / static_cast<float>(65535); // SHORT MAX
        }

        uint16_t get_real_frame(int unpitched_position) {
            int range_size = this->end + 1 - this->start;
            int adj_i;
            if (this->is_loop) {
                adj_i = unpitched_position % range_size;
            } else if (unpitched_position >= range_size) {
                throw PitchedBufferOverflow();
            } else {
                adj_i = unpitched_position;
            };

            return this->data[adj_i];
        }

        bool is_overflowing() {
            return (this->virtual_position * this->adjusted_pitch) - this->start > this->end;
        }
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_copy_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct PitchedBuffer *)ptr_long;

    PitchedBuffer* buffer = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
    buffer->data = ptr->data;
    buffer->data_size = ptr->data_size;
    buffer->pitch = ptr->pitch;
    buffer->max = ptr->max;
    buffer->start = ptr->start;
    buffer->end = ptr->end;
    buffer->is_loop = ptr->is_loop;
    buffer->virtual_position = ptr->virtual_position;
    buffer->pitch_adjustment = ptr->pitch_adjustment;
    buffer->virtual_size = ptr->virtual_size;
    buffer->adjusted_pitch = ptr->adjusted_pitch;

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_00024Companion_create(
        JNIEnv* env,
        jobject,
        jshortArray data,
        jint data_size,
        jfloat pitch,
        jint max,
        int start,
        int end,
        jboolean is_loop
    ) {
    PitchedBuffer* buffer = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
    buffer->data = env->GetShortArrayElements(data, nullptr);
    buffer->data_size = data_size;
    buffer->pitch = pitch;
    buffer->max = max;
    buffer->start = start;
    buffer->end = end;
    buffer->is_loop = is_loop;
    buffer->virtual_position = 0;
    buffer->pitch_adjustment = 1;
    // NOTE: May need to round
    buffer->virtual_size = static_cast<int>(static_cast<float>(end + 1 - start) / pitch);
    buffer->adjusted_pitch = pitch;

    return (jlong)buffer;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1range_1inner(JNIEnv* env, jobject, jlong ptr_long, jintArray output) {
    auto *ptr = (PitchedBuffer *)ptr_long;

    int data[2] = {
        ptr->start,
        ptr->end
    };

    env->SetIntArrayRegion(output, 0, 2, data);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1max(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->max;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1virtual_1size(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->virtual_size;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1virtual_1position(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->virtual_position;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_set_1virtual_1position(JNIEnv* env, jobject, jlong ptr_long, jint new_position) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    ptr->virtual_position = new_position;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_is_1overflowing_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->is_overflowing();
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_repitch_1inner(JNIEnv* env, jobject, jlong ptr_long, jfloat new_pitch_adj) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    ptr->repitch(new_pitch_adj);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->get();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_is_1loop(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->is_loop;
}


// extern "C" JNIEXPORT jfloatArray JNICALL
// Java_com_qfs_pagan_MainActivity_test_array(JNIEnv* env, jobject,  float t) {
//     float test[10];
//     for (int i = 0; i < 10; i++) {
//         test[i] = t;
//     }
//     return test;
// }

//extern "C" JNIEXPORT jfloatArray JNICALL
//Java_com_qfs_apres_soundfontplayer_SampleHandle_get_next_frame_inner(
//        JNIEnv* env,
//        jobject,
//        ) {
//}


class InvalidRiffException: public std::exception {};
struct ListChunkHeader {
    int index;
    std::string tag;
    int size;
    std::string type
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
