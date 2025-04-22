//
// Created by pent on 4/19/25.
//

#include "SampleHandle.h"

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1next_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    std::optional<std::tuple<float, float>> frame = ptr->get_next_frame();
    float intermediate[3];
    if (frame.has_value()) {
        intermediate[0] = std::get<0>(frame.value());
        intermediate[1] = std::get<1>(frame.value());
        intermediate[2] = 1;
    } else {
        intermediate[2] = 0;
    }

    jfloatArray output = env->NewFloatArray(3);
    env->SetFloatArrayRegion(output, 0, 3, intermediate);

    return output;
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024Companion_create(
        JNIEnv* env,
        jobject,

        jshortArray data,
        jint sample_rate,
        jfloat initial_attenuation,
        jboolean is_loop,
        jint loop_start,
        jint loop_end,
        jint stereo_mode,
        jlong volume_envelope_ptr,
        jfloat pitch_shift,
        jfloat filter_cutoff,
        jfloat pan,
        jlong volume_profile_ptr,
        jlong pan_profile_ptr
) {

    auto* handle = (SampleHandle*)malloc(sizeof(SampleHandle));
    handle->sample_rate = sample_rate;
    handle->initial_attenuation = initial_attenuation;
    if (is_loop) {
        handle->loop_points = std::make_tuple(loop_start, loop_end);
    } else {
        handle->loop_points = std::nullopt;
    }
    handle->stereo_mode = stereo_mode;
    handle->volume_envelope = (struct VolumeEnvelope *)volume_envelope_ptr;
    handle->pitch_shift = pitch_shift;
    handle->filter_cutoff = filter_cutoff;
    handle->pan = pan;
    handle->volume_profile = (struct ProfileBuffer *)volume_profile_ptr;
    handle->pan_profile = (struct ProfileBuffer *)pan_profile_ptr;

    return (jlong)handle;
}
