package com.qfs.radixulous.apres

import android.content.Context
import android.media.midi.*
import android.media.midi.MidiManager.OnDeviceOpenedListener
import android.util.Log
import com.qfs.radixulous.apres.riffreader.toUInt
import kotlinx.coroutines.*
import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or


interface MIDIEvent {
    abstract fun as_bytes(): ByteArray
}

fun event_from_bytes(bytes: MutableList<Byte>, default: Byte): MIDIEvent? {
    var output: MIDIEvent? = null
    var leadbyte = toUInt(bytes.removeFirst())
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
                val note = bytes.removeFirst().toInt()
                val velocity = bytes.removeFirst().toInt()
                output = NoteOff(channel, note, velocity)
            }
            0x9 -> {
                val channel = (leadbyte and 0x0F)
                val note = bytes.removeFirst().toInt()
                val velocity = bytes.removeFirst().toInt()
                output = if (velocity == 0) {
                    NoteOff(channel, note, velocity)
                } else {
                    NoteOn(channel, note, velocity)
                }
            }
            0xA -> {
                val channel = (leadbyte and 0x0F)
                val note = bytes.removeFirst().toInt()
                val velocity = bytes.removeFirst().toInt()
                output = PolyphonicKeyPressure(channel, note, velocity)
            }
            0xB -> {
                val channel = (leadbyte and 0x0F)
                val controller = bytes.removeFirst().toInt()
                val value = bytes.removeFirst().toInt()
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
                    (leadbyte and 0x0F),
                    bytes.removeFirst().toInt()
                )
            }
            0xD -> {
                output = ChannelPressure(
                    (leadbyte and 0x0F),
                    bytes.removeFirst().toInt()
                )
            }
            0xE -> {
                output = build_pitch_wheel_change(
                    (leadbyte and 0x0F).toByte(),
                    bytes.removeFirst(),
                    bytes.removeFirst()
                )
            }
            else -> { }
        }
    } else if (leadbyte == 0xF0) {
        val bytedump: MutableList<Byte> = mutableListOf()
        while (true) {
            val byte = bytes.removeFirst()
            if (byte.toInt() == 0xF7) {
                break
            } else {
                bytedump.add(byte)
            }
        }
        output = SystemExclusive(bytedump.toByteArray())
    } else if (leadbyte == 0xF2) {
        val lsb = bytes.removeFirst().toInt()
        val msb = bytes.removeFirst().toInt()
        val beat: Int = (msb shl 8) + lsb
        output = SongPositionPointer(beat)
    } else if (leadbyte == 0xF3) {
        output = SongSelect((bytes.removeFirst().toInt()) and 0x7F)
    } else if (leadbyte == 0xFF) {
        val meta_byte = bytes.removeFirst().toInt()
        val varlength = get_variable_length_number(bytes)

        if (meta_byte == 0x51) {
            output = SetTempo(dequeue_n(bytes, varlength))
        } else {
            val bytedump_list: MutableList<Byte> = mutableListOf()
            for (i in 0 until varlength) {
                bytedump_list.add(bytes.removeFirst())
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
                        bytedump_list.removeFirst()
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

data class SequenceNumber(var sequence: Int): MIDIEvent {
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

data class Text(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x01.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

data class CopyRightNotice(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x02.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }
    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

data class TrackName(var name: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val name_bytes = this.name.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x03.toByte()) + to_variable_length_bytes(name_bytes.size) + name_bytes
    }

    fun get_name(): String {
        return this.name
    }

    fun set_name(new_name: String) {
        this.name = new_name
    }
}

data class InstrumentName(var name: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val name_bytes = this.name.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x04.toByte()) + to_variable_length_bytes(name_bytes.size) + name_bytes
    }

    fun get_name(): String {
        return this.name
    }

    fun set_name(new_name: String) {
        this.name = new_name
    }
}

data class Lyric(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x05.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

data class Marker(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val text_bytes = this.text.toByteArray()
        return byteArrayOf(0xFF.toByte(), 0x06.toByte()) + to_variable_length_bytes(text_bytes.size) + text_bytes
    }

    fun get_text(): String {
        return this.text
    }

    fun set_text(new_text: String) {
        this.text = new_text
    }
}

data class CuePoint(var text: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val text_bytes = this.text.toByteArray()
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

data class ChannelPrefix(var channel: Int): MIDIEvent {
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

data class SetTempo(var uspqn: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x51.toByte(),
            0x03.toByte(),
            ((this.uspqn shr 16) and 0xFF).toByte(),
            ((this.uspqn shr 8) and 0xFF).toByte(),
            (this.uspqn and 0xFF).toByte()
        )
    }
    companion object {
        fun from_bpm(bpm: Float): SetTempo {
            return SetTempo((60000000.toFloat() / bpm).toInt())
        }
    }

    fun get_bpm(): Float {
        val uspqn = this.get_uspqn()
        return if (uspqn > 0) {
            60000000.toFloat() / uspqn
        } else {
            0.toFloat()
        }
    }

    fun get_uspqn(): Int {
        return this.uspqn
    }

    fun set_uspqn(new_uspqn: Int) {
        this.uspqn = new_uspqn
    }

    fun set_bpm(new_bpm: Float) {
        if (new_bpm > 0) {
            this.uspqn = (60000000.toFloat() / new_bpm) as Int
        } else {
            this.uspqn = 0
        }
    }
}

data class SMPTEOffset(var hour: Int, var minute: Int, var second: Int, var ff: Int, var fr: Int): MIDIEvent {
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

data class TimeSignature(var numerator: Int, var denominator: Int, var clocks_per_metronome: Int, var thirtysecondths_per_quarter: Int): MIDIEvent {
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

data class KeySignature(var key: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val misf = get_mi_sf(this.key)
        return byteArrayOf(
            0xFF.toByte(),
            0x59.toByte(),
            0x02.toByte(),
            misf.second,
            misf.first
        )
    }

    companion object {
        fun from_mi_sf(mi: Byte, sf: Byte): KeySignature {
            val chord_name = get_chord_name_from_mi_sf(mi, sf)
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

data class SequencerSpecific(var data: ByteArray): MIDIEvent {
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

data class NoteOn(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
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

data class NoteOff(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
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


data class PolyphonicKeyPressure(var channel: Int, var note: Int, var velocity: Int): MIDIEvent {
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

data class ProgramChange(var channel: Int, var program: Int): MIDIEvent {
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

data class ChannelPressure(var channel: Int, var pressure: Int): MIDIEvent {
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

data class PitchWheelChange(var channel: Int, var value: Float): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val unsigned_value = this.get_unsigned_value()
        val least = unsigned_value and 0x007F
        val most = (unsigned_value shr 8) and 0x007F
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
    fun get_value(): Float {
        return this.value
    }
    fun set_value(value: Float) {
        this.value = value
    }

    fun get_unsigned_value(): Int {
        return if (this.value == 0.toFloat()) {
            0x2000
        } else if (this.value < 0) {
            ((1.toFloat() + this.value) * 0x2000.toFloat()).toInt()
        } else {
            (this.value * 0x1FFF.toFloat()).toInt() + 0x2000
        }
    }
}

data class SystemExclusive(var data: ByteArray): MIDIEvent {
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

data class MTCQuarterFrame(var time_code: Int): MIDIEvent {
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

data class SongPositionPointer(var beat: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val least = this.beat and 0x007F
        val most = (this.beat shr 8) and 0x007F

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

data class SongSelect(var song: Int): MIDIEvent {
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

data class TimeCode(var rate: Int, var hour: Int, var minute: Int, var second: Int, var frame: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            ((this.rate shl 5) + this.hour).toByte(),
            (this.minute and 0x3F).toByte(),
            (this.second and 0x3F).toByte(),
            (this.frame and 0x1F).toByte()
        )
    }
}

data class MetaEvent(var byte: Byte, var bytes: ByteArray): MIDIEvent {
    override fun as_bytes(): ByteArray {
        var output = mutableListOf<Byte>(0xFF.toByte(), this.byte)
        for (b in this.bytes) {
            output.add(b)
        }
        return output.toByteArray()
    }
}

class MIDI {
    class MissingMThd(): Exception("Missing MThd")
    class InvalidChunkType(string: String): Exception("Invalid Chunk Type: $string")
    class TrackOOB(index: Int): Exception("Track $index Out of Bounds")
    class InvalidEventId(id: Int): Exception("No event mapped to id:$id")
    var ppqn: Int = 120
    var midi_format: Int = 1
    var events = HashMap<Int, MIDIEvent>()
    var event_id_gen: Int = 1
    var event_positions = HashMap<Int, Pair<Int, Int>>()
    var _active_byte: Byte = 0x90.toByte()

    companion object {
        fun from_path(file_path: String): MIDI {
            val midibytes = File(file_path).readBytes()
            return MIDI.from_bytes(midibytes)
        }

        fun from_bytes(file_bytes: ByteArray): MIDI {
            val working_bytes = file_bytes.toMutableList()
            val mlo = MIDI()
            var sub_bytes: MutableList<Byte> = mutableListOf()
            val chunkcount = HashMap<String, Int>()
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
                            mlo.process_mtrk_event(sub_bytes, current_deltatime, current_track)
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
    }

    fun process_mtrk_event(bytes: MutableList<Byte>, current_deltatime: Int, track: Int): Int {
        if (bytes.first() != null && bytes.first() in 0x80..0xEF) {
            this._active_byte = bytes.first()!!
        }



        return try {
            val event: MIDIEvent? = event_from_bytes(bytes, this._active_byte)
            if (event != null) {
                var first_byte = toUInt(event.as_bytes().first())
                if (first_byte in 0x90..0xEF) {
                    this._active_byte = event.as_bytes().first()
                } else if (event is NoteOff) {
                    this._active_byte = 0x10.toByte() or event.as_bytes().first()
                }
            }
            this.insert_event(track, current_deltatime, event!!)
        } catch (e: Exception) {
            -1
        }
    }

    public fun as_bytes(): ByteArray {
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

        val format = this.get_format()
        output.add((format / 256).toByte())
        output.add((format % 256).toByte())

        val track_count = this.count_tracks()
        output.add((track_count / 256).toByte())
        output.add((track_count % 256).toByte())

        val ppqn = this.get_ppqn()
        output.add((ppqn / 256).toByte())
        output.add((ppqn % 256).toByte())

        var track_event_bytes: MutableList<Byte>
        var track_byte_length: Int = 0
        val tracks = this.get_tracks()

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
                val working_event = this.get_event(eid)
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

    fun insert_event(track: Int, tick: Int, event: MIDIEvent): Int {
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

    fun push_event(track: Int, wait: Int, event: MIDIEvent): Int {
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

    fun get_event(event_id: Int): MIDIEvent? {
        val output: MIDIEvent? = this.events[event_id]
        return output
    }

    fun replace_event(event_id: Int, new_midi_event: MIDIEvent) {
        if (!this.events.containsKey(event_id)) {
            throw InvalidEventId(event_id)
        }
        this.events[event_id] = new_midi_event
    }

    fun get_all_events(): List<Pair<Int, MIDIEvent>> {
        val output: MutableList<Pair<Int, MIDIEvent>> = mutableListOf()
        for (eid in this.event_positions.keys) {
            val tick = this.event_positions[eid]!!.second
            output.add(Pair(tick, this.events[eid]!!))
        }

        return output.sortedBy { it.first }
    }

    fun get_all_events_grouped(): List<Pair<Int, List<MIDIEvent>>> {
        var event_pairs = this.get_all_events()
        var output = mutableListOf<Pair<Int, List<MIDIEvent>>>()
        var working_pair: Pair<Int, MutableList<MIDIEvent>>? = null
        for ((tick, event) in event_pairs) {
            if (working_pair != null && working_pair.first != tick) {
                output.add(
                    Pair(
                        working_pair.first,
                        working_pair.second.sortedBy {
                            return@sortedBy when (it) {
                                is NoteOn -> { 1 }
                                is NoteOff -> { -1 }
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
                            else -> { 0 }
                        }
                    }
                )
            )
        }
        return output
    }
}

fun dequeue_n(bytelist: MutableList<Byte>, n: Int): Int {
    var output = 0
    for (_i in 0 until n) {
        output *= 256
        val x = toUInt(bytelist.removeFirst())
        output += x
    }
    return output
}

fun get_variable_length_number(bytes: MutableList<Byte>): Int {
    var output: Int = 0
    while (true) {
        output = output shl 7
        val x = toUInt(bytes.removeFirst())
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
    return output.reversed()
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
    val unsigned_value = ((msb.toInt() and 0xFF) shl 8) + (lsb.toInt() and 0xFF)
    val new_value: Float = ((unsigned_value.toFloat() * 2.toFloat()) / 0x3FFF.toFloat()) - 1
    return PitchWheelChange(channel.toInt(), new_value)
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

// Reference code base on https://github.com/android/ndk-samples/tree/main/native-midi
open class MIDIController(var context: Context) {
    var virtualDevices: MutableList<VirtualMIDIDevice> = mutableListOf()

    fun registerVirtualDevice(device: VirtualMIDIDevice) {
        this.virtualDevices.add(device)
        device.setMidiController(this)
    }
    fun unregisterVirtualDevice(device: VirtualMIDIDevice) {
        var index = this.virtualDevices.indexOf(device)
        if (index >= 0) {
            this.virtualDevices.removeAt(index)
        }
    }

    // TODO: Native midi support
    //private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager


    //// Selected Device(s)
    //private var incomingDevice : MidiDevice? = null
    //private var outgoingDevice : MidiDevice? = null
    //private val outgoingPort: MidiInputPort? = null

    //private var process_queue: MutableList<MIDIEvent> = mutableListOf()

    //init {
    //    val midiDevices = getIncomingDevices() // method defined in snippet above
    //    if (midiDevices.isNotEmpty()) {
    //        this.openIncomingDevice(midiDevices[0])
    //    }
    //}

    //class OpenIncomingDeviceListener : OnDeviceOpenedListener {
    //    open external fun startReadingMidi(incomingDevice: MidiDevice?, portNumber: Int)
    //    open external fun stopReadingMidi()
    //    override fun onDeviceOpened(device: MidiDevice) {
    //        this.startReadingMidi(device, 0 /*mPortNumber*/)
    //    }
    //}

    //open fun openIncomingDevice(devInfo: MidiDeviceInfo?) {
    //    midiManager.openDevice(devInfo, OpenIncomingDeviceListener(), null)
    //}

    //open fun closeIncomingDevice() {
    //    if (this.incomingDevice != null) {
    //        // Native API
    //        this.incomingDevice = null
    //    }
    //}
    fun receiveMessage(event: MIDIEvent, source: VirtualMIDIDevice) {
        // Rebroadcast to listening devices
        for (device in this.virtualDevices) {
            if (device == source) {
                continue
            }
            device.receiveMessage(event)
        }
    }

    //open fun onNativeMessageReceive(message: ByteArray) {
    //    var event = event_from_bytes(message.toMutableList()) ?: return
    //    this.receiveMessage(event)
    //    this.process_queue.add(event)
    //    // Messages are received on some other thread, so switch to the UI thread
    //    // before attempting to access the UI
    //    // UiThreadStatement.runOnUiThread(Runnable { showReceivedMessage(message) })
    //}

    //// Send Device
    //class OpenOutgoingDeviceListener : OnDeviceOpenedListener {
    //    open external fun startWritingMidi(sendDevice: MidiDevice?, portNumber: Int)
    //    open external fun stopWritingMidi()
    //    override fun onDeviceOpened(device: MidiDevice) {
    //        this.startWritingMidi(device, 0 /*mPortNumber*/)
    //    }
    //}

    //open fun openSendDevice(devInfo: MidiDeviceInfo?) {
    //    this.midiManager.openDevice(devInfo, OpenOutgoingDeviceListener(), null)
    //}

    //open fun closeSendDevice() {
    //    if (this.outgoingDevice != null) {
    //        // Native API
    //        this.outgoingDevice = null
    //    }
    //}

    //fun sendMessage(event: MIDIEvent) {
    //    var bytes = event.as_bytes()
    //    this.writeMidi(bytes, bytes.size)
    //}

    //private fun getOutgoingDevices(): List<MidiDeviceInfo> {
    //    return midiManager.devices.filter { it.outputPortCount > 0 }
    //}

    //private fun getIncomingDevices() : List<MidiDeviceInfo> {
    //    return midiManager.devices.filter { it.inputPortCount > 0 }
    //}

    ////
    //// Native API stuff
    ////
    //open fun loadNativeAPI() {
    //    System.loadLibrary("native_midi")
    //}

    //open external fun writeMidi(data: ByteArray?, length: Int)
}

open class VirtualMIDIDevice {
    private var midi_controller: MIDIController? = null
    fun setMidiController(controller: MIDIController) {
        this.midi_controller = controller
    }

    fun is_registered(): Boolean {
        return this.midi_controller != null
    }

    fun sendEvent(event: MIDIEvent) {
        // TODO: Throw error?
        if (is_registered()) {
            this.midi_controller!!.receiveMessage(event, this)
        }
    }

    fun receiveMessage(event: MIDIEvent) {
        when (event) {
            is SequenceNumber -> this.onSequenceNumber(event)
            is Text -> this.onText(event)
            is CopyRightNotice -> this.onCopyRightNotice(event)
            is TrackName -> this.onTrackName(event)
            is InstrumentName -> this.onInstrumentName(event)
            is Lyric -> this.onLyric(event)
            is Marker -> this.onMarker(event)
            is CuePoint -> this.onCuePoint(event)
            is EndOfTrack -> this.onEndOfTrack(event)
            is ChannelPrefix -> this.onChannelPrefix(event)
            is SetTempo -> this.onSetTempo(event)
            is SMPTEOffset -> this.onSMPTEOffset(event)
            is TimeSignature -> this.onTimeSignature(event)
            is KeySignature -> this.onKeySignature(event)
            is SequencerSpecific -> this.onSequencerSpecific(event)
            is NoteOn -> this.onNoteOn(event)
            is NoteOff -> this.onNoteOff(event)
            is PolyphonicKeyPressure -> this.onPolyphonicKeyPressure(event)
            is HoldPedal -> this.onHoldPedal(event)
            is Portamento -> this.onPortamento(event)
            is Sustenuto -> this.onSustenuto(event)
            is SoftPedal -> this.onSoftPedal(event)
            is Legato -> this.onLegato(event)
            is Hold2Pedal -> this.onHold2Pedal(event)
            is SoundVariation -> this.onSoundVariation(event)
            is SoundTimbre -> this.onSoundTimbre(event)
            is SoundReleaseTime -> this.onSoundReleaseTime(event)
            is SoundAttack -> this.onSoundAttack(event)
            is SoundBrightness -> this.onSoundBrightness(event)
            is SoundControl1 -> this.onSoundControl1(event)
            is SoundControl2 -> this.onSoundControl2(event)
            is SoundControl3 -> this.onSoundControl3(event)
            is SoundControl4 -> this.onSoundControl4(event)
            is SoundControl5 -> this.onSoundControl5(event)
            is EffectsLevel -> this.onEffectsLevel(event)
            is TremuloLevel -> this.onTremuloLevel(event)
            is ChorusLevel -> this.onChorusLevel(event)
            is CelesteLevel -> this.onCelesteLevel(event)
            is PhaserLevel -> this.onPhaserLevel(event)
            is LocalControl -> this.onLocalControl(event)
            is MonophonicOperation -> this.onMonophonicOperation(event)
            is BankSelect -> this.onBankSelect(event)
            is BankSelectLSB -> this.onBankSelectLSB(event)
            is ModulationWheel -> this.onModulationWheel(event)
            is ModulationWheelLSB -> this.onModulationWheelLSB(event)
            is BreathController -> this.onBreathController(event)
            is BreathControllerLSB -> this.onBreathControllerLSB(event)
            is FootPedal -> this.onFootPedal(event)
            is FootPedalLSB -> this.onFootPedalLSB(event)
            is PortamentoTime -> this.onPortamentoTime(event)
            is PortamentoTimeLSB -> this.onPortamentoTimeLSB(event)
            is DataEntry -> this.onDataEntry(event)
            is DataEntryLSB -> this.onDataEntryLSB(event)
            is Volume -> this.onVolume(event)
            is VolumeLSB -> this.onVolumeLSB(event)
            is Balance -> this.onBalance(event)
            is BalanceLSB -> this.onBalanceLSB(event)
            is Pan -> this.onPan(event)
            is PanLSB -> this.onPanLSB(event)
            is Expression -> this.onExpression(event)
            is ExpressionLSB -> this.onExpressionLSB(event)
            is NonRegisteredParameterNumber -> this.onNonRegisteredParameterNumber(event)
            is NonRegisteredParameterNumberLSB -> this.onNonRegisteredParameterNumberLSB(event)
            is RegisteredParameterNumber -> this.onRegisteredParameterNumber(event)
            is RegisteredParameterNumberLSB -> this.onRegisteredParameterNumberLSB(event)
            is EffectControl1 -> this.onEffectControl1(event)
            is EffectControl1LSB -> this.onEffectControl1LSB(event)
            is EffectControl2 -> this.onEffectControl2(event)
            is EffectControl2LSB -> this.onEffectControl2LSB(event)
            is GeneralPurpose1 -> this.onGeneralPurpose1(event)
            is GeneralPurpose1LSB -> this.onGeneralPurpose1LSB(event)
            is GeneralPurpose2 -> this.onGeneralPurpose2(event)
            is GeneralPurpose2LSB -> this.onGeneralPurpose2LSB(event)
            is GeneralPurpose3 -> this.onGeneralPurpose3(event)
            is GeneralPurpose3LSB -> this.onGeneralPurpose3LSB(event)
            is GeneralPurpose4 -> this.onGeneralPurpose4(event)
            is GeneralPurpose4LSB -> this.onGeneralPurpose4LSB(event)
            is GeneralPurpose5 -> this.onGeneralPurpose5(event)
            is GeneralPurpose6 -> this.onGeneralPurpose6(event)
            is GeneralPurpose7 -> this.onGeneralPurpose7(event)
            is GeneralPurpose8 -> this.onGeneralPurpose8(event)
            is DataIncrement -> this.onDataIncrement(event)
            is DataDecrement -> this.onDataDecrement(event)
            is AllControllersOff -> this.onAllControllersOff(event)
            is AllNotesOff -> this.onAllNotesOff(event)
            is AllSoundOff -> this.onAllSoundOff(event)
            is OmniOff -> this.onOmniOff(event)
            is OmniOn -> this.onOmniOn(event)
            is PolyphonicOperation -> this.onPolyphonicOperation(event)
            is ProgramChange -> this.onProgramChange(event)
            is ChannelPressure -> this.onChannelPressure(event)
            is PitchWheelChange -> this.onPitchWheelChange(event)
            is SystemExclusive -> this.onSystemExclusive(event)
            is MTCQuarterFrame -> this.onMTCQuarterFrame(event)
            is SongPositionPointer -> this.onSongPositionPointer(event)
            is SongSelect -> this.onSongSelect(event)
            is TuneRequest -> this.onTuneRequest(event)
            is MIDIClock -> this.onMIDIClock(event)
            is MIDIStart -> this.onMIDIStart(event)
            is MIDIContinue -> this.onMIDIContinue(event)
            is MIDIStop -> this.onMIDIStop(event)
            is ActiveSense -> this.onActiveSense(event)
            is Reset -> this.onReset(event)
            is TimeCode -> this.onTimeCode(event)
        }
    }

    // abstract functions
    open fun onSequenceNumber(event: SequenceNumber) { }
    open fun onText(event: Text) { }
    open fun onCopyRightNotice(event: CopyRightNotice) { }
    open fun onTrackName(event: TrackName) { }
    open fun onInstrumentName(event: InstrumentName) { }
    open fun onLyric(event: Lyric) { }
    open fun onMarker(event: Marker) { }
    open fun onCuePoint(event: CuePoint) { }
    open fun onEndOfTrack(event: EndOfTrack) { }
    open fun onChannelPrefix(event: ChannelPrefix) { }
    open fun onSetTempo(event: SetTempo) { }
    open fun onSMPTEOffset(event: SMPTEOffset) { }
    open fun onTimeSignature(event: TimeSignature) { }
    open fun onKeySignature(event: KeySignature) { }
    open fun onSequencerSpecific(event: SequencerSpecific) { }
    open fun onNoteOn(event: NoteOn) { }
    open fun onNoteOff(event: NoteOff) { }
    open fun onPolyphonicKeyPressure(event: PolyphonicKeyPressure) { }
    open fun onHoldPedal(event: HoldPedal) { }
    open fun onPortamento(event: Portamento) { }
    open fun onSustenuto(event: Sustenuto) { }
    open fun onSoftPedal(event: SoftPedal) { }
    open fun onLegato(event: Legato) { }
    open fun onHold2Pedal(event: Hold2Pedal) { }
    open fun onSoundVariation(event: SoundVariation) { }
    open fun onSoundTimbre(event: SoundTimbre) { }
    open fun onSoundReleaseTime(event: SoundReleaseTime) { }
    open fun onSoundAttack(event: SoundAttack) { }
    open fun onSoundBrightness(event: SoundBrightness) { }
    open fun onSoundControl1(event: SoundControl1) { }
    open fun onSoundControl2(event: SoundControl2) { }
    open fun onSoundControl3(event: SoundControl3) { }
    open fun onSoundControl4(event: SoundControl4) { }
    open fun onSoundControl5(event: SoundControl5) { }
    open fun onEffectsLevel(event: EffectsLevel) { }
    open fun onTremuloLevel(event: TremuloLevel) { }
    open fun onChorusLevel(event: ChorusLevel) { }
    open fun onCelesteLevel(event: CelesteLevel) { }
    open fun onPhaserLevel(event: PhaserLevel) { }
    open fun onLocalControl(event: LocalControl) { }
    open fun onMonophonicOperation(event: MonophonicOperation) { }
    open fun onBankSelect(event: BankSelect) { }
    open fun onBankSelectLSB(event: BankSelectLSB) { }
    open fun onModulationWheel(event: ModulationWheel) { }
    open fun onModulationWheelLSB(event: ModulationWheelLSB) { }
    open fun onBreathController(event: BreathController) { }
    open fun onBreathControllerLSB(event: BreathControllerLSB) { }
    open fun onFootPedal(event: FootPedal) { }
    open fun onFootPedalLSB(event: FootPedalLSB) { }
    open fun onPortamentoTime(event: PortamentoTime) { }
    open fun onPortamentoTimeLSB(event: PortamentoTimeLSB) { }
    open fun onDataEntry(event: DataEntry) { }
    open fun onDataEntryLSB(event: DataEntryLSB) { }
    open fun onVolume(event: Volume) { }
    open fun onVolumeLSB(event: VolumeLSB) { }
    open fun onBalance(event: Balance) { }
    open fun onBalanceLSB(event: BalanceLSB) { }
    open fun onPan(event: Pan) { }
    open fun onPanLSB(event: PanLSB) { }
    open fun onExpression(event: Expression) { }
    open fun onExpressionLSB(event: ExpressionLSB) { }
    open fun onNonRegisteredParameterNumber(event: NonRegisteredParameterNumber) { }
    open fun onNonRegisteredParameterNumberLSB(event: NonRegisteredParameterNumberLSB) { }
    open fun onRegisteredParameterNumber(event: RegisteredParameterNumber) { }
    open fun onRegisteredParameterNumberLSB(event: RegisteredParameterNumberLSB) { }
    open fun onEffectControl1(event: EffectControl1) { }
    open fun onEffectControl1LSB(event: EffectControl1LSB) { }
    open fun onEffectControl2(event: EffectControl2) { }
    open fun onEffectControl2LSB(event: EffectControl2LSB) { }
    open fun onGeneralPurpose1(event: GeneralPurpose1) { }
    open fun onGeneralPurpose1LSB(event: GeneralPurpose1LSB) { }
    open fun onGeneralPurpose2(event: GeneralPurpose2) { }
    open fun onGeneralPurpose2LSB(event: GeneralPurpose2LSB) { }
    open fun onGeneralPurpose3(event: GeneralPurpose3) { }
    open fun onGeneralPurpose3LSB(event: GeneralPurpose3LSB) { }
    open fun onGeneralPurpose4(event: GeneralPurpose4) { }
    open fun onGeneralPurpose4LSB(event: GeneralPurpose4LSB) { }
    open fun onGeneralPurpose5(event: GeneralPurpose5) { }
    open fun onGeneralPurpose6(event: GeneralPurpose6) { }
    open fun onGeneralPurpose7(event: GeneralPurpose7) { }
    open fun onGeneralPurpose8(event: GeneralPurpose8) { }
    open fun onDataIncrement(event: DataIncrement) { }
    open fun onDataDecrement(event: DataDecrement) { }
    open fun onAllControllersOff(event: AllControllersOff) { }
    open fun onAllNotesOff(event: AllNotesOff) { }
    open fun onAllSoundOff(event: AllSoundOff) { }
    open fun onOmniOff(event: OmniOff) { }
    open fun onOmniOn(event: OmniOn) { }
    open fun onPolyphonicOperation(event: PolyphonicOperation) { }
    open fun onProgramChange(event: ProgramChange) { }
    open fun onChannelPressure(event: ChannelPressure) { }
    open fun onPitchWheelChange(event: PitchWheelChange) { }
    open fun onSystemExclusive(event: SystemExclusive) { }
    open fun onMTCQuarterFrame(event: MTCQuarterFrame) { }
    open fun onSongPositionPointer(event: SongPositionPointer) { }
    open fun onSongSelect(event: SongSelect) { }
    open fun onTuneRequest(event: TuneRequest) { }
    open fun onMIDIClock(event: MIDIClock) { }
    open fun onMIDIStart(event: MIDIStart) { }
    open fun onMIDIContinue(event: MIDIContinue) { }
    open fun onMIDIStop(event: MIDIStop) { }
    open fun onActiveSense(event: ActiveSense) { }
    open fun onReset(event: Reset) { }
    open fun onTimeCode(event: TimeCode) { }
}

class MIDIPlayer: VirtualMIDIDevice() {
    var playing = true
    fun play_midi(midi: MIDI) {
        if (! this.is_registered()) {
            Log.w("apres", "Can't play without registering a midi controller first")
            return
        }
        this.playing = true
        val ppqn = midi.get_ppqn()
        var us_per_tick = 60000000 / (ppqn * 120)
        var previous_tick = 0
        val start_time = System.currentTimeMillis()
        var delay_accum = 0
        var that = this
        for ((tick, events) in midi.get_all_events_grouped()) {
            if (!this.playing) {
                break
            }

            if ((tick - previous_tick) > 0) {
                val delay = ((tick - previous_tick) * us_per_tick) / 1000
                val drift = delay_accum - (System.currentTimeMillis() - start_time)
                delay_accum += delay

                if (delay + drift > 0) {
                    Thread.sleep(delay + drift)
                }
                previous_tick = tick
            }

            runBlocking {
                for (event in events) {
                    if (event is SetTempo) {
                        us_per_tick = event.get_uspqn() / ppqn
                    }
                    launch {
                        that.sendEvent(event)
                    }
                }
            }
        }
        // if the song wasn't manually stopped, return to the start
        if (this.playing) {
            this.sendEvent(SongPositionPointer(0))
        }

        for (i in 0 until 16) {
            this.sendEvent(AllSoundOff(i))
        }
    }

    override fun onMIDIStop(event: MIDIStop) {
        this.stop()
    }

    fun stop() {
        this.playing = false
    }
}

