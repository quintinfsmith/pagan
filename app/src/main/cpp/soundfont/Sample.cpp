#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

#include "Sample.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfont_Sample_00024Companion_create(
        JNIEnv* env,
        jobject,
        jstring name,
        jint loop_start,
        jint loop_end,
        jint sample_rate,
        jint original_pitch,
        jint pitch_correction,
        jint sample_type,
        jint placeholder_start,
        jint placeholder_end
) {
    auto* sample = (Sample*)malloc(sizeof(Sample));
    sample->name = name;
    sample->loop_start = loop_start;
    sample->loop_end = loop_end;
    sample->sample_rate = sample_rate;
    sample->original_pitch = original_pitch;
    sample->pitch_correction = pitch_correction;
    sample->data_placeholder_end = placeholder_end;
    sample->data_placeholder_start = placeholder_start;
    sample->sample_type = sample_type;

    return (jlong)sample;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfont_Sample_set_1data_1inner(JNIEnv* env, jobject, jlong ptr, jshortArray data) {
    auto* sample = (Sample*)ptr;
    sample->data = data;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_qfs_apres_soundfont_sample_get_1data_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->data;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1sample_1type_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->sample_type;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1sample_1rate_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->sample_rate;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1data_1placeholders(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    int c_array[2];
    c_array[0] = sample->data_placeholder_start;
    c_array[1] = sample->data_placeholder_end;

    jintArray jint_output = env->NewIntArray(2);
    env->SetIntArrayRegion(jint_output, 0, 2, c_array);

    return jint_output;
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1name_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->name;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1original_1pitch(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->original_pitch;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1pitch_1correction(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->pitch_correction;
}

