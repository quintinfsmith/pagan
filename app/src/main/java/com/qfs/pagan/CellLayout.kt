package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellLayout(var viewHolder: CellRecyclerViewHolder): LinearLayout(viewHolder.itemView.context) {
    init {
        (this.viewHolder.itemView as ViewGroup).removeAllViews()
        (this.viewHolder.itemView as ViewGroup).addView(this)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.width = MATCH_PARENT
        this.build()
    }
    fun get_activity(): MainActivity {
        return this.viewHolder.get_activity()
    }

    fun get_beat_tree(): OpusTree<OpusEvent> {
        return this.viewHolder.get_beat_tree()
    }

    fun get_beat_key(): BeatKey {
        return this.viewHolder.get_beat_key()
    }

    fun is_percussion(): Boolean {
        return this.viewHolder.is_percussion()
    }

    fun get_opus_manager(): OpusManager {
        return this.viewHolder.get_opus_manager()
    }

    fun build() {
        val tree = this.get_beat_tree()
        val max_weight = tree.get_max_child_weight()
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                this.buildTreeView(tree[i], listOf(i), max_weight)
            }
        } else {
            this.buildTreeView(tree, listOf(), max_weight)
        }
   }

    fun get_editor_table(): EditorTable {
        return this.viewHolder.get_adapter().get_column_adapter().get_editor_table()
    }

   private fun buildTreeView(tree: OpusTree<OpusEvent>, position: List<Int>, weight: Int) {
       if (tree.is_leaf()) {
           val tvLeaf = LeafButton(
               this.context,
               this.get_activity(),
               tree.get_event(),
               position,
               this.is_percussion()
           )

           this.addView(tvLeaf)

           (tvLeaf.layoutParams as LinearLayout.LayoutParams).apply {
               gravity = Gravity.CENTER
               height = resources.getDimension(R.dimen.line_height).toInt()
               width = 0
               this.weight = weight.toFloat()
           }

       } else {
           val new_weight = weight / tree.size
           for (i in 0 until tree.size) {
               val new_position = position.toMutableList()
               new_position.add(i)
               this.buildTreeView(tree[i], new_position, new_weight)
           }
       }
   }
}