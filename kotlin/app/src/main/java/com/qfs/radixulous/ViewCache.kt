package com.qfs.radixulous

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.qfs.radixulous.opusmanager.BeatKey
import java.lang.Integer.max

class ViewCache {
    private var view_cache: MutableList<MutableList<Pair<LinearLayout, MutableList<Pair<View?, HashMap<List<Int>, View>>>>>> = mutableListOf()
    private var line_label_cache: MutableList<View> = mutableListOf()
    private var column_label_cache: MutableList<View> = mutableListOf()
    private var column_widths: MutableList<Int> = mutableListOf()
    private var focused_leafs: MutableSet<Pair<BeatKey, List<Int>>> = mutableSetOf()
    private var active_context_menu_view: View? = null

    fun get_column_width(x: Int): Int {
        var w = 0
        for (i in 0 until this.view_cache.size) {
            for (j in 0 until this.view_cache[i].size) {
                var line = this.getLine(i, j)
                var wrapper = line.getChildAt(x)
                wrapper.measure(0, 0)
                w = max(wrapper.measuredWidth, w)
            }
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

    fun get_all_leafs(beatkey: BeatKey, position: List<Int>): List<Pair<View, List<Int>>> {
        val output: MutableList<Pair<View, List<Int>>> = mutableListOf()
        if (beatkey.channel >= this.view_cache.size) {
            return output
        }
        if (beatkey.line_offset >= this.view_cache[beatkey.channel].size) {
            return output
        }
        if (beatkey.beat >= this.view_cache[beatkey.channel][beatkey.line_offset].second.size) {
            return output
        }

        for ((key_pos, view) in this.view_cache[beatkey.channel][beatkey.line_offset].second[beatkey.beat].second) {
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
            output.add(Pair(this.view_cache[beatkey.channel][beatkey.line_offset].second[beatkey.beat].first!!, position))
        }

        return output
    }

    fun setActiveContextMenu(view: View) {
        this.active_context_menu_view = view
    }

    fun getActiveContextMenu(): View? {
        return this.active_context_menu_view
    }

    fun cacheLine(view: LinearLayout, channel: Int, line_offset: Int) {
        while (channel > this.view_cache.size - 1) {
            this.view_cache.add(mutableListOf())
        }

        if (line_offset < this.view_cache[channel].size) {
            this.view_cache[channel].add(line_offset, Pair(view, mutableListOf()))
        } else {
            this.view_cache[channel].add(Pair(view, mutableListOf()))
        }
    }

    fun cacheTree(view: View, beatkey: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            if (beatkey.beat < this.view_cache[beatkey.channel][beatkey.line_offset].second.size) {
                this.view_cache[beatkey.channel][beatkey.line_offset].second.add(beatkey.beat, Pair(view, HashMap()))
            } else {
                this.view_cache[beatkey.channel][beatkey.line_offset].second.add(Pair(view, HashMap()))
            }
        } else {
            this.view_cache[beatkey.channel][beatkey.line_offset].second[beatkey.beat].second[position] = view
        }
    }

    fun getTreeView(beatkey: BeatKey, position: List<Int>): View? {
        return if (position.isEmpty()) {
            this.view_cache[beatkey.channel][beatkey.line_offset].second[beatkey.beat].first
        } else {
            this.view_cache[beatkey.channel][beatkey.line_offset].second[beatkey.beat].second[position]
        }
    }

    fun getLine(channel: Int, line: Int): LinearLayout {
        return this.view_cache[channel][line].first
    }

    fun getLines(): List<List<LinearLayout>> {
        var output: MutableList<MutableList<LinearLayout>> = mutableListOf()
        for (line in this.view_cache) {
            output.add(mutableListOf())
            for (pair in line) {
                output.last().add(pair.first)
            }
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

    fun detachLine(channel: Int, line_offset: Int) {
        val label = this.line_label_cache.removeLast()
        (label.parent as ViewGroup).removeView(label)

        val view = this.view_cache[channel].removeAt(line_offset).first
        (view.parent as ViewGroup).removeView(view)

        // Remove empty channel
        if (this.view_cache[channel].isEmpty()) {
            this.view_cache.removeAt(channel)
        }

        for (i in 0 until this.focused_leafs.size) {
            var (beatkey, position) = this.popFocusedLeaf()!!
            if (channel == beatkey.channel && line_offset == beatkey.line_offset) {
                continue
            } else if (channel < beatkey.channel || (channel == beatkey.channel && line_offset < beatkey.line_offset)) {
                this.addFocusedLeaf(beatkey, position)
            } else {
                if (beatkey.line_offset > 0) {
                    beatkey.line_offset -= 1
                } else if (beatkey.channel > 0) {
                    beatkey.channel -= 1
                    beatkey.line_offset = this.view_cache[channel].size - 1
                } else {
                    continue
                }
                this.addFocusedLeaf(beatkey, position)
            }
        }
    }

    fun removeBeatView(beatkey: BeatKey) {
        val line_cache = this.view_cache[beatkey.channel][beatkey.line_offset].second

        line_cache[beatkey.beat].second.clear()

        var (view, _) = line_cache.removeAt(beatkey.beat)

        if (view != null) {
            var parent = view.parent as View
            var grandparent = parent.parent as ViewGroup
            grandparent.removeView(parent)
        }

        //// Detach using line. we cache each leaf, but there is still a wrapper to deal with.
        //var line = this.getLine(y)
        //line.removeViewAt(x)
    }

    fun getTreeViewPosition(view: View): Pair<BeatKey, List<Int>>? {
        for (i in 0 until this.view_cache.size) {
            for (j in 0 until this.view_cache[i].size) {
                val line_cache = this.view_cache[i][j].second
                for (x in 0 until line_cache.size) {
                    if (view == line_cache[x].first) {
                        return Pair(BeatKey(i, j, x), listOf())
                    }

                    for (key in line_cache[x].second.keys) {
                        if (line_cache[x].second[key] == view) {
                            return Pair(BeatKey(i, j, x), key)
                        }
                    }
                }
            }
        }
        return null
    }

    fun popFocusedLeaf(): Pair<BeatKey, List<Int>>? {
        return if (this.focused_leafs.any()) {
            var output = this.focused_leafs.first()
            this.focused_leafs.remove(output)
            output
        } else {
            null
        }
    }

    fun addFocusedLeaf(beatkey: BeatKey, position: List<Int>) {
        this.focused_leafs.add(Pair(beatkey, position))
    }

    fun getFocusedLeafs(): MutableSet<Pair<BeatKey, List<Int>>> {
        return this.focused_leafs
    }

    fun getLineCount(): Int {
        return this.view_cache.size
    }
}
