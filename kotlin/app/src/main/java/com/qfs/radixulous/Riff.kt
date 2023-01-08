package com.qfs.radixulous


class SoundFont(riff: Riff) {
    // Mandatory INFO
    var ifil: Pair<Int, Int> = Pair(0,0)
    var isng: String = "EMU8000"
    var inam: String = ""

    //Optional INFO
    var irom: String? = null
    var iver: Double? = null
    var icrd: String? = null // Date
    var ieng: String? = null
    var iprd: String? = null
    var icop: String? = null
    var icmt: String? = null
    var isft: String? = null

    // Populated by sdta
    // NOTE: smpl size needs to be 2 * sm24 size
    var sampleData: ByteArray = ByteArray(0)

    var presets: List<Preset> = listOf()

    constructor(riff: Riff) {
        var tmp_samples_a: ByteArray? = null
        var tmp_samples_b: ByteArray? = null
        for (list_chunk in riff.list_chunks) {
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
                                this.inam = bytes.toString()
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
                            },
                            "sm24" -> {
                                tmp_sample_b = bytes
                            }
                            else -> {} // Throw error
                        }
                    }
                }
                "pdta" -> {
                    for (sub_chunk in list_chunk.sub_chunks) {
                        var bytes = sub_chunk.bytes
                        when (sub_chunk.type) {
                            "phdr" -> {
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // Merge sample data and convert to big-endian
        if (tmp_sample_a != null) {
            if (tmp_sample_b != null) {
                this.sampleData = ByteArray(tmp_sample_a.size * 1.5)
                for (i in 0 until tmp_sample_a.size) {
                    this.sampleData[(i * 3)] = tmp_sample_a[(2 * i) + 1]
                    this.sampleData[(i * 3) + 1] = tmp_sample_a[(2 * i)]
                }
                for (i in 0 until tmp_sample_b.size) {
                    this.sampleData[(i * 3) + 2] = tmp_sample_b[i]
                }
            } else {
                this.sampleData = ByteArray(tmp_sample_a.size)
                for (i in 0 until tmp_sample_a.size) {
                    this.sampleData[(i * 2)] = tmp_sample_a[(2 * i) + 1]
                    this.sampleData[(i * 2) + 1] = tmp_sample_a[(2 * i)]
                }
            }
        }
    }
}

enum class SFModulator {}
enum class SFGenerator {}
enum class Transform {}

//PHDR
class Preset {
    var name: String = ""
    var preset: Int = 0 // MIDI Preset Number
    var bank: Int = 0 // MIDI Bank Number
    var preset_generat
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
}

//PBAG
class PresetBag {
    var preset_generators: List<PresetGenerator>
    var preset_modulators: SfModList
}

//PMOD
class SfModList {
    var sfModSrcOper: SFModulator
    var sfDestOper: SFGenerator
    var modAmount: Int
    var sfModAmtSrcOper: SFModulator
    var sfModTransOper: SFTransform
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
