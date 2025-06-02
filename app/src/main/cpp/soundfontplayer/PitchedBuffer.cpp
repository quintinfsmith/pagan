#include "PitchedBuffer.h"
#include <jni.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_copy_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct PitchedBuffer *)ptr_long;

    PitchedBuffer* buffer = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));

    buffer->data = ptr->data;
    buffer->default_pitch = ptr->default_pitch;
    buffer->start = ptr->start;
    buffer->end = ptr->end;
    buffer->is_loop = ptr->is_loop;
    buffer->virtual_position = ptr->virtual_position;
    buffer->pitched_increment = ptr->pitched_increment;
    buffer->real_position_preradix = ptr->real_position_preradix;
    buffer->real_position_postradix = ptr->real_position_postradix;
    buffer->virtual_size = ptr->virtual_size;

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_00024Companion_create(
        JNIEnv* env,
        jobject,
        jlong data_ptr_long,
        jfloat pitch,
        int start,
        int end,
        jboolean is_loop
) {

    auto* buffer = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
    buffer->data = (SampleData*)data_ptr_long;
    buffer->default_pitch = pitch;
    buffer->start = start;
    buffer->end = end;
    buffer->is_loop = is_loop;
    buffer->virtual_position = 0;
    buffer->real_position_preradix = 0;
    buffer->real_position_postradix = 0;

    buffer->repitch(1);
    return (jlong)buffer;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1range_1inner(JNIEnv* env, jobject, jlong ptr_long, jintArray output) {
    auto *ptr = (PitchedBuffer *)ptr_long;

    int data[2] = {
            ptr->start,
            ptr->end
    };

    env->SetIntArrayRegion(output, 0, 2, (jint *)data);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1virtual_1size(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->virtual_size;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1virtual_1position(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->virtual_position;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_set_1virtual_1position(JNIEnv* env, jobject, jlong ptr_long, jint new_position) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    ptr->set_position(new_position);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_is_1overflowing_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->is_overflowing();
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_repitch_1inner(JNIEnv* env, jobject, jlong ptr_long, jfloat new_pitch_adj) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    ptr->repitch(new_pitch_adj);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_get_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    float output = 0;
    try {
        output = ptr->get();
    } catch (PitchedBufferOverflow& e) {
        jclass jc = env->FindClass("com/qfs/apres/soundfontplayer/PitchedBuffer$PitchedBufferOverflow");
        if (jc) {
            env->ThrowNew(jc, e.what());
        }
    }
    return output;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_is_1loop(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    return ptr->is_loop;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_free(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (PitchedBuffer *)ptr_long;
    free(ptr);
}

