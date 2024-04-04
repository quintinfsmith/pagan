package com.qfs.pagan
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.Transition
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

class FragmentEditor : FragmentPagan<FragmentMainBinding>() {
    val view_model: EditorViewModel by viewModels()
    private var _active_context_menu_index: ContextMenuFlag? = null
    var project_change_flagged = false
    enum class ContextMenuFlag {
        Leaf,
        LeafPercussion,
        Line,
        Column,
        Linking,
        ControlLeafLine,
        ControlLeafChannel,
        ControlLeafGlobal,
        ControlLineLine,
        ControlLineChannel,
        ControlLineGlobal
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    override fun onResume() {
        super.onResume()

        val main = this.get_main()
        main.setup_project_config_drawer()
        val opus_manager = main.get_opus_manager()
        val drawer = main.findViewById<DrawerLayout>(R.id.drawer_layout)
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            return
        }
       val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
       if (channel_recycler.adapter == null) {
           ChannelOptionAdapter(opus_manager, channel_recycler)
       }
       val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
       if (channel_adapter.itemCount == 0) {
           channel_adapter.setup()
       }
    }

    override fun onStart() {
        super.onStart()
        this._set_result_listeners()
        this._setup_interactors_leaf()
        this._setup_interactors_column()
        this._setup_interactors_line()
    }

    override fun onStop() {
        // Assign to view model on stop, will be destroyed onDestroy, so need to
        // essentially dup this in onSaveInstanceState
        val main = this.get_main()
        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        val (scroll_x, scroll_y) = editor_table.get_scroll_offset()

        val opus_manager = main.get_opus_manager()
        this.view_model.backup_undo_stack = opus_manager.history_cache.copy()
        this.view_model.coarse_x = scroll_x.first
        this.view_model.fine_x = scroll_x.second
        this.view_model.coarse_y = scroll_y.first
        this.view_model.fine_y = scroll_y.second

        main.save_to_backup()

        val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_recycler.adapter != null) {
            (channel_recycler.adapter as ChannelOptionAdapter).clear()
            channel_recycler.adapter = null
        }


