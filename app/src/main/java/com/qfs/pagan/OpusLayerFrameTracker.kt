package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerHistory
import com.qfs.pagan.structure.OpusTree

open class OpusLayerFrameTracker: OpusLayerHistory() {
    private var frame_tracker: OpusFrameTracker? = null

    override fun on_project_changed() {
        super.on_project_changed()
    }

    override fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        super.set_event(beat_key, position, event)
        this.set_event_handles(beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.remove_event_handles(beat_key, position)
    }

    override fun split_tree(
        beat_key: BeatKey,
        position: List<Int>,
        splits: Int,
        move_event_to_end: Boolean
    ) {
        val need_frame_track_update = this.get_tree(beat_key, position).is_event()
        super.split_tree(beat_key, position, splits, move_event_to_end)

        if (!need_frame_track_update) {
            return
        }

        val new_position = position + listOf(0)
        this.remove_event_handles(beat_key, new_position)
        this.set_event_handles(beat_key, new_position)
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        super.new_line(channel, line_offset)
        this.frame_tracker!!.new_line(channel, line_offset)
    }

    private fun remove_event_handles(beat_key: BeatKey, position: List<Int>) {
        val frame_tracker = this.frame_tracker ?: return
        val channels =  this.get_all_channels()
        val line = channels[beat_key.channel].get_line(beat_key.line_offset)
        val (offset, _) = line.get_leaf_offset_and_width(beat_key.beat, position)

        frame_tracker.remove_handles(beat_key.channel, beat_key.line_offset, offset)
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        val frame_tracker = this.frame_tracker ?: return

        frame_tracker.insert_beat(beat_index, this.length - 1)
    }

    private fun set_event_handles(beat_key: BeatKey, position: List<Int>) {
        val frame_tracker = this.frame_tracker ?: return
        val event = this.get_tree(beat_key, position).get_event()!!

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