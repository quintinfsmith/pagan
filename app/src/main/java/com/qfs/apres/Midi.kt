package com.qfs.apres

import com.qfs.apres.event2.UMPEvent
import com.qfs.apres.event.EndOfTrack
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import java.io.File
import kotlin.experimental.or

class StandardMidiFileInterface {
    class InvalidChunkType(string: String): Exception("Invalid Chunk Type: $string")
    companion object {
        fun is_compatible(bytes: ByteArray): Boolean {
            val first_four = ByteArray(4) { i -> bytes[i] }
            return first_four.contentEquals("MThd".toByteArray())
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
            var current_track = 0
            var current_deltatime: Int
            var chunk_type: String

            var divword: Int
            var midi_format: Int
            var track_length: Int
            var found_header = false
            var ppqn = 120

            while (working_bytes.isNotEmpty()) {
                chunk_type = ""
                for (i in 0 until 4) {
                    chunk_type = "${chunk_type}${working_bytes.removeFirst().toInt().toChar()}"
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
                        dequeue_n(working_bytes, 2) // Get Number of tracks
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
                        track_length = dequeue_n(working_bytes, 4)
                        sub_bytes = mutableListOf()

                        for (i in 0 until track_length) {
                            sub_bytes.add(working_bytes.removeFirst())
                        }

                        while (sub_bytes.isNotEmpty()) {
                            current_deltatime += get_variable_length_number(sub_bytes)
                            active_byte = this.process_mtrk_event(mlo, sub_bytes, current_deltatime, current_track, active_byte)
                        }

                        current_track += 1
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

            val track_count = midi.count_tracks()
            output.add((track_count / 256).toByte())
            output.add((track_count % 256).toByte())

            val ppqn = midi.get_ppqn()
            output.add((ppqn / 256).toByte())
            output.add((ppqn % 256).toByte())

            var track_event_bytes: MutableList<Byte>
            var track_byte_length: Int
            val tracks = midi.get_tracks()

            for (ticks in tracks) {
                output.add('M'.code.toByte())
                output.add('T'.code.toByte())
                output.add('r'.code.toByte())
                output.add('k'.code.toByte())

                track_event_bytes = mutableListOf()
                var has_eot = false
                for (pair in ticks) {
                    val tick_delay = pair.first
                    val eid = pair.second
                    val working_event = midi.get_event(eid)
                    if (working_event != null) {
                        has_eot = has_eot || (working_event is EndOfTrack)
                        track_event_bytes += to_variable_length_bytes(tick_delay)
                        track_event_bytes += working_event.as_bytes().toMutableList()
                    }
                }

                // Automatically handle EndOfTrackEvent Here instead of requiring it to be in the MIDITrack object
                if (!has_eot) {
                    track_event_bytes.add(0x00)
                    track_event_bytes += EndOfTrack().as_bytes().toMutableList()
                }

                // track length in bytes
                track_byte_length = track_event_bytes.size
                output.add((track_byte_length shr 24).toByte())
                output.add(((track_byte_length shr 16) and 0xFF).toByte())
                output.add(((track_byte_length shr 8) and 0xFF).toByte())
                output.add((track_byte_length and 0xFF).toByte())
                output += track_event_bytes.toList()
            }

            return output.toByteArray()
        }

        fun process_mtrk_event(midi: Midi, bytes: MutableList<Byte>, current_deltatime: Int, track: Int, active_byte: Byte): Byte {
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
            midi.insert_event(track, current_deltatime, event!!)

            return adj_active_byte
        }
    }
}

class MidiContainerFileInterface {
    companion object {
        fun is_compatible(bytes: ByteArray): Boolean {
        }
        fun from_bytes(bytes: ByteArray): Midi {
        }
    }
}

class MidiClipFileInterface {
    companion object {
        fun is_compatible(bytes: ByteArray): Boolean {
        }
        fun from_bytes(bytes: ByteArray): Midi {
        }
    }
}


class Midi {
    class MissingMThd : Exception("Missing MThd")
    class TrackOOB(index: Int): Exception("Track $index Out of Bounds")
    class InvalidEventId(id: Int): Exception("No event mapped to id:$id")
    class UnknownMidiFileType: Exception()
    var ppqn: Int = 120
    var midi_format: Int = 1

    private var events = HashMap<Int, GeneralMIDIEvent>()
    private var event_id_gen: Int = 1
    private var event_positions = HashMap<Int, Pair<Int, Int>>()

    companion object {
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



    private fun _as_bytes_v2(): ByteArray {
    }

    fun as_bytes(version: Int? = null): ByteArray {
        val adj_version = if (version == null) {
            this.detect_version()
        } else {
            version
        }
        return when (adj_version) {
            1 -> this._as_bytes_v1()
            2 -> this._as_bytes_v2()
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

    // Get the track and tick of and event, given its id
    fun get_event_position(event_id: Int): Pair<Int, Int>? {
        return this.event_positions[event_id]
    }

    fun get_tracks(): List<List<Pair<Int, Int>>> {
        val tracks: MutableList<MutableList<Pair<Int, Int>>> = mutableListOf()
        for (eid in this.event_positions.keys) {
            val track = this.event_positions[eid]?.first!!
            val tick = this.event_positions[eid]?.second!!
            while (tracks.size <= track) {
                tracks.add(mutableListOf())
            }
            tracks[track].add(Pair(tick, eid))
        }

        val output: MutableList<MutableList<Pair<Int, Int>>> = mutableListOf()
        for (unsorted_track in tracks) {
            val track = unsorted_track.sortedBy { it.first }
            val current: MutableList<Pair<Int, Int>> = mutableListOf()
            var previous_tick: Int = 0
            for (pair in track) {
                val current_tick = pair.first
                val eid = pair.second
                current.add(Pair(current_tick - previous_tick, eid))
                previous_tick = current_tick
            }
            output.add(current)
        }
        return output
    }

    fun count_tracks(): Int {
        val used_tracks = HashSet<Int>()
        for (pair in this.event_positions.values) {
            used_tracks.add(pair.first)
        }
        return used_tracks.size
    }

    fun count_events(): Int {
        return this.event_positions.size
    }

    fun get_track_length(track: Int): Int {
        var max_tick: Int = 0
        for (pair in this.event_positions.values) {
            if (pair.first == track) {
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

    fun insert_event(track: Int, tick: Int, event: GeneralMIDIEvent): Int {
        if (track > 15) {
            throw TrackOOB(track)
        }
        val new_event_id = this.event_id_gen
        this.event_id_gen += 1

        this.events[new_event_id] = event
        this.move_event(track, tick, new_event_id)

        return new_event_id
    }

    fun move_event(new_track: Int, new_tick: Int, event_id: Int) {
        this.event_positions[event_id] = Pair(new_track, new_tick)
    }

    fun push_event(track: Int, wait: Int, event: GeneralMIDIEvent): Int {
        if (track > 15) {
            throw TrackOOB(track)
        }

        val new_event_id = this.event_id_gen
        this.event_id_gen += 1
        this.events[new_event_id] = event

        val last_tick_in_track = this.get_track_length(track) - 1
        this.move_event(track, last_tick_in_track + wait, new_event_id)

        return new_event_id
    }

    fun insert_event(tick: Int, event: GeneralMIDIEvent): Int {
        return this.insert_event(0, tick, event)
    }

    fun move_event(new_tick: Int, event_id: Int) {
        this.event_positions[event_id] = Pair(0, new_tick)
    }
    fun push_event(wait: Int, event: GeneralMIDIEvent) {
        return this.push_event(0, wait, event)
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
