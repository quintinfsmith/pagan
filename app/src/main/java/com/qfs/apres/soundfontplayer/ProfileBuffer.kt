package com.qfs.apres.soundfontplayer

class ProfileBuffer(val ptr: Long) {
    // TODO: Memory Management
    constructor(data: ControllerEventData, start_frame: Int): this(
        create(data.ptr, start_frame)
    )

    companion object {
        external fun create(data_ptr: Long, start_frame: Int): Long
    }

    external fun copy_jni(ptr: Long): Long
    fun copy(): ProfileBuffer {
        return ProfileBuffer(this.copy_jni(this.ptr))
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}

