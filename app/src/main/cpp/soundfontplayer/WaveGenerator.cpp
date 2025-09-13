//
// Created by pent on 8/12/25.
//

bool array_contains(int* array, int array_size, int value) {
    for (int k = 0; k < array_size; k++) {
        if (array[k] == value) return true;
    }
    return false;
}

bool apply_effect_buffer(EffectProfileBuffer* effect_buffer, float* working_array, int array_size) {
    switch (effect_buffer->data->type) {
        case TYPE_PAN: {
            auto* buffer = (PanBuffer *) effect_buffer;
            buffer->apply(working_array, array_size);
            break;
        }
        case TYPE_VOLUME: {
            auto* buffer = (VolumeBuffer *) effect_buffer;
            buffer->apply(working_array, array_size);
            break;
        }
        case TYPE_DELAY: {
            auto* buffer = (DelayBuffer *) effect_buffer;
            buffer->apply(working_array, array_size);
            break;
        }
        case TYPE_EQUALIZER: {
            auto* buffer = (EqualizerBuffer *) effect_buffer;
            buffer->apply(working_array, array_size);
            break;
        }
        default: return false;
    }

    return true;
}

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
    int key_width = 0;
    for (int i = 0; i < array_count; i++) {
        auto working_array = reinterpret_cast<jfloatArray>(env->GetObjectArrayElement(input_array, i));
        working_arrays[i] = env->GetFloatArrayElements(working_array, nullptr);

        auto working_keylist = reinterpret_cast<jintArray>(env->GetObjectArrayElement(merge_keys, i));
        working_keys[i] = env->GetIntArrayElements(working_keylist, nullptr);
        key_width = env->GetArrayLength(working_keylist);
    }

    // Set up effect buffers
    jint effect_buffer_count = env->GetArrayLength(buffer_keys_input);
    jlong* effect_buffers = env->GetLongArrayElements(effect_buffers_input, nullptr);
    jint* effect_keys = env->GetIntArrayElements(buffer_keys_input, nullptr);
    jint* effect_indices = env->GetIntArrayElements(buffer_layer_indices_input, nullptr);

    int depth = 0;
    int done_stack[array_count];
    int done_count = 0;
    int effect_buffers_applied[effect_buffer_count];
    int effect_buffers_applied_count = 0;

    __android_log_print(ANDROID_LOG_DEBUG, "", "||| %d %d", array_count, effect_buffer_count);

    while (depth <= key_width) {
        for (int i = 0; i < array_count; i++) {
            if (array_contains(done_stack, done_count, i)) continue;
            for (int j = i + 1; j < array_count; j++) {
                if (array_contains(done_stack, done_count, j)) continue;
                if (depth == key_width || working_keys[j][depth] == working_keys[i][depth]) {
                    for (int k = 0; k < frames; k++) {
                        working_arrays[i][k] += working_arrays[j][k];
                        working_arrays[i][k + frames] += working_arrays[i][k + frames];
                    }
                    done_stack[done_count++] = j;
                }
            }

            for (int j = 0; j < effect_buffer_count; j++) {
                if (depth != effect_indices[j] || (depth != key_width && effect_keys[j] != working_keys[i][depth])) continue;
                if (apply_effect_buffer((EffectProfileBuffer *) effect_buffers[j], working_arrays[i], (int) frames)) {
                    effect_buffers_applied[effect_buffers_applied_count++] = j;
                }
            }
        }
        depth++;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "", "|__ %d %d", depth, key_width);

    for (int i = 0; i < effect_buffer_count; i++) {
        if (!array_contains(effect_buffers_applied, effect_buffers_applied_count, i)) {
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
    for (int j = 0; j < frames; j++) {
        output_ptr[j * 2] += working_arrays[0][j];
        output_ptr[(j * 2) + 1] += working_arrays[0][j + frames];
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

