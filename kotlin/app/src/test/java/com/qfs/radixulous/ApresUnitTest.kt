package com.qfs.radixulous

import org.junit.Test
import org.junit.Assert.*
import com.qfs.radixulous.apres.*

fun intlist_to_bytearray(input: List<Int>): ByteArray {
    var output: MutableList<Byte> = mutableListOf()
    for (i in input) {
        output.add(i.toByte())
    }
    return output.toByteArray()
}

class ApresUnitTest {
    @Test
    fun test_initialize_load() {
        var midi_bytes = listOf(
            0x4D, 0x54, 0x68, 0x64, // MThd
            0x00, 0x00, 0x00, 0x06, // Length
            0x00, 0x01, // format = 1
            0x00, 0x01, // track count = 1
            0x00, 0x1b, // bytes in midi (excluding Mthd) = 20 expressed as variable length
            0x4D, 0x54, 0x72, 0x6B, // MTrk
            0x00, 0x00, 0x00, 92, // Length

            // EVENTS (Do not change order)

            // Meta Events
            // TimeSignature
            0x00, 0xFF, 0x58, 0x04, 0x04, 0x04, 0x24, 0x04,
            // Text ("ABC")
            0x00, 0xFF, 0x01, 0x03, 0x41, 0x42, 0x43,
            // CopyRightNotice ("ABC")
            0x00, 0xFF, 0x02, 0x03, 0x41, 0x42, 0x43,
            // TrackName ("ABC")
            0x00, 0xFF, 0x03, 0x03, 0x41, 0x42, 0x43,
            // InstrumentName ("ABC")
            0x00, 0xFF, 0x04, 0x03, 0x41, 0x42, 0x43,
            // Lyric ("ABC")
            0x00, 0xFF, 0x05, 0x03, 0x41, 0x42, 0x43,
            // Marker ("ABC")
            0x00, 0xFF, 0x06, 0x03, 0x41, 0x42, 0x43,
            // CuePoint ("ABC")
            0x00, 0xFF, 0x07, 0x03, 0x41, 0x42, 0x43,

            // Channel Prefix
            0x00, 0xFF, 0x20, 0x01, 0x00,

            // SetTempo (~210)
            0x00, 0xFF, 0x51, 0x03, 0x04, 0x5c, 0x12,

            // Channel Events
            // NoteOn
            0x00, 0x90, 0x40, 0x18,
            // AfterTouch
            0x30, 0xA0, 0x40, 0x50,
            // PitchWheel
            0x10, 0xE0, 0x00, 0x00,
            // ChannelPressure
            0x00, 0xD0, 0x2F,
            // NoteOff
            0x38, 0x80, 0x00, 0x40,

            0x00, 0xFF, 0x2F, 0x00 // EOT
        )

        val midi = MIDI.from_bytes(intlist_to_bytearray(midi_bytes))

        assertEquals(midi.count_tracks(), 1)
        assertEquals(midi.get_track_length(0), 121)
        assertEquals(midi.events.size, 16)
        assertEquals(midi.event_positions.size, 16)
        //assertEquals(midi_bytes, midi.as_bytes().toList())
        assertEquals(midi_bytes.size, midi.as_bytes().toList().size)
    }

    @Test
    fun test_add_event() {
        val midi = MIDI()
        val on_event = midi.push_event(0,0,NoteOn(0, 64, 100))
        var off_event = midi.push_event(0, 119, NoteOff(0,64,0))
        assertEquals(1, on_event)
        assertEquals(2, off_event)
        assertEquals(2, midi.events.size)
        assertEquals(2, midi.event_positions.size)
        assertEquals(1, midi.count_tracks())
        assertEquals(120, midi.get_track_length(0))
    }

