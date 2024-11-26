package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusPanEvent
import com.qfs.pagan.opusmanager.OpusReverbEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.roundToInt

abstract class LeafButtonCtl(
    context: Context,
    private var _event: OpusControlEvent?,
    var position: List<Int>,
    var control_level: CtlLineLevel,
    var control_type: ControlEventType
) : LeafButton(ContextThemeWrapper(context, R.style.ctl_leaf)) {
    init {
        this.minimumHeight = resources.getDimension(R.dimen.ctl_line_height).roundToInt()
        this.set_text()
    }
    abstract fun is_selected(): Boolean
    abstract fun is_secondary_selected(): Boolean
    abstract fun get_controller(): ActiveController<OpusControlEvent>

    override fun get_tint_list(): IntArray {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map

        return intArrayOf(
            color_map[ColorMap.Palette.LeafInvalid],
            color_map[ColorMap.Palette.LeafInvalidSelected],
            color_map[ColorMap.Palette.SecondarySelectionInvalid], // B

            color_map[ColorMap.Palette.CtlLeaf],
            color_map[ColorMap.Palette.CtlLeafSpill], // B

            // Primary
            color_map[ColorMap.Palette.Selection],
            color_map[ColorMap.Palette.LeafSelected],
            color_map[ColorMap.Palette.LeafSelected], // spill

            // Secondary
            color_map[ColorMap.Palette.Selection],
            color_map[ColorMap.Palette.SecondarySelection],
            color_map[ColorMap.Palette.SecondarySelection],

            color_map[ColorMap.Palette.CtlLine],
            color_map[ColorMap.Palette.CtlLine]
        )
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (this.layoutParams as LayoutParams).gravity = Gravity.CENTER_VERTICAL
        this.setPadding(0,0,0,0)
    }

    fun set_text() {
        this.removeAllViews()
        val event = this._event ?: return

        val value_text = LeafText(
            ContextThemeWrapper(this.context, R.style.ctl_leaf_value)
        )
        value_text.text = this.get_label_text(event)
        this.addView(value_text)

        (value_text.layoutParams as LayoutParams).gravity = Gravity.CENTER
        value_text.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun get_label_text(event: OpusControlEvent): String {
        return when (event) {
            is OpusVolumeEvent -> (event.value).toString()
            is OpusTempoEvent -> event.value.roundToInt().toString()
            is OpusReverbEvent -> "TODO"
            is OpusPanEvent -> {
                if (event.value > 0F) {
                    val n = (event.value * 10).roundToInt()
                    "$n"
                } else if (event.value < 0F) {
                    val n = (event.value * 10).roundToInt()
                    "$n"
                } else {
                    "0"
                }
            }
            else -> "???"
        }
    }


    override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        //if (this.parent == null || this._get_editor_table().needs_setup) {
        if (this.parent == null) {
            return drawableState
        }

        val new_state = mutableListOf<Int>()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val controller = this.get_controller()
        val beat = this.get_beat()
        val tree = try {
            controller.get_tree(beat, this.position)
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }
        val original_position = try {
            controller.get_blocking_position(beat, this.position) ?: Pair(beat, this.position)
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }
        val tree_original = try {
            controller.get_tree(original_position.first, original_position.second)
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }

        if (tree.is_event()) {
            new_state.add(R.attr.state_active)
        } else if (tree_original != tree) {
            new_state.add(R.attr.state_spill)
        }

        if (this.is_selected()) {
            new_state.add(R.attr.state_focused)
        } else if (this.is_secondary_selected()) {
            new_state.add(R.attr.state_focused_secondary)
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    // ------------------------------------------------------//
}
