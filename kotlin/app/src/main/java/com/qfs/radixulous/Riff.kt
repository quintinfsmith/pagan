package com.qfs.radixulous

import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.pow

class SoundFont(var riff: Riff) {
    // Mandatory INFO
    private var ifil: Pair<Int, Int> = Pair(0,0)
    private var isng: String = "EMU8000"
    private var inam: String = ""

    //Optional INFO
    private var irom: String? = null
    private var iver: Pair<Int, Int>? = null
    private var icrd: String? = null // Date
    private var ieng: String? = null
    private var iprd: String? = null
    private var icop: String? = null
    private var icmt: String? = null
    private var isft: String? = null

    // Populated by sdta
    // NOTE: smpl size needs to be 2 * sm24 size
    private var instruments: MutableList<Instrument> = mutableListOf()
    private var presets: MutableList<Preset> = mutableListOf()
    private var samples: MutableList<Sample> = mutableListOf()

    init {
        // Make a hashmap for easier access
        val pdta_map = HashMap<String, ByteArray>()
        for (index in this.riff.sub_chunks[2]) {
            var sub_chunk_type = this.riff.get_sub_chunk_type(2, index)
            var sub_chunk = this.riff.get_sub_chunk_data(2, index)
            pdta_map[sub_chunk_type] = sub_chunk
        }

        val ibag_bytes = pdta_map["ibag"]!!
        val inst_bytes = pdta_map["inst"]!!
        val shdr_bytes = pdta_map["shdr"]!!
        val imod_bytes = pdta_map["imod"]!!
        val pbag_bytes = pdta_map["pbag"]!!
        val phdr_bytes = pdta_map["phdr"]!!
        val igen_bytes = pdta_map["igen"]!!
        val pmod_bytes = pdta_map["pmod"]!!
        val pgen_bytes = pdta_map["pgen"]!!

        for (i in 0 until shdr_bytes.size / 46) {
            val offset = i * 46
            var sample_name = ""
            for (j in 0 until 20) {
                val b = shdr_bytes[offset + j].toInt()
                if (b == 0) {
                    break
                }
                sample_name = "$sample_name${b.toChar()}"
            }

            this.samples.add(
                Sample(
                    sample_name,
                    shdr_bytes[offset + 20].toInt()
                        + (shdr_bytes[offset + 21].toInt() * 256)
                        + (shdr_bytes[offset + 22].toInt() * (256 * 256))
                        + (shdr_bytes[offset + 23].toInt() * (256 * 256 * 256)),
                    shdr_bytes[offset + 24].toInt()
                        + (shdr_bytes[offset + 25].toInt() * 256)
                        + (shdr_bytes[offset + 26].toInt() * (256 * 256))
                        + (shdr_bytes[offset + 27].toInt() * (256 * 256 * 256)),
                    shdr_bytes[offset + 28].toInt()
                        + (shdr_bytes[offset + 29].toInt() * 256)
                        + (shdr_bytes[offset + 30].toInt() * (256 * 256))
                        + (shdr_bytes[offset + 31].toInt() * (256 * 256 * 256)),
                    shdr_bytes[offset + 32].toInt()
                        + (shdr_bytes[offset + 33].toInt() * 256)
                        + (shdr_bytes[offset + 34].toInt() * (256 * 256))
                        + (shdr_bytes[offset + 35].toInt() * (256 * 256 * 256)),
                    shdr_bytes[offset + 36].toInt()
                        + (shdr_bytes[offset + 37].toInt() * 256)
                        + (shdr_bytes[offset + 38].toInt() * (256 * 256))
                        + (shdr_bytes[offset + 39].toInt() * (256 * 256 * 256)),
                    shdr_bytes[offset + 40].toInt(),
                    shdr_bytes[offset + 41].toInt(),
                    shdr_bytes[offset + 42].toInt() + (shdr_bytes[offset + 43].toInt() * 256),
                    shdr_bytes[offset + 44].toInt() + (shdr_bytes[offset + 45].toInt() * 256)
                )
            )
        }

        // TODO: Verify INST chunk (mod 22 == 0, min size 44, and exists)
        val instrument_count = inst_bytes.size / 22
        val ibag_entry_size = 4

        val instrument_list_indices: MutableList<Pair<Int, Int>> = mutableListOf()
        for (i in 0 until instrument_count) {
            val offset = i * ibag_entry_size
            instrument_list_indices.add(
                Pair(
                    ibag_bytes[offset + 0] + (ibag_bytes[offset + 1] * 256),
                    ibag_bytes[offset + 2] + (ibag_bytes[offset + 3] * 256)
                )
            )
        }

        val igenerators: MutableList<Generator> = mutableListOf()
        for (i in 0 until igen_bytes.size / 4) {
            val offset = i * 4
            igenerators.add(
                Generator(
                    igen_bytes[offset + 0].toInt() + (igen_bytes[offset + 1].toInt() * 256),
                    igen_bytes[offset + 2].toInt(),
                    igen_bytes[offset + 3].toInt()
                )
            )
        }

        val imodulators: MutableList<Modulator> = mutableListOf()
        for (i in 0 until imod_bytes.size / 10) {
            val offset = i * 10
            imodulators.add(
                Modulator(
                    imod_bytes[offset + 0].toInt() + (imod_bytes[offset + 1].toInt() * 256),
                    imod_bytes[offset + 2].toInt() + (imod_bytes[offset + 3].toInt() * 256),
                    imod_bytes[offset + 4].toInt() + (imod_bytes[offset + 5].toInt() * 256),
                    imod_bytes[offset + 6].toInt() + (imod_bytes[offset + 7].toInt() * 256),
                    imod_bytes[offset + 8].toInt() + (imod_bytes[offset + 9].toInt() * 256)
                )
            )
        }

        // Instruments
        for (i in 0 until instrument_count - 1) {
            var instrument_name = ""
            for (j in 0 until 20) {
                val b = inst_bytes[(i * 22) + j].toInt()
                if (b == 0) {
                    break
                }
                instrument_name = "$instrument_name${b.toChar()}"
            }

            val instrument = Instrument(instrument_name)
            // Generators
            val generators_to_use: MutableList<Generator> = mutableListOf()

            for (j in instrument_list_indices[i].first until instrument_list_indices[i + 1].first) {
                generators_to_use.add(igenerators[j])
            }
            this.generate_instrument(instrument, generators_to_use)
            this.instruments.add(instrument)

            // TODO: Modulators
            // Modulators
            // for (j in instrument_list_indices[i].second until instrument_list_indices[i + 1].second) {
            //     this.modulate(instrument, imodulators[j])
            // }
        }


        val pgenerators: MutableList<Generator> = mutableListOf()
        for (i in 0 until pgen_bytes.size / 4) {
            val offset = i * 4
            pgenerators.add(
                Generator(
                    pgen_bytes[offset + 0].toInt() + (pgen_bytes[offset + 1].toInt() * 256),
                    pgen_bytes[offset + 2].toInt(),
                    pgen_bytes[offset + 3].toInt()
                )
            )
        }

        val pmodulators: MutableList<Modulator> = mutableListOf()
        for (i in 0 until pmod_bytes.size / 10) {
            val offset = i * 10
            pmodulators.add(
                Modulator(
                    pmod_bytes[offset + 0].toInt() + (pmod_bytes[offset + 1].toInt() * 256),
                    pmod_bytes[offset + 2].toInt() + (pmod_bytes[offset + 3].toInt() * 256),
                    pmod_bytes[offset + 4].toInt() + (pmod_bytes[offset + 5].toInt() * 256),
                    pmod_bytes[offset + 6].toInt() + (pmod_bytes[offset + 7].toInt() * 256),
                    pmod_bytes[offset + 8].toInt() + (pmod_bytes[offset + 9].toInt() * 256)
                )
            )
        }


        val preset_count = phdr_bytes.size / 38
        val pbag_entry_size = 4

        val preset_list_indices: MutableList<Pair<Int, Int>> = mutableListOf()
        for (i in 0 until preset_count) {
            val wPresetBagIndex = phdr_bytes[(i * 38) + 24] + (phdr_bytes[(i * 38) + 25] * 256)
            val offset = wPresetBagIndex * pbag_entry_size
            preset_list_indices.add(
                Pair(
                    pbag_bytes[offset + 0].toInt() + (pbag_bytes[offset + 1].toInt() * 256),
                    pbag_bytes[offset + 2].toInt() + (pbag_bytes[offset + 3].toInt() * 256)
                )
            )
        }

        for (i in 0 until preset_count - 1) {
            val wGenNdx = preset_list_indices[i].first
            val wGenNdx_next = preset_list_indices[i + 1].first

            var wModNdx = preset_list_indices[i].second
            var wModNdx_next = preset_list_indices[i + 1].second

            val preset_generators: MutableList<Generator> = mutableListOf()
            for (j in wGenNdx until wGenNdx_next) {
                val generator = pgenerators[j]
                preset_generators.add(generator)
                if (generator.sfGenOper == 41) {
                    break
                }
            }

            var name = ""
            for (j in 0 until 20) {
                val b = phdr_bytes[j + (i * 38)].toInt()
                if (b != 0) {
                    name = "$name${b.toChar()}"
                } else {
                    break
                }
            }

            val preset = Preset(
                name,
                phdr_bytes[(i * 38) + 20].toInt() + (phdr_bytes[(i * 38) + 21].toInt() * 256),
                phdr_bytes[(i * 38) + 22].toInt() + (phdr_bytes[(i * 38) + 22].toInt() * 256)
            )

            this.generate_preset(preset, preset_generators)
            this.presets.add(preset)
        }
        // Populate Samples
    }

