package com.qfs.apres.soundfont2

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

    override fun apply_generator(generator: Generator) {
        return // 0x29 is handled in SoundFont
    }
}
