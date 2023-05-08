package com.qfs.pagan.apres

import android.content.res.AssetManager
import com.qfs.pagan.apres.riffreader.Riff
import com.qfs.pagan.apres.riffreader.toUInt
import kotlin.math.max
import kotlin.math.pow

//class SoundFont(input_stream: InputStream) {
class SoundFont(var assets: AssetManager, var file_name: String) {
    class InvalidPresetIndex(index: Int, bank: Int): Exception("Preset Not Found $index:$bank")
    class InvalidSampleIdPosition(): Exception("SampleId Generator is not at end of ibag")
    data class CachedData(var data: ByteArray, var count: Int = 1)
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

    private var riff: Riff

    var pdta_chunks = HashMap<String, ByteArray>()

    init {
        this.riff = Riff(assets, file_name) { riff: Riff ->
            var info_chunk = riff.get_chunk_data(riff.list_chunks[0])
            var pdta_chunk = riff.get_chunk_data(riff.list_chunks[2])
            var info_offset = riff.list_chunks[0].index
            riff.sub_chunks[0].forEach { header: Riff.SubChunkHeader ->
                // '-12' since the sub chunk index is relative to the list chunk, but the list chunk index is absolute
                var header_offset = header.index + 8 - info_offset - 12
                when (header.tag) {
                    "ifil" -> {
                        this.ifil = Pair(
                            toUInt(info_chunk[header_offset + 0]) + (toUInt(info_chunk[header_offset + 1]) * 256),
                            toUInt(info_chunk[header_offset + 2]) + (toUInt(info_chunk[header_offset + 3]) * 256)
                        )
                    }
                    "isng" -> {
                        this.isng = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "INAM" -> {
                        this.inam = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "irom" -> {
                        this.irom = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "iver" -> {
                        this.iver = Pair(
                            toUInt(info_chunk[header_offset + 0]) + (toUInt(info_chunk[header_offset + 1]) * 256),
                            toUInt(info_chunk[header_offset + 2]) + (toUInt(info_chunk[header_offset + 3]) * 256)
                        )
                    }
                    "ICRD" -> {
                        this.icrd = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "IENG" -> {
                        this.ieng = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "IPRD" -> {
                        this.iprd = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "ICOP" -> {
                        this.icop = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "ICMT" -> {
                        this.icmt = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    "ISFT" -> {
                        this.isft = ByteArray(header.size) { j -> info_chunk[j + header_offset] }.toString(Charsets.UTF_8)
                    }
                    else -> {}
                }
            }

            var pdta_offset = riff.list_chunks[2].index
            riff.sub_chunks[2].forEachIndexed { i: Int, header: Riff.SubChunkHeader ->
                // '-12' since the sub chunk index is relative to the list chunk, but the list chunk index is absolute
                var offset = header.index + 8 - pdta_offset - 12
                this.pdta_chunks[header.tag] = ByteArray(header.size) { j ->
                    pdta_chunk[offset + j]
                }
            }
        }
    }

    fun get_sample(sample_index: Int): Sample {
        val shdr_bytes = this.pdta_chunks["shdr"]!!

        val offset = sample_index * 46

        var sample_name = ""
        for (j in 0 until 20) {
            val b = toUInt(shdr_bytes[offset + j])
            if (b == 0) {
                break
            }
            sample_name = "$sample_name${b.toChar()}"
        }

        val start = toUInt(shdr_bytes[offset + 20]) + (toUInt(shdr_bytes[offset + 21]) * 256) + (toUInt(shdr_bytes[offset + 22]) * 65536) + (toUInt(shdr_bytes[offset + 23]) * 16777216)
        val end = toUInt(shdr_bytes[offset + 24]) + (toUInt(shdr_bytes[offset + 25]) * 256) + (toUInt(shdr_bytes[offset + 26]) * 65536) + (toUInt(shdr_bytes[offset + 27]) * 16777216)

        //val sample_data = this.get_sample_data(start, end)!!
        return Sample(
            sample_name,
            toUInt(shdr_bytes[offset + 28])
                    + (toUInt(shdr_bytes[offset + 29]) * 256)
                    + (toUInt(shdr_bytes[offset + 30]) * 65536)
                    + (toUInt(shdr_bytes[offset + 31]) * 16777216)
                    - start,
            toUInt(shdr_bytes[offset + 32])
                    + (toUInt(shdr_bytes[offset + 33]) * 256)
                    + (toUInt(shdr_bytes[offset + 34]) * 65536)
                    + (toUInt(shdr_bytes[offset + 35]) * 16777216)
                    - start,
            toUInt(shdr_bytes[offset + 36])
                    + (toUInt(shdr_bytes[offset + 37]) * 256)
                    + (toUInt(shdr_bytes[offset + 38]) * 65536)
                    + (toUInt(shdr_bytes[offset + 39]) * 16777216),
            toUInt(shdr_bytes[offset + 40]),
            toUInt(shdr_bytes[offset + 41]),
            toUInt(shdr_bytes[offset + 42]) + (toUInt(shdr_bytes[offset + 43]) * 256),
            toUInt(shdr_bytes[offset + 44]) + (toUInt(shdr_bytes[offset + 45]) * 256),
            data_placeholder = Pair(start, end)
        )
    }

    fun get_available_presets(bank: Int): Set<Int> {
        val output = mutableSetOf<Int>()
        val phdr_bytes = this.pdta_chunks["phdr"]!!

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

            val current_index = toUInt(phdr_bytes[offset + 20]) + (toUInt(phdr_bytes[offset + 21]) * 256)
            val current_bank = toUInt(phdr_bytes[offset + 22]) + (toUInt(phdr_bytes[offset + 23]) * 256)

            if (current_bank == bank) {
                output.add(current_index)
            }
        }
        return output
    }

    fun get_preset(preset_index: Int, preset_bank: Int = 0): Preset {
        var output: Preset? = null
        val pbag_entry_size = 4

        this.riff.with { riff: Riff ->
            val phdr_bytes = this.pdta_chunks["phdr"]!!
            // Loop throught PHDR until we find the correct index/bank
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

                val current_index = toUInt(phdr_bytes[offset + 20]) + (toUInt(phdr_bytes[offset + 21]) * 256)
                val current_bank = toUInt(phdr_bytes[offset + 22]) + (toUInt(phdr_bytes[offset + 23]) * 256)

                // No need to process other preset information
                if (preset_index != current_index || preset_bank != current_bank) {
                    continue
                }

                var preset = Preset(phdr_name, current_index, current_bank)

                val wPresetBagIndex = toUInt(phdr_bytes[offset + 24]) + (toUInt(phdr_bytes[offset + 25]) * 256)
                val next_wPresetBagIndex = toUInt(phdr_bytes[38 + offset + 24]) + (toUInt(phdr_bytes[38 + offset + 25]) * 256)
                val zone_count = next_wPresetBagIndex - wPresetBagIndex
                var pbag_pairs = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
                for (j in 0 until zone_count) {
                    val pbag_bytes = ByteArray(pbag_entry_size * 2) { k: Int ->
                        pdta_chunks["pbag"]!![((j + wPresetBagIndex) * pbag_entry_size) + k]
                    }

                    pbag_pairs.add(
                        Pair(
                            Pair(
                                toUInt(pbag_bytes[0]) + (toUInt(pbag_bytes[1]) * 256),
                                toUInt(pbag_bytes[2]) + (toUInt(pbag_bytes[3]) * 256)
                            ),
                            Pair(
                                toUInt(pbag_bytes[4]) + (toUInt(pbag_bytes[5]) * 256),
                                toUInt(pbag_bytes[6]) + (toUInt(pbag_bytes[7]) * 256)
                            )
                        )
                    )
                }
                for ((pbag, next_pbag) in pbag_pairs) {
                    val generators_to_use: List<Generator> = this.get_preset_generators(
                        riff,
                        pbag.first,
                        next_pbag.first
                    )

                    this.generate_preset(preset, generators_to_use, current_index)
                }
                output = preset
                break
            }

            // NOW we can load all the sample data
            if (output != null) {
                var ordered_samples = mutableListOf<Sample>()
                for (preset_instrument in output!!.instruments) {
                    var instrument = preset_instrument.instrument ?: continue

                    for (instrument_sample in instrument.samples) {
                        var sample = instrument_sample.sample ?: continue
                        ordered_samples.add(sample)
                    }
                }
                ordered_samples = ordered_samples.sortedBy { sample: Sample ->
                    sample.data_placeholder.first
                }.toMutableList()
                var loaded_sample_data = HashMap<Pair<Int, Int>, ByteArray?>()
                for (sample in ordered_samples) {
                    if (! loaded_sample_data.containsKey(sample.data_placeholder)) {
                        loaded_sample_data[sample.data_placeholder] = this.get_sample_data(
                            sample.data_placeholder.first,
                            sample.data_placeholder.second
                        )
                    }
                    sample.data = loaded_sample_data[sample.data_placeholder]!!
                }
            }
        }

        // Order the samples then load them from the inputStream

        return output ?: throw InvalidPresetIndex(preset_index,preset_bank)
    }

    fun get_instrument(instrument_index: Int): Instrument {
        val ibag_entry_size = 4
        val inst_bytes = this.pdta_chunks["inst"]!!

        val offset = instrument_index * 22
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
            val ibag_bytes = ByteArray(ibag_entry_size) { k ->
                this.pdta_chunks["ibag"]!![(ibag_entry_size * (first_ibag_index + j)) + k]
            }
            val next_ibag_bytes = ByteArray(ibag_entry_size) { k ->
                this.pdta_chunks["ibag"]!![(ibag_entry_size * (first_ibag_index + j + 1)) + k]
            }

            val ibag = Pair(
                toUInt(ibag_bytes[0]) + (toUInt(ibag_bytes[1]) * 256),
                toUInt(ibag_bytes[2]) + (toUInt(ibag_bytes[3]) * 256)
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

        return instrument
    }


    private fun get_instrument_modulators(riff: Riff, from_index: Int, to_index: Int): List<Modulator> {
        val output: MutableList<Modulator> = mutableListOf()
        val bytes = ByteArray((to_index - from_index) * 10) { i ->
            this.pdta_chunks["imod"]!![(from_index * 10) + i]
        }

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
        val output: MutableList<Modulator> = mutableListOf()
        val bytes = ByteArray((to_index - from_index) * 10) { i ->
            this.pdta_chunks["pmod"]!![(from_index * 10) + i]
        }

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
        val output: MutableList<Generator> = mutableListOf()
        val bytes = ByteArray((to_index - from_index) * 4) { i ->
            this.pdta_chunks["pgen"]!![(from_index * 4) + i]
        }
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
        val output: MutableList<Generator> = mutableListOf()
        val bytes = ByteArray((to_index - from_index) * 4) { i ->
            this.pdta_chunks["igen"]!![(from_index * 4) + i]
        }

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
                working_generated.tuning_semi = generator.asIntSigned()
            }
            0x34 -> {
                working_generated.tuning_cent = generator.asIntSigned()
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
                        throw InvalidSampleIdPosition()
                    }
                    working_sample.sample = this.get_sample(generator.asInt())
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

    private fun generate_preset(preset: Preset, generators: List<Generator>, default_instrument: Int = 0) {
        val working_instrument = PresetInstrument()
        var instrument_set = false

        for (generator in generators) {
            when (generator.sfGenOper) {
                0x29 -> {
                    working_instrument.instrument = this.get_instrument(generator.asInt())
                    instrument_set = true
                }
                else -> {
                    this.generate(working_instrument, generator)
                }
            }
        }

        if (! instrument_set && preset.global_zone != null) {
            working_instrument.instrument = this.get_instrument(default_instrument)
        }

        preset.add_instrument(working_instrument)
    }

    fun get_sample_data(start_index: Int, end_index: Int): ByteArray? {

        val smpl = this.riff.get_sub_chunk_data(this.riff.sub_chunks[1][0],  (start_index * 2), 2 * (end_index - start_index))
        var wordsize = 2
        val sm24 = if (this.riff.sub_chunks[1].size == 2) {
            wordsize = 3
            this.riff.get_sub_chunk_data(this.riff.sub_chunks[1][1], start_index, end_index - start_index)
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
        val p = this.asIntSigned() / 1200
        return (2.0).pow(p.toDouble())
    }
    fun asPair(): Pair<Int, Int> {
        return Pair(this.shAmount, this.wAmount)
    }
}

data class Sample(
    var name: String,
    var loopStart: Int,
    var loopEnd: Int,
    var sampleRate: Int,
    var originalPitch: Int,
    var pitchCorrection: Int,
    var linkIndex: Int,
    var sampleType: Int,
    var data_placeholder: Pair<Int, Int>,
    var data: ByteArray? = null
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
    var global_zone: PresetInstrument? = null

    fun add_instrument(pinstrument: PresetInstrument) {
        if (pinstrument.instrument == null && global_zone == null) {
            this.global_zone = pinstrument
        } else {
            this.instruments.add(pinstrument)
        }
    }

    fun get_instruments(key: Int, velocity: Int): Set<PresetInstrument> {
        val output = mutableSetOf<PresetInstrument>()
        this.instruments.forEachIndexed { _, instrument ->
            if ( (instrument.key_range == null || (instrument.key_range!!.first <= key && instrument.key_range!!.second >= key)) &&
                (instrument.velocity_range == null || (instrument.velocity_range!!.first <= velocity && instrument.velocity_range!!.second >= velocity))
            ) {
                output.add(instrument)
            }
        }
        return output
    }

}

class PresetInstrument: Generated() {
    var instrument: Instrument? = null
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

class InstrumentSample: Generated() {
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
}

enum class sfSampleType {
    Mono,
    Right,
    Left,
    Linked,
    RomMono,
    RomRight,
    RomLeft,
    RomLinked
}