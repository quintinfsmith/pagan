package com.qfs.radixulous.apres

import java.io.File
import java.lang.Math.max
import kotlin.experimental.and

interface MIDIEvent {
    abstract fun as_bytes(): ByteArray
}

fun event_from_bytes(bytes: MutableList<Byte>, default: Byte): MIDIEvent? {
    var output: MIDIEvent? = null
    var leadbyte = bytes.removeFirst()
    var realtimes = listOf(0xF1, 0xF, 0xF8, 0xFC, 0xFE, 0xF7)
    var undefineds = listOf(0xF4, 0xF5, 0xF9, 0xFD)

    if (leadbyte in (0..0x80)) {
        bytes.add(0, leadbyte)
        bytes.add(0, default)
        output = event_from_bytes(bytes, default)
    } else if (leadbyte in (0x80..0xF0)) {
        var leadnibble: Int = (leadbyte.toInt()) shr 4
        when (leadnibble) {
            0x8 -> {
                var channel = (leadbyte and 0x0F).toInt()
                var note = bytes.removeFirst().toInt()
                var velocity = bytes.removeFirst().toInt()
                output = NoteOff(channel, note, velocity)
            }
            0x9 -> {
                var channel = (leadbyte and 0x0F).toInt()
                var note = bytes.removeFirst().toInt()
                var velocity = bytes.removeFirst().toInt()
                output = if (velocity == 0) {
                    NoteOff(channel, note, velocity)
                } else {
                    NoteOn(channel, note, velocity)
                }
            }
            0xA -> {
                var channel = (leadbyte and 0x0F).toInt()
                var note = bytes.removeFirst().toInt()
                var velocity = bytes.removeFirst().toInt()
                output = PolyphonicKeyPressure(channel, note, velocity)
            }
            0xB -> {
                var channel = (leadbyte and 0x0F).toInt()
                var controller = bytes.removeFirst().toInt()
                var value = bytes.removeFirst().toInt()
                output = when (controller) {
                    0x00 -> {
                        BankSelect(channel, value)
                    }
                    0x20 -> {
                        BankSelectLSB(channel, value)
                    }
                    0x01 -> {
                        ModulationWheel(channel, value)
                    }
                    0x21 -> {
                        ModulationWheelLSB(channel, value)
                    }
                    0x02 -> {
                        BreathController(channel, value)
                    }
                    0x22 -> {
                        BreathControllerLSB(channel, value)
                    }
                    0x04 -> {
                        FootPedal(channel, value)
                    }
                    0x24 -> {
                        FootPedalLSB(channel, value)
                    }
                    0x05 -> {
                        PortamentoTime(channel, value)
                    }
                    0x25 -> {
                        PortamentoTimeLSB(channel, value)
                    }
                    0x06 -> {
                        DataEntry(channel, value)
                    }
                    0x26 -> {
                        DataEntryLSB(channel, value)
                    }
                    0x07 -> {
                        Volume(channel, value)
                    }
                    0x27 -> {
                        VolumeLSB(channel, value)
                    }
                    0x08 -> {
                        Balance(channel, value)
                    }
                    0x28 -> {
                        BalanceLSB(channel, value)
                    }
                    0x0A -> {
                        Pan(channel, value)
                    }
                    0x2A -> {
                        PanLSB(channel, value)
                    }
                    0x0B -> {
                        Expression(channel, value)
                    }
                    0x2B -> {
                        ExpressionLSB(channel, value)
                    }
                    0x0C -> {
                        EffectControl1(channel, value)
                    }
                    0x2C -> {
                        EffectControl1LSB(channel, value)
                    }
                    0x0D -> {
                        EffectControl2(channel, value)
                    }
                    0x2D -> {
                        EffectControl2LSB(channel, value)
                    }
                    0x10 -> {
                        GeneralPurpose1(channel, value)
                    }
                    0x30 -> {
                        GeneralPurpose1LSB(channel, value)
                    }
                    0x11 -> {
                        GeneralPurpose2(channel, value)
                    }
                    0x31 -> {
                        GeneralPurpose2LSB(channel, value)
                    }
                    0x12 -> {
                        GeneralPurpose3(channel, value)
                    }
                    0x32 -> {
                        GeneralPurpose3LSB(channel, value)
                    }
                    0x13 -> {
                        GeneralPurpose4(channel, value)
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
                        NonRegisteredParameterNumber(channel, value)
                    }
                    0x64 -> {
                        RegisteredParameterNumberLSB(channel, value)
                    }
                    0x65 -> {
                        RegisteredParameterNumber(channel, value)
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
                        ControlChange(channel, controller, value)
                    }
                }
            }
            0xC -> {
                output = ProgramChange(
                    (leadbyte and 0x0F).toInt(),
                    bytes.removeFirst().toInt()
                )
            }
            0xD -> {
                output = ChannelPressure(
                    (leadbyte and 0x0F).toInt(),
                    bytes.removeFirst().toInt()
                )
            }
            0xE -> {
                output = build_pitch_wheel_change(
                    leadbyte and 0x0F.toByte(),
                    bytes.removeFirst(),
                    bytes.removeFirst()
                )
            }
            else -> {
            }

        }
    } else if (leadbyte == 0xF0.toByte()) {
        var bytedump: MutableList<Byte> = mutableListOf()
        while (true) {
            var byte = bytes.removeFirst()
            if (byte.toInt() == 0xF7) {
                break
            } else {
                bytedump.add(byte)
            }
        }
        output = SystemExclusive(bytedump.toByteArray())
    } else if (leadbyte == 0xF2.toByte()) {
        var lsb = bytes.removeFirst().toInt()
        var msb = bytes.removeFirst().toInt()
        var beat: Int = (msb shl 8) + lsb
        output = SongPositionPointer(beat)
    } else if (leadbyte == 0xF3.toByte()) {
        output = SongSelect((bytes.removeFirst().toInt()) and 0x7F)
    } else if (leadbyte == 0xFF.toByte()) {
        var meta_byte = bytes.removeFirst().toInt()
        var varlength = get_variable_length_number(bytes)
        if (meta_byte == 51) {
            output = SetTempo(dequeue_n(bytes, varlength))
        } else {
            var bytedump_list: MutableList<Byte> = mutableListOf()
            for (i in 0 until varlength) {
                bytedump_list.add(bytes.removeFirst())
            }
            var bytedump: ByteArray = bytedump_list.toByteArray()

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
                0x51 -> {}
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
                        bytedump_list.removeFirst()
                    }
                    output = SequencerSpecific(bytedump_list.toByteArray())
                }
                else -> {
                    throw Exception("UnknownEvent")
                }
            }
        }
    } else if (realtimes.contains(leadbyte.toInt())) {
        // pass. realtime events should be in file
    } else if (undefineds.contains(leadbyte.toInt())) {
        // specifically undefined behaviour
    }

    return output
}

