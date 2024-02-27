package com.qfs.apres.soundfont

class Preset(
    var name: String = "",
    var preset: Int = 0, // MIDI Preset Number
    var bank: Int = 0, // MIDI Bank Number
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
) {
    var instruments = HashMap<Int, InstrumentDirective>()
    var global_zone: InstrumentDirective? = null
    private val quick_instrument_ref_vel = Array<MutableSet<Int>>(128) { mutableSetOf() }
    private val quick_instrument_ref_key = Array<MutableSet<Int>>(128) { mutableSetOf() }


    fun add_instrument(pinstrument: InstrumentDirective) {
        if (pinstrument.instrument == null && this.global_zone == null) {
            this.global_zone = pinstrument
        } else {
            val hash_code = pinstrument.hashCode()
            this.instruments[hash_code] = pinstrument
            val key_range = if (pinstrument.key_range == null) {
                0..127
            } else {
                pinstrument.key_range!!.first ..pinstrument.key_range!!.second
            }
            for (i in key_range) {
                this.quick_instrument_ref_key[i].add(hash_code)
            }
            val vel_range = if (pinstrument.velocity_range == null) {
                0..127
            } else {
                pinstrument.velocity_range!!.first ..pinstrument.velocity_range!!.second
            }

            for (i in vel_range) {
                this.quick_instrument_ref_vel[i].add(hash_code)
            }
        }
    }

    fun get_instruments(key: Int, velocity: Int): Set<InstrumentDirective> {
        val ids = this.quick_instrument_ref_vel[velocity].intersect(this.quick_instrument_ref_key[key])
        val output = mutableSetOf<InstrumentDirective>()
        for (id in ids) {
            output.add(this.instruments[id]!!)
        }
        return output
    }
}