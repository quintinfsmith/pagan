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
            buffer = (DelayBuffer*)malloc(sizeof(DelayBuffer));
            break;
        }
        case TYPE_PAN: {
            buffer = (PanBuffer*)malloc(sizeof(PanBuffer));
            break;
        }
        case TYPE_VOLUME: {
            buffer = (VolumeBuffer*)malloc(sizeof(VolumeBuffer));
            break;
        }
        default: {
            buffer = (EffectProfileBuffer*)malloc(sizeof(EffectProfileBuffer));
        }
    }

    buffer->data = controller_event_data;
    buffer->current_frame = start_frame;
    buffer->current_index = 0;
    buffer->data_width = buffer->data->frames[0]->data_width;
    buffer->current_value = (float*)malloc(sizeof(float) * buffer->data_width);
    buffer->init();
    for (int i = 0; i < buffer->data_width; i++) {
        buffer->current_value[i] = 0;
    }

    buffer->set_frame(0);

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_ProfileBuffer_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct EffectProfileBuffer *) ptr_long;
    EffectProfileBuffer* buffer;
    switch (ptr->type) {
        case TYPE_DELAY: {
            buffer = (DelayBuffer*)malloc(sizeof(DelayBuffer));
            break;
        }
        case TYPE_PAN: {
            buffer = (PanBuffer*)malloc(sizeof(PanBuffer));
            break;
        }
        case TYPE_VOLUME: {
            buffer = (VolumeBuffer*)malloc(sizeof(VolumeBuffer));
            break;
        }
        default: {
            buffer = (EffectProfileBuffer*)malloc(sizeof(EffectProfileBuffer));
        }
    }

    ptr->copy_to(buffer);
    buffer->init();
    return (jlong)buffer;
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
