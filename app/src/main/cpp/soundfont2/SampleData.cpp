#include <malloc.h>
#include "SampleData.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfont2_SampleData_set_1data_1jni(JNIEnv* env, jobject, jshortArray input) {
    auto *ptr = (SampleData *)malloc(sizeof(SampleData));
    int size = env->GetArrayLength(input);

    short* input_ptr = env->GetShortArrayElements(input, 0);

    auto* data_ptr = (short*)malloc(sizeof(short) * size);
    for (int i = 0; i < size; i++) {
        data_ptr[i] = input_ptr[i];
    }

    ptr->data = data_ptr;
    ptr->size = size;

    env->ReleaseShortArrayElements(input, input_ptr, 0);

    return (jlong)ptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfont2_SampleData_destroy_1jni(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleData *)ptr_long;
    delete ptr;
}

