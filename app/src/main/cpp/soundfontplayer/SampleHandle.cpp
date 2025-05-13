#include "SampleHandle.h"
#include <string>

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1next_1frames_1jni(JNIEnv* env, jobject, jlong ptr_long, jint size, jint left_padding) {
    auto *ptr = (SampleHandle *)ptr_long;
    jfloat buffer[size * 2];
    ptr->get_next_frames(buffer, size, left_padding);

    jfloatArray output = env->NewFloatArray(size * 2);
    env->SetFloatArrayRegion(output, 0, size * 2, buffer);
    return output;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1uuid_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    return ptr->uuid;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1volume_1envelope_1ptr(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    return (jlong)ptr->volume_envelope;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1volume_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    return (jlong)ptr->volume_profile;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1volume_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long, jlong new_ptr) {
    auto *ptr = (SampleHandle *)ptr_long;
    auto *ptr_profile = (ProfileBuffer *)new_ptr;
    delete ptr->volume_profile;
    ptr->volume_profile = ptr_profile;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1pan_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    return (jlong)ptr->pan_profile;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1pan_1profile_1ptr(JNIEnv* env, jobject, jlong ptr_long, jlong new_ptr) {
    auto *ptr = (SampleHandle *)ptr_long;
    auto *ptr_profile = (struct ProfileBuffer *)new_ptr;
    delete ptr->pan_profile;
    ptr->pan_profile = ptr_profile;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1working_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (SampleHandle *)ptr_long;
    ptr->set_working_frame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1release_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (SampleHandle *)ptr_long;
    ptr->set_release_frame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_release_1note_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    ptr->release_note();
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_set_1kill_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long, jint frame) {
    auto *ptr = (SampleHandle *)ptr_long;
    ptr->set_kill_frame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_repitch_1jni(JNIEnv* env, jobject, jlong ptr_long, jfloat new_pitch) {
    auto *ptr = (SampleHandle *)ptr_long;
    ptr->repitch(new_pitch);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1release_1duration_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    return ptr->get_release_duration();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1release_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    return ptr->release_frame;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1next_1balance_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *)ptr_long;
    jfloatArray output = env->NewFloatArray(2);
    std::tuple<float, float> tmp = ptr->get_next_balance();
    float balance[2];
    balance[0] = std::get<0>(tmp);
    balance[1] = std::get<1>(tmp);

    env->SetFloatArrayRegion(output, 0, 2, balance);
    return output;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_00024Companion_create(
        JNIEnv* env,
        jobject,
        jlong data_ptr_long,
        jint sample_rate,
        jfloat initial_attenuation,
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

    handle->data = (SampleData*)data_ptr_long;
    handle->sample_rate = sample_rate;
    handle->initial_attenuation = initial_attenuation;
    handle->loop_start = loop_start;
    handle->loop_end = loop_end;

    handle->stereo_mode = stereo_mode;

    auto* original_volume_envelope = (VolumeEnvelope*)volume_envelope_ptr;
    handle->volume_envelope = (VolumeEnvelope*)malloc(sizeof(VolumeEnvelope));
    original_volume_envelope->copy_to(handle->volume_envelope);

    if (volume_profile_ptr == 0) {
        handle->volume_profile = nullptr;
    } else {
        auto *original_volume_profile = (ProfileBuffer *) volume_profile_ptr;
        handle->volume_profile = (ProfileBuffer *) malloc(sizeof(ProfileBuffer));
        original_volume_profile->copy_to(handle->volume_profile);
    }

    if (pan_profile_ptr == 0) {
        handle->pan_profile = nullptr;
    } else {
        auto* original_pan_profile = (ProfileBuffer*)pan_profile_ptr;
        handle->pan_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
        original_pan_profile->copy_to(handle->pan_profile);
    }

    handle->pitch_shift = pitch_shift;
    handle->filter_cutoff = filter_cutoff;
    handle->pan = pan;
    handle->previous_value = 0;
    handle->secondary_setup(nullptr, 0);

    return (jlong)handle;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_copy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *) ptr_long;
    auto* new_handle = (SampleHandle*)malloc(sizeof(SampleHandle));
    new_handle->uuid = SampleHandleUUIDGen++;
    new_handle->data = ptr->data;

    new_handle->sample_rate = ptr->sample_rate;
    new_handle->initial_attenuation = ptr->initial_attenuation;
    new_handle->loop_end = ptr->loop_end;
    new_handle->loop_start = ptr->loop_start;

    new_handle->stereo_mode = ptr->stereo_mode;
    new_handle->pitch_shift = ptr->pitch_shift;
    new_handle->filter_cutoff = ptr->filter_cutoff;
    new_handle->pan = ptr->pan;

    if (ptr->volume_profile != nullptr) {
        new_handle->volume_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
        ptr->volume_profile->copy_to(new_handle->volume_profile);
    } else {
        new_handle->volume_profile = nullptr;
    }

    if (ptr->pan_profile != nullptr) {
        new_handle->pan_profile = (ProfileBuffer*)malloc(sizeof(ProfileBuffer));
        ptr->pan_profile->copy_to(new_handle->pan_profile);
    } else {
        new_handle->pan_profile = nullptr;
    }

    new_handle->volume_envelope = (VolumeEnvelope*)malloc(sizeof(VolumeEnvelope));
    ptr->volume_envelope->copy_to(new_handle->volume_envelope);

    new_handle->secondary_setup(ptr->data_buffers, ptr->buffer_count);

    new_handle->release_frame = ptr->release_frame;
    new_handle->kill_frame = ptr->kill_frame;
    new_handle->is_dead = ptr->is_dead;
    new_handle->previous_value = ptr->previous_value;

    return (jlong)new_handle;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_is_1dead_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *) ptr_long;
    return ptr->is_dead;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1working_1frame_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *) ptr_long;
    return ptr->working_frame;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_get_1smoothing_1factor_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *) ptr_long;
    return ptr->smoothing_factor;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfontplayer_SampleHandle_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleHandle *) ptr_long;
    delete ptr;
}
