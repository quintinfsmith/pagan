package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuLink(context: Context, attrs: AttributeSet? = null): ContextMenuView(context, attrs) {
    val button_unlink: ButtonIcon
    val button_unlink_all: ButtonIcon
    val button_erase: ButtonIcon
    val radio_mode: RadioGroup
    val label: PaganTextView
    init {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.contextmenu_linking,
                this as ViewGroup,
                false
            )

        this.addView(view)

        this.button_unlink = this.findViewById(R.id.btnUnLink)
        this.button_unlink_all = this.findViewById(R.id.btnUnLinkAll)
        this.button_erase = this.findViewById(R.id.btnEraseSelection)
        this.label = this.findViewById(R.id.tvLinkLabel)
        this.radio_mode = this.findViewById<RadioGroup?>(R.id.rgLinkMode)

        this.refresh()
    }

    fun setup_interactions() {
        this.button_erase.setOnClickListener {
            this.get_opus_manager().unset()
        }

        this.radio_mode?.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
            val main = this.get_main()
            val opus_manager = this.get_opus_manager()

            main.configuration.link_mode = when (button_id) {
                R.id.rbLinkModeLink -> PaganConfiguration.LinkMode.LINK
                R.id.rbLinkModeMove -> PaganConfiguration.LinkMode.MOVE
                R.id.rbLinkModeCopy -> PaganConfiguration.LinkMode.COPY
                else -> PaganConfiguration.LinkMode.COPY
            }
            main.save_configuration()

            label.text = when (button_id) {
                R.id.rbLinkModeLink -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        resources.getString(R.string.label_link_range)
                    } else {
                        resources.getString(R.string.label_link_beat)
                    }
                }
                R.id.rbLinkModeMove -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        resources.getString(R.string.label_move_range)
                    } else {
                        resources.getString(R.string.label_move_beat)
                    }
                }
                // R.id.rbLinkModeCopy,
                else -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        resources.getString(R.string.label_copy_range)
                    } else {
                        resources.getString(R.string.label_copy_beat)
                    }
                }
            }
        }
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        this.radio_mode.check(when (main.configuration.link_mode) {
            PaganConfiguration.LinkMode.LINK -> R.id.rbLinkModeLink
            PaganConfiguration.LinkMode.MOVE -> R.id.rbLinkModeMove
            PaganConfiguration.LinkMode.COPY -> R.id.rbLinkModeCopy
        })


        val (is_networked, many_links) = if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            this.label.text = when (main.configuration.link_mode) {
                PaganConfiguration.LinkMode.LINK -> resources.getString(R.string.label_link_range)
                PaganConfiguration.LinkMode.MOVE -> resources.getString(R.string.label_move_range)
                PaganConfiguration.LinkMode.COPY -> resources.getString(R.string.label_copy_range)
            }

            var output = false
            for (beat_key in opus_manager.get_beatkeys_in_range(
                opus_manager.cursor.range!!.first,
                opus_manager.cursor.range!!.second
            )) {
                if (opus_manager.is_networked(beat_key)) {
                    output = true
                    break
                }
            }

            Pair(
                output,
                true
            )
        } else if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this.label.text = when (main.configuration.link_mode) {
                PaganConfiguration.LinkMode.LINK -> resources.getString(R.string.label_link_beat)
                PaganConfiguration.LinkMode.MOVE -> resources.getString(R.string.label_move_beat)
                PaganConfiguration.LinkMode.COPY ->  resources.getString(R.string.label_copy_beat)
            }

            val cursor_key = opus_manager.cursor.get_beatkey()
            Pair(
                opus_manager.is_networked(cursor_key),
                opus_manager.get_all_linked(cursor_key).size > 2
            )
        } else {
            return
        }

        if (is_networked) {
            this.button_unlink.visibility = View.VISIBLE
            this.button_unlink.setOnClickListener {
                this.click_button_unlink()
            }
            if (!many_links) {
                this.button_unlink_all.visibility = View.GONE
            } else {
                this.button_unlink_all.visibility = View.VISIBLE
                this.button_unlink_all.setOnClickListener {
                    this.click_button_unlink_all()
                }
            }
        } else {
            this.button_unlink.visibility = View.GONE
            this.button_unlink_all.visibility = View.GONE
        }
    }

    private fun click_button_unlink() {
        val opus_manager = this.get_opus_manager()
        opus_manager.unlink_beat()
    }

    private fun click_button_unlink_all() {
        val opus_manager = this.get_opus_manager()
        opus_manager.clear_link_pool()
    }

}