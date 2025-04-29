//
// Created by pent on 4/19/25.
//

#include "SampleHandle.h"
#include <android/log.h>
#include <string>

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1next_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    std::optional<float> frame = ptr->get_next_frame();
    float intermediate[2];
    if (frame.has_value()) {
        intermediate[0] = frame.value();
        intermediate[1] = 1;
    } else {
        intermediate[0] = 0;
        intermediate[1] = 0;
    }

    jfloatArray output = env->NewFloatArray(2);
    env->SetFloatArrayRegion(output, 0, 2, intermediate);
    return output;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1next_1frames_1jni(JNIEnv* env, jobject, jlong ptr_long, jint size, jint left_padding) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    jfloat buffer[size];
    ptr->get_next_frames(buffer, size, left_padding);

    jfloatArray output = env->NewFloatArray(size);
    env->SetFloatArrayRegion(output, 0, size, buffer);

    return output;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1uuid_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    return ptr->uuid;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1volume_1envelope_1ptr(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    return (jlong)ptr->volume_envelope;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1volume_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    return (jlong)ptr->volume_profile;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1volume_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long, jlong new_ptr) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    auto *ptr_profile = (struct ProfileBuffer *)new_ptr;
    ptr->volume_profile = ptr_profile;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1pan_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    return (jlong)ptr->pan_profile;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1pan_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long, jlong new_ptr) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    auto *ptr_profile = (struct ProfileBuffer *)new_ptr;
    ptr->pan_profile = ptr_profile;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1working_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    ptr->set_working_frame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1release_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    ptr->set_release_frame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_release_1note_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    ptr->release_note();
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1kill_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    ptr->set_kill_frame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_repitch_1jni(JNIEnv* env, jobject, jlong ptr_long, jfloat new_pitch) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    ptr->repitch(new_pitch);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1release_1duration_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    return ptr->get_release_duration();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1release_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    if (ptr->release_frame.has_value()) {
        return ptr->release_frame.value();
    } else {
        return -1;
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1next_1balance_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *)ptr_long;
    std::tuple<float, float> frame = ptr->get_next_balance();
    float intermediate[2];
    intermediate[0] = std::get<0>(frame);
    intermediate[1] = std::get<1>(frame);

    jfloatArray output = env->NewFloatArray(2);
    env->SetFloatArrayRegion(output, 0, 2, intermediate);
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

    handle->data_size = env->GetArrayLength(data);
    jshort* data_ptr_tmp = env->GetShortArrayElements(data, nullptr);
    handle->data = (jshort *)malloc(sizeof(jshort) * handle->data_size);
    for (int i = 0; i < handle->data_size; i++) {
        handle->data[i] = data_ptr_tmp[i];
    }

    handle->secondary_setup(nullptr, 0);

    return (jlong)handle;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *) ptr_long;
    auto* new_handle = (SampleHandle*)malloc(sizeof(SampleHandle));
    new_handle->uuid = SampleHandleUUIDGen++;
    new_handle->data = (jshort *)malloc(sizeof(jshort) * ptr->data_size);
    for (int i = 0; i < ptr->data_size; i++) {
        new_handle->data[i] = ptr->data[i];
    }
    new_handle->data_size = ptr->data_size;
    new_handle->sample_rate = ptr->sample_rate;
    new_handle->initial_attenuation = ptr->initial_attenuation;
    new_handle->loop_points = ptr->loop_points;

    new_handle->stereo_mode = ptr->stereo_mode;
    new_handle->pitch_shift = ptr->pitch_shift;
    new_handle->filter_cutoff = ptr->filter_cutoff;
    new_handle->pan = ptr->pan;

    new_handle->volume_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
    if (ptr->volume_profile != nullptr) {
        ptr->volume_profile->copy_to(new_handle->volume_profile);
    }

    new_handle->pan_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
    if (ptr->pan_profile != nullptr) {
        ptr->pan_profile->copy_to(new_handle->pan_profile);
    }

    new_handle->volume_envelope = (VolumeEnvelope*)malloc(sizeof(VolumeEnvelope));
    ptr->volume_envelope->copy_to(new_handle->volume_envelope);

    new_handle->secondary_setup(ptr->data_buffers, ptr->buffer_count);

    if (ptr->release_frame.has_value()) {
        new_handle->release_frame = ptr->release_frame.value();
    } else {
        new_handle->release_frame = std::nullopt;
    }

    if (ptr->kill_frame.has_value()) {
        new_handle->kill_frame = ptr->kill_frame.value();
    } else {
        new_handle->kill_frame = std::nullopt;
    }
    new_handle->is_dead = ptr->is_dead;

    return (jlong)new_handle;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_is_1dead_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *) ptr_long;
    return ptr->is_dead;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1working_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *) ptr_long;
    return ptr->working_frame;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1smoothing_1factor_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct SampleHandle *) ptr_long;
    return ptr->smoothing_factor;
}


extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_00024Companion_create(
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

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_get_1frames_1release(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct VolumeEnvelope *) ptr_long;
    return ptr->frames_release;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_set_1frames_1release(JNIEnv* env, jobject, jlong ptr_long, jint frames_release) {
    auto *ptr = (struct VolumeEnvelope *) ptr_long;
    ptr->frames_release = frames_release;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_get_1release(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct VolumeEnvelope *) ptr_long;
    return ptr->release;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_set_1release(JNIEnv* env, jobject, jlong ptr_long, jfloat new_value) {
    auto *ptr = (struct VolumeEnvelope *) ptr_long;
    ptr->release = new_value;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024ProfileBuffer_00024Companion_create(
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
    auto* vec = (ProfileBufferFrame**)malloc(sizeof (ProfileBufferFrame*) * array_length);

    jint* _indices = env->GetIntArrayElements(indices, 0);
    jfloat* _values = env->GetFloatArrayElements(values, 0);
    jfloat* _increments = env->GetFloatArrayElements(increments, 0);

    for (int i = 0; i < array_length; i++) {
        auto* ptr = (ProfileBufferFrame*)malloc(sizeof(ProfileBufferFrame));
        ptr->frame = (int)_indices[i];
        ptr->initial_value = (float)_values[i];
        ptr->increment = (float)_increments[i];
        vec[i] = ptr;
    }

    buffer->frames = vec;
    buffer->frame_count = array_length;
    buffer->start_frame = start_frame;

    buffer->current_frame = start_frame;
    buffer->current_index = 0;
    buffer->current_value = 0;
    buffer->next_frame_trigger = -1;
    if (!skip_set) {
        buffer->set_frame(start_frame);
    }

    return (jlong)buffer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024ProfileBuffer_copy_1jni(
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
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024ProfileBuffer_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (struct ProfileBuffer *) ptr_long;
    free(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024VolumeEnvelope_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
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

