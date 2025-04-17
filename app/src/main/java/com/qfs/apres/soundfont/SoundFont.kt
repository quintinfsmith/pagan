package com.qfs.apres.soundfont

import com.qfs.apres.soundfont.Generator.Operation
import com.qfs.apres.toUInt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SoundFont(file_path: String) {
    class InvalidSoundFont(file_path: String): Exception("Not a soundfont $file_path")
    class InvalidPresetIndex(index: Int, bank: Int): Exception("Preset Not Found $index:$bank")
    class InvalidSampleIdPosition : Exception("SampleId Generator is not at end of ibag")
    class InvalidSampleType(i: Int): Exception("Unknown Sample Type $i")
    class NoIROMDeclared: Exception("Need irom declared to read from ROM")

    data class CachedSampleData(var data: ShortArray, var count: Int = 1)

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

    private var pdta_chunks = HashMap<String, ByteArray>()
    private var sample_data_cache =  HashMap<Pair<Int, Int>, CachedSampleData>()

    companion object {
        fun sample_type_from_int(i: Int): SampleType {
            return when (i) {
                0x0001 -> SampleType.Mono
                0x0002 -> SampleType.Right
                0x0004 -> SampleType.Left
                0x0008 -> SampleType.Linked
                0x8001 -> SampleType.RomMono
                0x8002 -> SampleType.RomRight
                0x8004 -> SampleType.RomLeft
                0x8008 -> SampleType.RomLinked
                else -> throw InvalidSampleType(i)
            }
        }
    }

    init {
        this.riff = Riff(file_path)
        this.riff.with { riff: Riff ->
            if (riff.type_cc != "sfbk") {
                throw InvalidSoundFont(file_path)
            }
            val info_chunk = riff.get_chunk_data(riff.list_chunks[0])
            val pdta_chunk = riff.get_chunk_data(riff.list_chunks[2])
            val info_offset = riff.list_chunks[0].index
            for (header in riff.sub_chunks[0]) {
                // '-12' since the sub chunk index is relative to the list chunk, but the list chunk index is absolute
                val header_offset = header.index + 8 - info_offset - 12
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

            val pdta_offset = riff.list_chunks[2].index
            for (header in riff.sub_chunks[2]) {
                // '-12' since the sub chunk index is relative to the list chunk, but the list chunk index is absolute
                val offset = header.index + 8 - pdta_offset - 12
                this.pdta_chunks[header.tag] = ByteArray(header.size) { j ->
                    pdta_chunk[offset + j]
                }
            }
        }
    }


    fun get_sample(sample_index: Int, build_linked: Boolean = true): Sample {
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
        val sample_type = toUInt(shdr_bytes[offset + 44]) + (toUInt(shdr_bytes[offset + 45]) * 256)
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
            when (sample_type) {
                0x0002,
                0x0004,
                0x0008 -> {
                    val linked_addr = toUInt(shdr_bytes[offset + 42]) + (toUInt(shdr_bytes[offset + 43]) * 256)
                    if (build_linked && linked_addr != 0) {
                        this.get_sample(linked_addr, false)
                    } else {
                        null
                    }
                }
                else -> null
            },
            sample_type,
            data_placeholder = Pair(start, end)
        )
    }

    fun get_available_presets(): Set<Triple<String, Int, Int>> {
        val output = mutableSetOf<Triple<String, Int, Int>>()
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

            val current_program = toUInt(phdr_bytes[offset + 20]) + (toUInt(phdr_bytes[offset + 21]) * 256)
            val current_bank = toUInt(phdr_bytes[offset + 22]) + (toUInt(phdr_bytes[offset + 23]) * 256)

            output.add(Triple(phdr_name, current_program, current_bank))
        }
        return output
    }

    fun get_preset(preset_index: Int, preset_bank: Int = 0): Preset {
        var output: Preset? = null
        val pbag_entry_size = 4

        val phdr_bytes= this.pdta_chunks["phdr"]!!
        // Loop through PHDR until we find the correct index/bank
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

            val preset = Preset(phdr_name, current_index, current_bank)

            val wPresetBagIndex = toUInt(phdr_bytes[offset + 24]) + (toUInt(phdr_bytes[offset + 25]) * 256)
            val next_wPresetBagIndex = toUInt(phdr_bytes[38 + offset + 24]) + (toUInt(phdr_bytes[38 + offset + 25]) * 256)
            val zone_count = next_wPresetBagIndex - wPresetBagIndex
            val pbag_pairs = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
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
                    pbag.first,
                    next_pbag.first,
                )

                val modulators_to_use = this.get_preset_modulators(
                    pbag.second,
                    next_pbag.second
                )

                this.generate_preset(preset, generators_to_use, modulators_to_use)
            }
            output = preset
            break
        }

        // NOW we can load all the sample data
        if (output != null) {
            var ordered_samples = mutableListOf<Sample>()
            for ((_, preset_instrument) in output.instruments) {
                val instrument = preset_instrument.instrument ?: continue

                if (instrument.samples.isNotEmpty()) {
                    for (instrument_sample in instrument.samples.values) {
                        val sample = instrument_sample.sample ?: continue
                        ordered_samples.add(sample)
                    }
                } else {
                    val sample = instrument.global_zone.sample ?: continue
                    ordered_samples.add(sample)
                }
            }

            ordered_samples = ordered_samples.sortedBy { sample: Sample ->
                sample.data_placeholder.first
            }.toMutableList()


            this.riff.with {
                for (sample in ordered_samples) {
                    this.apply_sample_data(sample)

                    val linked_sample = sample.linked_sample
                    if (linked_sample != null) {
                        this.apply_sample_data(Sample(linked_sample))
                    }
                }
            }
        }

        return output ?: throw InvalidPresetIndex(preset_index, preset_bank)
    }

    fun apply_sample_data(sample: Sample) {
        sample.set_data(when (sample.sample_type) {
            0x8001,
            0x8002,
            0x8004,
            0x8008 -> {
                if (this.irom == null) {
                    throw NoIROMDeclared()
                }
                val placeholder = sample.data_placeholder
                this.read_rom_hook(placeholder.first, placeholder.second)
            }
            else -> {
                val placeholder = sample.data_placeholder
                this.get_sample_data(placeholder.first, placeholder.second)
            }
        })
    }

    open fun read_rom_hook(start: Int, end: Int): ShortArray {
        return ShortArray(0)
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
                ibag.first,
                next_ibag.first
            )

            val modulators_to_use = this.get_instrument_modulators(
                ibag.second,
                next_ibag.second
            )

            this.generate_instrument(instrument, generators_to_use, modulators_to_use)
        }

        return instrument
    }


    private fun get_instrument_modulators(from_index: Int, to_index: Int): List<Modulator> {
        val output: MutableList<Modulator> = mutableListOf()
        val bytes = ByteArray((to_index - from_index) * 10) { i ->
            this.pdta_chunks["imod"]!![(from_index * 10) + i]
        }

        // TODO: CHECK sfModDestOper if (offset + 2/3) & 0x80, then pointing to another modulator
        for (i in 0 until bytes.size / 10) {
            val offset = i * 10
            try {
                output.add(
                    Modulator.from_spec(
                        toUInt(bytes[offset + 0]) + (toUInt(bytes[offset + 1]) * 256),
                        toUInt(bytes[offset + 2]) + (toUInt(bytes[offset + 3]) * 256),
                        toUInt(bytes[offset + 4]) + (toUInt(bytes[offset + 5]) * 256),
                        toUInt(bytes[offset + 6]) + (toUInt(bytes[offset + 7]) * 256),
                        toUInt(bytes[offset + 8]) + (toUInt(bytes[offset + 9]) * 256)
                    )
                )
            } catch (e: Modulator.InvalidModulatorException) {
                // ignore
            } catch (e: Modulator.InvalidTransformOperation) {
                // ignore
            }
        }

        return output
    }

    private fun get_preset_modulators(from_index: Int, to_index: Int): List<Modulator> {
        val output: MutableList<Modulator> = mutableListOf()
        val bytes = ByteArray((to_index - from_index) * 10) { i ->
            this.pdta_chunks["pmod"]!![(from_index * 10) + i]
        }

        for (i in 0 until bytes.size / 10) {
            val offset = i * 10
            try {
                output.add(
                    Modulator.from_spec(
                        toUInt(bytes[offset + 0]) + (toUInt(bytes[offset + 1]) * 256),
                        toUInt(bytes[offset + 2]) + (toUInt(bytes[offset + 3]) * 256),
                        toUInt(bytes[offset + 4]) + (toUInt(bytes[offset + 5]) * 256),
                        toUInt(bytes[offset + 6]) + (toUInt(bytes[offset + 7]) * 256),
                        toUInt(bytes[offset + 8]) + (toUInt(bytes[offset + 9]) * 256)
                    )
                )
            } catch (e: Modulator.InvalidModulatorException) {
                // ignore
            } catch (e: Modulator.InvalidTransformOperation) {
                // ignore
            }
        }

        return output
    }

    private fun get_preset_generators(from_index: Int, to_index: Int): List<Generator> {
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

    private fun get_instrument_generators(from_index: Int, to_index: Int): List<Generator> {
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

    private fun generate_instrument(instrument: Instrument, generators: List<Generator>, modulators: List<Modulator>) {
        val is_global = generators.isEmpty() || generators.last().sfGenOper != 0x35

        val working_sample = SampleDirective()

        for (modulator in modulators) {
            working_sample.add_modulator(modulator)
        }

        working_sample.apply_generators(generators)
        if (generators.last().sfGenOper != 0x35) {
            throw InvalidSampleIdPosition()
        }
        working_sample.sample = this.get_sample(generators.last().asInt())


        if (is_global) {
            instrument.set_global_zone(working_sample)
        } else {
            instrument.add_sample(working_sample)
        }
    }

    private fun generate_preset(preset: Preset, generators: List<Generator>, modulators: List<Modulator>) {
        val is_global = generators.isEmpty() || generators.last().sfGenOper != 0x29 // && preset.instruments.isEmpty()
        val working_instrument = InstrumentDirective()
        for (modulator in modulators) {
            working_instrument.add_modulator(modulator)
        }

        working_instrument.apply_generators(generators)

        for (generator in generators) {
            if (generator.sfGenOper == 0x29) {
                working_instrument.instrument = this.get_instrument(generator.asInt())
                break
            }
        }

        if (!is_global) {
            preset.add_instrument(working_instrument)
        } else {
            preset.set_global_zone(working_instrument)
        }
    }

    private fun get_sample_data(start_index: Int, end_index: Int): ShortArray {
        val cache_key = Pair(start_index, end_index)

        if (this.sample_data_cache.containsKey(cache_key)) {
            this.sample_data_cache[cache_key]!!.count += 1
            return this.sample_data_cache[cache_key]!!.data
        }

        val smpl = this.riff.get_sub_chunk_data(this.riff.sub_chunks[1][0],  (start_index * 2), 2 * (end_index - start_index))
        val sm24 = if (this.riff.sub_chunks[1].size == 2) {
            this.riff.get_sub_chunk_data(this.riff.sub_chunks[1][1], start_index, end_index - start_index)
        } else {
            null
        }

        val inbuffer = ByteBuffer.wrap(smpl)
        inbuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = ShortArray(smpl.size / 2) {
            inbuffer.getShort()
        }

        // TODO: support. Can be ignored and it'll just have a lower resolution
        //if (sm24 != null) {
        //    //output = ShortArray((smpl.size + sm24.size) / 2)
        //    //var inbuffer = ByteBuffer.wrap(smpl)
        //    //inbuffer.order(ByteOrder.LITTLE_ENDIAN)
        //    //for (i in 0 until (smpl.size / 2)) {
        //    //    output[i + (i * 2)] = smpl[(i * 2) + 1]
        //    //    output[i + ((i * 2) + 1)] = smpl[i * 2]
        //    //}
        //    //for (i in sm24.indices) {
        //    //    output[(i * 3) + 2] = sm24[i]
        //    //}
        //}

        if (!this.sample_data_cache.containsKey(cache_key)) {
            this.sample_data_cache[cache_key] = CachedSampleData(
                data = output,
                count = 1
            )
        }
        return output
    }
}

