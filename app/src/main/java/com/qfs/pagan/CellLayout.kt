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
import com.qfs.pagan.OpusLayerInterface as OpusManager

class CellLayout(private val _column_layout: ColumnLayout, private val _y: Int): LinearLayout(_column_layout.context) {
    init {
        this.isClickable = false
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        val width = (this._column_layout.column_width_factor * resources.getDimension(R.dimen.base_leaf_width).roundToInt())
        this.layoutParams.width = width

        val beat_key = this.get_beat_key()
        val tree = this.get_beat_tree(beat_key)

        this.removeAllViews()
        this.buildTreeView(tree, listOf(), listOf())
    }

    fun invalidate_all() {
        val view_stack = mutableListOf<View>(this)
        while (view_stack.isNotEmpty()) {
            val current_view = view_stack.removeAt(0)
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

   private fun buildTreeView(tree: OpusTree<OpusEvent>, position: List<Int>, divisions: List<Int>) {
       if (tree.is_leaf()) {
           val tvLeaf = LeafButton(
               this.context,
               this.get_opus_manager().tuning_map.size,
               tree.get_event(),
               position,
               this.is_percussion()
           )

           this.addView(tvLeaf)

           (tvLeaf.layoutParams as LayoutParams).gravity = Gravity.CENTER
           (tvLeaf.layoutParams as LayoutParams).height = MATCH_PARENT
           var new_width_factor = this._column_layout.column_width_factor.toFloat()
           for (d in divisions) {
               new_width_factor /= d.toFloat()
           }

           (tvLeaf.layoutParams as LayoutParams).weight = new_width_factor
           (tvLeaf.layoutParams as LayoutParams).width = 0
           val base_leaf_width = resources.getDimension(R.dimen.base_leaf_width)
           tvLeaf.minimumWidth = base_leaf_width.roundToInt()
       } else {
           val new_divisions = divisions.toMutableList()
           new_divisions.add(tree.size)
           for (i in 0 until tree.size) {
               val new_position = position.toMutableList()
               new_position.add(i)
               this.buildTreeView(
                   tree[i],
                   new_position,
                   new_divisions
               )
           }
       }
   }

    private fun _get_beat(): Int {
        return (this.parent as ColumnLayout).get_beat()
    }

    fun get_beat_key(): BeatKey {
        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = opus_manager.get_std_offset(this._y)
        return BeatKey(channel, line_offset, this._get_beat())
    }

    fun get_beat_tree(beat_key: BeatKey? = null): OpusTree<OpusEvent> {
        val opus_manager = this.get_opus_manager()
        return opus_manager.get_tree(beat_key ?: this.get_beat_key())
    }

    fun is_percussion(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_percussion(this.get_beat_key().channel)
    }
}