class SequenceNumber(var sequence: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x00.toByte(),
            0x02.toByte(),
            ((this.sequence shr 8) and 0xFF).toByte(),
            (this.sequence and 0xFF).toByte()
        )
    }

    fun get_sequence(): Int {
        return this.sequence
    }

    fun set_sequence(new_sequence: Int) {
        this.sequence = new_sequence
    }
}

class Text(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x01.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

class CopyRightNotice(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x02.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }
    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

class TrackName(var name: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var name_bytes = this.name.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x03.toByte()) + to_variable_length_bytes(name_bytes.size) + name_bytes
    }

    fun get_name(): String {
        return this.name
    }

    fun set_name(new_name: String) {
        this.name = new_name
    }
}

class InstrumentName(var name: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var name_bytes = this.name.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x04.toByte()) + to_variable_length_bytes(name_bytes.size) + name_bytes
    }

    fun get_name(): String {
        return this.name
    }

    fun set_name(new_name: String) {
        this.name = new_name
    }
}

class Lyric(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x05.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

class Marker(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x06.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

class CuePoint(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x07.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

class EndOfTrack: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0x2F.toByte(), 0x00.toByte())
    }
}

class ChannelPrefix(var channel: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x20.toByte(),
            0x01.toByte(),
            this.channel.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
}