    // TODO
    //private fun modulate(modulatable: ModulatedGenerated, modulator: Modulator) { }

    private fun generate(working_generated: Generated, generator: Generator) {
        when (generator.sfGenOper) {
            0x05 -> {
                working_generated.mod_lfo_pitch = generator.asInt()
            }
            0x06 -> {
                working_generated.vib_lfo_pitch = generator.asInt()
            }
            0x07 -> {
                working_generated.mod_env_pitch = generator.asInt()
            }
            0x08 -> {
                working_generated.filter_cutoff = generator.asInt()
            }
            0x09 -> {
                working_generated.filter_resonance = generator.asInt().toDouble()
            }
            0x0A -> {
                working_generated.mod_lfo_filter = generator.asInt()
            }
            0x0B -> {
                working_generated.mod_env_filter = generator.asInt()
            }
            0x0D -> {
                working_generated.mod_lfo_volume = generator.asInt()
            }
            0x0E -> { } // Unused
            0x0F -> {
                working_generated.chorus = generator.asInt().toDouble() / 10.0
            }
            0x10 -> {
                working_generated.reverb = (generator.asInt().toDouble()) / 10.0
            }
            0x11 -> {
                working_generated.pan = (generator.asIntSigned().toDouble()) / 10
            }
            0x12 -> {}
            0x13 -> {}
            0x14 -> {}
            0x15 -> {
                working_generated.mod_lfo_delay = generator.asTimecent()
            }
            0x16 -> {
                working_generated.mod_lfo_freq = generator.asTimecent() * 8.176
            }
            0x17 -> {
                working_generated.vib_lfo_delay = generator.asTimecent()
            }
            0x18 -> {
                working_generated.vib_lfo_freq = generator.asTimecent() * 8.176
            }
            0x19 -> {
                working_generated.mod_env_delay = generator.asTimecent()
            }
            0x1A -> {
                working_generated.mod_env_attack = generator.asTimecent()
            }
            0x1B -> {
                working_generated.mod_env_hold = generator.asTimecent()
            }
            0x1C -> {
                working_generated.mod_env_decay = generator.asTimecent()
            }
            0x1D -> {
                working_generated.mod_env_sustain = max(1000, generator.asInt()).toDouble() / 10.0
            }
            0x1E -> {
                working_generated.mod_env_release = generator.asTimecent()
            }
            0x1F -> {
                working_generated.key_mod_env_hold = generator.asInt()
            }
            0x20 -> {
                working_generated.key_mod_env_decay = generator.asInt()
            }
            0x21 -> {
                working_generated.vol_env_delay = generator.asTimecent()
            }
            0x22 -> {
                working_generated.vol_env_attack = generator.asTimecent()
            }
            0x23 -> {
                working_generated.vol_env_hold = generator.asTimecent()
            }
            0x24 -> {
                working_generated.vol_env_decay = generator.asTimecent()
            }
            0x25 -> {
                working_generated.vol_env_sustain = max(1000, generator.asInt()).toDouble() / 10.0
            }
            0x26 -> {
                working_generated.vol_env_release = generator.asTimecent()
            }
            0x27 -> {
                working_generated.key_vol_env_hold = generator.asInt()
            }
            0x28 -> {
                working_generated.key_vol_env_decay = generator.asInt()
            }
            0x2A -> { } // Reserved
            0x2B -> {
                working_generated.key_range = generator.asPair()
            }
            0x2C -> {
                working_generated.velocity_range = generator.asPair()
            }
            0x30 -> {
                working_generated.attenuation = generator.asInt().toDouble() / 10
            }
            0x31 -> {} //reserved 2
            0x33 -> {
                working_generated.tuning_semi = generator.asInt()
            }
            0x34 -> {
                working_generated.tuning_cent = generator.asInt()
            }
            0x37 -> {} // Reserved 3
            0x38 -> {
                working_generated.scale_tuning = generator.asInt()
            }
            0x3B -> {} // Unused
            0x3C -> {} // Unused / EOS
        }
    }

