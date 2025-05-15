#include <jni.h>
#include <malloc.h>
#include "ProfileBuffer.h"
#include "ControllerEventData.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_00024Companion_create(
        JNIEnv* env,
        jobject,
        jlong cev_ptr,
        jint start_frame
) {
    auto* buffer = (ProfileBuffer *)malloc(sizeof(ProfileBuffer));
    buffer->data = (ControllerEventData *)cev_ptr;
    buffer->start_frame = start_frame;
    buffer->current_frame = start_frame;
    buffer->current_index = 0;
    buffer->current_value = 0;
    buffer->next_frame_trigger = -1;
    buffer->set_frame(0);

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_copy_1jni(
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
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long, jboolean deep) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;
    if (deep) {
        delete ptr->data;
    }
    free(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_set_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;
    ptr->set_frame(frame);
}