class SetTempo(var mspqn: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x51.toByte(),
            0x03.toByte(),
            ((this.mspqn shr 16) and 0xFF).toByte(),
            ((this.mspqn shr 8) and 0xFF).toByte(),
            (this.mspqn and 0xFF).toByte()
        )
    }
    companion object {
        fun from_bpm(bpm: Double): SetTempo {
            return SetTempo((60000000.toDouble() / bpm) as Int)
        }
    }

    fun get_bpm(): Double {
        var mspqn = this.get_mspqn()
        return if (mspqn > 0) {
            60000000.toDouble() / mspqn
        } else {
            0.toDouble()
        }
    }

    fun get_mspqn(): Int {
        return this.mspqn
    }

    fun set_mspqn(new_mspqn: Int) {
        this.mspqn = new_mspqn
    }

    fun set_bpm(new_bpm: Double) {
        if (new_bpm > 0) {
            this.mspqn = (60000000.toDouble() / new_bpm) as Int
        } else {
            this.mspqn = 0
        }
    }
}

class SMPTEOffset(var hour: Int, var minute: Int, var second: Int, var ff: Int, var fr: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x54.toByte(),
            0x05.toByte(),
            this.hour.toByte(),
            this.minute.toByte(),
            this.second.toByte(),
            this.ff.toByte(),
            this.fr.toByte()
        )
    }

    fun get_hour(): Int {
        return this.hour
    }
    fun get_minute(): Int {
        return this.minute
    }
    fun get_second(): Int {
        return this.second
    }
    fun get_ff(): Int {
        return this.ff
    }
    fun get_fr(): Int {
        return this.fr
    }
    fun set_hour(hour: Int) {
        this.hour = hour
    }
    fun set_minute(minute: Int) {
        this.minute = minute
    }
    fun set_second(second: Int) {
        this.second = second
    }
    fun set_ff(ff: Int) {
        this.ff = ff
    }
    fun set_fr(fr: Int) {
        this.fr = fr
    }
}

class TimeSignature(var numerator: Int, var denominator: Int, var clocks_per_metronome: Int, var thirtysecondths_per_quarter: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x58.toByte(),
            0x04.toByte(),
            this.numerator.toByte(),
            this.denominator.toByte(),
            this.clocks_per_metronome.toByte(),
            this.thirtysecondths_per_quarter.toByte()
        )
    }
    fun get_numerator(): Int {
        return this.numerator
    }

    fun get_denominator(): Int {
        return this.denominator
    }

    fun get_clocks_per_metronome(): Int {
        return this.clocks_per_metronome
    }

    fun get_thirtysecondths_per_quarter_note(): Int {
        return this.thirtysecondths_per_quarter
    }

    fun set_numerator(new_value: Int) {
        this.numerator = new_value
    }
    fun set_denominator(new_value: Int) {
        this.denominator = new_value
    }
    fun set_clocks_per_metronome(new_value: Int) {
        this.clocks_per_metronome = new_value
    }
    fun set_thirtysecondths_per_quarter_note(new_value: Int) {
        this.thirtysecondths_per_quarter = new_value
    }
}

class KeySignature(var key: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var misf = get_mi_sf(this.key)
        return byteArrayOf(
            0xFF.toByte(),
            0x59.toByte(),
            0x02.toByte(),
            misf.first,
            misf.second
        )
    }

    companion object {
        fun from_mi_sf(mi: Byte, sf: Byte): KeySignature {
            var chord_name = get_chord_name_from_mi_sf(mi, sf)
            return KeySignature(chord_name)
        }
    }

    fun get_key(): String {
        return this.key
    }

    fun set_key(key: String) {
        this.key = key
    }
}

class SequencerSpecific(var data: ByteArray): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0x7F.toByte()) + to_variable_length_bytes(this.data.size).toByteArray() + this.data
    }
    fun get_data(): ByteArray {
        return this.data
    }
    fun set_data(new_data: ByteArray) {
        this.data = new_data
    }
}

class NoteOn(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x90 or this.channel).toByte(),
            this.note.toByte(),
            this.velocity.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }

    fun get_note(): Int {
        return this.note
    }

    fun get_velocity(): Int {
        return this.velocity
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun set_note(note: Int) {
        this.note = note
    }

    fun set_velocity(velocity: Int) {
        this.velocity = velocity
    }
}

