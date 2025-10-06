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
    new (data_container) ControllerEventData(
        type,
        env->GetArrayLength(indices),
        value_width,
        env->GetIntArrayElements(indices, nullptr),
        env->GetIntArrayElements(end_indices, nullptr),
        env->GetFloatArrayElements(values, nullptr),
        env->GetFloatArrayElements(increments, nullptr)
    );

    return (jlong)data_container;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ControllerEventData *) ptr_long;
    auto* data_container = (ControllerEventData *)malloc(sizeof(ControllerEventData));
    new (data_container) ControllerEventData(ptr);
    return (jlong)data_container;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ControllerEventData_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ControllerEventData *) ptr_long;
    ptr->~ControllerEventData();
    free(ptr);
}

