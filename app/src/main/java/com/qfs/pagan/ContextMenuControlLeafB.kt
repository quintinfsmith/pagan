package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.widget.RadioGroup

class ContextMenuControlLeafB(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_line_ctl_leaf_b, context, attrs) {
    lateinit var button_erase: ButtonIcon
    lateinit var radio_mode: RadioGroup
    override fun init_properties() {
        super.init_properties()
        this.button_erase = this.findViewById(R.id.btnEraseSelection)
        this.radio_mode = this.findViewById<RadioGroup?>(R.id.rgLinkMode)
    }

    override fun setup_interactions() {
        this.button_erase.setOnClickListener {
            this.get_opus_manager().unset()
        }
        this.radio_mode?.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
            val main = this.get_main()
            main.configuration.link_mode = when (button_id) {
                R.id.rbLinkModeMove -> PaganConfiguration.LinkMode.MOVE
                R.id.rbLinkModeCopy -> PaganConfiguration.LinkMode.COPY
                else -> PaganConfiguration.LinkMode.COPY
            }
            main.save_configuration()
            this.refresh()
        }

    }

    override fun refresh() {
        val main = this.get_main()
        if (main.configuration.link_mode == PaganConfiguration.LinkMode.LINK) {
            main.configuration.link_mode = PaganConfiguration.LinkMode.COPY
            main.save_configuration()
        }
        this.radio_mode.check(when (main.configuration.link_mode) {
            PaganConfiguration.LinkMode.MOVE -> R.id.rbLinkModeMove
            PaganConfiguration.LinkMode.COPY -> R.id.rbLinkModeCopy
            else -> R.id.rbLinkModeCopy
        })
    }

}