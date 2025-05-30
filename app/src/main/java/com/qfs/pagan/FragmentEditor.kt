package com.qfs.pagan
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.qfs.pagan.ContextMenu.ContextMenuChannel
import com.qfs.pagan.ContextMenu.ContextMenuColumn
import com.qfs.pagan.ContextMenu.ContextMenuControlLeaf
import com.qfs.pagan.ContextMenu.ContextMenuControlLeafB
import com.qfs.pagan.ContextMenu.ContextMenuControlLine
import com.qfs.pagan.ContextMenu.ContextMenuLeaf
import com.qfs.pagan.ContextMenu.ContextMenuLeafPercussion
import com.qfs.pagan.ContextMenu.ContextMenuLine
import com.qfs.pagan.ContextMenu.ContextMenuRange
import com.qfs.pagan.ContextMenu.ContextMenuView
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusPanEvent
import com.qfs.pagan.opusmanager.OpusReverbEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

class FragmentEditor : FragmentPagan<FragmentMainBinding>() {
    val view_model: EditorViewModel by viewModels()
    var active_context_menu: ContextMenuView? = null
    var keyboard_input_interface: KeyboardInputInterface? = null

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        val main = this.get_activity()
        main.setup_project_config_drawer()
        val opus_manager = main.get_opus_manager()
        this.keyboard_input_interface = KeyboardInputInterface(opus_manager)
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

        val main = this.get_activity()
        main.save_to_backup()

