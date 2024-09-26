package com.qfs.apres.soundfont

import kotlin.math.pow

class Generator(
    var sfGenOper: Int,
    var shAmount: Int,
    var wAmount: Int
) {
    companion object {
        fun get_operation(sfGenOper: Int): Generator.Operation {
            return when (sfGenOper) {
                0x05 -> Operation.ModLFOPitch
                0x06 -> Operation.VibLFOPitch
                0x07 -> Operation.ModEnvPitch
                0x08 -> Operation.FilterCutoff
                0x09 -> Operation.FilterResonance
                0x0A -> Operation.ModLFOFilter
                0x0B -> Operation.ModEnvFilter
                0x0D -> Operation.ModLFOToVolume
                0x0F -> Operation.Chorus
                0x10 -> Operation.Reverb
                0x11 -> Operation.Pan
                0x15 -> Operation.ModLFODelay
                0x16 -> Operation.ModLFOFrequency
                0x17 -> Operation.VibLFODelay
                0x18 -> Operation.VibLFOFrequency
                0x19 -> Operation.ModEnvDelay
                0x1A -> Operation.ModEnvAttack
                0x1B -> Operation.ModEnvHold
                0x1C -> Operation.ModEnvDecay
                0x1D -> Operation.ModEnvSustain
                0x1E -> Operation.ModEnvRelease
                0x1F -> Operation.KeyModEnvHold
                0x20 -> Operation.KeyModEnvDecay
                0x21 -> Operation.VolEnvDelay
                0x22 -> Operation.VolEnvAttack
                0x23 -> Operation.VolEnvHold
                0x24 -> Operation.VolEnvDecay
                0x25 -> Operation.VolEnvSustain
                0x26 -> Operation.VolEnvRelease
                0x27 -> Operation.KeyVolEnvHold
                0x28 -> Operation.KeyVolEnvDecay
                0x2B -> Operation.KeyRange
                0x2C -> Operation.VelocityRange
                0x30 -> Operation.Attenuation
                0x33 -> Operation.TuningFine
                0x34 -> Operation.TuningCoarse
                0x38 -> Operation.ScaleTuning
                else -> Operation.Unknown
            }
        }
    }
    enum class Operation {
        ModLFOPitch,
        VibLFOPitch,
        ModEnvPitch,
        FilterCutoff,
        FilterResonance,
        ModLFOFilter,
        ModEnvFilter,
        ModLFOToVolume,
        Chorus,
        Reverb,
        Pan,
        ModLFODelay,
        ModLFOFrequency,
        VibLFODelay,
        VibLFOFrequency,
        ModEnvDelay,
        ModEnvAttack,
        ModEnvHold,
        ModEnvDecay,
        ModEnvSustain,
        ModEnvRelease,
        KeyModEnvHold,
        KeyModEnvDecay,
        VolEnvDelay,
        VolEnvAttack,
        VolEnvHold,
        VolEnvDecay,
        VolEnvSustain,
        VolEnvRelease,
        KeyVolEnvHold,
        KeyVolEnvDecay,
        KeyRange,
        VelocityRange,
        Attenuation,
        TuningFine,
        TuningCoarse,
        ScaleTuning,
        Unknown
    }

    fun get_operation(): Generator.Operation {
        return Generator.get_operation(this.sfGenOper)
    }

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

    fun asTimecent(): Float {
        val p = this.asIntSigned().toFloat() / 1200F
        return (2F).pow(p)
    }

    fun asPair(): Pair<Int, Int> {
        return Pair(this.shAmount, this.wAmount)
    }
}
