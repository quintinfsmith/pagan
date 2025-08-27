#include <jni.h>
#include <iostream>
#include <string>
#include <android/log.h>

#include "soundfont2/SampleData.cpp"
#include "soundfontplayer/PitchedBuffer.cpp"
#include "soundfontplayer/VolumeEnvelope.cpp"
#include "soundfontplayer/ControllerEventData.cpp"
#include "soundfontplayer/ProfileBuffer.cpp"
#include "soundfontplayer/SampleHandle.cpp"
#include "soundfontplayer/WaveGenerator.cpp"
#include "soundfontplayer/Complex.h"

// extern "C" JNIEXPORT jfloatArray JNICALL
// Java_com_qfs_pagan_MainActivity_test_array(JNIEnv* env, jobject,  float t) {
//     float test[10];
//     for (int i = 0; i < 10; i++) {
//         test[i] = t;
//     }
//     return test;
// }

//extern "C" JNIEXPORT jfloatArray JNICALL
//Java_com_qfs_apres_soundfontplayer_SampleHandle_get_next_frame_inner(
//        JNIEnv* env,
//        jobject,
//        ) {
//}
