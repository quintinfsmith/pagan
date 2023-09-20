package com.qfs.pagan

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.qfs.apres.InvalidMIDIFile
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.*
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.concurrent.thread

/**
 *
 */
class EditorFragment : PaganFragment() {
    // Boiler Plate //
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    //////////////////

    val view_model: EditorViewModel by viewModels()
    private var active_context_menu_index: ContextMenu? = null

    enum class ContextMenu {
        Leaf,
        Line,
        Column,
        Linking
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (this._binding == null) {
            _binding = FragmentMainBinding.inflate(inflater, container, false)

            this.get_main().apply {
                unlockDrawer()
                update_menu_options()
            }
        }
        return binding.root
    }
    override fun onStop() {
        val editor_table = this.get_main().findViewById<EditorTable>(R.id.etEditorTable)
        val (scroll_x, scroll_y) = editor_table.get_scroll_offset()
        this.view_model.coarse_x = scroll_x.first
        this.view_model.fine_x = scroll_x.second
        this.view_model.coarse_y = scroll_y.first
        this.view_model.fine_y = scroll_y.second
        super.onStop()
    }

    override fun onResume() {
        this.get_main().update_title_text()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val opus_manager = this.get_main().get_opus_manager()
        opus_manager.cursor_clear()

        if (this.view_model.coarse_x != null) {
            val editor_table = this.get_main().findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.setup()
            editor_table.update_cursor(this.get_main().get_opus_manager().cursor)
            thread {
                Thread.sleep(100)
                this.activity!!.runOnUiThread {
                    editor_table.precise_scroll(
                        this.view_model.coarse_x!!,
                        this.view_model.fine_x!!,
                        this.view_model.coarse_y!!,
                        this.view_model.fine_y!!
                    )

                    this.view_model.coarse_y = null
                    this.view_model.coarse_x = null
                    this.view_model.fine_y = null
                    this.view_model.fine_x = null
                }
            }
        }

        setFragmentResultListener(IntentFragmentToken.Load.name) { _, bundle: Bundle? ->
            if (bundle == null) {
                return@setFragmentResultListener
            }

            bundle.getString("PATH")?.let { path: String ->
                val main = this.get_main()
                main.get_opus_manager().load(path)
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportMidi.name) { _, bundle: Bundle? ->
            bundle!!.getString("URI")?.let { path ->
                val main = this.get_main()
                try {
                    main.import_midi(path)
                } catch (e: InvalidMIDIFile) {
                    main.get_opus_manager().new()
                    main.feedback_msg(getString(R.string.feedback_midi_fail))
                }
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportProject.name) { _, bundle: Bundle? ->
            try {
                bundle!!.getString("URI")?.let { path ->
                    val main = this.get_main()
                    main.import_project(path)
                }
            } catch (e: Exception) {
                val opus_manager = this.get_main().get_opus_manager()
                // if Not Loaded, just create new and throw a message up
                if (!opus_manager.first_load_done) {
                    opus_manager.new()
                }
                this.get_main().feedback_msg(getString(R.string.feedback_import_fail))
            }
        }

        setFragmentResultListener(IntentFragmentToken.New.name) { _, _: Bundle? ->
            val main = this.get_main()
            main.loading_reticle()
            main.get_opus_manager().new()
            main.cancel_reticle()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun reset_context_menu() {
        when (this.active_context_menu_index) {
            ContextMenu.Leaf -> {
                this.setContextMenu_leaf()
            }
            ContextMenu.Line -> {
                this.setContextMenu_line()
            }
            ContextMenu.Column -> {
                this.setContextMenu_column()
            }
            ContextMenu.Linking -> {
                this.setContextMenu_linking()
            }
            null -> { }
        }
    }

    fun clearContextMenu() {
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)
        llContextMenu.removeAllViews()
        this.active_context_menu_index = null
    }

    internal fun setContextMenu_linking() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_linking,
            llContextMenu,
            false
        )
        val btnUnLink = view.findViewById<ImageView>(R.id.btnUnLink)
        val btnUnLinkAll = view.findViewById<ImageView>(R.id.btnUnLinkAll)
        val btnCancelLink: TextView = view.findViewById(R.id.btnCancelLink)

        val (is_networked, many_links) = if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            var output = false
            for (beat_key in opus_manager.get_beatkeys_in_range(opus_manager.cursor.range!!.first, opus_manager.cursor.range!!.second)) {
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
            val cursor_key = opus_manager.cursor.get_beatkey()
            Pair(
                opus_manager.is_networked(cursor_key),
                opus_manager.get_all_linked(cursor_key).size > 2
            )
        } else {
            return
        }
        if (is_networked) {
            btnUnLink.setOnClickListener {
                this.interact_btnUnlink()
            }
            if (!many_links) {
                btnUnLinkAll.visibility = View.GONE
            } else {
                btnUnLinkAll.setOnClickListener {
                    this.interact_btnUnlinkAll()
                }
            }
        } else {
            btnUnLink.visibility = View.GONE
            btnUnLinkAll.visibility = View.GONE
        }

        btnCancelLink.setOnClickListener {
            this.interact_btnCancelLink()
        }

        llContextMenu.addView(view)
        this.active_context_menu_index = ContextMenu.Linking
    }

    fun setContextMenu_column() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_column,
            llContextMenu,
            false
        )
        val btnInsertBeat = view.findViewById<ImageView>(R.id.btnInsertBeat)
        val btnRemoveBeat = view.findViewById<ImageView>(R.id.btnRemoveBeat)

