package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

data class TimeSignature(var numerator: Int, var denominator: Int, var clocks_per_metronome: Int, var thirtysecondths_per_quarter: Int):
    MIDIEvent {
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