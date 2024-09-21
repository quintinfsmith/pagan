package com.qfs.pagan.opusmanager

import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.json.*
import com.qfs.pagan.Rational
import com.qfs.pagan.jsoninterfaces.OpusManagerJSONInterface
import com.qfs.pagan.structure.OpusTree
import java.io.File
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
open class OpusLayerBase {
    class BadBeatKey(beat_key: BeatKey) : Exception("BeatKey $beat_key doesn't exist")
    class NonEventConversion(beat_key: BeatKey, position: List<Int>) : Exception("Attempting to convert non-event @ $beat_key:$position")
    class NonPercussionEventSet : Exception("Attempting to set normal event on percussion channel")
    class PercussionEventSet : Exception("Attempting to set percussion event on non-percussion channel")
    class BadInsertPosition : Exception("Can't insert tree at top level")
    class RemovingLastBeatException : Exception("OpusManager requires at least 1 beat")
    class IncompatibleChannelException(channel_old: Int, channel_new: Int) : Exception("Can't move lines into or out of the percussion channel ($channel_old -> $channel_new)")
    class RangeOverflow(from_key: BeatKey, to_key: BeatKey, startkey: BeatKey) : Exception("Range($from_key .. $to_key) @ $startkey overflows")
    class EventlessTreeException: Exception("Tree requires event for operation")
    class InvalidOverwriteCall: Exception()
    class InvalidMergeException: Exception()
    class RemovingRootException: Exception()
    class InvalidChannel(channel: Int) : Exception("Channel $channel doesn't exist")
    class NoteOutOfRange(n: Int) : Exception("Attempting to use unsupported note $n")

    class EmptyPath : Exception("Path Required but not given")
    class MixedInstrumentException(first_key: BeatKey, second_key: BeatKey): Exception("Can't mix percussion with non-percussion instruments here")

