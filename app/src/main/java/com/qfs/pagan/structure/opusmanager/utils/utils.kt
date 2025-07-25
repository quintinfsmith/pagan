package com.qfs.pagan.structure.opusmanager.utils

inline fun <reified T> checked_cast(value: Any): T {
    if (value is T) {
        return value
    }  else {
        throw ClassCastException()
    }
}