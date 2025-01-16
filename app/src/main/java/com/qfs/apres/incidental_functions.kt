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
import com.qfs.apres.event.KeySignature
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
import com.qfs.apres.event.PitchWheelChange
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
import kotlin.experimental.and

class InvalidMIDIFile(var path: String): Exception("$path is not a valid midi file")

fun event_from_bytes(bytes: MutableList<Byte>, default: Byte): MIDIEvent? {
    var output: MIDIEvent? = null
    val leadbyte = toUInt(bytes.removeAt(0))
    val realtimes = listOf(0xF1, 0xF, 0xF8, 0xFC, 0xFE, 0xF7)
    val undefineds = listOf(0xF4, 0xF5, 0xF9, 0xFD)

    if (leadbyte in (0 .. 0x7F)) {
        bytes.add(0, leadbyte.toByte())
        bytes.add(0, default)
        output = event_from_bytes(bytes, default)
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
                    0x00 -> {
                        BankSelectMSB(channel, value)
                    }
                    0x20 -> {
                        BankSelectLSB(channel, value)
                    }
                    0x01 -> {
                        ModulationWheelMSB(channel, value)
                    }
                    0x21 -> {
                        ModulationWheelLSB(channel, value)
                    }
                    0x02 -> {
                        BreathControllerMSB(channel, value)
                    }
                    0x22 -> {
                        BreathControllerLSB(channel, value)
                    }
                    0x04 -> {
                        FootPedalMSB(channel, value)
                    }
                    0x24 -> {
                        FootPedalLSB(channel, value)
                    }
                    0x05 -> {
                        PortamentoTimeMSB(channel, value)
                    }
                    0x25 -> {
                        PortamentoTimeLSB(channel, value)
                    }
                    0x06 -> {
                        DataEntryMSB(channel, value)
                    }
                    0x26 -> {
                        DataEntryLSB(channel, value)
                    }
                    0x07 -> {
                        VolumeMSB(channel, value)
                    }
                    0x27 -> {
                        VolumeLSB(channel, value)
                    }
                    0x08 -> {
                        BalanceMSB(channel, value)
                    }
                    0x28 -> {
                        BalanceLSB(channel, value)
                    }
                    0x0A -> {
                        PanMSB(channel, value)
                    }
                    0x2A -> {
                        PanLSB(channel, value)
                    }
                    0x0B -> {
                        ExpressionMSB(channel, value)
                    }
                    0x2B -> {
                        ExpressionLSB(channel, value)
                    }
                    0x0C -> {
                        EffectControl1MSB(channel, value)
                    }
                    0x2C -> {
                        EffectControl1LSB(channel, value)
                    }
                    0x0D -> {
                        EffectControl2MSB(channel, value)
                    }
                    0x2D -> {
                        EffectControl2LSB(channel, value)
                    }
                    0x10 -> {
                        GeneralPurpose1MSB(channel, value)
                    }
                    0x30 -> {
                        GeneralPurpose1LSB(channel, value)
                    }
                    0x11 -> {
                        GeneralPurpose2MSB(channel, value)
                    }
                    0x31 -> {
                        GeneralPurpose2LSB(channel, value)
                    }
                    0x12 -> {
                        GeneralPurpose3MSB(channel, value)
                    }
                    0x32 -> {
                        GeneralPurpose3LSB(channel, value)
                    }
                    0x13 -> {
                        GeneralPurpose4MSB(channel, value)
                    }
                    0x33 -> {
                        GeneralPurpose4LSB(channel, value)
                    }
                    0x40 -> {
                        HoldPedal(channel, value)
                    }
                    0x41 -> {
                        Portamento(channel, value)
                    }
                    0x42 -> {
                        Sustenuto(channel, value)
                    }
                    0x43 -> {
                        SoftPedal(channel, value)
                    }
                    0x44 -> {
                        Legato(channel, value)
                    }
                    0x45 -> {
                        Hold2Pedal(channel, value)
                    }
                    0x46 -> {
                        SoundVariation(channel, value)
                    }
                    0x47 -> {
                        SoundTimbre(channel, value)
                    }
                    0x48 -> {
                        SoundReleaseTime(channel, value)
                    }
                    0x49 -> {
                        SoundAttack(channel, value)
                    }
                    0x4A -> {
                        SoundBrightness(channel, value)
                    }
                    0x4B -> {
                        SoundControl1(channel, value)
                    }
                    0x4C -> {
                        SoundControl2(channel, value)
                    }
                    0x4D -> {
                        SoundControl3(channel, value)
                    }
                    0x4E -> {
                        SoundControl4(channel, value)
                    }
                    0x4F -> {
                        SoundControl5(channel, value)
                    }
                    0x50 -> {
                        GeneralPurpose5(channel, value)
                    }
                    0x51 -> {
                        GeneralPurpose6(channel, value)
                    }
                    0x52 -> {
                        GeneralPurpose7(channel, value)
                    }
                    0x53 -> {
                        GeneralPurpose8(channel, value)
                    }
                    0x5B -> {
                        EffectsLevel(channel, value)
                    }
                    0x5C -> {
                        TremuloLevel(channel, value)
                    }
                    0x5D -> {
                        ChorusLevel(channel, value)
                    }
                    0x5E -> {
                        CelesteLevel(channel, value)
                    }
                    0x5F -> {
                        PhaserLevel(channel, value)
                    }
                    0x60 -> {
                        DataIncrement(channel)
                    }
                    0x61 -> {
                        DataDecrement(channel)
                    }
                    0x62 -> {
                        NonRegisteredParameterNumberLSB(channel, value)
                    }
                    0x63 -> {
                        NonRegisteredParameterNumberMSB(channel, value)
                    }
                    0x64 -> {
                        RegisteredParameterNumberLSB(channel, value)
                    }
                    0x65 -> {
                        RegisteredParameterNumberMSB(channel, value)
                    }
                    0x78 -> {
                        AllSoundOff(channel)
                    }
                    0x79 -> {
                        AllControllersOff(channel)
                    }
                    0x7A -> {
                        LocalControl(channel, value)
                    }
                    0x7B -> {
                        AllNotesOff(channel)
                    }
                    0x7C -> {
                        OmniOff(channel)
                    }
                    0x7D -> {
                        OmniOn(channel)
                    }
                    0xFE -> {
                        MonophonicOperation(channel, value)
                    }
                    0xFF -> {
                        PolyphonicOperation(channel)
                    }
                    else -> {
                        null
                        //ControlChange(channel, controller, value)
                    }
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

fun dequeue_n(bytelist: MutableList<Byte>, n: Int): Int {
    var output = 0
    for (_i in 0 until n) {
        output *= 256
        val x = toUInt(bytelist.removeAt(0))
        output += x
    }
    return output
}

fun get_variable_length_number(bytes: MutableList<Byte>): Int {
    var output: Int = 0
    while (true) {
        output = output shl 7
        val x = toUInt(bytes.removeAt(0))
        output = output or (x and 0x7F)
        if (x and 0x80 == 0) {
            break
        }
    }
    return output
}

fun to_variable_length_bytes(number: Int): List<Byte> {
    val output: MutableList<Byte> = mutableListOf()
    var first_pass = true
    var working_number = number
    while (working_number > 0 || first_pass) {
        var tmp = working_number and 0x7F
        working_number = working_number shr 7
        if (! first_pass) {
            tmp = tmp or 0x80
        }

        output.add(tmp.toByte())
        first_pass = false
    }
    return output.asReversed()
}

fun get_pitchwheel_value(n: Float): Int {
    val output = if (n < 0) {
        ((1 + n) * (0x2000)).toInt()
    } else if (n > 0) {
        (n * 0x1FFF).toInt() + 0x2000
    } else {
        0x2000
    }
    return output
}

fun build_key_signature(mi: Byte, sf: Byte): KeySignature {
    val chord_name = get_chord_name_from_mi_sf(mi, sf)
    return KeySignature(chord_name)
}

fun build_pitch_wheel_change(channel: Byte, lsb: Byte, msb: Byte): PitchWheelChange {
    return PitchWheelChange(channel.toInt(), PitchWheelChange.from_bytes(msb.toInt(), lsb.toInt()))
}

fun get_mi_sf(chord_name: String): Pair<Byte, Byte> {
    val output: Pair<Byte, Byte> = when (chord_name) {
        "A" -> {
            Pair(0, 3)
        }
        "A#", "Bb" -> {
            Pair(0, 10)
        }
        "B" -> {
            Pair(0, 5)
        }
        "C" -> {
            Pair(0, 0)
        }
        "C#", "Db" -> {
            Pair(0, 7)
        }
        "D" -> {
            Pair(0, 2)
        }
        "D#", "Eb" -> {
            Pair(0, 11)
        }
        "E" -> {
            Pair(0, 4)
        }
        "F" -> {
            Pair(0, 9)
        }
        "F#", "Gb" -> {
            Pair(0, 6)
        }
        "G" -> {
            Pair(0, 1)
        }
        "Am" -> {
            Pair(1, 0)
        }
        "A#m", "Bbm" -> {
            Pair(1, 7)
        }
        "Bm" -> {
            Pair(1, 2)
        }
        "Cm" -> {
            Pair(1, 11)
        }
        "C#m", "Dbm" -> {
            Pair(1, 4)
        }
        "Dm" -> {
            Pair(1, 9)
        }
        "D#m", "Ebm" -> {
            Pair(1, 6)
        }
        "Em" -> {
            Pair(1, 1)
        }
        "Fm" -> {
            Pair(1, 2)
        }
        "F#m", "Gbm" -> {
            Pair(1, 3)
        }
        "Gm" -> {
            Pair(1, 10)
        }
        else -> {
            Pair(0, 0) // Default to C Major
        }
    }
    return output
}

fun get_chord_name_from_mi_sf(mi: Byte, sf: Byte): String {
    val mi_int = mi.toInt()
    val sf_int: Int = if (sf < 0) {
        (sf and 0xFF.toByte()).toInt()
    } else {
        sf.toInt()
    }

    val map: List<List<String>> = listOf(
        listOf(
            "Cb", "Gb", "Db", "Ab",
            "Eb", "Bb", "F",
            "C", "G", "D", "A",
            "E", "B", "F#", "C#"
        ),
        listOf(
            "Abm", "Ebm", "Bbm", "Fm",
            "Cm", "Gm", "Dm",
            "Am", "Em", "Bm", "F#m",
            "C#m", "G#m", "D#m", "A#m"
        )
    )

    return map[mi_int][sf_int + 7]
}

fun toUInt(byte: Byte): Int {
    var new_int = (byte and 0x7F.toByte()).toInt()
    if (byte.toInt() < 0) {
        new_int += 128
    }
    return new_int
}
