package com.qfs.pagan

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuLine(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_row, context, attrs) {
    lateinit var button_insert: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_choose_percussion: ButtonStd

    override fun init_properties() {
        this.button_insert = this.findViewById(R.id.btnInsertLine)
        this.button_remove = this.findViewById(R.id.btnRemoveLine)
        this.button_choose_percussion = this.findViewById(R.id.btnChoosePercussion)
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Row) {
            throw OpusManagerCursor.InvalidModeException(opus_manager.cursor.mode, OpusManagerCursor.CursorMode.Row)
        }

        this.button_remove.isEnabled = opus_manager.get_visible_line_count() > 1

      //  if (main.get_soundfont() == null) {
      //      this.button_line_volume_popup.visibility = View.GONE
      //      this.seekbar_line_volume.visibility = View.GONE
      //  } else {
      //      if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      //          this.seekbar_line_volume.visibility = View.GONE
      //          this.button_line_volume_popup.visibility = View.VISIBLE
      //      } else {
      //          this.seekbar_line_volume.visibility = View.VISIBLE
      //          this.button_line_volume_popup.visibility = View.GONE
      //      }
      //  }


        val channel = opus_manager.cursor.channel
        val line_offset = opus_manager.cursor.line_offset

        if (!opus_manager.is_percussion(channel) || main.get_soundfont() == null) {
            this.button_choose_percussion.visibility = View.GONE
        } else {
            this.button_choose_percussion.visibility = View.VISIBLE
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            main.populate_active_percussion_names(false)
            this.button_choose_percussion.text = if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                this.context.getString(R.string.label_short_percussion, instrument)
            } else {
                this.context.getString(
                    R.string.label_choose_percussion,
                    instrument,
                    main.get_drum_name(instrument) ?: this.context.getString(R.string.drum_not_found)
                )
            }
        }

        this.button_remove.visibility = if (opus_manager.channels[channel].size == 1) {
            View.GONE
        } else {
            View.VISIBLE
        }

    }

    override fun setup_interactions() {
        this.button_choose_percussion.setOnClickListener {
            this.interact_btnChoosePercussion()
        }

        this.button_insert.setOnLongClickListener {
            this.long_click_button_insert_line()
        }

        this.button_insert.setOnClickListener {
            this.click_button_insert_line()
        }

        this.button_remove.setOnClickListener {
            this.click_button_remove_line()
        }

        this.button_remove.setOnLongClickListener {
            this.long_click_button_remove_line()
        }
    }

    fun click_button_insert_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.insert_line(1)
    }

    fun long_click_button_insert_line(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(
            this.context.getString(R.string.dlg_insert_lines),
            1,
            9,
        ) { count: Int ->
            opus_manager.insert_line(count)
        }
        return true
    }

    fun click_button_remove_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.remove_line(1)
    }

    fun long_click_button_remove_line(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val lines = opus_manager.channels[opus_manager.cursor.channel].size
        val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
        main.dialog_number_input(
            this.context.getString(R.string.dlg_remove_lines),
            1,
            max_lines
        ) { count: Int ->
            opus_manager.remove_line(count)
        }

        return true
    }

    private fun _line_volume_dialog(channel: Int, line_offset: Int) {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.dialog_line_volume,
                this as ViewGroup,
                false
            )
        val opus_manager = this.get_main().get_opus_manager()
        val line_volume = opus_manager.get_line_volume(channel, line_offset)

        val scroll_bar = view.findViewById<SeekBar>(R.id.line_volume_scrollbar)!!
        scroll_bar.progress = line_volume
        val title_text = view.findViewById<TextView>(R.id.line_volume_title)!!
        title_text.text = resources.getString(R.string.label_volume_scrollbar, line_volume * 100 / 96)
        title_text.contentDescription = resources.getString(R.string.label_volume_scrollbar, line_volume * 100 / 96)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = resources.getString(R.string.label_volume_scrollbar, p1 * 100 / 96)
                title_text.contentDescription = resources.getString(R.string.label_volume_scrollbar, p1 * 100 / 96)
                opus_manager.set_line_volume(channel, line_offset, p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog = AlertDialog.Builder(this.get_main())
        dialog.setView(view)
        dialog.show()
    }

    private fun interact_btnChoosePercussion() {
        val main = this.get_main()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val default_instrument = opus_manager.get_percussion_instrument(cursor.line_offset)

        val options = mutableListOf<Pair<Int, String>>()
        val sorted_keys = main.active_percussion_names.keys.toMutableList()
        sorted_keys.sort()
        for (note in sorted_keys) {
            val name = main.active_percussion_names[note]
            options.add(Pair(note - 27, "${note - 27}: $name"))
        }

        main.dialog_popup_menu(this.context.getString(R.string.dropdown_choose_percussion), options, default_instrument) { _: Int, value: Int ->
            opus_manager.set_percussion_instrument(value)
        }
    }

}