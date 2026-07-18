package com.qfs.pagan

import com.qfs.apres.Midi
import com.qfs.apres.event.Balance
import com.qfs.apres.event.BalanceMSB
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.Text
import com.qfs.apres.event.Volume
import com.qfs.apres.event.VolumeMSB
import com.qfs.apres.event2.ControlChange
import com.qfs.apres.event2.FlexGenericText
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.event2.ProgramChangeMessage
import com.qfs.apres.event2.SetTempoMessage
import com.qfs.apres.event2.UMPEvent
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlin.Int
import kotlin.collections.iterator
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

abstract class CEvent(val tick: Int, val channel: Int? = null) {
    fun get_midi_event(version: Int = Midi.VERSION_1): Array<out Pair<Int, GeneralMIDIEvent>> {
        val events = when (version) {
            Midi.VERSION_1 -> this.get_v1()
            Midi.VERSION_2_CLIP -> this.get_v2()
            else -> TODO()
        }
        val ticks = this.get_ticks(events.size)
        return Array(events.size) { i -> Pair(ticks[i], events[i]) }
    }
    abstract fun get_v1(): Array<MIDIEvent>
    abstract fun get_v2(): Array<UMPEvent>
    open fun get_ticks(size: Int): Array<Int> {
        return Array(size) { this.tick }
    }
}

class TextEvent(tick: Int, val msg: String): CEvent(tick, null) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(Text(this.msg))
    }
    override fun get_v2(): Array<UMPEvent> {
        return arrayOf(FlexGenericText(this.msg))
    }
}

class NoteEvent(tick: Int, channel: Int, var note: Int, var bend: Int, var velocity: Int, var uuid: Int, var duration: Int): CEvent(tick, channel) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(
            NoteOn(
                this.channel!!,
                this.note,
                this.velocity
            ),
            NoteOff(
                this.channel,
                this.note,
                0x00
            )
        )
    }

    override fun get_v2(): Array<UMPEvent> {
        return arrayOf(
            NoteOn79(
                index = uuid,
                note = this.note,
                bend = this.bend,
                channel = this.channel!!,
                velocity = this.velocity
            ),
            NoteOff79(
                index = uuid,
                note = this.note,
                bend = this.bend,
                channel = this.channel!!,
                velocity = this.velocity
            )
        )
    }

    override fun get_ticks(size: Int): Array<Int> {
        return arrayOf(this.tick, this.tick + this.duration)
    }
}

class VolumeEvent(tick: Int, channel: Int, val value: Float): CEvent(tick, channel) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(VolumeMSB(this.channel!!, min((100 * this.value).roundToInt(), 127)))
    }

    override fun get_v2(): Array<UMPEvent> {
        return arrayOf(
            ControlChange.from_compound(Volume(this.channel!!, (this.value * 100).roundToInt()))
        )
    }
}

class TempoEvent(tick: Int, val bpm: Float): CEvent(tick, null) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(SetTempo.from_bpm(this.bpm))
    }

    override fun get_v2(): Array<UMPEvent> {
        return arrayOf(SetTempoMessage(this.bpm))
    }

}
class PanEvent(tick: Int, channel: Int, val pan: Float): CEvent(tick, channel) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(BalanceMSB(this.channel!!, min(((this.pan + 1F) * 64).roundToInt(), 127)))
    }

    override fun get_v2(): Array<UMPEvent> {
        return arrayOf(
            ControlChange.from_compound(Balance(this.channel!!, ((this.pan + 1F) * 64).roundToInt()))
        )
    }

}
class BeatPointer(tick: Int, val beat: Int): CEvent(tick, null) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(SongPositionPointer(this.beat))
    }

    override fun get_v2(): Array<UMPEvent> {
        return arrayOf() // TODO
    }

}
class ProgramChangeEvent(tick: Int, channel: Int, val bank: Int, val program: Int): CEvent(tick, channel) {
    override fun get_v1(): Array<MIDIEvent> {
        return arrayOf(
            BankSelect(this.channel!!, this.bank),
            ProgramChange(this.channel, this.program)
        )
    }

    override fun get_v2(): Array<UMPEvent> {
        return arrayOf(
            ProgramChangeMessage(
                this.channel!!,
                0, // Not sure about this.
                this.program,
                this.bank,
                true
            )
        )
    }
}


