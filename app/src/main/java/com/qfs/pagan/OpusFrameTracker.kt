package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.structure.OpusTree

class OpusFrameTracker(val sample_handle_manager: SampleHandleManager) {
    var beat_count = 0
    private val _tempo_ratio_map = mutableListOf<Pair<Float, Float>>()// rational position:: tempo
    private var _cached_beat_frames: Array<Int>? = null

    val line_trackers: MutableList<MutableList<OpusLineFrameTracker>> = mutableListOf()


    fun new_line(channel: Int, line: Int) {

    }

    fun set_event(event: NoteOn79, channel: Int, line_offset: Int, offset: Rational, duration: Rational) {
        val handles = this.sample_handle_manager.gen_sample_handles(event)
        val frame_range = this.calculate_event_frame_range(offset, duration)
        val line_frame_tracker = this.line_trackers[channel][line_offset]

        line_frame_tracker.remove_handles_at_frame(frame_range.first)

        for (handle in handles) {
            handle.set_release_frame(frame_range.second - frame_range.first)
        }

        line_frame_tracker.add_handles(frame_range.first, handles)
    }

    private fun calculate_event_frame_range(offset: Rational, duration: Rational): Pair<Int, Int> {
        val beat = offset.toFloat().toInt()
        val frames_per_minute = 60F * this.sample_handle_manager.sample_rate

        // Find the tempo active at the beginning of the beat
        var working_position = beat.toFloat() / this.beat_count.toFloat()
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
        var start_frame = this._cached_beat_frames!![beat]
        val target_start_position = offset.toFloat() / this.beat_count.toFloat()
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_start_position) {
                start_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.beat_count

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        start_frame += (frames_per_beat * (target_start_position - working_position)).toInt() * this.beat_count

        // Calculate End Position
        working_position = target_start_position
        var end_frame = start_frame
        // Note: divide duration to keep in-line with 0-1 range
        val target_end_position = target_start_position + (duration / this.beat_count.toFloat())
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_end_position) {
                end_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.beat_count

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        end_frame += (frames_per_beat * (target_end_position - working_position)).toInt() * this.beat_count

        return Pair(start_frame, end_frame)
    }

    private fun map_tempo_changes(tempo_controller: ActiveController<OpusTempoEvent>) {
        var working_tempo = tempo_controller.initial_event.value

        this._tempo_ratio_map.add(Pair(0f, working_tempo))

        tempo_controller.beats.forEachIndexed { i: Int, tree: OpusTree<OpusTempoEvent>? ->
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
                            (i.toFloat() + working_offset) / tempo_controller.beat_count().toFloat(),
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