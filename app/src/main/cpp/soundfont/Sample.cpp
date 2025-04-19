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
        jboolean is_linked,
        jlong linked_sample_ptr,
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
    sample->is_linked = is_linked;
    sample->linked_sample_ref = (Sample *)linked_sample_ptr;
    sample->data_placeholder_end = placeholder_end;
    sample->data_placeholder_start = placeholder_start;

    return (jlong)sample;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfont_Sample_set_1data_1inner(JNIEnv* env, jobject, jlong ptr, jshortArray data) {
    auto* sample = (Sample*)ptr;
    sample->data = env->GetShortArrayElements(data, nullptr);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1sample_1type_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->sample_type;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1linked_1sample_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return (jlong)sample->linked_sample_ref;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfont_Sample_is_1linked_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->is_linked;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1data_1placeholders(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    int output[2];
    output[0] = sample->data_placeholder_start;
    output[1] = sample->data_placeholder_end;

    return reinterpret_cast<jintArray>(output);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfs_apres_soundfont_Sample_get_1name_1inner(JNIEnv* env, jobject, jlong ptr) {
    auto* sample = (Sample*)ptr;
    return sample->name;
}

Sample::Sample(const std::optional<Sample> &linkedSampleRef) : linked_sample_ref(linkedSampleRef) {}