class NoteOff(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x80 or this.channel).toByte(),
            this.note.toByte(),
            this.velocity.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }

    fun get_note(): Int {
        return this.note
    }

    fun get_velocity(): Int {
        return this.velocity
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun set_note(note: Int) {
        this.note = note
    }

    fun set_velocity(velocity: Int) {
        this.velocity = velocity
    }
}


class PolyphonicKeyPressure(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xA0 or this.channel).toByte(),
            this.note.toByte(),
            this.velocity.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }

    fun get_note(): Int {
        return this.note
    }

    fun get_velocity(): Int {
        return this.velocity
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun set_note(note: Int) {
        this.note = note
    }

    fun set_velocity(velocity: Int) {
        this.velocity = velocity
    }
}

open class ControlChange(var channel: Int, var controller: Int, open var value: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xB0 or this.get_channel()).toByte(),
            this.get_controller().toByte(),
            this.get_value().toByte()
        )
    }
    fun get_controller(): Int {
        return this.controller
    }
    fun get_channel(): Int {
        return this.channel
    }
    fun get_value(): Int {
        return this.value
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
    fun set_controller(controller: Int) {
        this.controller = controller
    }
    fun set_value(value: Int) {
        this.value = value
    }
}

abstract class VariableControlChange(var channel: Int, var value: Int): MIDIEvent {
    abstract val controller: Int
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xB0 or this.get_channel()).toByte(),
            this.get_controller().toByte(),
            this.get_value().toByte()
        )
    }
    fun get_controller(): Int {
        return this.controller
    }
    fun get_channel(): Int {
        return this.channel
    }
    fun get_value(): Int {
        return this.value
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
    fun set_value(value: Int) {
        this.value = value
    }
}

class HoldPedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x40
}
class Portamento(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x41
}
class Sustenuto(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x42
}
class SoftPedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x43
}
class Legato(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x44
}
class Hold2Pedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x45
}
class SoundVariation(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x46
}
class SoundTimbre(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x47
}
class SoundReleaseTime(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x48
}
class SoundAttack(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x49
}
class SoundBrightness(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4A
}
class SoundControl1(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4B
}
class SoundControl2(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4C
}
class SoundControl3(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4D
}
class SoundControl4(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4E
}
class SoundControl5(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x4F
}

class EffectsLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5B
}
class TremuloLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5C
}
class ChorusLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5D
}
class CelesteLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5E
}
class PhaserLevel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x5F
}
class LocalControl(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x7A
}
class MonophonicOperation(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0xFE
}


class BankSelect(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x00
}
class BankSelectLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x20
}
class ModulationWheel(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x01
}
class ModulationWheelLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x21
}
class BreathController(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x02
}
class BreathControllerLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x22
}
class FootPedal(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x04
}
class FootPedalLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x24
}
class PortamentoTime(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x05
}
class PortamentoTimeLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x25
}
class DataEntry(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x06
}
class DataEntryLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x26
}
class Volume(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x07
}
class VolumeLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x27
}
class Balance(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x08
}
class BalanceLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x28
}
class Pan(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0A
}
class PanLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2A
}
class Expression(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0B
}
class ExpressionLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2B
}
class NonRegisteredParameterNumber(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x63
}
class NonRegisteredParameterNumberLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x62
}
class RegisteredParameterNumber(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x65
}
class RegisteredParameterNumberLSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x64
}
class EffectControl1(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0C
}
class EffectControl1LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2C
}
class EffectControl2(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x0D
}
class EffectControl2LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x2D
}
class GeneralPurpose1(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x10
}
class GeneralPurpose1LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x30
}
class GeneralPurpose2(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x11
}
class GeneralPurpose2LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x31
}
class GeneralPurpose3(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x12
}
class GeneralPurpose3LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x32
}
class GeneralPurpose4(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x13
}
class GeneralPurpose4LSB(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x33
}
class GeneralPurpose5(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x50
}
class GeneralPurpose6(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x51
}
class GeneralPurpose7(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x52
}
class GeneralPurpose8(channel: Int, value: Int): VariableControlChange(channel, value) {
    override val controller = 0x53
}

