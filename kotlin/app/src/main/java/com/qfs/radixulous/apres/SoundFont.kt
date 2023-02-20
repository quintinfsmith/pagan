package com.qfs.radixulous.apres

import com.qfs.radixulous.apres.riffreader.Riff
import com.qfs.radixulous.apres.riffreader.toUInt
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

    var pdta_indices = HashMap<String, Int>()

    var samples: List<Sample> = listOf()
    var instruments: List<Instrument> = listOf()
    var presets: HashMap<Pair<Int, Int>, Preset>

    init {
        // Make a hashmap for easier access
        for (index in 0 until riff.sub_chunks[2].size) {
            val sub_chunk_type = riff.get_sub_chunk_type(2, index)
            this.pdta_indices[sub_chunk_type] = index
        }

        for (index in 0 until riff.sub_chunks[0].size) {
            when (riff.get_sub_chunk_type(2, index)) {
                "ifil" -> {
                    val bytes = riff.get_sub_chunk_data(2, index)
                    this.ifil = Pair(
                        toUInt(bytes[0]) + (toUInt(bytes[1]) * 256),
                        toUInt(bytes[2]) + (toUInt(bytes[3]) * 256)
                    )
                }
                "isng" -> {
                    this.isng = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "INAM" -> {
                    this.inam = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "irom" -> {
                    this.irom = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "iver" -> {
                    val bytes = riff.get_sub_chunk_data(2, index)
                    this.iver = Pair(
                        toUInt(bytes[0]) + (toUInt(bytes[1]) * 256),
                        toUInt(bytes[2]) + (toUInt(bytes[3]) * 256)
                    )

                }
                "ICRD" -> {
                    this.icrd = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "IENG" -> {
                    this.ieng = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "IPRD" -> {
                    this.iprd = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "ICOP" -> {
                    this.icop = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "ICMT" -> {
                    this.icmt = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                "ISFT" -> {
                    this.isft = riff.get_sub_chunk_data(2, index).toString(Charsets.UTF_8)
                }
                else -> {}
            }
        }

        this.samples = this.init_samples(riff)
        this.instruments = this.init_instruments(riff)
        this.presets = this.init_presets(riff)
    }

    private fun init_samples(riff: Riff): List<Sample> {
        val output: MutableList<Sample> = mutableListOf()
        val shdr_index = this.pdta_indices["shdr"]!!
        val shdr_bytes = riff.get_sub_chunk_data(2, shdr_index)
        for (index in 0 until shdr_bytes.size / 46) {
            val offset = index * 46
            var sample_name = ""
            for (j in 0 until 20) {
                val b = toUInt(shdr_bytes[offset + j])
                if (b == 0) {
                    break
                }
                sample_name = "$sample_name${b.toChar()}"
            }

            output.add(
                Sample(
                    sample_name,
                    toUInt(shdr_bytes[offset + 20])
                            + (toUInt(shdr_bytes[offset + 21]) * 256)
                            + (toUInt(shdr_bytes[offset + 22]) * 65536)
                            + (toUInt(shdr_bytes[offset + 23]) * 16777216),
                    toUInt(shdr_bytes[offset + 24])
                            + (toUInt(shdr_bytes[offset + 25]) * 256)
                            + (toUInt(shdr_bytes[offset + 26]) * 65536)
                            + (toUInt(shdr_bytes[offset + 27]) * 16777216),
                    toUInt(shdr_bytes[offset + 28])
                            + (toUInt(shdr_bytes[offset + 29]) * 256)
                            + (toUInt(shdr_bytes[offset + 30]) * 65536)
                            + (toUInt(shdr_bytes[offset + 31]) * 16777216),
                    toUInt(shdr_bytes[offset + 32])
                            + (toUInt(shdr_bytes[offset + 33]) * 256)
                            + (toUInt(shdr_bytes[offset + 34]) * 65536)
                            + (toUInt(shdr_bytes[offset + 35]) * 16777216),
                    toUInt(shdr_bytes[offset + 36])
                            + (toUInt(shdr_bytes[offset + 37]) * 256)
                            + (toUInt(shdr_bytes[offset + 38]) * 65536)
                            + (toUInt(shdr_bytes[offset + 39]) * 16777216),
                    toUInt(shdr_bytes[offset + 40]),
                    toUInt(shdr_bytes[offset + 41]),
                    toUInt(shdr_bytes[offset + 42]) + (toUInt(shdr_bytes[offset + 43]) * 256),
                    toUInt(shdr_bytes[offset + 44]) + (toUInt(shdr_bytes[offset + 45]) * 256)
                )
            )
        }
        return output
    }

    private fun init_presets(riff: Riff): HashMap<Pair<Int, Int>, Preset> {
        val output = HashMap<Pair<Int, Int>, Preset>()
        val phdr_index = this.pdta_indices["phdr"]!!
        val pbag_index = this.pdta_indices["pbag"]!!

        val phdr_bytes = riff.get_sub_chunk_data(2, phdr_index)

        val pbag_entry_size = 4
        for (index in 0 until (phdr_bytes.size / 38) - 1) {
            val offset = index * 38
            var phdr_name = ""
            for (j in 0 until 20) {
                val b = toUInt(phdr_bytes[j + offset])
                if (b == 0) {
                    break
                }
                phdr_name = "$phdr_name${b.toChar()}"
            }

            val preset = Preset(
                phdr_name,
                toUInt(phdr_bytes[offset + 20]) + (toUInt(phdr_bytes[offset + 21]) * 256),
                toUInt(phdr_bytes[offset + 22]) + (toUInt(phdr_bytes[offset + 23]) * 256)
            )

            val wPresetBagIndex = toUInt(phdr_bytes[offset + 24]) + (toUInt(phdr_bytes[offset + 25]) * 256)
            val next_wPresetBagIndex = toUInt(phdr_bytes[38 + offset + 24]) + (toUInt(phdr_bytes[38 + offset + 25]) * 256)
            val zone_count = next_wPresetBagIndex - wPresetBagIndex
            for (j in 0 until zone_count) {

                val pbag_bytes = riff.get_sub_chunk_data(
                    2,
                    pbag_index,
                    (j + wPresetBagIndex) * pbag_entry_size,
                    pbag_entry_size * 2
                )

                val pbag = Pair(
                    toUInt(pbag_bytes[0]) + (toUInt(pbag_bytes[1]) * 256),
                    toUInt(pbag_bytes[2]) + (toUInt(pbag_bytes[3]) * 256)
                )

                val next_pbag = Pair(
                    toUInt(pbag_bytes[4]) + (toUInt(pbag_bytes[5]) * 256),
                    toUInt(pbag_bytes[6]) + (toUInt(pbag_bytes[7]) * 256)
                )

                val generators_to_use: List<Generator> = this.get_preset_generators(
                    riff,
                    pbag.first,
                    next_pbag.first
                )

                this.generate_preset(preset, generators_to_use)
            }
            output[Pair(preset.bank, preset.preset)] = preset
        }

        return output
    }

    private fun init_instruments(riff: Riff): List<Instrument> {
        val output: MutableList<Instrument> = mutableListOf()
        val inst_index = this.pdta_indices["inst"]!!
        val ibag_bytes_index = this.pdta_indices["ibag"]!!
        val ibag_entry_size = 4
        val inst_bytes = riff.get_sub_chunk_data(2, inst_index)
        for (index in 0 until (inst_bytes.size / 22) - 1) {
            val offset = index * 22
            var inst_name = ""
            for (j in 0 until 20) {
                val b = toUInt(inst_bytes[offset + j])
                if (b == 0) {
                    break
                }
                inst_name = "$inst_name${b.toChar()}"
            }

            val first_ibag_index = toUInt(inst_bytes[offset + 20]) + (toUInt(inst_bytes[offset + 21]) * 256)
            val next_first_ibag_index = toUInt(inst_bytes[22 + offset + 20]) + (toUInt(inst_bytes[22 + offset + 21]) * 256)
            val zone_count = next_first_ibag_index - first_ibag_index

            val instrument = Instrument(inst_name)
            for (j in 0 until zone_count) {
                val ibag_bytes = riff.get_sub_chunk_data(
                    2,
                    ibag_bytes_index,
                    ibag_entry_size * (first_ibag_index + j),
                    ibag_entry_size
                )
                val ibag = Pair(
                    toUInt(ibag_bytes[0]) + (toUInt(ibag_bytes[1]) * 256),
                    toUInt(ibag_bytes[2]) + (toUInt(ibag_bytes[3]) * 256)
                )
                val next_ibag_bytes = riff.get_sub_chunk_data(
                    2,
                    ibag_bytes_index,
                    ibag_entry_size * (first_ibag_index + j + 1),
                    ibag_entry_size
                )

                val next_ibag = Pair(
                    toUInt(next_ibag_bytes[0]) + (toUInt(next_ibag_bytes[1]) * 256),
                    toUInt(next_ibag_bytes[2]) + (toUInt(next_ibag_bytes[3]) * 256)
                )

                val generators_to_use: List<Generator> = this.get_instrument_generators(
                    riff,
                    ibag.first,
                    next_ibag.first
                )

                this.generate_instrument(instrument, generators_to_use)
            }

            output.add(instrument)
        }
        return output
    }

    private fun get_instrument_modulators(riff: Riff, from_index: Int, to_index: Int): List<Modulator> {
        val imod_index = this.pdta_indices["imod"]!!
        val output: MutableList<Modulator> = mutableListOf()
        val bytes = riff.get_sub_chunk_data(2, imod_index, from_index * 10, (to_index - from_index) * 10)

        for (i in 0 until bytes.size / 10) {
            val offset = i * 10
            output.add(
                Modulator(
                    toUInt(bytes[offset + 0]) + (toUInt(bytes[offset + 1]) * 256),
                    toUInt(bytes[offset + 2]) + (toUInt(bytes[offset + 3]) * 256),
                    toUInt(bytes[offset + 4]) + (toUInt(bytes[offset + 5]) * 256),
                    toUInt(bytes[offset + 6]) + (toUInt(bytes[offset + 7]) * 256),
                    toUInt(bytes[offset + 8]) + (toUInt(bytes[offset + 9]) * 256)
                )
            )
        }

        return output
    }

    private fun get_preset_modulators(riff: Riff, from_index: Int, to_index: Int): List<Modulator> {
        val pmod_index = this.pdta_indices["pmod"]!!
        val output: MutableList<Modulator> = mutableListOf()
        val bytes = riff.get_sub_chunk_data(2, pmod_index, from_index * 10, (to_index - from_index) * 10)

        for (i in 0 until bytes.size / 10) {
            val offset = i * 10
            output.add(
                Modulator(
                    toUInt(bytes[offset + 0]) + (toUInt(bytes[offset + 1]) * 256),
                    toUInt(bytes[offset + 2]) + (toUInt(bytes[offset + 3]) * 256),
                    toUInt(bytes[offset + 4]) + (toUInt(bytes[offset + 5]) * 256),
                    toUInt(bytes[offset + 6]) + (toUInt(bytes[offset + 7]) * 256),
                    toUInt(bytes[offset + 8]) + (toUInt(bytes[offset + 9]) * 256)
                )
            )
        }

        return output
    }

    private fun get_preset_generators(riff: Riff, from_index: Int, to_index: Int): List<Generator> {
        val pgen_index = this.pdta_indices["pgen"]!!
        val output: MutableList<Generator> = mutableListOf()
        val bytes = riff.get_sub_chunk_data(2, pgen_index, from_index * 4, (to_index - from_index) * 4)

        for (i in 0 until bytes.size / 4) {
            val offset = i * 4
            output.add(
                Generator(
                    toUInt(bytes[offset + 0]) + (toUInt(bytes[offset + 1]) * 256),
                    toUInt(bytes[offset + 2]),
                    toUInt(bytes[offset + 3])
                )
            )
        }

        return output
    }

    private fun get_instrument_generators(riff: Riff, from_index: Int, to_index: Int): List<Generator> {
        val igen_index = this.pdta_indices["igen"]!!
        val output: MutableList<Generator> = mutableListOf()
        val bytes = riff.get_sub_chunk_data(2, igen_index, from_index * 4, (to_index - from_index) * 4)

        for (i in 0 until bytes.size / 4) {
            val offset = i * 4
            output.add(
                Generator(
                    toUInt(bytes[offset + 0]) + (toUInt(bytes[offset + 1]) * 256),
                    toUInt(bytes[offset + 2]),
                    toUInt(bytes[offset + 3])
                )
            )
        }

        return output
    }

    fun get_instrument(index: Int): Instrument {
        return this.instruments[index]
    }
    fun get_preset(index: Int, bank: Int = 0): Preset? {
        return this.presets[Pair(bank, index)]
    }
    fun get_sample(index: Int): Sample {
        return samples[index]
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
        val working_sample = InstrumentSample()
        generators.forEachIndexed { i, generator ->
            when (generator.sfGenOper) {
                0x35 -> {
                    if (i != generators.size - 1) {
                        throw Exception("SampleId Generator Out of order ($i / ${generators.size})")
                    }
                    working_sample.sampleIndex = generator.asInt()
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
        instrument.add_sample(working_sample)
    }

    private fun generate_preset(preset: Preset, generators: List<Generator>) {
        val working_instrument = PresetInstrument()
        for (generator in generators) {
            when (generator.sfGenOper) {
                0x29 -> {
                    working_instrument.instrumentIndex = generator.asInt()
                }
                else -> {
                    this.generate(working_instrument, generator)
                }
            }
        }
        preset.add_instrument(working_instrument)
    }


    fun get_sample_data(start_index: Int, end_index: Int): ByteArray? {
        val smpl = this.riff.get_sub_chunk_data(1, 0, (start_index * 2), 2 * (end_index - start_index))
        var wordsize = 2
        val sm24 = if (this.riff.sub_chunks[1].size == 2) {
            wordsize = 3
            this.riff.get_sub_chunk_data(1, 1, start_index, end_index - start_index)
        } else {
            null
        }

        val output: ByteArray
        if (sm24 != null) {
            output = ByteArray(smpl.size + sm24.size)
            for (i in 0 until (smpl.size / 2)) {
                output[i + (i * 2)] = smpl[(i * 2) + 1]
                output[i + ((i * 2) + 1)] = smpl[i * 2]
            }
            for (i in sm24.indices) {
                output[(i * 3) + 2] = sm24[i]
            }
        } else {
            // TODO: May need to handle smpl.size % 2 == 1
            output = ByteArray(smpl.size)
            for (i in 0 until (smpl.size / 2)) {
                output[i * 2] = smpl[(i * 2)]
                output[(i * 2) + 1] = smpl[(i * 2) + 1]
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
            0 - (((unsigned xor 0xFFFF) + 1) and 0x7FFF)
        } else {
            unsigned
        }
    }
    fun asTimecent(): Double {
        var p = this.asIntSigned() / 1200
        return (2.0).pow(p.toDouble())
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
    fun add_instrument(pinstrument: PresetInstrument) {
        this.instruments.add(pinstrument)
    }
}

class PresetInstrument: Generated() {
    var instrumentIndex: Int = 0
}

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

    fun get_sample(key: Int, velocity: Int): InstrumentSample? {
        this.samples.forEachIndexed { i, sample ->
            if ((sample.key_range == null || (sample.key_range!!.first <= key && sample.key_range!!.second >= key)) &&
                (sample.velocity_range == null || (sample.velocity_range!!.first <= velocity && sample.velocity_range!!.second >= velocity))) {
                return sample
            }
        }
        return null
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
