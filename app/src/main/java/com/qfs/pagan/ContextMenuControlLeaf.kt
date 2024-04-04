package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.Transition

class ContextMenuControlLeaf(context: Context, attrs: AttributeSet? = null): ContextMenuView(context, attrs) {
    val button_transition: ButtonStd
    val button_duration: ButtonStd
    val button_value: ButtonStd
    init {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.contextmenu_line_ctl_leaf,
                this as ViewGroup,
                false
            )

        this.addView(view)

        this.button_transition = this.findViewById<ButtonStd>(R.id.btnChooseTransition)
        this.button_duration = this.findViewById<ButtonStd>(R.id.btnDuration)
        this.button_value = this.findViewById<ButtonStd>(R.id.btnCtlAmount)
        this.refresh()

    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val ctl_tree = opus_manager.get_line_ctl_tree(
            cursor.ctl_type!!,
            BeatKey(
                cursor.channel,
                cursor.line_offset,
                cursor.beat
            ),
            cursor.position
        )

        this.button_value.text = if (!ctl_tree.is_event()) {
            opus_manager.get_current_ctl_line_value(
                cursor.ctl_type!!,
                BeatKey(
                    cursor.channel,
                    cursor.line_offset,
                    cursor.beat
                ),
                cursor.position
            ).toString()
        } else {
            // TODO: Formatting
            ctl_tree.event!!.value.toString()
        }

        this.button_duration.visibility =
            if (!ctl_tree.is_event() || ctl_tree.event!!.transition == Transition.Instantaneous) {
                View.GONE
            } else {
                View.VISIBLE
            }

        this.button_transition.text = if (!ctl_tree.is_event()) {
            ""
        } else {
            when (ctl_tree.event!!.transition) {
                Transition.Instantaneous -> "Immediate"
                Transition.Linear -> "Linear"
            }

        }
    }

    fun setup_interactions() {
        this.button_value.setOnClickListener {
            this.click_button_ctl_value()
        }

        this.button_transition.setOnClickListener {
            this.click_button_ctl_transition()
        }
    }

    fun click_button_ctl_transition() {

    }

    fun click_button_ctl_value() {
        this.get_main().dialog_number_input("Value", 0, 1, 0) {

        }
    }
}