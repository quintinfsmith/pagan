package com.qfs.radixulous

class SoundFont {
    // Mandatory INFO
    var ifil: Pair<Int, Int> = Pair(0,0)
    var isng: String = "EMU8000"
    var inam: String = ""

    //Optional INFO
    var irom: String? = null
    var iver: Pair<Int, Int>? = null
    var icrd: String? = null // Date
    var ieng: String? = null
    var iprd: String? = null
    var icop: String? = null
    var icmt: String? = null
    var isft: String? = null

    // Populated by sdta
    // NOTE: smpl size needs to be 2 * sm24 size
    var instruments: MutableList<Instrument> = mutableListOf()
    var presets: MutableList<Preset> = mutableListOf()
    var samples: MutableList<Sample> = mutableListOf()
    var sampleData: ByteArray = ByteArray(0)

    constructor(riff: Riff) {
        var tmp_sample_a: ByteArray? = null
        var tmp_sample_b: ByteArray? = null
        var pdta_index: Int = 0
        riff.list_chunks.forEachIndexed { i, list_chunk ->
            when (list_chunk.type) {
                "INFO" -> {
                    for (sub_chunk in list_chunk.sub_chunks) {
                        var bytes = sub_chunk.bytes
                        when (sub_chunk.type) {
                            "ifil" -> {
                                this.ifil = Pair(
                                    bytes[0].toInt() + (bytes[1].toInt() * 256),
                                    bytes[2].toInt() + (bytes[3].toInt() * 256)
                                )
                            }
                            "isng" -> {
                                this.isng = bytes.toString()
                            }
                            "INAM" -> {
                                this.inam = bytes.toString()
                            }
                            "irom" -> {
                                this.irom = bytes.toString()
                            }
                            "iver" -> {
                                this.iver = Pair(
                                    bytes[0].toInt() + (bytes[1].toInt() * 256),
                                    bytes[2].toInt() + (bytes[3].toInt() * 256)
                                )
                            }
                            "ICRD" -> {
                                this.icrd = bytes.toString()
                            }
                            "IENG" -> {
                                this.ieng = bytes.toString()
                            }
                            "IPRD" -> {
                                this.iprd = bytes.toString()
                            }
                            "ICOP" -> {
                                this.icop = bytes.toString()
                            }
                            "ICMT" -> {
                                this.icmt = bytes.toString()
                            }
                            "ISFT" -> {
                                this.isft = bytes.toString()
                            }
                            else -> {} // Throw error
                        }
                    }
                }
                "sdta" -> {
                    for (sub_chunk in list_chunk.sub_chunks) {
                        var bytes = sub_chunk.bytes
                        when (sub_chunk.type) {
                            "smpl" -> {
                                tmp_sample_a = bytes
                            }
                            "sm24" -> {
                                tmp_sample_b = bytes
                            }
                            else -> {} // Throw error
                        }
                    }
                }
                "pdta" -> {
                    pdta_index = i
                }
                else -> {}
            }
        }

        // Merge sample data and convert to big-endian
        var sample_word_size = 2
        if (tmp_sample_a != null) {
            if (tmp_sample_b != null) {
                sample_word_size = 3
                this.sampleData = ByteArray(tmp_sample_a!!.size + tmp_sample_b!!.size)
                for (i in 0 until tmp_sample_a!!.size) {
                    this.sampleData[(i * 3)] = tmp_sample_a!![(2 * i) + 1]
                    this.sampleData[(i * 3) + 1] = tmp_sample_a!![(2 * i)]
                }
                for (i in 0 until tmp_sample_b!!.size) {
                    this.sampleData[(i * 3) + 2] = tmp_sample_b!![i]
                }
            } else {
                this.sampleData = ByteArray(tmp_sample_a!!.size)
                for (i in 0 until tmp_sample_a!!.size) {
                    this.sampleData[(i * 2)] = tmp_sample_a!![(2 * i) + 1]
                    this.sampleData[(i * 2) + 1] = tmp_sample_a!![(2 * i)]
                }
            }
        }

        var pdta_chunk = riff.list_chunks[pdta_index]

        // Make a hashmap for easier access
        var pdta_map = HashMap<String, SubChunk>()
        for (sub_chunk in pdta_chunk.sub_chunks) {
            pdta_map[sub_chunk.type] = sub_chunk
        }

        var ibag_bytes = pdta_map["ibag"]!!.bytes
        var inst_bytes = pdta_map["inst"]!!.bytes
        var shdr_bytes = pdta_map["shdr"]!!.bytes

        for (i in 0 until shdr_bytes.size / 46) {
            var offset = i * 46
            var sample_name = ""
            for (j in 0 until 20) {
                var b = shdr_bytes[offset + j].toInt()
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
        var instrument_count = pdta_map["inst"]!!.bytes.size / 22
        var ibag_entry_size = 4

        var instrument_list_indices: MutableList<Pair<Int, Int>> = mutableListOf()
        for (i in 0 until instrument_count) {
            var ibag_index = inst_bytes[(i * ibag_entry_size)] + (inst_bytes[(i * ibag_entry_size) + 1] * 256)
            var offset = i * ibag_entry_size
            instrument_list_indices.add(
                Pair(
                    ibag_bytes[offset + 0] + (ibag_bytes[offset + 1] * 256),
                    ibag_bytes[offset + 2] + (ibag_bytes[offset + 3] * 256)
                )
            )
        }

        var igenerators: MutableList<Generator> = mutableListOf()
        for (i in 0 until pdta_map["igen"]!!.bytes.size / 4) {
            var offset = i * 4
            igenerators.add(
                Generator(
                    pdta_map["igen"]!!.bytes[offset].toInt() + (pdta_map["igen"]!!.bytes[offset + 1].toInt() * 256),
                    pdta_map["igen"]!!.bytes[offset + 2].toInt(),
                    pdta_map["igen"]!!.bytes[offset + 3].toInt()
                )
            )
        }
        var imodulators: MutableList<Modulator> = mutableListOf()
        var imod_bytes = pdta_map["imod"]!!.bytes
        for (i in 0 until imod_bytes.size / 10) {
            var offset = i * 10
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
                var b = inst_bytes[(i * 22) + j].toInt()
                if (b == 0) {
                    break
                }
                instrument_name = "$instrument_name${b.toChar()}"
            }

            var instrument = Instrument(instrument_name)
            // Generators
            var generators_to_use: MutableList<Generator> = mutableListOf()

            for (j in instrument_list_indices[i].first until instrument_list_indices[i + 1].first) {
                generators_to_use.add(igenerators[j])
            }
            this.generate_instrument(instrument, generators_to_use)

            // Modulators
            for (j in instrument_list_indices[i].second until instrument_list_indices[i + 1].second) {
                this.modulate(instrument, imodulators[j])
            }
        }


        var pgenerators: MutableList<Generator> = mutableListOf()
        for (i in 0 until pdta_map["pgen"]!!.bytes.size / 4) {
            var offset = i * 4
            pgenerators.add(
                Generator(
                    pdta_map["pgen"]!!.bytes[offset + 0].toInt() + (pdta_map["pgen"]!!.bytes[offset + 1].toInt() * 256),
                    pdta_map["pgen"]!!.bytes[offset + 2].toInt(),
                    pdta_map["pgen"]!!.bytes[offset + 3].toInt()
                )
            )
        }
        var pmodulators: MutableList<Modulator> = mutableListOf()
        var pmod_bytes = pdta_map["pmod"]!!.bytes
        for (i in 0 until pmod_bytes.size / 10) {
            var offset = i * 10
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


        var preset_count = pdta_map["phdr"]!!.bytes.size / 38
        var pbag_entry_size = 4

        var preset_list_indices: MutableList<Pair<Int, Int>> = mutableListOf()
        var pbag_bytes = pdta_map["pbag"]!!.bytes
        for (i in 0 until preset_count) {
            var wPresetBagIndex = pdta_map["phdr"]!!.bytes[(i * 38) + 24] + (pdta_map["phdr"]!!.bytes[(i * 38) + 25] * 256)
            var offset = wPresetBagIndex * pbag_entry_size
            preset_list_indices.add(
                Pair(
                    pbag_bytes[offset + 0].toInt() + (pbag_bytes[offset + 1] * 256).toInt(),
                    pbag_bytes[offset + 2].toInt() + (pbag_bytes[offset + 3] * 256).toInt()
                )
            )
        }

        for (i in 0 until preset_count - 1) {
            var wGenNdx = preset_list_indices[i].first
            var wModNdx = preset_list_indices[i].second

            var wGenNdx_next = preset_list_indices[i + 1].first
            var wModNdx_next = preset_list_indices[i + 1].second

            var preset_generators: MutableList<Generator> = mutableListOf()
            for (j in wGenNdx until wGenNdx_next) {
                var generator = pgenerators[j]
                preset_generators.add(generator)
                if (generator.sfGenOper == 41) {
                    break
                }
            }

            var instrument_generators: MutableList<Generator> = mutableListOf()
            for (j in wGenNdx until wGenNdx_next) {
                var generator = igenerators[j]
                instrument_generators.add(generator)
                if (generator.sfGenOper == 41) {
                    break
                }
            }

            var name = ""
            var phdr_bytes = pdta_map["phdr"]!!.bytes
            for (j in 0 until 20) {
                var b = phdr_bytes[j + (i * 38)].toInt()
                if (b != 0) {
                    name = "$name${b.toChar()}"
                } else {
                    break
                }
            }

            this.presets.add(
                Preset(
                    name,
                    pdta_map["phdr"]!!.bytes[(i * 38) + 20] + (pdta_map["phdr"]!!.bytes[(i * 38) + 21] * 256),
                    pdta_map["phdr"]!!.bytes[(i * 38) + 22] + (pdta_map["phdr"]!!.bytes[(i * 38) + 22] * 256),
                    preset_generators,
                    pmodulators[wModNdx],
                )
            )
        }
        // Populate Samples
    }

    private fun modulate(modulatable: ModulatedGenerated, modulator: Modulator) {

    }

    private fun generate_instrument(instrument: Instrument, generators: List<Generator>) {
        var working_sample = InstrumentSample()
        for (generator in generators) {
            when (generator.sfGenOper) {
                0x35 -> {
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
                0x05 -> {
                    working_sample.mod_lfo_pitch = generator.asInt()
                }
                0x06 -> {
                    working_sample.vib_lfo_pitch = generator.asInt()
                }
                0x07 -> {
                    working_sample.mod_env_pitch = generator.asInt()
                }
            }

        }
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
    fun asDouble(): Double {

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

enum class SFModulator {}
enum class SFGenerator {}
enum class Transform {}

open class ModulatedGenerated() {
    //var generators: List<Generator>,
    //var modulators: List<Modulator>,
    var key_range: Pair<Int, Int>? = null
    var velocity_range: Pair<Int, Int>? = null
    var attenuation: Double? = null
    var pan: Int? = null
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
    var mod_env_sustain: Double? = null
    var mod_env_release: Double? = null
    var mod_env_pitch: Int? = null
    var mod_env_filter: Int? = null
    var key_mod_env_hold: Int? = null
    var key_mod_env_decay: Int? = null
    var mod_lfo_delay: Int? = null
    var mod_lfo_freq: Int? = null
    var mod_lfo_pitch: Int? = null
    var mod_lfo_filter: Int? = null
    var mod_lfo_volume: Int? = null
    var vib_lfo_delay: Int? = null
    var vib_lfo_freq: Int? = null
    var vib_lfo_pitch: Int? = null
    var chorus: Int? = null
    var reverb: Int? = null
}

class Preset(
    var name: String = "",
    var preset: Int = 0, // MIDI Preset Number
    var bank: Int = 0, // MIDI Bank Number
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
): ModulatedGenerated() {

}

class Instrument(var name: String) {
    var samples: MutableList<InstrumentSample> = mutableListOf()
    fun add_sample(isample: InstrumentSample) {
        this.samples.add(isample)
    }
}
class InstrumentSample: ModulatedGenerated() {
    var sampleIndex: Int = 0
    var fixedKey: Int? = null
    var fixedVelocity: Int? = null
    var sampleStartOffset: Int? = null
    var sampleEndOffset: Int? = null
    var loopStartOffset: Int? = null
    var loopEndOffset: Int? = null
    var vib_lfo_delay: Double? = null
    var vib_lf_freq: Double? = null
    var vib_lfo_pitch: Int? = null
}



open class RiffChunk(var type: String)
class Riff(type: String, var list_chunks: List<ListChunk>): RiffChunk(type)
class ListChunk(type: String, var sub_chunks: List<SubChunk>): RiffChunk(type)
data class SubChunk(var type: String, var bytes: ByteArray)

//abstract class InfoChunk: SubChunk() { }
//abstract class SdtaChunk: SubChunk() { }
//abstract class PdtaChunk: SubChunk() { }
//
//data class IfilChunk: InfoChunk() { }
//data class IsngChunk: InfoChunk() { }
//data class InamChunk: InfoChunk() { }
//data class IromChunk: InfoChunk() { }
//data class IverChunk: InfoChunk() { }
//data class IcrdChunk: InfoChunk() { }
//data class IengChunk: InfoChunk() { }
//data class IprdChunk: InfoChunk() { }
//data class IcopChunk: InfoChunk() { }
//data class IcmtChunk: InfoChunk() { }
//data class IsftChunk: InfoChunk() { }
//
//data class SmplChunk: SdtaChunk() { }
//data class Sm24Chunk: SdtaChunk() { }
//
//data class PhdrChunk: PdtaChunk() {}
//data class PbagChunk: PdtaChunk() {}
//data class PmodChunk: PdtaChunk() {}
//data class PgenChunk: PdtaChunk() {}
//data class InstChunk: PdtaChunk() {}
//data class IbagChunk: PdtaChunk() {}
//data class ImodChunk: PdtaChunk() {}
//data class IgenChunk: PdtaChunk() {}
//data class ShdrChunk: PdtaChunk() {}

class RiffReader {
    fun from_bytes(bytes: ByteArray): Riff {
        var fourcc: String = ""
        var size = 0
        var typecc: String = ""
        for (i in 0 until 4) {
            fourcc = "$fourcc${bytes[i].toInt().toChar()}"
            typecc = "$typecc${bytes[i + 8].toInt().toChar()}"
            size *= 256
            size += bytes[7 - i].toInt()
        }

        if  (fourcc != "RIFF") {
            throw Exception("Invalid RIFF")
        }

        if (size % 2 == 1) {
            size += 1
        }

        var next_bytes = ByteArray(size)
        for (i in 0 until size) {
            next_bytes[i] = bytes[i + 12]
        }


        return Riff(typecc, this.get_list_chunks(next_bytes))
    }

    fun get_list_chunks(bytes: ByteArray): List<ListChunk> {
        var output: MutableList<ListChunk> = mutableListOf()
        var current_offset = 0
        while (current_offset < bytes.size) {
            var fourcc: String = ""
            var typecc: String = ""
            var size = 0
            for (i in 0 until 4) {
                fourcc = "$fourcc${bytes[i + current_offset].toInt().toChar()}"
                typecc = "$typecc${bytes[current_offset + i + 8].toInt().toChar()}"
                size *= 256
                size += bytes[current_offset + 7 - i].toInt()
            }

            if (fourcc != "LIST") {
                throw Exception("Invalid LIST Chunk")
            }

            var next_bytes = ByteArray(size)
            for (i in 0 until size) {
                next_bytes[i] = bytes[i + 12]
            }

            output.add(ListChunk(typecc, this.get_sub_chunks(next_bytes)))

            size += (size % 2) // consider padding
            current_offset += size
        }

        return output
    }

    fun get_sub_chunks(bytes: ByteArray): List<SubChunk> {
        var output: MutableList<SubChunk> = mutableListOf()
        var current_offset = 0
        while (current_offset < bytes.size) {
            var fourcc: String = ""
            var size = 0
            for (i in 0 until 4) {
                fourcc = "$fourcc${bytes[i + current_offset].toInt().toChar()}"
                size *= 256
                size += bytes[current_offset + 7 - i].toInt()
            }

            var next_bytes = ByteArray(size)
            for (i in 0 until size) {
                next_bytes[i] = bytes[i + 12]
            }

            output.add(SubChunk(fourcc, next_bytes))

            size += (size % 2) // consider padding
            current_offset += size
        }
        return output
    }

}