        val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_recycler.adapter != null) {
            (channel_recycler.adapter as ChannelOptionAdapter).clear()
            channel_recycler.adapter = null
        }

        super.onStop()
    }

    fun backup_position() {
        val main = this.get_activity()
        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        val (scroll_x, scroll_y) = editor_table.get_scroll_offset()

        val opus_manager = main.get_opus_manager()
        this.view_model.backup_undo_stack = opus_manager.history_cache.copy()
        this.view_model.scroll_x = scroll_x
        this.view_model.scroll_y = scroll_y
    }

    fun restore_view_model_position() {
        val main = this.get_activity()
        val editor_table = main.findViewById<EditorTable?>(R.id.etEditorTable)
        editor_table.precise_scroll(
            this.view_model.scroll_x,
            this.view_model.scroll_y
        )
    }

    override fun onDestroy() {
        // Editor table gets clears onDestroy because the fragment
        // can be stopped and started without destroying
        // and if it's not destroyed onViewStateRestored won't be called
        val main = this.get_activity()
        val editor_table = main.findViewById<EditorTable?>(R.id.etEditorTable)
        editor_table?.clear_column_map()
        editor_table?.clear()

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("scroll_x", this.view_model.scroll_x)
        outState.putInt("scroll_y", this.view_model.scroll_y)
        super.onSaveInstanceState(outState)
    }

    fun load_from_bkp() {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val bkp_json_path = "${main.applicationInfo.dataDir}/.bkp.json"
        val bytes = FileInputStream(bkp_json_path).readBytes()
        val backup_path: String = File("${main.applicationInfo.dataDir}/.bkp_path").readText()
        opus_manager.load(bytes, backup_path)
    }

    fun reload_from_bkp() {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val bkp_json_path = "${main.applicationInfo.dataDir}/.bkp.json"
        if (!File(bkp_json_path).exists()) {
            return
        }
        val bytes = FileInputStream(bkp_json_path).readBytes()
        val backup_path: String = File("${main.applicationInfo.dataDir}/.bkp_path").readText()

        opus_manager.reload(bytes, backup_path)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()

        val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)

        // SavedInstanceState may be created when the fragment isn't active, and save an empty state.
        // *NEED* to make sure it isn't empty before traversing this branch
        if (savedInstanceState != null) {
            this.view_model.scroll_x = savedInstanceState.getInt("scroll_x")
            this.view_model.scroll_y = savedInstanceState.getInt("scroll_y")
        } else if (!opus_manager.first_load_done) {
            // Navigate to (import / load/new)
            editor_table.visibility = View.VISIBLE
            return
        }

        editor_table.visibility = View.VISIBLE
        this.reload_from_bkp()


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
            this.view_model.backup_fragment_intent = Pair(IntentFragmentToken.Load, bundle)

            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.clear()
            val main = this.get_activity()
            main.loading_reticle_show(getString(R.string.reticle_msg_load_project))
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
            }

            thread {
                val path = bundle?.getString("PATH")
                if (path != null) {
                    main.get_opus_manager().load_path(path)
                }
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                this.view_model.backup_fragment_intent = null
                main.loading_reticle_hide()
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportMidi.name) { _, bundle: Bundle? ->
            val main = this.get_activity()
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.clear()
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
                        main.runOnUiThread {
                            if (!opus_manager.first_load_done) {
                                main.get_opus_manager().project_change_new()
                            } else {
                                this.reload_from_bkp()
                                editor_table.visibility = View.VISIBLE
                                this.restore_view_model_position()
                            }
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

        setFragmentResultListener(IntentFragmentToken.ImportGeneral.name) { _, bundle: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.clear()
            this.view_model.backup_fragment_intent = Pair(IntentFragmentToken.ImportGeneral, bundle)
            val main = this.get_activity()
            main.loading_reticle_show(getString(R.string.reticle_msg_import_project))
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
                this@FragmentEditor.clear_context_menu()
            }

            thread {
                val type: CompatibleFileType? = try {
                    bundle!!.getString("URI")?.let { path ->
                        main.get_file_type(path)
                    }
                } catch (e: Exception) {
                    null
                }

                var fallback_msg: String? = null
                try {
                    when (type) {
                        CompatibleFileType.Midi1 ->{
                            bundle!!.getString("URI")?.let { path ->
                                main.import_midi(path)
                            }
                        }
                        CompatibleFileType.Pagan -> {
                            bundle!!.getString("URI")?.let { path ->
                                main.import_project(path)
                            }
                        }
                        null -> {
                            fallback_msg = getString(R.string.feedback_file_not_found)
                        }
                    }
                } catch (e: Exception) {
                    fallback_msg = when (type!!) {
                        CompatibleFileType.Midi1 -> getString(R.string.feedback_midi_fail)
                        CompatibleFileType.Pagan -> getString(R.string.feedback_import_fail)
                    }
                }

                if (fallback_msg != null) {
                    val opus_manager = main.get_opus_manager()
                    // if Not Loaded, just create new and throw a message up
                    if (!opus_manager.first_load_done) {
                        opus_manager.project_change_new()
                    } else {
                        main.runOnUiThread {
                            this.reload_from_bkp()
                            editor_table.visibility = View.VISIBLE
                            this.restore_view_model_position()
                        }
                    }

                    this.get_activity().feedback_msg(fallback_msg)
                }

                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
                this.view_model.backup_fragment_intent = null
            }
        }

        setFragmentResultListener(IntentFragmentToken.ImportProject.name) { _, bundle: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.clear()
            val main = this.get_activity()
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
                        opus_manager.project_change_new()
                    } else {
                        main.runOnUiThread {
                            this.reload_from_bkp()
                            editor_table.visibility = View.VISIBLE
                            this.restore_view_model_position()
                        }
                    }

                    this.get_activity().feedback_msg(getString(R.string.feedback_import_fail))
                }
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
            }
        }

        setFragmentResultListener(IntentFragmentToken.MostRecent.name) { _, _ ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.clear()
            val main = this.get_activity()

            main.loading_reticle_show(getString(R.string.reticle_msg_load_project))

            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
            }

            thread {
                try {
                    this.load_from_bkp()
                } catch (e: Exception) {
                    val opus_manager = main.get_opus_manager()
                    // if Not Loaded, just create new and throw a message up
                    if (!opus_manager.first_load_done) {
                        opus_manager.project_change_new()
                    } else {
                        main.runOnUiThread {
                            this.reload_from_bkp()
                            editor_table.visibility = View.VISIBLE
                            this.restore_view_model_position()
                        }
                    }

                    this.get_activity().feedback_msg(getString(R.string.feedback_import_fail))
                }
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
            }
        }

        setFragmentResultListener(IntentFragmentToken.New.name) { _, _: Bundle? ->
            val editor_table = this.binding.root.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.clear()

            val main = this.get_activity()
            main.loading_reticle_show(getString(R.string.reticle_msg_new))
            main.runOnUiThread {
                editor_table?.visibility = View.INVISIBLE
            }
            thread {
                main.get_opus_manager().project_change_new()
                main.runOnUiThread {
                    editor_table?.visibility = View.VISIBLE
                }
                main.loading_reticle_hide()
            }
        }
    }

    fun refresh_context_menu() {
        this.active_context_menu?.refresh() ?: this.hide_context_menus()
    }

    private inline fun <reified T: ContextMenuView?> refresh_or_clear_context_menu(): Boolean {
        val llContextMenu = this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary)
        val llContextMenuSecondary = this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)

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
        this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary)?.visibility = GONE
        this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)?.visibility = GONE
    }
    fun on_show_context_menus(a: View, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int, i: Int): Boolean {
        val editor_table = this.get_activity().findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.force_scroll_to_cursor_vertical()
        return false
    }

    private fun show_context_menus() {
        val primary = this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary)
        primary.removeOnLayoutChangeListener(this::on_show_context_menus)
        primary.visibility = if (primary.isNotEmpty()) {
            primary.addOnLayoutChangeListener(this::on_show_context_menus)
            VISIBLE
        } else {
            GONE
        }

        val secondary = this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        secondary.removeOnLayoutChangeListener(this::on_show_context_menus)
        secondary.visibility = if (secondary.isNotEmpty()) {
            if (primary.isEmpty()) {
                secondary.addOnLayoutChangeListener(this::on_show_context_menus)
            }
            VISIBLE
        } else {
            GONE
        }
    }


    // fun scroll_to_cursor(cursor: OpusManagerCursor, force: Boolean = false) {
    //     val opus_manager = this.get_activity().get_opus_manager()
    //     val y = when (cursor.mode) {
    //         OpusManagerCursor.CursorMode.Line,
    //         OpusManagerCursor.CursorMode.Single -> {
    //             when (cursor.ctl_level) {
    //                 CtlLineLevel.Line -> opus_manager.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
    //                 CtlLineLevel.Channel -> opus_manager.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel)
    //                 CtlLineLevel.Global -> opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
    //                 null -> {
    //                     opus_manager.get_visible_row_from_ctl_line(
    //                         opus_manager.get_actual_line_index(
    //                             opus_manager.get_instrument_line_index(
    //                                 opus_manager.cursor.channel,
    //                                 opus_manager.cursor.line_offset
    //                             )
    //                         )
    //                     )
    //                 }
    //             }
    //         }
    //         OpusManagerCursor.CursorMode.Range -> {
    //             when (cursor.ctl_level) {
    //                 CtlLineLevel.Line -> opus_manager.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.range!!.second.channel, cursor.range!!.second.line_offset)
    //                 CtlLineLevel.Channel -> opus_manager.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.range!!.second.channel)
    //                 CtlLineLevel.Global ->  opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
    //                 null -> opus_manager.get_visible_row_from_ctl_line(
    //                     opus_manager.get_actual_line_index(
    //                         opus_manager.get_instrument_line_index(
    //                             cursor.range!!.second.channel,
    //                             cursor.range!!.second.line_offset
    //                         )
    //                     )
    //                 )
    //             }

    //         }
    //         OpusManagerCursor.CursorMode.Column,
    //         OpusManagerCursor.CursorMode.Unset -> null

    //         OpusManagerCursor.CursorMode.Channel -> {
    //             opus_manager.get_visible_row_from_ctl_line(
    //                 opus_manager.get_actual_line_index(
    //                     opus_manager.get_instrument_line_index(
    //                         opus_manager.cursor.channel,
    //                         0
    //                     )
    //                 )
    //             )
    //         }
    //     }

    //     val (beat, offset, offset_width) = when (cursor.mode) {
    //         OpusManagerCursor.CursorMode.Channel -> Triple(null, 0f, 1f)
    //         OpusManagerCursor.CursorMode.Line -> Triple(null, 0f, 1f)
    //         OpusManagerCursor.CursorMode.Column -> Triple(cursor.beat, 0f, 1f)
    //         OpusManagerCursor.CursorMode.Single -> {
    //             var tree = when (cursor.ctl_level) {
    //                 CtlLineLevel.Line -> opus_manager.get_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey())
    //                 CtlLineLevel.Channel -> opus_manager.get_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat)
    //                 CtlLineLevel.Global -> opus_manager.get_global_ctl_tree(cursor.ctl_type!!, cursor.beat)
    //                 null -> opus_manager.get_tree(cursor.get_beatkey())
    //             }

    //             var width = 1f
    //             var offset = 0f
    //             for (p in cursor.get_position()) {
    //                 width /= tree.size
    //                 offset += p * width
    //                 tree = tree[p]
    //             }
    //             Triple(cursor.beat, offset, width)
    //         }
    //         OpusManagerCursor.CursorMode.Range -> Triple(cursor.range!!.second.beat, 0f, 1f)
    //         OpusManagerCursor.CursorMode.Unset -> Triple(null, 0f, 1f)
    //     }

    //     // If the row is out of view, scrolls to it
    //     this.get_activity().runOnUiThread {
    //         val editor_table = this.get_activity().findViewById<EditorTable>(R.id.etEditorTable)
    //         editor_table.scroll_to_position(y = y, x = beat, offset = offset, offset_width = offset_width, force = force)
    //     }
    // }


    internal fun set_context_menu_control_line() {
        // KLUDGE: due to the Generics, i need a better way of checking type here. for now i'm forcing refresh
        this.clear_context_menu()

        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val channels = opus_manager.get_all_channels()

        val cursor = opus_manager.cursor
        val controller_set = when (cursor.ctl_level!!) {
            CtlLineLevel.Line -> {
                channels[cursor.channel].lines[cursor.line_offset].controllers
            }
            CtlLineLevel.Channel -> {
                val channel = cursor.channel
                channels[channel].controllers
            }
            CtlLineLevel.Global -> {
                opus_manager.controllers
            }
        }

        val widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> {
                val controller = controller_set.get_controller<OpusTempoEvent>(cursor.ctl_type!!)
                ControlWidgetTempo(controller.initial_event, cursor.ctl_level!!, true, main) { event: OpusTempoEvent ->
                    opus_manager.set_initial_event(event)
                }
            }
            ControlEventType.Volume -> {
                val controller = controller_set.get_controller<OpusVolumeEvent>(cursor.ctl_type!!)
                ControlWidgetVolume(controller.initial_event, cursor.ctl_level!!, true, main) { event: OpusVolumeEvent ->
                    opus_manager.set_initial_event(event)
                }
            }
            ControlEventType.Reverb -> {
                val controller = controller_set.get_controller<OpusReverbEvent>(cursor.ctl_type!!)
                ControlWidgetReverb(controller.initial_event, cursor.ctl_level!!, true, main) { event: OpusReverbEvent ->
                    opus_manager.set_initial_event(event)
                }
            }

            ControlEventType.Pan -> {
                val controller = controller_set.get_controller<OpusPanEvent>(cursor.ctl_type!!)
                ControlWidgetPan(controller.initial_event, cursor.ctl_level!!, true, main) { event: OpusPanEvent ->
                    opus_manager.set_initial_event(event)
                }
            }
        }


        this.active_context_menu = ContextMenuControlLine(
            widget,
            this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
            this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        )

        this.show_context_menus()
    }

    internal fun set_context_menu_line_control_leaf() {
        // KLUDGE: due to the Generics, i need a better way of checking type here. for now i'm forcing refresh
        this.clear_context_menu()

        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        val controller_set = opus_manager.get_active_active_control_set() ?: return

        val controller = controller_set.get_controller<OpusControlEvent>(cursor.ctl_type!!)
        val default = controller.get_latest_event(cursor.beat, cursor.get_position())?.copy() ?: controller.initial_event.copy()


        val (actual_beat, actual_position) = controller.get_blocking_position(cursor.beat, cursor.get_position()) ?: Pair(cursor.beat, cursor.get_position())
        val tree = controller.get_tree(actual_beat, actual_position)
        if (!tree.is_event()) {
            default.duration = 1
        }

        val widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> {
                ControlWidgetTempo(default as OpusTempoEvent, cursor.ctl_level!!, false, main) { event: OpusTempoEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }
            ControlEventType.Volume -> {
                ControlWidgetVolume(default as OpusVolumeEvent, cursor.ctl_level!!, false, main) { event: OpusVolumeEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }
            ControlEventType.Reverb -> {
                ControlWidgetReverb(default as OpusReverbEvent, cursor.ctl_level!!, false, main) { event: OpusReverbEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }

            ControlEventType.Pan -> {
                ControlWidgetPan(default as OpusPanEvent, cursor.ctl_level!!, false, main) { event: OpusPanEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }
        }

        this.active_context_menu = ContextMenuControlLeaf(
            widget,
            this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
            this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        )
        this.show_context_menus()
    }

    internal fun set_context_menu_line_control_leaf_b() {
        if (!this.refresh_or_clear_context_menu<ContextMenuControlLeafB>()) {
            this.active_context_menu = ContextMenuControlLeafB(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_range() {
        if (!this.refresh_or_clear_context_menu<com.qfs.pagan.ContextMenu.ContextMenuRange>()) {
            this.active_context_menu = ContextMenuRange(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_column() {
        if (this.get_activity().in_playback()) {
            this.clear_context_menu()
            return
        }

        if (!this.refresh_or_clear_context_menu<ContextMenuColumn>()) {
            this.active_context_menu = ContextMenuColumn(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_line() {
        if (!this.refresh_or_clear_context_menu<com.qfs.pagan.ContextMenu.ContextMenuLine>()) {
            this.active_context_menu = ContextMenuLine(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_channel() {
        if (!this.refresh_or_clear_context_menu<ContextMenuChannel>()) {
            this.active_context_menu = ContextMenuChannel(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }


    internal fun set_context_menu_leaf() {
        if (!this.refresh_or_clear_context_menu<com.qfs.pagan.ContextMenu.ContextMenuLeaf>()) {
            this.active_context_menu = ContextMenuLeaf(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_leaf_percussion() {
        if (!this.refresh_or_clear_context_menu<com.qfs.pagan.ContextMenu.ContextMenuLeafPercussion>()) {
            this.active_context_menu = ContextMenuLeafPercussion(
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.requireActivity().findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
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
        val spinner = view.findViewById<Spinner>(R.id.shortcut_spinner)!!

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        scroll_bar.max = opus_manager.length - 1
        scroll_bar.progress = this._get_start_column()

        title_text.text = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)
        title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = resources.getString(R.string.label_shortcut_scrollbar, p1)
                title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, p1)
                opus_manager.force_cursor_select_column(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog_builder = AlertDialog.Builder(activity, R.style.Theme_Pagan_Dialog)
        dialog_builder.setView(view)
        val dialog = dialog_builder.show()

        if (opus_manager.marked_sections.isEmpty()) {
            spinner.visibility = View.GONE
        } else {
            spinner.visibility = View.VISIBLE
            val keys = opus_manager.marked_sections.keys.toList().sorted()
            val items = List(keys.size + 1) { i: Int ->
                if (i == 0) {
                    activity.getString(R.string.jump_to_section)
                } else {
                    val section_name = opus_manager.marked_sections[keys[i - 1]]
                    if (section_name == null) {
                        getString(R.string.section_spinner_item, i, keys[i - 1])
                    } else {
                        "${keys[i - 1]}: ${section_name}"
                    }
                }
            }

            spinner.adapter = ArrayAdapter<String>(activity, R.layout.spinner_list, items)
            spinner.onItemSelectedListener = object: OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        opus_manager.force_cursor_select_column(keys[position - 1])
                        dialog.dismiss()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun _get_start_column(): Int {
        val opus_manager = this.get_activity().get_opus_manager()
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
                val editor_table = this.get_activity().findViewById<EditorTable>(R.id.etEditorTable)
                editor_table.get_first_visible_column_index()
            }
        }
    }
}