        btnInsertBeat.setOnClickListener {
            val beat = opus_manager.cursor.beat
            opus_manager.insert_beat_at_cursor(1)
            opus_manager.cursor_select_column(beat + 1)
        }
        btnInsertBeat.setOnLongClickListener {
            main.popup_number_dialog( getString(R.string.dlg_insert_beats), 1, 99) { count: Int ->
                val beat = opus_manager.cursor.beat
                opus_manager.insert_beat_at_cursor(count)
                opus_manager.cursor_select_column(beat + count)
            }
            true
        }

        btnRemoveBeat.setOnClickListener {
            val beat = opus_manager.cursor.beat
            try {
                opus_manager.remove_beat_at_cursor(1)
                if (beat >= opus_manager.opus_beat_count) {
                    opus_manager.cursor_select_column(opus_manager.opus_beat_count - 1)
                }
            } catch (e: BaseLayer.RemovingLastBeatException) {
                this.get_main().feedback_msg(getString(R.string.feedback_rm_lastbeat))
            }

            if (opus_manager.opus_beat_count == 1) {
                btnRemoveBeat.visibility = View.GONE
            }
        }

        btnRemoveBeat.setOnLongClickListener {
            main.popup_number_dialog(getString(R.string.dlg_remove_beats), 1, opus_manager.opus_beat_count - 1) { count: Int ->
                val beat = opus_manager.cursor.beat
                opus_manager.remove_beat_at_cursor(count)
                if (beat >= opus_manager.opus_beat_count) {
                    opus_manager.cursor_select_column(opus_manager.opus_beat_count - 1)
                }

                if (opus_manager.opus_beat_count == 1) {
                    btnRemoveBeat.visibility = View.GONE
                }
            }
            true
        }

