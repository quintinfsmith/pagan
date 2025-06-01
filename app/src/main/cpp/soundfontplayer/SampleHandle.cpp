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
        jfloat vibrato_frequency,
        jfloat vibrato_delay,
        jfloat vibrato_pitch
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

    handle->pitch_shift = pitch_shift;
    handle->filter_cutoff = filter_cutoff;
    handle->pan = pan;
    handle->previous_value = 0;
    handle->secondary_setup(nullptr, 0);
    if (vibrato_frequency != 0) {
        handle->vibrato_oscillator = new Oscillator(sample_rate, vibrato_frequency);
        handle->vibrato_pitch = vibrato_pitch;
        handle->vibrato_delay = (int)(vibrato_delay * sample_rate);
    } else {
        handle->vibrato_oscillator = nullptr;
        handle->vibrato_pitch = 0;
        handle->vibrato_delay = 0;
    }

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

    new_handle->volume_envelope = (VolumeEnvelope*)malloc(sizeof(VolumeEnvelope));
    ptr->volume_envelope->copy_to(new_handle->volume_envelope);

    new_handle->secondary_setup(ptr->data_buffers, ptr->buffer_count);

    new_handle->working_frame = ptr->working_frame;
    new_handle->release_frame = ptr->release_frame;
    new_handle->kill_frame = ptr->kill_frame;
    new_handle->is_dead = ptr->is_dead;
    new_handle->previous_value = ptr->previous_value;

    if (ptr->vibrato_oscillator != nullptr) {
        new_handle->vibrato_oscillator = new Oscillator(ptr->vibrato_oscillator->sample_rate, ptr->vibrato_oscillator->frequency);
        new_handle->vibrato_pitch = ptr->vibrato_pitch;
        new_handle->vibrato_delay = ptr->vibrato_delay;
    } else {
        new_handle->vibrato_oscillator = nullptr;
        new_handle->vibrato_pitch = 0;
        new_handle->vibrato_delay = 0;
    }

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
