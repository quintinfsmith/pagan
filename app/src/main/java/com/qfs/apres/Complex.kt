package com.qfs.apres

import kotlin.math.*

data class Complex(var real: Double, var imaginary: Double = 0.0) {
    companion object {
        fun arg(value: Complex): Double {
            return atan(value.imaginary / value.real)
        }
    }
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
        var divisor = (other.real.pow(2.0) + other.imaginary.pow(2.0))
        return Complex(
            ((this.real * other.real) + (this.imaginary + other.imaginary)) / divisor,
            (this.imaginary * other.real) - (this.real * other.imaginary) / divisor
        )
    }

    operator fun div(other: Double): Complex {
        return this / Complex(other, 0.0)
    }

    fun pow(other: Complex): Complex {
        val arg_value = arg(this)
        var common_factor = (this.real.pow(2.0) + this.imaginary.pow(2.0)).pow(other.real / 2.0) * exp((0.0 - other.imaginary) * arg_value)
        val inner = (other.real * arg_value) + ((other.imaginary / 2.0) * ln(this.real.pow(2.0) + this.imaginary.pow(2.0)))
        return Complex(
            cos(inner),
            sin(inner)
        )
    }

    fun pow(other: Double): Complex {
        return this.pow(Complex(other, 0.0))
    }

    fun pow(other: Int): Complex {
        return this.pow(Complex(other.toDouble(), 0.0))
    }

}

fun IFFT(sample: Array<Complex>): Array<Complex> {
    return FFT(sample, true)
}

fun FFT(sample: Array<Complex>, inverse: Boolean = false): Array<Complex> {
    if (sample.size == 1) {
        return sample
    }

    val omega = if (inverse) {
        Complex(Math.E, 0.0).pow(Complex(0.0, (-2.0 * Math.PI) / sample.size.toDouble())) / sample.size.toDouble()
    } else {
        Complex(Math.E, 0.0).pow(Complex(0.0, (2.0 * Math.PI) / sample.size.toDouble()))
    }
    val result_evens = FFT(Array<Complex>(sample.size / 2) { i: Int ->
        sample[i * 2]
    }, inverse)
    val result_odds = FFT(Array<Complex>(sample.size / 2) { i: Int ->
        sample[(i * 2) + 1]
    }, inverse)
    val output = Array<Complex>(sample.size) { i: Int ->
        val x = if (i < sample.size / 2) {
            i
        } else {
            i - (sample.size / 2)
        }
        result_evens[x] + (omega.pow(x) * result_odds[x])
    }

    return output
}
