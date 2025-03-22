package com.qfs.apres

import com.qfs.apres.event2.UMPEvent
import com.qfs.apres.event.EndOfClip
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event2.DeltaClockStamp
import com.qfs.apres.event2.EndOfClip
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.event2.SetTempoMessage
import com.qfs.apres.event2.StartOfClip
import java.io.File
import kotlin.experimental.or

class StandardMidiFileInterface {
    class InvalidChunkType(string: String): Exception("Invalid Chunk Type: $string")
    class MissingMThd : Exception("Missing MThd")
    companion object {
        fun is_compatible(bytes: ByteArray): Boolean {
            return try {
                val first_four = ByteArray(4) { i -> bytes[i] }
                first_four.contentEquals("MThd".toByteArray())
            } catch (e: Exception) {
                false
            }
        }

        fun from_bytes(file_bytes: ByteArray): Midi {
            var active_byte: Byte = 0x90.toByte()
            if (!StandardMidiFileInterface.is_compatible(file_bytes)) {
                val first_four = ByteArray(4) { i -> file_bytes[i] }
                throw InvalidChunkType(first_four.toString())
            }

            val working_bytes = file_bytes.toMutableList()
            val mlo = Midi()
            var sub_bytes: MutableList<Byte>
            val chunkcount = HashMap<String, Int>()
            var current_clip = 0
            var current_deltatime: Int
            var chunk_type: String

            var divword: Int
            var midi_format: Int
            var clip_length: Int
            var found_header = false
            var ppqn = 120

            while (working_bytes.isNotEmpty()) {
                chunk_type = ""
                for (i in 0 until 4) {
                    chunk_type = "${chunk_type}${working_bytes.removeAt(0).toInt().toChar()}"
                }

                if (chunkcount.containsKey(chunk_type)) {
                    val value = chunkcount[chunk_type]!!
                    chunkcount[chunk_type] = value + 1
                } else {
                    chunkcount[chunk_type] = 1
                }

                when (chunk_type) {
                    "MThd" -> {
                        dequeue_n(working_bytes, 4) // Get Size
                        midi_format = dequeue_n(working_bytes, 2)
                        dequeue_n(working_bytes, 2) // Get Number of clips
                        divword = dequeue_n(working_bytes, 2)

                        if (divword and 0x8000 > 0) {
                            //TODO: (from rust) handle divword > 0x8000
                        } else {
                            ppqn = (divword and 0x7FFF)
                        }

                        mlo.set_ppqn(ppqn)
                        mlo.set_format(midi_format)
                        found_header = true
                    }
                    "MTrk" -> {
                        if (! found_header) {
                            throw MissingMThd()
                        }
                        current_deltatime = 0
                        clip_length = dequeue_n(working_bytes, 4)
                        sub_bytes = mutableListOf()

                        for (i in 0 until track_length) {
                            sub_bytes.add(working_bytes.removeAt(0))
                        }

                        while (sub_bytes.isNotEmpty()) {
                            current_deltatime += get_variable_length_number(sub_bytes)
                            active_byte = this.process_mtrk_event(mlo, sub_bytes, current_deltatime, current_clip, active_byte)
                        }

                        current_clip += 1
                    }
                    else -> {
                        throw InvalidChunkType(chunk_type)
                    }
                }
            }
            return mlo
        }

        fun to_bytes(midi: Midi): ByteArray {
            val output: MutableList<Byte> = mutableListOf(
                'M'.code.toByte(),
                'T'.code.toByte(),
                'h'.code.toByte(),
                'd'.code.toByte(),
                0.toByte(),
                0.toByte(),
                0.toByte(),
                6.toByte()
            )

            val format = midi.get_format()
            output.add((format / 256).toByte())
            output.add((format % 256).toByte())

            val clip_count = midi.count_clips()
            output.add((clip_count / 256).toByte())
            output.add((clip_count % 256).toByte())

            val ppqn = midi.get_ppqn()
            output.add((ppqn / 256).toByte())
            output.add((ppqn % 256).toByte())

            var clip_event_bytes: MutableList<Byte>
            var clip_byte_length: Int
            val clips = midi.get_clips()

            for (ticks in clips) {
                output.add('M'.code.toByte())
                output.add('T'.code.toByte())
                output.add('r'.code.toByte())
                output.add('k'.code.toByte())

                clip_event_bytes = mutableListOf()
                var has_eot = false
                for (pair in ticks) {
                    val tick_delay = pair.first
                    val eid = pair.second
                    val working_event = midi.get_event(eid)
                    if (working_event != null) {
                        has_eot = has_eot || (working_event is EndOfClip)
                        clip_event_bytes += to_variable_length_bytes(tick_delay)
                        clip_event_bytes += working_event.as_bytes().toMutableList()
                    }
                }

                // Automatically handle EndOfClipEvent Here instead of requiring it to be in the MIDIClip object
                if (!has_eot) {
                    clip_event_bytes.add(0x00)
                    clip_event_bytes += EndOfClip().as_bytes().toMutableList()
                }

                // clip length in bytes
                clip_byte_length = clip_event_bytes.size
                output.add((clip_byte_length shr 24).toByte())
                output.add(((clip_byte_length shr 16) and 0xFF).toByte())
                output.add(((clip_byte_length shr 8) and 0xFF).toByte())
                output.add((clip_byte_length and 0xFF).toByte())
                output += clip_event_bytes.toList()
            }

            return output.toByteArray()
        }

        fun process_mtrk_event(midi: Midi, bytes: MutableList<Byte>, current_deltatime: Int, clip: Int, active_byte: Byte): Byte {
            var adj_active_byte = if (bytes.first() in 0x80..0xEF) {
                bytes.first()
            } else {
                active_byte
            }

            val event: GeneralMIDIEvent? = event_from_bytes(bytes, adj_active_byte)
            if (event != null) {
                val first_byte = toUInt(event.as_bytes().first())
                if (first_byte in 0x90..0xEF) {
                    adj_active_byte = event.as_bytes().first()
                } else if (event is NoteOff) {
                    adj_active_byte = 0x10.toByte() or event.as_bytes().first()
                }
            }
            midi.insert_event(clip, current_deltatime, event!!)

            return adj_active_byte
        }
    }
}

