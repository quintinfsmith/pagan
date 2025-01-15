package com.qfs.apres

import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class ComplexTest {
    @Test
    fun fft_test() {
        val sample_size = 2.0.pow(15).toInt()
        val frequency = 440F
        val wave_length = sample_size / frequency
        val PI_FLOAT = PI.toFloat()
        val sample = Array<Complex>(sample_size) { i: Int ->
            Complex(10 * sin((i.toFloat() * 2F * PI_FLOAT) / wave_length), 0F)
        }

        val transform = FFT(sample)
        println("${transform[frequency.toInt()]} ....")
        assert(1F < transform[frequency.toInt()].real)

        val untransform = IFFT(transform)
        for (i in untransform.indices) {
            assertEquals(
                "$i fail",
                sample[i],
                untransform[i]
            )
        }

    }
}