    @Test
    fun test_variable_length_conversion() {
        var test_cases = listOf(
            Pair(0, listOf(0.toByte())),
            Pair(127, listOf(0x7F.toByte())),
            Pair(128, listOf(0x81.toByte(), 0x00.toByte())),
            Pair(2097151, listOf(0xFF.toByte(), 0xFF.toByte(), 0x7F.toByte()))
        )

        for ((input_number, expectedlist) in test_cases) {
            var output_bytes = to_variable_length_bytes(input_number)
            assertEquals(expectedlist, output_bytes)

            var output_n = get_variable_length_number(expectedlist.toMutableList())
            assertEquals(input_number, output_n)
        }
    }

    @Test
    fun test_sequence_number_event() {
        var event = SequenceNumber(1)
        assertEquals(listOf(0xFF.toByte(), 0x00.toByte(), 0x02.toByte(), 0x00.toByte(), 0x01.toByte()), event.as_bytes().toList())
        event = SequenceNumber(13607)
        assertEquals(listOf(0xFF.toByte(), 0x00.toByte(), 0x02.toByte(), 0x35.toByte(), 0x27.toByte()), event.as_bytes().toList())

    }

    @Test
    fun test_text_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = Text(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x01.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }
    @Test
    fun test_copyright_notice_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = CopyRightNotice(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x02.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }
    @Test
    fun test_track_name_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = TrackName(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x03.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }
    @Test
    fun test_instrument_name_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = InstrumentName(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x04.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }
    @Test
    fun test_lyric_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = Lyric(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x05.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }
    @Test
    fun test_marker_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = Marker(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x06.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }

    @Test
    fun test_cue_point_event() {
        var some_text = "This is some text"
        var text_len_bytes = to_variable_length_bytes(some_text.length)

        var event = CuePoint(some_text)
        var compare_list = mutableListOf(0xFF.toByte(), 0x07.toByte())
        compare_list += text_len_bytes
        compare_list += some_text.toByteArray().toList()
        assertEquals(compare_list, event.as_bytes().toList())
    }

    @Test
    fun test_end_of_track_event() {
        var event = EndOfTrack()
        assertEquals(listOf(0xFF.toByte(), 0x2F.toByte(), 0x00.toByte()), event.as_bytes().toList())
    }