    companion object {
        private var _channel_uuid_generator: Int = 0x00
        fun gen_channel_uuid(): Int {
            return OpusLayerBase._channel_uuid_generator++
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

        private fun _tree_from_midi(midi: Midi): Triple<OpusTree<Set<Array<Int>>>, List<OpusTree<OpusControlEvent>>, List<Triple<Int, Int?, Int?>>> {
            var beat_size = midi.get_ppqn()
            var total_beat_offset = 0
            var last_ts_change = 0
            val beat_values: MutableList<OpusTree<Set<Array<Int>>>> = mutableListOf()
            val tempo_line = mutableListOf<OpusTree<OpusControlEvent>>()
            var max_tick = 0
            var working_tempo = 120F
            val instrument_map = mutableListOf<Triple<Int, Int?, Int?>>()

            val active_event_map = HashMap<Pair<Int,Int>, Array<Int>>()

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
                        var existing_event = active_event_map[Pair(channel, note)]!!
                        existing_event[2] = tick - existing_event[2]
                    }

                    // Add trees to list of trees
                    while (beat_values.size <= beat_index) {
                        val new_tree = OpusTree<Set<Array<Int>>>()
                        new_tree.set_size(beat_size)
                        beat_values.add(new_tree)
                    }

                    val tree = beat_values[beat_index]
                    val eventset = if (tree[inner_beat_offset].is_event()) {
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

                    // Need to resize the current beat to match the timesignature that change if noteons
                    // have already been added to that tree (shouldn't ever happen)
                    if (beat_index < beat_values.size) {
                        val original_beat_size = beat_size
                        val tree = beat_values[beat_index]
                        val tree_divisions = tree.divisions.toList()
                        tree.set_size(new_beat_size)
                        for ((f, child) in tree_divisions) {
                            if (!child.is_event()) {
                                continue
                            }
                            val new_index = f * new_beat_size / original_beat_size

                            val eventset = if (!tree[new_index].is_event()) {
                                mutableSetOf()
                            }  else {
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
                        val new_tree = OpusTree<OpusControlEvent>()
                        new_tree.set_size(beat_size)
                        tempo_line.add(new_tree)
                    }

                    val tree = tempo_line[beat_index]
                    tree[inner_beat_offset].set_event(OpusTempoEvent(working_tempo))
                } else if (event is ProgramChange) {
                    instrument_map.add(Triple(event.channel, null, event.get_program()))
                } else if (event is BankSelect) {
                    instrument_map.add(Triple(event.channel, event.value, null) )
                }
            }

            for ((_, opus_event) in active_event_map) {
                opus_event[2] = max_tick - opus_event[2]
            }

            active_event_map.clear()

            total_beat_offset += (max_tick - last_ts_change) / beat_size
            val opus = OpusTree<Set<Array<Int>>>()

            if (beat_values.isEmpty()) {
                for (i in 0 until 4) {
                    beat_values.add(OpusTree())
                }
            }

            opus.set_size(beat_values.size)

            var overflow_events = mutableSetOf<Array<Int>>()
            beat_values.forEachIndexed { i: Int, beat_tree: OpusTree<Set<Array<Int>>> ->
                // Quantize the beat ////////////
                val quantized_tree = OpusTree<Set<Array<Int>>>()
                quantized_tree.set_size(beat_tree.size)

                if (overflow_events.isNotEmpty()) {
                    quantized_tree[0].set_event(overflow_events.toSet())
                    overflow_events = mutableSetOf()
                }

                // Can easily merge quantized positions since the beats are still flat
                val qmap = beat_tree.get_quantization_map(listOf(2,2,2,3,5,7))
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
                        if (quantized_tree[new_position].is_event()) {
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

        fun from_midi(path: String): OpusLayerBase {
            val midi = Midi.from_path(path)
            return OpusLayerBase.from_midi(midi)
        }

        fun from_midi(midi: Midi): OpusLayerBase {
            val new_manager = OpusLayerBase()

            val (settree, tempo_line, instrument_map) = OpusLayerBase._tree_from_midi(midi)

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

                    if (event[0] == 9) {
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

                            if (i == insertion_index) { // passed all the checks, no need to keep looping
                                break
                            }
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

                            if (i == insertion_index) { // passed all the checks, no need to keep looping
                                break
                            }
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

            if (midi_channel_map.containsKey(9)) {
                // Add Percussion to channel_sizes list
                val adj_channel = midi_channel_map[9]!!
                while (adj_channel >= channel_sizes.size) {
                    channel_sizes.add(0)
                }

                for (blocks in blocked_percussion_ranges) {
                    channel_sizes[adj_channel] += blocks.size
                }

                // Move Percussion to Last Opus Manager Channel
                for ((mchannel, ochannel) in midi_channel_map) {
                    if (mchannel == 9) continue
                    if (ochannel > adj_channel) {
                        midi_channel_map[mchannel] = ochannel - 1
                    }
                }

                val percussion_line_count = channel_sizes.removeAt(adj_channel)
                midi_channel_map[9] = channel_sizes.size
                channel_sizes.add(percussion_line_count)
            } else {
                // If no percussion is found, add an empty percussion track
                midi_channel_map[9] = channel_sizes.size
                channel_sizes.add(1)
            }

            val sorted_channels = midi_channel_map.values.sortedBy { it }
            sorted_channels.forEachIndexed { i: Int, channel: Int ->
                if (i == sorted_channels.size - 1) {
                    for (j in 0 until channel_sizes[channel]) {
                        new_manager.percussion_channel.new_line()
                    }
                    return@forEachIndexed
                }
                new_manager.new_channel(lines = channel_sizes[channel])
            }

            new_manager.set_beat_count(settree.size)

            val events_to_set = mutableSetOf<Triple<BeatKey, List<Int>, Array<Int>>>()

            for ((position, event_set) in remapped_events) {
                val event_list = event_set.toMutableList()
                event_list.sortWith(compareBy { 127 - it.first[1] })
                for ((event, line_offset) in event_list) {
                    val event_channel = event[0]
                    val channel_index = midi_channel_map[event_channel]!!

                    // line_offset needs to be recalculated HERE for percussion as the percussion block map will change size during init
                    val adj_line_offset = if (event_channel == 9) {
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
                            if (new_manager.get_tree(working_beatkey!!, working_position).size != size) {
                                new_manager.split_tree(working_beatkey!!, working_position, size)
                            }
                            working_position.add(x)
                        }
                    }

                    if (working_beatkey != null) {
                        events_to_set.add(Triple(working_beatkey!!, working_position, event))
                    }
                }
            }

            for ((beatkey, position, event) in events_to_set) {
                if (event[0] == 9) {
                    new_manager.set_percussion_event(beatkey, position)
                    new_manager.set_duration(beatkey, position, event[2])
                } else {
                    val event_note = event[1] - 21
                    if (event_note in 0..127) {
                        new_manager.set_event(
                            beatkey,
                            position,
                            AbsoluteNoteEvent(
                                event_note,
                                event[2]
                            )
                        )
                    }
                }
            }

            val all_channels = List(new_manager.channels.size + 1) { i: Int ->
                if (i < new_manager.channels.size) {
                    new_manager.channels[i]
                } else {
                    new_manager.percussion_channel
                }
            }

            // Reduce
            all_channels.forEachIndexed { i: Int, channel: OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>> ->
                for (j in channel.lines.indices) {
                    for (k in 0 until new_manager.beat_count) {

                        val beat_tree = new_manager.get_tree(BeatKey(i, j, k), listOf())
                        val original_size = beat_tree.size
                        beat_tree.reduce()
                        beat_tree.traverse { working_tree: OpusTree<out InstrumentEvent>, event: InstrumentEvent? ->
                            if (event == null) {
                                return@traverse
                            }

                            var tmp_tree = beat_tree
                            var denominator = 1
                            val tree_position = working_tree.get_path()
                            for (p in tree_position) {
                                denominator *= tmp_tree.size
                                tmp_tree = tmp_tree[p]
                            }

                            // Not worrying too much about duration accuracy. would inevitably cause overly divided beats
                            // val leaf_ratio = 1f / denominator.toFloat() // (commented for clarity as to why I called the variable 'denominator', but don't use it as one)
                            event.duration = max(1, (event.duration * denominator.toFloat() / original_size).roundToInt())
                        }
                    }
                }
            }

            for ((midi_channel, bank, program) in instrument_map) {
                // Midi may have contained programchange event for channel, but no music
                val opus_channel = midi_channel_map[midi_channel] ?: continue
                if (bank != null) {
                    new_manager.set_channel_bank(opus_channel, bank)
                }
                if (program != null) {
                    new_manager.set_channel_program(opus_channel, program)
                }
            }

            for ((line_offset, instrument) in percussion_instrument_map) {
                new_manager.set_percussion_instrument(line_offset, instrument)
            }

            new_manager._setup_default_controllers()

            val tempo_controller = new_manager.controllers.get_controller(ControlEventType.Tempo)
            tempo_line.forEachIndexed { i: Int, tempo_tree: OpusTree<OpusControlEvent> ->
                // Limit the number of divisions in the tempo ctl line
                if (!tempo_tree.is_eventless()) {
                    var max_leafs = 0
                    for (channel in new_manager.channels) {
                        for (line in channel.lines) {
                            max_leafs = max(line.beats[i].get_total_child_weight(), max_leafs)
                        }
                    }

                    if (max_leafs < tempo_tree.get_total_child_weight()) {
                        tempo_tree.flatten()
                        val new_tree = OpusTree<OpusControlEvent>()
                        new_tree.set_size(max_leafs)

                        for ((index, child) in tempo_tree.divisions) {
                            if (child.is_event()) {
                                new_tree.set(index * max_leafs / tempo_tree.size, child)
                            }
                        }

                        new_tree.reduce()
                        tempo_controller.events[i] = new_tree
                    } else {
                        tempo_controller.events[i] = tempo_tree
                    }
                }
            }

            /*
                If the first leaf sets the tempo, use that as initial value instead of as a control event.
             */
            val first_tempo_tree = tempo_controller.get_tree(0)
            val position = first_tempo_tree.get_first_event_tree_position()
            val first_tempo_leaf = first_tempo_tree.get(position ?: listOf())

            if (first_tempo_leaf.is_event()) {
                tempo_controller.set_initial_event(first_tempo_leaf.event!!)
                first_tempo_leaf.unset_event()
            }

            return new_manager
        }

        fun initialize_basic(): OpusLayerBase {
            val new_manager = OpusLayerBase()
            new_manager.new_line(0) // Add percussion line
            new_manager.new_channel()
            new_manager.set_beat_count(4)
            new_manager.set_project_name(null)
            new_manager._setup_default_controllers()
            return new_manager
        }

        fun load(input: JSONHashMap): OpusLayerBase {
            val inner_map = input["d"] as JSONHashMap
            val opus_manager = OpusLayerBase()
            opus_manager.set_project_name(inner_map.get_stringn("title"))
            opus_manager.transpose = inner_map.get_int("transpose", 0)

            opus_manager.channels.clear()

            opus_manager.set_beat_count(inner_map.get_int("size"))
            for (generalized_channel in inner_map.get_list("channels").list) {
                opus_manager.add_channel(
                    OpusChannelJSONInterface.interpret(
                        generalized_channel as JSONHashMap,
                        opus_manager.beat_count
                    ) as OpusChannel
                )
            }
            opus_manager.percussion_channel = OpusChannelJSONInterface.interpret(
                inner_map.get_hashmap("percussion_channel"),
                opus_manager.beat_count
            ) as OpusPercussionChannel


            val generalized_tuning_map = inner_map.get_list("tuning_map")
            opus_manager.tuning_map = Array(generalized_tuning_map.list.size) { i: Int ->
                val g_pair = generalized_tuning_map.get_list(i)
                Pair(
                    g_pair.get_int(0),
                    g_pair.get_int(1)
                )
            }
            opus_manager.controllers = ActiveControlSetJSONInterface.from_json(inner_map.get_hashmap("controllers"), opus_manager.beat_count)

            return opus_manager
        }
    }

    private var _channel_uuid_map = HashMap<Int, OpusChannel>()

    var beat_count: Int = 1
    var controllers = ActiveControlSet(beat_count, setOf(ControlEventType.Tempo))
    var channels: MutableList<OpusChannel> = mutableListOf()
    var percussion_channel = OpusPercussionChannel()
    var path: String? = null
    var project_name: String? = null
    var transpose: Int = 0
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) }

    private var _cached_instrument_line_map = mutableListOf<Pair<Int, Int>>()
    private var _cached_std_line_map = HashMap<Pair<Int, Int>, Int>()

    // key: absolute line
    // Value: first is always a pointer to cached_abs_line_map, second and third are pointers to the relative ctl lines
    private var _cached_abs_line_map_map = mutableListOf<Triple<Int, CtlLineLevel?, ControlEventType?>>()
    private var _cached_inv_abs_line_map_map = HashMap<Int, Int>()

    //// RO Functions ////
    /**
     * Calculates the number of channels in use.
     */
    fun get_channel_count(): Int {
        return this.channels.size + 1
    }

    /**
     * Calculates the position of the first leaf in a given tree
     */
    fun get_first_position(beat_key: BeatKey, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_tree(beat_key, output)
        while (! tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    fun get_first_position_global_ctl(type: ControlEventType, beat: Int, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_global_ctl_tree(type, beat, output)
        while (! tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    fun get_first_position_channel_ctl(type: ControlEventType, channel: Int, beat: Int, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_channel_ctl_tree(type, channel, beat, output)
        while (! tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    fun get_first_position_line_ctl(type: ControlEventType, beat_key: BeatKey, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_line_ctl_tree(type, beat_key, output)
        while (! tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    fun is_valid(beat_key: BeatKey, position: List<Int>): Boolean {
        return try {
            this.get_tree(beat_key, position)
            true
        } catch (e: Exception) {
            false
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
        top_left_key.beat = max(0, min(top_left_key.beat, this.beat_count - 1))
        bottom_right_key.beat = max(0, min(bottom_right_key.beat, this.beat_count - 1))

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

            for (j in start .. end) {
                for (k in top_left_key.beat .. bottom_right_key.beat) {
                    output.add(BeatKey(i, j, k))
                }
            }
        }
        return output
    }

    /**
     * Calculate the x & y difference between two BeatKeys [beata] & [beatb]
     */
    open fun get_abs_difference(beata: BeatKey, beatb: BeatKey): Pair<Int, Int> {
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
    open fun get_percussion_instrument(line_offset: Int): Int {
        return this.percussion_channel.get_instrument(line_offset)
    }

    /**
     * Get the tree structure found within the BeatKey [beat_key] at [position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun get_percussion_tree(line_offset: Int, beat: Int, position: List<Int>? = null): OpusTree<PercussionEvent> {
        if (line_offset > this.percussion_channel.size) {
            throw BadBeatKey(BeatKey(this.channels.size, line_offset, beat))
        }
        return this.percussion_channel.get_tree(line_offset, beat, position ?: listOf())
    }

    /**
     * Get the tree structure found within the BeatKey [beat_key] at [position]
     * [position] defaults to null, indicating the root tree of the beat
     */
    fun get_tree(beat_key: BeatKey, position: List<Int>? = null): OpusTree<out InstrumentEvent> {
        val working_channel = try {
            this.get_channel(beat_key.channel)
        } catch (e: IndexOutOfBoundsException) {
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
    fun get_tree_copy(beat_key: BeatKey, position: List<Int>? = null): OpusTree<out InstrumentEvent> {
        // Because of the variance (out InstrumentEvent) my copy function in the OpusTree doesn't work correctly
        // Instead just copy the events here
        val working_tree = this.get_tree(beat_key, position).copy()
        working_tree.traverse { tree: OpusTree<out InstrumentEvent>, event: InstrumentEvent? ->
            if (event != null) {
                (tree as OpusTree<InstrumentEvent>).set_event(
                    event.copy()
                )
            }
        }
        return working_tree
    }

    fun get_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>? = null): OpusTree<OpusControlEvent> {
        if (channel >= this.channels.size) {
            throw InvalidChannel(channel)
        }
        return this.get_channel(channel).get_ctl_tree(
            type,
            beat,
            position ?: listOf()
        )
    }

    fun get_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>? = null): OpusTree<OpusControlEvent> {
        if (beat_key.channel >= this.channels.size) {
            throw BadBeatKey(beat_key)
        }

        val working_channel = this.get_channel(beat_key.channel)
        if (beat_key.line_offset > working_channel.size) {
            throw BadBeatKey(beat_key)
        }

        return working_channel.get_ctl_tree(
            beat_key.line_offset,
            type,
            beat_key.beat,
            position ?: listOf()
        )
    }

    fun get_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>? = null): OpusTree<OpusControlEvent> {
        return this.controllers.get_controller(type).get_tree(beat, position)
    }

    /**
     * Get the leaf immediately after the tree found at [beat_key]/[position], if any
     * *it may not be an immediate sibling, rather an aunt, niece, etc*
     */
    fun get_proceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<out InstrumentEvent>? {
        val pair = this.get_proceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    /**
     * Get the last event before the tree at [beat_key]/[position], if any exist.
     * This may or may not be in the preceding leaf, but will look for the first leaf with an associated event.
     */
    fun get_preceding_event(beat_key: BeatKey, position: List<Int>): InstrumentEvent? {
        // Gets first preceding event. may skip empty leafs
        var working_position = position.toList()
        var working_beat_key = beat_key
        while (!this.get_tree(working_beat_key, working_position).is_event()) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: return null
            working_beat_key = pair.first
            working_position = pair.second
        }
        return this.get_tree(working_beat_key, working_position).get_event()
    }

    fun get_proceding_event_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        val next = this.get_proceding_leaf_position(beat_key, position) ?: return null
        var working_beat_key = next.first
        var working_position = next.second
        var found_position: Pair<BeatKey, List<Int>>? = null
        while (found_position == null) {
            val working_tree = this.get_tree(working_beat_key, working_position)
            if (working_tree.is_event()) {
                found_position = Pair(working_beat_key, working_position)
            } else {
                val tmp = this.get_proceding_leaf_position(working_beat_key, working_position) ?: break
                working_beat_key = tmp.first
                working_position = tmp.second
            }
        }
        return if (found_position != null) {
            Pair(working_beat_key, working_position)
        } else {
            null
        }
    }

    /**
     * Get the leaf immediately before the tree found at [beat_key]/[position], if any
     * *it may not be an immediate sibling, rather an aunt, niece, etc*
     */
    fun get_preceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<out InstrumentEvent>? {
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
                    working_position.removeLast()
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
    fun get_proceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
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
                    working_position.removeLast()
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat_key.beat < this.beat_count - 1) {
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

    fun get_global_ctl_proceding_leaf_position(ctl_type: ControlEventType, beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        return this.controllers.get_controller(ctl_type).get_proceding_leaf_position(beat, position)
    }

    fun get_channel_ctl_proceding_leaf_position(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        return this.get_channel(channel).controllers.get_controller(ctl_type).get_proceding_leaf_position(beat, position)
    }

    fun get_line_ctl_proceding_leaf_position(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Pair<Int, List<Int>>? {
        return this.get_channel(beat_key.channel).lines[beat_key.line_offset].controllers.get_controller(ctl_type).get_proceding_leaf_position(beat_key.beat, position)
    }

    /**
     * Get the value of the event at location[beat_key]/[position], if any.
     * if the event is relative, it will look back and add up preceding values
     */
    open fun get_absolute_value(beat_key: BeatKey, position: List<Int>): Int? {
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

            if (working_tree.is_event()) {
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
     * Artifact. Checks if the [channel] is assigned to be used as percussion.
     * The last channel is now always used as percussion.
     */
    fun is_percussion(channel: Int): Boolean {
        return channel == this.channels.size
    }
    //// END RO Functions ////

    fun get_relative_value(beat_key: BeatKey, position: List<Int>): Int? {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw NonEventConversion(beat_key, position)
        }

        val event = tree.get_event()!!
        if (event is RelativeNoteEvent) {
            return event.offset
        }

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
    /**
     * Recalculate the event of the tree @ [beat_key]/[position]
     * to be relative to the events before it, if it isn't already
     *
     * @throws NonEventConversion If no event is present
     */
    open fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw NonEventConversion(beat_key, position)
        }

        val event = tree.get_event()!!
        if (event !is AbsoluteNoteEvent) {
            return
        }

        var working_beat_key: BeatKey = beat_key
        var working_position: List<Int> = position
        var preceding_value: Int? = null
        while (preceding_value == null) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            preceding_value = this.get_absolute_value(pair.first, pair.second)
            working_beat_key = pair.first
            working_position = pair.second
        }


        this.set_event(
            beat_key,
            position,
            RelativeNoteEvent(
                event.note - (preceding_value ?: 0),
                event.duration
            )
        )
    }

    /**
     * Recalculate the event of the tree @ [beat_key]/[position]
     * to not depend on preceding events
     *
     * @throws NonEventConversion If no event is present
     */
    open fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw NonEventConversion(beat_key, position)
        }
        val event = tree.get_event()!!
        if (event !is RelativeNoteEvent) {
            return
        }

        // The implied first value can be 0
        val value = this.get_absolute_value(beat_key, position) ?: event.offset
        val radix = this.tuning_map.size
        if (value < 0 || value > radix * 8) {
            throw NoteOutOfRange(value)
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

    /**
     * Insert a new tree @ [beat_key]/[position]
     *
     * @throws BadInsertPosition When attempting to insert a new tree next to a top-level tree
     */
    open fun insert(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }
        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_tree(beat_key, parent_position)

        val index = position.last()
        if (index > tree.size) {
            throw BadInsertPosition()
        }
        tree.insert(index)
    }

    open fun insert_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_line_ctl_tree(type, beat_key, parent_position)

        val index = position.last()
        tree.insert(index)
    }

    open fun insert_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_channel_ctl_tree(type, channel, beat, parent_position)

        val index = position.last()
        tree.insert(index)
    }

    open fun insert_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_global_ctl_tree(type, beat, parent_position)

        val index = position.last()
        tree.insert(index)
    }


    /**
     * Insert a new tree after [beat_key]/[position]
     *
     * @throws BadInsertPosition When attempting to insert a new tree next to a top-level tree
     */
    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val tree = this.get_tree(beat_key, position)
        val parent = tree.get_parent()!!

        val index = position.last()
        parent.insert(index + 1)
    }

    open fun insert_after_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_line_ctl_tree(type, beat_key, parent_position)

        val index = position.last()
        tree.insert(index + 1)
    }

    open fun insert_after_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_channel_ctl_tree(type, channel, beat, parent_position)

        val index = position.last()
        tree.insert(index + 1)
    }

    open fun insert_after_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_global_ctl_tree(type, beat, parent_position)

        val index = position.last()
        tree.insert(index + 1)
    }

    /**
     * Remove tree @ [beat_key]/[position] if it's not a top-level tree
     */
    open fun remove(beat_key: BeatKey, position: List<Int>) {
        // Can't remove beat
        if (position.isEmpty()) {
            throw RemovingRootException()
        }


        val tree = this.get_tree(beat_key, position)
        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            1 -> {
                throw Exception("SINGLE")
            }
            2 -> this.remove_one_of_two(beat_key, position)
            else -> this.remove_standard(beat_key, position)
        }
    }

    open fun remove_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw RemovingRootException()
        }
        val tree = this.get_channel_ctl_tree(type, channel, beat, position)

        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            2 -> this.remove_channel_ctl_one_of_two(type, channel, beat, position)
            else -> this.remove_channel_ctl_standard(type, channel, beat, position)
        }
    }

    open fun remove_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        // Can't remove beat
        if (position.isEmpty()) {
            throw RemovingRootException()
        }

        val tree = this.get_line_ctl_tree(type, beat_key, position)


        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            2 -> this.remove_line_ctl_one_of_two(type, beat_key, position)
            else -> this.remove_line_ctl_standard(type, beat_key, position)
        }
    }

    open fun remove_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw RemovingRootException()
        }

        val tree = this.get_global_ctl_tree(type, beat, position)

        val parent_tree = tree.parent!!
        when (parent_tree.size) {
            // 1 Shouldn't be able to happen and this isn't the place to check for that failure
            2 -> this.remove_global_ctl_one_of_two(type, beat, position)
            else -> this.remove_global_ctl_standard(type, beat, position)
        }
    }

    // remove_only, remove_one_of_two and remove_standard all exist so I could separate
    // them and use the "forget" wrapper at the History layer, while not breaking the LinksLayer
    open fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
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
        tree.detach()

        this.replace_tree(
            beat_key,
            position.subList(0, position.size - 1),
            replacer_tree
        )
    }

    open fun remove_line_ctl_one_of_two(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_line_ctl_tree(type, beat_key, position)
        val parent_tree = tree.parent!!
        tree.detach()
        val prev_position = position.toMutableList()
        prev_position.removeLast()
        val to_replace = parent_tree[0]
        this.replace_line_ctl_tree(
            type,
            beat_key,
            prev_position,
            to_replace
        )
    }

    open fun remove_channel_ctl_one_of_two(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        val tree = this.get_channel_ctl_tree(type, channel, beat, position)
        val parent_tree = tree.parent!!
        tree.detach()
        val prev_position = position.toMutableList()
        prev_position.removeLast()
        val to_replace = parent_tree[0]
        this.replace_channel_ctl_tree(
            type,
            channel,
            beat,
            prev_position,
            to_replace
        )
    }

    open fun remove_global_ctl_one_of_two(type: ControlEventType, beat: Int, position: List<Int>) {
        val tree = this.get_global_ctl_tree(type, beat, position)
        val parent_tree = tree.parent!!
        tree.detach()
        val prev_position = position.toMutableList()
        prev_position.removeLast()
        val to_replace = parent_tree[0]
        this.replace_global_ctl_tree(
            type,
            beat,
            prev_position,
            to_replace
        )
    }

    // remove_only, remove_one_of_two and remove_standard all exist so I could separate
    // them and use the "forget" wrapper at the History layer, while not breaking the LinksLayer
    open fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        tree.detach()
    }

    open fun remove_line_ctl_standard(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_line_ctl_tree(type, beat_key, position)
        tree.detach()
    }

    open fun remove_channel_ctl_standard(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        val tree = this.get_channel_ctl_tree(type, channel, beat, position)
        tree.detach()
    }

    open fun remove_global_ctl_standard(type: ControlEventType, beat: Int, position: List<Int>) {
        val tree = this.get_global_ctl_tree(type, beat, position)
        tree.detach()
    }

    open fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        if (!this.is_percussion(beat_key.channel)) {
            throw PercussionEventSet()
        }

        val tree = this.get_percussion_tree(beat_key.line_offset, beat_key.beat, position)
        if (tree.is_event()) {
            tree.unset_event()
        }

        tree.set_event(PercussionEvent())
    }

    open fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.percussion_channel.set_instrument(line_offset, instrument)
    }

    open fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        if (!this.is_percussion(channel)) {
            this.set_channel_bank(channel, instrument.first)
        }
        this.set_channel_program(channel, instrument.second)
    }

    private fun set_channel_program(channel: Int, program: Int) {
        this.get_channel(channel).set_midi_program(program)
    }

    private fun set_channel_bank(channel: Int, bank: Int) {
        if (this.is_percussion(channel)) {
            // TODO: Specify Exception
            throw Exception()
        } else {
            this.channels[channel].set_midi_bank(bank)
        }
    }

    open fun set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
        if (this.is_percussion(beat_key.channel)) {
            throw NonPercussionEventSet()
        }
        val tree = this.get_tree(beat_key, position) as OpusTree<InstrumentEvent>
        tree.set_event(event)
    }

    open fun set_line_ctl_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>, event: OpusControlEvent) {
        val tree = this.get_line_ctl_tree(type, beat_key, position)
        tree.set_event(event)
    }

    open fun set_channel_ctl_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, event: OpusControlEvent) {
        val tree = this.get_channel_ctl_tree(type, channel, beat, position)
        tree.set_event(event)
    }

    open fun set_global_ctl_event(type: ControlEventType, beat: Int, position: List<Int>, event: OpusControlEvent) {
        val tree = this.get_global_ctl_tree(type, beat, position)
        tree.set_event(event)
    }

    open fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean = false) {
        val tree: OpusTree<out InstrumentEvent> = this.get_tree(beat_key, position)
        this._split_opus_tree(tree, splits, move_event_to_end)
    }

    open fun split_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int) {
        val tree: OpusTree<OpusControlEvent> = this.get_line_ctl_tree(type, beat_key, position)
        this._split_opus_tree(tree, splits)
    }

    open fun split_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int) {
        val tree: OpusTree<OpusControlEvent> = this.get_channel_ctl_tree(type, channel, beat, position)
        this._split_opus_tree(tree, splits)
    }

    open fun split_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int) {
        val tree: OpusTree<OpusControlEvent> = this.get_global_ctl_tree(type, beat, position)
        this._split_opus_tree(tree, splits)
    }

    private fun <T> _split_opus_tree(tree: OpusTree<T>, splits: Int, move_event_to_end: Boolean = false) {
        if (tree.is_event()) {
            var working_tree = tree
            val event = working_tree.get_event()!!

            working_tree.unset_event()
            working_tree.set_size(splits)

            if (splits > 1) {
                working_tree = if (move_event_to_end) {
                    working_tree[working_tree.size - 1]
                } else {
                    working_tree[0]
                }
            }

            working_tree.set_event(event)
        } else {
            tree.set_size(splits)
        }
    }

    open fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        this._unset(tree)
    }
    
    open fun unset_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_line_ctl_tree(type, beat_key, position)
        this._unset(tree)
    }

    open fun unset_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        val tree = this.get_channel_ctl_tree(type, channel, beat, position)
        this._unset(tree)
    }

    open fun unset_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        val tree = this.get_global_ctl_tree(type, beat, position)
        this._unset(tree)
    }

    private fun <T> _unset(tree: OpusTree<T>) {
        tree.unset_event()
        tree.empty()

        if (tree.parent != null) {
            val index = tree.get_index()
            tree.parent!!.divisions.remove(index)
        }
    }

    private fun get_next_available_midi_channel(): Int {
        // Find the next available MIDI channel, ignore '9' which needs to be manually set
        // NOTE: This *will* generate past MIDI 1's limit of 16 channels
        val used_channels: MutableSet<Int> = mutableSetOf(9)
        for (channel in this.channels) {
            used_channels.add(channel.midi_channel)
        }
        var new_channel = 0
        while (new_channel in used_channels) {
            new_channel += 1
        }

        return new_channel
    }

    open fun new_channel(channel: Int? = null, lines: Int = 1, uuid: Int? = null) {
        val new_channel = OpusChannel(uuid ?: OpusLayerBase.gen_channel_uuid())
        new_channel.set_beat_count(this.beat_count)

        new_channel.midi_channel = this.get_next_available_midi_channel()

        this._channel_uuid_map[new_channel.uuid] = new_channel
        for (i in 0 until lines) {
            new_channel.new_line(i)
        }

        val new_channel_index = channel ?: this.channels.size
        this.channels.add(new_channel_index, new_channel)

        this.recache_line_maps()
    }

    open fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        if (this.is_percussion(channel_a) != this.is_percussion(channel_b)) {
            throw IncompatibleChannelException(channel_a, channel_b)
        }

        if (this.is_percussion(channel_a)) {
            val tmp_value = this.percussion_channel.lines[line_a].instrument
            this.percussion_channel.lines[line_a].instrument = this.percussion_channel.lines[line_b].instrument
            this.percussion_channel.lines[line_b].instrument = tmp_value
            val tmp_line = this.percussion_channel.lines[line_a]
            this.percussion_channel.lines[line_a] = this.percussion_channel.lines[line_b]
            this.percussion_channel.lines[line_b] = tmp_line
        } else {
            val tmp_line = this.channels[channel_a].lines[line_a]
            this.channels[channel_a].lines[line_a] = this.channels[channel_b].lines[line_b]
            this.channels[channel_b].lines[line_b] = tmp_line
        }
    }

    open fun insert_beats(beat_index: Int, count: Int) {
        for (i in 0 until count) {
            this.insert_beat(beat_index)
        }
    }

    open fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>? = null) {
        if (beat_index > this.beat_count) {
            throw IndexOutOfBoundsException()
        }
        this.beat_count += 1
        for (channel in this.channels) {
            channel.insert_beat(beat_index)
        }
        this.percussion_channel.insert_beat(beat_index)

        this.controllers.insert_beat(beat_index)

        if (beats_in_column == null) {
            return
        }

        var y = 0
        for (channel in this.channels) {
            for (line in channel.lines) {
                line.beats[beat_index] = beats_in_column[y] as OpusTree<TunedInstrumentEvent>
                y += 1
            }
        }
        for (line in this.percussion_channel.lines) {
            line.beats[beat_index] = beats_in_column.last() as OpusTree<PercussionEvent>
        }
    }

    open fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        if (line is OpusLine) {
            if (this.is_percussion(channel)) {
                // TODO: Specify Exception
                throw Exception()
            }
            this.channels[channel].insert_line(line_offset, line)
        } else if (line is OpusLinePercussion) {
            if (!this.is_percussion(channel)) {
                // TODO: SpecifyException
                throw Exception()
            }
            this.percussion_channel.insert_line(line_offset, line)
        }
        this.recache_line_maps()
    }

    open fun new_line(channel: Int, line_offset: Int? = null): OpusLineAbstract<*> {
        val working_channel = this.get_channel(channel)
        val output = working_channel.new_line(line_offset ?: working_channel.lines.size)
        this.recache_line_maps()
        return output
    }


    open fun remove_beat(beat_index: Int, count: Int = 1) {
        if (this.beat_count <= count) {
            throw RemovingLastBeatException()
        }

        if (beat_index < 0 || beat_index + count > this.beat_count) {
            throw IndexOutOfBoundsException()
        }

        for (i in 0 until count) {
            this.channels.forEachIndexed { c: Int, channel: OpusChannel ->
                channel.remove_beat(beat_index)
            }
            this.percussion_channel.remove_beat(beat_index)

            for (controller in this.controllers.controllers.values) {
                controller.remove_beat(beat_index)
            }
        }

        this.beat_count -= count
    }

    fun remove_channel_by_uuid(uuid: Int) {
        val channel = this._channel_uuid_map[uuid] ?: throw OpusChannel.InvalidChannelUUID(uuid)
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

    open fun remove_channel(channel: Int) {
        val opus_channel = this.channels.removeAt(channel)
        this._channel_uuid_map.remove(opus_channel.uuid)
        this.recache_line_maps()
    }

    open fun merge_leafs(beat_key_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        if (beat_key_from == beat_key_to && position_from == position_to) {
            throw InvalidMergeException()
        }

        val blocked_ranges = mutableSetOf<Pair<Rational, Rational>>()
        var working_position_pair = Pair(beat_key_from, this.get_first_position(beat_key_from, position_from))
        while (working_position_pair.first == beat_key_from && working_position_pair.second.size > position_from && working_position_pair.second.subList(0, position_from.size) == position_from) {
            val working_tree = this.get_tree(working_position_pair.first, working_position_pair.second)
            if (working_tree.is_event()) {
                val duration = working_tree.get_event()!!.duration
                var (working_offset, working_width) = this.get_leaf_offset_and_width(working_position_pair.first, working_position_pair.second)
                working_offset -= working_position_pair.first.beat
                val working_offset_end = working_offset + Rational(duration, working_width)
                blocked_ranges.add(Pair(working_offset, working_offset_end))
            }

            working_position_pair = this.get_proceding_leaf_position(working_position_pair.first, working_position_pair.second) ?: break

        }

        working_position_pair = Pair(beat_key_to, this.get_first_position(beat_key_to, position_to))
        while (working_position_pair.first == beat_key_to && working_position_pair.second.size > position_to && working_position_pair.second.subList(0, position_to.size) == position_to) {
            val working_tree = this.get_tree(working_position_pair.first, working_position_pair.second)
            if (working_tree.is_event()) {
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

            working_position_pair = this.get_proceding_leaf_position(working_position_pair.first, working_position_pair.second) ?: break
        }

        TODO("In Progress")

       // var (from_offset, from_width) = this.get_leaf_offset_and_width(beat_key_from, position_from)
       // var (to_offset, to_width) this.get_leaf_offset_and_width(beat_key_to, position_to)
       // TODO("In progress")

        this.replace_tree(beat_key_to, position_to, new_tree)
        this.unset(beat_key_from, position_from)
    }

    open fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_tree_copy(beatkey_from, position_from)
        this.unset(beatkey_from, position_from)
        this.replace_tree(beatkey_to, position_to, from_tree)
    }

    open fun move_global_ctl_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        val from_tree = this.get_global_ctl_tree(type, beat_from, position_from).copy(this::copy_control_event)
        this.unset_global_ctl(type, beat_from, position_from)
        this.replace_global_ctl_tree(type, beat_to, position_to, from_tree)
    }
    open fun move_channel_ctl_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        val from_tree = this.get_channel_ctl_tree(type, channel_from, beat_from, position_from).copy(this::copy_control_event)
        this.unset_channel_ctl(type, channel_from, beat_from, position_from)
        this.replace_channel_ctl_tree(type, channel_to, beat_to, position_to, from_tree)
    }
    open fun move_line_ctl_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_line_ctl_tree(type, beatkey_from, position_from).copy(this::copy_control_event)
        this.unset_line_ctl(type, beatkey_from, position_from)
        this.replace_line_ctl_tree(type, beatkey_to, position_to, from_tree)
    }

    open fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        val output = this.get_channel(channel).remove_line(line_offset)
        this.recache_line_maps()
        return output
    }

    open fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        if (this.is_percussion(beat_key.channel)) {
            this.percussion_channel.replace_tree(
                beat_key.line_offset,
                beat_key.beat,
                position,
                (tree as OpusTree<PercussionEvent>)
            )
        } else {
            this.channels[beat_key.channel].replace_tree(
                beat_key.line_offset,
                beat_key.beat,
                position,
                (tree as OpusTree<TunedInstrumentEvent>)
            )
        }
    }

    open fun replace_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusControlEvent>) {
        val tree_copy = tree.copy(this::copy_control_event)
        val controller = this.channels[beat_key.channel].lines[beat_key.line_offset].get_controller(type)
        controller.replace_tree(
            beat_key.beat,
            position ?: listOf(),
            tree_copy
        )
    }

    open fun replace_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>?, tree: OpusTree<OpusControlEvent>) {
        val tree_copy = tree.copy(this::copy_control_event)
        val controller = this.channels[channel].controllers.get_controller(type)
        controller.replace_tree(
            beat,
            position ?: listOf(),
            tree_copy
        )
    }

    open fun replace_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>?, tree: OpusTree<OpusControlEvent>) {
        val tree_copy = tree.copy(this::copy_control_event)
        val controller = this.controllers.get_controller(type)
        controller.replace_tree(
            beat,
            position ?: listOf(),
            tree_copy
        )
    }

    open fun set_beat_count(new_count: Int) {
        this.beat_count = new_count
        for (channel in this.channels) {
            channel.set_beat_count(new_count)
        }
        this.percussion_channel.set_beat_count(new_count)
        this.controllers.set_beat_count(new_count)
    }

    open fun get_midi(start_beat: Int = 0, end_beat_rel: Int? = null): Midi {
        data class StackItem<T>(var tree: OpusTree<T>, var divisions: Int, var offset: Int, var size: Int)
        data class PseudoMidiEvent(var channel: Int, var note: Int, var bend: Int, var velocity: Int, var uuid: Int)
        var event_uuid_gen = 0

        val end_beat = end_beat_rel ?: this.beat_count

        val midi = Midi()

        val pseudo_midi_map = mutableListOf<Triple<Int, PseudoMidiEvent, Boolean>>()
        val max_tick = midi.get_ppqn() * (this.beat_count + 1)
        val radix = this.tuning_map.size

        val tempo_controller = this.controllers.get_controller(ControlEventType.Tempo)

        var skip_initial_tempo_set = false
        for (i in start_beat until end_beat) {
            val tempo_tree = tempo_controller.get_tree(i)
            val stack: MutableList<StackItem<OpusControlEvent>> = mutableListOf(StackItem(tempo_tree, 1, (i - start_beat) * midi.ppqn, midi.ppqn))
            while (stack.isNotEmpty()) {
                val current = stack.removeFirst()
                if (current.tree.is_event()) {
                    val event = current.tree.get_event()!!
                    if (current.offset == 0) {
                        skip_initial_tempo_set = true
                    }
                    midi.insert_event(
                        0,
                        current.offset,
                        SetTempo.from_bpm(((event as OpusTempoEvent).value * 1000f).roundToInt() / 1000F)
                    )
                } else if (!current.tree.is_leaf()) {
                    val working_subdiv_size = current.size / current.tree.size
                    for ((j, subtree) in current.tree.divisions) {
                        stack.add(
                            StackItem(
                                subtree,
                                current.tree.size,
                                current.offset + (working_subdiv_size * j),
                                working_subdiv_size
                            )
                        )
                    }
                }
            }
        }

        if (!skip_initial_tempo_set) {
            val first_tempo_event = this.get_current_global_controller_event(ControlEventType.Tempo, start_beat, listOf()) as OpusTempoEvent
            midi.insert_event(
                0,
                0,
                SetTempo.from_bpm((first_tempo_event.value * 1000f).roundToInt() / 1000F)
            )
        }

        this.channels.forEachIndexed outer@{ c: Int, channel: OpusChannel ->
            midi.insert_event(
                0,
                0,
                BankSelect(channel.midi_channel, channel.midi_bank)
            )
            midi.insert_event(
                0,
                0,
                ProgramChange(channel.midi_channel, channel.midi_program)
            )
            channel.lines.forEachIndexed inner@{ l: Int, line: OpusLine ->
                // This only makes sense when volume controls aren't enabled (VOLCTLTMP)
                if ((line.get_controller(ControlEventType.Volume).initial_event as OpusVolumeEvent).value == 0) {
                    return@inner
                }
                var current_tick = 0
                var prev_note = 0
                line.beats.forEachIndexed { b: Int, beat_tree: OpusTree<TunedInstrumentEvent> ->
                    val stack: MutableList<StackItem<TunedInstrumentEvent>> = mutableListOf(StackItem(beat_tree, 1, current_tick, midi.ppqn))
                    while (stack.isNotEmpty()) {
                        val current = stack.removeFirst()
                        if (current.tree.is_event()) {
                            val event = current.tree.get_event()!!
                            val (note, bend) = if (this.is_percussion(c)) { // Ignore the event data and use percussion map
                                Pair(this.get_percussion_instrument(l) + 27, 0)
                            } else {
                                val current_note = when (event) {
                                    is RelativeNoteEvent -> {
                                        event.offset + prev_note
                                    }
                                    is AbsoluteNoteEvent -> {
                                        event.note
                                    }
                                    else -> break
                                }

                                val octave = current_note / radix
                                val offset = this.tuning_map[current_note % radix]

                                // This offset is calculated so the tuning map always reflects correctly
                                val transpose_offset = 12.0 * this.transpose.toDouble() / radix.toDouble()
                                val std_offset = (offset.first.toDouble() * 12.0 / offset.second.toDouble())

                                val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()

                                prev_note = current_note

                                Pair(
                                    (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21,
                                    bend
                                )
                            }

                            if (!(b < start_beat || b >= end_beat)) {
                                val pseudo_event = PseudoMidiEvent(
                                    channel.midi_channel,
                                    note,
                                    bend,
                                    (line.get_controller(ControlEventType.Volume).initial_event as OpusVolumeEvent).value,
                                    event_uuid_gen++
                                )
                                pseudo_midi_map.add(Triple(
                                    current.offset,
                                    pseudo_event,
                                    true
                                ))
                                pseudo_midi_map.add(Triple(
                                    min(current.offset + (current.size * event.duration), max_tick),
                                    pseudo_event,
                                    false
                                ))
                            }
                        } else if (!current.tree.is_leaf()) {
                            val working_subdiv_size = current.size / current.tree.size
                            for ((i, subtree) in current.tree.divisions) {
                                stack.add(
                                    StackItem(
                                        subtree,
                                        current.tree.size,
                                        current.offset + (working_subdiv_size * i),
                                        working_subdiv_size
                                    )
                                )
                            }
                        }
                    }

                    if (!(b < start_beat || b >= end_beat)) {
                        current_tick += midi.ppqn
                    }
                }
            }
        }

        // Handle Percussion Channel
        val channel = this.percussion_channel
        midi.insert_event(0, 0, BankSelect(9, channel.get_midi_bank()))
        midi.insert_event(0, 0, ProgramChange(9, channel.get_midi_program()))
        channel.lines.forEachIndexed inner@{ l: Int, line: OpusLinePercussion ->
            // This only makes sense when volume controls aren't enabled (VOLCTLTMP)
            if ((line.get_controller(ControlEventType.Volume).initial_event as OpusVolumeEvent).value == 0) {
                return@inner
            }

            var current_tick = 0
            line.beats.forEachIndexed { b: Int, beat_tree: OpusTree<PercussionEvent> ->
                val stack: MutableList<StackItem<PercussionEvent>> = mutableListOf(StackItem(beat_tree, 1, current_tick, midi.ppqn))
                while (stack.isNotEmpty()) {
                    val current = stack.removeFirst()
                    if (current.tree.is_event()) {
                        val event = current.tree.get_event()!!

                        if (!(b < start_beat || b >= end_beat)) {
                            val pseudo_event = PseudoMidiEvent(
                                9,
                                line.instrument + 27,
                                0,
                                (line.get_controller(ControlEventType.Volume).initial_event as OpusVolumeEvent).value,
                                event_uuid_gen++
                            )
                            pseudo_midi_map.add(Triple(
                                current.offset,
                                pseudo_event,
                                true
                            ))
                            pseudo_midi_map.add(Triple(
                                min(current.offset + (current.size * event.duration), max_tick),
                                pseudo_event,
                                false
                            ))
                        }
                    } else if (!current.tree.is_leaf()) {
                        val working_subdiv_size = current.size / current.tree.size
                        for ((i, subtree) in current.tree.divisions) {
                            stack.add(
                                StackItem(
                                    subtree,
                                    current.tree.size,
                                    current.offset + (working_subdiv_size * i),
                                    working_subdiv_size
                                )
                            )
                        }
                    }
                }

                if (!(b < start_beat || b >= end_beat)) {
                    current_tick += midi.ppqn
                }
            }
        }

        pseudo_midi_map.sortBy {
            (it.first * 2) + (if (it.third) { 1 } else { 0})
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

        for (beat in start_beat .. end_beat) {
            midi.insert_event(
                0,
                midi.ppqn * (beat - start_beat),
                SongPositionPointer(beat)
            )
        }

        return midi
    }


    open fun save(path: String? = null) {
        if (path == null && this.path == null) {
            throw EmptyPath()
        }

        if (path != null) {
            this.path = path
        }

        val file_obj = File(this.path!!)

        val generalized_object = OpusManagerJSONInterface.generalize(this)
        file_obj.writeText(generalized_object.to_string())
    }

    // Clear function is used for new projects
    open fun clear() {
        this.beat_count = 0
        this.channels.clear()
        this.path = null
        this.project_name = null
        this.tuning_map = Array(12) {
            i: Int -> Pair(i, 12)
        }
        this.percussion_channel.clear()
        this.transpose = 0
        this.controllers.clear()
    }

    /* Needs to be called by interface after new()/load()/import_midi() */
    open fun on_project_changed() { }

    fun load_path(path: String) {
        val json_content = File(path).readBytes()
        this.load(json_content, path)
    }

    open fun load(bytes: ByteArray, new_path: String? = null) {
        val json_content = bytes.toString(Charsets.UTF_8)
        val generalized_object = JSONParser.parse(json_content) as JSONHashMap
        val version = OpusManagerJSONInterface.detect_version(generalized_object)
        this.load_json(
            when (version) {
                OpusManagerJSONInterface.LATEST_VERSION -> generalized_object
                2 -> {
                    OpusManagerJSONInterface.convert_v2_to_v3(
                        generalized_object
                    )
                }
                1 -> {
                    OpusManagerJSONInterface.convert_v2_to_v3(
                        OpusManagerJSONInterface.convert_v1_to_v2(
                            generalized_object
                        )
                    )
                }
                0 ->  {
                    OpusManagerJSONInterface.convert_v2_to_v3(
                        OpusManagerJSONInterface.convert_v1_to_v2(
                            OpusManagerJSONInterface.convert_v0_to_v1(
                                generalized_object
                            )
                        )
                    )
                }
                else -> {
                    // *Unreachable
                    throw Exception()
                }
            }
        )

        this.path = new_path
    }

    open fun new() {
        this.import_from_other(OpusLayerBase.initialize_basic())
    }

    open fun load_json(json_data: JSONHashMap) {
        val input_manager = OpusLayerBase.load(json_data)
        this.import_from_other(input_manager)
        this._setup_default_controllers()
    }

    private fun _setup_default_controllers() {
        for (channel in this.channels) {
            for (line in channel.lines) {
                if (line.controllers.size() == 0) {
                    line.controllers.new_controller(ControlEventType.Volume)
                }
            }
            if (channel.controllers.size() == 0) {
                channel.controllers.new_controller(ControlEventType.Volume)
            }
        }

        for (line in this.percussion_channel.lines) {
            if (line.controllers.size() == 0) {
                line.controllers.new_controller(ControlEventType.Volume)
            }
        }
        if (this.percussion_channel.controllers.size() == 0) {
            this.percussion_channel.controllers.new_controller(ControlEventType.Volume)
        }

        if (this.controllers.size() == 0) {
            this.controllers.new_controller(ControlEventType.Tempo)
        }
        this.recache_line_maps()
    }

    fun import_midi(path: String) {
        this.import_from_other(OpusLayerBase.from_midi(path))
    }

    open fun import_midi(midi: Midi) {
        this.import_from_other(OpusLayerBase.from_midi(midi))
    }

    open fun set_project_name(new_name: String?) {
        this.project_name = new_name
    }

    open fun set_transpose(new_transpose: Int) {
        this.transpose = new_transpose
    }

    fun get_line_volume(channel: Int, line_offset: Int): Int {
        return (this.get_line_controller_initial_event(ControlEventType.Volume, channel, line_offset) as OpusVolumeEvent).value
    }

    internal fun _get_beatkeys_from_range(beat_key: BeatKey, from_key: BeatKey, to_key: BeatKey): List<BeatKey> {
        if (! this.is_valid_beat_range(from_key, to_key)) {
            throw RangeOverflow(from_key, to_key, beat_key)
        }

        val (y_diff, x_diff) = this.get_abs_difference(from_key, to_key)

        if (this.beat_count <= beat_key.beat + x_diff) {
            throw RangeOverflow(from_key, to_key, beat_key)
        }

        val (target_channel, target_offset) = try {
            this.get_channel_and_line_offset(this.get_instrument_line_index(beat_key.channel, beat_key.line_offset) + y_diff)
        } catch (e: IndexOutOfBoundsException) {
            throw RangeOverflow(from_key, to_key, beat_key)
        }

        val target_second_key = BeatKey(target_channel, target_offset, beat_key.beat + x_diff)


        return this.get_beatkeys_in_range(beat_key, target_second_key)
    }

    fun copy_control_event(event: OpusControlEvent?): OpusControlEvent? {
        return event?.copy()
    }

    open fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_corner, second_corner)

        val original_keys = this.get_beatkeys_in_range(from_key, to_key)
        val target_keys = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        for (i in original_keys.indices) {
            if (this.is_percussion(original_keys[i].channel) != this.is_percussion(target_keys[i].channel)) {
                throw MixedInstrumentException(original_keys[i], target_keys[i])
            }
        }

        // First, get the trees to copy. This prevents errors if the beat_key is within the two corner range
        val trees = mutableListOf<OpusTree<out InstrumentEvent>>()
        for (o_key in original_keys) {
            trees.add(this.get_tree_copy(o_key))
        }

        for (i in trees.indices) {
            this.replace_tree(
                target_keys[i],
                null,
                trees[i]
            )
        }
    }

    open fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_corner, second_corner)

        val original_keys = this.get_beatkeys_in_range(from_key, to_key)
        val target_keys = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        for (i in original_keys.indices) {
            if (this.is_percussion(original_keys[i].channel) != this.is_percussion(target_keys[i].channel)) {
                throw MixedInstrumentException(original_keys[i], target_keys[i])
            }
        }

        // First, get the trees to copy. This prevents errors if the beat_key is within the two corner range
        val trees = mutableListOf<OpusTree<out InstrumentEvent>>()
        for (o_key in original_keys) {
            trees.add(this.get_tree_copy(o_key))
        }

        for (clear_key in original_keys) {
            this.unset(clear_key, listOf())
        }

        for (clear_key in target_keys) {
            this.unset(clear_key, listOf())
        }

        for (i in target_keys.indices) {
            this.replace_tree(
                target_keys[i],
                null,
                trees[i]
            )
        }


    }

    fun is_valid_beat_range(first_corner: BeatKey, second_corner: BeatKey): Boolean {
        return if (this.channels.size + 1 <= first_corner.channel) {
            false
        } else if (this.get_channel(first_corner.channel).size <= first_corner.line_offset) {
            false
        } else if (this.beat_count <= first_corner.beat) {
            false
        } else if (this.channels.size + 1 <= second_corner.channel) {
            false
        } else if (this.get_channel(second_corner.channel).size <= second_corner.line_offset) {
            false
        } else if (this.beat_count <= second_corner.beat) {
            false
        } else {
            true
        }
    }

    private fun _copy_global_ctl_range(type: ControlEventType, target: Int, start: Int, end: Int, unset_original: Boolean = false) {
        if (target + (end - start) >= this.beat_count) {
            throw IndexOutOfBoundsException()
        }

        val overwrite_map = HashMap<Int, OpusTree<OpusControlEvent>>()
        val controller = this.controllers.get_controller(type)

        for (i in start .. end) {
            overwrite_map[target + (i - start)] = controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.unset_global_ctl(type, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.replace_global_ctl_tree(type, beat, null, tree)
        }
    }

    open fun overwrite_global_ctl_range(type: ControlEventType, target: Int, start: Int, end: Int) {
        this._copy_global_ctl_range(type, target, start, end, false)
    }

    open fun move_global_ctl_range(type: ControlEventType, target: Int, start: Int, end: Int) {
        this._copy_global_ctl_range(type, target, start, end, true)
    }

    private fun _copy_channel_ctl_range(type: ControlEventType, target_channel: Int, target_beat: Int, original_channel: Int, start: Int, end: Int, unset_original: Boolean) {
        if (target_beat + (end - start) >= this.beat_count) {
            throw IndexOutOfBoundsException()
        }

        val overwrite_map = HashMap<Int, OpusTree<OpusControlEvent>>()
        val original_controller = this.channels[original_channel].controllers.get_controller(type)

        for (i in start .. end) {
            overwrite_map[target_beat + (i - start)] = original_controller.get_tree(i).copy(this::copy_control_event)
            if (unset_original) {
                this.unset_channel_ctl(type, original_channel, i, listOf())
            }
        }

        for ((beat, tree) in overwrite_map) {
            this.replace_channel_ctl_tree(type, target_channel, beat, null, tree)
        }
    }

    open fun overwrite_channel_ctl_range(type: ControlEventType, target_channel: Int, target_beat: Int, original_channel: Int, start: Int, end: Int) {
        this._copy_channel_ctl_range(
            type,
            target_channel,
            target_beat,
            original_channel,
            start,
            end,
            false
        )
    }

    open fun move_channel_ctl_range(type: ControlEventType, target_channel: Int, target_beat: Int, original_channel: Int, start: Int, end: Int) {
        this._copy_channel_ctl_range(
            type,
            target_channel,
            target_beat,
            original_channel,
            start,
            end,
            true
        )
    }

    private fun _copy_line_ctl_range(type: ControlEventType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_corner, second_corner)

        val original_keys = this.get_beatkeys_in_range(from_key, to_key)
        val target_keys = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        for (i in original_keys.indices) {
            if (this.is_percussion(original_keys[i].channel) != this.is_percussion(target_keys[i].channel)) {
                throw MixedInstrumentException(original_keys[i], target_keys[i])
            }
        }

        // First, get the trees to copy. This prevents errors if the beat_key is within the two corner range
        val trees = mutableListOf<OpusTree<OpusControlEvent>>()
        for (o_key in original_keys) {
            trees.add(this.get_line_ctl_tree(type, o_key).copy(this::copy_control_event))
        }

        for (i in target_keys.indices) {
            this.replace_line_ctl_tree(
                type,
                target_keys[i],
                null,
                trees[i]
            )
        }

        if (unset_original) {
            for (clear_key in (original_keys - target_keys)) {
                this.unset_line_ctl(type, clear_key, listOf())
            }
        }
    }

    open fun overwrite_line_ctl_range(type: ControlEventType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._copy_line_ctl_range(type, beat_key, first_corner, second_corner, false)
    }

    open fun move_line_ctl_range(type: ControlEventType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._copy_line_ctl_range(type, beat_key, first_corner, second_corner, true)
    }

    open fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        for (selected_key in this.get_beatkeys_in_range(first_corner, second_corner)) {
            this.unset(selected_key, listOf())
        }
    }

    open fun unset_global_ctl_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        for (i in first_beat .. second_beat) {
            this.unset_global_ctl(type, i, listOf())
        }
    }

    open fun unset_channel_ctl_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        for (i in first_beat .. second_beat) {
            this.unset_channel_ctl(type, channel, i, listOf())
        }
    }

    open fun unset_line_ctl_range(type: ControlEventType, first_corner: BeatKey, second_corner: BeatKey) {
        for (selected_key in this.get_beatkeys_in_range(first_corner, second_corner)) {
            this.unset_line_ctl(type, selected_key, listOf())
        }
    }

    open fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw EventlessTreeException()
        }
        tree.event!!.duration = duration
    }


    // Calling this function every time a channel/line is modified should still be more efficient
    // than calculating offsets as needed
    open fun recache_line_maps() {
        this._cached_instrument_line_map.clear()
        this._cached_std_line_map.clear()
        var y = 0
        this.channels.forEachIndexed { channel_index: Int, channel: OpusChannel ->
            for (line_offset in channel.lines.indices) {
                val keypair = Pair(channel_index, line_offset)
                this._cached_instrument_line_map.add(keypair)
                this._cached_std_line_map[keypair] = y
                y += 1
            }
        }

        for (line_offset in this.percussion_channel.lines.indices) {
            val keypair = Pair(this.channels.size, line_offset)
            this._cached_instrument_line_map.add(keypair)
            this._cached_std_line_map[keypair] = y
            y += 1
        }

        this._cached_abs_line_map_map.clear()
        this._cached_inv_abs_line_map_map.clear()
        // STD Channels ---------------------
        this.channels.forEachIndexed { channel_index: Int, channel: OpusChannel ->
            for (line_offset in channel.lines.indices) {
                val keypair = Pair(channel_index, line_offset)
                this._cached_inv_abs_line_map_map[this._cached_std_line_map[keypair]!!] = this._cached_abs_line_map_map.size
                this._cached_abs_line_map_map.add(Triple(this._cached_std_line_map[keypair]!!, null, null))

                for ((type, _) in channel.lines[line_offset].controllers.controllers) {
                    this._cached_abs_line_map_map.add(
                        Triple(
                            this._cached_std_line_map[keypair]!!,
                            CtlLineLevel.Line,
                            type
                        )
                    )
                }
            }
            for (type in channel.controllers.controllers.keys) {
                this._cached_abs_line_map_map.add(
                    Triple(
                        channel_index,
                        CtlLineLevel.Channel,
                        type
                    )
                )
            }
        }

        // PERCUSSION Channel ---------------------------
        for (line_offset in this.percussion_channel.lines.indices) {
            val keypair = Pair(this.channels.size, line_offset)
            this._cached_inv_abs_line_map_map[this._cached_std_line_map[keypair]!!] = this._cached_abs_line_map_map.size
            this._cached_abs_line_map_map.add(Triple(this._cached_std_line_map[keypair]!!, null, null))

            for ((type, _) in this.percussion_channel.lines[line_offset].controllers.controllers) {
                this._cached_abs_line_map_map.add(
                    Triple(
                        this._cached_std_line_map[keypair]!!,
                        CtlLineLevel.Line,
                        type
                    )
                )
            }
        }
        for (type in this.percussion_channel.controllers.controllers.keys) {
            this._cached_abs_line_map_map.add(
                Triple(
                    this.channels.size,
                    CtlLineLevel.Channel,
                    type
                )
            )
        }
        // ------------------------------------------------------
        for (type in this.controllers.controllers.keys) {
            this._cached_abs_line_map_map.add(
                Triple(
                    -1,
                    CtlLineLevel.Global,
                    type
                )
            )
        }
    }

    open fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        val beat_keys = this._get_beat_keys_for_overwrite_beat_range_horizontally(first_key, second_key)
        for (key_list in beat_keys) {
            if (key_list.isEmpty()) {
                continue
            }
            for (i in 1 until key_list.size) {
                val overwrite_key = key_list[i]
                this.replace_tree(
                    overwrite_key,
                    null,
                    this.get_tree_copy(key_list[0])
                )
            }
        }
    }

    open fun _get_beat_keys_for_overwrite_beat_range_horizontally(first_key: BeatKey, second_key: BeatKey): List<List<BeatKey>> {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)
        val width = (to_key.beat - from_key.beat) + 1
        val count = ((this.beat_count - from_key.beat) / width) - 1
        val beat_keys = this.get_beatkeys_in_range(from_key, to_key)

        return List(beat_keys.size) { i: Int ->
            val beat_key = beat_keys[i]

            List(count + 1) { j: Int ->
                if (j == 0) {
                    beat_key
                } else {
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset,
                        beat_key.beat + (j * width)
                    )
                }
            }
        }
    }

    open fun overwrite_global_ctl_range_horizontally(type: ControlEventType, first_beat: Int, second_beat: Int) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        val width = (end - start) + 1
        val count = ((this.beat_count - start) / width) - 1
        for (i in 0 until width) {
            for (j in 0 until count) {
                this.replace_global_ctl_tree(
                    type,
                    ((j + 1) * width) + (i + start),
                    null,
                    this.get_global_ctl_tree(type, (i + start)).copy(this::copy_control_event)
                )
            }
        }
    }

    open fun overwrite_line_ctl_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)

        val width = (to_key.beat - from_key.beat) + 1
        val count = ((this.beat_count - from_key.beat) / width) - 1
        val beat_keys = this.get_beatkeys_in_range(from_key, to_key)
        for (beat_key in beat_keys) {
            val working_tree = this.get_line_ctl_tree(type, beat_key)
            for (i in 0 until count) {
                val to_overwrite = BeatKey(
                    beat_key.channel,
                    beat_key.line_offset,
                    beat_key.beat + ((i + 1) * width)
                )
                this.replace_line_ctl_tree(type, to_overwrite, null, working_tree.copy(this::copy_control_event))
            }
        }
    }

    open fun overwrite_channel_ctl_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        val start = min(first_beat, second_beat)
        val end = max(first_beat, second_beat)

        val width = (end - start) + 1
        val count = ((this.beat_count - start) / width) - 1
        for (i in 0 until width) {
            for (j in 0 until count) {
                this.replace_channel_ctl_tree(
                    type,
                    channel,
                    ((j + 1) * width) + (i + start),
                    null,
                    this.get_channel_ctl_tree(type, channel, (i + start)).copy(this::copy_control_event)
                )
            }
        }
    }


    open fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey) {
        if (beat_key.channel != channel || beat_key.line_offset != line_offset) {
            throw InvalidOverwriteCall()
        }
        val working_key = BeatKey(channel, line_offset, beat_key.beat + 1)
        for (x in beat_key.beat + 1 until this.beat_count) {
            working_key.beat = x
            this.replace_tree(working_key, null, this.get_tree_copy(beat_key))
        }
    }
    open fun _get_beat_keys_for_overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey): List<BeatKey> {
        val working_key = BeatKey(channel, line_offset, beat_key.beat + 1)
        return List<BeatKey>(this.beat_count - beat_key.beat) { i: Int ->
            working_key.beat = i + beat_key.beat
            working_key
        }

    }

    open fun overwrite_global_ctl_line(type: ControlEventType, beat: Int) {
        val original_tree = this.get_global_ctl_tree(type, beat)
        for (i in beat + 1 until this.beat_count) {
            this.replace_global_ctl_tree(type, i, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun overwrite_channel_ctl_line(type: ControlEventType, target_channel: Int, original_channel: Int, original_beat: Int) {
        if (target_channel != original_channel) {
            throw InvalidOverwriteCall()
        }

        val original_tree = this.get_channel_ctl_tree(type, original_channel, original_beat)
        for (i in original_beat + 1 until this.beat_count) {
            this.replace_channel_ctl_tree(type, target_channel, i, null, original_tree.copy(this::copy_control_event))
        }
    }

    open fun overwrite_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int, beat_key: BeatKey) {
        if (beat_key.channel != channel || beat_key.line_offset != line_offset) {
            throw InvalidOverwriteCall()
        }
        val working_key = BeatKey(channel, line_offset, beat_key.beat + 1)
        val original_tree = this.get_line_ctl_tree(type, beat_key)
        for (x in beat_key.beat + 1 until this.beat_count) {
            working_key.beat = x
            this.replace_line_ctl_tree(type, working_key, null, original_tree.copy(this::copy_control_event))
        }
    }

    /*
        Checks if the tuning falls within 12-tone equal-temper tuning.
        this means a tuning_map with less than 12 entries is still standard if
        every entry is also ((some number) / 12)
     */
    fun is_tuning_standard(): Boolean {
        val actuals = List<Double>(12) { i: Int ->
            i.toDouble() / 12.0
        }
        for ((numerator, denominator) in this.tuning_map) {
            if (!actuals.contains(numerator.toDouble() / denominator.toDouble())) {
                return false
            }
        }

        return true
    }

    open fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean = true) {
        val previous_radix = this.tuning_map.size

        val original = this.tuning_map
        this.tuning_map = new_map.clone()

        if (! mod_events || new_map.size == original.size) {
            return
        }

        val radix = new_map.size

        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusLine ->
                line.beats.forEachIndexed { k: Int, beat_tree: OpusTree<TunedInstrumentEvent> ->
                    beat_tree.traverse { tree: OpusTree<TunedInstrumentEvent>, event: TunedInstrumentEvent? ->
                        if (event == null) {
                            return@traverse
                        }

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

                            else -> {
                                return@traverse
                            }
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

    fun has_global_controller(type: ControlEventType): Boolean {
        return this.controllers.has_controller(type)
    }

    fun add_global_ctl_line(type: ControlEventType) {
        this.controllers.new_controller(type)
        this.recache_line_maps()
    }

    fun remove_global_ctl_line(type: ControlEventType) {
        this.controllers.remove_controller(type)
        this.recache_line_maps()
    }

    fun has_channel_controller(type: ControlEventType, channel: Int): Boolean {
        return this.channels[channel].controllers.has_controller(type)
    }

    fun add_channel_ctl_line(type: ControlEventType, channel: Int) {
        this.channels[channel].controllers.new_controller(type)
        this.recache_line_maps()
    }

    fun remove_channel_ctl_line(type: ControlEventType, channel: Int) {
        this.channels[channel].controllers.remove_controller(type)
        this.recache_line_maps()
    }

    fun has_line_controller(type: ControlEventType, channel: Int, line_offset: Int): Boolean {
        return this.channels[channel].lines[line_offset].controllers.has_controller(type)
    }

    fun add_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this.get_channel(channel).lines[line_offset].controllers.new_controller(type)
        this.recache_line_maps()
    }

    fun remove_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this.get_channel(channel).lines[line_offset].controllers.remove_controller(type)
        this.recache_line_maps()
    }

    fun ctl_line_level(y: Int): CtlLineLevel? {
        return this._cached_abs_line_map_map[y].second
    }

    fun get_ctl_line_type(y: Int): ControlEventType? {
        return this._cached_abs_line_map_map[y].third
    }

    fun get_ctl_line_info(y: Int): Triple<Int, CtlLineLevel?, ControlEventType?> {
        return this._cached_abs_line_map_map[y]
    }

    fun get_actual_line_index(abs: Int): Int {
        return this._cached_inv_abs_line_map_map[abs]!!
    }

    fun get_current_line_controller_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>): OpusControlEvent {
        val controller = this.get_channel(beat_key.channel).lines[beat_key.line_offset].controllers.get_controller(type)
        return controller.get_latest_event(beat_key.beat, position)
    }

    fun get_current_channel_controller_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>): OpusControlEvent {
        val controller = this.get_channel(channel).controllers.get_controller(type)
        return controller.get_latest_event(beat, position)
    }

    fun get_current_global_controller_event(type: ControlEventType, beat: Int, position: List<Int>): OpusControlEvent {
        val controller = this.controllers.get_controller(type)
        return controller.get_latest_event(beat, position)
    }

    open fun set_global_controller_initial_event(type: ControlEventType, event: OpusControlEvent) {
        val controller = this.controllers.get_controller(type)
        controller.initial_event = event
    }

    open fun set_channel_controller_initial_event(type: ControlEventType, channel: Int, event: OpusControlEvent) {
        val controller = this.get_channel(channel).controllers.get_controller(type)
        controller.initial_event = event
    }

    open fun set_line_controller_initial_event(type: ControlEventType, channel: Int, line_offset: Int, event: OpusControlEvent) {
        val controller = this.get_channel(channel).lines[line_offset].controllers.get_controller(type)
        controller.initial_event = event
    }
    fun get_channel(channel: Int): OpusChannelAbstract<*, *> {
        return if (this.is_percussion(channel)) {
            this.percussion_channel
        } else {
            this.channels[channel]
        }
    }
    fun get_line_controller_initial_event(type: ControlEventType, channel: Int, line_offset: Int): OpusControlEvent {
        val controller = this.get_channel(channel).lines[line_offset].controllers.get_controller(type)
        return controller.initial_event
    }

    fun get_channel_controller_initial_event(type: ControlEventType, channel: Int): OpusControlEvent {
        val controller = this.get_channel(channel).controllers.get_controller(type)
        return controller.initial_event
    }

    fun get_global_controller_initial_event(type: ControlEventType): OpusControlEvent {
        val controller = this.controllers.get_controller(type)
        return controller.initial_event
    }

    fun has_percussion(): Boolean {
        return !this.percussion_channel.is_empty()
    }

    fun import_from_other(other: OpusLayerBase) {
        this.clear()
        this.beat_count = other.beat_count
        this.channels = other.channels
        this.path = other.path
        this.project_name = other.project_name
        this.tuning_map = other.tuning_map.clone()
        this.transpose = other.transpose
        this._cached_instrument_line_map = other._cached_instrument_line_map
        this._cached_std_line_map = other._cached_std_line_map
        this.controllers = other.controllers
        this.percussion_channel = other.percussion_channel
    }

    open fun new_global_controller(type: ControlEventType) {
        this.controllers.new_controller(type)
        this.recache_line_maps()
    }


    override fun equals(other: Any?): Boolean {
        if (other !is OpusLayerBase
            || this.beat_count != other.beat_count
            || this.path != other.path
            || this.project_name != other.project_name
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

        return this.percussion_channel == other.percussion_channel
    }

    fun add_channel(channel: OpusChannel) {
        channel.uuid = OpusLayerBase.gen_channel_uuid()
        this.channels.add(channel)
        this._channel_uuid_map[channel.uuid] = channel
    }


    // Experimental/ not in use -yet ----------vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // /*
    //     Reduce the number of beats in a project without losing any information.
    //  */
    // fun squish(factor: Int) {
    //     for (channel in this.channels) {
    //         channel.squish(factor)
    //     }

    //     this.beat_count = ceil(this.beat_count.toDouble() / factor.toDouble()).toInt()
    // }

    // fun get_maximum_simultaneous_notes(): Int {
    //     return (this.get_press_breakdown().sortedBy { it.second }).last().second
    // }

    // /*
    //     Get the most usually active number of notes
    //  */
    // fun get_mode_simultaneous_notes(): Pair<Int, Double> {
    //     val merged_counts = HashMap<Int, Double>()
    //     for ((percentage, press_count) in this.get_press_breakdown()) {
    //         merged_counts[press_count] = merged_counts.getOrDefault(press_count, 0.0) + percentage
    //     }
    //     var mode_press_count = 0
    //     var mode_percentage = 0.0
    //     for ((press_count, percentage) in merged_counts) {
    //         if ((percentage == mode_percentage && mode_press_count < press_count) || percentage > mode_percentage) {
    //             mode_press_count = press_count
    //             mode_percentage = percentage
    //         }
    //     }
    //     return Pair(mode_press_count, mode_percentage)
    // }

    // fun find_like_range(top_left: BeatKey, bottom_right: BeatKey): List<Pair<BeatKey, BeatKey>> {
    //     val match_box = this.get_abs_difference(top_left, bottom_right)

    //     val matched_keys = this.get_beatkeys_in_range(top_left, bottom_right).toMutableList()
    //     val match_values = mutableListOf<OpusTree<InstrumentEvent>>()
    //     for (key in matched_keys) {
    //         match_values.add(this.get_tree(key))
    //     }

    //     val possible_corners = mutableListOf<BeatKey>()
    //     val top_corner_value = this.get_tree(top_left)
    //     // First get keys that *could* be matches of the top corner
    //     this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
    //         channel.lines.forEachIndexed { j: Int, line: OpusLine ->
    //             for (k in line.beats.indices) {
    //                 val working_key = BeatKey(i, j, k)
    //                 if (working_key in matched_keys) {
    //                     continue
    //                 }

    //                 if (top_corner_value == this.get_tree(working_key)) {
    //                     try {
    //                         this.get_std_offset(this.get_abs_offset(i, j) + match_box.first)
    //                         if (k + match_box.second < this.beat_count) {
    //                             possible_corners.add(working_key)
    //                         }
    //                     } catch (e: java.lang.IndexOutOfBoundsException) {
    //                         continue
    //                     }
    //                 }
    //             }
    //         }
    //     }

    //     val output = mutableListOf<Pair<BeatKey, BeatKey>>()

    //     for (working_top_corner in possible_corners) {
    //         val bottom_corner_pair = this.get_std_offset(this.get_abs_offset(working_top_corner.channel, working_top_corner.line_offset) + match_box.first)
    //         val working_bottom_corner = BeatKey(
    //             bottom_corner_pair.first,
    //             bottom_corner_pair.second,
    //             working_top_corner.beat + match_box.second
    //         )
    //         val working_keys = this.get_beatkeys_in_range(working_top_corner, working_bottom_corner)
    //         var is_match = true

    //         working_keys.forEachIndexed { i: Int, key: BeatKey ->
    //             is_match = (key !in matched_keys) && (this.get_tree(key) == match_values[i])
    //             if (!is_match) {
    //                 return@forEachIndexed
    //             }
    //         }

    //         if (is_match) {
    //             for (key in working_keys) {
    //                 matched_keys.add(key)
    //             }
    //             output.add(Pair(working_top_corner, working_bottom_corner))
    //         }
    //     }

    //     return output
    // }
    //
    // private fun get_press_breakdown(): List<Pair<Double, Int>> {
    //     val tick_map = mutableListOf<Pair<Double, Boolean>>()
    //     for (channel in this.channels) {
    //         for (line in channel.lines) {
    //             line.beats.forEachIndexed { beat_index: Int, beat_tree: OpusTree<InstrumentEvent> ->
    //                 beat_tree.traverse { tree: OpusTree<InstrumentEvent>, event: InstrumentEvent? ->
    //                     if (event == null) {
    //                         return@traverse
    //                     }

    //                     var tmp_tree = tree
    //                     val position = mutableListOf<Int>()
    //                     while (tmp_tree != beat_tree) {
    //                         position.add(0, tmp_tree.get_index()!!)
    //                         tmp_tree = tmp_tree.get_parent()!!
    //                     }

    //                     var position_scalar: Double = 0.0
    //                     tmp_tree = beat_tree
    //                     var running_size = 1

    //                     for (p in position) {
    //                         running_size *= tmp_tree.size
    //                         position_scalar += p.toDouble() / running_size.toDouble()
    //                         tmp_tree = tmp_tree[p]
    //                     }

    //                     tick_map.add(
    //                         Pair(
    //                             position_scalar + beat_index,
    //                             true
    //                         )
    //                     )

    //                     tick_map.add(
    //                         Pair(
    //                             position_scalar + beat_index + (event.duration.toDouble() / running_size.toDouble()),
    //                             false
    //                         )
    //                     )
    //                 }
    //             }
    //         }
    //     }

    //     tick_map.sortBy { it.first }
    //     val breakdown = mutableListOf<Pair<Double, Int>>()

    //     var currently_on = 0
    //     var last_position = 0.0
    //     for ((position, state) in tick_map) {
    //         if (position != last_position) {
    //             breakdown.add(Pair((position - last_position) / (this.beat_count + 1).toDouble(), currently_on))
    //         }

    //         if (state) {
    //             currently_on += 1
    //         } else {
    //             currently_on -= 1
    //         }

    //         last_position = position
    //     }

    //     return breakdown
    // }
    //
    // fun convert_all_events_to_absolute() {
    //     for (channel in this.channels) {
    //         for (line in channel.lines) {
    //             for (beat_tree in line.beats) {
    //                 var previous_value = 0
    //                 beat_tree.traverse { _: OpusTree<InstrumentEvent>, event: InstrumentEvent? ->
    //                     if (event == null) {
    //                         return@traverse
    //                     }
    //                     if (event.relative) {
    //                         event.relative = false
    //                         event.note += previous_value
    //                     }

    //                     previous_value = event.note
    //                 }
    //             }
    //         }
    //     }
    // }

    fun get_all_channels(): List<OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>>> {
        return List(this.channels.size + 1) { i: Int ->
            if (i < this.channels.size) {
                this.channels[i]
            } else {
                this.percussion_channel
            }
        }
    }

    internal fun get_leaf_offset_and_width(beat_key: BeatKey, position: List<Int>, mod_position: List<Int>? = null, mod_amount: Int = 0): Pair<Rational, Int> {
        /* use mod amount/mod_position to calculate size if a leaf were removed or added */

        var working_tree = this.get_tree(beat_key)
        var output = Rational(0, 1)
        var width_denominator = 1
        for (i in position.indices) {
            var p = position[i]
            var new_width_factor = working_tree.size
            if (mod_position != null) {
                if (i == mod_position.size - 1) {
                    if (p >= mod_position[i]) {
                        p += mod_amount
                    }
                    new_width_factor += mod_amount
                }
            }
            width_denominator *= new_width_factor
            output += Rational(p, width_denominator)
            working_tree = working_tree[position[i]]
        }

        output += beat_key.beat
        return Pair(output, width_denominator)
    }

}
