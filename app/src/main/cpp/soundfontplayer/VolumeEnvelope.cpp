//
// Created by pent on 5/1/25.
//
#include <jni.h>
#include <malloc.h>
#include "VolumeEnvelope.h"

extern "C" JNIEXPORT jlong JNICALL Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_00024Companion_create(
        JNIEnv* env,
        jobject,
        jint sample_rate,
        jfloat delay,
        jfloat attack,
        jfloat hold,
        jfloat decay,
        jfloat release,
        jfloat sustain_attenuation
) {
    auto* envelope = (VolumeEnvelope*)malloc(sizeof(VolumeEnvelope));
    envelope->delay = delay;
    envelope->attack = attack;
    envelope->hold = hold;
    envelope->decay = decay;
    envelope->release = release;
    envelope->sustain_attenuation = sustain_attenuation;
    envelope->set_sample_rate(sample_rate);

    return (jlong)envelope;
}

extern "C" JNIEXPORT jint JNICALL Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_get_1frames_1release(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (VolumeEnvelope *) ptr_long;
    return ptr->frames_release;
}

extern "C" JNIEXPORT void JNICALL Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_set_1frames_1release(JNIEnv* env, jobject, jlong ptr_long, jint frames_release) {
    auto *ptr = (VolumeEnvelope *) ptr_long;
    ptr->frames_release = frames_release;
}

extern "C" JNIEXPORT jfloat JNICALL Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_get_1release(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (VolumeEnvelope *) ptr_long;
    return ptr->release;
}

extern "C" JNIEXPORT void JNICALL Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_set_1release(JNIEnv* env, jobject, jlong ptr_long, jfloat new_value) {
    auto *ptr = (VolumeEnvelope *) ptr_long;
    ptr->release = new_value;
}

extern "C" JNIEXPORT void JNICALL Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (VolumeEnvelope *) ptr_long;
    delete ptr;
}

