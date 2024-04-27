package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuControlLeafB(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_line_ctl_leaf_b, context, attrs) {
    lateinit var button_erase: ButtonIcon
    lateinit var label: PaganTextView
    lateinit var switch: PaganSwitch
    override fun init_properties() {
        super.init_properties()
        this.button_erase = this.findViewById(R.id.btnEraseSelection)
        this.label = this.findViewById(R.id.tvLinkLabel)
        this.switch = this.findViewById(R.id.sCtlCopyMode)
    }

    override fun setup_interactions() {
        this.button_erase.setOnClickListener {
            if (it.isEnabled) {
                this.get_opus_manager().unset()
            }
        }

        this.switch.setOnCheckedChangeListener { switch: CompoundButton, value: Boolean ->
            val main = this.get_main()
            main.configuration.link_mode = if (value) {
                PaganConfiguration.LinkMode.MOVE
            } else {
                PaganConfiguration.LinkMode.COPY
            }
            main.save_configuration()
            this.refresh()
        }
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        this.button_erase.isEnabled = opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range
        if (main.configuration.link_mode == PaganConfiguration.LinkMode.LINK) {
            main.configuration.link_mode = PaganConfiguration.LinkMode.COPY
            main.save_configuration()
        }
        this.switch.isChecked = main.configuration.link_mode == PaganConfiguration.LinkMode.MOVE
        this.label.text = resources.getString(
            if (this.switch.isChecked) {
                R.string.label_move_ctl
            } else {
                R.string.label_copy_ctl
            }
        )
    }

}