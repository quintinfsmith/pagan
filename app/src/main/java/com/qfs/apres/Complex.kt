/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class Complex(var real: Double, var imaginary: Double = 0.0) {
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
            ((this.real * other.real) + (this.imaginary * other.imaginary)) / divisor,
            ((this.imaginary * other.real) - (this.real * other.imaginary)) / divisor
        )
    }

    operator fun div(other: Double): Complex {
        return this / Complex(other)
    }

    override fun equals(other: Any?): Boolean {
        return other is Complex && this.real == other.real && this.imaginary == other.imaginary
    }
}

fun IFFT(sample: Array<Complex>): Array<Complex> {
    return FFT(sample, true)
}

fun FFT(sample: Array<Double>, inverse: Boolean = false): Array<Complex> {
    return FFT(
        Array<Complex>(sample.size) { i: Int ->
            Complex(sample[i])
        },
        inverse
    )
}

fun FFT(sample: Array<Complex>, inverse: Boolean = false): Array<Complex> {
    if (sample.size == 1) {
        return sample
    }

    val half_size = sample.size / 2

    val inv_adj = if (inverse) -1.0 else 1.0
    val twiddle_factors = Array(half_size) { i: Int ->
        var v = (2.0 * PI * i.toDouble()) / sample.size.toDouble() * inv_adj
        Complex(cos(v), sin(v))
    }

    val result_evens = FFT(Array(half_size) { i: Int -> sample[i * 2] }, inverse)
    val result_odds = FFT(Array(half_size) { i: Int -> sample[(i * 2) + 1] }, inverse)

    return Array(sample.size) { i: Int ->
        val x = i % half_size

        val v = if (i < half_size) {
            result_evens[x] + (twiddle_factors[x] * result_odds[x])
        } else {
            result_evens[x] - (twiddle_factors[x] * result_odds[x])
        }

        if (inverse) {
            v / 2.0
        } else {
            v
        }
    }
}
