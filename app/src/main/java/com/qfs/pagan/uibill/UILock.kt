package com.qfs.pagan.uibill

import kotlin.math.max

class UILock {
    companion object {
        const val FULL = 2
        const val PARTIAL = 1
        const val NONE = 0
    }

    var flag = UILock.NONE
    var level = 0
    fun lock_partial() {
        this.flag = max(this.flag, UILock.PARTIAL)
        this.level += 1
    }

    fun lock_full() {
        this.flag = max(this.flag, UILock.FULL)
        this.level += 1
    }

    fun unlock() {
        this.level -= 1
        if (this.level == 0) {
            this.flag = UILock.NONE
        }
    }

    fun get_level(): Int {
        return this.level
    }

    fun is_locked(): Boolean {
        return this.level > 0
    }

    fun is_full_locked(): Boolean {
        return this.flag == UILock.FULL
    }
}