class MidiContainerFileInterface {
    companion object {
        fun is_compatible(bytes: ByteArray): Boolean {
            return try {
                val signature = ByteArray(8) { i -> bytes[i] }
                signature.contentEquals("SMF2CON1".toByteArray())
            } catch (e: Exception) {
                false
            }
        }
        fun from_bytes(bytes: ByteArray): Midi {
        }
        fun to_bytes(midi: Midi): ByteArray {
        }
    }
}

class MidiClipFileInterface {
    companion object {
        fun is_compatible(bytes: ByteArray): Boolean {
            return try {
                val signature = ByteArray(8) { i -> bytes[i] }
                signature.contentEquals("SMF2CLIP".toByteArray())
            } catch (e: Exception) {
                false
            }
        }
        fun from_bytes(bytes: ByteArray): Midi {}
        fun to_bytes(midi: Midi, clip_index: Int = 0): ByteArray {
            val output = "SMF2CLIP".toByteArray().toMutableList()
            output += listOf(0x00, 0x40, 0x00, 0x00)

            val ppqn = midi.get_ppqn()
            output.add(0x00)
            output.add(0x30)
            output.add((ppqn / 256).toByte())
            output.add((ppqn % 256).toByte())

            output += SetTempoMessage(120F).as_bytes()
            output += listOf(0x00, 0x40, 0x00, 0x00)
            output += StartOfClip().as_bytes()


            val ticks = midi.get_clips()[clip_index]

            val clip_event_bytes = mutableListOf()
            var has_eot = false
            for (pair in ticks) {
                val tick_delay = pair.first
                val eid = pair.second
                val working_event = midi.get_event(eid)
                if (working_event != null) {
                    has_eot = has_eot || (working_event is EndOfClip)
                    clip_event_bytes += DeltaClockStamp(tick_delay)
                    // TODO: handle v1 / ump
                    clip_event_bytes += working_event.as_bytes().toMutableList()
                }
            }

            // Automatically handle EndOfClipEvent Here instead of requiring it to be in the MIDIClip object
            if (!has_eot) {
                clip_event_bytes += DeltaClockStamp(0)
                clip_event_bytes += EndOfClip().as_bytes().toMutableList()
            }

            // clip length in bytes
            val clip_byte_length = clip_event_bytes.size
            output.add((clip_byte_length shr 24).toByte())
            output.add(((clip_byte_length shr 16) and 0xFF).toByte())
            output.add(((clip_byte_length shr 8) and 0xFF).toByte())
            output.add((clip_byte_length and 0xFF).toByte())
            output += clip_event_bytes.toList()


            return output.toByteArray()
        }
    }
}


