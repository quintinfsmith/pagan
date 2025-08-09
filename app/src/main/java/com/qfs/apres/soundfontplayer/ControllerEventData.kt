package com.qfs.apres.soundfontplayer

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class ControllerEventData(val ptr: Long) {
    class IndexedProfileBufferFrame(val first_frame: Int, val last_frame: Int, val value: FloatArray, val increment: FloatArray)

    constructor(frames: List<IndexedProfileBufferFrame>, type: EffectType): this(
        ControllerEventData.intermediary_create(frames, type)
    )

    companion object {
        fun intermediary_create(frames: List<IndexedProfileBufferFrame>, type: EffectType): Long {
            val value_width = frames[0].value.size
            val values = FloatArray(frames.size * value_width)
            val increments = FloatArray(frames.size * value_width)

            for (i in 0 until frames.size) {
                for (j in 0 until value_width) {
                    values[i * value_width] = frames[i].value[j]
                    increments[i * value_width] = frames[i].increment[j]
                }
            }

            return this.create(
                IntArray(frames.size) { i: Int -> frames[i].first_frame },
                IntArray(frames.size) { i: Int -> frames[i].last_frame },
                value_width,
                values,
                increments,
                type.i
            )
        }

        external fun create(frame_indices: IntArray, frame_end_indices: IntArray, value_width: Int, values: FloatArray, increments: FloatArray, type: Int): Long
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}