package com.qfs.pagan

import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.roundToInt
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellLayout(var viewHolder: CellRecyclerViewHolder): LinearLayout(viewHolder.itemView.context) {
    init {
        var item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)
        this.layoutParams.width = WRAP_CONTENT
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
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
    fun get_beat(): Int {
        return this.viewHolder.get_beat()
    }

    fun is_percussion(): Boolean {
        return this.viewHolder.is_percussion()
    }

    fun get_opus_manager(): OpusManager {
        return this.viewHolder.get_opus_manager()
    }

    fun build() {
        val tree = this.get_beat_tree()
        val max_width = (this.get_editor_table().get_column_width(this.get_beat()) * resources.getDimension(R.dimen.base_leaf_width).roundToInt())
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                this.buildTreeView(tree[i], listOf(i), max_width / tree.size)
            }
        } else {
            this.buildTreeView(tree, listOf(), max_width)
        }
   }

    fun get_editor_table(): EditorTable {
        return this.viewHolder.get_adapter().get_column_adapter().get_editor_table()
    }

   private fun buildTreeView(tree: OpusTree<OpusEvent>, position: List<Int>, new_width: Int) {
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
               height = MATCH_PARENT
               width = new_width
           }
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
}