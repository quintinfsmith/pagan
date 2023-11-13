package com.qfs.pagan

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.core.view.children
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.roundToInt
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellLayout(val column_layout: ColumnLayout, val y: Int): LinearLayout(column_layout.context) {
    class BeatKeyNotSet: Exception()
    init {
        this.isClickable = false
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        this.layoutParams.width = (column_layout.column_width_factor * resources.getDimension(R.dimen.base_leaf_width).roundToInt())
        val base_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()

        val beat_key = this.get_beat_key()
        val tree = this.get_beat_tree(beat_key)
        this.build(tree, this.get_editor_table().get_column_width(beat_key.beat) * base_leaf_width)
    }


    fun invalidate_all() {
        var view_stack = mutableListOf<View>(this)
        while (view_stack.isNotEmpty()) {
            var current_view = view_stack.removeAt(0)
            if (current_view is ViewGroup) {
                for (child in current_view.children) {
                    view_stack.add(child)
                }
            }
            current_view.postInvalidate()
            current_view.refreshDrawableState()
        }
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun get_editor_table(): EditorTable {
        return this.get_activity().findViewById(R.id.etEditorTable)
    }

    fun build(tree: OpusTree<OpusEvent>, max_width: Int) {
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                this.buildTreeView(tree[i], listOf(i), max_width.toFloat() / tree.size.toFloat())
            }
        } else {
            this.buildTreeView(tree, listOf(), max_width.toFloat())
        }
   }

   private fun buildTreeView(tree: OpusTree<OpusEvent>, position: List<Int>, new_width: Float) {
       if (tree.is_leaf()) {
           val tvLeaf = LeafButton(
               this.context,
               tree.get_event(),
               position,
               this.is_percussion()
           )

           this.addView(tvLeaf)

           (tvLeaf.layoutParams as LayoutParams).gravity = Gravity.CENTER
           (tvLeaf.layoutParams as LayoutParams).height = MATCH_PARENT
           (tvLeaf.layoutParams as LayoutParams).width = new_width.toInt()

           tvLeaf.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
       } else {
           val next_width = new_width / tree.size
           for (i in 0 until tree.size) {
               val new_position = position.toMutableList()
               new_position.add(i)
               this.buildTreeView(tree[i], new_position, next_width)
           }
       }
   }

    fun get_beat(): Int {
        return (this.parent as ColumnLayout).get_beat()
    }

    fun get_beat_key(): BeatKey {
        var opus_manager = this.get_opus_manager()
        var (channel, line_offset) = opus_manager.get_std_offset(this.y)
        return BeatKey(channel, line_offset, this.get_beat())
    }

    fun get_beat_tree(beat_key: BeatKey? = null): OpusTree<OpusEvent> {
        val opus_manager = this.get_opus_manager()
        return opus_manager.get_beat_tree(beat_key ?: this.get_beat_key())
    }

    fun is_percussion(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_percussion(this.get_beat_key().channel)
    }
}