class DataIncrement(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x60
}
class DataDecrement(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x61
}
class AllControllersOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x79
}
class AllNotesOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7B
}
class AllSoundOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x78
}
class OmniOff(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7C
}
class OmniOn(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0x7D
}
class PolyphonicOperation(channel: Int): VariableControlChange(channel, 0) {
    override val controller = 0xFF
}

class ProgramChange(var channel: Int, var program: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xC0 or this.channel).toByte(),
            this.program.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun get_program(): Int {
        return this.program
    }
    fun set_program(program: Int) {
        this.program = program
    }
}

class ChannelPressure(var channel: Int, var pressure: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0xD0 or this.channel).toByte(),
            this.pressure.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun get_pressure(): Int {
        return this.pressure
    }
    fun set_pressure(pressure: Int) {
        this.pressure = pressure
    }
}

class PitchWheelChange(var channel: Int, var value: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var unsigned_value = this.get_unsigned_value()
        var least = unsigned_value and 0x007F
        var most = (unsigned_value shr 8) and 0x007F
        return byteArrayOf(
            (0xE0 or this.channel).toByte(),
            least.toByte(),
            most.toByte()
        )
    }

    fun get_channel(): Int {
        return this.channel
    }
    fun set_channel(channel: Int) {
        this.channel = channel
    }
    fun get_value(): Int {
        return this.value
    }
    fun set_value(value: Int) {
        this.value = value
    }

    fun get_unsigned_value(): Int {
        return if (this.value == 0) {
            0x2000
        } else {
            ((this.value + 1) * 0x3FFF) / 2
        }
    }
}

class SystemExclusive(var data: ByteArray): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF0.toByte()) + this.data + byteArrayOf(0xF7.toByte())
    }

    fun get_data(): ByteArray {
        return this.data
    }

    fun set_data(new_data: ByteArray) {
        this.data = new_data
    }
}

class MTCQuarterFrame(var time_code: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF1.toByte(), this.time_code.toByte())
    }

    fun set_time_code(new_value: Int) {
        this.time_code = new_value
    }
    fun get_time_code(): Int {
        return this.time_code
    }
}

class SongPositionPointer(var beat: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var least = this.beat and 0x007F
        var most = (this.beat shr 8) and 0x007F

        return byteArrayOf(
            0xF2.toByte(),
            least.toByte(),
            most.toByte()
        )
    }

    fun get_beat(): Int {
        return this.beat
    }
    fun set_beat(beat: Int) {
        this.beat = beat
    }
}

class SongSelect(var song: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF3.toByte(),
            (this.song and 0xFF).toByte()
        )
    }

    fun set_song(song: Int) {
        this.song = song
    }
    fun get_song(): Int {
        return song
    }
}

class TuneRequest: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF6.toByte())
    }
}
class MIDIClock: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF8.toByte())
    }
}
class MIDIStart: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFA.toByte())
    }
}
class MIDIContinue: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFB.toByte())
    }
}
class MIDIStop: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFC.toByte())
    }
}
class ActiveSense: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFE.toByte())
    }
}
class Reset: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFF.toByte())
    }
}

class TimeCode(var rate: Int, var hour: Int, var minute: Int, var second: Int, var frame: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            ((this.rate shl 5) + this.hour).toByte(),
            (this.minute and 0x3F).toByte(),
            (this.second and 0x3F).toByte(),
            (this.frame and 0x1F).toByte()
        )
    }
}

class MIDI {
    var ppqn: Int = 120
    var midi_format: Int = 1
    var events = HashMap<Int, MIDIEvent>()
    var event_id_gen: Int = 1
    var event_positions = HashMap<Int, Pair<Int, Int>>()
    var _active_byte: Byte? = null

