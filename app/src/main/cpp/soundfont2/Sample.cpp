#include "Sample.h"
#include <jni.h>
#include <string>
#include <iostream>
#include <string>
#include <exception>


extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfont2_Sample_00024Companion_create(
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
    sample->name = env->GetStringUTFChars(name, nullptr);

    sample->loop_start = loop_start;
    sample->loop_end = loop_end;
    sample->sample_rate = sample_rate;
    sample->original_pitch = original_pitch;
    sample->pitch_correction = pitch_correction;
    sample->sample_type = sample_type;
    sample->data_placeholder_start = placeholder_start;
    sample->data_placeholder_end = placeholder_end;

    return (jlong)sample;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfont2_Sample_set_1data_1inner(JNIEnv* env, jobject, jlong ptr, jshortArray data) {
    auto* sample = (Sample*)ptr;
    jshort* raw_shorts = env->GetShortArrayElements(data, 0);
    int length = env->GetArrayLength(data);
    sample->data = (short*)malloc(sizeof(short) * length);
    for (int i = 0; i < length; i++) {
        sample->data[i] = raw_shorts[i];
    }
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1data_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    jshortArray jshort_output = env->NewShortArray(sample->data_placeholder_end - sample->data_placeholder_start);
    env->SetShortArrayRegion(jshort_output, 0, sample->data_placeholder_end - sample->data_placeholder_start, (jshort *)sample->data);
    return jshort_output;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1sample_1type_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->sample_type;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1sample_1rate_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->sample_rate;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1placeholder_1start(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->data_placeholder_start;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1placeholder_1end(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->data_placeholder_end;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_qfs_apres_soundfont2_Sample_jni_1data_1placeholders(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    int c_array[2];
    c_array[0] = sample->data_placeholder_start;
    c_array[1] = sample->data_placeholder_end;

    jintArray jint_output = env->NewIntArray(2);
    env->SetIntArrayRegion(jint_output, 0, 2, (jint *)c_array);

    return jint_output;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1name_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return env->NewStringUTF(sample->name);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1original_1pitch(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->original_pitch;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1pitch_1correction(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->pitch_correction;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1loop_1start(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->loop_start;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont2_Sample_get_1loop_1end(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->loop_end;
}