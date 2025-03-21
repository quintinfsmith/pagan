package com.qfs.apres.event2

import com.qfs.apres.event.GeneralMIDIEvent
import kotlin.experimental.or

abstract interface UMPEvent: GeneralMIDIEvent

private infix fun Byte.shl(i: Int): Byte {
    var n = this.toInt()
    for (x in 0 until i) {
        n *= 2
    }
    return n.toByte()
}

abstract interface FlexDataMessage: UMPEvent {
    abstract fun get_group(): Byte
    abstract fun get_form(): Byte
    abstract fun get_addrs(): Byte
    abstract fun get_channel(): Byte
    abstract fun get_status_bank(): Byte
    abstract fun get_status(): Byte

    abstract fun get_data(): ByteArray

    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xD0.toByte() or this.get_group(),
            (this.get_form() shl 6) or (this.get_addrs() shl 4) or this.get_channel(),
            this.get_status_bank(),
            this.get_status()
        ) + this.get_data()
    }
}

abstract class SystemExclusive(
    var group: Int,
    var status: Int,
    var stream: Int
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val data = this.get_data_bytes()
        // TODO: Validate data size and status
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            ((this.status shl 4) and (data.size)).toByte(),
            *data
        )
    }
    abstract fun get_data_bytes(): ByteArray
}

abstract class CapabilitiesInquiry(var muid_source: Int, var muid_destination: Int, var sub_id: Int, var channel: Int = 0x7F): GeneralMIDIEvent {
    private fun _get_source_as_bytes(): ByteArray {
        return ByteArray(4) { i: Int ->
            ((this.muid_source shr (i * 8)) and 0xFF).toByte()
        }
    }
    private fun _get_destination_as_bytes(): ByteArray {
        return ByteArray(4) { i: Int ->
            ((this.muid_destination shr (i * 8)) and 0xFF).toByte()
        }
    }
    abstract fun get_payload_bytes(): ByteArray

    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF0.toByte(),
            0x7E.toByte(),
            this.channel.toByte(),
            0x0D.toByte(), // MIDI CI
            this.sub_id.toByte(),
            0x01.toByte(), // MIDI CI  Version
            *this._get_source_as_bytes(),
            *this._get_destination_as_bytes(),
            *this.get_payload_bytes(),
            0xF7.toByte()
        )
    }

}

class InitiateProtocolNegotiation(
    muid_source: Int,
    muid_destination: Int,
    var authority: Int,
    var preferred_protocol_types: Array<Pair<Int, Int>>
): CapabilitiesInquiry(muid_source, muid_destination, 0x10) {
    /*
        See M2 101 section 6.3 for negotiation process.
        Reply is the same as initiate message
    */

    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.authority.toByte(),
            this.preferred_protocol_types.size.toByte(),
            *this.get_preferred_protocol_bytes(),
        )
    }

    fun get_preferred_protocol_bytes(): ByteArray {
        val output = ByteArray(5 * this.preferred_protocol_types.size) { 0.toByte() }

        for (i in this.preferred_protocol_types.indices) {
            var (version, subversion) = this.preferred_protocol_types[i]
            val offset = i * 5
            output[offset] = version.toByte()
            output[offset + 1] = subversion.toByte()
            // TODO: Figure out what extensions are (m2 101 6.4)
        }
        return output
    }
}

class InitiateProtocolNegotiationResponse(
    muid_source: Int,
    muid_destination: Int,
    var authority: Int,
    var preferred_protocol_types: Array<Pair<Int, Int>>
): CapabilitiesInquiry(muid_source, muid_destination, 0x11) {
    /*
        See M2 101 section 6.3 for negotiation process.
        Reply is the same as initiate message
    */

    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.authority.toByte(),
            this.preferred_protocol_types.size.toByte(),
            *this.get_preferred_protocol_bytes(),
        )
    }

    fun get_preferred_protocol_bytes(): ByteArray {
        val output = ByteArray(5 * this.preferred_protocol_types.size) { 0.toByte() }

        for (i in this.preferred_protocol_types.indices) {
            var (version, subversion) = this.preferred_protocol_types[i]
            val offset = i * 5
            output[offset] = version.toByte()
            output[offset + 1] = subversion.toByte()
            // TODO: Figure out what extensions are (m2 101 6.4)
        }
        return output
    }
}

