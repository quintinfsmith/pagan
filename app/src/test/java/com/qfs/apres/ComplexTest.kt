package com.qfs.apres

import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class ComplexTest {
    @Test
    fun fft_test() {
        val sample_size = 2.0.pow(15).toInt()
        val frequency = 440.0
        val wave_length = sample_size / frequency
        val sample = Array<Complex>(sample_size) { i: Int ->
            Complex(sin((i.toDouble() * 2.0 * PI) / wave_length))
        }

        val transform = FFT(sample)

        assert(1F < transform[frequency.toInt()].imaginary)

        val untransform = IFFT(transform)
        val retransform = FFT(untransform)
        for (i in untransform.indices) {
            assertEquals(
                "$i fail",
                (1000 * sample[i].real).roundToInt(),
                (1000 * untransform[i].real).roundToInt()
            )
        }

    }
}