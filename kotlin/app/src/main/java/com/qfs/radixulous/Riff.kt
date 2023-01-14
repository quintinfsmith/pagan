package com.qfs.radixulous

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.Log
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.InputStream
import kotlin.experimental.and
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
    var info_indices = HashMap<String, Int>()


    init {
        // Make a hashmap for easier access
        for (index in 0 until this.riff.sub_chunks[2].size) {
            var sub_chunk_type = this.riff.get_sub_chunk_type(2, index)
            this.pdta_indices[sub_chunk_type] = index
        }
        for (index in 0 until this.riff.sub_chunks[0].size) {
            var sub_chunk_type = this.riff.get_sub_chunk_type(2, index)
            this.info_indices[sub_chunk_type] = index
        }
    }

    fun get_sample(index: Int): Sample {
        var shdr_index = this.pdta_indices["shdr"]!!
        var shdr_bytes = this.riff.get_sub_chunk_data(2, shdr_index, (index * 46), 46)

        var sample_name = ""
        for (j in 0 until 20) {
            val b = toUInt(shdr_bytes[j])
            if (b == 0) {
                break
            }
            sample_name = "$sample_name${b.toChar()}"
        }


        return Sample(
            sample_name,
            toUInt(shdr_bytes[20])
                    + (toUInt(shdr_bytes[21]) * 256)
                    + (toUInt(shdr_bytes[22]) * 65536)
                    + (toUInt(shdr_bytes[23]) * 16777216),
            toUInt(shdr_bytes[24])
                    + (toUInt(shdr_bytes[25]) * 256)
                    + (toUInt(shdr_bytes[26]) * 65536)
                    + (toUInt(shdr_bytes[27]) * 16777216),
            toUInt(shdr_bytes[28])
                    + (toUInt(shdr_bytes[29]) * 256)
                    + (toUInt(shdr_bytes[30]) * 65536)
                    + (toUInt(shdr_bytes[31]) * 16777216),
            toUInt(shdr_bytes[32])
                    + (toUInt(shdr_bytes[33]) * 256)
                    + (toUInt(shdr_bytes[34]) * 65536)
                    + (toUInt(shdr_bytes[35]) * 16777216),
            toUInt(shdr_bytes[36])
                    + (toUInt(shdr_bytes[37]) * 256)
                    + (toUInt(shdr_bytes[38]) * 65536)
                    + (toUInt(shdr_bytes[39]) * 16777216),
            toUInt(shdr_bytes[40]),
            toUInt(shdr_bytes[41]),
            toUInt(shdr_bytes[42]) + (toUInt(shdr_bytes[43]) * 256),
            toUInt(shdr_bytes[44]) + (toUInt(shdr_bytes[45]) * 256)
        )
    }

    fun get_preset(index: Int): Preset {
        val phdr_index = this.pdta_indices["phdr"]!!
        var phdr_bytes = this.riff.get_sub_chunk_data(2, phdr_index, index * 38, 38)
        var phdr_name = ""
        for (j in 0 until 20) {
            val b = toUInt(phdr_bytes[j])
            if (b == 0) {
                break
            }
            phdr_name = "$phdr_name${b.toChar()}"
        }

        val wPresetBagIndex = toUInt(phdr_bytes[24]) + (toUInt(phdr_bytes[25]) * 256)

        val pbag_entry_size = 4
        val pbag_index = this.pdta_indices["pbag"]!!
        var ibag_bytes = this.riff.get_sub_chunk_data(
            2,
            pbag_index,
            (wPresetBagIndex * pbag_entry_size),
            pbag_entry_size * 2
        )

        var pbag = Pair(
            toUInt(ibag_bytes[0]) + (toUInt(ibag_bytes[1]) * 256),
            toUInt(ibag_bytes[2]) + (toUInt(ibag_bytes[3]) * 256)
        )

        var next_pbag = Pair(
            toUInt(ibag_bytes[4]) + (toUInt(ibag_bytes[5]) * 256),
            toUInt(ibag_bytes[6]) + (toUInt(ibag_bytes[7]) * 256)
        )

        val preset = Preset(phdr_name,
            toUInt(phdr_bytes[20]) + (toUInt(phdr_bytes[21]) * 256),
            toUInt(phdr_bytes[22]) + (toUInt(phdr_bytes[22]) * 256)
        )
        val generators_to_use: List<Generator> = this.get_preset_generators(
            pbag.first,
            next_pbag.first
        )

        this.generate_preset(preset, generators_to_use)

        return preset
    }

    fun get_instrument(index: Int): Instrument {
        val inst_index = this.pdta_indices["inst"]!!
        var name_bytes = this.riff.get_sub_chunk_data(2, inst_index, 0, 20)
        var inst_name = ""
        for (j in 0 until 20) {
            val b = toUInt(name_bytes[j])
            if (b == 0) {
                break
            }
            inst_name = "$inst_name${b.toChar()}"
        }

        val ibag_entry_size = 4
        val ibag_index = this.pdta_indices["ibag"]!!
        var bytes = this.riff.get_sub_chunk_data(
            2,
            ibag_index,
            (index * ibag_entry_size),
            ibag_entry_size * 2
        )

        var ibag = Pair(
            toUInt(bytes[0]) + (toUInt(bytes[1]) * 256),
            toUInt(bytes[2]) + (toUInt(bytes[3]) * 256)
        )

        var next_ibag = Pair(
            toUInt(bytes[4]) + (toUInt(bytes[5]) * 256),
            toUInt(bytes[6]) + (toUInt(bytes[7]) * 256)
        )

        val instrument = Instrument(inst_name)
        val generators_to_use: List<Generator> = this.get_instrument_generators(
            ibag.first,
            next_ibag.first
        )

        this.generate_instrument(instrument, generators_to_use)

        return instrument
    }

    fun get_instrument_modulators(from_index: Int, to_index: Int): List<Modulator> {
        var imod_index = this.pdta_indices["imod"]!!
        var output: MutableList<Modulator> = mutableListOf()
        var bytes = this.riff.get_sub_chunk_data(2, imod_index, from_index * 10, (to_index - from_index) * 10)

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

    fun get_instrument_generators(from_index: Int, to_index: Int): List<Generator> {
        var igen_index = this.pdta_indices["igen"]!!
        var output: MutableList<Generator> = mutableListOf()
        var bytes = this.riff.get_sub_chunk_data(2, igen_index, from_index * 10, (to_index - from_index) * 10)

        for (i in 0 until bytes.size / 10) {
            val offset = i * 10
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

    fun get_preset_modulators(from_index: Int, to_index: Int): List<Modulator> {
        var pmod_index = this.pdta_indices["pmod"]!!
        var output: MutableList<Modulator> = mutableListOf()
        var bytes = this.riff.get_sub_chunk_data(2, pmod_index, from_index * 10, (to_index - from_index) * 10)

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

    fun get_preset_generators(from_index: Int, to_index: Int): List<Generator> {
        var pgen_index = this.pdta_indices["pgen"]!!
        var output: MutableList<Generator> = mutableListOf()
        var bytes = this.riff.get_sub_chunk_data(2, pgen_index, from_index * 10, (to_index - from_index) * 10)

        for (i in 0 until bytes.size / 10) {
            val offset = i * 10
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
        instrument.add_sample(working_sample)
    }

    private fun generate_preset(preset: Preset, generators: List<Generator>) {
        var working_instrument = PresetInstrument()
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

    fun get_info_subchunk(tag: String): ByteArray? {
        if (!this.info_indices.containsKey(tag)) {
            return null
        }

        var tag_index = this.info_indices[tag]!!
        return this.riff.get_sub_chunk_data(0, tag_index)
    }

    fun get_ifil(): Pair<Int, Int>? {
        var bytes = this.get_info_subchunk("ifil") ?: return null
        return Pair(
            toUInt(bytes[0]) + (toUInt(bytes[1]) * 256),
            toUInt(bytes[2]) + (toUInt(bytes[3]) * 256)
        )
    }

    fun get_isng(): String? {
        var bytes = this.get_info_subchunk("isng") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_inam(): String? {
        var bytes = this.get_info_subchunk("INAM") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_irom(): String? {
        var bytes = this.get_info_subchunk("irom") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_iver(): Pair<Int, Int>? {
        var bytes = this.get_info_subchunk("iver") ?: return null
        return Pair(
            toUInt(bytes[0]) + (toUInt(bytes[1]) * 256),
            toUInt(bytes[2]) + (toUInt(bytes[3]) * 256)
        )
    }

    fun get_icrd(): String? {
        var bytes = this.get_info_subchunk("ICRD") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_ieng(): String? {
        var bytes = this.get_info_subchunk("IENG") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_iprd(): String? {
        var bytes = this.get_info_subchunk("IPRD") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_icop(): String? {
        var bytes = this.get_info_subchunk("ICOP") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_icmt(): String? {
        var bytes = this.get_info_subchunk("ICMT") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_isft(): String? {
        var bytes = this.get_info_subchunk("ISFT") ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun get_sample_data(start_index: Int, end_index: Int): ByteArray? {
        var smpl = this.riff.get_sub_chunk_data(1, 0, (start_index * 2), 2 * (end_index - start_index))
        var wordsize = 2
        var sm24 = if (this.riff.sub_chunks[1].size == 2) {
            wordsize = 3
            this.riff.get_sub_chunk_data(1, 1, start_index, end_index - start_index)
        } else {
            null
        }

        var output: ByteArray
        if (sm24 != null) {
            output = ByteArray(smpl.size + sm24.size)
            var bump = 0
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
                output[i * 2] = smpl[(i * 2) + 1]
                output[(i * 2) + 1] = smpl[i * 2]
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
    fun add_instrument(pinstrument: PresetInstrument) {
        this.instruments.add(pinstrument)
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
        var key_indices = if (this.key_sample_map.containsKey(key)) {
            this.key_sample_map[key]!!.toSet()
        } else {
            setOf()
        }
        var vel_indices = if (this.vel_sample_map.containsKey(velocity)) {
            this.vel_sample_map[velocity]!!.toSet()
        } else {
            setOf()
        }

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

//class Riff(var fileDescriptor: AssetFileDescriptor) {
class Riff(var assetmanager: AssetManager, var filePath: String) {
    var list_chunks: MutableList<Int> = mutableListOf()
    var sub_chunks: MutableList<List<Int>> = mutableListOf()

    init {
        var input_stream = this.assetmanager.open(filePath)
        var fourcc = this.get_string(input_stream, 4)
        var riff_size = this.get_little_endian(input_stream, 4)
        var typecc = this.get_string(input_stream, 4)

        var working_index = 12
        while (working_index < riff_size - 4) {
            this.list_chunks.add(working_index)
            var tag = this.get_string(input_stream, 4)
            working_index += 4

            var chunk_size = this.get_little_endian(input_stream, 4)
            working_index += 4

            var type = this.get_string(input_stream, 4)
            working_index += 4

            var sub_chunk_list: MutableList<Int> = mutableListOf()
            var sub_index = 0
            while (sub_index < chunk_size - 4) {
                sub_chunk_list.add(working_index + sub_index)

                var sub_chunk_tag = this.get_string(input_stream, 4)
                sub_index += 4

                var sub_chunk_size = this.get_little_endian(input_stream, 4)
                sub_index += 4


                input_stream.skip(sub_chunk_size.toLong()) // Eat chunk
                sub_index += sub_chunk_size
            }

            working_index += sub_index
            this.sub_chunks.add(sub_chunk_list)
        }

        input_stream.close()
    }

    fun get_list_chunk_type(list_index: Int): String {
        var input_stream = this.assetmanager.open(this.filePath)
        var offset = this.list_chunks[list_index]
        this.get_bytes(input_stream, offset + 8) // Eat to offset
        var output = this.get_string(input_stream, 4)
        input_stream.close()
        return output
    }

    fun get_sub_chunk_type(list_index: Int, chunk_index: Int): String {
        var input_stream = this.assetmanager.open(this.filePath)
        var offset = this.sub_chunks[list_index][chunk_index]
        input_stream.skip(offset.toLong()) // Eat to offset
        var output = this.get_string(input_stream, 4)
        input_stream.close()
        return output
    }

    fun get_sub_chunk_size(list_index: Int, chunk_index: Int): Int {
        var input_stream = this.assetmanager.open(this.filePath)
        var offset = this.sub_chunks[list_index][chunk_index]
        input_stream.skip(offset.toLong() + 4) // Eat to offset
        var output = this.get_little_endian(input_stream, 4)
        input_stream.close()
        return output
    }

    fun get_sub_chunk_data(list_index: Int, chunk_index: Int, inner_offset: Int? = null, cropped_size: Int? = null): ByteArray {
        var input_stream = this.assetmanager.open(this.filePath)

        // First get the offset of the sub chunk
        var offset = this.sub_chunks[list_index][chunk_index]
        // skip to offset
        input_stream.skip((offset + 4).toLong())
        // get the *actual* size of the sub chunk
        var size = this.get_little_endian(input_stream, 4)


        if (inner_offset != null) {
            input_stream.skip(inner_offset.toLong())
            size -= inner_offset
        }

        if (cropped_size != null && cropped_size <= size) {
            size = cropped_size
        }

        var output = this.get_bytes(input_stream, size)
        input_stream.close()
        return output
    }

    fun get_bytes(input_stream: InputStream, size: Int): ByteArray {
        var buffer = ByteArray(size)
        input_stream.read(buffer)
        return buffer
    }

    fun get_string(input_stream: InputStream, size: Int): String {
        return this.get_bytes(input_stream, size).toString(Charsets.UTF_8)
    }

    fun get_little_endian(input_stream: InputStream, size: Int): Int {
        var buffer = ByteArray(size)
        input_stream.read(buffer)
        var output = 0
        for (i in 0 until size) {
            output *= 256
            output += toUInt(buffer[size - 1 - i])
        }
        return output
    }
}
fun toUInt(byte: Byte): Int {
    var new_int = (byte and 0x7F.toByte()).toInt()
    if (byte and 0x80.toByte() == 0x80.toByte()) {
        new_int += 128
    }
    return new_int
}