package com.qfs.pagan

import com.qfs.pagan.opusmanager.OpusLayerHistory

open class OpusLayerFrameTracker: OpusLayerHistory() {
    val channel_frame_trackers = mutableListOf<OpusChannelFrameTracker>()
}