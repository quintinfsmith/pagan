package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusLayerHistory

open class OpusLayerFrameTracker: OpusLayerHistory() {
    private var frame_tracker: OpusFrameTracker? = null

    override fun on_project_changed() {
        super.on_project_changed()

    }

    override fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        super.set_event(beat_key, position, event)

        val frame_tracker = this.frame_tracker ?: return

        val channels =  this.get_all_channels()
        val line = channels[beat_key.channel].get_line(beat_key.line_offset)
        val (offset, width) = line.get_leaf_offset_and_width(beat_key.beat, position)
        val note_offset = this.get_absolute_value(beat_key, position) ?: return
        frame_tracker.set_event(
            note_offset,
            beat_key.channel,
            beat_key.line_offset,
            offset,
            Rational(event.duration, width)
        )
    }

    fun new_frame_tracker(sample_handle_manager: SampleHandleManager) {
        this.frame_tracker = null

        val new_frame_tracker = OpusFrameTracker(sample_handle_manager, this.transpose, this.tuning_map)
        new_frame_tracker.map_tempo_changes(this.get_global_controller(ControlEventType.Tempo))
        new_frame_tracker.map_beat_frames(this.length)

        val channels = this.get_all_channels()
        for (c in channels.indices) {
            val channel = channels[c]
            new_frame_tracker.new_channel()
            for (i in channel.lines.indices) {
                new_frame_tracker.new_line(c)

            }
        }


        this.frame_tracker = new_frame_tracker
    }


    private fun get_sample_handle_manager(): SampleHandleManager? {
        return this.frame_tracker?.sample_handle_manager
    }
}