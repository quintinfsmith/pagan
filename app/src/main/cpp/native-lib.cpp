#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include "soundfont/Sample.cpp"
#include "soundfont/PitchedBuffer.cpp"
#include "SampleHandle.cpp"
#include <android/log.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_copy_1inner(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct PitchedBuffer *)ptr_long;

    PitchedBuffer* buffer = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));

    buffer->data = ptr->data;
    buffer->data_size = ptr->data_size;
    buffer->pitch = ptr->pitch;
    buffer->start = ptr->start;
    buffer->end = ptr->end;
    buffer->is_loop = ptr->is_loop;
    buffer->virtual_position = ptr->virtual_position;
    buffer->pitch_adjustment = ptr->pitch_adjustment;
    buffer->virtual_size = ptr->virtual_size;
    buffer->adjusted_pitch = ptr->adjusted_pitch;

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_PitchedBuffer_00024Companion_create(
        JNIEnv* env,
        jobject,
        jshortArray data,
        jint data_size,
        jfloat pitch,
        int start,
        int end,
        jboolean is_loop
    ) {

    auto* buffer = (PitchedBuffer*)malloc(sizeof(PitchedBuffer));
    int d_size = env->GetArrayLength(data);
    buffer->data = (jshort*)malloc(sizeof(jshort) * d_size);

    env->GetShortArrayRegion(data, 0, d_size, buffer->data);
    buffer->data_size = d_size;
    buffer->pitch = pitch;
    buffer->start = start;
    buffer->end = end;
    buffer->is_loop = is_loop;
    buffer->virtual_position = 0;
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
    ptr->virtual_position = new_position;
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


// extern "C" JNIEXPORT jfloatArray JNICALL
// Java_com_qfs_pagan_MainActivity_test_array(JNIEnv* env, jobject,  float t) {
//     float test[10];
//     for (int i = 0; i < 10; i++) {
//         test[i] = t;
//     }
//     return test;
// }

//extern "C" JNIEXPORT jfloatArray JNICALL
//Java_com_qfs_apres_soundfontplayer_SampleHandle_get_next_frame_inner(
//        JNIEnv* env,
//        jobject,
//        ) {
//}
