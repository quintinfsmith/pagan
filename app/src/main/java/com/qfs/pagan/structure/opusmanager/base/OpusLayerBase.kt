package com.qfs.pagan.structure.opusmanager.base

import com.qfs.apres.Midi
import com.qfs.apres.event.BalanceMSB
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.Text
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event.VolumeMSB
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONParser
import com.qfs.json.JSONString
import com.qfs.pagan.enumerate
import com.qfs.pagan.jsoninterfaces.OpusManagerJSONInterface
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.ActiveControlSetJSONInterface
import com.qfs.pagan.structure.opusmanager.OpusChannelJSONInterface
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectControlSet
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.Effectable
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.DelayController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.SingleFloatEvent
import com.qfs.pagan.structure.opusmanager.utils.checked_cast
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The logic of the Opus Manager.
 *
 * This is completely separated from user interface or state.
 * @constructor Creates an unusably empty object. new() / load() / import() need to be called still
 */
open class OpusLayerBase: Effectable {
    companion object {
        private var _channel_uuid_generator: Int = 0x00

        fun gen_channel_uuid(): Int {
            return this._channel_uuid_generator++
        }

        fun get_ordered_beat_key_pair(first: BeatKey, second: BeatKey): Pair<BeatKey, BeatKey> {
            val (from_key, to_key) = if (first.channel < second.channel) {
                Pair(
                    BeatKey(first.channel, first.line_offset, -1),
                    BeatKey(second.channel, second.line_offset, -1)
                )
            } else if (first.channel == second.channel) {
                Pair(
                    BeatKey(
                        first.channel,
                        min(first.line_offset, second.line_offset),
                        -1
                    ),
                    BeatKey(
                        first.channel,
                        max(first.line_offset, second.line_offset),
                        -1
                    )
                )
            } else {
                Pair(
                    BeatKey(second.channel, second.line_offset, -1),
                    BeatKey(first.channel, first.line_offset, -1)
                )
            }
            from_key.beat = min(first.beat, second.beat)
            to_key.beat = max(first.beat, second.beat)
            return Pair(from_key, to_key)
        }

        fun tree_from_midi(midi: Midi): Triple<ReducibleTree<Set<Array<Int>>>, List<ReducibleTree<OpusTempoEvent>>, List<Triple<Int, Int?, Int?>>> {
            var beat_size = midi.get_ppqn()
            var total_beat_offset = 0
            var last_ts_change = 0
            val beat_values: MutableList<ReducibleTree<Set<Array<Int>>>> = mutableListOf()
            val tempo_line = mutableListOf<ReducibleTree<OpusTempoEvent>>()
            var max_tick = 0
            var working_tempo: Float
            val instrument_map = mutableListOf<Triple<Int, Int?, Int?>>()
            val active_event_map = HashMap<Pair<Int, Int>, Array<Int>>()

            var denominator = 4F
            for (pair in midi.get_all_events()) {
                val tick = pair.first
                val event = pair.second

                max_tick = max(tick, max_tick)
                val beat_index = ((tick - last_ts_change) / beat_size) + total_beat_offset
                val inner_beat_offset = (tick - last_ts_change) % beat_size
                if (event is NoteOn && event.get_velocity() > 0) {
                    val (channel, note) = Pair(event.channel, event.get_note())

                    // Turn off note if its playing
                    if (active_event_map.containsKey(Pair(channel, note))) {
                        val existing_event = active_event_map[Pair(channel, note)]!!
                        existing_event[2] = tick - existing_event[2]
                    }

                    // Add trees to list of trees
                    while (beat_values.size <= beat_index) {
                        val new_tree = ReducibleTree<Set<Array<Int>>>()
                        new_tree.set_size(beat_size)
                        beat_values.add(new_tree)
                    }

                    val tree = beat_values[beat_index]
                    val eventset = if (tree[inner_beat_offset].has_event()) {
                        tree[inner_beat_offset].get_event()!!.toMutableSet()
                    } else {
                        mutableSetOf()
                    }

                    val opus_event = arrayOf(
                        channel,
                        note,
                        tick
                    )
                    eventset.add(opus_event)

                    tree[inner_beat_offset].set_event(eventset)
                    active_event_map[Pair(channel, event.get_note())] = opus_event
                } else if ((event is NoteOn && event.get_velocity() == 0) || event is NoteOff) {
                    val (channel, note) = if (event is NoteOn) {
                        Pair(event.channel, event.get_note())
                    } else {
                        Pair((event as NoteOff).channel, event.get_note())
                    }

                    val opus_event = active_event_map.remove(Pair(channel, note)) ?: continue
                    opus_event[2] = tick - opus_event[2]
                } else if (event is TimeSignature) {
                    total_beat_offset += (tick - last_ts_change) / beat_size

                    denominator = 2F.pow(event.get_denominator())
                    val new_beat_size = (midi.get_ppqn().toFloat() * (4 / denominator)).toInt()

                    // Need to resize the current beat to match the time signature that change if noteons
                    // have already been added to that tree (shouldn't ever happen)
                    if (beat_index < beat_values.size) {
                        val original_beat_size = beat_size
                        val tree = beat_values[beat_index]
                        val tree_divisions = tree.divisions.toList()
                        tree.set_size(new_beat_size)
                        for ((f, child) in tree_divisions) {
                            if (!child.has_event()) continue
                            val new_index = f * new_beat_size / original_beat_size

                            val eventset = if (!tree[new_index].has_event()) {
                                mutableSetOf()
                            } else {
                                tree[new_index].get_event()!!.toMutableSet()
                            }
                            for (e in child.get_event()!!) {
                                eventset.add(e)
                            }
                            tree[new_index].set_event(eventset)
                        }
                    }

                    last_ts_change = tick
                    beat_size = new_beat_size
                } else if (event is SetTempo) {
                    working_tempo = ((event.get_bpm() * (denominator / 4)) * 1000F).roundToInt().toFloat() / 1000F

                    while (tempo_line.size <= beat_index) {
                        val new_tree = ReducibleTree<OpusTempoEvent>()
                        new_tree.set_size(beat_size)
                        tempo_line.add(new_tree)
                    }

                    val tree = tempo_line[beat_index]
                    tree[inner_beat_offset].set_event(OpusTempoEvent(working_tempo))
                } else if (event is ProgramChange) {
                    instrument_map.add(Triple(event.channel, null, event.get_program()))
                } else if (event is BankSelect) {
                    instrument_map.add(Triple(event.channel, event.value, null))
                }
            }

            for ((_, opus_event) in active_event_map) {
                opus_event[2] = max_tick - opus_event[2]
            }

            active_event_map.clear()

            total_beat_offset += (max_tick - last_ts_change) / beat_size
            val opus = ReducibleTree<Set<Array<Int>>>()

            if (beat_values.isEmpty()) {
                for (i in 0 until 4) {
                    beat_values.add(ReducibleTree())
                }
            }

            opus.set_size(beat_values.size)

            var overflow_events = mutableSetOf<Array<Int>>()
            for ((i, beat_tree) in beat_values.enumerate()) {
                // Quantize the beat ////////////
                val quantized_tree = ReducibleTree<Set<Array<Int>>>()
                quantized_tree.set_size(beat_tree.size)

                if (overflow_events.isNotEmpty()) {
                    quantized_tree[0].set_event(overflow_events.toSet())
                    overflow_events = mutableSetOf()
                }

                // Can easily merge quantized positions since the beats are still flat
                val qmap = beat_tree.get_quantization_map(listOf(2, 2, 2, 3, 5, 7))
                for ((new_position, old_positions) in qmap) {
                    val new_event_set = mutableSetOf<Array<Int>>()
                    for (old_position in old_positions) {
                        val next_tree = beat_tree[old_position]
                        for (e in next_tree.get_event()!!) {
                            new_event_set.add(e)
                        }
                    }

                    if (new_position == quantized_tree.size) {
                        if (i < beat_values.size - 1) {
                            for (e in new_event_set) {
                                overflow_events.add(e)
                            }
                        }
                    } else {
                        if (quantized_tree[new_position].has_event()) {
                            for (e in quantized_tree[new_position].get_event()!!) {
                                new_event_set.add(e)
                            }
                        }

                        quantized_tree[new_position].set_event(new_event_set.toSet())
                    }
                }
                /////////////////////////////////////
                opus[i] = quantized_tree
            }

            for (tree in tempo_line) {
                tree.reduce()
                tree.clear_singles()
            }

            /*
                NOTE: tempo_line *could* have more beats than the opus if there is a tempo change
                after the last NoteOff event. that gets ignored here
             */
            return Triple(
                opus,
                tempo_line.subList(0, min(tempo_line.size, opus.size)),
                instrument_map
            )
        }

        fun initialize_basic(): OpusLayerBase {
            val new_manager = OpusLayerBase()
            new_manager.new_channel()
            new_manager.new_channel(is_percussion = true)
            new_manager.set_beat_count(4)
            new_manager.set_project_name(null)
            new_manager.set_project_notes(null)
            return new_manager
        }

        fun next_position(position: List<Int>, i: Int): List<Int> {
            return List(position.size + 1) { j: Int ->
                if (j == position.size) {
                    i
                } else {
                    position[j]
                }
            }
        }
    }