        super.onStop()
    }

    override fun onDestroy() {
        // Editor table gets clears onDestroy because the fragment
        // can be stopped and started without destroying
        // and if it's not destroyed onViewStateRestored won't be called
        val main = this.get_main()
        val editor_table = main.findViewById<EditorTable?>(R.id.etEditorTable)
        editor_table?.clear()

        super.onDestroy()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("coarse_x", this.view_model.coarse_x)
        outState.putInt("fine_x", this.view_model.fine_x)
        outState.putInt("coarse_y", this.view_model.coarse_y)
        outState.putInt("fine_y", this.view_model.fine_y)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (this.project_change_flagged) {
            return
        }

        val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)

        // SavedInstanceState may be created when the fragment isn't active, and save an empty state.
        // *NEED* to make sure it isn't empty before traversing this branch
        if (savedInstanceState != null) {
            this.view_model.coarse_x = savedInstanceState.getInt("coarse_x")
            this.view_model.fine_x = savedInstanceState.getInt("fine_x")
            this.view_model.coarse_y = savedInstanceState.getInt("coarse_y")
            this.view_model.fine_y = savedInstanceState.getInt("fine_y")
        } else if (!opus_manager.first_load_done) {
            // Navigate to (import / load/new)
            editor_table.visibility = View.VISIBLE
            return
        }

        main.drawer_unlock()

        if (savedInstanceState != null) {
            val bkp_json_path = "${main.applicationInfo.dataDir}/.bkp.json"
            if (!File(bkp_json_path).exists()) {
               return
            }
            val bytes = FileInputStream(bkp_json_path).readBytes()
            val backup_path: String = File("${main.applicationInfo.dataDir}/.bkp_path").readText()

            opus_manager.reload(bytes, backup_path)
        } else {
            opus_manager.cursor_clear()
            editor_table.setup()
        }
        editor_table.visibility = View.VISIBLE
        editor_table.precise_scroll(
            this.view_model.coarse_x,
            this.view_model.fine_x,
            this.view_model.coarse_y,
            this.view_model.fine_y
        )

        // At the moment, can't save the history cache into a bundle, so restore it if
        // it exists, if not, too bad i guess
        if (this.view_model.backup_undo_stack != null) {
            opus_manager.history_cache = this.view_model.backup_undo_stack!!
            this.view_model.backup_undo_stack = null
        }


        main.setup_project_config_drawer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.clearContextMenu()

        val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.visibility = View.INVISIBLE

        /*
            KLUDGE ALERT.
            there's a middle state when orientation changes during a file select
            the fragment result gets processed on then the fragment gets reinit'd
            So this is how we re-pass any incomplete calls
         */
        if (this.view_model.backup_fragment_intent != null) {
            val (token, bundle) = this.view_model.backup_fragment_intent!!
            if (bundle != null) {
                this.setFragmentResult(token.name, bundle)
            }
            this.view_model.backup_fragment_intent = null
        }
    }

    private fun _set_result_listeners() {
        setFragmentResultListener(IntentFragmentToken.Load.name) { _, bundle: Bundle? ->
            this.view_model.backup_fragment_intent = Pair(IntentFragmentToken.ImportMidi, bundle)

            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_load_project))
            main.drawer_lock()
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
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
                main.drawer_unlock()
                this.project_change_flagged = false
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportMidi.name) { _, bundle: Bundle? ->
            val main = this.get_main()
            main.drawer_lock()
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            this.view_model.backup_fragment_intent = Pair(IntentFragmentToken.ImportMidi, bundle)
            main.loading_reticle_show(getString(R.string.reticle_msg_import_midi))
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
                this@FragmentEditor.clearContextMenu()
            }
            thread {
                val path = bundle?.getString("URI")
                if (path != null) {
                    try {
                        main.import_midi(path)
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
                main.drawer_unlock()
                main.loading_reticle_hide()
                this.project_change_flagged = false
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportProject.name) { _, bundle: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_import_project))
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
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
                this.project_change_flagged = false
            }
        }

        setFragmentResultListener(IntentFragmentToken.New.name) { _, _: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            val main = this.get_main()
            main.loading_reticle_show(getString(R.string.reticle_msg_new))
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
            }
            thread {
                main.get_opus_manager().new()
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
                this.project_change_flagged = false
            }
        }
    }

    fun reset_context_menu() {
        when (this._active_context_menu_index) {
            ContextMenuFlag.Leaf -> {
                this.set_context_menu_leaf()
            }
            ContextMenuFlag.LeafPercussion -> {
                this.set_context_menu_leaf_percussion()
            }
            ContextMenuFlag.Line -> {
                this.set_context_menu_line()
            }
            ContextMenuFlag.Column -> {
                this.set_context_menu_column()
            }
            ContextMenuFlag.Linking -> {
                this.setContextMenu_linking()
            }
            ContextMenuFlag.ControlLeafLine -> {
                this.set_context_menu_line_control_leaf()
            }
            else -> { }
        }
    }

    fun clearContextMenu() {
        this._active_context_menu_index = null
        val llContextCell = this.activity!!.findViewById<LinearLayout?>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout?>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout?>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View?>(R.id.llContextLink)
        val llContextLineCtlLeaf = this.activity!!.findViewById<View?>(R.id.llContextLineCtlLeaf)

        llContextCell?.visibility = View.GONE
        llContextRow?.visibility = View.GONE
        llContextCol?.visibility = View.GONE
        llContextLink?.visibility = View.GONE
        llContextLineCtlLeaf?.visibility = View.GONE
    }

    internal fun set_context_menu_line_control_leaf() {
        this._active_context_menu_index = ContextMenuFlag.ControlLeafLine
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        val llContextLineCtlLeaf = this.activity!!.findViewById<View>(R.id.llContextLineCtlLeaf)

        llContextCell.visibility = View.GONE
        llContextRow.visibility = View.GONE
        llContextCol.visibility = View.GONE
        llContextLink.visibility = View.GONE
        llContextLineCtlLeaf.visibility = View.VISIBLE
        //////////////////////////////////////////////
        val opus_manager = this.get_main().get_opus_manager()
        val cursor = opus_manager.cursor
        val ctl_tree = opus_manager.get_line_ctl_tree(
            cursor.ctl_type!!,
            BeatKey(
                cursor.channel,
                cursor.line_offset,
                cursor.beat
            ),
            cursor.position
        )

        val btn_transition = llContextLineCtlLeaf.findViewById<ButtonStd>(R.id.btnChooseTransition)
        val btn_duration = llContextLineCtlLeaf.findViewById<ButtonStd>(R.id.btnDuration)
        val btn_value = llContextLineCtlLeaf.findViewById<ButtonStd>(R.id.btnCtlAmount)

        if (!ctl_tree.is_event()) {
            btn_value.text = opus_manager.get_current_ctl_line_value(
                cursor.ctl_type!!,
                BeatKey(
                    cursor.channel,
                    cursor.line_offset,
                    cursor.beat
                ),
                cursor.position
            ).toString()
        } else {
            // TODO: Formatting
            btn_value.text = ctl_tree.event!!.value.toString()
        }

        if (!ctl_tree.is_event() || ctl_tree.event!!.transition == Transition.Instantaneous) {
            btn_duration.visibility = View.GONE
        } else {
            btn_duration.visibility = View.VISIBLE
        }

        btn_value.setOnClickListener {
            this.click_button_ctl_value()
        }

        btn_transition.setOnClickListener {
            this.click_button_ctl_transition()
        }

        btn_transition.text = if (!ctl_tree.is_event()) {
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

    internal fun setContextMenu_linking() {
        this._active_context_menu_index = ContextMenuFlag.Linking
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        val llContextLineCtlLeaf = this.activity!!.findViewById<View>(R.id.llContextLineCtlLeaf)

        llContextCell.visibility = View.GONE
        llContextRow.visibility = View.GONE
        llContextCol.visibility = View.GONE
        llContextLink.visibility = View.VISIBLE
        llContextLineCtlLeaf.visibility = View.GONE
        //////////////////////////////////////////////

        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val btnUnLink = llContextLink.findViewById<ImageView>(R.id.btnUnLink)
        val btnUnLinkAll = llContextLink.findViewById<ImageView>(R.id.btnUnLinkAll)
        val btnEraseSelection = llContextLink.findViewById<ImageView>(R.id.btnEraseSelection)

        val label = llContextLink.findViewById<TextView>(R.id.tvLinkLabel)
        val rgLinkMode = llContextLink.findViewById<RadioGroup?>(R.id.rgLinkMode)
        rgLinkMode.check(when (main.configuration.link_mode) {
            PaganConfiguration.LinkMode.LINK -> R.id.rbLinkModeLink
            PaganConfiguration.LinkMode.MOVE -> R.id.rbLinkModeMove
            PaganConfiguration.LinkMode.COPY -> R.id.rbLinkModeCopy
        })
        rgLinkMode?.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
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
                this.click_button_unlink()
            }
            if (!many_links) {
                btnUnLinkAll.visibility = View.GONE
            } else {
                btnUnLinkAll.visibility = View.VISIBLE
                btnUnLinkAll.setOnClickListener {
                    this.click_button_unlink_all()
                }
            }
        } else {
            btnUnLink.visibility = View.GONE
            btnUnLinkAll.visibility = View.GONE
        }

        btnEraseSelection.setOnClickListener {
            this.get_main().get_opus_manager().unset()
        }
    }

    fun set_context_menu_column() {
        this._active_context_menu_index = ContextMenuFlag.Column
        // TODO: Implement
    }

    fun set_context_menu_line() {
        this._active_context_menu_index = ContextMenuFlag.Line
        // TODO: Implement
    }

    private fun _setup_interactors_column() {
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)

        val btnInsertBeat = llContextCol.findViewById<ImageView>(R.id.btnInsertBeat)
        val btnRemoveBeat = llContextCol.findViewById<ImageView>(R.id.btnRemoveBeat)

        btnInsertBeat.setOnClickListener {
            this.click_button_insert_beat()
        }

        btnInsertBeat.setOnLongClickListener {
            this.long_click_button_insert_beat()
        }

        btnRemoveBeat.setOnClickListener {
            this.click_button_remove_beat()
        }

        btnRemoveBeat.setOnLongClickListener {
            this.long_click_button_remove_beat()
        }
    }

    private fun _setup_interactors_line() {
        // TODO: Implement
    }

    private fun _setup_interactors_leaf() {
        // TODO: Implement
    }

    internal fun set_context_menu_leaf() {
        this._active_context_menu_index = ContextMenuFlag.Leaf
        // TODO: Implement
    }


    internal fun set_context_menu_leaf_percussion() {
        this._active_context_menu_index = ContextMenuFlag.LeafPercussion
        val llContextCell = this.activity!!.findViewById<LinearLayout>(R.id.llContextCell)
        val llContextRow = this.activity!!.findViewById<LinearLayout>(R.id.llContextRow)
        val llContextCol = this.activity!!.findViewById<LinearLayout>(R.id.llContextCol)
        val llContextLink = this.activity!!.findViewById<View>(R.id.llContextLink)
        val llContextLineCtlLeaf = this.activity!!.findViewById<View>(R.id.llContextLineCtlLeaf)

        llContextCell.visibility = View.VISIBLE
        llContextRow.visibility = View.GONE
        llContextCol.visibility = View.GONE
        llContextLink.visibility = View.GONE
        llContextLineCtlLeaf.visibility = View.GONE
        //////////////////////////////////////////////

        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val btnSplit = llContextCell.findViewById<View>(R.id.btnSplit)
        btnSplit.visibility = View.VISIBLE

        val btnInsert = llContextCell.findViewById<View>(R.id.btnInsert)
        btnInsert.visibility = View.VISIBLE

        val btnUnset = llContextCell.findViewById<ImageView>(R.id.btnUnset)
        btnUnset.visibility = View.VISIBLE

        val rosRelativeOption = llContextCell.findViewById<RelativeOptionSelector>(R.id.rosRelativeOption)
        rosRelativeOption.visibility = View.GONE

        val nsOctave: NumberSelector = llContextCell.findViewById(R.id.nsOctave)
        nsOctave.visibility = View.GONE

        val nsOffset: NumberSelector = llContextCell.findViewById(R.id.nsOffset)
        nsOffset.visibility = View.GONE

        val btnDuration = llContextCell.findViewById<TextView>(R.id.btnDuration)
        val current_tree = opus_manager.get_tree()

        // If event exists, change relative mode, other wise use active relative mode
        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            btnUnset.setImageResource(R.drawable.unset)

            btnDuration.text = getString(R.string.label_duration, event.duration)
            btnDuration.visibility = View.VISIBLE
        } else {
            btnUnset.setImageResource(R.drawable.set_percussion)
            btnDuration.visibility = View.GONE
        }


        val btnRemove = llContextCell.findViewById<View>(R.id.btnRemove)
        if (opus_manager.cursor.get_position().isEmpty()) {
            btnRemove.visibility = View.GONE
        } else {
            btnRemove.visibility = View.VISIBLE
        }

    }

    private fun click_button_unlink() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.unlink_beat()
    }

    private fun click_button_unlink_all() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.clear_link_pool()
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
        scroll_bar.progress = this._get_start_column()

        title_text.text = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)
        title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = resources.getString(R.string.label_shortcut_scrollbar, p1)
                title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, p1)
                opus_manager.cursor_select_column(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog = AlertDialog.Builder(this.activity)
        dialog.setView(view)
        this.get_main()._adjust_dialog_colors(dialog.show())
    }

    private fun _get_start_column(): Int {
        val opus_manager = this.get_main().get_opus_manager()
        val cursor = opus_manager.cursor
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
