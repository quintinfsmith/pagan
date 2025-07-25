package com.qfs.apres.soundfont2

import kotlin.math.max
import kotlin.math.min

class PseudoArray<T>(var size: Int) {
    enum class OverlapBehavior {
        OverWrite,
        Merge,
        Ignore
    }
    var array_sections = HashMap<Int, List<T>>()

    operator fun get(i: Int): T? {
        for ((index, chunk) in this.array_sections) {
            if (i >= index && i < index + chunk.size) {
                return chunk[i - index]
            }
        }
        return null
    }

    operator fun set(i: Int, value: T) {
        this.place_section(i, listOf(value))
    }

    fun place_section(index: Int, section: List<T>, overlap_function: ((T, T) -> T)? = null) {
        val overlapping_indices = this.get_overlapping_sections(index, section.size)

        val first_index = min(overlapping_indices.min(), index)
        var last_index = index + section.size
        for (i in overlapping_indices) {
            last_index = max(i + this.array_sections[i]!!.size, last_index)
        }

        val new_section = MutableList<T?>(last_index - first_index) { null }
        for (x in overlapping_indices) {
            val offset = x - first_index
            for (i in 0 until this.array_sections[x]!!.size) {
                new_section[i + offset] = this.array_sections[x]!![i]
            }
            this.array_sections.remove(x)
        }

        for (i in 0 until section.size) {
            val elm = section[i]
            if (overlap_function == null || new_section[index - first_index] == null) {
                new_section[index - first_index] = elm
            } else {
                new_section[index - first_index] = overlap_function(new_section[index - first_index]!!, elm)
            }
        }

        // new_section can be assumed to have been fully filled in
        this.array_sections[first_index] = List<T>(new_section.size) { i: Int ->
            new_section[i]!!
        }
    }

    fun get_overlapping_sections(index: Int, size: Int): List<Int> {
        val incoming_f = index + size
        val output = mutableListOf<Int>()
        val sorted_indices = this.array_sections.keys.sorted()

        for (i in sorted_indices) {
            val section = this.array_sections[i]!!
            val working_f = i + section.size
            if (index >= i && index <= working_f) {
                output.add(i)
            } else if (i >= index && i <= incoming_f) {
                output.add(i)
            }
        }

        return output
    }

    fun remove_section(index: Int, size: Int) {
        val overlapping_indices = this.get_overlapping_sections(index, size)
        val end = index + size

        for (working_index in overlapping_indices) {
            val section = this.array_sections[working_index]!!
            if (working_index >= index && working_index + section.size <= index + size) {
                this.array_sections.remove(working_index)
            } else if (working_index >= index) {
                this.array_sections.remove(working_index)
                this.array_sections[end] = section.subList(working_index + section.size - end, section.size)
            } else {
                this.array_sections[working_index] = section.subList(0, index - working_index)
            }
        }
    }

    fun merge(other: PseudoArray<T>, operation: ((T, T) -> T)? = null) {
        for ((index, section) in other.array_sections) {
            this.place_section(index, section, operation)
        }
    }

    fun sub_section(index: Int, size: Int): PseudoArray<T> {
        val overlapping_indices = this.get_overlapping_sections(index, size)
        val end = index + size
        val output = PseudoArray<T>(size)

        for (working_index in overlapping_indices) {
            val section = this.array_sections[working_index]!!
            if (working_index >= index && working_index + section.size <= index + size) {
                output.place_section(working_index - index, section.toList())
            } else if (working_index >= index) {
                output.place_section(working_index - index, section.subList(working_index + section.size - end, section.size))
            } else {
                output.place_section(0, section.subList(0, index - working_index))
            }
        }

        return output
    }
}