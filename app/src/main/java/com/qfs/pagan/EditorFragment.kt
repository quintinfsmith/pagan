package com.qfs.pagan
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.BaseLayer
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.concurrent.thread
class EditorFragment : PaganFragment<FragmentMainBinding>() {
    val view_model: EditorViewModel by viewModels()
    private var active_context_menu_index: ContextMenu? = null
    var test_flag = false
    enum class ContextMenu {
        Leaf,
        Line,
        Column,
        Linking
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    override fun onResume() {
        super.onResume()

        var main = this.get_main()
        val opus_manager = main.get_opus_manager()
        var channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_recycler.adapter == null) {
            ChannelOptionAdapter(opus_manager, channel_recycler)
        }
        var channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
        if (channel_adapter.itemCount == 0) {
            channel_adapter.setup()
        }
    }

    override fun onStart() {
        super.onStart()
        this.set_result_listeners()
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        // Assign to view model on stop, will be destroyed onDestroy, so need to
        // essentially dup this in onSaveInstanceState
        val editor_table = this.get_main().findViewById<EditorTable>(R.id.etEditorTable)
        val (scroll_x, scroll_y) = editor_table.get_scroll_offset()
        this.view_model.backup_undo_stack = this.get_main().get_opus_manager().history_cache.copy()
        this.view_model.coarse_x = scroll_x.first
        this.view_model.fine_x = scroll_x.second
        this.view_model.coarse_y = scroll_y.first
        this.view_model.fine_y = scroll_y.second

        val opus_manager = this.get_main().get_opus_manager()
        this.view_model.backup_json = Json.encodeToString(opus_manager.to_json()).toByteArray()
        this.view_model.backup_path = opus_manager.path

        var main = this.get_main()
        var channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_recycler.adapter != null) {
            (channel_recycler.adapter as ChannelOptionAdapter).clear()
        }

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this._binding == null) {
            return
        }
        outState.putInt("coarse_x", this.view_model.coarse_x)
        outState.putInt("fine_x", this.view_model.fine_x)
        outState.putInt("coarse_y", this.view_model.coarse_y)
        outState.putInt("fine_y", this.view_model.fine_y)
        outState.putByteArray("backup_json", this.view_model.backup_json)
        outState.putString("backup_path", this.view_model.backup_path)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
        val path: String?
        val coarse_x: Int
        val fine_x: Int
        val coarse_y: Int
        val fine_y: Int
        val backup_json: ByteArray?
        val backup_path: String?
        if (savedInstanceState != null) {
            // Orientation Change/Brought back from background
            coarse_x = savedInstanceState.getInt("coarse_x")
            fine_x = savedInstanceState.getInt("fine_x")
            coarse_y = savedInstanceState.getInt("coarse_y")
            fine_y = savedInstanceState.getInt("fine_y")
            backup_json = savedInstanceState.getByteArray("backup_json")
            backup_path = savedInstanceState.getString("backup_path")
        } else if (this.view_model.backup_json != null) {
            // Navigate Back,
            coarse_x = this.view_model.coarse_x
            fine_x = this.view_model.fine_x
            coarse_y = this.view_model.coarse_y
            fine_y = this.view_model.fine_y
            backup_json = this.view_model.backup_json
            backup_path = this.view_model.backup_path
        } else {
            // Navigate to (import / load/new)
            editor_table.visibility = View.VISIBLE
            return
        }

        val main = this.get_main()
        main.drawer_unlock()

        val opus_manager = main.get_opus_manager()
        opus_manager.load(backup_json!!, backup_path)
        editor_table.visibility = View.VISIBLE

        editor_table.precise_scroll(coarse_x, fine_x, coarse_y, fine_y)

        // At the moment, can't save the history cache into a bundle, so restore it if
        // it exists, if not, too bad i guess
        if (this.view_model.backup_undo_stack != null) {
            opus_manager.history_cache = this.view_model.backup_undo_stack!!
            this.view_model.backup_undo_stack = null
        }

        this.view_model.clear()


        main.setup_project_config_drawer()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.clearContextMenu()

        val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.visibility = View.GONE

        /*
            KLUDGE ALERT.
            there's a middle state when orientation changes during a file select
            the fragment result gets processed on then the fragment gets reinit'd
            So this is how we re-pass any incomplete calls
         */
        if (this.view_model.backup_fragment_intent != null) {
            var (token, bundle) = this.view_model.backup_fragment_intent!!
            if (bundle != null) {
                this.setFragmentResult(token.name, bundle)
            }
            this.view_model.backup_fragment_intent = null
        }
    }

    fun set_result_listeners() {
        setFragmentResultListener(IntentFragmentToken.Load.name) { _, bundle: Bundle? ->
            this.view_model.backup_fragment_intent = Pair(IntentFragmentToken.ImportMidi, bundle)

            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_load_project))

            main.runOnUiThread {
                editor_table?.visibility = View.GONE
            }

            thread {
                val path = bundle?.getString("PATH")
                if (path != null) {
                    main.get_opus_manager().load(path)
                }
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                this.view_model.backup_fragment_intent = null
                main.loading_reticle_hide()
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportMidi.name) { _, bundle: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            this.view_model.backup_fragment_intent = Pair(IntentFragmentToken.ImportMidi, bundle)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_import_midi))
            main.runOnUiThread {
                editor_table?.visibility = View.GONE
                this@EditorFragment.clearContextMenu()
            }
            thread {
                val path = bundle?.getString("URI")
                if (path != null) {
                    try {
                        main.import_midi(path)
                        //} catch (e: InvalidMIDIFile) {
                    } catch (e: Exception) {
                        val opus_manager = main.get_opus_manager()
                        if (!opus_manager.first_load_done) {
                            main.get_opus_manager().new()
                        }
                        main.feedback_msg(getString(R.string.feedback_midi_fail))
                    }
                }
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                this.view_model.backup_fragment_intent = null
                main.loading_reticle_hide()
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportProject.name) { _, bundle: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_import_project))
            main.runOnUiThread {
                editor_table?.visibility = View.GONE
            }
            thread {
                try {
                    bundle!!.getString("URI")?.let { path ->
                        main.import_project(path)
                    }
                } catch (e: Exception) {
                    val opus_manager = main.get_opus_manager()
                    // if Not Loaded, just create new and throw a message up
                    if (!opus_manager.first_load_done) {
                        opus_manager.new()
                    }

                    this.get_main().feedback_msg(getString(R.string.feedback_import_fail))
                }
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
            }
        }

        setFragmentResultListener(IntentFragmentToken.New.name) { _, _: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_new))
            main.runOnUiThread {
                editor_table?.visibility = View.GONE
            }
            thread {
                main.get_opus_manager().new()
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
            }
        }
    }


    fun reset_context_menu() {
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
        this.active_context_menu_index = null
        val llContextCell = this.activity!!.findViewById<LinearLayout?>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout?>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout?>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View?>(R.id.llContextLink)
        llContextCell?.visibility = View.GONE
        llContextRow?.visibility = View.GONE
        llContextCol?.visibility = View.GONE
        llContextLink?.visibility = View.GONE
    }

    internal fun setContextMenu_linking() {
        this.active_context_menu_index = ContextMenu.Linking
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        llContextCell.visibility = View.GONE
        llContextRow.visibility = View.GONE
        llContextCol.visibility = View.GONE
        llContextLink.visibility = View.VISIBLE
        //////////////////////////////////////////////
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val btnUnLink = llContextLink.findViewById<ImageView>(R.id.btnUnLink)
        val btnUnLinkAll = llContextLink.findViewById<ImageView>(R.id.btnUnLinkAll)

        val label = llContextLink.findViewById<TextView>(R.id.tvLinkLabel)
        val rgLinkMode = llContextLink.findViewById<RadioGroup?>(R.id.rgLinkMode)
        rgLinkMode.check(when (main.configuration.link_mode) {
            PaganConfiguration.LinkMode.LINK -> R.id.rbLinkModeLink
            PaganConfiguration.LinkMode.MOVE -> R.id.rbLinkModeMove
            PaganConfiguration.LinkMode.COPY -> R.id.rbLinkModeCopy
        })
        rgLinkMode?.setOnCheckedChangeListener { radioGroup: RadioGroup, button_id: Int ->
            main.configuration.link_mode = when (button_id) {
                R.id.rbLinkModeLink -> PaganConfiguration.LinkMode.LINK
                R.id.rbLinkModeMove -> PaganConfiguration.LinkMode.MOVE
                R.id.rbLinkModeCopy -> PaganConfiguration.LinkMode.COPY
                else -> PaganConfiguration.LinkMode.COPY
            }
            main.save_configuration()

            label?.text = when (button_id) {
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

        val (is_networked, many_links) = if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            label?.text = when (main.configuration.link_mode) {
                PaganConfiguration.LinkMode.LINK -> resources.getString(R.string.label_link_range)
                PaganConfiguration.LinkMode.MOVE -> resources.getString(R.string.label_move_range)
                PaganConfiguration.LinkMode.COPY ->  resources.getString(R.string.label_copy_range)
            }

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
            label?.text = when (main.configuration.link_mode) {
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
            btnUnLink.visibility = View.VISIBLE
            btnUnLink.setOnClickListener {
                this.interact_btnUnlink()
            }
            if (!many_links) {
                btnUnLinkAll.visibility = View.GONE
            } else {
                btnUnLinkAll.visibility = View.VISIBLE
                btnUnLinkAll.setOnClickListener {
                    this.interact_btnUnlinkAll()
                }
            }
        } else {
            btnUnLink.visibility = View.GONE
            btnUnLinkAll.visibility = View.GONE
        }

    }

    fun setContextMenu_column() {
        this.active_context_menu_index = ContextMenu.Column
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        llContextCell.visibility = View.GONE
        llContextRow.visibility = View.GONE
        llContextLink.visibility = View.GONE

        val main = this.get_main()
        if (main.in_playback()) {
            llContextCol.visibility = View.GONE
            return
        } else {
            llContextCol.visibility = View.VISIBLE
        }
        //////////////////////////////////////////////////////////

        val opus_manager = main.get_opus_manager()

        val btnInsertBeat = llContextCol.findViewById<ImageView>(R.id.btnInsertBeat)
        val btnRemoveBeat = llContextCol.findViewById<ImageView>(R.id.btnRemoveBeat)

        btnInsertBeat.setOnClickListener {
            val beat = opus_manager.cursor.beat
            opus_manager.insert_beat_at_cursor(1)
            opus_manager.cursor_select_column(beat + 1)
        }

        btnInsertBeat.setOnLongClickListener {
            main.dialog_number_input( getString(R.string.dlg_insert_beats), 1, 99) { count: Int ->
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
                if (beat >= opus_manager.beat_count) {
                    opus_manager.cursor_select_column(opus_manager.beat_count - 1)
                }
            } catch (e: BaseLayer.RemovingLastBeatException) {
                this.get_main().feedback_msg(getString(R.string.feedback_rm_lastbeat))
            }

            if (opus_manager.beat_count == 1) {
                btnRemoveBeat.visibility = View.GONE
            }
        }

        btnRemoveBeat.setOnLongClickListener {
            main.dialog_number_input(getString(R.string.dlg_remove_beats), 1, opus_manager.beat_count - 1) { count: Int ->
                val beat = opus_manager.cursor.beat
                opus_manager.remove_beat_at_cursor(count)
                if (beat >= opus_manager.beat_count) {
                    opus_manager.cursor_select_column(opus_manager.beat_count - 1)
                }

                if (opus_manager.beat_count == 1) {
                    btnRemoveBeat.visibility = View.GONE
                }
            }
            true
        }

        if (opus_manager.beat_count == 1) {
            btnRemoveBeat.visibility = View.GONE
        } else {
            btnRemoveBeat.visibility = View.VISIBLE
        }
    }

    fun setContextMenu_line() {
        this.active_context_menu_index = ContextMenu.Line
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        llContextCell.visibility = View.GONE
        llContextRow.visibility = View.VISIBLE
        llContextCol.visibility = View.GONE
        llContextLink.visibility = View.GONE
        ///////////////////////////////////////////

        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Row) {
            throw OpusManagerCursor.InvalidModeException(opus_manager.cursor.mode, OpusManagerCursor.CursorMode.Row)
        }

        val sbLineVolume =  llContextRow.findViewById<SeekBar>(R.id.sbLineVolume)
        val btnRemoveLine = llContextRow.findViewById<ImageView>(R.id.btnRemoveLine)
        val btnInsertLine = llContextRow.findViewById<ImageView>(R.id.btnInsertLine)
        val btnChoosePercussion: TextView = llContextRow.findViewById(R.id.btnChoosePercussion)

        if (opus_manager.get_visible_line_count() == 1) {
            btnRemoveLine.visibility = View.GONE
        } else {
            btnRemoveLine.visibility = View.VISIBLE
        }

        val channel = opus_manager.cursor.channel
        val line_offset = opus_manager.cursor.line_offset

        if (!opus_manager.is_percussion(channel)) {
            btnChoosePercussion.visibility = View.GONE
        } else {
            btnChoosePercussion.visibility = View.VISIBLE
            val instrument = opus_manager.get_percussion_instrument(line_offset)

            btnChoosePercussion.setOnClickListener {
                this.interact_btnChoosePercussion()
            }

            if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Need to call get_drum name to repopulate instrument list if needed
                main.get_drum_name(instrument)
                if (instrument < 10) {
                    btnChoosePercussion.text = "!0$instrument"
                } else {
                    btnChoosePercussion.text = "!$instrument"
                }
            } else {
                btnChoosePercussion.text = main.getString(
                    R.string.label_choose_percussion,
                    instrument,
                    main.get_drum_name(instrument) ?: "Drum Not Found"
                )
            }
        }

        if (opus_manager.channels[channel].size == 1) {
            btnRemoveLine.visibility = View.GONE
        } else {
            btnRemoveLine.visibility = View.VISIBLE
            btnRemoveLine.setOnClickListener {
                opus_manager.remove_line(1)
            }

            btnRemoveLine.setOnLongClickListener {
                val lines = opus_manager.channels[opus_manager.cursor.channel].size
                val max_lines = min(lines - 1, lines - opus_manager.cursor.line_offset)
                main.dialog_number_input(
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
            main.dialog_number_input(
                getString(R.string.dlg_insert_lines),
                1,
                9,
            ) { count: Int ->
                opus_manager.insert_line(count)
                opus_manager.cursor_select_row(channel, line_offset + count)
            }
            true
        }

        //if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        //    sbLineVolume.layoutParams.width = ((llContextMenu.measuredHeight - (btnRemoveLine.measuredHeight + btnInsertLine.measuredHeight)) * .6).toInt()
        //}

        sbLineVolume.progress = opus_manager.get_line_volume(channel, line_offset)
        sbLineVolume.contentDescription = resources.getString(R.string.label_volume_scrollbar, sbLineVolume.progress / sbLineVolume.max)
        sbLineVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                p0.contentDescription = resources.getString(R.string.label_volume_scrollbar, (p1 / p0.max))
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                opus_manager.set_line_volume(channel, line_offset, seekbar.progress)
            }
        })
    }

    internal fun setContextMenu_leaf() {
        this.active_context_menu_index = ContextMenu.Leaf
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        llContextCell.visibility = View.VISIBLE
        llContextRow.visibility = View.GONE
        llContextCol.visibility = View.GONE
        llContextLink.visibility = View.GONE
        //////////////////////////////////////////////
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val rosRelativeOption = llContextCell.findViewById<RelativeOptionSelector>(R.id.rosRelativeOption)

        val btnUnset = llContextCell.findViewById<ImageView>(R.id.btnUnset)
        val btnSplit = llContextCell.findViewById<View>(R.id.btnSplit)
        val btnRemove = llContextCell.findViewById<View>(R.id.btnRemove)
        val btnInsert = llContextCell.findViewById<View>(R.id.btnInsert)
        val btnDuration = llContextCell.findViewById<TextView>(R.id.btnDuration)

        val nsOctave: NumberSelector = llContextCell.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = llContextCell.findViewById(R.id.nsOffset)

        val current_tree = opus_manager.get_tree()

        // If event exists, change relative mode, other wise use active relative mode
        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            opus_manager.set_relative_mode(event)
        }


        if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
            nsOctave.visibility = View.GONE
            nsOffset.visibility = View.GONE
            rosRelativeOption.visibility = View.GONE

            if (!opus_manager.get_tree().is_event()) {
                btnUnset.setImageResource(R.drawable.set_percussion)
            } else {
                btnUnset.setImageResource(R.drawable.unset)
            }

            btnUnset.setOnClickListener {
                this.interact_btnUnset()
            }

        } else {
            if (main.configuration.relative_mode) {
                rosRelativeOption.visibility = View.VISIBLE
            } else {
                rosRelativeOption.visibility = View.GONE
            }
            nsOctave.visibility = View.VISIBLE
            nsOffset.visibility = View.VISIBLE
            nsOffset.set_max(opus_manager.radix - 1)
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
                btnUnset.setImageResource(R.drawable.unset)
            } else {
                nsOctave.unset_active_button()
                nsOffset.unset_active_button()
            }

            nsOffset.setOnChange(this::interact_nsOffset)
            nsOctave.setOnChange(this::interact_nsOctave)
            rosRelativeOption.setOnChange(this::interact_rosRelativeOption)
        }
        rosRelativeOption.setState(opus_manager.relative_mode, true)

        btnSplit.setOnClickListener {
            val beatkey = opus_manager.cursor.get_beatkey()
            val position = opus_manager.cursor.get_position().toMutableList()

            opus_manager.split_tree(2)

            position.add(0)
            opus_manager.cursor_select(beatkey, position)
        }

        btnSplit.setOnLongClickListener {
            main.dialog_number_input(getString(R.string.dlg_split), 2, 29) { splits: Int ->
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
            btnUnset.visibility = View.GONE
        } else {
            btnUnset.visibility = View.VISIBLE
            btnUnset.setOnClickListener {
                this.interact_btnUnset()
            }
        }

        if (opus_manager.cursor.get_position().isEmpty()) {
            btnRemove.visibility = View.GONE
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
            main.dialog_number_input(getString(R.string.dlg_insert), 1, 29) { count: Int ->
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
                main.dialog_number_input("Duration", 1, 99, default=event.duration) { value: Int ->
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
            btnDuration.visibility = View.VISIBLE
        } else {
            btnDuration.visibility = View.GONE
        }
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
                        ((preceding_event.note / opus_manager.radix) * opus_manager.radix) + progress
                    } else {
                        progress
                    }
                }
            }
        }

        val event = OpusEvent(
            value,
            opus_manager.radix,
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
                    (0 - progress) * opus_manager.radix
                }
                1 -> {
                    progress * opus_manager.radix
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        (progress * opus_manager.radix) + (preceding_event.note % opus_manager.radix)
                    } else {
                        (progress * opus_manager.radix)
                    }
                }
            }
        }

        val event = OpusEvent(
            value,
            opus_manager.radix,
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

        main.dialog_popup_menu(getString(R.string.dropdown_choose_percussion), options, default_instrument) { index: Int, value: Int ->
            opus_manager.set_percussion_instrument(value)
        }
    }

    fun shortcut_dialog() {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.dialog_shortcut,
                this.view as ViewGroup,
                false
            )

        val scroll_bar = view.findViewById<SeekBar>(R.id.shortcut_scrollbar)!!
        val title_text = view.findViewById<TextView>(R.id.shortcut_title)!!

        val opus_manager = this.get_main().get_opus_manager()
        scroll_bar.max = opus_manager.beat_count - 1
        scroll_bar.progress = this.get_start_column()

        title_text.text = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)
        title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = resources.getString(R.string.label_shortcut_scrollbar, p1)
                title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, p1)
                opus_manager.cursor_select_column(p1, true)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog = AlertDialog.Builder(this.activity)
        dialog.setView(view)
        dialog.show()
    }

    fun get_start_column(): Int {
        val opus_manager = this.get_main().get_opus_manager()
        var cursor = opus_manager.cursor
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                cursor.beat
            }
            OpusManagerCursor.CursorMode.Range -> {
                cursor.range!!.first.beat
            }
            else -> {
                val editor_table = this.get_main().findViewById<EditorTable>(R.id.etEditorTable)
                editor_table.get_first_visible_column_index()
            }
        }
    }
}
