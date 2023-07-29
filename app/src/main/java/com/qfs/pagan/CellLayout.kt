package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellLayout(context: Context): LinearLayout(context) {
    var viewHolder: CellViewHolder? = null

    fun get_activity(): MainActivity {
        return viewHolder!!.get_activity()
    }

    fun get_beat_tree(): OpusTree<OpusEvent> {
        return this.viewHolder!!.get_beat_tree()
    }

    fun get_beat_key(): BeatKey {
        return this.viewHolder!!.get_beat_key()
    }

    fun is_percussion(): Boolean {
        return this.viewHolder!!.is_percussion()
    }

    fun get_opus_manager(): OpusManager {
        return this.viewHolder!!.get_opus_manager()
    }

    fun build() {
        this.removeAllViews()

        val tree = this.get_beat_tree()
        val top_node = PositionNode()
        val max_weight = tree.get_max_child_weight()
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                val next_node = PositionNode(i)
                next_node.previous = top_node
                this.buildTreeView(tree[i], next_node, max_weight)
            }
        } else {
            this.buildTreeView(tree, top_node, max_weight)
        }
   }

   private fun buildTreeView(tree: OpusTree<OpusEvent>, position_node: PositionNode, weight: Int) {
       val position = position_node.to_list()

       if (tree.is_leaf()) {
           val opus_manager = this.get_opus_manager()
           val beat_key = this.get_beat_key()
           val tvLeaf = LeafButton(
               this.context,
               this.get_activity(),
               tree.get_event(),
               position_node,
               this.is_percussion()
           )

           // Apply States ...
           if (tree.is_event()) {
               val abs_value = opus_manager.get_absolute_value(beat_key, position)
               tvLeaf.set_invalid(abs_value == null || abs_value < 0)
           }

           if (opus_manager.is_networked(beat_key)) {
               tvLeaf.set_linked(true)
           }

           this.apply_cursor_focus(tvLeaf, position)
           // ... Done Applying states

           //tvLeaf.setOnClickListener {
           //    this.interact_leafView_click(tvLeaf)
           //}

           //tvLeaf.setOnFocusChangeListener { _, is_focused: Boolean ->
           //    if (is_focused) {
           //        this.interact_leafView_click(tvLeaf)
           //    }
           //}

           //tvLeaf.setOnLongClickListener {
           //    this.interact_leafView_doubletap(tvLeaf)
           //    true
           //}

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
               val new_node = PositionNode(i)
               new_node.previous = position_node
               this.buildTreeView(tree[i], new_node, new_weight)
           }
       }
   }

    private fun apply_cursor_focus(leaf_button: LeafButton, position: List<Int>) {
        val opus_manager = this.get_opus_manager()
        val beat_key = this.get_beat_key()
        when (opus_manager.cursor.mode) {
            Cursor.CursorMode.Row -> {
                if (beat_key.channel == opus_manager.cursor.channel && beat_key.line_offset == opus_manager.cursor.line_offset) {
                    leaf_button.set_focused(true)
                }
            }
            Cursor.CursorMode.Column -> {
                if (beat_key.beat == opus_manager.cursor.beat) {
                    leaf_button.set_focused(true)
                }
            }
            Cursor.CursorMode.Single -> {
                val linked_beats = opus_manager.get_all_linked(beat_key)
                if (linked_beats.contains(opus_manager.cursor.get_beatkey()) && position == opus_manager.cursor.get_position()) {
                    leaf_button.set_focused(true)
                }
            }
            Cursor.CursorMode.Range -> {
                val (from_key, to_key) = opus_manager.cursor.range!!
                val vert_ok = if (beat_key.channel > from_key.channel && beat_key.channel < to_key.channel) {
                    true
                } else if (from_key.channel == to_key.channel && beat_key.channel == from_key.channel) {
                    beat_key.line_offset >= from_key.line_offset && beat_key.line_offset <= to_key.line_offset
                } else if (beat_key.channel == from_key.channel) {
                    beat_key.line_offset >= from_key.line_offset
                } else if (beat_key.channel == to_key.channel) {
                    beat_key.line_offset <= to_key.line_offset
                } else {
                    false
                }

                leaf_button.set_focused(vert_ok && beat_key.beat in (from_key.beat .. to_key.beat))
            }
            else -> { }
        }
    }
}