package com.qfs.pagan

inline fun <reified T> MutableList<T>.swap_sections(first_index: Int, first_size: Int, second_index: Int, second_size: Int) {
    if (first_index == second_index) return

    val (lesser, larger) = if (first_index < second_index) {
        Pair(Pair(first_index, first_size), Pair(second_index, second_size))
    } else {
        Pair(Pair(second_index, second_size), Pair(first_index, first_size))
    }

    val larger_items = Array<T>(larger.second) {
        this.removeAt(larger.first)
    }
    val lesser_items = Array<T>(lesser.second) {
        this.removeAt(lesser.first)
    }

    for ((i, elm) in larger_items.enumerate()) {
        this.add(lesser.first + i, elm)
    }
    for ((i, elm) in lesser_items.enumerate()) {
        this.add(larger.first + i + (larger.second - lesser.second), elm)
    }
}