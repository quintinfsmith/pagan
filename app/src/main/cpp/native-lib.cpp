#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

#include "soundfont/SampleData.cpp"
#include "soundfontplayer/PitchedBuffer.cpp"
#include "soundfontplayer/VolumeEnvelope.cpp"
#include "soundfontplayer/ControllerEventData.cpp"
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
Java_com_qfs_apres_soundfontplayer_WaveGenerator_merge_1arrays(
        JNIEnv* env,
        jobject,
        jobjectArray input_array,
        jint frames,
        jobjectArray merge_keys,
        jlongArray effect_buffers,
        jintArray buffer_layer_indices,
        jintArray buffer_keys
    ) {
    int array_count = env->GetArrayLength(input_array);

    float* working_arrays[array_count];
    int* working_keys[array_count];

    // Put input arrays into jfloat ptrs & Put merge_keys into jint ptrs
    for (int i = 0; i < array_count; i++) {
        auto working_array = reinterpret_cast<jfloatArray>(env->GetObjectArrayElement(input_array, i));
        working_arrays[i] = env->GetFloatArrayElements(working_array, nullptr);

        auto working_keylist = reinterpret_cast<jintArray>(env->GetObjectArrayElement(merge_keys, i));
        working_keys[i] = env->GetIntArrayElements(working_keylist, nullptr);
    }

    int current_array_count = array_count;
    auto tmp_first_layer = reinterpret_cast<jintArray>(env->GetObjectArrayElement(merge_keys, 0));
    int layer_count = env->GetArrayLength(tmp_first_layer);
    for (int layer = 0; layer < layer_count; layer++) {
        int done[current_array_count];
        int done_size = 0;
        int new_arrays_size = 0;

        for (int i = 0; i < current_array_count; i++) {
            bool skip = false;
            for (int j = 0; j < done_size; j++) {
                if (done[j] == working_keys[i][layer]) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;

            if (i != new_arrays_size) {
                for (int j = 0; j < frames; j++) {
                    int jj = j * 2;
                    working_arrays[new_arrays_size][jj] = working_arrays[i][jj];
                    working_arrays[new_arrays_size][jj + 1] = working_arrays[i][jj + 1];
                }
            }

            for (int k = i + 1; k < current_array_count; k++) {
                if (working_keys[k][layer] != working_keys[i][layer]) {
                    continue;
                }
                for (int j = 0; j < frames; j++) {
                    int jj = j * 2;
                    working_arrays[new_arrays_size][jj] += working_arrays[k][jj];
                    working_arrays[new_arrays_size][jj + 1] += working_arrays[k][jj + 1];
                }
            }

            done[done_size++] = working_keys[i][layer];
            working_keys[new_arrays_size] = working_keys[i];
            new_arrays_size++;
        }

        current_array_count = new_arrays_size;
    }

    jfloat output_ptr[frames * 2];
    for (int i = 0; i < frames; i++) {
        int k = i * 2;
        output_ptr[k] = 0;
        output_ptr[k + 1] = 0;
    }

    for (int i = 0; i < current_array_count; i++) {
        jfloat* input_ptr = working_arrays[i];
        for (int j = 0; j < frames; j++) {
            int k = j * 2;
            output_ptr[k] += input_ptr[k];
            output_ptr[k + 1] += input_ptr[k + 1];
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
