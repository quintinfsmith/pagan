package com.qfs.pagan

data class Rational(var n: Int, var d: Int) {
    override fun toString(): String {
        return "($n / $d)"
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
}