    @Test
    fun test_channel_prefix_event() {
        for (i in 0 until 256) {
            var event = ChannelPrefix(i)
            assertEquals(listOf(0xFF.toByte(), 0x20.toByte(), 0x01.toByte(), i.toByte()), event.as_bytes().toList())
        }
    }
    @Test
    fun test_set_tempo_event() {
        var test_cases_bpm = listOf(
            Pair(120, 500000),
            Pair(280, 214285),
            Pair(1, 0x00FFFFFF),
            Pair(60000000, 1)
        )
        for ((_, expected_uspqn) in test_cases_bpm) {
            var event = SetTempo(expected_uspqn)
            assertEquals(
                listOf(
                    0xFF.toByte(), 0x51.toByte(), 0x03.toByte(),
                    ((expected_uspqn / (256 * 256)) % 256).toByte(),
                    ((expected_uspqn / 256) % 256).toByte(),
                    (expected_uspqn % 256).toByte()
                ),
                event.as_bytes().toList()
            )
        }

    }
    @Test
    fun test_smpte_offset_event() {
        var event = SMPTEOffset(1,2,3,4,5)
        assertEquals(
            listOf(
                0xFF.toByte(),
                0x54.toByte(),
                0x05.toByte(),
                0x01.toByte(),
                0x02.toByte(),
                0x03.toByte(),
                0x04.toByte(),
                0x05.toByte()
            ),
            event.as_bytes().toList()
        )
    }
    @Test
    fun test_time_signature_event() {
        var event = TimeSignature(4,4,32,3)
        assertEquals(
            listOf(
                0xFF.toByte(),
                0x58.toByte(),
                0x04.toByte(),
                0x04.toByte(),
                0x04.toByte(),
                0x20.toByte(),
                0x03.toByte()
            ),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_key_signature_event() {
        var event = KeySignature("A")
        assertEquals(
            listOf(
                0xFF.toByte(),
                0x59.toByte(),
                0x02.toByte(),
                0x03.toByte(),
                0x00.toByte()
            ),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_squence_specific_event() {
        var event = SystemExclusive(byteArrayOf(0x00.toByte()))
        assertEquals(
            listOf(
                0xF0.toByte(),
                0x00.toByte(),
                0xF7.toByte()
            ),
            event.as_bytes().toList()
        )
    }
    @Test
    fun test_note_on_event() {
        var event = NoteOn(14,23,33)
        assertEquals(
            listOf(
                0x9E.toByte(),
                0x17.toByte(),
                0x21.toByte()
            ),
            event.as_bytes().toList()
        )
    }
    @Test
    fun test_note_off_event() {
        var event = NoteOff(14,23,33)
        assertEquals(
            listOf(
                0x8E.toByte(),
                0x17.toByte(),
                0x21.toByte()
            ),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_aftertouch_event() {
        var event = PolyphonicKeyPressure(14,23,33)
        assertEquals(
            listOf(
                0xAE.toByte(),
                0x17.toByte(),
                0x21.toByte()
            ),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_program_change_event() {
        var event = ProgramChange(14,35)
        assertEquals(
            listOf( 0xCE.toByte(), 0x23.toByte() ),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_channel_pressure_event() {
        var event = ChannelPressure(14, 23)
        assertEquals(
            listOf(0xDE.toByte(), 0x17.toByte()),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_pitchwheel_change_event() {
        var test_cases = listOf(
            Triple(-1.0, 0, 0),
            Triple(-0.5, 0x10,0x00),
            Triple(0.0, 0x20, 0x00),
            Triple(0.5, 0x2F, 0x7F),
            Triple(1.0, 0x3F, 0x7F)
        )

        for ((input, msb, lsb) in test_cases) {
            var event = PitchWheelChange(14, input.toFloat())
            assertEquals(
                input.toFloat(),
                event.value
            )
            assertEquals(
                listOf(0xEE.toByte(), lsb.toByte(), msb.toByte()),
                event.as_bytes().toList()
            )
        }
    }

    @Test
    fun test_system_exclusive_event() {
        var event = SystemExclusive(byteArrayOf(0,0,1,0))
        assertEquals(
            listOf(0xF0.toByte(), 0x00, 0x00, 0x01, 0x00, 0xF7.toByte()),
            event.as_bytes().toList()
        )
    }

    @Test
    fun test_control_change_events() {
        var general_event = ControlChange(0x0E, 0x17, 0x21)
        assertEquals(listOf(0xBE.toByte(), 0x17.toByte(), 0x21.toByte()), general_event.as_bytes().toList())

        var channel = 1
        var value = 25
        var compare_variable_controls = listOf(
            Pair(BankSelect(channel, value), 0x00),
            Pair(BankSelectLSB(channel, value), 0x20),
            Pair(ModulationWheel(channel, value), 0x01),
            Pair(ModulationWheelLSB(channel, value), 0x21),
            Pair(BreathController(channel, value), 0x02),
            Pair(BreathControllerLSB(channel, value), 0x22),
            Pair(FootPedal(channel, value), 0x04),
            Pair(FootPedalLSB(channel, value), 0x24),
            Pair(PortamentoTime(channel, value), 0x05),
            Pair(PortamentoTimeLSB(channel, value), 0x25),
            Pair(DataEntry(channel, value), 0x06),
            Pair(DataEntryLSB(channel, value), 0x26),
            Pair(Volume(channel, value), 0x07),
            Pair(VolumeLSB(channel, value), 0x27),
            Pair(Balance(channel, value), 0x08),
            Pair(BalanceLSB(channel, value), 0x28),
            Pair(Pan(channel, value), 0x0A),
            Pair(PanLSB(channel, value), 0x2A),
            Pair(Expression(channel, value), 0x0B),
            Pair(ExpressionLSB(channel, value), 0x2B),
            Pair(EffectControl1(channel, value), 0x0C),
            Pair(EffectControl1LSB(channel, value), 0x2C),
            Pair(EffectControl2(channel, value), 0x0D),
            Pair(EffectControl2LSB(channel, value), 0x2D),
            Pair(GeneralPurpose1(channel, value), 0x10),
            Pair(GeneralPurpose1LSB(channel, value), 0x30),
            Pair(GeneralPurpose2(channel, value), 0x11),
            Pair(GeneralPurpose2LSB(channel, value), 0x31),
            Pair(GeneralPurpose3(channel, value), 0x12),
            Pair(GeneralPurpose3LSB(channel, value), 0x32),
            Pair(GeneralPurpose4(channel, value), 0x13),
            Pair(GeneralPurpose4LSB(channel, value), 0x33),
            Pair(HoldPedal(channel, value), 0x40),
            Pair(Portamento(channel, value), 0x41),
            Pair(Sustenuto(channel, value), 0x42),
            Pair(SoftPedal(channel, value), 0x43),
            Pair(Legato(channel, value), 0x44),
            Pair(Hold2Pedal(channel, value), 0x45),
            Pair(SoundVariation(channel, value), 0x46),
            Pair(SoundTimbre(channel, value), 0x47),
            Pair(SoundReleaseTime(channel, value), 0x48),
            Pair(SoundAttack(channel, value), 0x49),
            Pair(SoundBrightness(channel, value), 0x4A),
            Pair(SoundControl1(channel, value), 0x4B),
            Pair(SoundControl2(channel, value), 0x4C),
            Pair(SoundControl3(channel, value), 0x4D),
            Pair(SoundControl4(channel, value), 0x4E),
            Pair(SoundControl5(channel, value), 0x4F),
            Pair(GeneralPurpose5(channel, value), 0x50),
            Pair(GeneralPurpose6(channel, value), 0x51),
            Pair(GeneralPurpose7(channel, value), 0x52),
            Pair(GeneralPurpose8(channel, value), 0x53),
            Pair(EffectsLevel(channel, value), 0x5B),
            Pair(TremuloLevel(channel, value), 0x5C),
            Pair(ChorusLevel(channel, value), 0x5D),
            Pair(CelesteLevel(channel, value), 0x5E),
            Pair(PhaserLevel(channel, value), 0x5F),
            Pair(RegisteredParameterNumber(channel, value), 0x65),
            Pair(RegisteredParameterNumberLSB(channel, value), 0x64),
            Pair(NonRegisteredParameterNumber(channel, value), 0x63),
            Pair(NonRegisteredParameterNumberLSB(channel, value), 0x62),
            Pair(LocalControl(channel, value), 0x7A),
            Pair(MonophonicOperation(channel, value), 0xFE)
        )

        var compare_constant_controls = listOf(
            Pair(DataIncrement(channel), 0x60),
            Pair(DataDecrement(channel), 0x61),
            Pair(PolyphonicOperation(channel), 0xFF),
            Pair(AllSoundOff(channel), 0x78),
            Pair(AllControllersOff(channel), 0x79),
            Pair(AllNotesOff(channel), 0x7B),
            Pair(OmniOff(channel), 0x7C ),
            Pair(OmniOn(channel), 0x7D)
        )


        for ((event, controller_value) in compare_variable_controls) {
            assertEquals(
                listOf((0xB0 or channel).toByte(), controller_value.toByte(), value.toByte()),
                event.as_bytes().toList()
            )
        }

        for ((event, controller_value) in compare_constant_controls) {
            assertEquals(
                listOf((0xB0 or channel).toByte(), controller_value.toByte(), 0.toByte()),
                event.as_bytes().toList()
            )
        }
    }
    @Test
    fun test_chords() {
        assertEquals(
            "Eb",
            get_chord_name_from_mi_sf(0.toByte(), 253.toByte())
        )
        assertEquals(
            "A#m",
            get_chord_name_from_mi_sf(1.toByte(), 7.toByte())
        )
    }
}
