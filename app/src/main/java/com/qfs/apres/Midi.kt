package com.qfs.apres

import com.qfs.apres.event.AllControllersOff
import com.qfs.apres.event.AllNotesOff
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BalanceLSB
import com.qfs.apres.event.BalanceMSB
import com.qfs.apres.event.BankSelectLSB
import com.qfs.apres.event.BankSelectMSB
import com.qfs.apres.event.BreathControllerLSB
import com.qfs.apres.event.BreathControllerMSB
import com.qfs.apres.event.CelesteLevel
import com.qfs.apres.event.ChannelPrefix
import com.qfs.apres.event.ChannelPressure
import com.qfs.apres.event.ChorusLevel
import com.qfs.apres.event.CopyRightNotice
import com.qfs.apres.event.CuePoint
import com.qfs.apres.event.DataDecrement
import com.qfs.apres.event.DataEntryLSB
import com.qfs.apres.event.DataEntryMSB
import com.qfs.apres.event.DataIncrement
import com.qfs.apres.event.EffectControl1LSB
import com.qfs.apres.event.EffectControl1MSB
import com.qfs.apres.event.EffectControl2LSB
import com.qfs.apres.event.EffectControl2MSB
import com.qfs.apres.event.EffectsLevel
import com.qfs.apres.event.EndOfTrack
import com.qfs.apres.event.ExpressionLSB
import com.qfs.apres.event.ExpressionMSB
import com.qfs.apres.event.FootPedalLSB
import com.qfs.apres.event.FootPedalMSB
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.GeneralPurpose1LSB
import com.qfs.apres.event.GeneralPurpose1MSB
import com.qfs.apres.event.GeneralPurpose2LSB
import com.qfs.apres.event.GeneralPurpose2MSB
import com.qfs.apres.event.GeneralPurpose3LSB
import com.qfs.apres.event.GeneralPurpose3MSB
import com.qfs.apres.event.GeneralPurpose4LSB
import com.qfs.apres.event.GeneralPurpose4MSB
import com.qfs.apres.event.GeneralPurpose5
import com.qfs.apres.event.GeneralPurpose6
import com.qfs.apres.event.GeneralPurpose7
import com.qfs.apres.event.GeneralPurpose8
import com.qfs.apres.event.Hold2Pedal
import com.qfs.apres.event.HoldPedal
import com.qfs.apres.event.InstrumentName
import com.qfs.apres.event.Legato
import com.qfs.apres.event.LocalControl
import com.qfs.apres.event.Lyric
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.Marker
import com.qfs.apres.event.MetaEvent
import com.qfs.apres.event.ModulationWheelLSB
import com.qfs.apres.event.ModulationWheelMSB
import com.qfs.apres.event.MonophonicOperation
import com.qfs.apres.event.NonRegisteredParameterNumberLSB
import com.qfs.apres.event.NonRegisteredParameterNumberMSB
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.OmniOff
import com.qfs.apres.event.OmniOn
import com.qfs.apres.event.PanLSB
import com.qfs.apres.event.PanMSB
import com.qfs.apres.event.PhaserLevel
import com.qfs.apres.event.PolyphonicKeyPressure
import com.qfs.apres.event.PolyphonicOperation
import com.qfs.apres.event.Portamento
import com.qfs.apres.event.PortamentoTimeLSB
import com.qfs.apres.event.PortamentoTimeMSB
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.RegisteredParameterNumberLSB
import com.qfs.apres.event.RegisteredParameterNumberMSB
import com.qfs.apres.event.SMPTEOffset
import com.qfs.apres.event.SequenceNumber
import com.qfs.apres.event.SequencerSpecific
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SoftPedal
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.SongSelect
import com.qfs.apres.event.SoundAttack
import com.qfs.apres.event.SoundBrightness
import com.qfs.apres.event.SoundControl1
import com.qfs.apres.event.SoundControl2
import com.qfs.apres.event.SoundControl3
import com.qfs.apres.event.SoundControl4
import com.qfs.apres.event.SoundControl5
import com.qfs.apres.event.SoundReleaseTime
import com.qfs.apres.event.SoundTimbre
import com.qfs.apres.event.SoundVariation
import com.qfs.apres.event.Sustenuto
import com.qfs.apres.event.SystemExclusive
import com.qfs.apres.event.Text
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event.TrackName
import com.qfs.apres.event.TremuloLevel
import com.qfs.apres.event.VolumeLSB
import com.qfs.apres.event.VolumeMSB
import com.qfs.apres.event2.DeltaClockStamp
import com.qfs.apres.event2.EndOfClip
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.event2.SetTempoMessage
import com.qfs.apres.event2.StartOfClip
import com.qfs.apres.event2.UMPEvent
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

                        for (i in 0 until clip_length) {
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

        private fun process_mtrk_event(midi: Midi, bytes: MutableList<Byte>, current_deltatime: Int, clip: Int, active_byte: Byte): Byte {
            var adj_active_byte = if (bytes.first() in 0x80..0xEF) {
                bytes.first()
            } else {
                active_byte
            }

            val event: GeneralMIDIEvent? = StandardMidiFileInterface.event_from_bytes(bytes, adj_active_byte)
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

        fun event_from_bytes(bytes: MutableList<Byte>, default: Byte): MIDIEvent? {
            var output: MIDIEvent? = null
            val leadbyte = toUInt(bytes.removeAt(0))
            val realtimes = listOf(0xF1, 0xF, 0xF8, 0xFC, 0xFE, 0xF7)
            val undefineds = listOf(0xF4, 0xF5, 0xF9, 0xFD)

            if (leadbyte in (0 .. 0x7F)) {
                bytes.add(0, leadbyte.toByte())
                bytes.add(0, default)
                output = StandardMidiFileInterface.event_from_bytes(bytes, default)
            } else if (leadbyte in (0x80 .. 0xEF)) {
                val leadnibble: Int = leadbyte shr 4
                when (leadnibble) {
                    0x8 -> {
                        val channel = (leadbyte and 0x0F)
                        val note = bytes.removeAt(0).toInt()
                        val velocity = bytes.removeAt(0).toInt()
                        output = NoteOff(channel, note, velocity)
                    }
                    0x9 -> {
                        val channel = (leadbyte and 0x0F)
                        val note = bytes.removeAt(0).toInt()
                        val velocity = bytes.removeAt(0).toInt()
                        output = if (velocity == 0) {
                            NoteOff(channel, note, velocity)
                        } else {
                            NoteOn(channel, note, velocity)
                        }
                    }
                    0xA -> {
                        val channel = (leadbyte and 0x0F)
                        val note = bytes.removeAt(0).toInt()
                        val velocity = bytes.removeAt(0).toInt()
                        output = PolyphonicKeyPressure(channel, note, velocity)
                    }
                    0xB -> {
                        val channel = (leadbyte and 0x0F)
                        val controller = bytes.removeAt(0).toInt()
                        val value = bytes.removeAt(0).toInt()
                        output = when (controller) {
                            0x00 -> BankSelectMSB(channel, value)
                            0x20 -> BankSelectLSB(channel, value)
                            0x01 -> ModulationWheelMSB(channel, value)
                            0x21 -> ModulationWheelLSB(channel, value)
                            0x02 -> BreathControllerMSB(channel, value)
                            0x22 -> BreathControllerLSB(channel, value)
                            0x04 -> FootPedalMSB(channel, value)
                            0x24 -> FootPedalLSB(channel, value)
                            0x05 -> PortamentoTimeMSB(channel, value)
                            0x25 -> PortamentoTimeLSB(channel, value)
                            0x06 -> DataEntryMSB(channel, value)
                            0x26 -> DataEntryLSB(channel, value)
                            0x07 -> VolumeMSB(channel, value)
                            0x27 -> VolumeLSB(channel, value)
                            0x08 -> BalanceMSB(channel, value)
                            0x28 -> BalanceLSB(channel, value)
                            0x0A -> PanMSB(channel, value)
                            0x2A -> PanLSB(channel, value)
                            0x0B -> ExpressionMSB(channel, value)
                            0x2B -> ExpressionLSB(channel, value)
                            0x0C -> EffectControl1MSB(channel, value)
                            0x2C -> EffectControl1LSB(channel, value)
                            0x0D -> EffectControl2MSB(channel, value)
                            0x2D -> EffectControl2LSB(channel, value)
                            0x10 -> GeneralPurpose1MSB(channel, value)
                            0x30 -> GeneralPurpose1LSB(channel, value)
                            0x11 -> GeneralPurpose2MSB(channel, value)
                            0x31 -> GeneralPurpose2LSB(channel, value)
                            0x12 -> GeneralPurpose3MSB(channel, value)
                            0x32 -> GeneralPurpose3LSB(channel, value)
                            0x13 -> GeneralPurpose4MSB(channel, value)
                            0x33 -> GeneralPurpose4LSB(channel, value)
                            0x40 -> HoldPedal(channel, value)
                            0x41 -> Portamento(channel, value)
                            0x42 -> Sustenuto(channel, value)
                            0x43 -> SoftPedal(channel, value)
                            0x44 -> Legato(channel, value)
                            0x45 -> Hold2Pedal(channel, value)
                            0x46 -> SoundVariation(channel, value)
                            0x47 -> SoundTimbre(channel, value)
                            0x48 -> SoundReleaseTime(channel, value)
                            0x49 -> SoundAttack(channel, value)
                            0x4A -> SoundBrightness(channel, value)
                            0x4B -> SoundControl1(channel, value)
                            0x4C -> SoundControl2(channel, value)
                            0x4D -> SoundControl3(channel, value)
                            0x4E -> SoundControl4(channel, value)
                            0x4F -> SoundControl5(channel, value)
                            0x50 -> GeneralPurpose5(channel, value)
                            0x51 -> GeneralPurpose6(channel, value)
                            0x52 -> GeneralPurpose7(channel, value)
                            0x53 -> GeneralPurpose8(channel, value)
                            0x5B -> EffectsLevel(channel, value)
                            0x5C -> TremuloLevel(channel, value)
                            0x5D -> ChorusLevel(channel, value)
                            0x5E -> CelesteLevel(channel, value)
                            0x5F -> PhaserLevel(channel, value)
                            0x60 -> DataIncrement(channel)
                            0x61 -> DataDecrement(channel)
                            0x62 -> NonRegisteredParameterNumberLSB(channel, value)
                            0x63 -> NonRegisteredParameterNumberMSB(channel, value)
                            0x64 -> RegisteredParameterNumberLSB(channel, value)
                            0x65 -> RegisteredParameterNumberMSB(channel, value)
                            0x78 -> AllSoundOff(channel)
                            0x79 -> AllControllersOff(channel)
                            0x7A -> LocalControl(channel, value)
                            0x7B -> AllNotesOff(channel)
                            0x7C -> OmniOff(channel)
                            0x7D -> OmniOn(channel)
                            0xFE -> MonophonicOperation(channel, value)
                            0xFF -> PolyphonicOperation(channel)
                            else -> null
                        }
                    }
                    0xC -> {
                        output = ProgramChange(
                            (leadbyte and 0x0F),
                            bytes.removeAt(0).toInt()
                        )
                    }
                    0xD -> {
                        output = ChannelPressure(
                            (leadbyte and 0x0F),
                            bytes.removeAt(0).toInt()
                        )
                    }
                    0xE -> {
                        output = build_pitch_wheel_change(
                            (leadbyte and 0x0F).toByte(),
                            bytes.removeAt(0),
                            bytes.removeAt(0)
                        )
                    }
                    else -> { }
                }
            } else if (leadbyte == 0xF0) {
                val bytedump: MutableList<Byte> = mutableListOf()
                while (true) {
                    val byte = bytes.removeAt(0)
                    if (byte.toInt() == 0xF7) {
                        break
                    } else {
                        bytedump.add(byte)
                    }
                }
                output = SystemExclusive(bytedump.toByteArray())
            } else if (leadbyte == 0xF2) {
                val lsb = bytes.removeAt(0).toInt()
                val msb = bytes.removeAt(0).toInt()
                val beat: Int = (msb shl 8) + lsb
                output = SongPositionPointer(beat)
            } else if (leadbyte == 0xF3) {
                output = SongSelect((bytes.removeAt(0).toInt()) and 0x7F)
            } else if (leadbyte == 0xFF) {
                val meta_byte = bytes.removeAt(0).toInt()
                val varlength = get_variable_length_number(bytes)

                if (meta_byte == 0x51) {
                    output = SetTempo(dequeue_n(bytes, varlength))
                } else {
                    val bytedump_list: MutableList<Byte> = mutableListOf()
                    for (i in 0 until varlength) {
                        bytedump_list.add(bytes.removeAt(0))
                    }
                    val bytedump: ByteArray = bytedump_list.toByteArray()
                    when (meta_byte) {
                        0x00 -> {
                            output = SequenceNumber(
                                ((bytedump[0].toInt()) * 256) + bytedump[1].toInt()
                            )
                        }
                        0x01 -> {
                            output = Text(String(bytedump))
                        }
                        0x02 -> {
                            output = CopyRightNotice(String(bytedump))
                        }
                        0x03 -> {
                            output = TrackName(String(bytedump))
                        }
                        0x04 -> {
                            output = InstrumentName(String(bytedump))
                        }
                        0x05 -> {
                            output = Lyric(String(bytedump))
                        }
                        0x06 -> {
                            output = Marker(String(bytedump))
                        }
                        0x07 -> {
                            output = CuePoint(String(bytedump))
                        }
                        0x20 -> {
                            output = ChannelPrefix(bytedump[0].toInt())
                        }
                        0x2F -> {
                            output = EndOfTrack()
                        }
                        0x54 -> {
                            output = SMPTEOffset(
                                bytedump[0].toInt(),
                                bytedump[1].toInt(),
                                bytedump[2].toInt(),
                                bytedump[3].toInt(),
                                bytedump[4].toInt()
                            )
                        }
                        0x58 -> {
                            output = TimeSignature(
                                bytedump[0].toInt(),
                                bytedump[1].toInt(),
                                bytedump[2].toInt(),
                                bytedump[3].toInt()
                            )
                        }
                        0x59 -> {
                            output = build_key_signature(bytedump[1], bytedump[0])
                        }
                        0x7F -> {
                            for (i in 0 until 3) {
                                bytedump_list.removeAt(0)
                            }
                            output = SequencerSpecific(bytedump_list.toByteArray())
                        }
                        else -> {
                            output = MetaEvent(meta_byte.toByte(), bytedump_list.toByteArray())
                        }
                    }
                }
            } else if (realtimes.contains(leadbyte)) {
                // pass. realtime events should be in file
            } else if (undefineds.contains(leadbyte)) {
                // specifically undefined behaviour
            }

            return output
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
            TODO()
        }
        fun to_bytes(midi: Midi): ByteArray {
            TODO()
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
        fun from_bytes(bytes: ByteArray): Midi {
            TODO("Not implemented yet")
        }
        fun to_bytes(midi: Midi, clip_index: Int = 0): ByteArray {
            val output = "SMF2CLIP".toByteArray().toMutableList()
            output += listOf(0x00, 0x40, 0x00, 0x00)

            val ppqn = midi.get_ppqn()
            output.add(0x00)
            output.add(0x30)
            output.add((ppqn / 256).toByte())
            output.add((ppqn % 256).toByte())

            output += SetTempoMessage(120F).as_bytes().toList()
            output += listOf(0x00, 0x40, 0x00, 0x00)
            output += StartOfClip().as_bytes().toList()


            val ticks = midi.get_clips()[clip_index]

            val clip_event_bytes = mutableListOf<Byte>()
            var has_eot = false
            for (pair in ticks) {
                val tick_delay = pair.first
                val eid = pair.second
                val working_event = midi.get_event(eid)
                if (working_event != null) {
                    has_eot = has_eot || (working_event is EndOfClip)
                    clip_event_bytes += DeltaClockStamp(tick_delay).as_bytes().toList()
                    clip_event_bytes += when (working_event) {
                        is MIDIEvent -> working_event.as_ump_bytes()
                        else -> working_event.as_bytes()
                    }.toMutableList()
                }
            }

            // Automatically handle EndOfClipEvent Here instead of requiring it to be in the MIDIClip object
            if (!has_eot) {
                clip_event_bytes += DeltaClockStamp(0).as_bytes().toList()
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
        var output = Midi.VERSION_1
        for (event in this.events) {
            if (event is UMPEvent) {
                output = Midi.VERSION_2_CLIP
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

    fun push_event(wait: Int, event: GeneralMIDIEvent): Int {
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
