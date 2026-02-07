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
import kotlin.math.max
import kotlin.math.min

fun greatest_common_denominator(first: Int, second: Int): Int {
    if (first == 0 || second == 0) {
        throw Exception("Can't gcd $first and $second")
    }
    var tmp: Int
    var a = max(first, second)
    var b = min(first, second)
    return if (b > 0) {
        while ((a % b) > 0) {
            tmp = a % b
            a = b
            b = tmp
        }
        b
    } else {
        a
    }
}

fun get_prime_factors(n: Int): List<Int> {
    val primes: MutableList<Int> = mutableListOf()
    for (i in 2 until (n / 2)) {
        var is_prime = true
        for (p in primes) {
            if (i % p == 0) {
                is_prime = false
                break
            }
        }
        if (is_prime) {
            primes.add(i)
        }
    }


    val factors: MutableList<Int> = mutableListOf()
    for (p in primes) {
        if (p > n / 2) {
            break
        } else if (n % p == 0) {
            factors.add(p)
        }
    }
    // No Primes found, n is prime
    if (factors.isEmpty()) {
        factors.add(n)
    }

    return factors
}

fun lowest_common_multiple(number_list: List<Int>): Int {
    val prime_factors: Array<List<Int>> = Array(number_list.size) { i ->
        get_prime_factors(number_list[i])
    }

    val common_factor_map: HashMap<Int, Int> = HashMap()
    for (factors in prime_factors) {
        for (factor in factors) {
            if (! common_factor_map.containsKey(factor)) {
                common_factor_map[factor] = 0
            }
            val current = common_factor_map[factor]!!
            common_factor_map[factor] = max(current, factors.count { e -> e == factor })
        }
    }

    var output = 1
    for (key in common_factor_map.keys) {
        output *= key * common_factor_map[key]!!
    }

    return output
}

