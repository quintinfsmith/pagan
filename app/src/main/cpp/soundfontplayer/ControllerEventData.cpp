#include <jni.h>
#include <malloc.h>
#include "ControllerEventData.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_00024Companion_create(
        JNIEnv* env,
        jobject,
        jintArray indices,
        jintArray end_indices,
        jint value_width,
        jfloatArray values,
        jfloatArray increments,
        jint type
) {
    auto* data_container = (ControllerEventData *)malloc(sizeof(ControllerEventData));
    int array_length = env->GetArrayLength(indices);
    auto* vec = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * array_length);

    jint* _indices = env->GetIntArrayElements(indices, 0);
    jint* _end_indices = env->GetIntArrayElements(end_indices, 0);
    jfloat* _values = env->GetFloatArrayElements(values, 0);
    jfloat* _increments = env->GetFloatArrayElements(increments, 0);

    for (int i = 0; i < array_length; i++) {
        auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
        ptr->frame = (int)_indices[i];
        ptr->end = (int)_end_indices[i];
        ptr->data_width = value_width;

        ptr->initial_value = (float*)malloc(sizeof(float) * value_width);
        ptr->increment = (float*)malloc(sizeof(float) * value_width);
        for (int j = 0; j < value_width; j++) {
            ptr->initial_value[j] = (float)_values[(i * value_width) + j];
            ptr->increment[j] = (float)_increments[(i * value_width) + j];
        }

        vec[i] = ptr;
    }

    data_container->frames = vec;
    data_container->frame_count = array_length;

    data_container->type = type;

    return (jlong)data_container;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ControllerEventData *) ptr_long;
    auto* data_container = (ControllerEventData *)malloc(sizeof(ControllerEventData));
    ptr->copy_to(data_container);
    return (jlong)data_container;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ControllerEventData *) ptr_long;
    free(ptr);
}

