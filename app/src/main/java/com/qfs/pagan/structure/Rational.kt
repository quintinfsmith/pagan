package com.qfs.pagan.structure

import kotlin.math.abs

/**
 *  Class to handle Rational Values
 */
data class Rational(var numerator: Int, var denominator: Int) {
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

    operator fun compareTo(other: Any): Int {
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
            else -> throw Exception()
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
}