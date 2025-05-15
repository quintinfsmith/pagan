package com.qfs.apres.soundfontplayer

class ControllerEventData(val ptr: Long) {
    constructor(frames: Array<Pair<Int, Pair<Float, Float>>>, type: Int): this(
        ControllerEventData.Companion.intermediary_create(frames, type)
    )

    companion object {
        fun intermediary_create(frames: Array<Pair<Int, Pair<Float, Float>>>, type: Int): Long {
            return create(
                IntArray(frames.size) { i: Int -> frames[i].first },
                FloatArray(frames.size) { i: Int -> frames[i].second.first },
                FloatArray(frames.size) { i: Int -> frames[i].second.second },
                type
            )
        }
        external fun create(frame_indices: IntArray, values: FloatArray, increments: FloatArray, type: Int): Long
    }
}