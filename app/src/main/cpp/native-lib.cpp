#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_qfs_pagan_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
    // int size = 44100 * 60 * 2;
    int size = 44100;
    int size_b = 10;
    float test[size][size_b];
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size_b; j++) {
            test[i][j] = 4.0;
        }
    }

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

struct Test {
    int a = 0;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_pagan_Test_create(JNIEnv* env, jobject /* this */) {
    struct Test *p = (struct Test *)malloc(sizeof(struct Test));
    return (jlong)p;
}


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

