package com.qfs.apres.soundfont


class Modulator(val source_operator: Operator, val value: Operator, val destination: Generator.Operation, val transform: TransformOperation, val amount: Short) {
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
            value = Operator.NO_CONTROLLER,
            destination = Generator.Operation.Attenuation,
            transform = TransformOperation.Linear,
            amount = 960.toShort()
        )
        val DEFAULT_NOTEON_VELOCITY_TO_FILTER_CUTOFF = Modulator(
            Operator(
                type = ModulatorSourceType.Linear,
                polar = false,
                direction = true,
                continuous = false,
                index = 2,
            ),
            destination = Generator.Operation.FilterCutoff,
            //amount = -2400.toShort(), //TODO: Double check this doesn't need to be altered
            amount = 1.toShort(), //TODO: Double check this doesn't need to be altered
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
            destination = Generator.Operation.VibLFOPitch,
            amount = 50.toShort(),
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
            destination = Generator.Operation.VibLFOPitch,
            amount = 50.toShort(),
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
            destination = Generator.Operation.Attenuation,
            amount = 960.toShort(),
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
            destination = Generator.Operation.Pan,
            amount = 1000.toShort(), // 1000 tenths of a percent. TODO: Double check this is stored correctly
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
            destination = Generator.Operation.Attenuation,
            amount = 960.toShort(),
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
            destination = Generator.Operation.Reverb,
            amount = 200.toShort(), // 200 tenths of a percent TODO: DOUBLE CHECK
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
            destination = Generator.Operation.Chorus,
            amount = 200.toShort(), // 10ths of a percent TODO DOUBLE CHECK
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
                destination = Generator.get_operation(sfModDestOper),
                transform = when (sfModTransOper) {
                    0 -> TransformOperation.Linear
                    2 -> TransformOperation.Absolute
                    else -> throw InvalidTransformOperation(sfModTransOper)
                },
                amount = amount.toShort()
            )
        }
    }

    class InvalidModulatorException(modulator_value: Int): Exception(Integer.toHexString(modulator_value))
    class InvalidTransformOperation(value: Int): Exception("$value")
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
            val NO_CONTROLLER: Operator = Operator(
                index = 0,
                continuous = false,
                polar = false,
                direction = false,
                type = ModulatorSourceType.Linear
            )

            fun from_int(input: Int): Operator {
                return Operator(
                    index = input and 0x003f,
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

    //fun modulate(source_value: Float, mod_value: Int): Float {
    //    TODO()
    //}
}
