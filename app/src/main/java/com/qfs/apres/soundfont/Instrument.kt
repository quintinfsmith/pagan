package com.qfs.apres.soundfont

class Instrument(var name: String) {
    var samples: MutableList<InstrumentSample> = mutableListOf()
    var global_sample: InstrumentSample? = null
    fun add_sample(isample: InstrumentSample) {
        if (global_sample == null) {
            global_sample = isample
        } else {
            this.samples.add(isample)
        }
    }

    fun get_samples(key: Int, velocity: Int): Set<InstrumentSample> {
        val output = mutableSetOf<InstrumentSample>()
        this.samples.forEachIndexed { _, sample ->
            if (
                (sample.key_range == null || (sample.key_range!!.first <= key && sample.key_range!!.second >= key)) &&
                (sample.velocity_range == null || (sample.velocity_range!!.first <= velocity && sample.velocity_range!!.second >= velocity))
            ) {
                output.add(sample)
                if (sample.sample!!.sampleType == 1) {
                    return output
                }
            }
        }
        return output
    }
}