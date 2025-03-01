package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusLayerHistory
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.PercussionEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor

open class OpusLayerFrameTracker: OpusLayerHistory() {
    val channel_frame_trackers = mutableListOf<OpusChannelFrameTracker>()
    var sample_handle_manager: SampleHandleManager? = null
    private val _tempo_ratio_map = mutableListOf<Pair<Float, Float>>()// rational position:: tempo
    private var _cached_beat_frames: Array<Int>? = null

    override fun on_project_changed() {
        super.on_project_changed()

    }

    override fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        super.set_event(beat_key, position, event)
        if (this.sample_handle_manager == null) {
            return
        }

        val handles = this.sample_handle_manager!!.gen_sample_handles(this._gen_midi_event(event, beat_key) ?: return)
        val frame_range = this.calculate_event_frame_range(beat_key, position)

        for (handle in handles) {
            handle.set_release_frame(frame_range.second - frame_range.first)
        }

        val line_frame_tracker = this.get_line_frame_tracker(beat_key.channel, beat_key.line_offset)
        line_frame_tracker.add_handles(frame_range.first, handles)
    }

    fun get_line_frame_tracker(channel: Int, line_offset: Int): OpusLineFrameTracker {
        return this.channel_frame_trackers[channel].get_line(line_offset)
    }

    fun attach_sample_handle_manager(sample_handle_manager: SampleHandleManager) {
        this.sample_handle_manager = sample_handle_manager
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

    private fun calculate_event_frame_range(beat_key: BeatKey, position: List<Int>): Pair<Int, Int> {
        var working_tree = this.get_tree(beat_key)
        var offset = Rational(0, 1)
        var width = 1
        for (p in position) {
            offset.n += p
            offset.d *= working_tree.size
            width *= working_tree.size
        }
        var relative_offset = offset.toFloat()
        var duration = width.toFloat() * (working_tree.get_event()?.duration ?: 1).toFloat()


        val frames_per_minute = 60F * this.sample_handle_manager!!.sample_rate
        // Find the tempo active at the beginning of the beat
        var working_position = beat_key.beat.toFloat() / this.length.toFloat()
        var working_tempo = 0f
        var tempo_index = 0

        for (i in this._tempo_ratio_map.size - 1 downTo 0) {
            val (absolute_offset, tempo) = this._tempo_ratio_map[i]
            if (absolute_offset <= working_position) {
                working_tempo = tempo
                tempo_index = i
                break
            }
        }

        var frames_per_beat = (frames_per_minute / working_tempo).toInt()

        // Calculate Start Position
        var start_frame = this._cached_beat_frames!![beat_key.beat]
        val target_start_position = (beat_key.beat.toFloat() + relative_offset) / this.length.toFloat()
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_start_position) {
                start_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.length

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        start_frame += (frames_per_beat * (target_start_position - working_position)).toInt() * this.length

        // Calculate End Position
        working_position = target_start_position
        var end_frame = start_frame
        // Note: divide duration to keep in-line with 0-1 range
        val target_end_position = target_start_position + (duration / this.length.toFloat())
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_end_position) {
                end_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.length

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        end_frame += (frames_per_beat * (target_end_position - working_position)).toInt() * this.length

        return Pair(start_frame, end_frame)
    }

    private fun map_tempo_changes() {
        val controller = this.controllers.get_controller<OpusTempoEvent>(
            ControlEventType.Tempo)
        var working_tempo = controller.initial_event.value

        this._tempo_ratio_map.add(Pair(0f, working_tempo))

        controller.beats.forEachIndexed { i: Int, tree: OpusTree<OpusTempoEvent>? ->
            if (tree == null) {
                return@forEachIndexed
            }

            val stack = mutableListOf(Triple(tree, 1F, 0F))
            while (stack.isNotEmpty()) {
                val (working_tree, working_ratio, working_offset) = stack.removeAt(0)

                if (working_tree.is_event()) {
                    working_tempo = working_tree.get_event()!!.value
                    this._tempo_ratio_map.add(
                        Pair(
                            (i.toFloat() + working_offset) / this.length.toFloat(),
                            working_tempo
                        )
                    )
                } else if (!working_tree.is_leaf()) {
                    for ((j, child) in working_tree.divisions) {
                        val child_width = working_ratio / working_tree.size.toFloat()
                        stack.add(
                            Triple(
                                child,
                                child_width,
                                working_offset + (j * child_width)
                            )
                        )
                    }
                }
            }
        }
    }
}