    private fun generate_instrument(instrument: Instrument, generators: List<Generator>) {
        var working_sample = InstrumentSample()
        for (generator in generators) {
            when (generator.sfGenOper) {
                0x35 -> {
                    working_sample.sampleIndex = generator.asInt()
                    instrument.add_sample(working_sample)
                    working_sample = InstrumentSample()
                }
                0x00 -> {
                    working_sample.sampleStartOffset = if (working_sample.sampleStartOffset == null) {
                        generator.asInt()
                    } else {
                        working_sample.sampleStartOffset!! + generator.asInt()
                    }
                }
                0x01 -> {
                    working_sample.sampleEndOffset = generator.asInt()
                }
                0x02 -> {
                    working_sample.loopStartOffset = generator.asInt()
                }
                0x03 -> {
                    working_sample.loopEndOffset = generator.asInt()
                }
                0x04 -> {
                    working_sample.sampleStartOffset = if (working_sample.sampleStartOffset == null) {
                        generator.asInt() * 32768
                    } else {
                        working_sample.sampleStartOffset!! + (generator.asInt() * 32768)
                    }
                }
                0x0C -> {
                    working_sample.sampleEndOffset = if (working_sample.sampleEndOffset == null) {
                        generator.asInt() * 32768
                    } else {
                        working_sample.sampleEndOffset!! + (generator.asInt() * 32768)
                    }
                }
                0x2A -> { } // Reserved
                0x2B -> { } // Key Range
                0x2C -> { } // VelRange
                0x2D -> {
                    working_sample.loopStartOffset = if (working_sample.loopStartOffset == null) {
                        generator.asInt() * 32768
                    } else {
                        working_sample.loopStartOffset!! + (generator.asInt() * 32768)
                    }
                }
                0x2E -> { // Instrument Specific  (keynum)
                    working_sample.keynum = generator.asInt()
                }
                0x2F -> { //Instrument Specific (velocity)
                    working_sample.velocity = generator.asInt()
                }
                0x32 -> {
                    working_sample.loopEndOffset = if (working_sample.loopEndOffset == null) {
                        generator.asInt() * 32768
                    } else {
                        working_sample.loopEndOffset!! + (generator.asInt() * 32768)
                    }
                }
                0x36 -> {
                    working_sample.sampleMode = generator.asInt()
                }
                0x39 -> {
                    working_sample.exclusive_class = generator.asInt()
                }
                0x3A -> {
                    working_sample.root_key = generator.asInt()
                }
                else -> {
                    this.generate(working_sample, generator)
                }
            }

        }
    }
    private fun generate_preset(preset: Preset, generators: List<Generator>) {
        var working_instrument = PresetInstrument()
        for (generator in generators) {
            when (generator.sfGenOper) {
                0x29 -> {
                    working_instrument.instrumentIndex = generator.asInt()
                    preset.add_instrument(working_instrument)
                    working_instrument = PresetInstrument()
                }
                else -> {
                    this.generate(working_instrument, generator)
                }
            }
        }
    }

