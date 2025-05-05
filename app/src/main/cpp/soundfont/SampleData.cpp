#include <jni.h>
#include <malloc.h>
#include "SampleData.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_qfs_apres_soundfont_SampleData_set_1data_1jni(JNIEnv* env, jobject, jshortArray input) {
    auto *ptr = (SampleData *)malloc(sizeof(SampleData));
    int size = env->GetArrayLength(input);

    short*  data_ptr = env->GetShortArrayElements(input, 0);
    ptr->data = data_ptr;
    ptr->size = size;

    return (jlong)ptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qfs_apres_soundfont_SampleData_destroy(JNIEnv* env, jobject, jlong ptr_long) {
    auto *ptr = (SampleData *)ptr_long;
    delete ptr;
}
