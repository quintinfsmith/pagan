/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres.soundfont2

class SampleDirective: Generated() {
    var sample: List<Sample>? = null
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

    override fun apply_generator(generator: Generator) {
        when (generator.sfGenOper) {
            0x35 -> {
                //Sample needs to be set in the Soundfont
            }

            0x00 -> {
                this.sampleStartOffset = if (this.sampleStartOffset == null) {
                    generator.asIntSigned()
                } else {
                    this.sampleStartOffset!! + generator.asIntSigned()
                }
            }

            0x01 -> {
                this.sampleEndOffset = generator.asIntSigned()
            }

            0x02 -> {
                this.loopStartOffset = generator.asIntSigned()
            }

            0x03 -> {
                this.loopEndOffset = generator.asIntSigned()
            }

            0x04 -> {
                this.sampleStartOffset = if (this.sampleStartOffset == null) {
                    generator.asIntSigned() * 32768
                } else {
                    this.sampleStartOffset!! + (generator.asIntSigned() * 32768)
                }
            }

            0x0C -> {
                this.sampleEndOffset = if (this.sampleEndOffset == null) {
                    generator.asIntSigned() * 32768
                } else {
                    this.sampleEndOffset!! + (generator.asIntSigned() * 32768)
                }
            }

            0x2D -> {
                this.loopStartOffset = if (this.loopStartOffset == null) {
                    generator.asIntSigned() * 32768
                } else {
                    this.loopStartOffset!! + (generator.asIntSigned() * 32768)
                }
            }

            0x2E -> { // Instrument Specific  (keynum)
                this.keynum = generator.asInt()
            }

            0x2F -> { //Instrument Specific (velocity)
                this.velocity = generator.asInt()
            }

            0x32 -> {
                this.loopEndOffset = if (this.loopEndOffset == null) {
                    generator.asIntSigned() * 32768
                } else {
                    this.loopEndOffset!! + (generator.asIntSigned() * 32768)
                }
            }

            0x36 -> {
                this.sampleMode = generator.asInt()
            }

            0x39 -> {
                this.exclusive_class = generator.asInt()
            }

            0x3A -> {
                this.root_key = generator.asInt()
            }

            else -> {
                // Unhandled
            }
        }
    }
}
