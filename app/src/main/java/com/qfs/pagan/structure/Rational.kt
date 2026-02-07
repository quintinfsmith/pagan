/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure

import kotlin.math.abs

/**
 *  Class to handle Rational Values
 */
data class Rational(var numerator: Int, var denominator: Int) : Comparable<Any> {
    init {
        this.reduce()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Rational -> (this.numerator * other.denominator) == (other.numerator * this.denominator)
            is Int -> this == Rational(other, 1)
            is Float -> other == this.toFloat()
            is Double -> other == this.toDouble()
            else -> false
        }
    }

    override operator fun compareTo(other: Any): Int {
        return when (other) {
            is Rational -> (this.numerator * other.denominator) - (other.numerator * this.denominator)
            is Int -> this.compareTo(Rational(other, 1))
            is Float -> this.toFloat().compareTo(other)
            is Double -> this.toDouble().compareTo(other)
            else -> throw Exception()
        }
    }

    operator fun plus(other: Any): Rational {
        return when (other) {
            is Rational -> {
                Rational(
                    (this.numerator * other.denominator) + (other.numerator * this.denominator),
                    (this.denominator * other.denominator)
                )
            }
            is Int -> this + Rational(other, 1)
            else -> throw Exception()
        }
    }

    operator fun minus(other: Any): Rational {
        return when (other) {
            is Rational -> {
                Rational(
                    (this.numerator * other.denominator) - (other.numerator * this.denominator),
                    (this.denominator * other.denominator)
                )
            }
            is Int -> this - Rational(other, 1)
            else -> throw Exception()
        }
    }

    operator fun times(other: Any): Rational {
        return when (other) {
            is Rational -> Rational(this.numerator * other.numerator, this.denominator * other.denominator)
            is Int -> this * Rational(other, 1)
            else -> throw Exception()
        }
    }

    operator fun div(other: Any): Rational {
        return when (other) {
            is Rational -> this * Rational(other.denominator, other.numerator)
            is Int -> this * Rational(1, other)
            else -> throw Exception("${other.javaClass}")
        }
    }

    override fun toString(): String {
        return "(${this.numerator} / ${this.denominator})"
    }

    fun toFloat(): Float {
        return this.numerator.toFloat() / this.denominator.toFloat()
    }

    fun toDouble(): Double {
        return this.numerator.toDouble() / this.denominator.toDouble()
    }

    /**
     * Convert the numerator and denominator to their lowest equivalent form.
     */
    fun reduce() {
        if (this.numerator == 0) {
            this.numerator = 0
            this.denominator = 1
        } else {
            val gcd = try {
                greatest_common_denominator(abs(this.numerator), abs(this.denominator))
            } catch (e: Exception) {
                return
            }
            this.numerator /= gcd
            this.denominator /= gcd
        }
    }


    fun toInt(): Int {
        return this.numerator / this.denominator
    }
}

operator fun Int.plus(b: Rational) = Rational(this, 1) + b
operator fun Int.minus(b: Rational) = Rational(this, 1) - b
operator fun Int.times(b: Rational) = Rational(this, 1) * b
fun List<Rational>.sum() = {
    var output = Rational(0, 1)
    for (x in this) {
        output += x
    }
    output
}
