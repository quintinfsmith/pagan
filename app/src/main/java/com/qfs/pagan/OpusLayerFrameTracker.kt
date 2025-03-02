package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusLayerHistory
import com.qfs.pagan.opusmanager.PercussionEvent
import kotlin.math.floor

open class OpusLayerFrameTracker: OpusLayerHistory() {
    private var frame_tracker: OpusFrameTracker? = null

    override fun on_project_changed() {
        super.on_project_changed()

    }

    override fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        super.set_event(beat_key, position, event)

        val frame_tracker = this.frame_tracker ?: return

        val line = this.get_all_channels()[beat_key.channel].get_line(beat_key.line_offset)
        val (offset, width) = line.get_leaf_offset_and_width(beat_key.beat, position)
        val midi_event = this._gen_midi_event(event, beat_key) ?: return

        frame_tracker.set_event(midi_event, beat_key.channel, beat_key.line_offset, offset, Rational(event.duration, width))
    }

    fun new_frame_tracker(sample_handle_manager: SampleHandleManager) {
        this.frame_tracker = null

        val new_frame_tracker = OpusFrameTracker(sample_handle_manager)
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

    private fun _gen_midi_event(event: InstrumentEvent, beat_key: BeatKey): NoteOn79? {
        val velocity = (this.get_line_volume(beat_key.channel, beat_key.line_offset) * 127F).toInt()

        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        val (note, bend) = when (event) {
            is PercussionEvent -> {
                Pair(27 + this.get_percussion_instrument(beat_key.line_offset), 0)
            }
            is AbsoluteNoteEvent -> {
                // Can happen since we convert RelativeNotes to Absolute ones before passing them to this function
                if (event.note < 0) {
                    return null
                }
                val radix = this.tuning_map.size
                val octave = event.note / radix
                val offset = this.tuning_map[event.note % radix]

                // This offset is calculated so the tuning map always reflects correctly
                val transpose_offset = 12F * this.transpose.first.toFloat() / this.transpose.second.toFloat()
                val std_offset = (offset.first.toFloat() * 12F / offset.second.toFloat())

                Pair(
                    21 + (octave * 12) + std_offset.toInt() + transpose_offset.toInt(),
                    (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512F).toInt()
                )
            }
            else -> Pair(0, 0) // Should be unreachable
        }

        return NoteOn79(
            index = 0, // Set index as note is applied
            channel = this.get_channel(beat_key.channel).get_midi_channel(),
            velocity = velocity shl 8,
            note = note,
            bend = bend
        )
    }

    private fun get_sample_handle_manager(): SampleHandleManager? {
        return this.frame_tracker?.sample_handle_manager
    }
}