class SetNewProtocol(source: Int, destination: Int, var authority: Int, var version: Int, var subversion: Int): CapabilitiesInquiry(source, destination, 0x12) {
    // NOTE: WAIT 100ms when setting protocol to wait for receiver to set protocol
    // This is in the spec.

    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.authority.toByte(),
            this.version.toByte(),
            this.subversion.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )
    }
}

class TestNewProtocolInitiatorToResponder(source: Int, destination: Int, var authority: Int): CapabilitiesInquiry(source, destination, 0x13) {
    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.authority.toByte(),
            *(ByteArray(48) { it.toByte() }) // Test Pattern
        )
    }
}

class TestNewProtocolResponderToInitiator(source: Int, destination: Int, var authority: Int): CapabilitiesInquiry(source, destination, 0x14) {
    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.authority.toByte(),
            *(ByteArray(48) { it.toByte() }) // Test Pattern
        )
    }
}

class ConfirmNewProtocolEstablished(source: Int, destination: Int, var authority: Int): CapabilitiesInquiry(source, destination, 0x15) {
    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(this.authority.toByte())
    }
}

class ProfileInquiry(source: Int, destination: Int, channel: Int = 0x7F): CapabilitiesInquiry(source, destination, 0x20, channel) {
    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf()
    }
}

abstract class ProfileID(var bank: Int, var number: Int, var version: Int, var level: Int, var id: Int = 0x7E) {
    fun as_bytes(): ByteArray {
        return byteArrayOf(
            this.id.toByte(),
            this.bank.toByte(),
            this.number.toByte(),
            this.version.toByte(),
            this.level.toByte()
        )
    }
}

class GMProfileID(): ProfileID(0, 0, 1, 1)

class ProfileInquiryResponse(
    source: Int,
    destination: Int,
    channel: Int = 0x7F,
    var enabled: Array<ProfileID>,
    var disabled: Array<ProfileID>
): CapabilitiesInquiry(source, destination, 0x21, channel) {
    override fun get_payload_bytes(): ByteArray {
        val enabled_profiles = ByteArray(this.enabled.size * 5) { 0x00.toByte() }
        for (i in 0 until this.enabled.size) {
            this.enabled[i].as_bytes().forEachIndexed { j: Int, byte: Byte ->
                enabled_profiles[(i * 5) + j] = byte
            }
        }

        val disabled_profiles = ByteArray(this.disabled.size * 5) { 0x00.toByte() }
        for (i in 0 until this.disabled.size) {
            this.disabled[i].as_bytes().forEachIndexed { j: Int, byte: Byte ->
                disabled_profiles[(i * 5) + j] = byte
            }
        }

        return byteArrayOf(
            this.enabled.size.toByte(),
            *enabled_profiles,
            this.disabled.size.toByte(),
            *disabled_profiles
        )
    }
}

class ProfileOn(source: Int, destination: Int, channel: Int = 0x7F, var profile: ProfileID): CapabilitiesInquiry(source, destination, 0x22, channel) {
    override fun get_payload_bytes(): ByteArray {
        return this.profile.as_bytes()
    }
}

class ProfileOff(source: Int, destination: Int, channel: Int = 0x7F, var profile: ProfileID): CapabilitiesInquiry(source, destination, 0x23, channel) {
    override fun get_payload_bytes(): ByteArray {
        return this.profile.as_bytes()
    }
}

class ProfileEnabledReport(source: Int, channel: Int = 0x7F, var profile: ProfileID): CapabilitiesInquiry(source, 0x7F7F7F7F, 0x24, channel) {
    override fun get_payload_bytes(): ByteArray {
        return this.profile.as_bytes()
    }
}
class ProfileDisabledReport(source: Int, channel: Int = 0x7F, var profile: ProfileID): CapabilitiesInquiry(source, 0x7F7F7F7F, 0x25, channel) {
    override fun get_payload_bytes(): ByteArray {
        return this.profile.as_bytes()
    }
}

class ProfileSpecificData(source: Int, destination: Int, channel: Int = 0x7F, var profile: ProfileID, var data: ByteArray): CapabilitiesInquiry(source, destination, 0x2F, channel) {
    override fun get_payload_bytes(): ByteArray {
        val data_len_bytes = ByteArray(4) { i: Int ->
            (this.data.size shr (i * 8) and 0xFF).toByte()
        }

        return byteArrayOf(
            *this.profile.as_bytes(),
            *data_len_bytes,
            *this.data
        )
    }
}


class PropertyExchange(
    source: Int,
    destination: Int,
    var simulataneous_requests_supported: Int,
    var major_version: Int,
    var minor_version: Int
): CapabilitiesInquiry(source, destination, 0x30) {
    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.simulataneous_requests_supported.toByte(),
            this.major_version.toByte(),
            this.minor_version.toByte()
        )
    }
}

