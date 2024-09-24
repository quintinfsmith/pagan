package com.qfs.apres.soundfont

class Modulator(val source_operator: Operator, val value: Operator, val destination: Int, val transform: TransformOperation, val amount: Short) {
    companion object {
        // Defaults
        val DEFAULT_NOTEON_VELOCITY_TO_INITIAL_ATTENUATION = Modulator(
            Operator(
                type = ModulatorSourceType.Concave,
                polar = false,
                direction = true,
                continuous = false,
                index = 2,
            ),
            Operator.NO_CONTROLLER,
            48,
            TransformOperation.Linear,
            960
        )
        val DEFAULT_NOTEON_VELOCITY_TO_FILTER_CUTOFF = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = false,
                direction = true,
                continuous = false,
                index = 2,
            ),
            destination = 8,
            amount = -2400, //TODO: Double check this doesn't need to be altered
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )
        val DEFAULT_CHANNEL_PRESSURE_TO_VIBRATO_LFO_PITCH_DEPTH = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = false,
                direction = false,
                continuous = false,
                index = 13
            ),
            destination = 6,
            amount = 50,
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )
        val DEFAULT_CONTINUOUS_CONTROLLER_1_TO_VIBRATO_LFO_PITCH_DEPTH = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = false,
                direction = false,
                continuous = true,
                index = 1
            ),
            destination = 6,
            amount = 50,
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )
        val DEFAULT_CONTINUOUS_CONTROLLER_7_TO_INITIAL_ATTENUATION = Modulator(
            Operator(
                type = ModulatorSourceType.Concave,
                polar = false,
                direction = false,
                continuous = true,
                index = 7
            ),
            destination = 48,
            amount = 960,
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )
        val DEFAULT_CONTINUOUS_CONTROLLER_10_TO_PAN_POSITION = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = true,
                direction = false,
                continuous = true,
                index = 10
            ),
            destination = 17,
            amount = 1000, // 1000 tenths of a percent. TODO: Double check this is stored correctly
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )

        val DEFAULT_CONTINUOUS_CONTROLLER_11_TO_INITIAL_ATTENUATION = Modulator(
            Operator(
                type = ModulatorSourceType.Concave,
                polar = false,
                direction = true,
                continuous = true,
                index = 11
            ),
            destination = 48,
            amount = 960,
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )

        val DEFAULT_CONTINUOUS_CONTROLLER_91_TO_REVERB_EFFECTS_SEND = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = false,
                direction = false,
                continuous = true,
                index = 91
            ),
            destination = 16,
            amount = 200, // 200 tenths of a percent TODO: DOUBLE CHECK
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )

        val DEFAULT_CONTINUOUS_CONTROLLER_93_TO_CHORUS_EFFECTS_SEND = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = false,
                direction = false,
                continuous = true,
                index = 93
            ),
            destination = 15,
            amount = 200, // 10ths of a percent TODO DOUBLE CHECK
            value = Operator.NO_CONTROLLER,
            transform = TransformOperation.Linear
        )

        // TODO: Not sure what is meant by 'initial Pitch' in the spec (8.4.10), leaving for now
        //val DEFAULT_PITCH_WHEEL_TO_INITIAL_PITCH_CONTROLLED_BY_PITCH_WHEEL_SENSITIVITY = Modulator(
        //    Operator(
        //        type = ModulatorSourceType.Linear,
        //        polar = true,
        //        direction = false,
        //        continuous = false,
        //        index = 14
        //    ),
        //    destination =

        //)

        //

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

    class Operator(var index: Int, var continuous: Boolean, var polar: Boolean, var direction: Boolean, var type: ModulatorSourceType) {
        companion object {
            val NO_CONTROLLER: Operator = Operator(0, false, false, false, ModulatorSourceType.Linear)
            fun from_int(input: Int): Operator {
                return Operator(
                    index = input and 0x003f,
                    //link = input and 0x00FF == 0x00FF,
                    continuous = (input and 0x0080) == 0x0080,
                    polar = input and 0x0100 == 0x0100,
                    direction = input and 0x0200 == 0x0200,
                    type = when (input shr 10) {
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
            return "index: ${this.index}\ncontinuous: ${this.continuous}\npolar: ${this.polar}\ndirection: ${this.direction}\nsource_type: ${this.type}"
        }
    }


    fun to_string(): String {
        return "Mod Amount: ${Integer.toHexString(amount.toInt())}\n" +
        "V: ${this.value.to_string().replace("\n", "\n    ")}\n" +
        "D: ${this.destination}\n" +
        "Source Mod: ${this.source_operator.to_string().replace("\n", "\n    ")}\n" +
        "Source Trans Oper: ${this.transform}"
    }
}
