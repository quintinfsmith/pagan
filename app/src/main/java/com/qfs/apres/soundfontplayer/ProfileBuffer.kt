package com.qfs.apres.soundfontplayer

class ProfileBuffer(val ptr: Long) {
    // TODO: Memory Management
    constructor(data: ControllerEventData, start_frame: Int = 0): this(
        create(data.ptr, start_frame)
    )

    companion object {
        external fun create(data_ptr: Long, start_frame: Int): Long
    }

    external fun set_frame_jni(ptr: Long, frame: Int)
    fun set_frame(frame: Int) {
        this.set_frame_jni(this.ptr, frame)
    }

    external fun copy_jni(ptr: Long): Long
    fun copy(): ProfileBuffer {
        return ProfileBuffer(this.copy_jni(this.ptr))
    }

    external fun destroy_jni(ptr: Long, deep: Boolean)
    fun destroy(deep: Boolean = false) {
        this.destroy_jni(this.ptr, deep)
    }

    external fun get_data_ptr_jni(ptr: Long): Long
    fun get_data(): ControllerEventData {
        return ControllerEventData(this.get_data_ptr_jni(this.ptr))
    }
}

