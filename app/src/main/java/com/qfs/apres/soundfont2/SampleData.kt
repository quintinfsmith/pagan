package com.qfs.apres.soundfont2

class SampleData(var ptr: Long) {
    var size: Int = 0

    external fun set_data_jni(data_array: ShortArray): Long
    fun set_data(data_array: ShortArray) {
        this.size = data_array.size
        this.ptr = this.set_data_jni(data_array)
    }

    fun copy(): SampleData {
        return SampleData(this.ptr)
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}