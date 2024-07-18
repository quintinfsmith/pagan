package com.qfs.apres.soundfont

class Instrument(var name: String) {
    var samples = HashMap<Int, SampleDirective>()
    var global_zone: SampleDirective = SampleDirective()
    val modulators: MutableList<Modulator> = mutableListOf()

    private val quick_ref_vel = Array<MutableSet<Int>>(128) { mutableSetOf() }
    private val quick_ref_key = Array<MutableSet<Int>>(128) { mutableSetOf() }
    fun set_global_zone(new_global_zone: SampleDirective) {
        this.global_zone = new_global_zone
    }

    fun add_sample(sample_directive: SampleDirective) {
        val hash_code = sample_directive.hashCode()
        this.samples[hash_code] = sample_directive

        val key_range = if (sample_directive.key_range == null) {
            0..127
        } else {
            sample_directive.key_range!!.first..sample_directive.key_range!!.second
        }

        for (i in key_range) {
            this.quick_ref_key[i].add(hash_code)
        }

        val vel_range = if (sample_directive.velocity_range == null) {
            0..127
        } else {
            sample_directive.velocity_range!!.first..sample_directive.velocity_range!!.second
        }

        for (i in vel_range) {
            this.quick_ref_vel[i].add(hash_code)
        }
    }

    fun get_samples(key: Int, velocity: Int): Set<SampleDirective> {
        val output = mutableSetOf<SampleDirective>()
        if (this.samples.isNotEmpty()) {
            val ids = this.quick_ref_vel[velocity].intersect(this.quick_ref_key[key])
            for (id in ids) {
                output.add(this.samples[id]!!)
            }
        } else {
            val key_range = if (this.global_zone.key_range == null) {
                0..127
            } else {
                this.global_zone.key_range!!.first..this.global_zone.key_range!!.second
            }

            val vel_range = if (this.global_zone.velocity_range == null) {
                0..127
            } else {
                this.global_zone.velocity_range!!.first..this.global_zone.velocity_range!!.second
            }

            if (key_range.contains(key) && vel_range.contains(velocity)) {
                output.add(this.global_zone)
            }
        }
        return output
    }
}
