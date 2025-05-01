#include <jni.h>
#include <malloc.h>

#include "ProfileBuffer.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024ProfileBuffer_00024Companion_create(
        JNIEnv* env,
        jobject,
        jintArray indices,
        jfloatArray values,
        jfloatArray increments,
        jint start_frame,
        jboolean skip_set
) {
    auto* buffer = (ProfileBuffer *)malloc(sizeof(ProfileBuffer));

    int array_length = env->GetArrayLength(indices);
    auto* vec = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * array_length);

    jint* _indices = env->GetIntArrayElements(indices, 0);
    jfloat* _values = env->GetFloatArrayElements(values, 0);
    jfloat* _increments = env->GetFloatArrayElements(increments, 0);

    for (int i = 0; i < array_length; i++) {
        auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
        ptr->frame = (int)_indices[i];
        ptr->initial_value = (float)_values[i];
        ptr->increment = (float)_increments[i];
        vec[i] = ptr;
    }

    buffer->frames = vec;
    buffer->frame_count = array_length;
    buffer->start_frame = start_frame;

    buffer->current_frame = start_frame;
    buffer->current_index = 0;
    buffer->current_value = 0;
    buffer->next_frame_trigger = -1;
    if (!skip_set) {
        buffer->set_frame(start_frame);
    }

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024ProfileBuffer_copy_1jni(
        JNIEnv* env,
        jobject,
        jlong ptr_long
) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;

    auto* buffer = (ProfileBuffer *)malloc(sizeof(ProfileBuffer));
    ptr->copy_to(buffer);

    return (jlong)buffer;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024ProfileBuffer_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;
    free(ptr);
}