class ProjectToMIDIConverter {
    companion object {
        fun get_midi(opus_manager: OpusLayerBase, start_beat: Int = 0, end_beat: Int? = null, include_pointers: Boolean = false, version: Int = Midi.VERSION_1): Midi {
            data class StackItem<T>(var tree: ReducibleTree<T>, var divisions: Int, var offset: Int, var size: Int, var position: List<Int>)
            val cevents = mutableListOf<CEvent>()
            var event_uuid_gen = 0

            val midi = Midi()
            midi.ppqn = 480

            cevents.add(TextEvent(0, "Generated with Pagan Music Sequencer."))
            opus_manager.project_notes?.let {
                cevents.add(TextEvent(0,  it))
            }

            // Set default values
            for (i in opus_manager.channels.indices) {
                val channel = opus_manager.channels[i]
                if (channel.muted) continue
                cevents.add(VolumeEvent(0, i, 1F))
            }

            val max_tick = midi.get_ppqn() * (opus_manager.length + 1)
            val radix = opus_manager.tuning_map.size

            fun <U: EffectEvent> apply_active_controller(controller: EffectController<U>, gen_event_callback: (U, U?, Int, Int) -> List<CEvent>) {
                var skip_initial_set = false
                val initial_event = controller.get_initial_event()
                var latest_event = initial_event

                for (i in start_beat until (end_beat ?: opus_manager.length)) {
                    val working_tree = controller.get_tree(i)
                    val stack: MutableList<StackItem<U>> = mutableListOf(StackItem(working_tree, 1, (i - start_beat) * midi.ppqn, midi.ppqn, listOf()))
                    while (stack.isNotEmpty()) {
                        val current = stack.removeAt(0)
                        if (current.tree.has_event()) {
                            val event = current.tree.get_event()!!
                            if (current.offset == 0) {
                                skip_initial_set = true
                            }

                            for (event in gen_event_callback(event, latest_event, current.offset, current.size)) {
                                cevents.add(event)
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
                    val event = gen_event_callback(initial_event, null, 0, 0).first()
                    cevents.add(event)
                }
            }

            val tempo_controller = opus_manager.get_controller<OpusTempoEvent>(EffectType.Tempo)
            apply_active_controller(tempo_controller) { event: OpusTempoEvent, previous_event: OpusTempoEvent?, frame_offset: Int, frames: Int ->
                when (event.transition) {
                    //TempoEvent(frame_offset,  (event.value * 1000f).roundToInt() / 1000F),
                    EffectTransition.InstantB -> {
                        listOf(
                            TempoEvent(frame_offset,  event.value),
                            TempoEvent(frame_offset + frames, previous_event?.value ?: 120F)
                        )
                    }
                    else -> {
                        listOf(
                            TempoEvent(frame_offset,  event.value),
                        )
                    }
                }
            }

            val channels = opus_manager.get_all_channels()
            for (c in channels.indices) {
                val pan_controller = channels[c].get_controller<OpusPanEvent>(EffectType.Pan)
                val midi_channel = opus_manager.get_midi_channel(c)
                apply_active_controller(pan_controller) { event: OpusPanEvent, previous_event: OpusPanEvent?, frame_offset: Int, frames: Int ->
                    when (event.transition) {
                        EffectTransition.Instant -> {
                         //   val value = min(((event.value + 1F) * 64).toInt(), 127)
                            listOf(PanEvent(frame_offset, midi_channel, event.value))
                        }

                        EffectTransition.InstantB -> {
                            listOf(
                                PanEvent(frame_offset, midi_channel, event.value),
                                PanEvent(frame_offset + frames, midi_channel, previous_event?.value ?: 0F)
                            )
                        }

                        EffectTransition.LinearB,
                        EffectTransition.Linear -> {
                            val latest_value = (previous_event?.value ?: 0F)
                            val diff = (event.value - latest_value) / (frames * event.duration).toFloat()
                            val working_list = mutableListOf<PanEvent>()
                            var last_val: Float? = null
                            for (x in 0 until frames * event.duration) {
                                val mid_val = latest_value + (x * diff)
                                if (last_val != mid_val) {
                                    working_list.add(PanEvent(frame_offset + x, midi_channel, mid_val))
                                }
                                last_val = mid_val
                            }

                            // Restore original value after slide
                            if (event.transition == EffectTransition.LinearB) {
                                val value = previous_event?.value ?: 0F
                                working_list.add(PanEvent(frame_offset + (frames * event.duration), midi_channel, value))
                            }

                            working_list
                        }

                        EffectTransition.RLinear -> {
                            val latest_value = previous_event?.value ?: 0F
                            val diff = (latest_value - event.value) / (frames * event.duration).toFloat()
                            val working_list = mutableListOf<CEvent>()
                            var last_val: Float? = null
                            for (x in 0 until frames * event.duration) {
                                val value = event.value + (x * diff)
                                if (last_val != value) {
                                    working_list.add(PanEvent(frame_offset + x, midi_channel, value))
                                }
                                last_val = value
                            }
                            working_list
                        }
                    }
                }

                if (channels[c].controllers.has_controller(EffectType.Volume)) {
                    val volume_controller = channels[c].get_controller<OpusVolumeEvent>(EffectType.Volume)
                    apply_active_controller(volume_controller) { event: OpusVolumeEvent, previous_event: OpusVolumeEvent?, frame_offset: Int, frames: Int ->
                        when (event.transition) {
                            EffectTransition.Instant -> {
                                listOf(VolumeEvent(frame_offset, midi_channel, event.value))
                            }

                            EffectTransition.InstantB -> {
                                listOf(
                                    VolumeEvent(frame_offset, midi_channel, event.value),
                                    VolumeEvent(frame_offset + frames, midi_channel, previous_event?.value ?: 0F)
                                )
                            }

                            EffectTransition.LinearB,
                            EffectTransition.Linear -> {
                                val working_value = previous_event?.value ?: 1F
                                val diff = (event.value - working_value) / (frames * event.duration).toFloat()
                                val working_list = mutableListOf<CEvent>()
                                var last_val: Float? = null
                                for (x in 0 until frames * event.duration) {
                                    val value = working_value + (x * diff)
                                    if (last_val != value) {
                                        working_list.add(VolumeEvent(frame_offset + x, midi_channel, value))
                                    }
                                    last_val = value
                                }

                                // Restore original value after slide
                                if (event.transition == EffectTransition.LinearB) {
                                    val value = (previous_event?.value ?: 1F)
                                    working_list.add(VolumeEvent(frame_offset + (frames * event.duration), midi_channel, value))
                                }

                                working_list
                            }

                            EffectTransition.RLinear -> {
                                val latest_value = previous_event?.value ?: 1F
                                val diff = (latest_value - event.value) / (frames * event.duration).toFloat()
                                val working_list = mutableListOf<CEvent>()
                                var last_val: Float? = null
                                for (x in 0 until frames * event.duration) {
                                    val value = event.value + (x * diff)
                                    if (last_val != value) {
                                        working_list.add(VolumeEvent(frame_offset + x, midi_channel, value))
                                    }
                                    last_val = value
                                }
                                working_list
                            }
                        }
                    }
                }
            }

            fun parse_delay_map(delay_controller: EffectController<DelayEvent>): List<Pair<IntRange, DelayEvent>> {
                // NOTE: Assumes instant transition only
                val intermediary = mutableListOf<Pair<Int, DelayEvent>>()
                intermediary.add(Pair(0, delay_controller.initial_event))
                for (b in delay_controller.beats.indices) {
                    val beat_tree = delay_controller.beats[b]
                    val stack: MutableList<StackItem<DelayEvent>> = mutableListOf(StackItem(beat_tree, 1, midi.ppqn * b, midi.ppqn, listOf()))
                    while (stack.isNotEmpty()) {
                        val current = stack.removeAt(0)
                        if (current.tree.has_event()) {
                            if (!(b < start_beat || b >= (end_beat ?: opus_manager.length))) {
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

            fun get_delayed_pseudo_events(input_event: NoteEvent, delay_event_map: List<Pair<IntRange, DelayEvent>>?): List<NoteEvent> {
                if (delay_event_map == null) return listOf()
                val output = mutableListOf<NoteEvent>()

                var repeats_remaining = -1
                var echo_offset = input_event.tick
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
                            NoteEvent(
                                echo_offset,
                                input_event.channel!!,
                                input_event.note,
                                input_event.bend,
                                working_velocity,
                                event_uuid_gen++,
                                input_event.duration
                            )
                        )
                        echo_offset += delay_in_ticks
                        working_velocity = (working_velocity * attenuation).toInt()
                        repeats_remaining -= 1
                    }
                }

                return output
            }

            val global_delay_map = if (opus_manager.has_global_controller(EffectType.Delay)) {
                parse_delay_map(opus_manager.get_global_controller(EffectType.Delay))
            } else {
                null
            }

            var percussion_exported = false
            var working_midi_channel = 0
            for (c in opus_manager.channels.indices) {
                if (opus_manager.channels[c].muted) continue
                val channel = opus_manager.get_channel(c)

                val midi_channel = if (opus_manager.is_percussion(c)) {
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

                val channel_delay_map = if (opus_manager.has_channel_controller(EffectType.Delay, c)) {
                    parse_delay_map(opus_manager.get_channel_controller(EffectType.Delay, c))
                } else {
                    null
                }

                cevents.add(ProgramChangeEvent(0, midi_channel, channel.get_midi_bank(), channel.midi_program))

                for (l in channel.lines.indices) {
                    val line = channel.lines[l]
                    if (line.muted) continue

                    val line_delay_map = if (opus_manager.has_line_controller(EffectType.Delay, c, l)) {
                        parse_delay_map(opus_manager.get_line_controller(EffectType.Delay, c, l))
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
                                val (note, bend) = if (opus_manager.is_percussion(c)) { // Ignore the event data and use percussion map
                                    Pair(opus_manager.get_percussion_instrument(c, l) + 27, 0)
                                } else {
                                    val current_note = when (event) {
                                        is RelativeNoteEvent -> event.offset + prev_note
                                        is AbsoluteNoteEvent -> event.note
                                        else -> break
                                    }

                                    val octave = current_note / radix
                                    val offset = opus_manager.tuning_map[current_note % radix]

                                    // This offset is calculated so the tuning map always reflects correctly
                                    val transpose_offset = 12.0 * opus_manager.transpose.first.toDouble() / opus_manager.transpose.second.toDouble()
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

                                if (!(b < start_beat || b >= (end_beat ?: opus_manager.length))) {
                                    val event_velocity = (opus_manager.get_current_velocity(BeatKey(c, l, b), current.position) * 100F).toInt()
                                    val pseudo_event = NoteEvent(
                                        current.offset,
                                        midi_channel,
                                        note,
                                        bend,
                                        event_velocity,
                                        event_uuid_gen++,
                                        min((current.size * event.duration), max_tick),
                                    )

                                    val line_repeats = get_delayed_pseudo_events(pseudo_event, line_delay_map)
                                    val channel_repeats = get_delayed_pseudo_events(pseudo_event, channel_delay_map).toMutableList()
                                    for (working_event in line_repeats) {
                                        channel_repeats.addAll(get_delayed_pseudo_events(working_event, channel_delay_map))
                                    }
                                    val global_repeats = get_delayed_pseudo_events(pseudo_event, global_delay_map).toMutableList()
                                    for (working_event in channel_repeats) {
                                        global_repeats.addAll(get_delayed_pseudo_events(working_event, global_delay_map))
                                    }
                                    cevents.add(pseudo_event)
                                    for (working_event in (line_repeats + channel_repeats + global_repeats)) {
                                        cevents.add(working_event)
                                    }
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

                        if (!((b < start_beat || b >= (end_beat ?: opus_manager.length)))) {
                            current_tick += midi.ppqn
                        }
                    }
                }
            }

            if (include_pointers) {
                for (beat in start_beat until (end_beat ?: opus_manager.length)) {
                    cevents.add(
                        BeatPointer(midi.ppqn * (beat - start_beat), beat)
                    )
                }
            }

            cevents.sortBy { it.tick }

            for (event in cevents) {
                for ((tick, midi_event) in event.get_midi_event(version)) {
                    midi.insert_event(tick, midi_event)
                }
            }

            return midi
        }
    }
}
