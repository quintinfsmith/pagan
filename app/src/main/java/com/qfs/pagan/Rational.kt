package com.qfs.pagan
import com.qfs.pagan.structure.greatest_common_denominator
import kotlin.math.abs

data class Rational(var n: Int, var d: Int) {
    init {
        this.reduce()
    }
    override fun toString(): String {
        this.reduce()
        return "(${this.n} / ${this.d})"
    }

    fun toFloat(): Float {
        return this.n.toFloat() / this.d.toFloat()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Rational -> (this.n * other.d) == (other.n * this.d)
            is Int -> this == Rational(other, 1)
            else -> false
        }
    }

    operator fun compareTo(other: Any): Int {
        return when (other) {
            is Rational -> (this.n * other.d) - (other.n * this.d)
            is Int -> this.compareTo(Rational(other, 1))
            else -> throw Exception()
        }
    }

    operator fun plus(other: Any): Rational {
        return when (other) {
            is Rational -> {
                Rational(
                    (this.n * other.d) + (other.n * this.d),
                    (this.d * other.d)
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
                    (this.n * other.d) - (other.n * this.d),
                    (this.d * other.d)
                )
            }
            is Int -> this - Rational(other, 1)
            else -> throw Exception()
        }
    }

    operator fun times(other: Any): Rational {
        return when (other) {
            is Rational -> Rational(this.n * other.n, this.d * other.d)
            is Int -> this * Rational(other, 1)
            else -> throw Exception()
        }
    }

    operator fun div(other: Any): Rational {
        return when (other) {
            is Rational -> this * Rational(other.d, other.n)
            is Int -> this * Rational(1, other)
            else -> throw Exception()
        }
    }

    fun reduce() {
        if (this.n == 0) {
            this.n = 0
            this.d = 1
        } else {
            val gcd = try {
                greatest_common_denominator(abs(this.n), abs(this.d))
            } catch (e: Exception) {
                return
            }

            this.n /= gcd
            this.d /= gcd

        }
    }
}
