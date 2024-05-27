package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuLink(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_linking, R.layout.contextmenu_linking_secondary, primary_container, secondary_container) {
    lateinit var button_unlink: ButtonIcon
    lateinit var button_unlink_all: ButtonIcon
    lateinit var button_erase: ButtonIcon
    lateinit var radio_mode: RadioGroup
    lateinit var label: PaganTextView
    override fun init_properties() {
        this.button_unlink = this.primary!!.findViewById(R.id.btnUnLink)
        this.button_unlink_all = this.primary!!.findViewById(R.id.btnUnLinkAll)
        this.button_erase = this.primary!!.findViewById(R.id.btnEraseSelection)
        this.label = this.secondary!!.findViewById(R.id.tvLinkLabel)
        this.radio_mode = this.secondary!!.findViewById<RadioGroup?>(R.id.rgLinkMode)
    }

    override fun setup_interactions() {
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
                R.id.rbLinkModeMerge -> PaganConfiguration.LinkMode.MERGE
                else -> PaganConfiguration.LinkMode.COPY
            }
            main.save_configuration()

            label.text = when (button_id) {
                R.id.rbLinkModeLink -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        this.context.resources.getString(R.string.label_link_range)
                    } else {
                        this.context.resources.getString(R.string.label_link_beat)
                    }
                }
                R.id.rbLinkModeMove -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        this.context.resources.getString(R.string.label_move_range)
                    } else {
                        this.context.resources.getString(R.string.label_move_beat)
                    }
                }
                R.id.rbLinkModeMerge -> {
                    if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                        this.context.resources.getString(R.string.label_merge_range)
                    } else {
                        this.context.resources.getString(R.string.label_merge_beat)
                    }
                }
                // R.id.rbLinkModeCopy,
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

        this.radio_mode.check(when (main.configuration.link_mode) {
            PaganConfiguration.LinkMode.LINK -> R.id.rbLinkModeLink
            PaganConfiguration.LinkMode.MOVE -> R.id.rbLinkModeMove
            PaganConfiguration.LinkMode.COPY -> R.id.rbLinkModeCopy
            PaganConfiguration.LinkMode.MERGE -> R.id.rbLinkModeMerge
        })


        val (is_networked, many_links) = if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            this.label.text = when (main.configuration.link_mode) {
                PaganConfiguration.LinkMode.LINK -> this.context.resources.getString(R.string.label_link_range)
                PaganConfiguration.LinkMode.MOVE -> this.context.resources.getString(R.string.label_move_range)
                PaganConfiguration.LinkMode.COPY -> this.context.resources.getString(R.string.label_copy_range)
                PaganConfiguration.LinkMode.MERGE -> this.context.resources.getString(R.string.label_merge_range)
            }

            var output = false
            val (first_key, second_key) = opus_manager.cursor.get_ordered_range()!!
            for (beat_key in opus_manager.get_beatkeys_in_range(first_key, second_key)) {
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
                PaganConfiguration.LinkMode.LINK -> this.context.resources.getString(R.string.label_link_beat)
                PaganConfiguration.LinkMode.MOVE -> this.context.resources.getString(R.string.label_move_beat)
                PaganConfiguration.LinkMode.COPY ->  this.context.resources.getString(R.string.label_copy_beat)
                PaganConfiguration.LinkMode.MERGE -> this.context.resources.getString(R.string.label_merge_beat)
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