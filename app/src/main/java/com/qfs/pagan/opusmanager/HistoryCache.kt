package com.qfs.pagan.opusmanager

class HistoryCache {
    class HistoryError(val e: Exception, val failed_node: HistoryNode?): Exception()
    class HistoryNode(var token: HistoryToken, var args: List<Any>) {
        var children: MutableList<HistoryNode> = mutableListOf()
        var parent: HistoryNode? = null
    }
    private val _max_history_size = 100
    private var _history_lock = 0
    private var _history: MutableList<HistoryNode> = mutableListOf()
    private var _working_node: HistoryNode? = null

    fun isLocked(): Boolean {
        return this._history_lock != 0
    }

    fun isEmpty(): Boolean {
        return this._history.isEmpty()
    }

    fun append_undoer(token: HistoryToken, args: List<Any>) {
        if (this.isLocked()) {
            return
        }
        val new_node = HistoryNode(token, args)

        if (this._working_node != null) {
            new_node.parent = this._working_node
            this._working_node!!.children.add(new_node)
        } else {
            this._history.add(new_node)
        }

        this._check_size()
    }

    // Keep track of all history as one group
    fun <T> remember(callback: () -> T): T {
        this._open_multi()
        try {
            val output = callback()
            this._close_multi()
            return output
        } catch (e: Exception) {
            throw HistoryError(e, this.cancel_multi())
        }
    }

    // Run a callback with logging history
    fun <T> forget(callback: () -> T): T {
        this.lock()
        try {
            val output = callback()
            this.unlock()
            return output
        } catch (e: Exception) {
            this.unlock()
            throw e
        }
    }

    private fun _open_multi() {
        if (this.isLocked()) {
            return
        }

        val next_node = HistoryNode(HistoryToken.MULTI, listOf())

        if (this._working_node != null) {
            next_node.parent = this._working_node
            this._working_node!!.children.add(next_node)
        } else {
            this._history.add(next_node)
        }
        this._working_node = next_node
    }

    private fun _close_multi() {
        if (this.isLocked()) {
            return
        }

        if (this._working_node != null) {
            this._working_node = this._working_node!!.parent
        }
    }

    private fun cancel_multi(): HistoryNode? {
        if (this.isLocked()) {
            return null
        }
        this._close_multi()
        return if (this._working_node != null) {
            this._working_node!!.children.removeLast()
        } else {
            this._history.removeLast()
        }
    }
    private fun _check_size() {
        while (this._history.size > this._max_history_size) {
            this._history.removeFirst()
        }
    }


    fun clear() {
        this._history.clear()
    }

    fun lock() {
        this._history_lock += 1
    }

    fun unlock() {
        this._history_lock -= 1
    }

    fun pop(): HistoryNode? {
        return if (this._history.isEmpty()) {
            null
        } else {
            this._history.removeLast()
        }
    }

    fun peek(): HistoryNode? {
        return if (this._history.isEmpty()) {
            null
        } else {
            this._history.last()
        }
    }

    fun copy(): HistoryCache {
        val c = HistoryCache()
        c._history = this._history.toMutableList()
        c._working_node = this._working_node
        return c
    }
}
