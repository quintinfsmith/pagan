package com.qfs.pagan

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuRange(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_range, R.layout.contextmenu_range_secondary, primary_container, secondary_container) {
    private lateinit var _button_erase: ImageView
    private lateinit var _radio_mode: RadioGroup
    private lateinit var _label: PaganTextView

    init {
        this.refresh()
    }

    override fun init_properties() {
        this._button_erase = this.primary!!.findViewById(R.id.btnEraseSelection)
        this._label = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.secondary!!.findViewById(R.id.tvMoveModeLabelB)
        } else {
            this.primary.findViewById(R.id.tvMoveModeLabel)
        }

        this._radio_mode = this.secondary!!.findViewById(R.id.rgMoveMode)
    }

    override fun setup_interactions() {
        this._button_erase.setOnClickListener {
            this.get_opus_manager().unset()
        }

        this._radio_mode.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
            val main = this.get_main()
            val opus_manager = this.get_opus_manager()

            main.configuration.move_mode = when (button_id) {
                R.id.rbMoveModeMove -> PaganConfiguration.MoveMode.MOVE
                R.id.rbMoveModeCopy -> PaganConfiguration.MoveMode.COPY
                R.id.rbMoveModeMerge -> PaganConfiguration.MoveMode.MERGE
                else -> PaganConfiguration.MoveMode.COPY
            }
            main.save_configuration()

            _label.text = when (button_id) {
                R.id.rbMoveModeMove -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        this.context.resources.getString(R.string.label_move_range)
                    } else {
                        this.context.resources.getString(R.string.label_move_beat)
                    }
                }
                R.id.rbMoveModeMerge -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        this.context.resources.getString(R.string.label_merge_range)
                    } else {
                        this.context.resources.getString(R.string.label_merge_beat)
                    }
                }
                else -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        this.context.resources.getString(R.string.label_copy_range)
                    } else {
                        this.context.resources.getString(R.string.label_copy_beat)
                    }
                }
            }
        }
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        this._radio_mode.check(when (main.configuration.move_mode) {
            PaganConfiguration.MoveMode.MOVE -> R.id.rbMoveModeMove
            PaganConfiguration.MoveMode.COPY -> R.id.rbMoveModeCopy
            PaganConfiguration.MoveMode.MERGE -> R.id.rbMoveModeMerge
        })

        if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this._label.text = when (main.configuration.move_mode) {
                PaganConfiguration.MoveMode.MOVE -> this.context.resources.getString(R.string.label_move_beat)
                PaganConfiguration.MoveMode.COPY ->  this.context.resources.getString(R.string.label_copy_beat)
                PaganConfiguration.MoveMode.MERGE -> this.context.resources.getString(R.string.label_merge_beat)
            }
        } else {
            return
        }
    }

}
