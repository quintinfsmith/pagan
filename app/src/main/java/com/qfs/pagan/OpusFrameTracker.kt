package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor

class OpusFrameTracker(val sample_handle_manager: SampleHandleManager, var transpose: Pair<Int, Int>, var tuning_map: Array<Pair<Int, Int>>) {
    var frame_count = 0
    private val _tempo_ratio_map = mutableListOf<Pair<Float, Float>>()// rational position:: tempo
    private var _cached_beat_frames: Array<Int>? = null
    private var global_volume_map: Map<Int, Pair<Float,Float>>? = null
    private var channel_volume_maps = HashMap<Int, Map<Int, Pair<Float, Float>>>()

    val beat_count: Int
        get() = this._cached_beat_frames?.size ?: 0

    val line_trackers: MutableList<MutableList<OpusLineFrameTracker>> = mutableListOf()
    val midi_channel_map: MutableList<Int> = mutableListOf()

    fun new_line(c: Int, l: Int? = null): OpusLineFrameTracker {
        val new_line = OpusLineFrameTracker()
        this.line_trackers[c].add(l ?: (this.line_trackers[c].size - 1), new_line)
        new_line.generated_frames = Array(this.frame_count * 2) { 0F }
        return new_line
    }

    fun new_channel(c: Int? = null) {
        this.line_trackers.add(c ?: this.line_trackers.size, mutableListOf())
        this.midi_channel_map.add(c ?: this.midi_channel_map.size, 0)
    }

    fun remove_handles(channel: Int, line_offset: Int, offset: Rational) {
        val frame_range = this.calculate_frame_range(offset, Rational(1,1))
        this.line_trackers[channel][line_offset].remove_handles_at_frame(frame_range.first)
    }

    fun set_event(note_offset: Int, channel: Int, line_offset: Int, offset: Rational, duration: Rational) {
        val event = this._gen_midi_event(note_offset, channel, line_offset, offset) ?: return
        val handles = this.sample_handle_manager.gen_sample_handles(event)
        val frame_range = this.calculate_frame_range(offset, duration)
        val line_frame_tracker = this.line_trackers[channel][line_offset]

        line_frame_tracker.remove_handles_at_frame(frame_range.first)

        for (handle in handles) {
            handle.set_release_frame(frame_range.second - frame_range.first)
        }

        line_frame_tracker.add_handles(frame_range.first, handles)
    }

