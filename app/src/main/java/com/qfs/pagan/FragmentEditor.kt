package com.qfs.pagan
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.view.isNotEmpty
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.OpusManagerCursor
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

class FragmentEditor : FragmentPagan<FragmentMainBinding>() {
    val view_model: EditorViewModel by viewModels()
    var project_change_flagged = false
    var active_context_menu: ContextMenuView? = null

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
    }

    override fun onStop() {
        // Assign to view model on stop, will be destroyed onDestroy, so need to
        // essentially dup this in onSaveInstanceState
        this.backup_position()

        val main = this.get_main()
        main.save_to_backup()

        val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_recycler.adapter != null) {
            (channel_recycler.adapter as ChannelOptionAdapter).clear()
            channel_recycler.adapter = null
        }


        super.onStop()
    }

    fun backup_position() {
        val main = this.get_main()
        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        val (scroll_x, scroll_y) = editor_table.get_scroll_offset()

        val opus_manager = main.get_opus_manager()
        this.view_model.backup_undo_stack = opus_manager.history_cache.copy()
        this.view_model.coarse_x = scroll_x.first
        this.view_model.fine_x = scroll_x.second
        this.view_model.coarse_y = scroll_y.first
        this.view_model.fine_y = scroll_y.second

        // KLUDGE ALERT: RecyclerView loses position if fine offset is 0, so i need to offset it slightly
        if (scroll_x.second == 0) {
            this.view_model.fine_x = -1
            editor_table.precise_scroll(scroll_x.first, -1, scroll_y.first, scroll_y.second)
        }
    }

    fun restore_view_model_position() {
        val main = this.get_main()
        val editor_table = main.findViewById<EditorTable?>(R.id.etEditorTable)
        editor_table.precise_scroll(
            this.view_model.coarse_x,
            this.view_model.fine_x,
            this.view_model.coarse_y,
            this.view_model.fine_y
        )
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
        this.restore_view_model_position()

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
        this.clear_context_menu()

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
                this@FragmentEditor.clear_context_menu()
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

    fun refresh_context_menu() {
        this.active_context_menu?.refresh() ?: this.hide_context_menus()
    }

    private inline fun <reified T: ContextMenuView?> refresh_or_clear_context_menu(): Boolean {
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary)
        val llContextMenuSecondary = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)

        if (this.active_context_menu !is T) {
            llContextMenu.removeAllViews()
            llContextMenu.visibility = GONE
            llContextMenuSecondary.removeAllViews()
            llContextMenuSecondary.visibility = GONE
            this.active_context_menu = null
            return false
        }

        this.active_context_menu?.refresh()

        return true
    }

    fun clear_context_menu() {
        this.hide_context_menus()
        if (this.active_context_menu == null) {
            return
        }

        if (this.active_context_menu!!.primary != null) {
            if (this.active_context_menu!!.primary!!.parent != null) {
                (this.active_context_menu!!.primary!!.parent as ViewGroup).removeAllViews()
            }
        }

        if (this.active_context_menu!!.secondary != null) {
            if (this.active_context_menu!!.secondary!!.parent != null) {
                (this.active_context_menu!!.secondary!!.parent as ViewGroup).removeAllViews()
            }
        }

        this.active_context_menu = null
    }

    private fun hide_context_menus() {
        this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary)?.visibility = GONE
        this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)?.visibility = GONE
    }

    private fun show_context_menus() {
        val primary = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary)
        primary.visibility = if (primary.isNotEmpty()) {
            VISIBLE
        } else {
            GONE
        }

        val secondary = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        secondary.visibility = if (secondary.isNotEmpty()) {
            VISIBLE
        } else {
            GONE
        }
    }


    internal fun set_context_menu_control_line() {
        if (!this.refresh_or_clear_context_menu<ContextMenuControlLine>()) {
            this.active_context_menu = ContextMenuControlLine(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_line_control_leaf() {
        if (!this.refresh_or_clear_context_menu<ContextMenuControlLeaf>()) {
            this.active_context_menu = ContextMenuControlLeaf(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_line_control_leaf_b() {
        if (!this.refresh_or_clear_context_menu<ContextMenuControlLeafB>()) {
            this.active_context_menu = ContextMenuControlLeafB(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_linking() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLink>()) {
            this.active_context_menu = ContextMenuLink(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_column() {
        if (this.get_main().in_playback()) {
            this.clear_context_menu()
            return
        }

        if (!this.refresh_or_clear_context_menu<ContextMenuColumn>()) {
            this.active_context_menu = ContextMenuColumn(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_line() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLine>()) {
            this.active_context_menu = ContextMenuLine(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_leaf() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLeaf>()) {
            this.active_context_menu = ContextMenuLeaf(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
        }
    }

    internal fun set_context_menu_leaf_percussion() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLeafPercussion>()) {
            this.active_context_menu = ContextMenuLeafPercussion(
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.activity!!.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
            this.show_context_menus()
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
