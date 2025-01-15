package com.qfs.apres

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class Complex(var real: Float, var imaginary: Float = 0F) {
    operator fun plus(other: Complex): Complex {
        return Complex(
            (this.real + other.real),
            (this.imaginary + other.imaginary)
        )
    }

    operator fun minus(other: Complex): Complex {
        return Complex(
            (this.real - other.real),
            (this.imaginary - other.imaginary)
        )
    }

    operator fun times(other: Complex): Complex {
        return Complex(
            (this.real * other.real) - (this.imaginary * other.imaginary),
            (this.real * other.imaginary) + (this.imaginary * other.real)
        )
    }

    operator fun div(other: Complex): Complex {
        var divisor = (other.real.pow(2F) + other.imaginary.pow(2F))
        return Complex(
            ((this.real * other.real) + (this.imaginary * other.imaginary)) / divisor,
            (this.imaginary * other.real) - (this.real * other.imaginary) / divisor
        )
    }

    operator fun div(other: Float): Complex {
        return this / Complex(other, 0F)
    }
}

fun IFFT(sample: Array<Complex>): Array<Complex> {
    return FFT(sample, true)
}

fun FFT(sample: Array<Float>, inverse: Boolean = false): Array<Complex> {
    return FFT(
        Array<Complex>(sample.size) { i: Int ->
            Complex(sample[i], 0F)
        },
        inverse
    )
}

fun FFT(sample: Array<Complex>, inverse: Boolean = false): Array<Complex> {
    if (sample.size == 1) {
        return sample
    }

    val twiddle_factors = Array(sample.size) { i: Int ->
        val v = (-2F * PI.toFloat() * i.toFloat()) / sample.size.toFloat()
        Complex(cos(v), sin(v))
    }

    val half_size = sample.size / 2
    val result_evens = FFT(Array(half_size) { i: Int -> sample[i * 2] }, inverse)
    val result_odds = FFT(Array(half_size) { i: Int -> sample[(i * 2) + 1] }, inverse)

    return Array(sample.size) { i: Int ->
        val x = i % (sample.size / 2)
        result_evens[x] + (twiddle_factors[i] * result_odds[x])
    }
}
