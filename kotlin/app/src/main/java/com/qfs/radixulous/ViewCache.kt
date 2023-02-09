package com.qfs.radixulous

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import java.lang.Integer.max

class ViewCache {
    private var view_cache: MutableList<Pair<LinearLayout, MutableList<Pair<View?, HashMap<List<Int>, View>>>>> = mutableListOf()
    private var line_label_cache: MutableList<View> = mutableListOf()
    private var column_label_cache: MutableList<View> = mutableListOf()
    private var column_widths: MutableList<Int> = mutableListOf()
    private var focused_leafs: MutableSet<Triple<Int, Int, List<Int>>> = mutableSetOf()
    private var active_context_menu_view: View? = null

    fun get_column_width(x: Int): Int {
        var w = 0
        for (y in 0 until this.view_cache.size) {
            var line = this.getLine(y)
            var wrapper = line.getChildAt(x)
            wrapper.measure(0,0)
            w = max(wrapper.measuredWidth, w)
        }
        return w
    }

    fun set_column_width(x: Int, new_width: Int) {
        this.column_widths[x] = max(new_width, this.column_widths[x])
    }

    fun add_column_width(x: Int) {
        this.column_widths.add(x, 0)
    }

    fun remove_column_width(x: Int) {
        this.column_widths.remove(x)
    }

    fun get_all_leafs(y: Int, x: Int, position: List<Int>): List<Pair<View, List<Int>>> {
        val output: MutableList<Pair<View, List<Int>>> = mutableListOf()
        if (y >= this.view_cache.size || x >= this.view_cache[y].second.size) {
            return output
        }
        for ((key_pos, view) in this.view_cache[y].second[x].second) {
            try {
                view as ViewGroup
                continue
            } catch (e: Exception) {
                // pass. leaves can't be view groups so this is just a check
            }
            if (position.size <= key_pos.size && key_pos.subList(0, position.size) == position) {
                output.add(Pair(view, key_pos))
            }
        }
        if (output.isEmpty() && position.isEmpty()) {
            output.add(Pair(this.view_cache[y].second[x].first!!, position))
        }

        return output
    }

    fun setActiveContextMenu(view: View) {
        this.active_context_menu_view = view
    }

    fun getActiveContextMenu(): View? {
        return this.active_context_menu_view
    }

    fun cacheLine(view: LinearLayout, y: Int) {
        if (y < this.view_cache.size) {
            this.view_cache.add(y, Pair(view, mutableListOf()))
        } else {
            this.view_cache.add(Pair(view, mutableListOf()))
        }
    }

    fun cacheTree(view: View, y: Int, x: Int, position: List<Int>) {
        if (position.isEmpty()) {
            if (x < this.view_cache[y].second.size) {
                this.view_cache[y].second.add(x, Pair(view, HashMap()))
            } else {
                this.view_cache[y].second.add(Pair(view, HashMap()))
            }
        } else {
            this.view_cache[y].second[x].second[position] = view
        }
    }

    fun getTreeView(y: Int, x: Int, position: List<Int>): View? {
        return if (position.isEmpty()) {
            this.view_cache[y].second[x].first
        } else {
            this.view_cache[y].second[x].second[position]
        }
    }

    fun getLine(y: Int): LinearLayout {
        return this.view_cache[y].first
    }

    fun getLines(): List<LinearLayout> {
        var output: MutableList<LinearLayout> = mutableListOf()
        for (pair in this.view_cache) {
            output.add(pair.first)
        }
        return output
    }

    fun addColumnLabel(view: View) {
        this.column_label_cache.add(view)
    }

    fun getColumnLabel(x: Int): View {
        return this.column_label_cache[x]
    }

    fun detachColumnLabel() {
        val label = this.column_label_cache.removeLast()
        (label.parent as ViewGroup).removeView(label)
    }

    fun addLineLabel(view: View) {
        this.line_label_cache.add(view)
    }

    fun getLineLabel(y: Int): View? {
        return if (y < this.line_label_cache.size) {
            this.line_label_cache[y]
        } else {
            null
        }
    }

    fun detachLine(y: Int) {
        val label = this.line_label_cache.removeAt(y)
        (label.parent as ViewGroup).removeView(label)

        val view = this.view_cache.removeAt(y).first
        (view.parent as ViewGroup).removeView(view)

        for (i in 0 until this.focused_leafs.size) {
            var (vy, vx, position) = this.popFocusedLeaf()!!
            if (vy < y) {
                this.addFocusedLeaf(vy, vx, position)
            } else if (vy > y) {
                this.addFocusedLeaf(vy - 1, vx, position)
            }
        }
    }

    fun removeBeatView(y: Int, x: Int) {
        val line_cache = this.view_cache[y].second

        // I think this is redundant. commenting out for now
        //for ((pos, view) in line_cache[x].second) {
        //    (view.parent as ViewGroup).removeView(view)
        //}

        line_cache[x].second.clear()

        line_cache.removeAt(x)
        // Detach using line. we cache each leaf, but there is still a wrapper to deal with.
        var line = this.getLine(y)
        line.removeViewAt(x)
    }

    fun getTreeViewYXPosition(view: View): Triple<Int, Int, List<Int>>? {
        for (y in 0 until this.view_cache.size) {
            val line_cache = this.view_cache[y].second
            for (x in 0 until line_cache.size) {
                if (view == line_cache[x].first) {
                    return Triple(y, x, listOf())
                }

                for (key in line_cache[x].second.keys) {
                    if (line_cache[x].second[key] == view) {
                        return Triple(y, x, key)
                    }
                }
            }
        }
        return null
    }

    fun popFocusedLeaf(): Triple<Int, Int, List<Int>>? {
        return if (this.focused_leafs.any()) {
            var output = this.focused_leafs.first()
            this.focused_leafs.remove(output)
            output
        } else {
            null
        }
    }

    fun addFocusedLeaf(y: Int, x: Int, position: List<Int>) {
        this.focused_leafs.add(Triple(y, x, position))
    }

    fun getFocusedLeafs(): MutableSet<Triple<Int, Int, List<Int>>> {
        return this.focused_leafs
    }

    fun getLineCount(): Int {
        return this.view_cache.size
    }
}
