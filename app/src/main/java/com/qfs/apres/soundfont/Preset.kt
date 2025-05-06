package com.qfs.apres.soundfont

class Preset(
    var name: String = "",
    var preset: Int = 0, // MIDI Preset Number
    var bank: Int = 0, // MIDI Bank Number
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
) {

    companion object {
        var next_uid: Int = 0
        fun gen_uid(): Int {
            return next_uid++
        }
    }
    var instruments = HashMap<Int, InstrumentDirective>()
    var global_zone = InstrumentDirective()
    private val quick_instrument_ref_vel = Array<MutableSet<Int>>(128) { mutableSetOf() }
    private val quick_instrument_ref_key = Array<MutableSet<Int>>(128) { mutableSetOf() }

    val uid = Preset.gen_uid()

    fun set_global_zone(new_global_zone: InstrumentDirective) {
        this.global_zone = new_global_zone
    }

    fun add_instrument(pinstrument: InstrumentDirective) {
        val uuid = pinstrument.uid
        val key_range = if (pinstrument.key_range == null) {
            0..127
        } else {
            pinstrument.key_range!!.first ..pinstrument.key_range!!.second
        }
        for (i in key_range) {
            this.quick_instrument_ref_key[i].add(uuid)
        }
        val vel_range = if (pinstrument.velocity_range == null) {
            0..127
        } else {
            pinstrument.velocity_range!!.first ..pinstrument.velocity_range!!.second
        }

        for (i in vel_range) {
            this.quick_instrument_ref_vel[i].add(uuid)
        }

        this.instruments[uuid] = pinstrument
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
