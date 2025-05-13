#include <jni.h>
#include <malloc.h>
#include "ControllerEventData.h"
extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_00024Companion_create(
        JNIEnv* env,
        jobject,
        jintArray indices,
        jfloatArray values,
        jfloatArray increments
) {
    auto* buffer = (ControllerEventData *)malloc(sizeof(ControllerEventData));

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

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_copy_1jni(
        JNIEnv* env,
        jobject,
        jlong ptr_long
) {
    auto *ptr = (struct ControllerEventData *) ptr_long;
    auto* buffer = (ControllerEventData *)malloc(sizeof(ControllerEventData));
    ptr->copy_to(buffer);
    return (jlong)buffer;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ControllerEventData *) ptr_long;
    free(ptr);
}

