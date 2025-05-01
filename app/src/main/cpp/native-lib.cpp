#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include <android/log.h>

#include "soundfont/Sample.cpp"
#include "soundfontplayer/PitchedBuffer.cpp"
#include "soundfontplayer/SampleHandle.cpp"

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_WaveGenerator_tanh_1array(JNIEnv* env, jobject, jfloatArray input_array) {
    int input_size = env->GetArrayLength(input_array);
    jfloat output_ptr[input_size];
    jfloat* input_ptr = env->GetFloatArrayElements(input_array, nullptr);
    for (int i = 0; i < input_size; i++) {
        output_ptr[i] = tanh(input_ptr[i]);
    }
    jfloatArray output = env->NewFloatArray(input_size);
    env->SetFloatArrayRegion(output, 0, input_size, output_ptr);
    return output;
}
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_WaveGenerator_merge_1arrays(JNIEnv* env, jobject, jobjectArray input_array, jint frames) {
    int array_count = env->GetArrayLength(input_array);
    jfloat output_ptr[frames * 2];
    for (int i = 0; i < frames; i++) {
        output_ptr[i * 2] = 0;
        output_ptr[(i * 2) + 1] = 0;
    }

    for (int i = 0; i < array_count; i++) {
        auto working_array = reinterpret_cast<jfloatArray>(env->GetObjectArrayElement(input_array, i));
        jfloat* input_ptr = env->GetFloatArrayElements(working_array, nullptr);
        for (int j = 0; j < frames; j++) {
            output_ptr[(j * 2)] += input_ptr[(j * 2)];
            output_ptr[(j * 2) + 1] += input_ptr[(j * 2)];
        }
    }

    jfloatArray output = env->NewFloatArray(frames * 2);
    env->SetFloatArrayRegion(output, 0, frames * 2, output_ptr);
    return output;
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
