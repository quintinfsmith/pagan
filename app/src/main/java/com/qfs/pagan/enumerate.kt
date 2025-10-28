package com.qfs.pagan

fun <T> Array<T>.enumerate():  Array<Pair<Int, T>> {
    return Array(this.size) { i ->
        Pair(i, this[i])
    }
}

fun <T> List<T>.enumerate():  Array<Pair<Int, T>> {
    return Array(this.size) { i ->
        Pair(i, this[i])
    }
}
