package com.qfs.apres.soundfontplayer

class ControllerEventData(val ptr: Long) {
    constructor(frames: Array<Pair<Pair<Int, Int>, Pair<FloatArray, FloatArray>>>, type: Int): this(
        ControllerEventData.intermediary_create(frames, type)
    )

    companion object {
        fun intermediary_create(frames: Array<Pair<Pair<Int, Int>, Pair<FloatArray, FloatArray>>>, type: Int): Long {
            val value_width = frames[0].second.first.size
            val values = FloatArray(frames.size * value_width)
            val increments = FloatArray(frames.size * value_width)

            for (i in 0 until frames.size) {
                for (j in 0 until value_width) {
                    values[i * value_width] = frames[i].second.first[j]
                    increments[i * value_width] = frames[i].second.second[j]
                }
            }

            return this.create(
                IntArray(frames.size) { i: Int -> frames[i].first.first },
                IntArray(frames.size) { i: Int -> frames[i].first.second },
                value_width,
                values,
                increments,
                type
            )
        }
        external fun create(frame_indices: IntArray, frame_end_indices: IntArray, value_width: Int, values: FloatArray, increments: FloatArray, type: Int): Long
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}