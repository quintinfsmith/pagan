package com.qfs.apres.soundfontplayer

import com.qfs.apres.soundfontplayer.ProfileBuffer
import com.qfs.apres.soundfontplayer.ProfileBuffer.Companion.create

class ControllerEventData(val ptr: Long) {
    constructor(frames: Array<Pair<Int, Pair<Float, Float>>>): this(
        ControllerEventData.Companion.intermediary_create(frames)
    )

    companion object {
        fun intermediary_create(frames: Array<Pair<Int, Pair<Float, Float>>>): Long {
            return create(
                IntArray(frames.size) { i: Int -> frames[i].first },
                FloatArray(frames.size) { i: Int -> frames[i].second.first },
                FloatArray(frames.size) { i: Int -> frames[i].second.second }
            )
        }
        external fun create(
            frame_indices: IntArray,
            values: FloatArray,
            increments: FloatArray
        ): Long
    }
}