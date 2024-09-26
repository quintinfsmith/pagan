package com.qfs.apres.soundfont

class InstrumentDirective: Generated() {
    var instrument: Instrument? = null
    val modulators = HashMap<Generator.Operation, MutableSet<Modulator>>()
    fun add_modulator(modulator: Modulator) {
        val key = modulator.destination
        if (!this.modulators.contains(key)) {
            this.modulators[key] = mutableSetOf()
        }
        this.modulators[key]!!.add(modulator)
    }
}
