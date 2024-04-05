package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.Transition

class ContextMenuControlLeaf(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_line_ctl_leaf, context, attrs) {
    lateinit var button_transition: ButtonStd
    lateinit var button_duration: ButtonStd
    lateinit var button_value: ButtonStd
    lateinit var button_split: ButtonIcon

    override fun init_properties() {
        this.button_transition = this.findViewById(R.id.btnChooseTransition)
        this.button_duration = this.findViewById(R.id.btnDuration)
        this.button_value = this.findViewById(R.id.btnCtlAmount)
        this.button_split = this.findViewById(R.id.btnSplit)
    }

    override fun setup_interactions() {
        this.button_value.setOnClickListener {
            this.click_button_ctl_value()
        }

        this.button_transition.setOnClickListener {
            this.click_button_ctl_transition()
        }

        this.button_split.setOnClickListener {
            this.click_button_split()
        }
        this.button_split.setOnLongClickListener {
            this.long_click_button_split()
            true
        }
    }
    fun click_button_split() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Global -> opus_manager.split_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position, 2)
            CtlLineLevel.Channel -> opus_manager.split_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, 2)
            CtlLineLevel.Line -> opus_manager.split_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, 2)
            null -> {
                Log.d("AAA", "HM?")
            }
        }
    }

    fun long_click_button_split() {
        this.get_main().dialog_number_input("Split", 2, 32, 2) { split_count: Int ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.split_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position, split_count)
                CtlLineLevel.Channel -> opus_manager.split_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, split_count)
                CtlLineLevel.Line -> opus_manager.split_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, split_count)
                null -> { }
            }
        }
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = BeatKey(
            cursor.channel,
            cursor.line_offset,
            cursor.beat
        )
        val ctl_tree = opus_manager.get_line_ctl_tree(
            cursor.ctl_type!!,
            beat_key,
            cursor.position
        )

        this.button_value.text = if (!ctl_tree.is_event()) {
            opus_manager.get_current_ctl_line_value(
                cursor.ctl_type!!,
                beat_key,
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

    fun click_button_ctl_transition() {

    }

    fun click_button_ctl_value() {
        this.get_main().dialog_number_input("Value", 0, 1, 0) {

        }
    }
}