        if (opus_manager.opus_beat_count == 1) {
            btnRemoveBeat.visibility = View.GONE
        }
        llContextMenu.addView(view)
        if (llContextMenu.childCount == 2) {
            llContextMenu.removeViewAt(0)
        }
        this.active_context_menu_index = ContextMenu.Column
    }

    fun setContextMenu_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Row) {
            throw OpusManagerCursor.InvalidModeException(opus_manager.cursor.mode, OpusManagerCursor.CursorMode.Row)
        }
        this.clearContextMenu()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_row,
            llContextMenu,
            false
        )
        val sbLineVolume = view.findViewById<SeekBar>(R.id.sbLineVolume)
        val btnRemoveLine = view.findViewById<ImageView>(R.id.btnRemoveLine)
        val btnInsertLine = view.findViewById<ImageView>(R.id.btnInsertLine)
        val btnChoosePercussion: TextView = view.findViewById(R.id.btnChoosePercussion)

        if (opus_manager.get_total_line_count() == 1) {
            btnRemoveLine.visibility = View.GONE
        }

        val channel = opus_manager.cursor.channel
        val line_offset = opus_manager.cursor.line_offset

        if (!opus_manager.is_percussion(channel)) {
            btnChoosePercussion.visibility = View.GONE
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)

            btnChoosePercussion.setOnClickListener {
                this.interact_btnChoosePercussion()
            }

            btnChoosePercussion.text = main.getString(
                R.string.label_choose_percussion,
                instrument,
                main.get_drum_name(instrument) ?: "Drum Not Found"
            )
        }

        if (opus_manager.channels[channel].size == 1) {
            btnRemoveLine.visibility = View.GONE
        } else {
            btnRemoveLine.setOnClickListener {
                opus_manager.remove_line(1)
                opus_manager.cursor_select_row(channel, max(0, line_offset - 1))
            }

            btnRemoveLine.setOnLongClickListener {
                val lines = opus_manager.channels[opus_manager.cursor.channel].size
                val max_lines = min(lines - 1, lines - opus_manager.cursor.line_offset)
                main.popup_number_dialog(
                    getString(R.string.dlg_remove_lines),
                    1,
                    max_lines
                ) { count: Int ->
                    opus_manager.remove_line(count)
                }
                true
            }
        }

        btnInsertLine.setOnClickListener {
            opus_manager.insert_line(1)
            opus_manager.cursor_select_row(channel, line_offset + 1)
        }

        btnInsertLine.setOnLongClickListener {
            main.popup_number_dialog(
                getString(R.string.dlg_insert_lines),
                1,
                9,
            ) { count: Int ->
                opus_manager.insert_line(count)
                opus_manager.cursor_select_row(channel, line_offset + count)
            }
            true
        }

        sbLineVolume.progress = opus_manager.get_line_volume(channel, line_offset)
        sbLineVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) { }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                opus_manager.set_line_volume(channel, line_offset, seekbar.progress)
            }
        })

        llContextMenu.addView(view)
        this.active_context_menu_index = ContextMenu.Line
    }

    internal fun setContextMenu_leaf() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_cell,
            llContextMenu,
            false
        )
        llContextMenu.addView(view)

        val rosRelativeOption = view.findViewById<RelativeOptionSelector>(R.id.rosRelativeOption)

        val btnUnset = view.findViewById<ImageView>(R.id.btnUnset)
        val btnSplit = view.findViewById<View>(R.id.btnSplit)
        val btnRemove = view.findViewById<View>(R.id.btnRemove)
        val btnInsert = view.findViewById<View>(R.id.btnInsert)
        val btnDuration = view.findViewById<TextView>(R.id.btnDuration)

        val nsOctave: NumberSelector = view.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = view.findViewById(R.id.nsOffset)

        val current_tree = opus_manager.get_tree()

        // If event exists, change relative mode, other wise use active relative mode
        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            opus_manager.set_relative_mode(event)
        }


        rosRelativeOption.setState(opus_manager.relative_mode, true)
        if (main.configuration.relative_mode) {
            rosRelativeOption.visibility = View.VISIBLE
        } else {
            rosRelativeOption.visibility = View.GONE
        }


        if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
            nsOctave.visibility = View.GONE
            nsOffset.visibility = View.GONE
            rosRelativeOption.visibility = View.GONE

            if (!opus_manager.get_tree().is_event()) {
                btnUnset.setImageResource(R.drawable.set_percussion)
            }

            btnUnset.setOnClickListener {
                this.interact_btnUnset()
            }

        } else {
            if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                val value = if (event.relative && ! main.configuration.relative_mode) {
                    opus_manager.get_absolute_value(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())!!
                } else {
                    if (event.note < 0) {
                        0 - event.note
                    } else {
                        event.note
                    }
                }
                if (value >= 0) {
                    nsOffset.setState(value % event.radix, manual = true, surpress_callback = true)
                    nsOctave.setState(value / event.radix, manual = true, surpress_callback = true)
                }
            }

            nsOffset.setOnChange(this::interact_nsOffset)
            nsOctave.setOnChange(this::interact_nsOctave)
            rosRelativeOption.setOnChange(this::interact_rosRelativeOption)
        }

        btnSplit.setOnClickListener {
            val beatkey = opus_manager.cursor.get_beatkey()
            val position = opus_manager.cursor.get_position().toMutableList()

            opus_manager.split_tree(2)

            position.add(0)
            opus_manager.cursor_select(beatkey, position)
        }

        btnSplit.setOnLongClickListener {
            main.popup_number_dialog(getString(R.string.dlg_split), 2, 29) { splits: Int ->
                val beatkey = opus_manager.cursor.get_beatkey()
                val position = opus_manager.cursor.get_position().toMutableList()

                opus_manager.split_tree(splits)

                position.add(0)
                opus_manager.cursor_select(beatkey, position)
            }
            true
        }

        val channel = opus_manager.cursor.channel
        if (!opus_manager.is_percussion(channel) && current_tree.is_leaf() && !current_tree.is_event()) {
            btnUnset.visibility = View.INVISIBLE
        } else {
            btnUnset.setOnClickListener {
                this.interact_btnUnset()
            }
        }

        if (opus_manager.cursor.get_position().isEmpty()) {
            btnRemove.visibility = View.INVISIBLE
        } else {
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener {
                opus_manager.remove(1)
            }

            btnRemove.setOnLongClickListener {
                val position = opus_manager.cursor.get_position().toMutableList()
                if (position.isNotEmpty()) {
                    position.removeLast()

                }
                opus_manager.cursor_select(
                    opus_manager.cursor.get_beatkey(),
                    position
                )

                opus_manager.unset()
                true
            }
        }

        btnInsert.setOnClickListener {
            val beat_key = opus_manager.cursor.get_beatkey()
            val position = opus_manager.cursor.get_position().toMutableList()
            if (position.isEmpty()) {
                opus_manager.split_tree(2)
                position.add(0)
            } else {
                opus_manager.insert_after(1)
                position[position.size - 1] += 1
            }
            opus_manager.cursor_select(beat_key, position)
        }

        btnInsert.setOnLongClickListener {
            main.popup_number_dialog(getString(R.string.dlg_insert), 1, 29) { count: Int ->
                val beat_key = opus_manager.cursor.get_beatkey()
                val position = opus_manager.cursor.get_position().toMutableList()
                if (position.isEmpty()) {
                    position.add(0)
                    opus_manager.split_tree(count + 1)
                } else {
                    opus_manager.insert_after(count)
                    position[position.size - 1] += count
                }
                opus_manager.cursor_select(beat_key, position)
            }
            true
        }

        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            btnDuration.setOnClickListener {
                main.popup_number_dialog("Duration", 1, 99, default=event.duration) { value: Int ->
                    val adj_value = max(value, 1)
                    val cursor = opus_manager.cursor
                    val beat_key = cursor.get_beatkey()
                    val position = cursor.get_position()
                    opus_manager.set_duration(beat_key, position, adj_value)
                    (it as TextView).text = "x$adj_value"
                }
            }
            btnDuration.setOnLongClickListener {
                val cursor = opus_manager.cursor
                val beat_key = cursor.get_beatkey()
                val position = cursor.get_position()
                opus_manager.set_duration(beat_key, position, 1)
                (it as TextView).text = "x1"
                true
            }
            btnDuration.text = "x${event.duration}"
        } else {
            btnDuration.visibility = View.INVISIBLE
        }


        this.active_context_menu_index = ContextMenu.Leaf
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val current_tree = opus_manager.get_tree()

        opus_manager.relative_mode = view.getState()!!

        var event = current_tree.get_event() ?: return

        val nsOctave: NumberSelector = main.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = main.findViewById(R.id.nsOffset)

        when (opus_manager.relative_mode) {
            0 -> {
                if (event.relative) {
                    try {
                        opus_manager.convert_event_to_absolute()
                        event = current_tree.get_event()!!
                    } catch (e: Exception) {
                        event.note = 0
                        event.relative = false
                        opus_manager.set_event(event.copy())
                    }
                }
                nsOctave.setState(event.note / event.radix, manual = true, surpress_callback = true)
                nsOffset.setState(event.note % event.radix, manual = true, surpress_callback = true)
            }
            1 -> {
                if (!event.relative) {
                    opus_manager.convert_event_to_relative()
                    event = current_tree.get_event()!!
                }

                if (event.note < 0) {
                    nsOctave.unset_active_button()
                    nsOffset.unset_active_button()
                } else {
                    nsOctave.setState(event.note / event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                    nsOffset.setState(event.note % event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                }
            }
            2 -> {
                if (!event.relative) {
                    opus_manager.convert_event_to_relative()
                    event = current_tree.get_event()!!
                }

                if (event.note > 0) {
                    nsOctave.unset_active_button()
                    nsOffset.unset_active_button()
                } else {
                    nsOctave.setState(
                        (0 - event.note) / event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                    nsOffset.setState(
                        (0 - event.note) % event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                }
            }
        }
    }

    fun play_event(beat_key: BeatKey, position: List<Int>) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        if (event_note < 0) {
            return
        }
        val volume = opus_manager.channels[beat_key.channel].get_line_volume(beat_key.line_offset)
        main.play_event(
            beat_key.channel,
            event_note,
            volume
        )

    }

    private fun interact_btnUnset() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val channel = opus_manager.cursor.channel
        if (!opus_manager.is_percussion(channel) || opus_manager.get_tree().is_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event()
        }
        this.reset_context_menu()
    }

    private fun interact_btnUnlink() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.unlink_beat()
        opus_manager.cursor.is_linking = false
        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val beat_key = opus_manager.cursor.get_beatkey()
                opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, _) = opus_manager.cursor.range!!
                opus_manager.cursor_select(first, opus_manager.get_first_position(first))
            }
            else -> {}
        }
    }

    private fun interact_btnUnlinkAll() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.clear_link_pool()
        opus_manager.cursor.is_linking = false
        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val beat_key = opus_manager.cursor.get_beatkey()
                opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, _) = opus_manager.cursor.range!!
                opus_manager.cursor_select(first, opus_manager.get_first_position(first))
            }
            else -> {}
        }

    }

    private fun interact_btnCancelLink() {
        val opus_manager = this.get_main().get_opus_manager()
        opus_manager.cursor.is_linking = false
        if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val beat_key = opus_manager.cursor.range!!.first
            opus_manager.cursor_select(
                beat_key,
                opus_manager.get_first_position(beat_key)
            )
        } else {
            opus_manager.cursor_select(
                opus_manager.cursor.get_beatkey(),
                opus_manager.cursor.get_position()
            )
        }
    }


    private fun interact_nsOffset(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree()

        val duration = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.duration
        } else {
            1
        }

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            var prev_note = if (opus_manager.relative_mode != 0) {
                val nsOctave = this.get_main().findViewById<NumberSelector>(R.id.nsOctave)
                if (nsOctave.getState() == null) {
                    nsOctave.setState(0, manual = true, surpress_callback = true)
                    0
                } else {
                    event.note
                }
            } else {
                event.note
            }

            when (opus_manager.relative_mode) {
                2 -> {
                    if (prev_note > 0) {
                        prev_note *= -1
                    }
                    ((prev_note / event.radix) * event.radix) - progress
                }
                else -> {
                    ((prev_note / event.radix) * event.radix) + progress
                }
            }
        } else {
            when (opus_manager.relative_mode) {
                2 -> {
                    0 - progress
                }
                1 -> {
                    progress
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        ((preceding_event.note / opus_manager.RADIX) * opus_manager.RADIX) + progress
                    } else {
                        progress
                    }
                }
            }
        }

        val event = OpusEvent(
            value,
            opus_manager.RADIX,
            opus_manager.cursor.channel,
            opus_manager.relative_mode != 0,
            duration
        )

        opus_manager.set_event(event)
        this.play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
        this.reset_context_menu()
    }

    private fun interact_nsOctave(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState() ?: return

        val current_tree = opus_manager.get_tree()
        val duration = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.duration
        } else {
            1
        }

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            val prev_note = if (opus_manager.relative_mode != 0) {
                val nsOffset  = this.get_main().findViewById<NumberSelector>(R.id.nsOffset)
                if (nsOffset.getState() == null) {
                    nsOffset.setState(0, manual = true, surpress_callback = true)
                    0
                } else {
                    event.note
                }
            } else {
                event.note
            }

            when (opus_manager.relative_mode) {
                2 -> {
                    0 - (((0 - prev_note) % event.radix) + (progress * event.radix))
                }
                else -> {
                    ((prev_note % event.radix) + (progress * event.radix))
                }
            }
        } else {
            when (opus_manager.relative_mode) {
                2 -> {
                    (0 - progress) * opus_manager.RADIX
                }
                1 -> {
                    progress * opus_manager.RADIX
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        (progress * opus_manager.RADIX) + (preceding_event.note % opus_manager.RADIX)
                    } else {
                        (progress * opus_manager.RADIX)
                    }
                }
            }
        }

        val event = OpusEvent(
            value,
            opus_manager.RADIX,
            opus_manager.cursor.channel,
            opus_manager.relative_mode != 0,
            duration
        )

        opus_manager.set_event(event)
        this.play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
        this.reset_context_menu()
    }


    private fun interact_btnChoosePercussion() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        val default_instrument = opus_manager.get_percussion_instrument(cursor.line_offset)

        val options = mutableListOf<Pair<Int, String>>()
        val sorted_keys = main.active_percussion_names.keys.toMutableList()
        sorted_keys.sort()
        for (note in sorted_keys) {
            val name = main.active_percussion_names[note]
            options.add(Pair(note - 27, "${note - 27}: $name"))
        }

        main.popup_menu_dialog(getString(R.string.dropdown_choose_percussion), options, default_instrument) { index: Int, value: Int ->
            opus_manager.set_percussion_instrument(value)
        }
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        val main = this.get_main()
        val editor_table = this.get_main().findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.scroll_to_position(x = beat)

        if (select) {
            val opus_manager = main.get_opus_manager()
            opus_manager.cursor_select_column(beat)
        }
    }

    fun shortcut_dialog() {
        val scroll_bar = SeekBar(this.activity)
        val opus_manager = this.get_main().get_opus_manager()
        scroll_bar.max = opus_manager.opus_beat_count - 1
        val cursor = opus_manager.cursor
        scroll_bar.progress = when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                cursor.beat
            }
            OpusManagerCursor.CursorMode.Range -> {
                cursor.range!!.first.beat
            }
            else -> {
                0
            }
        }
        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                opus_manager.cursor_select_column(p1, true)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })
        AlertDialog.Builder(this.activity).apply {
            setTitle("Jump to Beat")
            setView(scroll_bar)
            show()
        }
    }
}