class Midi {
    class ClipOOB(index: Int): Exception("Clip $index Out of Bounds")
    class InvalidEventId(id: Int): Exception("No event mapped to id:$id")
    class UnknownMidiFileType: Exception()
    var ppqn: Int = 120
    var midi_format: Int = 1

    private var events = HashMap<Int, GeneralMIDIEvent>()
    private var event_id_gen: Int = 1
    private var event_positions = HashMap<Int, Pair<Int, Int>>()


    companion object {
        const val VERSION_1 = 0
        const val VERSION_2_CONTAINER = 1
        const val VERSION_2_CLIP = 2
        fun from_path(file_path: String): Midi {
            val midibytes = File(file_path).readBytes()
            return Midi.from_bytes(midibytes)
        }

        fun from_bytes(file_bytes: ByteArray): Midi {
            if (StandardMidiFileInterface.is_compatible(file_bytes)) {
                return StandardMidiFileInterface.from_bytes(file_bytes)
            }
            if (MidiContainerFileInterface.is_compatible(file_bytes)) {
                return MidiContainerFileInterface.from_bytes(file_bytes)
            }
            if (MidiClipFileInterface.is_compatible(file_bytes)) {
                return MidiClipFileInterface.from_bytes(file_bytes)
            }

            throw UnknownMidiFileType()
        }
    }

    fun as_bytes(version: Int? = null): ByteArray {
        val adj_version = if (version == null) {
            this.detect_version()
        } else {
            version
        }
        return when (adj_version) {
            Midi.VERSION_1 -> StandardMidiFileInterface.to_bytes(this)
            Midi.VERSION_2_CONTAINER -> MidiContainerFileInterface.to_bytes(this)
            Midi.VERSION_2_CLIP -> MidiClipFileInterface.to_bytes(this)
            else -> throw Exception()
        }
    }

    fun detect_version(): Int {
        var output = 1
        for (event in this.events) {
            if (event is UMPEvent) {
                output = 2
                break
            }
        }

        return output
    }

    // Save the midi object to a file
    fun save(path: String) {
        val bytes = this.as_bytes()
        File(path).writeBytes(bytes)
    }

    // Get the clip and tick of and event, given its id
    fun get_event_position(event_id: Int): Pair<Int, Int>? {
        return this.event_positions[event_id]
    }

    fun get_clips(): List<List<Pair<Int, Int>>> {
        val clips: MutableList<MutableList<Pair<Int, Int>>> = mutableListOf()
        for (eid in this.event_positions.keys) {
            val clip = this.event_positions[eid]?.first!!
            val tick = this.event_positions[eid]?.second!!
            while (clips.size <= clip) {
                clips.add(mutableListOf())
            }
            clips[clip].add(Pair(tick, eid))
        }

        val output: MutableList<MutableList<Pair<Int, Int>>> = mutableListOf()
        for (unsorted_clip in clips) {
            val clip = unsorted_clip.sortedBy { it.first }
            val current: MutableList<Pair<Int, Int>> = mutableListOf()
            var previous_tick: Int = 0
            for (pair in clip) {
                val current_tick = pair.first
                val eid = pair.second
                current.add(Pair(current_tick - previous_tick, eid))
                previous_tick = current_tick
            }
            output.add(current)
        }
        return output
    }

    fun count_clips(): Int {
        val used_clips = HashSet<Int>()
        for (pair in this.event_positions.values) {
            used_clips.add(pair.first)
        }
        return used_clips.size
    }

    fun count_events(): Int {
        return this.event_positions.size
    }

    fun get_clip_length(clip: Int): Int {
        var max_tick: Int = 0
        for (pair in this.event_positions.values) {
            if (pair.first == clip) {
                max_tick = kotlin.math.max(max_tick, pair.second)
            }
        }

        return max_tick + 1
    }