    fun get_info_subchunk(tag: String): ByteArray? {
        for (i in this.riff.sub_chunks[0]) {
            if (this.riff.get_sub_chunk_type(0, i) != tag) {
                continue
            }

            return this.riff.get_sub_chunk_data(0, i)
        }
        return null
    }

    fun get_ifil(): Pair<Int, Int>? {
        var bytes = this.get_info_subchunk("ifil") ?: return null
        return Pair(
            bytes[0].toInt() + (bytes[1].toInt() * 256),
            bytes[2].toInt() + (bytes[3].toInt() * 256)
        )
    }

    fun get_isng(): String? {
        var bytes = this.get_info_subchunk("isng") ?: return null
        return bytes.toString()
    }

    fun get_inam(): String? {
        var bytes = this.get_info_subchunk("INAM") ?: return null
        return bytes.toString()
    }

    fun get_irom(): String? {
        var bytes = this.get_info_subchunk("irom") ?: return null
        return bytes.toString()
    }

    fun get_iver(): Pair<Int, Int>? {
        var bytes = this.get_info_subchunk("iver") ?: return null
        return Pair(
            bytes[0].toInt() + (bytes[1].toInt() * 256),
            bytes[2].toInt() + (bytes[3].toInt() * 256)
        )
    }

    fun get_icrd(): String? {
        var bytes = this.get_info_subchunk("ICRD") ?: return null
        return bytes.toString()
    }

