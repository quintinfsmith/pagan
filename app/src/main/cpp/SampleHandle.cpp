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

    auto* handle = (SampleHandle *)malloc(sizeof(SampleHandle));
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

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *) ptr_long;
    auto* new_handle = (SampleHandle*)malloc(sizeof(SampleHandle));
    new_handle->uuid = SampleHandleUUIDGen++;
    new_handle->data = ptr->data;
    new_handle->data_size = ptr->data_size;
    new_handle->sample_rate = ptr->sample_rate;
    new_handle->initial_attenuation = ptr->initial_attenuation;
    new_handle->loop_points = ptr->loop_points;
    new_handle->stereo_mode = ptr->stereo_mode;
    new_handle->pitch_shift = ptr->pitch_shift;
    new_handle->filter_cutoff = ptr->filter_cutoff;
    new_handle->pan = ptr->pan;

    new_handle->volume_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
    ptr->volume_profile->copy_to(new_handle->volume_profile);

    new_handle->pan_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
    ptr->pan_profile->copy_to(new_handle->pan_profile);

    new_handle->volume_envelope = (VolumeEnvelope*)malloc(sizeof(VolumeEnvelope));
    ptr->volume_envelope->copy_to(new_handle->volume_envelope);

    new_handle->secondary_setup(ptr->data_buffers);

    return (jlong)new_handle;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_0024VolumeEnvelope_00024Companion_create(
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

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_0024ProfileBuffer_00024Companion_create(
    JNIEnv* env,
    jobject,
    jintArray indices,
    jfloatArray values,
    jfloatArray increments,
    jint start_frame,
    jboolean skip_set
) {
    auto* buffer = (ProfileBuffer *)malloc(sizeof(ProfileBuffer));
    int array_length = env->GetArrayLength(indices);
    jint* _indices = env->GetIntArrayElements(indices, 0);
    jfloat* _values = env->GetFloatArrayElements(values, 0);
    jfloat* _increments = env->GetFloatArrayElements(increments, 0);
    std::vector<ProfileBufferFrame> frames;
    frames.reserve(array_length);

    for (int i = 0; i < array_length; i++) {
        frames.push_back(
            ProfileBufferFrame {
                (int)_indices[i],
                (float)_values[i],
                (float)_increments[i]
            }
        );
    }

    buffer->frames = std::move(frames);
    buffer->start_frame = start_frame;
    if (!skip_set) {
        buffer->set_frame(start_frame);
    }

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_0024ProfileBuffer_copy_1jni(
        JNIEnv* env,
        jobject,
        jlong ptr_long
) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;

    auto* buffer = (ProfileBuffer *)malloc(sizeof(ProfileBuffer));
    ptr->copy_to(buffer);

    return (jlong)buffer;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_0024ProfileBuffer_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;
    free(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_0024VolumeEnvelope_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct VolumeEnvelope *) ptr_long;
    free(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *) ptr_long;
    free(ptr->volume_envelope);
    free(ptr->pan_profile);
    free(ptr->volume_profile);
    free(ptr);
}