    fun set_ppqn(new_ppqn: Int) {
        this.ppqn = new_ppqn
    }

    fun get_ppqn(): Int {
        return this.ppqn
    }

    fun set_format(new_format: Int) {
        this.midi_format = new_format
    }

    fun get_format(): Int {
        return this.midi_format
    }

    fun insert_event(clip: Int, tick: Int, event: GeneralMIDIEvent): Int {
        if (clip > 15) {
            throw ClipOOB(clip)
        }
        val new_event_id = this.event_id_gen
        this.event_id_gen += 1

        this.events[new_event_id] = event
        this.move_event(clip, tick, new_event_id)

        return new_event_id
    }

    fun move_event(new_clip: Int, new_tick: Int, event_id: Int) {
        this.event_positions[event_id] = Pair(new_clip, new_tick)
    }

    fun push_event(wait: Int, event: GeneralMIDIEvent) {
        return this.push_event(0, wait, event)
    }

    fun push_event(clip: Int, wait: Int, event: GeneralMIDIEvent): Int {
        if (clip > 15) {
            throw ClipOOB(clip)
        }

        val new_event_id = this.event_id_gen
        this.event_id_gen += 1
        this.events[new_event_id] = event

        val last_tick_in_clip = this.get_clip_length(clip) - 1
        this.move_event(clip, last_tick_in_clip + wait, new_event_id)

        return new_event_id
    }

    fun insert_event(tick: Int, event: GeneralMIDIEvent): Int {
        return this.insert_event(0, tick, event)
    }

    fun move_event(new_tick: Int, event_id: Int) {
        this.event_positions[event_id] = Pair(0, new_tick)
    }



    fun get_event(event_id: Int): GeneralMIDIEvent? {
        return events[event_id]
    }

    fun replace_event(event_id: Int, new_midi_event: GeneralMIDIEvent) {
        if (!this.events.containsKey(event_id)) {
            throw InvalidEventId(event_id)
        }
        this.events[event_id] = new_midi_event
    }

    fun get_all_events(): List<Pair<Int, GeneralMIDIEvent>> {
        val output: MutableList<Pair<Int, GeneralMIDIEvent>> = mutableListOf()
        for (eid in this.event_positions.keys) {
            val tick = this.event_positions[eid]!!.second
            output.add(Pair(tick, this.events[eid]!!))
        }

        return output.sortedBy {
            (it.first * 10) + when (it.second) {
                is TimeSignature -> { 6 }
                is SongPositionPointer -> { 6 }
                is NoteOff -> { 7 }
                is NoteOn -> { 8 }
                else -> { 9 }
            }
        }
    }

    fun get_all_events_grouped(): List<Pair<Int, List<GeneralMIDIEvent>>> {
        val event_pairs = this.get_all_events()
        val output = mutableListOf<Pair<Int, List<GeneralMIDIEvent>>>()
        var working_pair: Pair<Int, MutableList<GeneralMIDIEvent>>? = null
        for ((tick, event) in event_pairs) {
            if (working_pair != null && working_pair.first != tick) {
                output.add(
                    Pair(
                        working_pair.first,
                        working_pair.second.sortedBy {
                            return@sortedBy when (it) {
                                is NoteOn -> { 1 }
                                is NoteOn79 -> { 1 }
                                is NoteOff -> { -1 }
                                is NoteOff79 -> { -1 }
                                is SongPositionPointer -> { -2 }
                                else -> { 0 }
                            }
                        }
                    )
                )
                working_pair = Pair(tick, mutableListOf())
            } else if (working_pair == null) {
                working_pair = Pair(tick, mutableListOf())
            }

            working_pair.second.add(event)
        }

        if (working_pair != null) {
            output.add(
                Pair(
                    working_pair.first,
                    working_pair.second.sortedBy {
                        return@sortedBy when (it) {
                            is NoteOn -> { 1 }
                            is NoteOff -> { -1 }
                            is NoteOn79 -> { 1 }
                            is NoteOff79 -> { -1 }
                            else -> { 0 }
                        }
                    }
                )
            )
        }
        return output
    }
}
