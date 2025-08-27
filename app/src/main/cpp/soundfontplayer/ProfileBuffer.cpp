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
    jlong output;
    switch (controller_event_data->type) {
        case TYPE_DELAY: {
            auto* buffer = (DelayBuffer*)malloc(sizeof(DelayBuffer));
            buffer->init(controller_event_data, start_frame);
            output = (jlong)buffer;
            break;
        }
        case TYPE_PAN: {
            auto* buffer = (PanBuffer*)malloc(sizeof(PanBuffer));
            buffer->init(controller_event_data, start_frame);
            output = (jlong)buffer;
            break;
        }
        case TYPE_VOLUME: {
            auto* buffer = (VolumeBuffer*)malloc(sizeof(VolumeBuffer));
            buffer->init(controller_event_data, start_frame);
            output = (jlong)buffer;
            break;
        }
        default: {
            auto* buffer = (EffectProfileBuffer*)malloc(sizeof(EffectProfileBuffer));
            buffer->init(controller_event_data, start_frame);
            output = (jlong)buffer;
        }
    }

    return output;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct EffectProfileBuffer *) ptr_long;
    jlong output;
    switch (ptr->type) {
        case TYPE_DELAY: {
            auto* original =(DelayBuffer*)ptr;
            auto* buffer = (DelayBuffer*)malloc(sizeof(DelayBuffer));
            original->copy_to(buffer);
            output = (jlong)buffer;

            break;
        }
        case TYPE_PAN: {
            auto* original =(PanBuffer*)ptr;
            auto* buffer = (PanBuffer*)malloc(sizeof(PanBuffer));
            original->copy_to(buffer);
            output = (jlong)buffer;
            break;
        }
        case TYPE_VOLUME: {
            auto* original =(VolumeBuffer*)ptr;
            auto* buffer = (VolumeBuffer*)malloc(sizeof(VolumeBuffer));
            original->copy_to(buffer);
            output = (jlong)buffer;
            break;
        }
        default: {
            auto* buffer = (EffectProfileBuffer*)malloc(sizeof(EffectProfileBuffer));
            ptr->copy_to(buffer);
            output = (jlong)buffer;
        }
    }
    return output;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_get_1data_1ptr_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct EffectProfileBuffer *) ptr_long;
    return (jlong)ptr->data;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long, jboolean deep) {
    auto *ptr = (struct EffectProfileBuffer *) ptr_long;
    if (deep) {
        delete ptr->data;
    }
    free(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_set_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (struct EffectProfileBuffer *) ptr_long;
    ptr->set_frame(frame);
}
