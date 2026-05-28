package com.qfs.apres.soundfontplayer

interface JNIObject<T> {
    var ptr: Long
    fun check(): T? {
        return if (this.ptr == 0L) null
        else this as T
    }
}