class PropertyExchangeResponse(
    source: Int,
    destination: Int,
    var simulataneous_requests_supported: Int,
    var major_version: Int,
    var minor_version: Int
): CapabilitiesInquiry(source, destination, 0x31) {
    override fun get_payload_bytes(): ByteArray {
        return byteArrayOf(
            this.simulataneous_requests_supported.toByte(),
            this.major_version.toByte(),
            this.minor_version.toByte()
        )
    }
}

/*
    TODO: HasPropertyData (m2-101 8.6)
    TODO: HasPropertyDataResponse (m2-101 8.7)
    TODO: GetPropertyData (m2-101 8.8)
    TODO: GetPropertyDataResponse (m2-101 8.9)
    TODO: SetPropertyData (m2-101 8.10)
    TODO: SetPropertyDataResponse (m2-101 8.11)
    TODO: Subscription (m2-101 8.12)
    TODO: SubscriptionResponse (m2-101 8.13)
    TODO: Notify (m2-101 8.14)
*/



// abstract class MidiCI(group: Int, stream: Int): SystemExclusive(group, 0xD, stream) {
// }

class StartOfClip(): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF0.toByte(),
            0x20.toByte(),
            *(ByteArray(14) { 0.toByte() })
        )
    }
}

class EndOfClip(): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xF0.toByte(),
            0x21.toByte(),
            *(ByteArray(14) { 0.toByte() })
        )
    }
}

class PolyPressure(
    var channel: Int,
    var index: Int,
    var pressure: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val pressure_int = (this.pressure * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xA0 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x0.toByte(),

            // Assuming little endian
            ((pressure_int shr 24) and 0xFF).toByte(),
            ((pressure_int shr 16) and 0xFF).toByte(),
            ((pressure_int shr 8) and 0xFF).toByte(),
            (pressure_int and 0xFF).toByte(),
        )
    }
}

class ChannelPressure(
    var channel: Int,
    var pressure: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val pressure_int = (this.pressure * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xD0 or (this.channel and 0x0F)).toByte(),
            0x00.toByte(),
            0x00.toByte(),
            // Assuming little endian
            ((pressure_int shr 24) and 0xFF).toByte(),
            ((pressure_int shr 16) and 0xFF).toByte(),
            ((pressure_int shr 8) and 0xFF).toByte(),
            (pressure_int and 0xFF).toByte(),
        )
    }
}

class ChannelPitchBend(
    var channel: Int,
    var bend: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val bend_int = (((this.bend + 1F) / 2F) * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xE0 or (this.channel and 0x0F)).toByte(),
            0x00.toByte(),
            ((bend_int shr 24) and 0xFF).toByte(),
            ((bend_int shr 16) and 0xFF).toByte(),
            ((bend_int shr 8) and 0xFF).toByte(),
            (bend_int and 0xFF).toByte()
        )
    }
}

class PerNotePitchBend(
    val index: Int = 0,
    var channel: Int,
    var bend: Float,
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        val bend_int = (((this.bend + 1F) / 2F) * 0xFFFFFFFF).toInt()
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0x60 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x00.toByte(),
            ((bend_int shr 24) and 0xFF).toByte(),
            ((bend_int shr 16) and 0xFF).toByte(),
            ((bend_int shr 8) and 0xFF).toByte(),
            (bend_int and 0xFF).toByte()
        )
    }
}

class NoteOn79(
    var index: Int = 0,
    var channel: Int,
    var note: Int,
    var velocity: Int,
    var bend: Int = 0, // 512ths of a semitone
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0x90 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x03.toByte(),
            ((this.velocity and 0xFF00) shr 8).toByte(),
            (this.velocity and 0x00FF).toByte(),
            (((this.note and 0x7F) shl 9) or (this.bend and 0x01FF)).toByte()
        )
    }
}

class NoteOff79(
    var index: Int = 0,
    var channel: Int,
    var note: Int,
    var velocity: Int,
    var bend: Int = 0, // 512ths of a semitone
    var group: Int = 0
): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0x80 or (this.channel and 0x0F)).toByte(),
            (this.index and 0x7F).toByte(),
            0x03.toByte(),
            ((this.velocity and 0xFF00) shr 8).toByte(),
            (this.velocity and 0x00FF).toByte(),
            (((this.note and 0x7F) shl 9) or (this.bend and 0x01FF)).toByte()
        )
    }
}

