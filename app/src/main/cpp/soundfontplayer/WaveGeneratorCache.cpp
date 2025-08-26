//
// Created by pent on 8/12/25.
//

#include "WaveGeneratorCache.h"

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_WaveGenerator_merge_1arrays(
        JNIEnv* env,
        jobject,
        jobjectArray input_array,
        jint frames,
        jobjectArray merge_keys,
        jlongArray effect_buffers_input,
        jintArray buffer_layer_indices_input,
        jintArray buffer_keys_input
) {
    // NOTE: The array channels are split between first and last half instead of the normal multiplexing
    // The output of this function will be multiplexed though.
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

    // Set up effect buffers
    jint effect_buffer_count = env->GetArrayLength(buffer_keys_input);
    jlong* effect_buffers = env->GetLongArrayElements(effect_buffers_input, nullptr);
    jint* effect_keys = env->GetIntArrayElements(buffer_keys_input, nullptr);
    jint* effect_indices = env->GetIntArrayElements(buffer_layer_indices_input, nullptr);

    int current_array_count = array_count;
    auto tmp_first_layer = reinterpret_cast<jintArray>(env->GetObjectArrayElement(merge_keys, 0));
    int layer_count = env->GetArrayLength(tmp_first_layer);

    int shifted_buffers[effect_buffer_count];
    int shift_buffers_size = 0;

    // Merge the layers first, since initially, the data arrays are actually sample specific
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
                    working_arrays[new_arrays_size][j] = working_arrays[i][j];
                    working_arrays[new_arrays_size][j + frames] = working_arrays[i][j + frames];
                }
            }

            for (int k = i + 1; k < current_array_count; k++) {
                if (working_keys[k][layer] != working_keys[i][layer]) {
                    continue;
                }
                for (int j = 0; j < frames; j++) {
                    working_arrays[new_arrays_size][j] += working_arrays[k][j];
                    working_arrays[new_arrays_size][j + frames] += working_arrays[k][j + frames];
                }
            }

            done[done_size++] = working_keys[i][layer];
            working_keys[new_arrays_size] = working_keys[i];
            new_arrays_size++;
        }

        current_array_count = new_arrays_size;

        for (int i = 0; i < current_array_count; i++) {
            for (int j = 0; j < effect_buffer_count; j++) {
                if (layer != effect_indices[j] || effect_keys[j] != working_keys[i][layer]) {
                    continue;
                }
                auto* effect_buffer = (EffectProfileBuffer*)effect_buffers[j];

                if (effect_buffer->data->type == TYPE_PAN) {
                    apply_pan(effect_buffer, working_arrays[i], (int)frames);
                    shifted_buffers[shift_buffers_size++] = j;
                } else if (effect_buffer->data->type == TYPE_VOLUME) {
                    apply_volume(effect_buffer, working_arrays[i], (int)frames);
                    shifted_buffers[shift_buffers_size++] = j;
                } else if (effect_buffer->data->type == TYPE_DELAY) {
                    apply_delay(effect_buffer, working_arrays[i], (int)frames);
                    shifted_buffers[shift_buffers_size++] = j;
                }
            }
        }
    }

    // set the positions of the buffers that weren't needed
    for (int i = 0; i < effect_buffer_count; i++) {
        bool found = false;
        for (int j = 0; j < shift_buffers_size; j++) {
            if (shifted_buffers[j] == i) {
                found = true;
                break;
            }
        }
        if (!found) {
            auto* effect_buffer = (EffectProfileBuffer*)effect_buffers[i];
            effect_buffer->drain((int)frames);
        }
    }

    jfloat output_ptr[frames * 2];
    for (int i = 0; i < frames * 2; i++) {
        output_ptr[i] = 0;
    }
    // move the merged and modified signal into a single array,
    // Multiplexing the channels
    for (int i = 0; i < current_array_count; i++) {
        jfloat* input_ptr = working_arrays[i];
        for (int j = 0; j < frames; j++) {
            output_ptr[j * 2] += input_ptr[j];
            output_ptr[(j * 2) + 1] += input_ptr[j + frames];
        }
    }

    jfloatArray output = env->NewFloatArray(frames * 2);
    env->SetFloatArrayRegion(output, 0, frames * 2, output_ptr);
    return output;
}

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