    var length: Int = 1
    var controllers = EffectControlSet(this.length, setOf(EffectType.Tempo))
    var channels: MutableList<OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>>> = mutableListOf()
    var project_name: String? = null
    var project_notes: String? = null
    var transpose: Pair<Int, Int> = Pair(0, 12)
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) }
    var marked_sections = HashMap<Int, String?>()

    private var _cached_instrument_line_map = mutableListOf<Pair<Int, Int>>()
    private var _cached_std_line_map = HashMap<Pair<Int, Int>, Int>()

    // key: absolute line
    // Value: first is always a pointer to cached_abs_line_map, second and third are pointers to the relative ctl lines
    private var _cached_abs_line_map_map = mutableListOf<Triple<Int, CtlLineLevel?, EffectType?>>()
    private var _cached_inv_abs_line_map_map = HashMap<Int, Int>()

    private val _cached_row_map = HashMap<Int, Int>() // Key: visible line, Value: control_line
    private val _cached_inv_visible_line_map = HashMap<Int, Int>()
    private val _cached_ctl_map_line = HashMap<Triple<Int, Int, EffectType>, Int>()
    private val _cached_ctl_map_channel = HashMap<Pair<Int, EffectType>, Int>()
    private val _cached_ctl_map_global = HashMap<EffectType, Int>()
    private var _channel_uuid_map = HashMap<Int, OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>>>()

    internal var _blocked_action_catcher = 0
    internal var project_changing = false


    fun get_radix(): Int {
        return this.tuning_map.size
    }

    //// RO Functions ////
    /**
     * Calculates the number of channels in use.
     */
    fun get_channel_count(): Int {
        return this.channels.size
    }

    /**
     * Calculates the position of the first leaf in a given tree at [beat_key]/[start_position]
     */
    fun get_first_position(beat_key: BeatKey, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_tree(beat_key, output)
        while (!tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    /**
     * Calculates the position of the first leaf of the global [type] effect controller in a given tree at [beat]/[start_position]
     */
    fun get_first_position_global_ctl(type: EffectType, beat: Int, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_global_ctl_tree<EffectEvent>(type, beat, output)
        while (!tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    /**
     * Calculates the position of the first leaf of the channel [type] effect controller in a given tree at [channel][beat]/[start_position]
     */
    fun get_first_position_channel_ctl(type: EffectType, channel: Int, beat: Int, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat, output)
        while (!tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    /**
     * Calculates the position of the first leaf of the line [type] effect controller in a given tree at [beat_key]/[start_position]
     */
    fun get_first_position_line_ctl(type: EffectType, beat_key: BeatKey, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key, output)
        while (!tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    /**
     *  Insert extra lines to fit overlapping events (happens on import midi or old savve file versions)
     */
    private fun _reshape_lines_from_blocked_trees() {
        val channels = this.get_all_channels()
        for (i in channels.indices) {
            val remap_trees = mutableListOf<Pair<Int, MutableList<Triple<BeatKey, List<Int>, Int>>>>() // BeatKey, Position, New Line Offset
            val working_channel = channels[i]
            for (j in 0 until channels[i].size) {
                var beat_key = BeatKey(i, j, 0)
                var position = this.get_first_position(beat_key, listOf())

                if (!this.get_tree(beat_key, position).has_event()) {
                    val pair = this.get_proceeding_event_position(beat_key, position) ?: continue
                    beat_key = BeatKey(i, j, pair.first)
                    position = pair.second
                }

                val current_remap = mutableListOf<Triple<BeatKey, List<Int>, Int>>()
                val overlap_lanes = mutableListOf<Rational?>()

                while (true) {
                    val (offset, width) = this.get_leaf_offset_and_width(beat_key, position)
                    val end_position = offset + Rational(
                        this.get_tree(beat_key, position).get_event()!!.duration, width
                    )

                    var lane_index = 0
                    while (lane_index < overlap_lanes.size) {
                        val check_position = overlap_lanes[lane_index]
                        if (check_position == null) {
                            break
                        } else if (check_position <= offset) {
                            overlap_lanes[lane_index] = null
                            break
                        }

                        lane_index += 1
                    }

                    if (lane_index == overlap_lanes.size) {
                        overlap_lanes.add(null)
                    }
                    overlap_lanes[lane_index] = end_position

                    if (lane_index != 0) {
                        current_remap.add(Triple(beat_key.copy(), position.toList(), lane_index))
                    }

                    val pair = this.get_proceeding_event_position(beat_key, position) ?: break
                    beat_key.beat = pair.first
                    position = pair.second
                }
                remap_trees.add(Pair(overlap_lanes.size, current_remap))
            }


            for (j in remap_trees.size - 1 downTo 0) {
                val (lines_to_insert, remaps) = remap_trees[j]
                for (k in 0 until lines_to_insert - 1) {
                    this.new_line(i, j + 1)
                    for ((type, controller) in working_channel.lines[j].controllers.get_all()) {

                        working_channel.lines[j + 1].controllers.new_controller(type)
                        working_channel.lines[j + 1].get_controller<EffectEvent>(type).set_initial_event(
                            controller.initial_event.copy()
                        )
                    }
                    if (this.is_percussion(i)) {
                        this.percussion_set_instrument(
                            i,
                            j + 1,
                            this.get_percussion_instrument(i, j)
                        )
                    }
                }

                val replaced_beat_keys = mutableSetOf<BeatKey>()
                for ((working_beat_key, working_position, new_index) in remaps) {
                    val new_key = BeatKey(
                        working_beat_key.channel,
                        working_beat_key.line_offset + new_index,
                        working_beat_key.beat
                    )

                    if (!replaced_beat_keys.contains(new_key)) {
                        val new_tree = this.get_tree_copy(working_beat_key)

                        new_tree.traverse { working_tree: ReducibleTree<*>, event: InstrumentEvent? ->
                            if (event != null) {
                                working_tree.unset_event()
                            }
                        }

                        this.replace_tree(new_key, listOf(), new_tree)
                        replaced_beat_keys.add(new_key)
                    }
                    this.replace_tree(
                        new_key,
                        working_position,
                        this.get_tree_copy(working_beat_key, working_position)
                    )
                    this.unset(working_beat_key, working_position)
                }
            }
        }
    }

    /**
     * Calculates how many instrument lines are in use.
     */
    fun get_total_line_count(): Int {
        return this._cached_instrument_line_map.size
    }

    /**
     * Calculates how many lines down a given row is.
     */
    fun get_instrument_line_index(channel_index: Int, line_offset: Int): Int {
        return this._cached_std_line_map[Pair(channel_index, line_offset)] ?: throw IndexOutOfBoundsException()
    }

    /**
     * Finds all the BeatKeys in a range denoted by [top_left_key] & [bottom_right_key].
     */
    fun get_beatkeys_in_range(top_left_key: BeatKey, bottom_right_key: BeatKey): List<BeatKey> {
        // No point in throwing an error if beats are out of range, so just limit them
        top_left_key.beat = max(0, min(top_left_key.beat, this.length - 1))
        bottom_right_key.beat = max(0, min(bottom_right_key.beat, this.length - 1))

        val output = mutableListOf<BeatKey>()
        this.get_all_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>> ->
            if (i < top_left_key.channel || i > bottom_right_key.channel) {
                return@forEachIndexed // Continues
            }

            val (start, end) = if (top_left_key.channel == bottom_right_key.channel) {
                Pair(top_left_key.line_offset, bottom_right_key.line_offset)
            } else {
                when (i) {
                    top_left_key.channel -> {
                        Pair(top_left_key.line_offset, channel.size - 1)
                    }

                    bottom_right_key.channel -> {
                        Pair(0, bottom_right_key.line_offset)
                    }

                    else -> {
                        Pair(0, channel.size - 1)
                    }
                }
            }

            for (j in start..end) {
                for (k in top_left_key.beat..bottom_right_key.beat) {
                    output.add(BeatKey(i, j, k))
                }
            }
        }
        return output
    }

    /**
     * Calculate the x & y difference between two BeatKeys [beata] & [beatb]
     */
    fun get_abs_difference(beata: BeatKey, beatb: BeatKey): Pair<Int, Int> {
        val beata_y = this.get_instrument_line_index(beata.channel, beata.line_offset)
        val beatb_y = this.get_instrument_line_index(beatb.channel, beatb.line_offset)

        return Pair(beatb_y - beata_y, beatb.beat - beata.beat)
    }

    /**
     * Calculate which channel and line offset is denoted by the [absolute]th line
     */
    fun get_channel_and_line_offset(absolute: Int): Pair<Int, Int> {
        if (absolute >= this._cached_instrument_line_map.size) {
            throw IndexOutOfBoundsException()
        }

        return this._cached_instrument_line_map[absolute]
    }

    /**
     * Get the midi instrument current used by Channel [channel]
     */
    fun get_channel_instrument(channel: Int): Pair<Int, Int> {
        return this.get_channel(channel).get_instrument()
    }

    /**
     * Get the percussion instrument used on the [line_offset]th line of the percussion channel
     */
    fun get_percussion_instrument(channel: Int, line_offset: Int): Int {
        if (!this.is_percussion(channel)) {
            throw PercussionChannelRequired(channel)
        }
        return (this.channels[channel] as OpusPercussionChannel).get_instrument(line_offset)
    }

    /**
     * Get the tree structure found within the BeatKey [beat] at [position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun get_percussion_tree(beat_key: BeatKey, position: List<Int>? = null): ReducibleTree<PercussionEvent> {
        if (!this.is_percussion(beat_key.channel)) throw PercussionChannelRequired(beat_key.channel)

        val percussion_channel = this.channels[beat_key.channel]
        if (beat_key.line_offset >= percussion_channel.size) throw BadBeatKey(beat_key)

        return (percussion_channel as OpusPercussionChannel).get_tree(beat_key.line_offset, beat_key.beat, position ?: listOf())
    }

    /**
     * Get the tree structure found within the BeatKey [beat_key] at [position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun get_tree(beat_key: BeatKey, position: List<Int>? = null): ReducibleTree<InstrumentEvent> {
        val working_channel = try {
            this.get_channel(beat_key.channel)
        } catch (_: IndexOutOfBoundsException) {
            throw BadBeatKey(beat_key)
        }
        if (beat_key.line_offset >= working_channel.size) {
            throw BadBeatKey(beat_key)
        }
        return working_channel.get_tree(
            beat_key.line_offset,
            beat_key.beat,
            position ?: listOf()
        )
    }

    /**
     * Get a copy of the tree structure found within the BeatKey [beat_key] at [position]
     * [position] defaults to null, indicating the root tree of the beat
    */
    fun get_tree_copy(beat_key: BeatKey, position: List<Int>? = null): ReducibleTree<InstrumentEvent> {
        // Because of the variance (out InstrumentEvent) my copy function in the OpusTree doesn't work correctly
        // Instead just copy the events here
        val working_tree = this.get_tree(beat_key, position).copy { event ->
            event?.copy()
        }
        return working_tree
    }

    /**
     * Get a copy of the tree structure found in the line controller of type [type] at [beat_key]/[position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun <T : EffectEvent> get_line_ctl_tree_copy(type: EffectType, beat_key: BeatKey, position: List<Int>? = null): ReducibleTree<T> {
        // Because of the variance (out InstrumentEvent) my copy function in the OpusTree doesn't work correctly
        // Instead just copy the events here
        val working_tree = this.get_line_ctl_tree<T>(type, beat_key, position).copy()
        working_tree.traverse { tree: ReducibleTree<T>, event: T? ->
            if (event != null) {
                val event_copy: T = event.copy() as T
                tree.set_event(event_copy)
            }
        }
        return working_tree
    }

    /**
     * Get a copy of the tree structure found in the channel controller of channel [channel] of type [type] at [beat]/[position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun <T : EffectEvent> get_channel_ctl_tree_copy(type: EffectType, channel: Int, beat: Int, position: List<Int>? = null): ReducibleTree<T> {
        // Because of the variance (out InstrumentEvent) my copy function in the OpusTree doesn't work correctly
        // Instead just copy the events here
        val working_tree = this.get_channel_ctl_tree<T>(type, channel, beat, position).copy()
        working_tree.traverse { tree: ReducibleTree<T>, event: T? ->
            if (event != null) {
                val event_copy: T = event.copy() as T
                tree.set_event(event_copy)
            }
        }
        return working_tree
    }

    /**
     * Get a copy of the tree structure found in the global controller of type [type] at [beat]/[position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun <T : EffectEvent> get_global_ctl_tree_copy(type: EffectType, beat: Int, position: List<Int>? = null): ReducibleTree<T> {
        // Because of the variance (out InstrumentEvent) my copy function in the OpusTree doesn't work correctly
        // Instead just copy the events here
        val working_tree = this.get_global_ctl_tree<T>(type, beat, position).copy()
        working_tree.traverse { tree: ReducibleTree<T>, event: T? ->
            if (event != null) {
                val event_copy: T = event.copy() as T
                tree.set_event(event_copy)
            }
        }
        return working_tree
    }

    /**
     * Get the tree structure found in the channel controller of channel [channel] of type [type] at [beat]/[position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun <T : EffectEvent> get_channel_ctl_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>? = null): ReducibleTree<T> {
        if (channel >= this.channels.size) {
            throw InvalidChannel(channel)
        }
        return this.get_channel(channel).get_ctl_tree(
            type,
            beat,
            position ?: listOf()
        )
    }

    /**
     * Get a copy of the tree structure found in the line controller of type [type] at [beat_key]/[position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun <T : EffectEvent> get_line_ctl_tree(type: EffectType, beat_key: BeatKey, position: List<Int>? = null): ReducibleTree<T> {
        if (beat_key.channel >= this.channels.size) throw BadBeatKey(beat_key)

        val working_channel = this.get_channel(beat_key.channel)
        if (beat_key.line_offset > working_channel.size) throw BadBeatKey(beat_key)

        return working_channel.get_ctl_tree(
            beat_key.line_offset,
            type,
            beat_key.beat,
            position ?: listOf()
        )
    }

    /**
     * Get the tree structure found in the global controller of type [type] at [beat]/[position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun <T : EffectEvent> get_global_ctl_tree(type: EffectType, beat: Int, position: List<Int>? = null): ReducibleTree<T> {
        return this.get_controller<T>(type).get_tree(beat, position)
    }


    /**
     * Get the leaf immediately after the tree found at [beat_key]/[position], if any
     * *it may not be an immediate sibling, rather an aunt, niece, etc*
     */
    fun get_proceeding_leaf(beat_key: BeatKey, position: List<Int>): ReducibleTree<out InstrumentEvent>? {
        val pair = this.get_proceeding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    /**
     * Get the last event before the tree at [beat_key]/[position], if any exist.
     * This may or may not be in the preceding leaf, but will look for the first leaf with an associated event.
     */
    fun get_preceding_event(beat_key: BeatKey, position: List<Int>): InstrumentEvent? {
        val (adj_beat_key, adj_position) = this.get_preceding_event_position(beat_key, position) ?: return null
        return this.get_tree(adj_beat_key, adj_position).get_event()
    }

    fun get_preceding_event_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        // Gets first preceding event. may skip empty leafs
        var working_position = position.toList()
        var working_beat_key = beat_key
        while (!this.get_tree(working_beat_key, working_position).has_event()) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: return null
            working_beat_key = pair.first
            working_position = pair.second
        }
        return Pair(working_beat_key, working_position)
    }

    /**
     * Get the location of the next event after the node @ [beat_key]/[position], if one exists.
     */
    fun get_proceeding_event_position(beat_key: BeatKey, position: List<Int>): Pair<Int, List<Int>>? {
        return this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_proceding_event_position(beat_key.beat, position)
    }

    /**
     * Get the leaf immediately before the tree found at [beat_key]/[position], if any
     * *it may not be an immediate sibling, rather an aunt, niece, etc*
     */
    fun get_preceding_leaf(beat_key: BeatKey, position: List<Int>): ReducibleTree<out InstrumentEvent>? {
        // Gets first preceding leaf, event or not
        val pair = this.get_preceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    /**
     * Get the location of the leaf immediately before the tree found at [beat_key]/[position].
     * *it may not be an immediate sibling, rather an aunt, niece, etc*
     */
    fun get_preceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        val working_position = position.toMutableList()
        val working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)

        // Move left/up
        while (true) {
            if (working_position.isNotEmpty()) {
                if (working_position.last() > 0) {
                    working_position[working_position.size - 1] -= 1
                    break
                } else {
                    working_position.removeAt(working_position.size - 1)
                }
            } else if (working_beat_key.beat > 0) {
                working_beat_key.beat -= 1
                break
            } else {
                return null
            }
        }

        var working_tree = this.get_tree(working_beat_key, working_position)

        // Move right/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(working_tree.size - 1)
            working_tree = working_tree[working_tree.size - 1]
        }

        return Pair(working_beat_key, working_position)
    }

    /**
     * Get the location of the leaf immediately after the tree found at [beat_key]/[position].
     * *it may not be an immediate sibling, rather an aunt, niece, etc*
     */
    fun get_proceeding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        var working_position = position.toMutableList()
        val working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)
        var working_tree = this.get_tree(working_beat_key, working_position)

        // Move right/up
        while (true) {
            if (working_tree.parent != null) {
                if (working_tree.parent!!.size - 1 > working_position.last()) {
                    working_position[working_position.size - 1] += 1
                    working_tree = this.get_tree(working_beat_key, working_position)
                    break
                } else {
                    working_position.removeAt(working_position.size - 1)
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat_key.beat < this.length - 1) {
                working_beat_key.beat += 1
                working_position = mutableListOf()
                working_tree = this.get_tree(working_beat_key, working_position)
                break
            } else {
                return null
            }
        }
        // Move left/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(0)
            working_tree = working_tree[0]
        }
        return Pair(working_beat_key, working_position)
    }

    /**
     * Get the leaf immediately after (in a depth-first-search) the node @ [beat]/[position] in the global controller of type [ctl_type]
     */
    fun get_global_ctl_proceeding_leaf_position(ctl_type: EffectType, beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        return this.get_controller<EffectEvent>(ctl_type).get_proceding_leaf_position(beat, position)
    }

    /**
     * Get the leaf immediately after (in a depth-first-search) the node @ [beat]/[position] in the [channel] controller of type [ctl_type]
     */
    fun get_channel_ctl_proceeding_leaf_position(ctl_type: EffectType, channel: Int, beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        return this.get_channel(channel).get_controller<EffectEvent>(ctl_type).get_proceding_leaf_position(beat, position)
    }

    /**
     * Get the leaf immediately after (in a depth-first-search) the line controller's node @ [beat_key]/[position] of the controller of type [ctl_type]
     */
    fun get_line_ctl_proceeding_leaf_position(ctl_type: EffectType, beat_key: BeatKey, position: List<Int>): Pair<Int, List<Int>>? {
        return this.get_channel(beat_key.channel).lines[beat_key.line_offset].get_controller<EffectEvent>(ctl_type).get_proceding_leaf_position(beat_key.beat, position)
    }

    /**
     * Get the value of the event at location[beat_key]/[position], if any.
     * if the event is relative, it will look back and add up preceding values
     */
    fun get_absolute_value(beat_key: BeatKey, position: List<Int>): Int? {
        val tree = this.get_tree(beat_key, position)

        val event = tree.get_event()
        if (event is AbsoluteNoteEvent) {
            return event.note
        }


        var working_beat_key = beat_key
        var working_position = position.toList()
        var abs_value = 0
        // Need the value set flag in the case  the value is set to 0
        var value_set_flag = false

        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            working_beat_key = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat_key, working_position)

            when (val working_event = working_tree.get_event()) {
                is RelativeNoteEvent -> {
                    value_set_flag = true
                    abs_value += working_event.offset
                }

                is AbsoluteNoteEvent -> {
                    value_set_flag = true
                    abs_value += working_event.note
                    break
                }

                else -> {
                    continue
                }
            }
        }
        return if (event == null) {
            if (value_set_flag) {
                abs_value
            } else {
                null
            }
        } else if (event is RelativeNoteEvent) {
            abs_value + event.offset
        } else { // Unreachable
            null
        }
    }

    /**
     * Check if the tree at location [beat_key]/[position] has any absolute event
     * between it and the beginning of the opus on it's line.
     */
    fun has_preceding_absolute_event(beat_key: BeatKey, position: List<Int>): Boolean {
        var working_beat_key = beat_key
        var working_position = position.toList()

        var output = false
        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            working_beat_key = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.has_event()) {
                val working_event = working_tree.get_event()!!
                if (working_event is AbsoluteNoteEvent) {
                    output = true
                    break
                }
            }
        }

        return output
    }

    /**
     * Checks if the [channel] is assigned to be used as percussion.
     */
    fun is_percussion(channel: Int): Boolean {
        return this.get_channel(channel) is OpusPercussionChannel
    }

    /**
     * Given the instrument line index [y], get the CtlLevel if the line is a controller.
     */
    fun ctl_line_level(y: Int): CtlLineLevel? {
        return this._cached_abs_line_map_map[y].second
    }

    /**
     * Given the instrument line index [y], get the ControlEventType if the line is a controller.
     */
    fun get_ctl_line_type(y: Int): EffectType? {
        return this._cached_abs_line_map_map[y].third
    }

    /**
     * Given the instrument line index [y], get a pointer Int, the CtlLineLeve and the ControlEventType.
     * The pointer value will be different depending on the CtlLineLevel.
     * if it's not a Controller or its a Line level controller, the pointer will be the instrument line index.
     * if it's a Channel Controller, the pointer will be the channel.
     * if it's a Global Controller, the pointer is 0 and isn't needed.
     */
    fun get_ctl_line_info(y: Int): Triple<Int, CtlLineLevel?, EffectType?> {
        return this._cached_abs_line_map_map[y]
    }

    /**
     * Given the instrument line index [y] (doesn't consider controllers) get the actual line index (DOES consider controllers)
     */
    fun get_actual_line_index(y: Int): Int {
        return this._cached_inv_abs_line_map_map[y]!!
    }

    /**
     * Get the Channel Object @ [channel]
     */
    fun get_channel(channel: Int): OpusChannelAbstract<*, *> {
        return this.channels[channel]
    }

    /**
     * Get the [type] Controller of the line at [channel], [line_offset]
     */
    fun <T: EffectEvent> get_line_controller(type: EffectType, channel: Int, line_offset: Int): EffectController<T> {
        return this.get_channel(channel).lines[line_offset].get_controller(type)
    }

    /**
     * Get the [type] controller of the channel at [channel]
     */
    fun <T : EffectEvent> get_channel_controller(type: EffectType, channel: Int): EffectController<T> {
        val controller = this.get_channel(channel).get_controller<T>(type)
        return controller
    }

    /**
     * Get the [type] controller of the project.
     */
    fun <T : EffectEvent> get_global_controller(type: EffectType): EffectController<T> {
        return this.get_controller(type)
    }

    /**
     * Get the [type] controller event for the line at [beat_key].channel, [beat_key].line_offset found at [beat_key].beat / [position]
     */
    fun <T: EffectEvent> get_line_controller_event(type: EffectType, beat_key: BeatKey, position: List<Int>): T? {
        val controller = this.get_line_controller<T>(type, beat_key.channel, beat_key.line_offset)
        return controller.get_tree(beat_key.beat, position).get_event()
    }

    /**
     * Get the [type] Controller event for the [channel] at [beat]/[position]
     */
    fun <T: EffectEvent> get_channel_controller_event(type: EffectType, channel: Int, beat: Int, position: List<Int>): T? {
        val controller = this.get_channel_controller<T>(type, channel)
        return controller.get_tree(beat, position).get_event()
    }

    /**
     * Get the [type] controller for the project at [beat]/[position]
     */
    fun <T: EffectEvent> get_global_controller_event(type: EffectType, beat: Int, position: List<Int>): T? {
        val controller = this.get_global_controller<T>(type)
        return controller.get_tree(beat, position).get_event()
    }

    /**
     * Get the initial [type] controller event of the line at [channel], [line_offset]
     */
    fun <T : EffectEvent> get_line_controller_initial_event(type: EffectType, channel: Int, line_offset: Int, copy: Boolean = false): T {
        val output = this.get_line_controller<T>(type, channel, line_offset).initial_event
        return if (copy) {
            output.copy() as T
        } else {
            output
        }
    }

    /**
     * Get the initial [type] controller event of the channel at [channel]
     */
    fun <T : EffectEvent> get_channel_controller_initial_event(type: EffectType, channel: Int, copy: Boolean = false): T {
        val output = this.get_channel_controller<T>(type, channel).initial_event
        return if (copy) {
            output.copy() as T
        } else {
            output
        }
    }

    /**
     * Get the initial [type] controller event of the project
     */
    fun <T : EffectEvent> get_global_controller_initial_event(type: EffectType, copy: Boolean = false): T {
        val output = this.get_global_controller<T>(type).initial_event
        return if (copy) {
            output.copy() as T
        } else {
            output
        }
    }

    /**
     * Get the [type] controller event of the line at [channel], [line_offset] or the first preceding if it's null.
     */
    fun <T : EffectEvent> get_current_line_controller_event(type: EffectType, beat_key: BeatKey, position: List<Int>): T {
        val controller = this.get_channel(beat_key.channel).lines[beat_key.line_offset].get_controller<T>(type)

        val (real_beat, real_position) = controller.get_actual_position(beat_key.beat, position)
        controller.get_tree(real_beat, real_position).get_event()?.let {
            return it
        }

        return controller.coerce_latest_persistent_event(real_beat, real_position)
    }

    /**
     * Get the [type] Controller event for the [channel] at [beat]/[position] or the first preceding if it's null.
     */
    fun <T : EffectEvent> get_current_channel_controller_event(type: EffectType, channel: Int, beat: Int, position: List<Int>): T {
        val controller = this.get_channel(channel).get_controller<T>(type)
        controller.get_tree(beat, position).get_event()?.let {
            return it
        }

        return controller.coerce_latest_persistent_event(beat, position)
    }

    /**
     * Get the [type] controller for the project at [beat]/[position] or the first preceding if it's null
     */
    fun <T : EffectEvent> get_current_global_controller_event(type: EffectType, beat: Int, position: List<Int>): T {
        val controller = this.get_controller<T>(type)

        controller.get_tree(beat, position).get_event()?.let {
            return it
        }

        return controller.coerce_latest_persistent_event(beat, position)
    }

    /**
     * Get the value of the actual (or potential) event relative to the preceding event found at [beat_key]/[position]
     */
    fun get_relative_value(beat_key: BeatKey, position: List<Int>): Int {
        val tree = this.get_tree(beat_key, position)
        if (!tree.has_event()) throw NonEventConversion(beat_key, position)

        val event = tree.get_event()!!
        if (event is RelativeNoteEvent) return event.offset

        var working_beat_key: BeatKey = beat_key
        var working_position: List<Int> = position
        var preceding_value: Int? = null
        while (preceding_value == null) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            preceding_value = this.get_absolute_value(pair.first, pair.second)
            working_beat_key = pair.first
            working_position = pair.second
        }
        return (event as AbsoluteNoteEvent).note - (preceding_value ?: 0)
    }
    //// END RO Functions ////

    /*
     * ---------------------------- 1st Order Functions ---------------------------
     * These are the base write functions. All of them need to be implimented at higher Layers for the Opus Editor to work correctly
     */
    /**
     * Insert a new tree @ [beat_key]/[position]
     *
     * @throws BadInsertPosition When attempting to insert a new tree next to a top-level tree
     */
    open fun insert(beat_key: BeatKey, position: List<Int>) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].insert_tree(beat_key.line_offset, beat_key.beat, position)
        }
    }

    /**
     * Insert a new tree after [beat_key]/[position]
     *
     * @throws BadInsertPosition When attempting to insert a new tree next to a top-level tree
     */
    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].insert_after(beat_key.line_offset, beat_key.beat, position)
        }
    }

    /**
     * Insert a leaf into the tree of the channel [channel] [type] effect controller at [beat]/[position]
     */
    open fun controller_channel_insert(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_blocked_tree_exception(channel) {
            this.get_all_channels()[channel].controller_channel_insert_leaf(type, beat, position)
        }
    }

    /**
     * Insert a leaf into the tree of the channel [channel] [type] effect controller after [beat]/[position]
     */
    open fun controller_channel_insert_after(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_blocked_tree_exception(channel) {
            this.get_all_channels()[channel].controller_channel_insert_leaf_after(type, beat, position)
        }
    }

    /**
     * Replace the node of the channel [channel] [type] effect controller at [beat]/[position] with [tree]
     */
    open fun <T : EffectEvent> controller_channel_replace_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        this._catch_blocked_tree_exception(channel) {
            val tree_copy = tree.copy(this::copy_control_event)
            this.get_all_channels()[channel].replace_channel_control_leaf(type, beat, position ?: listOf(), tree_copy)
        }
    }

    /**
     * Set the node of the channel [channel] [type] effect controller at [beat]/[position] as event [event]
     */
    open fun <T : EffectEvent> controller_channel_set_event(type: EffectType, channel: Int, beat: Int, position: List<Int>, event: T) {
        this._catch_blocked_tree_exception(channel) {
            this.get_all_channels()[channel].set_controller_event(type, beat, position, event)
        }
    }

    /**
     * Split the node of the channel [channel] [type] effect controller at [beat]/[position] into [splits] splits
     */
    open fun controller_channel_split_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean = false) {
        this.get_all_channels()[channel].get_controller<EffectEvent>(type).split_tree(beat, position, splits, move_event_to_end)
    }

    /**
     * Remove all children and events of the channel [channel] [type] effect controller at [beat]/[position]
     */
    open fun controller_channel_unset(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.get_all_channels()[channel].get_controller<EffectEvent>(type).unset(beat, position)
    }

    /**
     * Replace the node of the line at [beat_key.channel], [beat_key.line_offset]'s [type] effect controller at [beat]/[position] with [tree]
     */
    open fun <T: EffectEvent> controller_line_replace_tree(type: EffectType, beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<T>) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            val tree_copy = tree.copy(this::copy_control_event)
            this.get_all_channels()[beat_key.channel].replace_line_control_leaf(type, beat_key.line_offset, beat_key.beat, position ?: listOf(), tree_copy)
        }
    }

    /**
     * Set the node of the line at [beat_key.channel], [beat_key.line_offset]'s [type] effect controller at [beat]/[position] as event [event]
     */
    open fun <T: EffectEvent> controller_line_set_event(type: EffectType, beat_key: BeatKey, position: List<Int>, event: T) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].set_line_controller_event(type, beat_key.line_offset, beat_key.beat, position, event)
        }
    }

    /**
     * Replace the node of the global [type] effect controller at [beat]/[position] with [tree]
     */
    open fun <T: EffectEvent> controller_global_replace_tree(type: EffectType, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        this._catch_global_ctl_blocked_tree_exception(type) {
            val tree_copy = tree.copy(this::copy_control_event)
            val controller = this.get_controller<T>(type)
            controller.replace_tree(
                beat,
                position ?: listOf(),
                tree_copy
            )
        }
    }

    /**
     * Set the node of the global [type] effect controller at [beat]/[position] as event [event]
     */
    open fun <T : EffectEvent> controller_global_set_event(type: EffectType, beat: Int, position: List<Int>, event: T) {
        this._catch_global_ctl_blocked_tree_exception(type) {
            this.get_controller<T>(type).set_event(beat, position, event)
        }
    }

    /**
     * Split the node of the line at [beat_key.channel], [beat_key.line_offset]'s [type] effect controller at [beat]/[position] into [splits] splits
     */
    open fun controller_line_split_tree(type: EffectType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean = false) {
        this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<EffectEvent>(type).split_tree(beat_key.beat, position, splits, move_event_to_end)
    }

    /**
     * Split the node of the global [type] effect controller at [beat]/[position] into [splits] splits
     */
    open fun controller_global_split_tree(type: EffectType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean = false) {
        this._catch_global_ctl_blocked_tree_exception(type) {
            this.get_controller<EffectEvent>(type).split_tree(beat, position, splits, move_event_to_end)
        }
    }

    /**
     * Remove all children and events of the line at [beat_key.channel], [beat_key.line_offset]'s [type] effect controller at [beat]/[position]
     */
    open fun controller_line_unset(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<EffectEvent>(type).unset(beat_key.beat, position)
    }

    /**
     * Remove all children and events of the global [type] effect controller at [beat]/[position]
     */
    open fun controller_global_unset(type: EffectType, beat: Int, position: List<Int>) {
        this.get_controller<EffectEvent>(type).unset(beat, position)
    }

    /**
     * Insert a leaf into the tree of the line at [beat_key.channel], [beat_key.line_offset]'s [type] effect controller at [beat]/[position]
     */
    open fun controller_line_insert(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].controller_line_insert_leaf(type, beat_key.line_offset, beat_key.beat, position)
        }
    }

    /**
     * Insert a leaf into the tree of the line at [beat_key.channel], [beat_key.line_offset]'s [type] effect controller after [beat]/[position]
     */
    open fun controller_line_insert_after(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].controller_line_insert_leaf_after(type, beat_key.line_offset, beat_key.beat, position)
        }
    }

    /**
     * Insert a leaf into the tree of the global [type] effect controller at [beat]/[position]
     */
    open fun controller_global_insert(type: EffectType, beat: Int, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_global_ctl_blocked_tree_exception(type) {
            val controller = this.get_controller<EffectEvent>(type)
            controller.insert(beat, position)
        }
    }

    /**
     * Insert a leaf into the tree of the global [type] effect controller after [beat]/[position]
     */
    open fun controller_global_insert_after(type: EffectType, beat: Int, position: List<Int>) {
        if (position.isEmpty()) throw BadInsertPosition()
        this._catch_global_ctl_blocked_tree_exception(type) {
            val controller = this.get_controller<EffectEvent>(type)
            controller.insert_after(beat, position)
        }
    }

    /**
     * Set a percussion event at [beat_key]/[position].
     * The [beat_key.channel] must be the percussion channel.
     */
    open fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_percussion_tree(beat_key, position)
        if (tree.has_event()) {
            tree.unset_event()
        }

        tree.set_event(PercussionEvent())
    }

    /**
     * Set the percussion instrument of the line at [line_offset] of the percussion channel to [instrument].
     */
    open fun percussion_set_instrument(channel: Int, line_offset: Int, instrument: Int) {
        if (!this.is_percussion(channel)) throw PercussionChannelRequired(channel)
        (this.get_channel(channel) as OpusPercussionChannel).set_instrument(line_offset, instrument)
    }

    /**
     * Set the bank of the channel at [channel] to [instrument.first] and the program to [instrument.second].
     */
    open fun channel_set_preset(channel: Int, instrument: Pair<Int, Int>) {
        if (!this.is_percussion(channel)) {
            this.set_channel_bank(channel, instrument.first)
        }
        this.set_channel_program(channel, instrument.second)
    }

    /**
     * Replace the node at [beat_key]/[position] with [tree]
     */
    open fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<out InstrumentEvent>) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            if (this.is_percussion(beat_key.channel)) {
                (this.get_channel(beat_key.channel) as OpusPercussionChannel).replace_tree(
                    beat_key.line_offset,
                    beat_key.beat,
                    position,
                    checked_cast<ReducibleTree<PercussionEvent>>(tree)
                )
            } else {
                (this.get_channel(beat_key.channel) as OpusChannel).replace_tree(
                    beat_key.line_offset,
                    beat_key.beat,
                    position,
                    checked_cast<ReducibleTree<TunedInstrumentEvent>>(tree)
                )
            }
        }
    }

    /**
     * Set the node at [beat_key]/[position] as the event [event]
     */
    open fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            if (this.is_percussion(beat_key.channel)) {
                (this.get_channel(beat_key.channel) as OpusPercussionChannel).set_event(beat_key.line_offset, beat_key.beat, position, event as PercussionEvent)
            } else {
                (this.get_channel(beat_key.channel) as OpusChannel).set_event(beat_key.line_offset, beat_key.beat, position, event as TunedInstrumentEvent)
            }
        }
    }

    /**
     * Split node at [beat_key]/[position] into [splits] branches.
     * If [move_event_to_end] is true and the target is an event, the event will be moved to the last leaf,
     * otherwise it will be the first leaf.
     */
    open fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean = false) {
        this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].split_tree(beat_key.beat, position, splits, move_event_to_end)
    }

    /**
     * Remove all children and events of the node at [beat_key]/[position]
     */
    open fun unset(beat_key: BeatKey, position: List<Int>) {
        this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].unset(beat_key.beat, position)
    }

    /**
     * Add a new empty channel at [channel].
     * Use [lines] to set the initial number of lines of the new channel,
     * [uuid] is the unique identifier used when rebuilding a channel
     */
    open fun new_channel(channel: Int? = null, lines: Int = 1, uuid: Int? = null, is_percussion: Boolean = false) {
        val actual_uuid = uuid ?: OpusLayerBase.gen_channel_uuid()
        val new_channel = if (is_percussion) {
            OpusPercussionChannel(actual_uuid)
        } else {
            OpusChannel(actual_uuid)
        }
        new_channel.set_beat_count(this.length)

        this._channel_uuid_map[new_channel.uuid] = new_channel
        for (i in 0 until lines) {
            new_channel.new_line(i)
        }

        val new_channel_index = channel ?: this.channels.size
        this.channels.add(new_channel_index, new_channel)

        this.recache_line_maps()
    }

    /**
     * Remove the channel at [channel]. Percussion can't be removed.
     */
    open fun remove_channel(channel: Int) {
        val opus_channel = this.channels.removeAt(channel)
        this._channel_uuid_map.remove(opus_channel.uuid)
        this.recache_line_maps()
    }

    /**
     * Move line at [line_offset_from] in [channel_index_from], to [line_offset_to] / [channel_index_to]
     */
    open fun move_line(channel_index_from: Int, line_offset_from: Int, channel_index_to: Int, line_offset_to: Int) {
        if (this.is_percussion(channel_index_from) != this.is_percussion(channel_index_to)) {
            throw IncompatibleChannelException(channel_index_from, channel_index_to)
        }
        this.new_line(channel_index_to, line_offset_to)
        var adj_line_offset_from = line_offset_from
        if (channel_index_to == channel_index_from && line_offset_to < line_offset_from) {
            adj_line_offset_from += 1
        }

        this.swap_lines(channel_index_from, adj_line_offset_from, channel_index_to, line_offset_to)
        if (this.get_channel(channel_index_from).lines.size > 1) {
            this.remove_line(channel_index_from, adj_line_offset_from)
        } else {
            this.remove_channel(channel_index_from)
        }
    }
    /**
     * Swap line [line_offset_a] of channel [channel_index_a] with line [line_offset_b] of channel [channel_index_b].
     */
    open fun swap_lines(channel_index_a: Int, line_offset_a: Int, channel_index_b: Int, line_offset_b: Int) {
        if (this.is_percussion(channel_index_a) != this.is_percussion(channel_index_b)) {
            throw IncompatibleChannelException(channel_index_a, channel_index_b)
        }

        if (this.is_percussion(channel_index_a)) {
            val channel_a = this.get_channel(channel_index_a) as OpusPercussionChannel
            val channel_b = this.get_channel(channel_index_b) as OpusPercussionChannel
            val tmp_line = channel_a.lines[line_offset_a]
            channel_a.lines[line_offset_a] = channel_b.lines[line_offset_b]
            channel_b.lines[line_offset_b] = tmp_line
        } else {
            val channel_a = this.get_channel(channel_index_a) as OpusChannel
            val channel_b = this.get_channel(channel_index_b) as OpusChannel
            val tmp_line = channel_a.lines[line_offset_a]
            channel_a.lines[line_offset_a] = channel_b.lines[line_offset_b]
            channel_b.lines[line_offset_b] = tmp_line
        }

        this.recache_line_maps()
    }

    /**
     * Insert a beat at [beat_index] into all existing controllers, channels and lines.
     * populate the new beat with [beats_in_column]
     */
    open fun insert_beat(beat_index: Int, beats_in_column: List<ReducibleTree<OpusEvent>>? = null) {
        if (beat_index > this.length) throw IndexOutOfBoundsException()

        this.length += 1
        for (channel in this.channels) {
            channel.insert_beat(beat_index)
        }

        this.controllers.insert_beat(beat_index)

        if (beats_in_column != null) {
            this._apply_column_trees(beat_index, beats_in_column)
        }

        val tag_beats = this.marked_sections.keys.sorted().toMutableList()
        tag_beats.reverse()
        for (tag in tag_beats) {
            if (tag >= beat_index) {
                this.marked_sections[tag + 1] = this.marked_sections.remove(tag)
            }
        }
    }

    /**
     * Add a line [line] into channel [channel] at [line_offset].
     */
    open fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        val is_percussion = this.is_percussion(channel)
        when (line) {
            is OpusLine -> {
                if (is_percussion) throw InvalidLineException()
                (this.get_channel(channel) as OpusChannel).insert_line(line_offset, line)
            }
            is OpusLinePercussion -> {
                if (!is_percussion) throw InvalidPercussionLineException()
                (this.get_channel(channel) as OpusPercussionChannel).insert_line(line_offset, line)
            }
            else -> throw UnhandledLineType(line)
        }
        this.recache_line_maps()
    }

    /**
     * Create a new, empty line in channel [channel] at [line_offset] or the end of the channel if [line_offset] is null
     */
    open fun new_line(channel: Int, line_offset: Int? = null) {
        val working_channel = this.get_channel(channel)
        working_channel.new_line(line_offset ?: working_channel.lines.size)
        this.recache_line_maps()
    }

    /**
     * remove the beat at [beat_index] [count] times from all controllers, lines and channels.
     */
    open fun remove_beat(beat_index: Int, count: Int = 1) {
        if (this.length <= count) throw RemovingLastBeatException()
        if (beat_index >= this.length) throw IndexOutOfBoundsException()

        this.blocked_check_remove_beat(beat_index, count)

        val working_beat_index = min(beat_index + count - 1, this.length - 1) - (count - 1)
        if (working_beat_index < 0) throw IndexOutOfBoundsException()

        for (channel in this.get_all_channels()) {
            channel.remove_beat(working_beat_index, count)
        }

        for ((_, controller) in this.controllers.get_all()) {
            controller.remove_beat(working_beat_index, count)
        }

        val tag_beats = this.marked_sections.keys.sorted()
        for (tag in tag_beats) {
            if (tag >= beat_index + count) {
                this.marked_sections[tag - count] = this.marked_sections.remove(tag)
            } else if (tag >= beat_index && tag < beat_index + count) {
                this.remove_tagged_section(tag)
            }
        }

        this.length -= count
    }

    /**
     * Remove a line and its effect controllers.
     */
    open fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        val output = this.get_channel(channel).remove_line(line_offset)
        this.recache_line_maps()
        return output
    }

    /**
     * Set the project name to [new_name].
     */
    open fun set_project_name(new_name: String?) {
        this.project_name = new_name
    }

    /**
     * Modify the project notes
     */
    open fun set_project_notes(notes: String?) {
        this.project_notes = notes
    }

    /**
     * Set the initial transposition value of the project.
     * [new_transpose.second] is the notes per octave.
     * [new_transpose.first] is the number of steps up in the octave.
     */
    open fun set_transpose(new_transpose: Pair<Int, Int>) {
        this.transpose = new_transpose
    }

    /**
     * Apply a new tuning map [new_map].
     * f [mod_events] is true, then update all existing events to their closest counterpart in the new tuning scheme.
     */
    open fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean = true) {
        val previous_radix = this.tuning_map.size

        val original = this.tuning_map
        this.tuning_map = new_map.clone()

        if (!mod_events || new_map.size == original.size) return

        val radix = new_map.size

        for (i in this.channels.indices) {
            if (this.is_percussion(i)) continue
            (this.get_channel(i) as OpusChannel).lines.forEachIndexed { j: Int, line: OpusLine ->
                line.beats.forEachIndexed { k: Int, beat_tree: ReducibleTree<TunedInstrumentEvent> ->
                    beat_tree.traverse { tree: ReducibleTree<TunedInstrumentEvent>, event: TunedInstrumentEvent? ->
                        if (event == null) return@traverse

                        val position = tree.get_path()
                        val new_event = when (event) {
                            is AbsoluteNoteEvent -> {
                                val new_event = event.copy()
                                new_event.note = event.note * radix / previous_radix
                                new_event

                            }

                            is RelativeNoteEvent -> {
                                val new_event = event.copy()
                                new_event.offset = event.offset * radix / previous_radix
                                new_event
                            }

                            else -> return@traverse
                        }


                        this.set_event(
                            BeatKey(i, j, k),
                            position,
                            new_event
                        )
                    }
                }
            }
        }
    }

    open fun set_line_color(channel: Int, line_offset: Int, color: Int?) {
        this.get_all_channels()[channel].set_line_color(line_offset, color)
    }

    /**
     * Remove the [type] effect controller of the [line_offset]th line of the [channel_index]th channel.
     */
    open fun remove_line_controller(type: EffectType, channel_index: Int, line_offset: Int) {
        val channel = this.get_all_channels()[channel_index]
        val line = channel.lines[line_offset]
        line.controllers.remove_controller(type)
        this.recache_line_maps()
    }

    /**
     * Add a [type] effect controller for the [line_offset]th line of the [channel_index]th channel.
     */
    open fun new_line_controller(type: EffectType, channel_index: Int, line_offset: Int) {
        val channel = this.get_all_channels()[channel_index]
        val line = channel.lines[line_offset]
        line.get_controller<EffectEvent>(type)
        this.recache_line_maps()
    }

    /**
     * Show or hide the [type] effect controller for the [line_offset]th line of the [channel_index]th channel.
     */
    open fun set_line_controller_visibility(type: EffectType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        val channel = this.get_all_channels()[channel_index]
        val line = channel.lines[line_offset]
        val controller = line.get_controller<EffectEvent>(type)
        controller.visible = visibility
        this.recache_line_maps()
    }

    open fun move_channel(channel_index: Int, new_channel_index: Int) {
        val channel = this.channels.removeAt(channel_index)
        val adj_new_channel_index = if (channel_index < new_channel_index) {
            new_channel_index - 1
        } else {
            new_channel_index
        }
        this.channels.add(adj_new_channel_index, channel)
        this.recache_line_maps()
    }

    /**
     * Remove the [type] effect controller for the [channel_index]th channel.
     */
    open fun remove_channel_controller(type: EffectType, channel_index: Int) {
        val channel = this.get_all_channels()[channel_index]
        channel.controllers.remove_controller(type)
        this.recache_line_maps()
    }

    /**
     * Add a [type] effect controller for the channel at [channel_index].
     */
    open fun new_channel_controller(type: EffectType, channel_index: Int) {
        val channel = this.get_all_channels()[channel_index]
        channel.get_controller<EffectEvent>(type)
        this.recache_line_maps()
    }

    /**
     * Show or hide the [type] effect controller for the channel at [channel_index].
     */
    open fun set_channel_controller_visibility(type: EffectType, channel_index: Int, visibility: Boolean) {
        val channel = this.get_all_channels()[channel_index]
        val controller = channel.get_controller<EffectEvent>(type)
        controller.visible = visibility
        this.recache_line_maps()
    }

    /**
     * Remove the global [type] effect controller.
     */
    open fun remove_global_controller(type: EffectType) {
        this.controllers.remove_controller(type)
        this.recache_line_maps()
    }

    /**
     * Add a global [type] effect controller.
     */
    open fun new_global_controller(type: EffectType) {
        this.controllers.new_controller(type)
        this.get_controller<EffectEvent>(type)
        this.recache_line_maps()
    }

    /**
     * Show/Hide the global [type] effect controller.
     */
    open fun set_global_controller_visibility(type: EffectType, visibility: Boolean) {
        val controller = this.get_controller<EffectEvent>(type)
        controller.visible = visibility
        this.recache_line_maps()
    }

    /**
     * Mute Playback of channel at [channel]
     */
    open fun mute_channel(channel: Int) {
        this.get_all_channels()[channel].mute()
    }

    /**
     * Unmute Playback of channel at [channel]
     */
    open fun unmute_channel(channel: Int) {
        this.get_all_channels()[channel].unmute()
    }

    /**
     * Mute Playback of line at [channel], [line_offset]
     */
    open fun mute_line(channel: Int, line_offset: Int) {
        this.get_all_channels()[channel].get_line(line_offset).mute()
    }

    /**
     * Unmute Playback of line at [channel], [line_offset]
     */
    open fun unmute_line(channel: Int, line_offset: Int) {
        this.get_all_channels()[channel].get_line(line_offset).unmute()
    }

    /*
     * ---------------------------- 2nd Order Functions ---------------------------
     * Convenience functions. These are functions to call 1st order functions (or other 2nd order functions). This could be
     * in the case of needing to call them multiple times or needing to use the 1st order functions to
     * achieve an ostensibly different function (eg, remove_one_of_two is actually a replace_tree where the parent is replaced by one of the children)
     * Creating repeater functions here also allows the history layer to group actions together more easily.
     */

    open fun offset_range(amount: Int, first_key: BeatKey, second_key: BeatKey) {
        for (beat_key in this.get_beatkeys_in_range(first_key, second_key)) {
            this.get_tree(beat_key).traverse { subtree: ReducibleTree<out InstrumentEvent>, event: OpusEvent? ->
                if (event != null && event is AbsoluteNoteEvent) {
                    val new_event = event.copy()
                    new_event.note += amount
                    this.set_event(beat_key, subtree.get_path(), new_event)
                }
            }
        }
    }

    open fun controller_channel_remove_one_of_two(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        val to_replace_position = List(position.size) { i: Int ->
            if (i < position.size - 1) {
                position[i]
            } else if (position.last() == 0) {
                1
            } else {
                0
            }
        }

        val replacer_tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat, to_replace_position)
        this.controller_channel_replace_tree(type, channel, beat, position.subList(0, position.size - 1), replacer_tree)
    }

    open fun controller_channel_remove_standard(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this._catch_blocked_tree_exception(channel) {
            this.get_all_channels()[channel].controller_channel_remove_leaf(type, beat, position)
        }
    }

    fun controller_channel_remove_line(type: EffectType, channel: Int) {
        this.get_all_channels()[channel].controllers.remove_controller(type)
        this.recache_line_maps()
    }

    open fun controller_channel_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        val from_tree = this.get_channel_ctl_tree<EffectEvent>(type, channel_from, beat_from, position_from).copy(this::copy_control_event)
        this.controller_channel_unset(type, channel_from, beat_from, position_from)
        this.controller_channel_replace_tree(type, channel_to, beat_to, position_to, from_tree)
    }

    open fun controller_global_to_channel_move_leaf(type: EffectType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        val from_tree = this.get_global_ctl_tree<EffectEvent>(type, beat_from, position_from).copy(this::copy_control_event)
        this.controller_global_unset(type, beat_from, position_from)
        this.controller_global_replace_tree(type, beat_to, position_to, from_tree)
    }

    open fun controller_channel_to_line_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_channel_ctl_tree<EffectEvent>(type, channel_from, beat_from, position_from).copy(this::copy_control_event)
        this.controller_channel_unset(type, channel_from, beat_from, position_from)
        this.controller_line_replace_tree(type, beat_key_to, position_to, from_tree)
    }

    open fun controller_global_to_line_move_leaf(type: EffectType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        val from_tree = this.get_global_ctl_tree<EffectEvent>(type, beat, position).copy(this::copy_control_event)
        this.controller_global_unset(type, beat, position)
        this.controller_line_replace_tree(type, target_key, target_position, from_tree)
    }

    open fun controller_channel_unset_line(type: EffectType, channel: Int) {
        val controller = this.get_all_channels()[channel].get_controller<EffectEvent>(type)
        for (beat in 0 until this.length) {
            val line_ctl_tree = controller.get_tree(beat)
            if (line_ctl_tree.is_eventless()) {
                continue
            }
            this.controller_channel_unset(type, channel, beat, listOf())
        }
    }

    open fun insert_after_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int = 1) {
        for (i in 0 until repeat) {
            this.insert_after(beat_key, position)
        }
    }

    open fun insert_beats(beat_index: Int, count: Int) {
        for (i in 0 until count) {
            this.insert_beat(beat_index)
        }
    }

    open fun insert_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int = 1) {
        for (i in 0 until repeat) {
            this.insert(beat_key, position)
        }
    }

    /*
        remove_one_of_two and remove_standard all exist so I could separate
        them and use the "forget" wrapper at the History layer
     */
    open fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        val to_replace_position = List(position.size) { i: Int ->
            if (i < position.size - 1) {
                position[i]
            } else if (position.last() == 0) {
                1
            } else {
                0
            }
        }

        val replacer_tree = this.get_tree(beat_key, to_replace_position)
        this.replace_tree(
            beat_key,
            position.subList(0, position.size - 1),
            replacer_tree
        )
    }

    open fun controller_line_remove_one_of_two(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        val to_replace_position = List(position.size) { i: Int ->
            if (i < position.size - 1) {
                position[i]
            } else if (position.last() == 0) {
                1
            } else {
                0
            }
        }

        val replacer_tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key, to_replace_position)
        this.controller_line_replace_tree(type, beat_key, position.subList(0, position.size - 1), replacer_tree)
    }

    open fun controller_global_remove_one_of_two(type: EffectType, beat: Int, position: List<Int>) {
        val to_replace_position = List(position.size) { i: Int ->
            if (i < position.size - 1) {
                position[i]
            } else if (position.last() == 0) {
                1
            } else {
                0
            }
        }

        val replacer_tree = this.get_global_ctl_tree<EffectEvent>(type, beat, to_replace_position)
        this.controller_global_replace_tree(type, beat, position.subList(0, position.size - 1), replacer_tree)
    }

    open fun remove_line_repeat(channel: Int, line_offset: Int, count: Int) {
        for (i in 0 until count) {
            val working_channel = this.get_channel(channel)
            if (working_channel.size == 0) {
                break
            }
            try {
                this.remove_line(
                    channel,
                    min(line_offset, working_channel.size - 1)
                )
            } catch (_: OpusChannelAbstract.LastLineException) {
                break
            }
        }
    }

    open fun new_line_repeat(channel: Int, line_offset: Int, count: Int) {
        for (i in 0 until count) {
            this.new_line(channel, line_offset)
        }
    }

    open fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_tree_copy(beatkey_from, position_from)
        this.unset(beatkey_from, position_from)
        this.replace_tree(beatkey_to, position_to, from_tree)
    }

    open fun controller_global_move_leaf(type: EffectType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        val from_tree = this.get_global_ctl_tree<EffectEvent>(type, beat_from, position_from).copy(this::copy_control_event)
        this.controller_global_unset(type, beat_from, position_from)
        this.controller_global_replace_tree(type, beat_to, position_to, from_tree)
    }

    open fun controller_line_to_global_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        val from_tree = this.get_line_ctl_tree_copy<EffectEvent>(type, beatkey_from, position_from)
        this.controller_line_unset(type, beatkey_from, position_from)
        this.controller_global_replace_tree(type, target_beat, target_position, from_tree)
    }

    open fun controller_channel_to_global_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        val from_tree = this.get_channel_ctl_tree_copy<EffectEvent>(type, channel_from, beat_from, position_from)
        this.controller_channel_unset(type, channel_from, beat_from, position_from)
        this.controller_global_replace_tree(type, target_beat, target_position, from_tree)
    }

    open fun controller_line_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_line_ctl_tree<EffectEvent>(type, beatkey_from, position_from).copy(this::copy_control_event)
        this.controller_line_unset(type, beatkey_from, position_from)
        this.controller_line_replace_tree(type, beatkey_to, position_to, from_tree)
    }

    open fun controller_line_to_channel_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        val from_tree = this.get_line_ctl_tree<EffectEvent>(type, beatkey_from, position_from).copy(this::copy_control_event)
        this.controller_line_unset(type, beatkey_from, position_from)
        this.controller_channel_replace_tree(type, channel_to, beat_to, position_to, from_tree)
    }

    open fun _controller_global_copy_range(type: EffectType, target: Int, point_a: Int, point_b: Int, unset_original: Boolean = false) {
        val start = min(point_a, point_b)
        val end = max(point_a, point_b)

        // Expand the song duration if necessary
        val max_beat = (end - start) + target
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val controller = this.get_controller<EffectEvent>(type)

        for (i in start .. end) {
            overwrite_map[target + (i - start)] = controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_global_unset(type, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.controller_global_replace_tree(type, beat, null, tree)
        }
    }

    fun controller_global_overwrite_range(type: EffectType, target: Int, start: Int, end: Int) {
        this._controller_global_copy_range(type, target, start, end, false)
    }

    fun controller_global_move_range(type: EffectType, target: Int, start: Int, end: Int) {
        this._controller_global_copy_range(type, target, start, end, true)
    }

    open fun _controller_channel_to_global_copy_range(type: EffectType, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        val start = min(point_a, point_b)
        val end = max(point_a, point_b)
        while (this.length <= target_beat + (end - start)) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_all_channels()[original_channel].get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_channel_unset(type, original_channel, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.controller_global_replace_tree(type, beat, null, tree)
        }
    }

    fun controller_channel_to_global_overwrite_range(type: EffectType, target_beat: Int, original_channel: Int, start: Int, end: Int) {
        this._controller_channel_to_global_copy_range(type, target_beat, original_channel, start, end, false)
    }

    fun controller_channel_to_global_move_range(type: EffectType, target_beat: Int, original_channel: Int, start: Int, end: Int) {
        this._controller_channel_to_global_copy_range(type, target_beat, original_channel, start, end, true)
    }

    open fun _controller_channel_copy_range(type: EffectType, target_channel: Int, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        val start = min(point_a, point_b)
        val end = max(point_a, point_b)

        // Expand the song duration if necessary
        val max_beat = (end - start) + target_beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_all_channels()[original_channel].get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_channel_unset(type, original_channel, i, listOf())
            }
            if (target_channel != original_channel || (i - start) + target_beat != start) {
                this.controller_channel_unset(type, target_channel, (i - start) + target_beat, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.controller_channel_replace_tree(type, target_channel, beat, null, tree)
        }
    }

    fun controller_channel_overwrite_range(type: EffectType, target_channel: Int, target_beat: Int, original_channel: Int, start: Int, end: Int) {
        this._controller_channel_copy_range(type, target_channel, target_beat, original_channel, start, end, false)
    }

    fun controller_channel_move_range(type: EffectType, target_channel: Int, target_beat: Int, original_channel: Int, start: Int, end: Int) {
        this._controller_channel_copy_range(type, target_channel, target_beat, original_channel, start, end, true)
    }

    open fun _controller_line_copy_range(type: EffectType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        // Expand the song duration if necessary
        val first_beat = min(second_corner.beat, first_corner.beat)
        val second_beat = max(second_corner.beat, first_corner.beat)
        val max_beat = (second_beat - first_beat) + beat_key.beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_corner, second_corner)

        val original_keys = this.get_beatkeys_in_range(from_key, to_key)
        val target_keys = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        // First, get the trees to copy. This prevents errors if the beat_key is within the two corner range
        val trees = mutableListOf<ReducibleTree<EffectEvent>>()
        for (o_key in original_keys) {
            trees.add(this.get_line_ctl_tree<EffectEvent>(type, o_key).copy(this::copy_control_event))
        }

        for (i in target_keys.indices) {
            this.controller_line_replace_tree(
                type,
                target_keys[i],
                null,
                trees[i]
            )
        }

        if (unset_original) {
            for (clear_key in (original_keys - target_keys.toSet())) {
                this.controller_line_unset(type, clear_key, listOf())
            }
        }
    }

    fun controller_line_overwrite_range(type: EffectType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._controller_line_copy_range(type, beat_key, first_corner, second_corner, false)
    }

    fun controller_line_move_range(type: EffectType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._controller_line_copy_range(type, beat_key, first_corner, second_corner, true)
    }

    open fun _controller_channel_to_line_copy_range(type: EffectType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        val start = min(beat_a, beat_b)
        val end = max(beat_a, beat_b)

        val max_beat = (end - start) + target_key.beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_all_channels()[channel_from].get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_key.beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_channel_unset(type, channel_from, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            val working_key = BeatKey(target_key.channel, target_key.line_offset, beat)
            this.controller_line_replace_tree(type, working_key, null, tree)
        }
    }

    fun controller_channel_to_line_move_range(type: EffectType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey) {
        this._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, true)
    }

    fun controller_channel_to_line_overwrite_range(type: EffectType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey) {
        this._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, false)
    }

    open fun _controller_global_to_line_copy_range(type: EffectType, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        val start = min(beat_a, beat_b)
        val end = max(beat_a, beat_b)
        val max_beat = (end - start) + target_key.beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_key.beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_global_unset(type, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            val working_key = BeatKey(target_key.channel, target_key.line_offset, beat)
            this.controller_line_replace_tree(type, working_key, null, tree)
        }
    }

    fun controller_global_to_line_move_range(type: EffectType, beat_a: Int, beat_b: Int, target_key: BeatKey) {
        this._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, true)
    }

    fun controller_global_to_line_overwrite_range(type: EffectType, beat_a: Int, beat_b: Int, target_key: BeatKey) {
        this._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, false)
    }

    open fun _controller_line_to_channel_copy_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int, unset_original: Boolean) {
        val start = min(beat_a, beat_b)
        val end = max(beat_a, beat_b)

        val max_beat = (end - start) + target_beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_all_channels()[from_channel].lines[from_line_offset].get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_line_unset(type, BeatKey(from_channel, from_line_offset, i), listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.controller_channel_replace_tree(type, target_channel, beat, null, tree)
        }
    }

    fun controller_line_to_channel_overwrite_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int) {
        this._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, false)
    }

    fun controller_line_to_channel_move_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int) {
        this._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, true)
    }

    open fun _controller_line_to_global_copy_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int, unset_original: Boolean) {
        val start = min(beat_a, beat_b)
        val end = max(beat_a, beat_b)

        val max_beat = (end - start) + target_beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_all_channels()[from_channel].lines[from_line_offset].get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_line_unset(type, BeatKey(from_channel, from_line_offset, i), listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.controller_global_replace_tree(type, beat, null, tree)
        }
    }

    fun controller_line_to_global_overwrite_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int) {
        this._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, false)
    }

    fun controller_line_to_global_move_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int) {
        this._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, true)
    }

    open fun _controller_global_to_channel_copy_range(type: EffectType, target_channel: Int, target_beat: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        val start = min(point_a, point_b)
        val end = max(point_a, point_b)
        val max_beat = (end - start) + target_beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val overwrite_map = HashMap<Int, ReducibleTree<EffectEvent>>()
        val original_controller = this.get_controller<EffectEvent>(type)
        for (i in start .. end) {
            overwrite_map[target_beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.controller_global_unset(type, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.controller_channel_replace_tree(type, target_channel, beat, null, tree)
        }
    }

    fun controller_global_to_channel_overwrite_range(type: EffectType, target_beat: Int, target_channel: Int, start: Int, end: Int) {
        this._controller_global_to_channel_copy_range(type, target_beat, target_channel, start, end, false)
    }

    fun controller_global_to_channel_move_range(type: EffectType, target_beat: Int, target_channel: Int, start: Int, end: Int) {
        this._controller_global_to_channel_copy_range(type, target_beat, target_channel, start, end, true)
    }

    open fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        // Expand the song duration if necessary
        val first_beat = min(first_corner.beat, second_corner.beat)
        val second_beat = max(first_corner.beat, second_corner.beat)
        val max_beat = (second_beat - first_beat) + beat_key.beat
        while (max_beat >= this.length) {
            this.insert_beat(this.length)
        }

        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_corner, second_corner)
        val original_keys = this.get_beatkeys_in_range(from_key, to_key)


        val target_keys = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        for (i in original_keys.indices) {
            if (this.is_percussion(original_keys[i].channel) != this.is_percussion(target_keys[i].channel)) {
                throw MixedInstrumentException(original_keys[i], target_keys[i])
            }
        }

        // First, get the trees to copy. This prevents errors if the beat_key is within the two corner range
        val trees = mutableListOf<Pair<ReducibleTree<out InstrumentEvent>, HashMap<EffectType, ReducibleTree<EffectEvent>>>>()
        for (o_key in original_keys) {
            val control_tree_map = HashMap<EffectType, ReducibleTree<EffectEvent>>()
            for ((type, _) in this.get_all_channels()[o_key.channel].lines[o_key.line_offset].controllers.get_all()) {
                control_tree_map[type] = this.get_line_ctl_tree_copy(type, o_key, listOf())
            }

            trees.add(
                Pair(
                    this.get_tree_copy(o_key),
                    control_tree_map
                )
            )

        }

        for (i in trees.indices) {
            this.unset(target_keys[i], listOf())
            val clear_key = target_keys[i]
            for ((type, _) in this.get_all_channels()[clear_key.channel].lines[clear_key.line_offset].controllers.get_all()) {
                this.controller_line_unset(type, clear_key, listOf())
            }
        }

        for (i in trees.indices) {
            this.replace_tree(
                target_keys[i],
                null,
                trees[i].first
            )

            for ((type, ctl_tree) in trees[i].second) {
                if (!this.has_line_controller(type, target_keys[i].channel, target_keys[i].line_offset)) {
                    this.new_line_controller(type, target_keys[i].channel, target_keys[i].line_offset)
                }
                this.controller_line_replace_tree(
                    type,
                    target_keys[i],
                    listOf(),
                    ctl_tree
                )
            }
        }
    }

    open fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        // Expand the song duration if necessary
        val first_beat = min(first_corner.beat, second_corner.beat)
        val second_beat = max(first_corner.beat, second_corner.beat)
        val max_beat = (second_beat - first_beat) + 1 + beat_key.beat
        if (max_beat > this.length) {
            this.insert_beats(this.length, max_beat - this.length)
        }

        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_corner, second_corner)

        val original_keys = this.get_beatkeys_in_range(from_key, to_key)
        val target_keys = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        for (i in original_keys.indices) {
            if (this.is_percussion(original_keys[i].channel) != this.is_percussion(target_keys[i].channel)) {
                throw MixedInstrumentException(original_keys[i], target_keys[i])
            }
        }

        // First, get the trees to copy. This prevents errors if the beat_key is within the two corner range
        val trees = mutableListOf<Pair<ReducibleTree<out InstrumentEvent>, HashMap<EffectType, ReducibleTree<EffectEvent>>>>()
        for (o_key in original_keys) {
            val control_tree_map = HashMap<EffectType, ReducibleTree<EffectEvent>>()
            for ((type, _) in this.get_all_channels()[o_key.channel].lines[o_key.line_offset].controllers.get_all()) {
                control_tree_map[type] = this.get_line_ctl_tree_copy(type, o_key, listOf())
            }

            trees.add(
                Pair(
                    this.get_tree_copy(o_key),
                    control_tree_map
                )
            )

        }

        for (clear_key in original_keys) {
            this.unset(clear_key, listOf())
            for ((type, _) in this.get_all_channels()[clear_key.channel].lines[clear_key.line_offset].controllers.get_all()) {
                this.controller_line_unset(type, clear_key, listOf())
            }
        }

        for (clear_key in target_keys) {
            this.unset(clear_key, listOf())
            for ((type, _) in this.get_all_channels()[clear_key.channel].lines[clear_key.line_offset].controllers.get_all()) {
                this.controller_line_unset(type, clear_key, listOf())
            }
        }

        for (i in target_keys.indices) {
            this.replace_tree(target_keys[i], null, trees[i].first)
            for ((type, ctl_tree) in trees[i].second) {
                if (!this.has_line_controller(type, target_keys[i].channel, target_keys[i].line_offset)) {
                    this.new_line_controller(type, target_keys[i].channel, target_keys[i].line_offset)
                }
                this.controller_line_replace_tree(
                    type,
                    target_keys[i],
                    listOf(),
                    ctl_tree
                )
            }
        }
    }

    open fun unset_line(channel: Int, line_offset: Int) {
        val line = this.get_all_channels()[channel].lines[line_offset]
        for (beat in 0 until this.length) {
            val tree = line.get_tree(beat)
            if (tree.is_eventless()) {
                continue
            }
            this.unset(BeatKey(channel, line_offset, beat), listOf())
        }
    }

    open fun controller_line_unset_line(type: EffectType, channel: Int, line_offset: Int) {
        val controller = this.get_all_channels()[channel].lines[line_offset].get_controller<EffectEvent>(type)
        for (beat in 0 until this.length) {
            val line_ctl_tree = controller.get_tree(beat)
            if (line_ctl_tree.is_eventless()) {
                continue
            }
            this.controller_line_unset(type, BeatKey(channel, line_offset, beat), listOf())
        }
    }

    open fun controller_global_unset_line(type: EffectType) {
        val controller = this.get_controller<EffectEvent>(type)
        for (beat in 0 until this.length) {
            val line_ctl_tree = controller.get_tree(beat)
            if (line_ctl_tree.is_eventless()) {
                continue
            }
            this.controller_global_unset(type, beat, listOf())
        }
    }

    open fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        for (selected_key in this.get_beatkeys_in_range(first_corner, second_corner)) {
            this.unset(selected_key, listOf())
        }
    }

    open fun controller_global_unset_range(type: EffectType, first_beat: Int, second_beat: Int) {
        for (i in first_beat .. second_beat) {
            this.controller_global_unset(type, i, listOf())
        }
    }

    open fun controller_channel_unset_range(type: EffectType, channel: Int, first_beat: Int, second_beat: Int) {
        for (i in first_beat .. second_beat) {
            this.controller_channel_unset(type, channel, i, listOf())
        }
    }

    open fun controller_line_unset_range(type: EffectType, first_corner: BeatKey, second_corner: BeatKey) {
        for (selected_key in this.get_beatkeys_in_range(first_corner, second_corner)) {
            this.controller_line_unset(type, selected_key, listOf())
        }
    }

    open fun unset_beat(beat: Int) {
        /* Clear STD channels ----------------------------------*/
        for (i in 0 until this.channels.size) {
            val channel = this.channels[i]
            for (j in 0 until channel.lines.size) {
                val line = channel.lines[j]
                this.unset(BeatKey(i, j, beat), listOf())
                for ((type, _) in line.controllers.get_all()) {
                    this.controller_line_unset(type, BeatKey(i, j, beat), listOf())
                }

            }
            for ((type, _) in channel.controllers.get_all()) {
                this.controller_channel_unset(type, i, beat, listOf())
            }
        }
        /*-------------------------------------------------------*/

        for ((type, _) in this.controllers.get_all()) {
            this.controller_global_unset(type, beat, listOf())
        }
    }

    open fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            val tree = this.get_tree(beat_key, position)
            if (!tree.has_event()) {
                throw EventlessTreeException()
            }
            val new_event = tree.event!!.copy()
            new_event.duration = duration
            this.set_event(beat_key, position, new_event)
        }
    }

    /**
     * Remove tree @ [beat_key]/[position] if it's not a top-level tree
     */
    open fun remove(beat_key: BeatKey, position: List<Int>) {
        // Can't remove beat
        if (position.isEmpty()) throw RemovingRootException()

        val tree = this.get_tree(beat_key, position)
        val parent_tree = tree.parent!!
        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            1 -> throw TrivialBranchException(beat_key, position)
            2 -> this.remove_one_of_two(beat_key, position)
            else -> this.remove_standard(beat_key, position)
        }
    }

    open fun controller_line_remove(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        // Can't remove beat
        if (position.isEmpty()) {
            throw RemovingRootException()
        }

        val tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key, position)


        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            2 -> this.controller_line_remove_one_of_two(type, beat_key, position)
            else -> this.controller_line_remove_standard(type, beat_key, position)
        }
    }

    open fun controller_channel_remove(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw RemovingRootException()
        }
        val tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat, position)
        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            2 -> this.controller_channel_remove_one_of_two(type, channel, beat, position)
            else -> this.controller_channel_remove_standard(type, channel, beat, position)
        }
    }

    open fun controller_global_remove(type: EffectType, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw RemovingRootException()
        }

        val tree = this.get_global_ctl_tree<EffectEvent>(type, beat, position)

        val parent_tree = tree.parent!!
        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            2 -> this.controller_global_remove_one_of_two(type, beat, position)
            else -> this.controller_global_remove_standard(type, beat, position)
        }
    }

    open fun remove_repeat(beat_key: BeatKey, position: List<Int>, count: Int = 1) {
        val adj_position = position.toMutableList()
        for (i in 0 until count) {
            val tree = this.get_tree(beat_key, adj_position)
            val parent_size = tree.parent?.size ?: 0
            this.remove(beat_key, adj_position)

            if (parent_size <= 2) { // Will be pruned
                adj_position.removeAt(adj_position.size - 1)
            } else if (adj_position.last() == parent_size - 1) {
                adj_position[adj_position.size - 1] -= 1
            }
        }
    }

    open fun repeat_controller_line_remove(type: EffectType, beat_key: BeatKey, position: List<Int>, count: Int) {
        val adj_position = position.toMutableList()
        for (i in 0 until count) {
            val tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key, adj_position)
            val parent_size = tree.parent?.size ?: 0

            this.controller_line_remove(type, beat_key, adj_position)
            if (parent_size <= 2) { // Will be pruned
                adj_position.removeAt(adj_position.size - 1)
            } else if (adj_position.last() == parent_size - 1) {
                adj_position[adj_position.size - 1] -= 1
            }
        }
    }

    open fun repeat_controller_channel_remove(type: EffectType, channel: Int, beat: Int, position: List<Int>, repeat: Int = 1) {
        val adj_position = position.toMutableList()
        for (i in 0 until repeat) {
            val tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat, adj_position)
            val parent_size = tree.parent?.size ?: 0

            this.controller_channel_remove(type, channel, beat, adj_position)

            if (parent_size <= 2) { // Will be pruned
                adj_position.removeAt(adj_position.size - 1)
            } else if (adj_position.last() == parent_size - 1) {
                adj_position[adj_position.size - 1] -= 1
            }
        }
    }

    open fun repeat_controller_global_remove(type: EffectType, beat: Int, position: List<Int>, count: Int) {
        val adj_position = position.toMutableList()
        for (i in 0 until count) {
            val tree = this.get_global_ctl_tree<EffectEvent>(type, beat, adj_position)
            val parent_size = tree.parent?.size ?: 0

            this.controller_global_remove(type, beat, adj_position)

            if (parent_size <= 2) { // Will be pruned
                adj_position.removeAt(adj_position.size - 1)
            } else if (adj_position.last() == parent_size - 1) {
                adj_position[adj_position.size - 1] -= 1
            }
        }
    }

    /*
        remove_one_of_two and remove_standard all exist so I could separate
        them and use the "forget" wrapper at the History layer
     */
    open fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].remove(beat_key.line_offset, beat_key.beat, position)
        }
    }

    open fun controller_line_remove_standard(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this._catch_blocked_tree_exception(beat_key.channel) {
            this.get_all_channels()[beat_key.channel].controller_line_remove_leaf(type, beat_key.line_offset, beat_key.beat, position)
        }
    }

    open fun controller_global_remove_standard(type: EffectType, beat: Int, position: List<Int>) {
        this._catch_global_ctl_blocked_tree_exception(type) {
            this.get_controller<EffectEvent>(type).remove_node(beat, position)
        }
    }

    open fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int? = null) {
        val first_beat = min(first_key.beat, second_key.beat)
        val second_beat = max(first_key.beat, second_key.beat)

        // Increase song duration as needed
        val chunk_size = second_beat - first_beat + 1
        val count = repeat ?: ceil((this.length - first_beat).toFloat() / chunk_size.toFloat()).toInt()
        val increase_count = max(0, ((count * chunk_size) + first_beat) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        val beat_keys = this._get_beat_keys_for_overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key, count)
        // Need to unset all events FIRST so no replaces get blocked
        for ((target_key, key_list) in beat_keys) {
            if (key_list.isEmpty()) continue
            for (overwrite_key in key_list) {
                if (overwrite_key != target_key) {
                    this.unset(overwrite_key, listOf())
                    // Copy all effects as well
                    for ((type, _) in this.get_all_channels()[overwrite_key.channel].lines[overwrite_key.line_offset].controllers.get_all()) {
                        this.controller_line_unset(type, overwrite_key, listOf())
                    }
                }
            }
        }

        for ((target_key, key_list) in beat_keys) {
            if (key_list.isEmpty()) continue
            for (overwrite_key in key_list) {
                if (target_key != overwrite_key) {
                    this.replace_tree(overwrite_key, null, this.get_tree_copy(target_key))
                    for ((type, _) in this.get_all_channels()[target_key.channel].lines[target_key.line_offset].controllers.get_all()) {
                        if (!this.has_line_controller(type, overwrite_key.channel, overwrite_key.line_offset)) {
                            this.new_line_controller(type, overwrite_key.channel, overwrite_key.line_offset)
                        }

                        this.controller_line_replace_tree(type, overwrite_key, listOf(), this.get_line_ctl_tree(type, target_key, listOf()))
                    }
                }
            }
        }
    }

    open fun controller_global_overwrite_range_horizontally(type: EffectType, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song duration as needed
        val chunk_size = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / chunk_size.toFloat()).toInt()
        val increase_count = max(0, ((count * chunk_size) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset targets to prevent blocking
        for (i in 0 until chunk_size) {
            for (j in 0 until count) {
                if (j != 0) {
                    this.controller_global_unset(
                        type,
                        (j * chunk_size) + (i + start),
                        listOf()
                    )
                }
            }
        }

        for (i in 0 until chunk_size) {
            for (j in 0 until count) {
                if (j != 0) {
                    this.controller_global_replace_tree(
                        type,
                        (j * chunk_size) + (i + start),
                        null,
                        this.get_global_ctl_tree<EffectEvent>(type, (i + start)).copy(this::copy_control_event)
                    )
                }
            }
        }
    }

    open fun controller_global_to_line_overwrite_range_horizontally(type: EffectType, target_channel: Int, target_line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song duration as needed
        val width = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset targets to prevent blocking
        for (i in 0 until width) {
            for (j in 0 until count) {
                if (j != 0) {
                    this.controller_line_unset(
                        type,
                        BeatKey(
                            target_channel,
                            target_line_offset,
                            (j * width) + (i + start),
                        ),
                        listOf()
                    )
                }
            }
        }

        for (i in 0 until width) {
            for (j in 0 until count) {
                if (j == 0) continue

                this.controller_line_replace_tree(
                    type,
                    BeatKey(
                        target_channel,
                        target_line_offset,
                        (j * width) + (i + start),
                    ),
                    null,
                    this.get_global_ctl_tree<EffectEvent>(type, (i + start)).copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_line_to_channel_overwrite_range_horizontally(type: EffectType, channel: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int? = null) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)

        // Increase song duration as needed
        val width = to_key.beat - from_key.beat + 1
        val count = repeat ?: ceil((this.length - from_key.beat).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + from_key.beat) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset Targets to prevent blocking
        val beat_keys = this.get_beatkeys_in_range(from_key, to_key)
        for (beat_key in beat_keys) {
            for (i in 0 until count) {
                this.controller_channel_unset(type, channel, beat_key.beat + (i * width), listOf())
            }
        }

        for (beat_key in beat_keys) {
            val working_tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key)
            for (i in 0 until count) {
                this.controller_channel_replace_tree(
                    type,
                    channel,
                    beat_key.beat + (i * width),
                    null,
                    working_tree.copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_global_to_channel_overwrite_range_horizontally(type: EffectType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song duration as needed
        val width = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset Targets to prevent blocking
        for (i in 0 until width) {
            for (j in 1 until count) {
                this.controller_channel_unset(type, channel, (j * width) + (i + start), listOf())
            }
        }

        for (i in 0 until width) {
            for (j in 1 until count) {
                this.controller_channel_replace_tree(
                    type,
                    channel,
                    (j * width) + (i + start),
                    null,
                    this.get_global_ctl_tree<EffectEvent>(type, (i + start)).copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_line_overwrite_range_horizontally(type: EffectType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int? = null) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)
        // Increase song duration as needed
        val width = to_key.beat - from_key.beat + 1
        val count = repeat ?: ceil((this.length - from_key.beat).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + from_key.beat) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        val beat_keys = this.get_beatkeys_in_range(from_key, to_key)
        // Unset targets to prevent blocking
        for (beat_key in beat_keys) {
            for (i in 0 until count) {
                val to_overwrite = BeatKey(channel, line_offset, beat_key.beat + (i * width))
                if (to_overwrite != beat_key) {
                    this.controller_line_unset(type, to_overwrite, listOf())
                }
            }
        }

        for (beat_key in beat_keys) {
            val working_tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key)
            for (i in 0 until count) {
                val to_overwrite = BeatKey(
                    channel,
                    line_offset,
                    beat_key.beat + (i * width)
                )
                if (to_overwrite != beat_key) {
                    this.controller_line_replace_tree(type, to_overwrite, null, working_tree.copy(this::copy_control_event))
                }
            }
        }
    }

    open fun controller_line_to_global_overwrite_range_horizontally(type: EffectType, channel: Int, line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song size as needed
        val width = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset Targets to prevent blocking
        for (i in start .. end) {
            for (j in 0 until count) {
                this.controller_global_unset(type, ((j + 1) * width) + i, listOf())
            }
        }

        for (i in start .. end) {

            val working_beat_key = BeatKey(channel, line_offset, i)
            for (j in 0 until count) {
                this.controller_global_replace_tree(
                    type,
                    ((j + 1) * width) + i,
                    null,
                    this.get_line_ctl_tree<EffectEvent>(type, working_beat_key).copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_channel_to_global_overwrite_range_horizontally(type: EffectType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song size as needed
        val width = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset Targets to prevent blocking
        for (i in start .. end) {
            for (j in 0 until count) {
                this.controller_global_unset(type, (j * width) + i, listOf())
            }
        }

        for (i in start .. end) {
            for (j in 0 until count) {
                this.controller_global_replace_tree(
                    type,
                    (j * width) + i,
                    null,
                    this.get_channel_ctl_tree<EffectEvent>(type, channel, i).copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_channel_overwrite_range_horizontally(type: EffectType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song size as needed
        val width = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(this.length, increase_count)
        }

        // Unset Targets first to prevent blocking
        for (i in start .. end) {
            for (j in 0 until count) {
                if (target_channel != from_channel || j != 0) {
                    this.controller_channel_unset(type, target_channel, (j * width) + i, listOf())
                }
            }
        }

        for (i in start .. end) {
            val working_tree = this.get_channel_ctl_tree<EffectEvent>(type, from_channel, i)
            for (j in 0 until count) {
                this.controller_channel_replace_tree(
                    type,
                    target_channel,
                    (j * width) + i,
                    null,
                    working_tree.copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_channel_to_line_overwrite_range_horizontally(type: EffectType, target_channel: Int, target_line_offset: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int? = null) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        // Increase song duration as needed
        val width = end - start + 1
        val count = repeat ?: ceil((this.length - start).toFloat() / width.toFloat()).toInt()
        val increase_count = max(0, ((count * width) + start) - this.length)
        if (increase_count > 0) {
            this.insert_beats(end + 1, increase_count)
        }


        // Unset Targets first to prevent blocking.
        for (i in start .. end) {
            for (j in 0 until count) {
                val working_key = BeatKey(target_channel, target_line_offset, (j * width) + i)
                this.controller_line_unset(type, working_key, listOf())
            }
        }

        for (i in start .. end) {
            val working_tree = this.get_channel_ctl_tree<EffectEvent>(type, from_channel, i)
            for (j in 0 until count) {
                this.controller_line_replace_tree(
                    type,
                    BeatKey(
                        target_channel,
                        target_line_offset,
                        (j * width) + i
                    ),
                    null,
                    working_tree.copy(this::copy_control_event)
                )
            }
        }
    }

    open fun controller_channel_overwrite_line(type: EffectType, target_channel: Int, original_channel: Int, original_beat: Int, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - original_beat)
        val increase_length = (adj_repeat + original_beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_channel_ctl_tree<EffectEvent>(type, original_channel, original_beat)
        for (i in 0 until adj_repeat) {
            this.controller_channel_replace_tree(type, target_channel, i + original_beat, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun controller_line_to_channel_overwrite_line(type: EffectType, target_channel: Int, original_key: BeatKey, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - original_key.beat)
        val increase_length = (adj_repeat + original_key.beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_line_ctl_tree<EffectEvent>(type, original_key)
        for (i in 0 until adj_repeat) {
            this.controller_channel_replace_tree(type, target_channel, i + original_key.beat, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun controller_global_to_channel_overwrite_line(type: EffectType, target_channel: Int, beat: Int, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - beat)
        val increase_length = (adj_repeat + beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_global_ctl_tree<EffectEvent>(type, beat)
        for (i in 0 until adj_repeat) {
            this.controller_channel_replace_tree(type, target_channel, i + beat, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int? = null) {
        if (this.is_percussion(beat_key.channel) != this.is_percussion(channel)) {
            throw InvalidOverwriteCall()
        }

        val working_key = BeatKey(channel, line_offset, beat_key.beat)
        val adj_repeat = repeat ?: (this.length - working_key.beat)
        val increase_length = (adj_repeat + working_key.beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        for (x in 0 until adj_repeat) {
            working_key.beat = x + beat_key.beat
            this.replace_tree(working_key, null, this.get_tree_copy(beat_key))
        }
    }

    open fun controller_global_overwrite_line(type: EffectType, beat: Int, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - beat)
        val increase_length = (adj_repeat + beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_global_ctl_tree<EffectEvent>(type, beat)
        for (i in 0 until adj_repeat) {
            this.controller_global_replace_tree(type, i + beat, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun controller_channel_to_global_overwrite_line(type: EffectType, channel: Int, beat: Int, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - beat)
        val increase_length = (adj_repeat + beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat)
        for (i in 0 until adj_repeat) {
            this.controller_global_replace_tree(type, i + beat, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun controller_line_to_global_overwrite_line(type: EffectType, beat_key: BeatKey, repeat: Int? = null) {
        val beat = beat_key.beat
        val adj_repeat = repeat ?: (this.length - beat)
        val increase_length = (adj_repeat + beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key)
        for (i in 0 until adj_repeat) {
            this.controller_global_replace_tree(type, i + beat, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun controller_global_to_line_overwrite_line(type: EffectType, from_beat: Int, target_channel: Int, target_line_offset: Int, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - from_beat)
        val increase_length = (adj_repeat + from_beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_global_ctl_tree<EffectEvent>(type, from_beat)
        for (i in 0 until adj_repeat) {
            this.controller_line_replace_tree(
                type,
                BeatKey(target_channel, target_line_offset, i + adj_repeat),
                listOf(),
                original_tree.copy(this::copy_control_event)
            )
        }
    }

    open fun controller_channel_to_line_overwrite_line(type: EffectType, target_channel: Int, target_line_offset: Int, original_channel: Int, original_beat: Int, repeat: Int? = null) {
        val adj_repeat = repeat ?: (this.length - original_beat)
        val increase_length = (adj_repeat + original_beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val original_tree = this.get_channel_ctl_tree<EffectEvent>(type, original_channel, original_beat)
        for (i in 0 until adj_repeat) {
            this.controller_line_replace_tree(
                type,
                BeatKey(target_channel, target_line_offset, i + original_beat),
                null,
                original_tree.copy(this::copy_control_event)
            )
        }
    }

    open fun controller_line_overwrite_line(type: EffectType, channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int? = null) {
        val beat = beat_key.beat
        val adj_repeat = repeat ?: (this.length - beat)
        val increase_length = (adj_repeat + beat) - this.length
        if (increase_length > 0) {
            this.insert_beats(this.length, increase_length)
        }

        val working_key = BeatKey(channel, line_offset, beat_key.beat)
        val original_tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key)
        for (x in 0 until adj_repeat) {
            working_key.beat = x + beat
            this.controller_line_replace_tree(type, working_key, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun set_all_line_controller_visibility(type: EffectType) {
       for (c in this.channels.indices) {
           for (l in this.channels[c].lines.indices) {
               if (!this.has_line_controller(type, c, l) || !this.get_line_controller<EffectEvent>(type, c, l).visible) {
                   this.toggle_line_controller_visibility(type, c, l)
               }
           }
       }
    }
    open fun unset_all_line_controller_visibility(type: EffectType) {
        for (c in this.channels.indices) {
            for (l in this.channels[c].lines.indices) {
                if (this.has_line_controller(type, c, l) && this.get_line_controller<EffectEvent>(type, c, l).visible) {
                    this.toggle_line_controller_visibility(type, c, l)
                }
            }
        }
    }

    open fun toggle_line_controller_visibility(type: EffectType, channel_index: Int, line_offset: Int) {
        val channel = this.get_all_channels()[channel_index]
        val line = channel.lines[line_offset]
        val exists = line.controllers.has_controller(type)
        if (!exists) {
            this.new_line_controller(type, channel_index, line_offset)
        }

        val controller = line.get_controller<EffectEvent>(type)
        this.set_line_controller_visibility(type, channel_index, line_offset, !controller.visible)
    }

    open fun set_all_channel_controller_visibility(type: EffectType) {
        for (c in this.channels.indices) {
            if (!this.has_channel_controller(type, c) || !this.get_channel_controller<EffectEvent>(type, c).visible) {
                this.toggle_channel_controller_visibility(type, c)
            }
        }
    }

    open fun unset_all_channel_controller_visibility(type: EffectType) {
        for (c in this.channels.indices) {
            if (this.has_channel_controller(type, c) && this.get_channel_controller<EffectEvent>(type, c).visible) {
                this.toggle_channel_controller_visibility(type, c)
            }
        }
    }

    open fun toggle_channel_controller_visibility(type: EffectType, channel_index: Int) {
        val channel = this.get_all_channels()[channel_index]
        val exists = channel.controllers.has_controller(type)
        if (!exists) {
            this.new_channel_controller(type, channel_index)
        }
        val controller = channel.get_controller<EffectEvent>(type)
        this.set_channel_controller_visibility(type, channel_index, !controller.visible)
    }

    open fun toggle_global_controller_visibility(type: EffectType) {
        val exists = this.controllers.has_controller(type)
        if (!exists) {
            this.new_global_controller(type)
        }
        val controller = this.get_controller<EffectEvent>(type)
        this.set_global_controller_visibility(type, !controller.visible)
    }

    open fun convert_events_in_tree_to_relative(beat_key: BeatKey, position: List<Int>) {
        val stack = mutableListOf(Pair(beat_key, position))
        while (stack.isNotEmpty()) {
            val (working_beat_key, working_position) = stack.removeAt(0)
            val working_tree = this.get_tree(working_beat_key, working_position)
            if (working_tree.has_event()) {
                this.convert_event_to_relative(working_beat_key, working_position)
            } else if (!working_tree.is_leaf()) {
                for ((i, _) in working_tree.divisions) {
                    stack.add(Pair(beat_key, List(working_position.size + 1) { j: Int ->
                        if (j != working_position.size) {
                            working_position[j]
                        } else {
                            i
                        }
                    }))
                }
            }
        }
    }

    open fun convert_events_in_tree_to_absolute(beat_key: BeatKey, position: List<Int>) {
        val stack = mutableListOf(Pair(beat_key, position))
        while (stack.isNotEmpty()) {
            val (working_beat_key, working_position) = stack.removeAt(0)
            val working_tree = this.get_tree(working_beat_key, working_position)
            if (working_tree.has_event()) {
                this.convert_event_to_absolute(working_beat_key, working_position)
            } else if (!working_tree.is_leaf()) {
                for ((i, _) in working_tree.divisions) {
                    stack.add(Pair(beat_key, List(working_position.size + 1) { j: Int ->
                        if (j != working_position.size) {
                            working_position[j]
                        } else {
                            i
                        }
                    }))
                }
            }
        }
    }

    open fun convert_events_in_beat_to_relative(beat: Int) {
        val channels = this.get_all_channels()
        for (i in channels.indices) {
            val channel = channels[i]
            for (j in 0 until channel.lines.size) {
                this.convert_events_in_tree_to_relative(BeatKey(i, j, beat), listOf())
            }
        }
    }

    open fun convert_events_in_beat_to_absolute(beat: Int) {
        val channels = this.get_all_channels()
        for (i in channels.indices) {
            val channel = channels[i]
            for (j in 0 until channel.lines.size) {
                this.convert_events_in_tree_to_absolute(BeatKey(i, j, beat), listOf())
            }
        }
    }

    open fun convert_events_in_line_to_relative(channel: Int, line_offset: Int) {
        for (beat in 0 until this.length) {
            this.convert_events_in_tree_to_relative(BeatKey(channel, line_offset, beat), listOf())
        }
    }

    open fun convert_events_in_line_to_absolute(channel: Int, line_offset: Int) {
        for (beat in 0 until this.length) {
            this.convert_events_in_tree_to_absolute(BeatKey(channel, line_offset, beat), listOf())
        }
    }

    /**
     * Recalculate the event of the tree @ [beat_key]/[position]
     * to be relative to the events before it, if it isn't already
     *
     * @throws NonEventConversion If no event is present
     */
    open fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.has_event()) throw NonEventConversion(beat_key, position)

        val event = tree.get_event()!!
        if (event !is AbsoluteNoteEvent) return

        var working_beat_key: BeatKey = beat_key
        var working_position: List<Int> = position
        var preceding_value: Int? = null
        while (preceding_value == null) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            preceding_value = this.get_absolute_value(pair.first, pair.second)
            working_beat_key = pair.first
            working_position = pair.second
        }

        val new_value = event.note - (preceding_value ?: 0)
        this.set_event(beat_key, position, RelativeNoteEvent(new_value, event.duration))
    }

    /**
     * Recalculate the event of the tree @ [beat_key]/[position]
     * to not depend on preceding events
     *
     * @throws NonEventConversion If no event is present
     */
    open fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.has_event()) {
            throw NonEventConversion(beat_key, position)
        }

        val event = tree.get_event()!!
        if (event !is RelativeNoteEvent) return

        // The implied first value can be 0
        var value = this.get_absolute_value(beat_key, position) ?: event.offset
        val radix = this.tuning_map.size
        if (value < 0 || value >= radix * 8) {
            value = min(abs(value), (radix * 8) - 1)
        }
        this.set_event(
            beat_key,
            position,
            AbsoluteNoteEvent(
                value,
                event.duration
            )
        )
    }

    /* -------------------------- End of Ordered functions ----------------------------------------- */
    private fun set_channel_program(channel: Int, program: Int) {
        this.get_channel(channel).set_midi_program(program)
    }

    fun <T: EffectEvent> controller_global_get_actual_position(ctl_type: EffectType, beat: Int, position: List<Int>): Pair<Int, List<Int>> {
        return this.get_controller<T>(ctl_type).get_blocking_position(beat, position) ?: Pair(beat, position)
    }

    fun <T: EffectEvent> controller_channel_get_actual_position(ctl_type: EffectType, channel: Int, beat: Int, position: List<Int>): Pair<Int, List<Int>> {
        return this.get_all_channels()[channel].get_controller<T>(ctl_type).get_blocking_position(beat, position) ?: Pair(beat, position)
    }

    fun <T: EffectEvent> controller_line_get_actual_position(ctl_type: EffectType, beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>> {
        val output = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<T>(ctl_type).get_blocking_position(beat_key.beat, position)
        return if (output == null) {
            Pair(beat_key, position)
        } else {
            Pair(
                BeatKey(beat_key.channel, beat_key.line_offset, output.first),
                output.second
            )
        }
    }

    open fun <T: EffectEvent> controller_global_set_initial_event(type: EffectType, event: T) {
        val controller = this.get_controller<T>(type)
        controller.initial_event = event
    }

    open fun <T: EffectEvent> controller_channel_set_initial_event(type: EffectType, channel: Int, event: T) {
        val controller = this.get_channel(channel).get_controller<T>(type)
        controller.initial_event = event
    }

    open fun <T: EffectEvent> controller_line_set_initial_event(type: EffectType, channel: Int, line_offset: Int, event: T) {
        val controller = this.get_channel(channel).lines[line_offset].get_controller<T>(type)
        controller.initial_event = event
    }

    private fun set_channel_bank(channel: Int, bank: Int) {
        if (this.is_percussion(channel)) {
            throw PercussionBankException()
        } else {
            (this.channels[channel] as OpusChannel).set_midi_bank(bank)
        }
    }

    /* only used in insert_beat. NO WHERE ELSE */
    open fun _apply_column_trees(beat_index: Int, beats_in_column: List<ReducibleTree<OpusEvent>>) {
        var y = 0
        for (channel in 0 until this.channels.size) {
            for (line in 0 until this.channels[channel].lines.size) {
                val beat_key = BeatKey(channel, line, beat_index)
                this.replace_tree(
                    beat_key,
                    listOf(),
                    checked_cast<ReducibleTree<TunedInstrumentEvent>>(beats_in_column[y++])
                )
            }
        }
    }

    fun remove_channel_by_uuid(uuid: Int) {
        val channel = this._channel_uuid_map[uuid] ?: throw OpusChannelAbstract.InvalidChannelUUID(uuid)
        var channel_index: Int? = null
        for (i in 0 until this.channels.size) {
            if (this.channels[i] === channel) {
                channel_index = i
                break
            }
        }
        if (channel_index != null) {
            this.remove_channel(channel_index)
        }
    }

    open fun merge_leafs(beat_key_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        if (beat_key_from == beat_key_to && position_from == position_to) {
            throw InvalidMergeException()
        }

        val blocked_ranges = mutableSetOf<Pair<Rational, Rational>>()
        var working_position_pair = Pair(beat_key_from, this.get_first_position(beat_key_from, position_from))
        while (working_position_pair.first == beat_key_from && working_position_pair.second.size > position_from.size && working_position_pair.second.subList(0, position_from.size) == position_from) {
            val working_tree = this.get_tree(working_position_pair.first, working_position_pair.second)
            if (working_tree.has_event()) {
                val duration = working_tree.get_event()!!.duration
                var (working_offset, working_width) = this.get_leaf_offset_and_width(working_position_pair.first, working_position_pair.second)
                working_offset -= working_position_pair.first.beat
                val working_offset_end = working_offset + Rational(duration, working_width)
                blocked_ranges.add(Pair(working_offset, working_offset_end))
            }

            working_position_pair = this.get_proceeding_leaf_position(working_position_pair.first, working_position_pair.second) ?: break
        }

        working_position_pair = Pair(beat_key_to, this.get_first_position(beat_key_to, position_to))
        while (working_position_pair.first == beat_key_to && working_position_pair.second.size > position_to.size && working_position_pair.second.subList(0, position_to.size) == position_to) {
            val working_tree = this.get_tree(working_position_pair.first, working_position_pair.second)
            if (working_tree.has_event()) {
                val duration = working_tree.get_event()!!.duration
                var (working_offset, working_width) = this.get_leaf_offset_and_width(working_position_pair.first, working_position_pair.second)
                working_offset -= working_position_pair.first.beat
                val working_offset_end = working_offset + Rational(duration, working_width)
                for ((from_offset, to_offset) in blocked_ranges) {
                    if ((working_offset >= from_offset && working_offset < to_offset) || (working_offset <= from_offset && working_offset_end > from_offset)) {
                        throw InvalidMergeException()
                    }
                }
                blocked_ranges.add(Pair(working_offset, working_offset_end))
            }

            working_position_pair = this.get_proceeding_leaf_position(working_position_pair.first, working_position_pair.second) ?: break
        }

        TODO("In Progress")

       // var (from_offset, from_width) = this.get_leaf_offset_and_width(beat_key_from, position_from)
       // var (to_offset, to_width) this.get_leaf_offset_and_width(beat_key_to, position_to)
       // TODO("In progress")

        //this.replace_tree(beat_key_to, position_to, new_tree)
        //this.unset(beat_key_from, position_from)
    }

    open fun set_beat_count(new_count: Int) {
        this.length = new_count
        for (channel in this.get_all_channels()) {
            channel.set_beat_count(new_count)
        }
        this.controllers.set_beat_count(new_count)
    }

    // Try to stick to GM by forcing percussion to channel 9
    fun get_midi_channel(real_channel: Int): Int {
        var first_percussion_index = this.channels.size + 1
        for (i in this.channels.indices) {
            if (this.is_percussion(i)) {
                first_percussion_index = i
                break
            }
        }

        return if (first_percussion_index > real_channel) {
            if (real_channel >= Midi.PERCUSSION_CHANNEL) {
                real_channel + 1
            } else {
                real_channel
            }
        } else if (first_percussion_index == real_channel) {
            Midi.PERCUSSION_CHANNEL
        } else {
            if (real_channel <= Midi.PERCUSSION_CHANNEL) {
                real_channel - 1
            } else {
                real_channel
            }
        }
    }

    fun get_midi(start_beat: Int = 0, end_beat: Int? = null, include_pointers: Boolean = false): Midi {
        data class StackItem<T>(var tree: ReducibleTree<T>, var divisions: Int, var offset: Int, var size: Int, var position: List<Int>)
        data class PseudoMidiEvent(var channel: Int, var note: Int, var bend: Int, var velocity: Int, var uuid: Int)
        var event_uuid_gen = 0

        val midi = Midi()
        midi.ppqn = 480

        midi.insert_event(0, Text("Generated with Pagan Music Sequencer."))
        this.project_notes?.let {
            midi.insert_event(0, Text(it))
        }

        // Set default values
        for (i in this.channels.indices) {
            val channel = this.channels[i]
            if (channel.muted) continue
            midi.insert_event(0, 0, VolumeMSB(i, 0x64))
        }

        val pseudo_midi_map = mutableListOf<Triple<Int, PseudoMidiEvent, Boolean>>()
        val max_tick = midi.get_ppqn() * (this.length + 1)
        val radix = this.tuning_map.size

        fun <U: EffectEvent> apply_active_controller(controller: EffectController<U>, gen_event_callback: (U, U?, Int) -> List<Pair<Int, MIDIEvent>>) {
            var skip_initial_set = false
            val initial_event = controller.get_initial_event()
            var latest_event = initial_event

            for (i in start_beat until (end_beat ?: this.length)) {
                val working_tree = controller.get_tree(i)
                val stack: MutableList<StackItem<U>> = mutableListOf(StackItem(working_tree, 1, (i - start_beat) * midi.ppqn, midi.ppqn, listOf()))
                while (stack.isNotEmpty()) {
                    val current = stack.removeAt(0)
                    if (current.tree.has_event()) {
                        val event = current.tree.get_event()!!
                        if (current.offset == 0) {
                            skip_initial_set = true
                        }

                        for ((j, midi_event) in gen_event_callback(event, latest_event, current.size)) {
                            midi.insert_event(0, current.offset + j, midi_event)
                        }

                        // Don't track reset_transitions, since their values will not affect next events
                        if (event.is_persistent()) {
                            latest_event = event
                        }

                    } else if (!current.tree.is_leaf()) {
                        val working_subdiv_size = current.size / current.tree.size
                        for ((j, subtree) in current.tree.divisions) {
                            stack.add(
                                StackItem(
                                    subtree,
                                    current.tree.size,
                                    current.offset + (working_subdiv_size * j),
                                    working_subdiv_size,
                                    current.position + listOf(j)
                                )
                            )
                        }
                    }
                }
            }

            if (!skip_initial_set) {
                val (_, midi_event) = gen_event_callback(initial_event, null, 0).first()
                midi.insert_event(0, 0, midi_event)
            }
        }

        val tempo_controller = this.get_controller<OpusTempoEvent>(EffectType.Tempo)
        apply_active_controller(tempo_controller) { event: OpusTempoEvent, previous_event: OpusTempoEvent?, frames: Int ->
            when (event.transition) {
                EffectTransition.RInstant -> {
                    listOf(
                        Pair(0, SetTempo.Companion.from_bpm((event.value * 1000f).roundToInt() / 1000F)),
                        Pair(frames, SetTempo.Companion.from_bpm(((previous_event?.value ?: 120F) * 1000f).roundToInt() / 1000F))
                    )
                }
                else -> {
                    listOf(
                        Pair(0, SetTempo.Companion.from_bpm((event.value * 1000f).roundToInt() / 1000F))
                    )
                }
            }
        }

        val channels = this.get_all_channels()
        for (c in channels.indices) {
            val pan_controller = channels[c].get_controller<OpusPanEvent>(EffectType.Pan)
            apply_active_controller(pan_controller) { event: OpusPanEvent, previous_event: OpusPanEvent?, frames: Int ->
                when (event.transition) {
                    EffectTransition.Instant -> {
                        val value = min(((event.value + 1F) * 64).toInt(), 127)
                        listOf(Pair(0, BalanceMSB(c, value)))
                    }

                    EffectTransition.RInstant -> {
                        val return_value = min((((previous_event?.value ?: 0F) + 1F) * 64).toInt(), 127)
                        val value = min(((event.value + 1F) * 64).toInt(), 127)
                        listOf(
                            Pair(0, BalanceMSB(c, value)),
                            Pair(frames, BalanceMSB(c, return_value)),
                        )
                    }

                    EffectTransition.Linear -> {
                        val latest_value = (previous_event?.value ?: 0F)
                        val diff = (event.value - latest_value) / (frames * event.duration).toFloat()
                        val working_list = mutableListOf<Pair<Int, MIDIEvent>>()
                        var last_val: Int? = null
                        for (x in 0 until frames * event.duration) {
                            val mid_val = latest_value + (x * diff)
                            val value = min(((mid_val + 1F) * 64).toInt(), 127)
                            if (last_val != value) {
                                working_list.add(Pair(x, BalanceMSB(c, value)))
                            }
                            last_val = value
                        }
                        working_list
                    }

                    EffectTransition.RLinear -> {
                        val latest_value = (previous_event?.value ?: 0F)
                        val diff = (latest_value - event.value) / (frames * event.duration).toFloat()
                        val working_list = mutableListOf<Pair<Int, MIDIEvent>>()
                        var last_val: Int? = null
                        for (x in 0 until frames * event.duration) {
                            val mid_val = event.value + (x * diff)
                            val value = min(((mid_val + 1F) * 64).toInt(), 127)
                            if (last_val != value) {
                                working_list.add(Pair(x, BalanceMSB(c, value)))
                            }
                            last_val = value
                        }
                        working_list
                    }
                }
            }

            if (channels[c].controllers.has_controller(EffectType.Volume)) {
                val volume_controller = channels[c].get_controller<OpusVolumeEvent>(EffectType.Volume)
                apply_active_controller(volume_controller) { event: OpusVolumeEvent, previous_event: OpusVolumeEvent?, frames: Int ->
                    when (event.transition) {
                        EffectTransition.Instant -> {
                            val value = event.value * 100
                            listOf(Pair(0, VolumeMSB(c, value.toInt())))
                        }

                        EffectTransition.RInstant -> {
                            val return_value = (previous_event?.value ?: 0F) * 100
                            val value = event.value * 100
                            listOf(
                                Pair(0, VolumeMSB(c, value.toInt())),
                                Pair(frames, VolumeMSB(c, return_value.toInt())),
                            )
                        }

                        EffectTransition.Linear -> {
                            val working_value = previous_event?.value ?: 64F
                            val diff = (event.value - working_value) / (frames * event.duration).toFloat()
                            val working_list = mutableListOf<Pair<Int, MIDIEvent>>()
                            var last_val: Int? = null
                            for (x in 0 until frames * event.duration) {
                                val mid_val = working_value + (x * diff)
                                val value = min((mid_val * 100).toInt(), 127)
                                if (last_val != value) {
                                    working_list.add(Pair(x, VolumeMSB(c, value)))
                                }
                                last_val = value
                            }
                            working_list
                        }

                        EffectTransition.RLinear -> {
                            val latest_value = previous_event?.value ?: 64F
                            val diff = (latest_value - event.value) / (frames * event.duration).toFloat()
                            val working_list = mutableListOf<Pair<Int, MIDIEvent>>()
                            var last_val: Int? = null
                            for (x in 0 until frames * event.duration) {
                                val mid_val = event.value + (x * diff)
                                val value = min((mid_val * 100).toInt(), 127)
                                if (last_val != value) {
                                    working_list.add(Pair(x, VolumeMSB(c, value)))
                                }
                                last_val = value
                            }
                            working_list
                        }
                    }
                }
            }
        }

        fun parse_delay_map(delay_controller: DelayController): List<Pair<IntRange, DelayEvent>> {
            // NOTE: Assumes instant transition only
            val intermediary = mutableListOf<Pair<Int, DelayEvent>>()
            intermediary.add(Pair(0, delay_controller.initial_event))
            for (b in delay_controller.beats.indices) {
                val beat_tree = delay_controller.beats[b]
                val stack: MutableList<StackItem<DelayEvent>> = mutableListOf(StackItem(beat_tree, 1, midi.ppqn * b, midi.ppqn, listOf()))
                while (stack.isNotEmpty()) {
                    val current = stack.removeAt(0)
                    if (current.tree.has_event()) {
                        if (!(b < start_beat || b >= (end_beat ?: this.length))) {
                            val event = current.tree.get_event()!!
                            intermediary.add(
                                Pair(
                                    current.offset,
                                    event
                                )
                            )
                        }
                    } else if (!current.tree.is_leaf()) {
                        val working_subdiv_size = current.size / current.tree.size
                        for ((i, subtree) in current.tree.divisions) {
                            stack.add(
                                StackItem(
                                    tree = subtree,
                                    divisions = current.tree.size,
                                    offset = current.offset + (working_subdiv_size * i),
                                    size = working_subdiv_size,
                                    position = current.position + listOf(i)
                                )
                            )
                        }
                    }
                }
            }

            val output = mutableListOf<Pair<IntRange, DelayEvent>>()
            var previous_entry = intermediary[0]
            for (i in 1 until intermediary.size) {
                val current_entry = intermediary[i]
                output.add(
                    Pair(
                        previous_entry.first until current_entry.first,
                        previous_entry.second
                    )
                )
                previous_entry = current_entry
            }
            output.add(
                Pair(
                    previous_entry.first until midi.ppqn * delay_controller.beats.size,
                    previous_entry.second
                )
            )

            return output
        }

        fun get_delayed_pseudo_events(input_event: PseudoMidiEvent, initial_offset: Int, delay_event_map: List<Pair<IntRange, DelayEvent>>?): List<Pair<Int,PseudoMidiEvent>> {
            if (delay_event_map == null) return listOf()
            val output = mutableListOf<Pair<Int, PseudoMidiEvent>>()

            var repeats_remaining = -1
            var echo_offset = initial_offset
            var working_velocity = input_event.velocity

            for ((delay_range, delay_event) in delay_event_map) {
                if (!delay_range.contains(echo_offset)) continue
                val attenuation = delay_event.fade
                val delay_in_ticks = midi.ppqn * delay_event.denominator / delay_event.numerator

                repeats_remaining = if (repeats_remaining == -1) {
                    echo_offset += delay_in_ticks
                    working_velocity = (working_velocity * attenuation).toInt()
                    delay_event.echo
                } else {
                    min(repeats_remaining, delay_event.echo)
                }

                while (repeats_remaining > 0 && delay_range.contains(echo_offset)) {
                    output.add(
                        Pair(
                            echo_offset,
                            PseudoMidiEvent(
                                input_event.channel,
                                input_event.note,
                                input_event.bend,
                                working_velocity,
                                event_uuid_gen++
                            )
                        )
                    )
                    echo_offset += delay_in_ticks
                    working_velocity = (working_velocity * attenuation).toInt()
                    repeats_remaining -= 1
                }
            }

            return output
        }

        val global_delay_map = if (this.has_global_controller(EffectType.Delay)) {
            parse_delay_map(this.get_global_controller<DelayEvent>(EffectType.Delay) as DelayController)
        } else {
            null
        }

        var percussion_exported = false
        var working_midi_channel = 0
        for (c in this.channels.indices) {
            if (this.channels[c].muted) continue
            val channel = this.get_channel(c)

            val midi_channel = if (this.is_percussion(c)) {
                if (percussion_exported) continue
                percussion_exported = true
                Midi.PERCUSSION_CHANNEL
            } else {
                if (working_midi_channel > 15) continue
                if (working_midi_channel == Midi.PERCUSSION_CHANNEL) {
                    working_midi_channel++
                }
                working_midi_channel++
            }

            val channel_delay_map = if (this.has_channel_controller(EffectType.Delay, c)) {
                parse_delay_map(this.get_channel_controller<DelayEvent>(EffectType.Delay, c) as DelayController)
            } else {
                null
            }

            midi.insert_event(0, 0, BankSelect(midi_channel, channel.get_midi_bank()))
            midi.insert_event(0, 0, ProgramChange(midi_channel, channel.midi_program))

            for (l in channel.lines.indices) {
                val line = channel.lines[l]
                if (line.muted) continue

                val line_delay_map = if (this.has_line_controller(EffectType.Delay, c, l)) {
                    parse_delay_map(this.get_line_controller<DelayEvent>(EffectType.Delay, c, l) as DelayController)
                } else {
                    null
                }

                var current_tick = 0
                var prev_note = 0

                line.beats.forEachIndexed { b: Int, beat_tree: ReducibleTree<out InstrumentEvent> ->
                    val stack: MutableList<StackItem<out InstrumentEvent>> = mutableListOf(StackItem(beat_tree, 1, current_tick, midi.ppqn, listOf()))
                    while (stack.isNotEmpty()) {
                        val current = stack.removeAt(0)
                        if (current.tree.has_event()) {
                            val event = current.tree.get_event()!!
                            val (note, bend) = if (this.is_percussion(c)) { // Ignore the event data and use percussion map
                                Pair(this.get_percussion_instrument(c, l) + 27, 0)
                            } else {
                                val current_note = when (event) {
                                    is RelativeNoteEvent -> event.offset + prev_note
                                    is AbsoluteNoteEvent -> event.note
                                    else -> break
                                }

                                val octave = current_note / radix
                                val offset = this.tuning_map[current_note % radix]

                                // This offset is calculated so the tuning map always reflects correctly
                                val transpose_offset = 12.0 * this.transpose.first.toDouble() / this.transpose.second.toDouble()
                                val std_offset = offset.first.toDouble() * 12.0 / offset.second.toDouble()

                                val bend = ((std_offset - floor(std_offset) + transpose_offset - floor(
                                    transpose_offset
                                )) * 512.0).toInt()

                                prev_note = current_note

                                Pair(
                                    (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21,
                                    bend
                                )
                            }

                            if (!(b < start_beat || b >= (end_beat ?: this.length))) {
                                val event_velocity = (this.get_current_velocity(BeatKey(c, l, b), current.position) * 100F).toInt()
                                val pseudo_event = PseudoMidiEvent(
                                    midi_channel,
                                    note,
                                    bend,
                                    event_velocity,
                                    event_uuid_gen++
                                )

                                val original_start = Triple(
                                    current.offset,
                                    pseudo_event,
                                    true
                                )

                                val original_end = Triple(
                                    min(current.offset + (current.size * event.duration), max_tick),
                                    pseudo_event,
                                    false
                                )

                                val line_repeats = get_delayed_pseudo_events(pseudo_event, current.offset, line_delay_map)
                                val channel_repeats = get_delayed_pseudo_events(pseudo_event, current.offset, channel_delay_map).toMutableList()
                                for ((offset, working_event) in line_repeats) {
                                    channel_repeats.addAll(get_delayed_pseudo_events(working_event, offset, channel_delay_map))
                                }
                                val global_repeats = get_delayed_pseudo_events(pseudo_event, current.offset, global_delay_map).toMutableList()
                                for ((offset, working_event) in channel_repeats) {
                                    global_repeats.addAll(get_delayed_pseudo_events(working_event, offset, global_delay_map))
                                }
                                val all_repeats = line_repeats + channel_repeats + global_repeats

                                var working_pair: Pair<Int, PseudoMidiEvent>? = null
                                for ((offset, working_event) in all_repeats) {
                                    working_pair?.let { (previous_offset: Int, previous_event: PseudoMidiEvent) ->
                                        pseudo_midi_map.add(
                                            Triple(
                                                listOf(previous_offset + (current.size * event.duration), max_tick, offset).min(),
                                                previous_event,
                                                false
                                            )
                                        )
                                    }
                                    pseudo_midi_map.add(Triple(offset, working_event, true))
                                    working_pair = Pair(offset, working_event)
                                }
                                working_pair?.let { (working_offset: Int, working_event: PseudoMidiEvent) ->
                                    pseudo_midi_map.add(
                                        Triple(
                                            min(working_offset + (current.size * event.duration), max_tick),
                                            working_event,
                                            false
                                        )
                                    )
                                }
                                pseudo_midi_map.add(original_start)
                                pseudo_midi_map.add(original_end)
                            }
                        } else if (!current.tree.is_leaf()) {
                            val working_subdiv_size = current.size / current.tree.size
                            for ((i, subtree) in current.tree.divisions) {
                                stack.add(
                                    StackItem(
                                        tree = subtree,
                                        divisions = current.tree.size,
                                        offset = current.offset + (working_subdiv_size * i),
                                        size = working_subdiv_size,
                                        position = current.position + listOf(i)
                                    )
                                )
                            }
                        }
                    }

                    if (!((b < start_beat || b >= (end_beat ?: this.length)))) {
                        current_tick += midi.ppqn
                    }
                }
            }
        }

        pseudo_midi_map.sortBy {
            (it.first * 2) + if (it.third) { 1 } else { 0 }
        }

        val index_map = HashMap<PseudoMidiEvent, Int>()
        val std_tuning = this.is_tuning_standard()

        for ((tick, pseudo_event, is_on) in pseudo_midi_map) {
            midi.insert_event(
                0,
                tick,
                if (is_on) {
                    if (!std_tuning) {
                        var current_index = 0
                        while (index_map.containsValue(current_index)) {
                            current_index += 1
                        }
                        index_map[pseudo_event] = current_index
                        NoteOn79(
                            index = current_index,
                            note = pseudo_event.note,
                            bend = pseudo_event.bend,
                            channel = pseudo_event.channel,
                            velocity = pseudo_event.velocity shl 8
                        )
                    } else {
                        NoteOn(
                            channel = pseudo_event.channel,
                            note = pseudo_event.note,
                            velocity = pseudo_event.velocity
                        )
                    }
                } else {
                    if (!std_tuning) {
                        NoteOff79(
                            index = index_map.remove(pseudo_event)!!,
                            note = pseudo_event.note,
                            bend = pseudo_event.bend,
                            channel = pseudo_event.channel,
                            velocity = pseudo_event.velocity shl 8
                        )
                    } else {
                        NoteOff(
                            channel = pseudo_event.channel,
                            note = pseudo_event.note,
                            velocity = pseudo_event.velocity
                        )
                    }
                }
            )
        }

        if (include_pointers) {
            for (beat in start_beat until (end_beat ?: this.length)) {
                midi.insert_event(
                    0,
                    midi.ppqn * (beat - start_beat),
                    SongPositionPointer(beat)
                )
            }
        }

        return midi
    }

    open fun to_json(): JSONHashMap {
        val output = JSONHashMap()

        val channels: MutableList<JSONHashMap> = mutableListOf()
        for (channel in this.channels) {
            channels.add(OpusChannelJSONInterface.Companion.generalize(channel))
        }
        output["size"] = this.length
        output["tuning_map"] = JSONList(this.tuning_map.size) { i: Int ->
            JSONList(
                JSONInteger(this.tuning_map[i].first),
                JSONInteger(this.tuning_map[i].second)
            )
        }
        val tags = JSONHashMap()
        for ((beat, tag) in this.marked_sections) {
            tags[beat.toString()] = tag
        }
        output["tags"] = tags

        output["transpose"] = JSONInteger(this.transpose.first)
        output["transpose_radix"] = JSONInteger(this.transpose.second)
        output["controllers"] = ActiveControlSetJSONInterface.Companion.to_json(this.controllers)

        output["channels"] = JSONList(this.channels.size) { i: Int ->
            OpusChannelJSONInterface.Companion.generalize(this.channels[i])
        }

        output["title"] = if (this.project_name == null) {
            null
        } else {
            JSONString(this.project_name!!)
        }
        output["notes"] = if (this.project_notes == null) {
            null
        } else {
            JSONString(this.project_notes!!)
        }

        return JSONHashMap(
            "d" to output,
            "v" to JSONInteger(OpusManagerJSONInterface.Companion.LATEST_VERSION)
        )
    }

    // Clear function is used for new projects
    open fun clear() {
        this.marked_sections.clear()
        this.length = 0
        this.channels.clear()
        this.project_name = null
        this.tuning_map = Array(12) {
            i: Int -> Pair(i, 12)
        }

        this.transpose = Pair(0, 12)
        this.controllers.clear()

        this._cached_row_map.clear()
        this._cached_inv_visible_line_map.clear()
        this._cached_ctl_map_line.clear()
        this._cached_ctl_map_channel.clear()
        this._cached_ctl_map_global.clear()
    }

    open fun tag_section(beat: Int, title: String?) {
        this.marked_sections[beat] = title
    }

    open fun remove_tagged_section(beat: Int) {
        this.marked_sections.remove(beat)
    }

    fun get_marked_sections(): List<Pair<Int, String?>> {
        return this.marked_sections.toList()
    }

    fun is_beat_tagged(beat: Int): Boolean {
        return this.marked_sections.containsKey(beat)
    }

    /* Needs to be called by interface after new()/load()/import_midi() */
    open fun on_project_changed() {
        this.set_global_controller_visibility(EffectType.Tempo, true)
        this._reshape_lines_from_blocked_trees()
        this.recache_line_maps()
    }

    open fun load(bytes: ByteArray, on_load_callback: (() -> Unit)? = null) {
        val json_content = bytes.toString(Charsets.UTF_8)
        var generalized_object = JSONParser.Companion.parse<JSONHashMap>(json_content) ?: throw EmptyJSONException()
        var version = OpusManagerJSONInterface.Companion.detect_version(generalized_object)
        while (version < OpusManagerJSONInterface.Companion.LATEST_VERSION) {
            generalized_object = when (version++) {
                3 -> OpusManagerJSONInterface.Companion.convert_v3_to_v4(generalized_object)
                2 -> OpusManagerJSONInterface.Companion.convert_v2_to_v3(generalized_object)
                1 -> OpusManagerJSONInterface.Companion.convert_v1_to_v2(generalized_object)
                0 -> OpusManagerJSONInterface.Companion.convert_v0_to_v1(generalized_object)
                else -> throw UnknownSaveVersion(version - 1)
            }
        }

        this.project_change_json(generalized_object, on_load_callback)
    }

    open fun project_change_wrapper(callback: () -> Unit) {
        this.clear()
        this.project_changing = true
        try {
            callback()
        } finally {
            this.project_changing = false
        }

        this.on_project_changed()
    }

    open fun project_refresh() {
        this.on_project_changed()
    }

    fun project_change_new() {
        this.project_change_wrapper {
            this._project_change_new()
        }
    }

    open fun _project_change_new() {
        this.import_from_other(OpusLayerBase.initialize_basic())
    }

    fun project_change_json(json_data: JSONHashMap, on_load_callback: (() -> Unit)? = null) {
        this.project_change_wrapper {
            this._project_change_json(json_data)
            on_load_callback?.let { it() }
        }
    }

    open fun _project_change_json(json_data: JSONHashMap) {
        val inner_map = json_data["d"] as JSONHashMap
        this.set_project_name(inner_map.get_stringn("title"))
        this.set_project_notes(inner_map.get_stringn("notes"))
        this.channels.clear()

        this.set_beat_count(inner_map.get_int("size"))
        for (generalized_channel in inner_map.get_list("channels")) {
            val channel = OpusChannelJSONInterface.Companion.interpret(
                generalized_channel as JSONHashMap,
                this.length
            )

            channel.uuid = OpusLayerBase.gen_channel_uuid()
            this.channels.add(channel)
            this._channel_uuid_map[channel.uuid] = channel
        }

        val generalized_tuning_map = inner_map.get_list("tuning_map")
        this.tuning_map = Array(generalized_tuning_map.size) { i: Int ->
            val g_pair = generalized_tuning_map.get_list(i)
            Pair(
                g_pair.get_int(0),
                g_pair.get_int(1)
            )
        }

        this.transpose = Pair(
            inner_map.get_int("transpose", 0),
            inner_map.get_int("transpose_radix", this.tuning_map.size)
        )

        this.controllers = ActiveControlSetJSONInterface.Companion.from_json(inner_map.get_hashmap("controllers"), this.length)

        this.marked_sections.clear()
        val tags = inner_map.get_hashmapn("tags")
        if (tags != null) {
            for (key in tags.keys) {
                val beat = key.toInt()
                // technically possible if the user manually edits the file for sections to be out of bounds
                if (beat in 0 until this.length) {
                    this.marked_sections[beat] = tags.get_stringn(key)
                }
            }
        }
    }

    fun project_change_midi(midi: Midi) {
        this.project_change_wrapper {
            this._project_change_midi(midi)
        }
    }

    open fun _project_change_midi(midi: Midi) {
        val (settree, tempo_line, instrument_map) = OpusLayerBase.tree_from_midi(midi)
        val mapped_events = settree.get_events_mapped()
        val midi_channel_map = HashMap<Int, Int>()
        val channel_sizes = mutableListOf<Int>()

        val percussion_map = HashMap<Int, Int>()
        val percussion_instrument_map = HashMap<Int, Int>()

        // Calculate the number of lines needed per channel
        val blocked_ranges = HashMap<Int, MutableList<MutableList<Pair<Rational, Rational>>>>()
        val blocked_percussion_ranges = mutableListOf<MutableList<MutableList<Pair<Rational, Rational>>>>()

        // Map the events so i don't have to calculate overlaps twice
        val remapped_events = mutableListOf<Pair<List<Pair<Int, Int>>, MutableList<Pair<Array<Int>, Int>>>>()

        for ((position, event_set) in mapped_events) {
            remapped_events.add(Pair(position, mutableListOf()))

            var working_start = Rational(position[0].first, 1)
            var width_denominator = 1

            for ((i, size) in position.subList(1, position.size)) {
                width_denominator *= size
                working_start += Rational(i, width_denominator)
            }

            for (event in event_set) {
                val event_channel = event[0]
                if (!midi_channel_map.contains(event_channel)) {
                    midi_channel_map[event_channel] = midi_channel_map.size
                }
                val channel_index = midi_channel_map[event_channel]!!
                val event_size = Rational(event[2], position[1].second)
                val working_end = working_start + event_size

                if (event_channel == Midi.PERCUSSION_CHANNEL) {
                    val event_note = event[1]
                    if (!percussion_map.contains(event_note)) {
                        percussion_map[event_note] = blocked_percussion_ranges.size
                        blocked_percussion_ranges.add(mutableListOf())
                    }
                    val index = percussion_map[event_note]!!

                    var insertion_index = 0
                    for (i in 0 until blocked_percussion_ranges[index].size) {
                        for ((start, end) in blocked_percussion_ranges[index][i]) {
                            if ((working_start >= start && working_start < end) || (working_end > start && working_end <= end) || (start >= working_start && start < working_end) || (end > working_start && end <= working_end)) {
                                insertion_index += 1
                                break
                            }
                        }

                        // passed all the checks, no need to keep looping
                        if (i == insertion_index) break
                    }

                    if (insertion_index == blocked_percussion_ranges[index].size) {
                        blocked_percussion_ranges[index].add(mutableListOf())
                    }
                    blocked_percussion_ranges[index][insertion_index].add(Pair(working_start, working_end))
                    remapped_events.last().second.add(Pair(event, insertion_index))

                } else {
                    if (!blocked_ranges.containsKey(channel_index)) {
                        blocked_ranges[channel_index] = mutableListOf()
                    }

                    var insertion_index = 0
                    for (i in 0 until blocked_ranges[channel_index]!!.size) {
                        for ((start, end) in blocked_ranges[channel_index]!![i]) {
                            if ((working_start >= start && working_start < end) || (working_end > start && working_end <= end) || (start >= working_start && start < working_end) || (end > working_start && end <= working_end)) {
                                insertion_index += 1
                                break
                            }
                        }

                        // passed all the checks, no need to keep looping
                        if (i == insertion_index) break
                    }

                    if (insertion_index == blocked_ranges[channel_index]!!.size) {
                        blocked_ranges[channel_index]!!.add(mutableListOf())
                    }

                    blocked_ranges[channel_index]!![insertion_index].add(Pair(working_start, working_end))
                    remapped_events.last().second.add(Pair(event, insertion_index))
                }
            }
        }

        for ((channel, blocks) in blocked_ranges) {
            while (channel >= channel_sizes.size) {
                channel_sizes.add(0)
            }
            channel_sizes[channel] = blocks.size
        }

        if (midi_channel_map.containsKey(Midi.PERCUSSION_CHANNEL)) {
            // Add Percussion to channel_sizes list
            val adj_channel = midi_channel_map[Midi.PERCUSSION_CHANNEL]!!
            while (adj_channel >= channel_sizes.size) {
                channel_sizes.add(0)
            }

            for (blocks in blocked_percussion_ranges) {
                channel_sizes[adj_channel] += blocks.size
            }

            // Move Percussion to Last Opus Manager Channel
            // TODO: No Longer Necessary
            for ((mchannel, ochannel) in midi_channel_map) {
                if (mchannel == Midi.PERCUSSION_CHANNEL) continue
                if (ochannel > adj_channel) {
                    midi_channel_map[mchannel] = ochannel - 1
                }
            }

            val percussion_line_count = channel_sizes.removeAt(adj_channel)
            midi_channel_map[Midi.PERCUSSION_CHANNEL] = channel_sizes.size
            channel_sizes.add(percussion_line_count)
        }

        val keys = midi_channel_map.keys.toList()
        val sorted_channels = MutableList(keys.size) { i: Int ->
            Pair(midi_channel_map[keys[i]]!!, keys[i])
        }.sortedBy { it.first }

        sorted_channels.forEachIndexed { i: Int, pair: Pair<Int, Int> ->
            val (channel, midi_channel) = pair
            this.new_channel(lines = channel_sizes[channel], is_percussion = midi_channel == Midi.PERCUSSION_CHANNEL)
        }

        // Flag ignore blocking so we don't keep rechecking
        for (channel in this.get_all_channels()) {
            for (line in channel.lines) {
                line.flag_ignore_blocking = true
            }
        }

        this.set_beat_count(settree.size)

        var split_dur: Long = 0

        val events_to_set = mutableSetOf<Triple<BeatKey, List<Int>, Array<Int>>>()
        for ((position, event_set) in remapped_events) {
            val event_list = event_set.toMutableList()
            event_list.sortWith(compareBy { 127 - it.first[1] })
            for ((event, line_offset) in event_list) {
                val event_channel = event[0]
                val channel_index = midi_channel_map[event_channel]!!

                // line_offset needs to be recalculated HERE for percussion as the percussion block map will change size during init
                val adj_line_offset = if (event_channel == Midi.PERCUSSION_CHANNEL) {
                    val event_note = event[1]
                    var re_adj_line_offset = 0
                    val coarse_index = percussion_map[event_note]!!
                    for (i in 0 until coarse_index) {
                        re_adj_line_offset += blocked_percussion_ranges[i].size
                    }

                    val new_offset = line_offset + re_adj_line_offset
                    percussion_instrument_map[new_offset] = event_note - 27
                    new_offset
                } else {
                    line_offset
                }

                val working_position = mutableListOf<Int>()
                var working_beatkey: BeatKey? = null

                position.forEachIndexed { i: Int, (x, size): Pair<Int, Int> ->
                    if (i == 0) {
                        working_beatkey = BeatKey(channel_index, adj_line_offset, x)
                    } else {
                        if (this.get_tree(working_beatkey!!, working_position).size != size) {
                            val c = System.currentTimeMillis()
                            this.split_tree(working_beatkey, working_position, size)
                            split_dur += System.currentTimeMillis() - c
                        }
                        working_position.add(x)
                    }
                }

                working_beatkey?.let {
                    events_to_set.add(Triple(it, working_position, event))
                }
            }
        }

        for ((beatkey, position, event) in events_to_set) {
            if (event[0] == Midi.PERCUSSION_CHANNEL) {
                val tree = (this.get_channel(beatkey.channel) as OpusPercussionChannel).lines[beatkey.line_offset].get_tree(beatkey.beat, position)
                tree.set_event(PercussionEvent(event[2]))

                //this.set_percussion_event(beatkey, position)
                //this.set_duration(beatkey, position, event[2])
            } else {
                val event_note = event[1] - 21
                if (event_note in 0..127) {
                    val tree = (this.get_channel(beatkey.channel) as OpusChannel).lines[beatkey.line_offset].get_tree(beatkey.beat, position)

                    tree.set_event(
                        AbsoluteNoteEvent(
                            event_note,
                            event[2]
                        )
                    )
                }
            }
        }

        val all_channels = this.get_all_channels()
        // Reduce
        all_channels.forEachIndexed { i: Int, channel: OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>> ->
            for (j in channel.lines.indices) {
                for (k in 0 until this.length) {

                    val beat_tree = this.get_tree(BeatKey(i, j, k), listOf())
                    val original_size = beat_tree.size
                    beat_tree.reduce()
                    beat_tree.traverse { working_tree: ReducibleTree<out InstrumentEvent>, event: InstrumentEvent? ->
                        if (event == null) return@traverse

                        var tmp_tree = beat_tree
                        var denominator = 1
                        val tree_position = working_tree.get_path()
                        for (p in tree_position) {
                            denominator *= tmp_tree.size
                            tmp_tree = tmp_tree[p]
                        }

                        // Not worrying too much about duration accuracy. would inevitably cause overly divided beats
                        // val leaf_ratio = 1f / denominator.toFloat() // (commented for clarity as to why I called the variable 'denominator', but don't use it as one)
                        event.duration = max(
                            1,
                            (event.duration * denominator.toFloat() / original_size).roundToInt()
                        )
                    }
                }
            }
        }


        for ((midi_channel, bank, program) in instrument_map) {
            // Midi may have contained programchange event for channel, but no music
            val opus_channel = midi_channel_map[midi_channel] ?: continue
            if (bank != null) {
                this.set_channel_bank(opus_channel, bank)
            }
            if (program != null) {
                this.set_channel_program(opus_channel, program)
            }
        }

        for (c in this.channels.indices) {
            if (!this.is_percussion(c)) continue
            for ((line_offset, instrument) in percussion_instrument_map) {
                this.percussion_set_instrument(c, line_offset, instrument)
            }
            break
        }

        val tempo_controller = this.get_controller<OpusTempoEvent>(EffectType.Tempo)
        tempo_line.forEachIndexed { i: Int, tempo_tree: ReducibleTree<OpusTempoEvent> ->
            // Limit the number of divisions in the tempo ctl line
            if (!tempo_tree.is_eventless()) {
                var max_leafs = 0
                for (channel in this.channels) {
                    for (line in channel.lines) {
                        max_leafs = max(line.beats[i].weighted_size, max_leafs)
                    }
                }

                if (max_leafs < tempo_tree.weighted_size) {
                    tempo_tree.flatten()
                    val new_tree = ReducibleTree<OpusTempoEvent>()
                    new_tree.set_size(max_leafs)

                    for ((index, child) in tempo_tree.divisions) {
                        if (child.has_event()) {
                            new_tree[index * max_leafs / tempo_tree.size] = child
                        }
                    }

                    new_tree.reduce()
                    tempo_controller.beats[i] = new_tree
                } else {
                    tempo_controller.beats[i] = tempo_tree
                }
            }
        }

        /*
            If the first leaf sets the tempo, use that as initial value instead of as a control event.
         */
        val first_tempo_tree = tempo_controller.get_tree(0)
        val position = first_tempo_tree.get_first_event_tree_position()?.toIntArray() ?: intArrayOf()
        val first_tempo_leaf = first_tempo_tree.get(*position)

        if (first_tempo_leaf.has_event()) {
            tempo_controller.set_initial_event(first_tempo_leaf.event!!)
            first_tempo_leaf.unset_event()
        }

        // setup block/overlap caches ------------------
        for (channel in this.get_all_channels()) {
            for (line in channel.lines) {
                line.init_blocked_tree_caches()
                for ((_, controller) in line.controllers.get_all()) {
                    controller.init_blocked_tree_caches()
                }
            }
            for ((_, controller) in channel.controllers.get_all()) {
                controller.init_blocked_tree_caches()
            }
        }

        for ((_, controller) in this.controllers.get_all()) {
            controller.init_blocked_tree_caches()
        }
        // ----------------------------------------------
        // Unflag so blocking gets tracked
        for (channel in this.get_all_channels()) {
            for (line in channel.lines) {
                line.flag_ignore_blocking = false
            }
        }
    }

    fun <T: EffectEvent> get_current_effects(type: EffectType, beat_key: BeatKey, position: List<Int>): Triple<T?, T?, T?> {
        // if the velocity controller exists, use that otherwise consider velocity to be volume
        val channel = this.channels[beat_key.channel]
        val line = channel.lines[beat_key.line_offset]
        val event_position = line.get_tree(beat_key.beat).get_rational_position(position)

        return Triple(
            if (this.controllers.has_controller(type)) {
                this.get_controller<T>(type).coerce_event(beat_key.beat, event_position)
            } else {
                null
            },
            if (channel.controllers.has_controller(type)) {
                channel.get_controller<T>(type).coerce_event(beat_key.beat, event_position)
            } else {
                null
            },
            if (channel.controllers.has_controller(type)) {
                line.get_controller<T>(type).coerce_event(beat_key.beat, event_position)
            } else {
                null
            }
        )
    }

    fun <T: EffectEvent> get_current_line_effect(type: EffectType, beat_key: BeatKey, position: List<Int>): T? {
        // if the velocity controller exists, use that otherwise consider velocity to be volume
        val line = this.channels[beat_key.channel].lines[beat_key.line_offset]

        return if (line.controllers.has_controller(type)) {
            val event_position = line.get_tree(beat_key.beat).get_rational_position(position)
            val controller = line.get_controller<T>(type)
            controller.coerce_event(beat_key.beat, event_position)
        } else {
            null
        }
    }

    fun get_current_velocity(beat_key: BeatKey, position: List<Int>): Float {
        val event: SingleFloatEvent? = this.get_current_line_effect(EffectType.Velocity, beat_key, position) ?: this.get_current_line_effect(EffectType.Volume, beat_key, position)
        return event?.value ?: 1F
    }

    internal fun _get_beatkeys_from_range(beat_key: BeatKey, from_key: BeatKey, to_key: BeatKey): List<BeatKey> {
        if (! this._is_valid_beat_range(from_key, to_key)) {
            throw RangeOverflow(from_key, to_key, beat_key)
        }

        val (y_diff, x_diff) = this.get_abs_difference(from_key, to_key)

        if (this.length <= beat_key.beat + x_diff) {
            throw RangeOverflow(from_key, to_key, beat_key)
        }

        val (target_channel, target_offset) = try {
            this.get_channel_and_line_offset(this.get_instrument_line_index(beat_key.channel, beat_key.line_offset) + y_diff)
        } catch (_: IndexOutOfBoundsException) {
            throw RangeOverflow(from_key, to_key, beat_key)
        }

        val target_second_key = BeatKey(target_channel, target_offset, beat_key.beat + x_diff)
        return this.get_beatkeys_in_range(beat_key, target_second_key)
    }

    fun <T: EffectEvent> copy_control_event(event: T?): T? {
        return if (event == null) {
            null
        } else {
            event.copy() as T
        }
    }

    private fun _is_valid_beat_range(first_corner: BeatKey, second_corner: BeatKey): Boolean {
        return this.channels.size > first_corner.channel
            && this.get_channel(first_corner.channel).size > first_corner.line_offset
            && this.length > first_corner.beat
            && this.channels.size > second_corner.channel
            && this.get_channel(second_corner.channel).size > second_corner.line_offset
            && this.length > second_corner.beat
    }

    // Calling this function every time a channel/line is modified should still be more efficient
    // than calculating offsets as needed
    open fun recache_line_maps() {
        this._cached_instrument_line_map.clear()
        this._cached_std_line_map.clear()

        var y = 0
        for (c in this.channels.indices) {
            val channel = this.get_channel(c)
            for (line_offset in channel.lines.indices) {
                val keypair = Pair(c, line_offset)
                this._cached_instrument_line_map.add(keypair)
                this._cached_std_line_map[keypair] = y
                y += 1
            }
        }

        this._cached_abs_line_map_map.clear()
        this._cached_inv_abs_line_map_map.clear()

        val channels = this.get_all_channels()
        for (channel_index in channels.indices) {
            val channel = channels[channel_index]
            for (line_offset in channel.lines.indices) {
                val keypair = Pair(channel_index, line_offset)
                this._cached_inv_abs_line_map_map[this._cached_std_line_map[keypair]!!] = this._cached_abs_line_map_map.size
                this._cached_abs_line_map_map.add(Triple(this._cached_std_line_map[keypair]!!, null, null))

                for ((type, _) in channel.lines[line_offset].controllers.get_all()) {
                    this._cached_abs_line_map_map.add(
                        Triple(
                            this._cached_std_line_map[keypair]!!,
                            CtlLineLevel.Line,
                            type
                        )
                    )
                }
            }
            for ((type, _) in channel.controllers.get_all()) {
                this._cached_abs_line_map_map.add(
                    Triple(
                        channel_index,
                        CtlLineLevel.Channel,
                        type
                    )
                )
            }
        }

        for ((type, _) in this.controllers.get_all()) {
            this._cached_abs_line_map_map.add(
                Triple(
                    -1,
                    CtlLineLevel.Global,
                    type
                )
            )
        }


        /* Now, calculate and cache based on visibility ---------------------------------------------*/
        this._cached_row_map.clear()
        this._cached_inv_visible_line_map.clear()
        this._cached_ctl_map_line.clear()
        this._cached_ctl_map_channel.clear()
        this._cached_ctl_map_global.clear()

        var ctl_line = 0
        var visible_line = 0

        for (channel_index in channels.indices) {
            val channel = channels[channel_index]
            for (line_offset in channel.lines.indices) {
                this._cached_inv_visible_line_map[ctl_line] = visible_line
                this._cached_row_map[visible_line] = ctl_line
                visible_line += 1
                ctl_line += 1

                for ((type, controller) in channel.lines[line_offset].controllers.get_all()) {
                    if (controller.visible) {
                        this._cached_inv_visible_line_map[ctl_line] = visible_line
                        this._cached_row_map[visible_line] = ctl_line
                        this._cached_ctl_map_line[Triple(channel_index, line_offset, type)] = visible_line
                        visible_line += 1
                    }
                    ctl_line += 1
                }
            }

            for ((type, controller) in channel.controllers.get_all()) {
                if (controller.visible) {
                    this._cached_inv_visible_line_map[ctl_line] = visible_line
                    this._cached_row_map[visible_line] = ctl_line
                    this._cached_ctl_map_channel[Pair(channel_index, type)] = visible_line
                    visible_line += 1
                }
                ctl_line += 1
            }
        }

        for ((type, controller) in this.controllers.get_all()) {
            if (controller.visible) {
                this._cached_inv_visible_line_map[ctl_line] = visible_line
                this._cached_row_map[visible_line] = ctl_line
                this._cached_ctl_map_global[type] = visible_line
                visible_line += 1
            }
            ctl_line += 1
        }
    }

    private fun _get_beat_keys_for_overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey): List<BeatKey> {
        val working_key = BeatKey(channel, line_offset, beat_key.beat)
        return List(this.length - beat_key.beat) { i: Int ->
            working_key.beat = i + beat_key.beat
            working_key
        }

    }

    private fun _get_beat_keys_for_overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, count: Int): List<Pair<BeatKey, List<BeatKey>>> {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)
        val width = to_key.beat - from_key.beat + 1
        val beat_keys = this.get_beatkeys_in_range(from_key, to_key)
        val y_index_main = this.get_instrument_line_index(channel, line_offset)
        val y_index_from = this.get_instrument_line_index(from_key.channel, from_key.line_offset)
        val y_diff = y_index_main - y_index_from

        return List(beat_keys.size) { i: Int ->
            val beat_key = beat_keys[i]
            val y_index_new = this.get_instrument_line_index(beat_key.channel, beat_key.line_offset)
            val (new_channel, new_line_offset) = this.get_channel_and_line_offset(y_index_new + y_diff)
            if (this.is_percussion(new_channel) != this.is_percussion(beat_key.channel)) {
                throw MixedInstrumentException(beat_key,
                    BeatKey(new_channel, new_line_offset, beat_key.beat)
                )
            }
            Pair(
                beat_key,
                List(count) { j: Int ->
                    BeatKey(new_channel, new_line_offset, beat_key.beat + (j * width))
                }
            )
        }
    }

    /*
        Checks if the tuning falls within 12-tone equal-temper tuning.
        this means a tuning_map with less than 12 entries is still standard if
        every entry is also ((some number) / 12)
     */
    fun is_tuning_standard(): Boolean {
        val actuals = List(12) { i: Int -> i.toDouble() / 12.0 }
        if (!actuals.contains((this.transpose.first.toDouble() / this.transpose.second.toDouble()) % 1.0)) return false

        for ((numerator, denominator) in this.tuning_map) {
            if (!actuals.contains(numerator.toDouble() / denominator.toDouble())) return false
        }

        return true
    }

    fun has_global_controller(type: EffectType): Boolean {
        return this.controllers.has_controller(type)
    }

    fun has_channel_controller(type: EffectType, channel: Int): Boolean {
        return this.get_all_channels()[channel].controllers.has_controller(type)
    }

    fun has_line_controller(type: EffectType, channel: Int, line_offset: Int): Boolean {
        return this.get_all_channels()[channel].lines[line_offset].controllers.has_controller(type)
    }

    open fun <T: OpusLayerBase> import_from_other(other: T) {
        this.clear()
        this.length = other.length
        this.channels = other.channels
        this.project_name = other.project_name
        this.tuning_map = other.tuning_map.clone()
        this.transpose = other.transpose.copy()
        this._cached_instrument_line_map = other._cached_instrument_line_map
        this._cached_std_line_map = other._cached_std_line_map
        this.controllers = other.controllers

        // Saves having to recache_lines()
        this._cached_row_map.putAll(other._cached_row_map)
        this._cached_inv_visible_line_map.putAll(other._cached_inv_visible_line_map)
        this._cached_ctl_map_line.putAll(other._cached_ctl_map_line)
        this._cached_ctl_map_channel.putAll(other._cached_ctl_map_channel)
        this._cached_ctl_map_global.putAll(other._cached_ctl_map_global)
        this._cached_inv_abs_line_map_map.putAll(other._cached_inv_abs_line_map_map)
        this._cached_abs_line_map_map.addAll(other._cached_abs_line_map_map)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OpusLayerBase
            || this.length != other.length
            || this.project_name != other.project_name
            || this.project_notes != other.project_notes
            || this.transpose != other.transpose
            || !this.tuning_map.contentEquals(other.tuning_map)
            || this.channels.size != other.channels.size) {
            return false
        }

        for (i in 0 until this.channels.size) {
            if (this.channels[i] != other.channels[i]) {
                return false
            }
        }

        return other.controllers == this.controllers
    }

    fun get_all_channels(): List<OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>>> {
        return this.channels
    }
    fun get_percussion_channels(include_muted: Boolean = true): List<Pair<Int, OpusPercussionChannel>> {
        val output = mutableListOf<Pair<Int, OpusPercussionChannel>>()
        for (c in 0 until this.channels.size) {
            if (this.is_percussion(c) && (include_muted || !this.channels[c].muted)) {
                output.add(Pair(c, this.get_channel(c) as OpusPercussionChannel))
            }
        }
        return output
    }

    internal fun get_leaf_offset_and_width(beat_key: BeatKey, position: List<Int>, mod_position: List<Int>? = null, mod_amount: Int = 0): Pair<Rational, Int> {
        return this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_leaf_offset_and_width(beat_key.beat, position, mod_position, mod_amount)
    }

    fun get_actual_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>> {
        val output = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_blocking_position(beat_key.beat, position)
        return if (output == null) {
            Pair(beat_key, position)
        } else {
            Pair(BeatKey(beat_key.channel, beat_key.line_offset, output.first), output.second)
        }
    }

    fun get_blocking_amount(beat_key: BeatKey, position: List<Int>): Rational? {
        return this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_blocking_amount(beat_key.beat, position)
    }

    fun blocked_check_remove_beat(index: Int, count: Int = 1) {
        val channels = this.get_all_channels()
        for (channel in channels.indices) {
            this._catch_blocked_tree_exception(channel) {
                channels[channel].blocked_check_remove_beat(index, count)
            }
        }
    }

    private fun <T> _catch_blocked_tree_exception(channel: Int, callback: () -> T): T {
        this._blocked_action_catcher += 1
        return try {
            val output = callback()
            this._blocked_action_catcher -= 1
            output
        } catch (e: OpusChannelAbstract.BlockedTreeException) {
            this._blocked_action_catcher -= 1
             this.on_action_blocked(
                 BeatKey(
                     channel,
                     e.line_offset,
                     e.e.blocker_beat
                 ),
                 e.e.blocker_position
             )
             throw BlockedActionException()
        } catch (e: OpusChannelAbstract.BlockedLineCtlTreeException) {
            this._blocked_action_catcher -= 1
            this.on_action_blocked_line_ctl(
                e.e.type,
                BeatKey(
                    channel,
                    e.line_offset,
                    e.e.e.blocker_beat,
                ),
                e.e.e.blocker_position
            )
            throw BlockedActionException()
        } catch (e: OpusChannelAbstract.BlockedCtlTreeException) {
            this._blocked_action_catcher -= 1
            this.on_action_blocked_channel_ctl(
                e.e.type,
                channel,
                e.e.e.blocker_beat,
                e.e.e.blocker_position
            )
            throw BlockedActionException()
        } catch (e: Exception) {
            this._blocked_action_catcher -= 1
            throw e
        }

        // global is just a OpusTreeArray.BlockedTree at this layer and is caught with catch_global_ctl_blocked_tree_exception
    }

    private fun <T> _catch_global_ctl_blocked_tree_exception(type: EffectType, callback: () -> T): T {
        return try {
            callback()
        } catch (e: ReducibleTreeArray.BlockedTreeException) {
            this.on_action_blocked_global_ctl(type, e.blocker_beat, e.blocker_position)
            throw BlockedActionException()
        }
    }

    fun is_line_ctl_visible(type: EffectType, channel: Int, line_offset: Int): Boolean {
        val line = this.get_all_channels()[channel].lines[line_offset]
        return line.controllers.has_controller(type) && line.get_controller<EffectEvent>(type).visible
    }

    fun is_channel_ctl_visible(type: EffectType, channel_index: Int): Boolean {
        val channel = this.get_all_channels()[channel_index]
        return channel.controllers.has_controller(type) && channel.get_controller<EffectEvent>(type).visible
    }

    fun is_global_ctl_visible(type: EffectType): Boolean {
        return this.controllers.has_controller(type) && this.get_controller<EffectEvent>(type).visible
    }

    open fun on_action_blocked(blocker_key: BeatKey, blocker_position: List<Int>) { }
    open fun on_action_blocked_global_ctl(type: EffectType, blocker_beat: Int, blocker_position: List<Int>) {}
    open fun on_action_blocked_channel_ctl(type: EffectType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {}
    open fun on_action_blocked_line_ctl(type: EffectType, blocker_key: BeatKey, blocker_position: List<Int>) {}

    fun get_visible_channels(): List<OpusChannelAbstract<*, *>> {
        return this.channels
    }

    /*
        Get the number of visible lines, control lines included
     */
    fun get_row_count(): Int {
        return this._cached_row_map.size
    }

    /*
        Given the row, get the line number in the Opus
     */
    fun get_ctl_line_from_row(row: Int): Int {
        return this._cached_row_map[row] ?: throw Exception("Row $row not found.")
    }

    fun get_visible_row_from_pair(channel: Int, line_offset: Int): Int {
        return this.get_visible_row_from_ctl_line(
            this.get_actual_line_index(
                this.get_instrument_line_index(channel, line_offset)
            )
        ) ?: -1
    }

    fun get_visible_row_from_ctl_line(line: Int): Int? {
        return this._cached_inv_visible_line_map[line]
    }

    fun get_visible_row_from_ctl_line_line(type: EffectType, channel: Int, line_offset: Int): Int {
        return this._cached_ctl_map_line[Triple(channel, line_offset, type)] ?: throw LineEffectRowNotVisible(type, channel, line_offset)
    }

    fun get_visible_row_from_ctl_line_channel(type: EffectType, channel: Int): Int {
        return this._cached_ctl_map_channel[Pair(channel, type)] ?: throw ChannelEffectRowNotVisible(type, channel)
    }

    fun get_visible_row_from_ctl_line_global(type: EffectType): Int {
        return this._cached_ctl_map_global[type] ?: throw GlobalEffectRowNotVisible(type)
    }

    override fun hashCode(): Int {
        var result = this.length
        result = ((result shl 5) + (result shr 27)).xor(this.controllers.hashCode())
        result = ((result shl 5) + (result shr 27)).xor(this.channels.hashCode())
        result = ((result shl 5) + (result shr 27)).xor((this.project_name?.hashCode() ?: 0))
        result = ((result shl 5) + (result shr 27)).xor(this.transpose.hashCode())
        result = ((result shl 5) + (result shr 27)).xor(this.tuning_map.contentHashCode())
        return result
    }

    override fun <T : EffectEvent> get_controller(type: EffectType): EffectController<T> {
        return this.controllers.get<T>(type)
    }
}
