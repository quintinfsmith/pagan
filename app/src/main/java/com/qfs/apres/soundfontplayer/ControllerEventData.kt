package com.qfs.apres.soundfontplayer

class ControllerEventData(val ptr: Long, val type: EffectType) {
    class IndexedProfileBufferFrame(val first_frame: Int, val last_frame: Int, val value: FloatArray, val increment: FloatArray)

    constructor(size: Int, frames: List<IndexedProfileBufferFrame>, type: EffectType): this(
        ControllerEventData.intermediary_create(size, frames, type), type
    )

    companion object {
        fun intermediary_create(size: Int, frames: List<IndexedProfileBufferFrame>, type: EffectType): Long {
            val value_width = frames[0].value.size
            val values = FloatArray(frames.size * value_width)
            val increments = FloatArray(frames.size * value_width)

            for (i in 0 until frames.size) {
                for (j in 0 until value_width) {
                    values[(i * value_width) + j] = frames[i].value[j]
                    increments[(i * value_width) + j] = frames[i].increment[j]
                }
            }

            return this.create(
                size,
                IntArray(frames.size) { i: Int -> frames[i].first_frame },
                IntArray(frames.size) { i: Int -> frames[i].last_frame },
                value_width,
                values,
                increments,
                type.i
            )
        }

        external fun create(size: Int, frame_indices: IntArray, frame_end_indices: IntArray, value_width: Int, values: FloatArray, increments: FloatArray, type: Int): Long
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}