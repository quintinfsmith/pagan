package com.qfs.apres.soundfont

class Preset(
    var name: String = "",
    var preset: Int = 0, // MIDI Preset Number
    var bank: Int = 0, // MIDI Bank Number
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
) {
    var instruments: MutableList<PresetInstrument> = mutableListOf()
    var global_zone: PresetInstrument? = null

    fun add_instrument(pinstrument: PresetInstrument) {
        if (pinstrument.instrument == null && global_zone == null) {
            this.global_zone = pinstrument
        } else {
            this.instruments.add(pinstrument)
        }
    }

    fun get_instruments(key: Int, velocity: Int): Set<PresetInstrument> {
        val output = mutableSetOf<PresetInstrument>()
        this.instruments.forEachIndexed { _, instrument ->
            if ( (instrument.key_range == null || (instrument.key_range!!.first <= key && instrument.key_range!!.second >= key)) &&
                (instrument.velocity_range == null || (instrument.velocity_range!!.first <= velocity && instrument.velocity_range!!.second >= velocity))
            ) {
                output.add(instrument)
            }
        }
        return output
    }
}