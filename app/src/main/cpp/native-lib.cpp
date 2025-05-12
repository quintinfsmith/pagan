#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

#include "soundfont/SampleData.cpp"
#include "soundfontplayer/PitchedBuffer.cpp"
#include "soundfontplayer/VolumeEnvelope.cpp"
#include "soundfontplayer/ProfileBuffer.cpp"
#include "soundfontplayer/SampleHandle.cpp"

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_WaveGenerator_tanh_1array(JNIEnv* env, jobject, jfloatArray input_array) {
    int input_size = env->GetArrayLength(input_array);
    jfloat output_ptr[input_size];
    jfloat* input_ptr = env->GetFloatArrayElements(input_array, nullptr);
    for (int i = 0; i < input_size; i++) {
        output_ptr[i] = tanh(input_ptr[i]);
    }

    env->ReleaseFloatArrayElements(input_array, input_ptr, 0);

    jfloatArray output = env->NewFloatArray(input_size);
    env->SetFloatArrayRegion(output, 0, input_size, output_ptr);
    return output;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_WaveGenerator_merge_1arrays(JNIEnv* env, jobject, jobjectArray input_array, jint frames, jobjectArray merge_keys) {
    int array_count = env->GetArrayLength(input_array);
    jfloat output_ptr[frames * 2];
    for (int i = 0; i < frames; i++) {
        int k = i * 2;
        output_ptr[k] = 0;
        output_ptr[k + 1] = 0;
    }

    // TODO: HANDLE LAYERS

    for (int i = 0; i < array_count; i++) {
        auto working_array = reinterpret_cast<jfloatArray>(env->GetObjectArrayElement(input_array, i));
        jfloat* input_ptr = env->GetFloatArrayElements(working_array, nullptr);
        for (int j = 0; j < frames; j++) {
            int k = j * 2;
            int x = j * 3;
            output_ptr[k] += input_ptr[x] * input_ptr[x + 1];
            output_ptr[k + 1] += input_ptr[x] * input_ptr[x + 2];
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
