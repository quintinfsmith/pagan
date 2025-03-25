#include <jni.h>
#include <string>
#include <exception>

struct Test {
    int a = 0;
    public:
        std::string boop() {
            return "bum";
        }
};
extern "C" JNIEXPORT jstring JNICALL
Java_com_qfs_pagan_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
    // int size = 44100 * 60 * 2;
    int size = 44100;
    int size_b = 10;
    float test[size][size_b];
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size_b; j++) {
            test[i][j] = 4.0;
        }
    }

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_pagan_Test_create(JNIEnv* env, jobject /* this */) {
    struct Test *p = (struct Test *)malloc(sizeof(struct Test));
    std::string s = p->boop();
    return (jlong)p;
}

class PitchedBufferOverflow : public std::exception {};

struct PitchedBuffer {
    jshort* data;
    int data_size;
    float pitch;
    bool max_known;
    float max;
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
            this->virtual_size = static_cast<int>(static_cast<float>(this->end + 1 - this->start) / (this->pitch * this->pitch_adjustment));
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
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_create(
        JNIEnv* env,
        jobject,
        jshortArray data,
        jint data_size,
        jfloat pitch,
        jboolean max_known,
        jfloat max,
        int start,
        int end,
        jboolean is_loop
    ) {

    // NOTE: May need to round
    int virtual_size = static_cast<int>(static_cast<float>(end + 1 - start) / pitch);


    struct PitchedBuffer buffer = PitchedBuffer {
        env->GetShortArrayElements(data, NULL),
        data_size,
        pitch,
        static_cast<bool>(max_known),
        max,
        start,
        end,
        static_cast<bool>(is_loop),
        0,
        1.0,
        virtual_size,
        pitch
    };

    return (jlong)&buffer;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_max(JNIEnv* env, jobject, jlong ptr_long) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    return ptr->max;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_virtual_size(JNIEnv* env, jobject, jlong ptr_long) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    return ptr->virtual_size;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_virtual_position(JNIEnv* env, jobject, jlong ptr_long) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    return ptr->virtual_position;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_set_virtual_position(JNIEnv* env, jobject, jlong ptr_long, jint new_position) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    ptr->virtual_position = new_position;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_is_overflowing_inner(JNIEnv* env, jobject, jlong ptr_long) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    return (ptr->virtual_position * ptr->adjusted_pitch) - ptr->start > ptr->end;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_repitch_inner(JNIEnv* env, jobject, jlong ptr_long, jfloat new_pitch_adj) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    ptr->repitch(new_pitch_adj);
}

extern "C" JNIEXPORT float JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get(JNIEnv* env, jobject, jlong ptr_long) {
    struct PitchedBuffer *ptr = (struct PitchedBuffer *)ptr_long;
    return ptr->get();
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

