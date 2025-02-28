package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandleManager

class OpusChannelFrameTracker(val sample_handle_manager: SampleHandleManager) {
    val line_trackers = mutableListOf<OpusLineFrameTracker>()

    fun new_line(i: Int) {
        this.line_trackers.add(i, OpusLineFrameTracker())
    }

    fun process_line(i: Int) {

    }
}