    fun get_ieng(): String? {
        var bytes = this.get_info_subchunk("IENG") ?: return null
        return bytes.toString()
    }

    fun get_iprd(): String? {
        var bytes = this.get_info_subchunk("IPRD") ?: return null
        return bytes.toString()
    }

    fun get_icop(): String? {
        var bytes = this.get_info_subchunk("ICOP") ?: return null
        return bytes.toString()
    }

    fun get_icmt(): String? {
        var bytes = this.get_info_subchunk("ICMT") ?: return null
        return bytes.toString()
    }

    fun get_isft(): String? {
        var bytes = this.get_info_subchunk("ISFT") ?: return null
        return bytes.toString()
    }

    fun get_sample_data(start_index: Int, end_index: Int): ByteArray? {
        var smpl = this.riff.get_sub_chunk_data(1, 0, start_index * 2, 2 * (end_index - start_index))
        var wordsize = 2
        var sm24 = if (this.riff.sub_chunks[1].size == 2) {
            wordsize = 3
            this.riff.get_sub_chunk_data(1, 1, start_index, end_index - start_index)
        } else {
            null
        }

        var output = if (sm24 != null) {
            ByteArray(smpl.size + sm24.size)
        } else {
            ByteArray(smpl.size)
        }

        for (i in 0 until smpl.size / 2) {
            output[(i * 3) + 0] = smpl[(i * 2)]
            output[(i * 3) + 1] = smpl[(i * 2) + 1]
        }

        if (sm24 != null) {
            for (i in sm24.indices) {
                output[(i * 3) + 2] = sm24[i]
            }
        }

        return output
    }
}

data class Modulator(
    var sfModSrcOper: Int,
    var sfModDestOper: Int,
    var modAmount: Int,
    var sfModAmtSrcOper: Int,
    var sfModTransOper: Int
)

class Generator(
    var sfGenOper: Int,
    var shAmount: Int,
    var wAmount: Int
) {
    fun asInt(): Int {
        return shAmount + (wAmount * 256)
    }
    fun asIntSigned(): Int {
        val unsigned = shAmount + (wAmount * 256)
        // Get 2's compliment
        return if (unsigned shr 15 == 1) {
            unsigned.inv()
        } else {
            unsigned
        }
    }
    fun asTimecent(): Double {
        return (2.0).pow(this.asIntSigned().toDouble() / 1200)
    }
    fun asPair(): Pair<Int, Int> {
        return Pair(this.shAmount, this.wAmount)
    }
}

data class Sample(
    var name: String,
    var start: Int,
    var end: Int,
    var loopStart: Int,
    var loopEnd: Int,
    var sampleRate: Int,
    var originalPitch: Int,
    var pithCorrection: Int,
    var linkIndex: Int,
    var sampleType: Int
)

