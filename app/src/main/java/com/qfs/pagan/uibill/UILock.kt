package com.qfs.pagan.uibill

class UILock {
    var level = 0
    fun lock() {
        this.level += 1
    }

    fun unlock() {
        this.level -= 1
    }

    fun get_level(): Int {
        return this.level
    }

    fun is_locked(): Boolean {
        return this.level > 0
    }
}
