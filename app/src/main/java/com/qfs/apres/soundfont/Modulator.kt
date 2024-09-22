package com.qfs.apres.soundfont

class Modulator(
    sfModSrcOper: Int,
    sfModDestOper: Int,
    val amount: Short,
    sfModAmtSrcOper: Int,
    var sfModTransOper: Int
) {
    class InvalidModulatorException(modulator_value: Int): Exception("${Integer.toHexString(modulator_value)}")
    class InvalidTransformOperation(value: Int): Exception("${value}")
    enum class ModulatorSourceType {
        Linear,
        Concave,
        Convex,
        Switch
    }
    class Operator(sfModSrcOper: Int) {
        val link = sfModSrcOper and 0x00FF == 0x00FF
        val continuous = (sfModSrcOper and 0x0080) == 0x0080
        val polar = sfModSrcOper and 0x0100 == 0x0100
        val direction = sfModSrcOper and 0x0200 == 0x0200
        val source_type = when (sfModSrcOper shr 10) {
            0 -> ModulatorSourceType.Linear
            1 -> ModulatorSourceType.Concave
            2 -> ModulatorSourceType.Convex
            3 -> ModulatorSourceType.Switch
            else -> {
                throw InvalidModulatorException(sfModSrcOper)
            }
        }

        fun to_string(): String {
            return "link: ${this.link}\ncontinuous: ${this.continuous}\npolar: ${this.polar}\ndirection: ${this.direction}\nsource_type: ${this.source_type}"
        }
    }

    enum class TransformOperation {
        Linear,
        Absolute
    }

    val source_operator = Operator(sfModSrcOper)
    val valve = Operator(sfModAmtSrcOper)
    val generator = sfModDestOper
    val transform_operation = when (sfModTransOper) {
        0 -> TransformOperation.Linear
        2 -> TransformOperation.Absolute
        else -> throw InvalidTransformOperation(sfModTransOper)
    }

    fun to_string(): String {
        return "Mod Amount: ${Integer.toHexString(amount.toInt())}\n" +
        "Source Mod: ${this.source_operator.to_string().replace("\n", "\n    ")}\n" +
        "Source Trans Oper: ${this.transform_operation}"
    }
}