open class Generated {
    var key_range: Pair<Int, Int>? = null
    var velocity_range: Pair<Int, Int>? = null
    var attenuation: Double? = null
    var pan: Double? = null
    var tuning_semi: Int? = null
    var tuning_cent: Int? = null
    var scale_tuning: Int? = null
    var filter_cutoff: Int? = null
    var filter_resonance: Double? = null
    var vol_env_delay: Double? = null
    var vol_env_attack: Double? = null
    var vol_env_hold: Double? = null
    var vol_env_decay: Double? = null
    var vol_env_sustain: Double? = null
    var vol_env_release: Double? = null
    var key_vol_env_hold: Int? = null
    var key_vol_env_decay: Int? = null
    var mod_env_attack: Double? = null
    var mod_env_hold: Double? = null
    var mod_env_delay: Double? = null
    var mod_env_decay: Double? = null
    var mod_env_sustain: Double? = null
    var mod_env_release: Double? = null
    var mod_env_pitch: Int? = null
    var mod_env_filter: Int? = null
    var key_mod_env_hold: Int? = null
    var key_mod_env_decay: Int? = null
    var mod_lfo_delay: Double? = null
    var mod_lfo_freq: Double? = null
    var mod_lfo_pitch: Int? = null
    var mod_lfo_filter: Int? = null
    var mod_lfo_volume: Int? = null
    var vib_lfo_delay: Double? = null
    var vib_lfo_freq: Double? = null
    var vib_lfo_pitch: Int? = null
    var chorus: Double? = null
    var reverb: Double? = null
}

class Preset(
    var name: String = "",
    var preset: Int = 0, // MIDI Preset Number
    var bank: Int = 0, // MIDI Bank Number
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
) {
    var instruments: MutableList<PresetInstrument> = mutableListOf()
    var key_instrument_map = HashMap<Int, MutableList<Int>>()
    var vel_instrument_map = HashMap<Int, MutableList<Int>>()
    fun add_instrument(pinstrument: PresetInstrument) {
        this.instruments.add(pinstrument)
        val key_range = if (pinstrument.key_range != null) {
            pinstrument.key_range!!
        } else {
            Pair(0, 127)
        }
        for (i in key_range.first .. key_range.second) { // INCLUSIVE
            if (!this.key_instrument_map.containsKey(i)) {
                this.key_instrument_map[i] = mutableListOf()
            }
            this.key_instrument_map[i]!!.add(this.instruments.size - 1)
        }


        val velocity_range = if (pinstrument.velocity_range != null) {
            pinstrument.velocity_range!!
        } else {
            Pair(0, 127)
        }
        for (i in velocity_range.first .. velocity_range.second) { // INCLUSIVE
            if (!this.vel_instrument_map.containsKey(i)) {
                this.vel_instrument_map[i] = mutableListOf()
            }
            this.vel_instrument_map[i]!!.add(this.instruments.size - 1)
        }
    }

    fun get_instruments(key: Int, velocity: Int): List<PresetInstrument> {
        var output: MutableList<PresetInstrument> = mutableListOf()
        var key_indices = this.key_instrument_map[key]!!.toSet()
        var vel_indices = this.vel_instrument_map[velocity]!!.toSet()
        var active_indices = key_indices.intersect(vel_indices)
        for (index in active_indices) {
            output.add(this.instruments[index])
        }

        return output
    }
}

class PresetInstrument: Generated() {
    var instrumentIndex: Int = 0
}

class Instrument(var name: String) {
    var samples: MutableList<InstrumentSample> = mutableListOf()
    var key_sample_map = HashMap<Int, MutableList<Int>>()
    var vel_sample_map = HashMap<Int, MutableList<Int>>()
    fun add_sample(isample: InstrumentSample) {
        this.samples.add(isample)
        val key_range = if (isample.key_range != null) {
            isample.key_range!!
        } else {
            Pair(0, 127)
        }
        for (i in key_range.first .. key_range.second) { // INCLUSIVE
            if (!this.key_sample_map.containsKey(i)) {
                this.key_sample_map[i] = mutableListOf()
            }
            this.key_sample_map[i]!!.add(this.samples.size - 1)
        }


        val velocity_range = if (isample.velocity_range != null) {
            isample.velocity_range!!
        } else {
            Pair(0, 127)
        }
        for (i in velocity_range.first .. velocity_range.second) { // INCLUSIVE
            if (!this.vel_sample_map.containsKey(i)) {
                this.vel_sample_map[i] = mutableListOf()
            }
            this.vel_sample_map[i]!!.add(this.samples.size - 1)
        }
    }

