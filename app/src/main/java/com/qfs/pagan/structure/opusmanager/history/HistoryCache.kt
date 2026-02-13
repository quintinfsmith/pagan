/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.history

class HistoryCache {
    class HistoryNode(var token: HistoryToken, var args: List<Any>) {
        var children: MutableList<HistoryNode> = mutableListOf()
        var parent: HistoryNode? = null
    }
    private val _max_history_size = 100
    private var _future_lock = 0 // Applying Redo
    private var _reverse_lock = 0 // Applying Undo
    private var _history_lock = 0 // Generic Lock
    private var _history: MutableList<HistoryNode> = mutableListOf()
    private var _future: MutableList<HistoryNode> = mutableListOf()
    private var _working_node: HistoryNode? = null

    fun is_applying_redo(): Boolean {
        return this._future_lock != 0
    }

    fun is_applying_undo(): Boolean {
        return this._reverse_lock != 0
    }

    fun is_locked(): Boolean {
        return this._history_lock != 0
    }

    fun has_undoable_actions(): Boolean {
        return this._history.isNotEmpty()
    }
    fun has_redoable_actions(): Boolean {
        return this._future.isNotEmpty()
    }

    fun prepend_undoer(token: HistoryToken, args: List<Any>) {
        if (this.is_locked()) return
        val new_node = HistoryNode(token, args)

        if (this._working_node != null) {
            new_node.parent = this._working_node
            this._working_node!!.children.add(0, new_node)
        } else {
            if (this.is_applying_undo()) {
                this._future.add(0, new_node)
            } else {
                this._history.add(0, new_node)
            }
        }

        this._check_size()
    }

    fun append_undoer(token: HistoryToken, args: List<Any>) {
        if (this.is_locked()) return
        val new_node = HistoryNode(token, args)

        if (this._working_node != null) {
            new_node.parent = this._working_node
            this._working_node!!.children.add(new_node)
        } else {
            if (this.is_applying_undo()) {
                this._future.add(new_node)
            } else {
                this._history.add(new_node)
            }
        }

        this._check_size()
    }

    // Keep track of all history as one group
    fun <T> remember(callback: () -> T): T {
        return try {
            this._open_multi()
            val output = callback()
            this._close_multi()
            output
        } catch (e: Exception) {
            this._close_multi()
            throw e
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
        if (this.is_locked()) return

        val next_node = HistoryNode(HistoryToken.MULTI, listOf())

        if (this._working_node != null) {
            next_node.parent = this._working_node
            this._working_node!!.children.add(next_node)
        } else {
            if (this.is_applying_undo()) {
                this._future.add(next_node)
            } else {
                this._history.add(next_node)
            }
        }
        this._working_node = next_node
    }

    private fun _close_multi() {
        if (this.is_locked()) return

        if (this._working_node != null) {
            this._working_node = this._working_node!!.parent
        }
    }

    fun size(): Int {
        return this._history.size
    }

    private fun _check_size() {
        if (!this.is_applying_undo()) {
            while (this._history.size > this._max_history_size) {
                this._history.removeAt(0)
            }
            if (!this.is_applying_redo()) {
                this._future.clear()
            }
        }
    }

    fun clear() {
        this._history.clear()
        this._future.clear()
    }

    fun start_redo() {
        this._future_lock += 1
    }
    fun end_redo() {
        this._future_lock -= 1
    }
    fun start_undo() {
        this._reverse_lock += 1
    }
    fun end_undo() {
        this._reverse_lock -= 1
    }

    fun lock() {
        this._history_lock += 1
    }
    fun unlock() {
        this._history_lock -= 1
    }

    fun pop(): HistoryNode? {
        return if (this.is_applying_undo()) {
            if (this._history.isEmpty()) {
                null
            } else {
                this._history.removeAt(this._history.size - 1)
            }
        } else {
            if (this._future.isEmpty()) {
                null
            } else {
                this._future.removeAt(this._future.size - 1)
            }
        }
    }

    // Unused
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
        c._future = this._future.toMutableList()
        return c
    }
}
