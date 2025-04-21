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
