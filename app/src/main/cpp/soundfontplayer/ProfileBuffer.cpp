#include <jni.h>
#include <malloc.h>
#include "EffectProfileBuffer.h"
#include "ControllerEventData.h"
#include "SampleHandle.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_00024Companion_create(
        JNIEnv* env,
        jobject,
        jlong cev_ptr,
        jint start_frame
) {

    auto* controller_event_data = (ControllerEventData *)cev_ptr;
    EffectProfileBuffer* buffer;
    switch (controller_event_data->type) {
        case TYPE_DELAY: {
            auto* derived_buffer = (DelayBuffer*)malloc(sizeof(DelayBuffer));
            derived_buffer->also_init();
            buffer = derived_buffer;
            break;
        }
        case TYPE_PAN: {
            auto* derived_buffer = (PanBuffer*)malloc(sizeof(PanBuffer));
            buffer = derived_buffer;
            break;
        }
        case TYPE_VOLUME: {
            auto* derived_buffer = (VolumeBuffer*)malloc(sizeof(VolumeBuffer));
            buffer = derived_buffer;
            break;
        }
        default: {
            buffer = (EffectProfileBuffer*)malloc(sizeof(EffectProfileBuffer));
        }
    }

    buffer->init(controller_event_data, start_frame);

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct EffectProfileBuffer *) ptr_long;
    EffectProfileBuffer* buffer;
    switch (ptr->type) {
        case TYPE_DELAY: {
            auto* original =(DelayBuffer*)ptr;
            auto* derived_buffer = (DelayBuffer*)malloc(sizeof(DelayBuffer));
            original->copy_to(derived_buffer);
            buffer = derived_buffer;
            break;
        }
        case TYPE_PAN: {
            auto* original =(PanBuffer*)ptr;
            auto* derived_buffer = (PanBuffer*)malloc(sizeof(PanBuffer));
            original->copy_to(derived_buffer);
            buffer = derived_buffer;
            break;
        }
        case TYPE_VOLUME: {
            auto* original =(VolumeBuffer*)ptr;
            auto* derived_buffer = (VolumeBuffer*)malloc(sizeof(VolumeBuffer));
            original->copy_to(derived_buffer);
            buffer = derived_buffer;
            break;
        }
        default: {
            buffer = (EffectProfileBuffer*)malloc(sizeof(EffectProfileBuffer));
            ptr->copy_to(buffer);
        }
    }
    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_get_1data_1ptr_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (EffectProfileBuffer *) ptr_long;
    return (jlong)ptr->data;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long, jboolean deep) {
    auto *ptr = (EffectProfileBuffer *) ptr_long;
    if (deep) {
        delete ptr->data;
    }
    free(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_set_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (EffectProfileBuffer *) ptr_long;
    ptr->set_frame(frame);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_allow_1empty_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (EffectProfileBuffer *) ptr_long;

    switch (ptr->data->type) {
        case TYPE_DELAY: {
            auto* typed_ptr = (DelayBuffer*) ptr_long;
            // DEBUG
            return true;
            //return typed_ptr->has_pending_echoes();
        }
        default: {
            return false;
        }
    }
}
