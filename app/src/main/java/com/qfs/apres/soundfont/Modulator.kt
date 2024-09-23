package com.qfs.apres.soundfont

class Modulator(val source_operator: Operator, val value: Operator, val destination: Int, val transform: TransformOperation, val amount: Short) {
    companion object {
        // TODO: Fix shit name
        fun from_spec(sfModSrcOper: Int, sfModDestOper: Int, amount: Int, sfModAmtSrcOper: Int, sfModTransOper: Int): Modulator {
            return Modulator(
                source_operator = Operator.from_int(sfModSrcOper),
                value = Operator.from_int(sfModAmtSrcOper),
                destination = sfModDestOper,
                transform = when (sfModTransOper) {
                    0 -> TransformOperation.Linear
                    2 -> TransformOperation.Absolute
                    else -> throw InvalidTransformOperation(sfModTransOper)
                },
                amount = amount.toShort()
            )
        }
    }

    class InvalidModulatorException(modulator_value: Int): Exception("${Integer.toHexString(modulator_value)}")
    class InvalidTransformOperation(value: Int): Exception("${value}")
    enum class ModulatorSourceType {
        Linear,
        Concave,
        Convex,
        Switch
    }

    enum class TransformOperation {
        Linear,
        Absolute
    }

    class Operator(var link: Boolean, var continuous: Boolean, var polar: Boolean, var direction: Boolean, var source_type: ModulatorSourceType) {
        companion object {
            fun from_int(input: Int): Operator {
                return Operator(
                    link = input and 0x00FF == 0x00FF,
                    continuous = (input and 0x0080) == 0x0080,
                    polar = input and 0x0100 == 0x0100,
                    direction = input and 0x0200 == 0x0200,
                    source_type = when (input shr 10) {
                        0 -> ModulatorSourceType.Linear
                        1 -> ModulatorSourceType.Concave
                        2 -> ModulatorSourceType.Convex
                        3 -> ModulatorSourceType.Switch
                        else -> {
                            throw InvalidModulatorException(input)
                        }
                    }
                )
            }
        }
        fun to_string(): String {
            return "link: ${this.link}\ncontinuous: ${this.continuous}\npolar: ${this.polar}\ndirection: ${this.direction}\nsource_type: ${this.source_type}"
        }
    }


    fun to_string(): String {
        return "Mod Amount: ${Integer.toHexString(amount.toInt())}\n" +
        "Source Mod: ${this.source_operator.to_string().replace("\n", "\n    ")}\n" +
        "Source Trans Oper: ${this.transform}"
    }
}
