package com.qfs.apres.soundfont

class Instrument(var name: String) {
    var samples = HashMap<Int, InstrumentSample>()
    var global_sample: InstrumentSample? = null

    private val quick_ref_vel = Array<MutableSet<Int>>(128) { mutableSetOf() }
    private val quick_ref_key = Array<MutableSet<Int>>(128) { mutableSetOf() }

    fun add_sample(isample: InstrumentSample) {
        if (global_sample == null) {
            global_sample = isample
        } else {
            val hash_code = isample.hashCode()
            this.samples[hash_code] = isample

            val key_range = if (isample.key_range == null) {
                0..127
            } else {
                isample.key_range!!.first ..isample.key_range!!.second
            }
            for (i in key_range) {
                this.quick_ref_key[i].add(hash_code)
            }
            val vel_range = if (isample.velocity_range == null) {
                0..127
            } else {
                isample.velocity_range!!.first ..isample.velocity_range!!.second
            }

            for (i in vel_range) {
                this.quick_ref_vel[i].add(hash_code)
            }
        }
    }

    fun get_samples(key: Int, velocity: Int): Set<InstrumentSample> {
        val ids = this.quick_ref_vel[velocity].intersect(this.quick_ref_key[key])
        val output = mutableSetOf<InstrumentSample>()
        for (id in ids) {
            output.add(this.samples[id]!!)
        }
        return output
    }
}