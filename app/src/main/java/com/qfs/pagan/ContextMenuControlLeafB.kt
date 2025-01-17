package com.qfs.pagan

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup

class ContextMenuControlLeafB(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_line_ctl_leaf_b, R.layout.contextmenu_line_ctl_leaf_b_secondary, primary_container, secondary_container) {
    private lateinit var _button_erase: ImageView
    private lateinit var _radio_mode: RadioGroup

    init {
        this.refresh()
    }

    override fun init_properties() {
        this._button_erase = this.primary!!.findViewById(R.id.btnEraseSelection)
        this._radio_mode = this.secondary!!.findViewById(R.id.rgMoveMode)
    }


    override fun setup_interactions() {
        this._button_erase.setOnClickListener {
            this.get_opus_manager().unset()
        }
        this._radio_mode.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
            val main = this.get_main()
            main.configuration.move_mode = when (button_id) {
                R.id.rbMoveModeMove -> PaganConfiguration.MoveMode.MOVE
                R.id.rbMoveModeCopy -> PaganConfiguration.MoveMode.COPY
                else -> PaganConfiguration.MoveMode.COPY
            }
            main.save_configuration()
            this.refresh()
        }

    }

    override fun refresh() {
        val main = this.get_main()
        this._radio_mode.check(when (main.configuration.move_mode) {
            PaganConfiguration.MoveMode.MOVE -> R.id.rbMoveModeMove
            PaganConfiguration.MoveMode.COPY -> R.id.rbMoveModeCopy
            else -> R.id.rbMoveModeCopy
        })
    }

}