    fun get_samples(key: Int, velocity: Int): List<InstrumentSample> {
        var output: MutableList<InstrumentSample> = mutableListOf()
        var key_indices = this.key_sample_map[key]!!.toSet()
        var vel_indices = this.vel_sample_map[velocity]!!.toSet()
        var active_indices = key_indices.intersect(vel_indices)
        for (index in active_indices) {
            output.add(this.samples[index])
        }

        return output
    }
}

class InstrumentSample: Generated() {
    var sampleIndex: Int = 0
    var sampleStartOffset: Int? = null
    var sampleEndOffset: Int? = null
    var loopStartOffset: Int? = null
    var loopEndOffset: Int? = null
    var sampleMode: Int? = null
    var root_key: Int? = null
    var exclusive_class: Int? = null
    var keynum: Int? = null
    var velocity: Int? = null
}

//------------ RIFF  --------------//
open class RiffChunk(var type: String)
class ListChunk(type: String, var sub_chunks: List<SubChunk>): RiffChunk(type)
data class SubChunk(var type: String, var bytes: ByteArray)

class Riff(var fileDescriptor: FileDescriptor) {
    var list_chunks: MutableList<Int> = mutableListOf()
    var sub_chunks: MutableList<List<Int>> = mutableListOf()

    init {
        var input_stream = FileInputStream(this.fileDescriptor)
        var fourcc = this.get_bytes(input_stream, 4).toString()
        var riff_size = this.get_little_endian(input_stream, 4)
        var typecc = this.get_bytes(input_stream, 4).toString()

        var working_index = 12
        while (working_index < riff_size - 4) {
            this.list_chunks.add(working_index)
            var tag = this.get_bytes(input_stream, 4).toString()
            working_index += 4
            var chunk_size = this.get_little_endian(input_stream, 4)
            working_index += 4

            var type = this.get_bytes(input_stream, 4)
            working_index += 4

            var sub_chunk_list: MutableList<Int> = mutableListOf()
            var sub_index = 0
            while (sub_index < chunk_size - 4) {
                sub_chunk_list.add(working_index + sub_index)
                var sub_chunk_tag = this.get_bytes(input_stream, 4).toString()
                sub_index += 4
                var sub_chunk_size = this.get_little_endian(input_stream, 4)
                sub_index += 4
                sub_index += sub_chunk_size
            }
            this.sub_chunks.add(sub_chunk_list)
        }
        input_stream.close()
    }

    fun get_list_chunk_type(list_index: Int): String {
        var input_stream = FileInputStream(this.fileDescriptor)
        var offset = this.list_chunks[list_index]
        this.get_bytes(input_stream, offset + 8) // Eat to offset
        var output = this.get_bytes(input_stream, 4).toString()
        input_stream.close()
        return output
    }
    fun get_sub_chunk_type(list_index: Int, chunk_index: Int): String {
        var input_stream = FileInputStream(this.fileDescriptor)
        var offset = this.sub_chunks[list_index][chunk_index]
        this.get_bytes(input_stream, offset) // Eat to offset
        var output = this.get_bytes(input_stream, 4).toString()
        input_stream.close()
        return output
    }

    fun get_sub_chunk_data(list_index: Int, chunk_index: Int, inner_offset: Int? = null, cropped_size: Int? = null): ByteArray {
        var input_stream = FileInputStream(this.fileDescriptor)
        var offset = this.sub_chunks[list_index][chunk_index]
        this.get_bytes(input_stream, offset + 4) // Eat to offset

        var size = this.get_little_endian(input_stream, 4)

        if (inner_offset != null) {
            this.get_bytes(input_stream, inner_offset)
            size -= inner_offset
        }

        if (cropped_size != null && cropped_size <= size) {
            size = cropped_size
        }

        var output = this.get_bytes(input_stream, size)
        input_stream.close()
        return output
    }

    fun get_bytes(input_stream: FileInputStream, size: Int): ByteArray {
        var buffer = ByteArray(size)
        input_stream.read(buffer)
        return buffer
    }

    fun get_little_endian(input_stream: FileInputStream, size: Int): Int {
        var buffer = ByteArray(size)
        input_stream.read(buffer)
        var output = 0
        for (i in 0 until size) {
            output *= 256
            output += buffer[size - 1 - i].toInt()
        }
        return output
    }

}
