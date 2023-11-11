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

class CellLayout(var view_holder: CellViewHolder): LinearLayout(view_holder.itemView.context) {
    class BeatKeyNotSet: Exception()
    init {
        this.isClickable = false
        (this.view_holder.itemView as ViewGroup).removeAllViews()
        (this.view_holder.itemView as ViewGroup).addView(this)
        this.build()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        this.layoutParams.width = (this.get_editor_table()
            .get_column_width(this.get_beat()) * resources.getDimension(R.dimen.base_leaf_width)
            .roundToInt())
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

    fun build() {
        val tree = this.get_beat_tree()
        val max_width = (this.get_editor_table().get_column_width(this.get_beat()) * resources.getDimension(R.dimen.base_leaf_width).roundToInt())

        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                this.buildTreeView(tree[i], listOf(i), 1F / tree.size.toFloat())
            }
        } else {
            this.buildTreeView(tree, listOf(), 1F)
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
           (tvLeaf.layoutParams as LayoutParams).weight = new_width
           (tvLeaf.layoutParams as LayoutParams).width = 0

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
        return this.get_beat_key().beat
    }

    fun get_beat_key(): BeatKey {
        return this.view_holder.beat_key ?: throw BeatKeyNotSet()
    }

    fun get_beat_tree(): OpusTree<OpusEvent> {
        val opus_manager = this.get_opus_manager()
        val beat_key = this.get_beat_key()
        return opus_manager.get_beat_tree(beat_key)
    }

    fun is_percussion(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_percussion(this.get_beat_key().channel)
    }
}