package com.qfs.apres.soundfont

class SampleDirective: Generated() {
    var sample: Sample? = null
    var sampleStartOffset: Int? = null
    var sampleEndOffset: Int? = null
    var loopStartOffset: Int? = null
    var loopEndOffset: Int? = null
    var sampleMode: Int? = null
    var root_key: Int? = null
    var exclusive_class: Int? = null
    var keynum: Int? = null
    var velocity: Int? = null
    val modulators = HashMap<Generator.Operation, MutableSet<Modulator>>()
    fun add_modulator(modulator: Modulator) {
        val key = modulator.destination
        if (!this.modulators.contains(key)) {
            this.modulators[key] = mutableSetOf()
        }
        this.modulators[key]!!.add(modulator)
    }
}