    private fun calculate_frame_range(offset: Rational, duration: Rational): Pair<Int, Int> {
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
        val target_end_position = target_start_position + Rational(duration.n, duration.d * this.beat_count).toFloat()
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

    fun map_tempo_changes(tempo_controller: ActiveController<OpusTempoEvent>) {
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

    fun map_global_volume(volume_controller: ActiveController<OpusVolumeEvent>) {
        this.global_volume_map = this.get_volume_change_map(volume_controller)
    }

    fun map_channel_volume(channel: Int, volume_controller: ActiveController<OpusVolumeEvent>) {
        this.channel_volume_maps[channel] = this.get_volume_change_map(volume_controller)
    }

    fun map_line_volume(channel: Int, line: Int, volume_controller: ActiveController<OpusVolumeEvent>) {
        this.line_trackers[channel][line].set_volume_map(this.get_volume_change_map(volume_controller))
    }

    fun get_line_volume(channel: Int, line_offset: Int, offset: Rational): Float {
        val (frame, _) = this.calculate_frame_range(offset, Rational(0,1))
        val line = this.line_trackers[channel][line_offset]
        return line.get_volume(frame)
    }

    fun get_volume_change_map(volume_controller: ActiveController<OpusVolumeEvent>): Map<Int, Pair<Float, Float>> {
        data class StackItem(val position: List<Int>, val tree: OpusTree<OpusVolumeEvent>?, val offset: Rational)
        val working_hashmap = HashMap<Int, Pair<Float, Float>>()
        var working_volume = volume_controller.get_initial_event().value
        for (b in 0 until volume_controller.beat_count()) {
            val stack: MutableList<StackItem> = mutableListOf(StackItem(listOf(), volume_controller.get_tree(b), Rational(0, 1)))
            while (stack.isNotEmpty()) {
                val working_item = stack.removeAt(0)
                val working_tree = working_item.tree ?: continue

                if (working_tree.is_event()) {
                    val working_event = working_tree.get_event()!!
                    val duration = Rational(working_event.duration, working_item.offset.d)
                    val (start_frame, end_frame) = this.calculate_frame_range(working_item.offset + b, duration)
                    val diff = working_event.value - working_volume
                    if (diff == 0F) {
                        continue
                    }

                    when (working_event.transition) {
                        ControlTransition.Instant -> {
                            working_hashmap[start_frame] = Pair(working_event.value, 0F)
                        }
                        ControlTransition.Linear -> {
                            val d = diff / (end_frame - start_frame).toFloat()
                            working_hashmap[start_frame] = Pair(working_volume, d)
                            working_hashmap[end_frame] =  Pair(working_event.value, 0F)
                        }
                    }
                    working_volume = working_event.value
                } else if (!working_tree.is_leaf()) {
                    val new_width  = working_item.offset.d * working_tree.size
                    for (i in 0 until working_tree.size) {
                        val new_position = working_item.position.toMutableList()
                        new_position.add(i)
                        stack.add(
                            StackItem(
                                new_position,
                                working_tree[i],
                                Rational(working_item.offset.n + i, new_width)
                            )
                        )
                    }
                }
            }
        }

        return working_hashmap.toSortedMap()
    }

    fun map_beat_frames(beat_count: Int): Array<Int> {
        val frames_per_minute = 60F * this.sample_handle_manager.sample_rate.toFloat()

        val beats = mutableListOf(0)

        var working_frame = 0
        val working_tempo = this._tempo_ratio_map[0].second
        var frames_per_beat = (frames_per_minute / working_tempo).toInt()
        var tempo_index = 0
        val frames_to_add = mutableListOf<Float>()
        // First, just get the frame of each beat
        for (i in 1 until beat_count + 1) {
            val beat_position = i.toFloat() / beat_count.toFloat()
            var working_position = (i - 1).toFloat() / beat_count.toFloat()

            while (tempo_index < this._tempo_ratio_map.size) {
                val tempo_change_position = (this._tempo_ratio_map[tempo_index].first)

                if (tempo_change_position < beat_position) {
                    frames_to_add.add(frames_per_beat * (tempo_change_position - working_position))

                    working_position = tempo_change_position
                    frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()
                    tempo_index += 1
                } else {
                    break
                }
            }
            frames_to_add.add(frames_per_beat * (beat_position - working_position))
            working_frame += (frames_to_add.sum() * beat_count).toInt()
            frames_to_add.clear()

            beats.add(working_frame)
        }

        this.frame_count = beats.removeAt(beats.size - 1) ?: 0
        this._cached_beat_frames = beats.toTypedArray()

        return this._cached_beat_frames!!
    }

    private fun _gen_midi_event(note_offset: Int, channel: Int, line_offset: Int, relative_offset: Rational): NoteOn79? {
        val velocity = (this.get_line_volume(channel, line_offset, relative_offset) * 127F).toInt()

        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        // TODO: Better is_percussion check
        val (note, bend) = if (channel == this.line_trackers.size - 1) {
           Pair(27 + note_offset, 0)
        } else {
            // Can happen since we convert RelativeNotes to Absolute ones before passing them to this function
            if (note_offset < 0) {
                return null
            }

            val radix = this.tuning_map.size
            val octave = note_offset / radix
            val offset = this.tuning_map[note_offset % radix]

            // This offset is calculated so the tuning map always reflects correctly
            val transpose_offset = 12F * this.transpose.first.toFloat() / this.transpose.second.toFloat()
            val std_offset = (offset.first.toFloat() * 12F / offset.second.toFloat())

            Pair(
                21 + (octave * 12) + std_offset.toInt() + transpose_offset.toInt(),
                (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512F).toInt()
            )
        }

        return NoteOn79(
            index = 0, // Set index as note is applied
            channel = this.midi_channel_map[channel],
            velocity = velocity shl 8,
            note = note,
            bend = bend
        )
    }

    fun insert_beat(i: Int, old_beat_count: Int) {
        for (x in this._tempo_ratio_map.indices) {
            val (position, tempo) = this._tempo_ratio_map[x]
            val old_beat = position * old_beat_count.toFloat()

            val new_position = if (i <= old_beat) {
                (old_beat + 1) / (old_beat_count + 1).toFloat()
            } else {
                old_beat / (old_beat_count + 1).toFloat()
            }

            this._tempo_ratio_map[x] = Pair(new_position, tempo)
        }

        // TODO: This could be done precisely
        this.map_beat_frames(old_beat_count + 1)


        val frame_range = this.calculate_frame_range(Rational(i, old_beat_count + 1), Rational(1,1))!!
        for (line_trackers in this.line_trackers) {
            for (line_tracker in line_trackers) {
                line_tracker.insert_frames(frame_range.second - frame_range.first, frame_range.first)
            }
        }
    }
}