    public fun from_path(file_path: String): MIDI {
        var midibytes = File(file_path).readBytes()
        return MIDI.from_bytes(midibytes)
    }
    companion object {
        fun from_bytes(file_bytes: ByteArray): MIDI {
            var working_bytes: MutableList<Byte> = mutableListOf()
            for (b in file_bytes) {
                working_bytes.add(b)
            }
            var mlo = MIDI()
            var sub_bytes: MutableList<Byte> = mutableListOf()
            var chunkcount = HashMap<String, Int>()
            var current_track: Int = 0
            var current_deltatime: Int = 0
            var chunk_type: String = ""

            var divword = 0
            var midi_format = 0
            var track_length = 0
            var found_header = false
            var ppqn = 120
            while (working_bytes.isNotEmpty()) {
                chunk_type = ""
                for (i in 0 .. 4) {
                    chunk_type = "${chunk_type}${working_bytes.removeFirst() as Char}"
                }
                var value: Int
                if (chunkcount.containsKey(chunk_type)) {
                    value = chunkcount[chunk_type]!!
                    chunkcount[chunk_type] = value + 1
                } else {
                    value = 1
                    chunkcount[chunk_type] = 0
                }


                when (chunk_type) {
                    "MThd" -> {
                        dequeue_n(working_bytes, 4) // Get Size
                        midi_format = dequeue_n(working_bytes, 2)
                        dequeue_n(working_bytes, 4) // Get Number of tracks
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
                            throw Exception("MISSING MTrk")
                        }
                        current_deltatime = 0
                        track_length = dequeue_n(working_bytes, 4)
                        sub_bytes = mutableListOf()
                        for (i in 0 until track_length) {
                            sub_bytes.add(working_bytes.removeFirst())
                        }
                        while (sub_bytes.isNotEmpty()) {
                            current_deltatime += get_variable_length_number(sub_bytes)
                            var eid = mlo.process_mtrk_event(sub_bytes, current_deltatime, current_track)
                        }
                        current_track += 1
                    }
                    else -> {
                        throw Exception("Invalid Bytes")
                    }
                }
            }
            return mlo
        }
    }

    fun process_mtrk_event(bytes: MutableList<Byte>, current_deltatime: Int, track: Int): Int {
        if (bytes.first() in 0x80..0xef) {
            this._active_byte = bytes.first()!!
        }

        var event: MIDIEvent? = event_from_bytes(bytes, this._active_byte!!) ?: throw Exception("Invalid Bytes")

        return this.insert_event(track, current_deltatime, event!!)
    }
    public fun as_bytes(): ByteArray {
        var output: MutableList<Byte> = mutableListOf(
            'M'.toByte(),
            'T'.toByte(),
            'h'.toByte(),
            'd'.toByte()
        )

        var format = this.get_format()
        output.add((format / 256).toByte())
        output.add((format % 256).toByte())

        var track_count = this.count_tracks()
        output.add((track_count / 256).toByte())
        output.add((track_count % 256).toByte())

        var ppqn = this.get_ppqn()
        output.add((ppqn / 256).toByte())
        output.add((ppqn % 256).toByte())

        var track_event_bytes: MutableList<Byte>
        var track_byte_length: Int = 0
        var tracks = this.get_tracks()

        for (ticks in tracks) {
            output.add('M'.toByte())
            output.add('T'.toByte())
            output.add('r'.toByte())
            output.add('k'.toByte())

            track_event_bytes = mutableListOf()
            for (pair in ticks) {
                var tick_delay = pair.first
                var eid = pair.second
                var working_event = this.get_event(eid)
                if (working_event != null) {
                    track_event_bytes += to_variable_length_bytes(tick_delay)
                    track_event_bytes += working_event.as_bytes().toMutableList()
                }
            }
            // Automatically handle EndOfTrackEvent Here instead of requiring it to be in the MIDITrack object
            track_event_bytes.add(0x00)
            track_event_bytes += EndOfTrack().as_bytes().toMutableList()

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

    // Save the midi object to a file
    fun save(path: String) {
        var bytes = this.as_bytes()
        File(path).writeBytes(bytes)
    }

    // Get the track and tick of and event, given its id
    fun get_event_position(event_id: Int): Pair<Int, Int>? {
        return this.event_positions[event_id]
    }

    fun get_tracks(): List<List<Pair<Int, Int>>> {
        var tracks: MutableList<MutableList<Pair<Int, Int>>> = mutableListOf()
        for (eid in this.event_positions.keys) {
            var track = this.event_positions[eid]?.first!!
            var tick = this.event_positions[eid]?.second!!
            while (tracks.size <= track) {
                tracks.add(mutableListOf())
            }
            tracks[track].add(Pair(tick, eid))
        }

        var output: MutableList<MutableList<Pair<Int, Int>>> = mutableListOf()
        for (unsorted_track in tracks) {
            var track = unsorted_track.sortedBy { it.first }
            var current: MutableList<Pair<Int, Int>> = mutableListOf()
            var previous_tick: Int = 0
            for (pair in track) {
                var current_tick = pair.first
                var eid = pair.second
                current.add(Pair(current_tick - previous_tick, eid))
                previous_tick = current_tick
            }
            output.add(current)
        }
        return output
    }

    fun count_tracks(): Int {
        var used_tracks = HashSet<Int>()
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
                max_tick = max(max_tick, pair.second)
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

    fun insert_event(track: Int, tick: Int, event: MIDIEvent): Int {
        if (track > 15) {
            throw Exception("TrackOutOfBounds")
        }
        var new_event_id = this.event_id_gen
        this.event_id_gen += 1

        this.events[new_event_id] = event
        this.move_event(track, tick, new_event_id)

        return new_event_id
    }

    fun move_event(new_track: Int, new_tick: Int, event_id: Int) {
        this.event_positions[event_id] = Pair(new_track, new_tick)
    }

    fun push_event(track: Int, wait: Int, event: MIDIEvent): Int {
        if (track > 15) {
            throw Exception("TrackOutOfBounds")
        }

        var new_event_id = this.event_id_gen
        this.event_id_gen += 1
        this.events[new_event_id] = event

        var last_tick_in_track = this.get_track_length(track) - 1
        this.move_event(track, last_tick_in_track + wait, new_event_id)

        return new_event_id
    }

    fun get_event(event_id: Int): MIDIEvent? {
        var output: MIDIEvent? = this.events[event_id]
        return output
    }

    fun replace_event(event_id: Int, new_midi_event: MIDIEvent) {
        if (!this.events.containsKey(event_id)) {
            throw Exception("EventNotFound: ${event_id}")
        }
        this.events[event_id] = new_midi_event
    }

    fun get_all_events(): List<Pair<Int, MIDIEvent>> {
        var output: MutableList<Pair<Int, MIDIEvent>> = mutableListOf()
        for (eid in this.event_positions.keys) {
            var tick = this.event_positions[eid]!!.second
            output.add(Pair(tick, this.events[eid]!!))
        }

        return output.sortedBy { it.first }
    }
}

fun dequeue_n(bytelist: MutableList<Byte>, n: Int): Int {
    var output = 0
    for (_i in 0 until n) {
        output *= 256
        var x = bytelist.removeFirst()
        output += x as Int
    }
    return output
}

fun get_variable_length_number(bytes: MutableList<Byte>): Int {
    var output: Int = 0

    while (true) {
        output = output shl 7
        var x = bytes.removeFirst() as Int
        output = output or (x and 0x7F)
        if (x and 0x80 == 0) {
            break
        }
    }
    return output
}

fun to_variable_length_bytes(number: Int): List<Byte> {
    var output: MutableList<Byte> = mutableListOf()
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
    return output.reversed()
}

fun get_pitchwheel_value(n: Float): Int {
    var output = if (n < 0) {
        ((1 + n) * (0x2000)).toInt()
    } else if (n > 0) {
        ((n * 0x1FFF) as Int) + 0x2000
    } else {
        0x2000
    }
    return output
}

fun build_key_signature(mi: Byte, sf: Byte): KeySignature {
    var chord_name = get_chord_name_from_mi_sf(mi, sf)
    return KeySignature(chord_name)
}

fun build_pitch_wheel_change(channel: Byte, lsb: Byte, msb: Byte): PitchWheelChange {
    var unsigned_value = (msb as Int shl 8) + (lsb as Int)
    var new_value: Float = ((unsigned_value.toFloat() * 2.toFloat()) / 0x3FFF.toFloat()) - 1
    return PitchWheelChange(channel as Int, new_value as Int)
}

fun get_mi_sf(chord_name: String): Pair<Byte, Byte> {
    var output: Pair<Byte, Byte> = when (chord_name) {
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
    var map: List<List<String>> = listOf(
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

    return map[mi as Int][(sf as Int) + 7]
}
