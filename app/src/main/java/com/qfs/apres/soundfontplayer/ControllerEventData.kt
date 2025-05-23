package com.qfs.apres.soundfontplayer

class ControllerEventData(val ptr: Long) {
    constructor(frames: Array<Pair<Pair<Int, Int>, Pair<Float, Float>>>, type: Int): this(
        intermediary_create(frames, type)
    )

    companion object {
        fun intermediary_create(frames: Array<Pair<Pair<Int, Int>, Pair<Float, Float>>>, type: Int): Long {
            return create(
                IntArray(frames.size) { i: Int -> frames[i].first.first },
                IntArray(frames.size) { i: Int -> frames[i].first.second },
                FloatArray(frames.size) { i: Int -> frames[i].second.first },
                FloatArray(frames.size) { i: Int -> frames[i].second.second },
                type
            )
        }
        external fun create(frame_indices: IntArray, frame_end_indices: IntArray, values: FloatArray, increments: FloatArray, type: Int): Long
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}