class ProgramChange(
    var index: Int,
    var channel: Int,
    var group: Int,
    var program: Int,
    var bank: Int,
    var bank_select: Boolean = false,
): UMPEvent {
    override fun as_bytes(): ByteArray {
        var option_flags = 0x00
        if (this.bank_select) {
            option_flags += 1
        }

        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            (0xC0 or (this.channel and 0x0F)).toByte(),
            0x00.toByte(),
            option_flags.toByte(),
            (0x7F and this.program).toByte(),
            0x00.toByte(),
            ((this.bank and 0x3f80) shr 7).toByte(),
            (this.bank and 0x007F).toByte()
        )
    }
}

class ControlChange(
    var index: Int,
    var group: Int,
    var status: Int,
    var channel: Int,
    var value: Int
): UMPEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            (0x40 or (this.group and 0x0F)).toByte(),
            ((this.status shl 4) or (this.channel and 0x0F)).toByte(),
            (this.index and 0xFF).toByte(),
            0x00.toByte(),
            ((this.value shr 24) and 0xFF).toByte(),
            ((this.value shr 16) and 0xFF).toByte(),
            ((this.value shr 8) and 0xFF).toByte(),
            (this.value and 0xFF).toByte()
        )
    }
}

class SetTempoMessage(var bpm: Float): FlexDataMessage {
    override fun get_group(): Byte {
        return 0.toByte()
    }

    override fun get_form(): Byte {
        return 0.toByte()
    }

    override fun get_addrs(): Byte {
        return 1.toByte()
    }

    override fun get_channel(): Byte {
        return 0.toByte()
    }

    override fun get_status_bank(): Byte {
        return 0.toByte()
    }

    override fun get_status(): Byte {
        return 0.toByte()
    }

    override fun get_data(): ByteArray {
        // deca-nano seconds per quarter note
        val tnspqn = (6000000000.toFloat() / this.bpm).toInt()
        return ByteArray(4) { i: Int ->
            ((tnspqn shr i) and 0xFF).toByte()
        }
    }
}

class SetTimeSignatureMessage(var numerator: Int, var denominator: Int, var thirtysecondths_per_quarter: Int): FlexDataMessage {
    override fun get_group(): Byte {
        return 0
    }

    override fun get_form(): Byte {
        return 0
    }

    override fun get_addrs(): Byte {
        return 1
    }

    override fun get_channel(): Byte {
        return 0
    }

    override fun get_status_bank(): Byte {
        return 0
    }

    override fun get_status(): Byte {
        return 1
    }

    override fun get_data(): ByteArray {
        return byteArrayOf(
            this.numerator.toByte(),
            this.denominator.toByte(),
            this.thirtysecondths_per_quarter.toByte(),
            0
        ) + ByteArray(8) { 0 }
    }
}

class SetMetronomeMessage(
    var clocks_per_click: Int,
    var accent_first: Int,
    var accent_second: Int = 0,
    var accent_third: Int = 0,
    var subdivision_clicks_first: Int = 0,
    var subdivision_clicks_second: Int = 0): FlexDataMessage {

    override fun get_group(): Byte {
        return 0
    }

    override fun get_form(): Byte {
        return 0
    }

    override fun get_addrs(): Byte {
        return 1
    }

    override fun get_channel(): Byte {
        return 0
    }

    override fun get_status_bank(): Byte {
        return 0
    }

    override fun get_status(): Byte {
        return 2
    }

    override fun get_data(): ByteArray {
        return byteArrayOf(
            this.clocks_per_click.toByte(),
            this.accent_first.toByte(),
            this.accent_second.toByte(),
            this.accent_third.toByte(),
            this.subdivision_clicks_first.toByte(),
            this.subdivision_clicks_second.toByte()
        ) + ByteArray(6) { 0 }
    }

}


class SetKeySignatureMessage(var tonic: Int, var sharps: Int): FlexDataMessage {
    override fun get_group(): Byte {
        return 0
    }

    override fun get_form(): Byte {
        return 0
    }

    override fun get_addrs(): Byte {
        return 0
    }

    override fun get_channel(): Byte {
        return 0
    }

    override fun get_status_bank(): Byte {
        return 0
    }

    override fun get_status(): Byte {
        return 5
    }

    override fun get_data(): ByteArray {
        return byteArrayOf(
            ((this.sharps shl 4) or (this.tonic)).toByte()
        ) + ByteArray(11)
    }
}