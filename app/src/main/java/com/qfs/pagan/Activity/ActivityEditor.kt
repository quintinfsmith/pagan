package com.qfs.pagan.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.MidiController
import com.qfs.apres.MidiPlayer
import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.soundfont2.Riff
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.json.JSONHashMap
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionAdapter
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionRecycler
import com.qfs.pagan.EditorTable
import com.qfs.pagan.HexEditText
import com.qfs.pagan.MidiFeedbackDispatcher
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganBroadcastReceiver
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PlaybackDevice
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.R
import com.qfs.pagan.TuningMapRecycler
import com.qfs.pagan.TuningMapRecyclerAdapter
import com.qfs.pagan.databinding.ActivityEditorBinding
import com.qfs.pagan.enumerate
import com.qfs.pagan.numberinput.RangedFloatInput
import com.qfs.pagan.numberinput.RangedIntegerInput
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelEditorState
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max

class ActivityEditor : PaganActivity() {
    companion object {
        init {
            System.loadLibrary("pagan")
        }
    }

    val editor_view_model: ViewModelEditorController by this.viewModels()
    val view_model_ui_state: ViewModelEditorState by this.viewModels()

    private var _initial_load = true // Used to prevent save dialog from popping up on first load/new/import
    // flag to indicate that the landing page has been navigated away from for navigation management
    private var _integer_dialog_defaults = HashMap<String, Int>()
    private var _float_dialog_defaults = HashMap<String, Float>()
    private var _virtual_input_device = MidiPlayer()
    private lateinit var _midi_interface: MidiController
    //private var _sample_handle_manager: SampleHandleManager? = null
    // private var _feedback_sample_manager: SampleHandleManager? = null
    private var _midi_playback_device: PlaybackDevice? = null
    private var _midi_feedback_dispatcher = MidiFeedbackDispatcher()

    private lateinit var _binding: ActivityEditorBinding
    private var _options_menu: Menu? = null
    private var _forced_title_text: String? = null
    private var _blocker_scroll_y: Float? = null
    private var broadcast_receiver = PaganBroadcastReceiver()
    private var receiver_intent_filter = IntentFilter("com.qfs.pagan.CANCEL_EXPORT_WAV")

    // Notification shiz -------------------------------------------------
    var NOTIFICATION_ID = 0
    val CHANNEL_ID = "com.qfs.pagan" // TODO: Use String Resource
    private var _notification_channel: NotificationChannel? = null
    private var _active_notification: NotificationCompat.Builder? = null
    // -------------------------------------------------------------------
    var active_project: Uri?
        get() = this.editor_view_model.active_project
        set(value) {
            this.editor_view_model.active_project = value
        }

    internal var result_launcher_settings =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.getStringExtra(EXTRA_ACTIVE_PROJECT)?.toUri()?.also { uri ->
                    this.active_project = uri
                }
            }
        }

    internal var result_launcher_import =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.also { uri ->
                    this.handle_uri(uri)
                }
            }
        }




    override fun onPause() {
        this.playback_stop()
        this.unregisterReceiver(this.broadcast_receiver)
        this.playback_stop_midi_output()
        this._midi_interface.close_connected_devices()
        this._binding.toolbar.hideOverflowMenu()
        super.onPause()
    }

    // TODO: Rewrite this withour fragment
    //override fun onKeyDown(key_code: Int, event: KeyEvent?): Boolean {
    //    val active_fragment = this.get_active_fragment()
    //    val cancel_super = if (event != null) {
    //        when (active_fragment) {
    //            is FragmentEditor ->
    //                try {
    //                    active_fragment.keyboard_input_interface?.input(key_code, event) ?: false
    //                } catch (e: Exception) {
    //                    true
    //                }
    //            else -> false
    //        }
    //    } else {
    //        false
    //    }

    //    return if (cancel_super) {
    //        true
    //    } else {
    //        super.onKeyDown(key_code, event)
    //    }
    //}

    // Check if the soundfont was removed
    fun soundfont_file_check() {
        if (this.configuration.soundfont == null) return

        if (this.get_soundfont_uri() == null) {
            this.disable_soundfont()
            this.update_menu_options()
        }
    }

    override fun onResume() {
        this.loading_reticle_hide()
        super.onResume()
        this.drawer_lock()

        this.registerReceiver(
            this.broadcast_receiver,
            this.receiver_intent_filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RECEIVER_NOT_EXPORTED
            } else {
                0
            }
        )

        this.recheck_active_midi_device()
        this.update_title_text()
    }

    fun recheck_active_midi_device() {
        this.editor_view_model.active_midi_device?.let {
            if (this.is_connected_to_physical_device()) {
                this.editor_view_model.playback_state_midi = PlaybackState.Ready
                this._midi_interface.open_output_device(it)
            }  else {
                this.set_active_midi_device(null)
            }
        }

        // Second check here if midi device wasn't found
        if (this.editor_view_model.active_midi_device == null) {
            if (this.editor_view_model.playback_device != null) {
                this.editor_view_model.playback_state_soundfont = PlaybackState.Ready
            }
            this.soundfont_file_check()
        }
    }


    fun save_to_backup() {
        this.view_model.project_manager.save_to_backup(
            this.get_opus_manager(),
            this.active_project
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Can't reliably put json in outstate. there is a size limit
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        outState.putInt("x", editor_table.get_scroll_x())
        outState.putInt("y", editor_table.get_scroll_y())
        this.save_to_backup()
        super.onSaveInstanceState(outState)
    }

    fun refresh(x: Int, y: Int) {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()

        this.get_opus_manager().project_refresh()
        this.runOnUiThread {
            editor_table?.scroll(x, y)
        }
    }

    fun setup_new() {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()
        this.get_opus_manager().project_change_new()
        this.on_project_change_new()
    }

    fun load_project(uri: Uri) {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()

        val input_stream = this.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText().toByteArray(Charsets.UTF_8)

        reader.close()
        input_stream?.close()

        this.get_opus_manager().load(content) {
            this.active_project = uri
        }
    }

    fun load_from_bkp() {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()
        val opus_manager = this.get_opus_manager()
        val project_manager = this.view_model.project_manager
        val (backup_uri, bytes) = project_manager.read_backup()

        opus_manager.load(bytes) {
            this.active_project = backup_uri
        }
    }

    private fun handle_uri(uri: Uri) {
        val type: CompatibleFileType? = try {
            this.get_file_type(uri)
        } catch (_: Exception) {
            null
        }

        val inner_callback: ((Uri) -> Unit) = when (type) {
            CompatibleFileType.Midi1 -> { uri -> this.import_midi(uri) }
            CompatibleFileType.Pagan -> { uri -> this.import_project(uri) }
            else -> { _ -> throw FileNotFoundException(uri.toString()) }
        }

        this.dialog_save_project {
            thread {
                this.loading_reticle_show()
                this.runOnUiThread {
                    this.findViewById<EditorTable>(R.id.etEditorTable).visibility = View.GONE
                }

                val fallback_msg = try {
                    inner_callback(uri)
                    null
                } catch (_: Exception) {
                    when (type) {
                        CompatibleFileType.Midi1 -> this.getString(R.string.feedback_midi_fail)
                        CompatibleFileType.Pagan -> this.getString(R.string.feedback_import_fail)
                        null -> this.getString(R.string.feedback_file_not_found)
                    }
                }

                if (fallback_msg != null) {
                    this.feedback_msg(fallback_msg)
                }

                this.loading_reticle_hide()
                this.runOnUiThread {
                    this.clear_forced_title()
                    this.findViewById<EditorTable>(R.id.etEditorTable).visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this._midi_interface = object : MidiController(this, false) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.update_menu_options()
                }
            }

            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.update_menu_options()
                    if (device_info == this@ActivityEditor.editor_view_model.active_midi_device) {
                        this@ActivityEditor.set_active_midi_device(null)
                    }
                }
            }
        }

        this._midi_interface.connect_virtual_input_device(this._virtual_input_device)

        // Listens for SongPositionPointer (provided by midi) and scrolls to that beat
        this._midi_interface.connect_virtual_output_device(object : VirtualMidiOutputDevice {
            val FORCE_SCROLL = true
            override fun onSongPositionPointer(event: SongPositionPointer) {
                if (event.get_beat() >= this@ActivityEditor.get_opus_manager().length) {
                    return
                }
                this@ActivityEditor.get_opus_manager().cursor_select_column(event.get_beat())
                // Force scroll here, cursor_select_column doesn't scroll if the column is already visible
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.findViewById<EditorTable>(R.id.etEditorTable)?.scroll_to_position(x = event.get_beat(), force = this.FORCE_SCROLL)
                }
            }
        })

        this._midi_interface.connect_virtual_input_device(this._midi_feedback_dispatcher)

        this.requestedOrientation = this.configuration.force_orientation
        AppCompatDelegate.setDefaultNightMode(this.configuration.night_mode)

        this._binding = ActivityEditorBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)

        val toolbar = this._binding.toolbar
        toolbar.background = null

        toolbar.setOnLongClickListener {
            this.get_action_interface().set_project_name_and_notes()
            true
        }

       // this.editor_view_model.action_interface.attach_activity(this)
        // this.editor_view_model.opus_manager.attach_activity(this)

        //////////////////////////////////////////
        /// if (this.editor_view_model.active_midi_device == null) {
        ///     this.get_soundfont_uri()?.let { uri ->
        ///         val sf_file = DocumentFile.fromSingleUri(this, uri) ?: return@let
        ///         if (!sf_file.exists()) return@let

        ///         try {
        ///             this.editor_view_model.audio_interface.set_soundfont(SoundFont(this, uri))
        ///             this.editor_view_model.playback_device?.attach_activity(this)
        ///         } catch (_: Riff.InvalidRiff) {
        ///             this.configuration.soundfont = null
        ///             // Invalid soundfont somehow set
        ///         }
        ///     }
        /// }

        // this.update_channel_instruments(this.get_opus_manager().channels.size)
        ///////////////////////////////////////////

        val drawer_layout = this.findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        drawer_layout.addDrawerListener(
            object : ActionBarDrawerToggle( this, drawer_layout, R.string.drawer_open, R.string.drawer_close) {
                override fun onDrawerOpened(drawerView: View) {
                    this@ActivityEditor.get_action_interface().track(ActionTracker.TrackedAction.DrawerOpen)
                    val channel_recycler = this@ActivityEditor.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter == null) {
                        ChannelOptionAdapter(
                            this@ActivityEditor.get_opus_manager(),
                            channel_recycler
                        )
                    }
                    super.onDrawerOpened(drawerView)

                    val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                    if (channel_adapter.itemCount == 0) {
                        channel_adapter.setup()
                    }

                    this@ActivityEditor.playback_stop()
                    this@ActivityEditor.playback_stop_midi_output()
                    this@ActivityEditor.drawer_unlock() // So the drawer can be closed with a swipe
                }

                override fun onDrawerClosed(drawerView: View) {
                    this@ActivityEditor.get_action_interface().track(ActionTracker.TrackedAction.DrawerClose)
                    super.onDrawerClosed(drawerView)
                    this@ActivityEditor.drawer_lock() // so the drawer can't be opened with a swipe
                }
            }
        )

        if (savedInstanceState != null) {
            // if the activity is forgotten, the opus_manager is be uninitialized
        } else if (this.intent.getBooleanExtra("load_backup", false)) {
            this.load_from_bkp()
        } else if (this.intent.data == null) {
            this.setup_new()
        } else if (this.get_project_manager().contains(this.intent.data!!)) {
            this.load_project(this.intent.data!!)
        } else {
            this.handle_uri(this.intent.data!!)
        }

        this._initial_load = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        this._options_menu = menu

        Handler(Looper.getMainLooper()).post {
            val view: View = this.findViewById<View?>(R.id.itmPlay) ?: return@post
            view.setOnLongClickListener {
                this@ActivityEditor.handle_play_press(true)
                true
            }
        }

        val output = super.onCreateOptionsMenu(menu)
        this.update_menu_options()
        return output
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.itmPlay) {
            this.playback_stop()
            this.playback_stop_midi_output()
        }

        when (item.itemId) {
            android.R.id.home -> {
                this.drawer_open()
            }

            R.id.itmNewProject -> {
                this.dialog_save_project {
                    this.get_action_interface().new_project()
                }
            }

            R.id.itmLoadProject -> {
                this.dialog_load_project { uri: Uri ->
                    this.dialog_save_project {
                        val editor_table = this.findViewById<View?>(R.id.etEditorTable)
                        this.get_opus_manager().cursor_clear()
                        thread {
                            this.loading_reticle_show()
                            this.runOnUiThread { editor_table?.visibility = View.GONE }
                            this.get_action_interface().load_project(uri)
                            this.runOnUiThread { editor_table?.visibility = View.VISIBLE }
                            this.loading_reticle_hide()
                        }
                    }
                }
            }

            R.id.itmImportMidi -> {
                this.result_launcher_import.launch(
                    Intent().apply {
                        this.action = Intent.ACTION_GET_CONTENT
                        this.type = "*/*" // Allow all, for some reason the emulators don't recognize midi files
                    }
                )
            }

            R.id.itmUndo -> {
                this.get_action_interface().apply_undo()
            }

            R.id.itmPlay -> {
                this.handle_play_press(false)
            }

            R.id.itmSettings -> {
            }
            R.id.itmAbout -> {
            }
            R.id.itmDebug -> {
                this.save_actions()
                this.feedback_msg("SAVED ACTIONS")
            }
            R.id.itmMidiDeviceInfo -> {
                this.dialog_midi_device_management()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun handle_play_press(long_press: Boolean) {
        if (this.editor_view_model.active_midi_device == null) {
            when (this.editor_view_model.playback_state_soundfont) {
                PlaybackState.Ready -> {
                    this.playback_start(long_press)
                }

                PlaybackState.Queued,
                PlaybackState.Playing -> {
                    this.playback_stop()
                }

                else -> {}
            }
        } else {
            when (this.editor_view_model.playback_state_midi) {
                PlaybackState.Ready -> {
                    this.playback_start_midi_output(long_press)
                }
                PlaybackState.Queued,
                PlaybackState.Playing -> {
                    this.playback_stop_midi_output()
                }
                else -> { /* pass */ }
            }
        }
    }

    fun open_settings() {
        this.loading_reticle_show()
        this.result_launcher_settings.launch(
            Intent(this, ActivitySettings::class.java).apply {
                this@ActivityEditor.active_project?.let {
                    this.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                }
            }
        )
    }

    fun open_about() {
        this.loading_reticle_show()
        this.startActivity(Intent(this, ActivityAbout::class.java))
    }

    private fun _project_save() {
        this.loading_reticle_show()
        this.active_project = this.get_project_manager().save(this.get_opus_manager(), this.active_project, this.configuration.indent_json)
        this.feedback_msg(this.getString(R.string.feedback_project_saved))
        this.update_menu_options()
        this.findViewById<View?>(R.id.btnDeleteProject)?.isEnabled = true
        this.findViewById<View?>(R.id.btnCopyProject)?.isEnabled = true

        this.drawer_close()
        this.loading_reticle_hide()
    }

    fun project_save() {
    }

    fun project_move_to_copy() {
        this.dialog_save_project {
            val opus_manager = this.get_opus_manager()
            val old_title = opus_manager.project_name
            val new_title: String? = if (old_title == null) {
                null
            } else {
                "$old_title (Copy)"
            }
            opus_manager.project_name = new_title

            this.active_project = null
            this.update_title_text()
            this.feedback_msg(this.getString(R.string.feedback_on_copy))

            this.findViewById<View>(R.id.btnDeleteProject).isEnabled = false
            this.findViewById<View>(R.id.btnCopyProject).isEnabled = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun _enable_blocker_view() {
        this.findViewById<LinearLayout?>(R.id.llClearOverlay)?.let { blocker_view ->
            if (blocker_view.isVisible) {
                return
            }

            blocker_view.setOnTouchListener { _, motion_event ->
                /* Allow Scrolling on the y axis when scrolling in the main_recycler */
                if (motion_event?.action == 1) {
                    this._blocker_scroll_y = null
                } else if (motion_event?.action == MotionEvent.ACTION_MOVE) {
                    val scroll_view = this.findViewById<EditorTable>(R.id.etEditorTable)
                    val rel_y = (motion_event.y - scroll_view.y)
                    scroll_view.scrollBy(0, ((this._blocker_scroll_y ?: rel_y) - rel_y).toInt())
                    this._blocker_scroll_y = rel_y
                }
                true
            }

            blocker_view.visibility = View.VISIBLE
            this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun _disable_blocker_view() {
        // Assume playback states have been updated BEFORE calling this function
        // Need to check both since they both use the same blocker, though it should never be an issue
        if (this.editor_view_model.in_playback()) return
        this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay) ?: return
        blocker_view.visibility = View.GONE
    }

    private fun playback_start(loop_playback: Boolean = false) {
        if (!this.editor_view_model.update_playback_state_soundfont(PlaybackState.Queued)) {
            this.feedback_msg(this.getString(R.string.playback_failed))
            return
        }

        this._enable_blocker_view()
        this.runOnUiThread {
            this.set_playback_button(R.drawable.baseline_play_disabled_24)
            this.force_title_text(this.getString(R.string.reticle_msg_start_playback))
            this.loading_reticle_show()
        }

        var start_point = this.get_working_column()
        if (start_point >= this.get_opus_manager().length - 1) {
            start_point = 0
        }
        // Currently, Midi2.0 output is not supported. will be needed for N-radix projects
        thread {
            // May Not need to set instruments at beginning of playback, commenting for now
            //val opus_manager = this.get_opus_manager()
            //opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            //    val (bank, program) = channel.get_instrument()
            //    opus_manager.sample_handle_manager?.select_bank(channel.midi_channel, bank)
            //    opus_manager.sample_handle_manager?.change_program(channel.midi_channel, program)
            //}

            this.editor_view_model.playback_device?.set_clip_same_line_release(this.configuration.clip_same_line_release)
            this.editor_view_model.playback_device?.play_opus(start_point, loop_playback)
        }
    }

    private fun playback_start_midi_output(loop_playback: Boolean = false) {
        if (!this.editor_view_model.update_playback_state_midi(PlaybackState.Queued)) {
            this.feedback_msg(this.getString(R.string.playback_failed))
            return
        }

        if (this.editor_view_model.active_midi_device == null) {
            this.feedback_msg(this.getString(R.string.midi_device_unset))
            return
        }

        this.force_title_text(this.getString(R.string.reticle_msg_start_playback))
        this.loading_reticle_show()
        this._enable_blocker_view()

        var start_point = this.get_working_column()
        val opus_manager = this.get_opus_manager()
        if (start_point >= opus_manager.length - 1) {
            start_point = 0
        }

        val midi = opus_manager.get_midi(start_point, include_pointers = true)

        this.loading_reticle_hide()
        this.runOnUiThread {
            this.clear_forced_title()
            this.set_playback_button(if (loop_playback) R.drawable.icon_pause_loop else R.drawable.icon_pause)
        }

        if (!this.editor_view_model.update_playback_state_midi(PlaybackState.Playing)) {
            this.restore_midi_playback_state()
            return
        }

        thread {
            try {
                this._midi_interface.open_output_device(this.editor_view_model.active_midi_device!!)
                this._virtual_input_device.play_midi(midi, loop_playback) {
                    this.runOnUiThread {
                        this.playback_stop_midi_output()
                    }
                }
            } catch (_: IOException) {
                this.runOnUiThread {
                    this.playback_stop_midi_output()
                }
            }
        }
    }

    internal fun playback_stop() {
        if (this.editor_view_model.update_playback_state_soundfont(PlaybackState.Stopping)) {
            this.loading_reticle_hide()
            this.clear_forced_title()
            this.editor_view_model.playback_device?.kill()
        }
    }

    internal fun playback_stop_midi_output() {
        if (this.editor_view_model.update_playback_state_midi(PlaybackState.Stopping)) {
            this.loading_reticle_hide()
            this.clear_forced_title()
            this._virtual_input_device.stop()
            this.restore_midi_playback_state()
        }
    }

    fun restore_midi_playback_state() {
        if (this.editor_view_model.update_playback_state_midi(PlaybackState.Ready)) {
            this.runOnUiThread {
                this.set_playback_button(R.drawable.icon_play)
                this._disable_blocker_view()
            }
        }
    }

    // Ui Wrappers ////////////////////////////////////////////
    private fun drawer_close() {
    }

    private fun drawer_open() {
    }

    fun drawer_lock() {
        this._binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun drawer_unlock() {
        try {
            this._binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            this.findViewById<LinearLayout>(R.id.config_drawer)?.refreshDrawableState()
        } catch (_: UninitializedPropertyAccessException) {
            // pass, if it's not initialized, it's not locked
        }
    }

    fun update_title_text() {
        this.set_title_text(
            if (this._forced_title_text != null) {
                this._forced_title_text!!
            } else {
                this.get_opus_manager().project_name ?: this.getString(R.string.untitled_opus)
            }
        )
    }

    fun set_title_text(new_text: String) {
        this.runOnUiThread {
            this._binding.toolbar.title = new_text
        }
    }

    fun force_title_text(msg: String) {
        this._forced_title_text = msg
        this.update_title_text()
    }

    fun clear_forced_title() {
        this._forced_title_text = null
        this.update_title_text()
    }

    override fun on_reticle_hide() {
        super.on_reticle_hide()
        this.clear_forced_title()
    }

    override fun on_reticle_show() {
        super.on_reticle_show()
        this.force_title_text(this.getString(R.string.reticle_msg_load))
    }

    fun update_menu_options() {
        val options_menu = this._options_menu ?: return
        val show_midi_devices = (this._midi_interface.output_devices_connected() && this.get_opus_manager().is_tuning_standard())

        options_menu.findItem(R.id.itmLoadProject).isVisible = this.has_projects_saved()
        options_menu.findItem(R.id.itmPlay).isVisible = (this.get_soundfont() != null) || (this.editor_view_model.active_midi_device != null)
        options_menu.findItem(R.id.itmMidiDeviceInfo).isVisible = show_midi_devices
        options_menu.findItem(R.id.itmDebug).isVisible = this.is_debug_on()
    }

    fun setup_project_config_drawer() {
        val opus_manager = this.get_opus_manager()

        this.findViewById<MaterialButton?>(R.id.btnChangeProjectName)?.setOnClickListener {
            this.get_action_interface().set_project_name_and_notes()
        }
        this.findViewById<MaterialButton?>(R.id.btnRadix)?.setOnClickListener {
            this.get_action_interface().set_tuning_table_and_transpose()
        }
        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            this.get_action_interface().insert_channel(opus_manager.channels.size)
        }
        this.findViewById<View>(R.id.btnAddPercussion).setOnClickListener {
            this.get_action_interface().insert_percussion_channel(opus_manager.channels.size)
        }

        this.setup_project_config_drawer_export_button()
        this.findViewById<View>(R.id.btnSaveProject).setOnClickListener {
            if (!it.isEnabled) return@setOnClickListener
            this.get_action_interface().save()
        }

        this.findViewById<View>(R.id.btnSaveProject).setOnLongClickListener {
            if (!it.isEnabled) return@setOnLongClickListener false
            this.export_project()
            true
        }


        val file_exists = this.active_project != null

        this.findViewById<View?>(R.id.btnDeleteProject)?.let { button ->
            button.isEnabled = file_exists
            button.setOnClickListener {
                if (it.isEnabled) {
                    this.active_project?.let { uri ->
                        this.dialog_delete_project(uri)
                    }
                }
            }
        }

        this.findViewById<View?>(R.id.btnCopyProject)?.let { button ->
            button.isEnabled = file_exists
            button.setOnClickListener {
                if (!it.isEnabled) return@setOnClickListener
                this.get_action_interface().project_copy()
            }
        }

        this.findViewById<View>(R.id.export_progress_cancel).setOnClickListener {
            this.export_wav_cancel()
        }
    }

    internal fun _build_dialog_title_view(text: String): TextView {
        val output = TextView(ContextThemeWrapper(this, R.style.dialog_title))
        output.text = text
        return output
    }

    internal fun setup_project_config_drawer_export_button() {
        this.findViewById<ConstraintLayout>(R.id.clExportProgress)?.let { export_progress_wrapper ->
            export_progress_wrapper.visibility = if (!this.editor_view_model.is_exporting()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }


    // Ui Wrappers End ////////////////////////////////////////

    private fun _get_default_drum_options(): List<Pair<String, Int>> {
        val midi_drums = this.resources.getStringArray(R.array.midi_drums)
        return List(midi_drums.size) { i: Int ->
            Pair(midi_drums[i]!!, i + 27)
        }
    }

    fun update_channel_instrument(midi_channel: Int, instrument: Pair<Int, Int>) {
        val (midi_bank, midi_program) = instrument
        this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
        this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))
        this.editor_view_model.audio_interface.update_channel_preset(midi_channel, midi_bank, midi_program)
    }

    // Update peripheral device instruments, ie feedback device and midi devices
    // NOTE: Not conforming to GM
    fun update_channel_instruments(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (index == null) {
            for ((i, channel) in opus_manager.channels.enumerate()) {
                val midi_channel = opus_manager.get_midi_channel(i)
                val (midi_bank, midi_program) = channel.get_preset()
                this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
                this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))
                this.editor_view_model.audio_interface.update_channel_preset(midi_channel, midi_bank, midi_program)
            }
        } else {
            val opus_channel = opus_manager.get_channel(index)
            this.update_channel_instrument(
                opus_manager.get_midi_channel(index),
                opus_channel.get_preset()
            )
        }
    }

    fun get_opus_manager(): OpusLayerInterface {
        return this.editor_view_model.opus_manager
    }

    fun play_event(channel: Int, event_value: Int, velocity: Float = .5F) {
        if (event_value < 0) return // No sound to play

        val opus_manager = this.get_opus_manager()
        val midi_channel = opus_manager.get_midi_channel(channel)

        val radix = opus_manager.get_radix()
        val (note, bend) = if (opus_manager.is_percussion(channel)) { // Ignore the event data and use percussion map
            Pair(event_value + 27, 0)
        } else {
            val octave = event_value / radix
            val offset = opus_manager.tuning_map[event_value % radix]

            val transpose_offset = 12.0 * opus_manager.transpose.first.toDouble() / opus_manager.transpose.second.toDouble()
            val std_offset = 12.0 * offset.first.toDouble() / offset.second.toDouble()

            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            val new_note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21

            Pair(new_note, bend)
        }

        if (note > 127) return

        if (this.editor_view_model.audio_interface.has_soundfont()) {
            this.editor_view_model.audio_interface.play_feedback(midi_channel, note, bend, (velocity * 127F).toInt() shl 8)
        } else {
            try {
              //  this._midi_feedback_dispatcher.play_note(
              //      midi_channel,
              //      note,
              //      bend,
              //      (velocity * 127F).toInt(),
              //      !opus_manager.is_tuning_standard()
              //  )
            } catch (_: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But i'm sure that'll come back to bite me
            }
        }
    }

    fun import_project(uri: Uri) {
        this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.get_opus_manager().load(bytes)
            this.active_project = null
        }
    }

    fun import_midi(uri: Uri) {
        val bytes = this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).readBytes()
        } ?: throw InvalidMIDIFile(uri.toString())

        val midi = try {
            Midi.Companion.from_bytes(bytes)
        } catch (_: Exception) {
            throw InvalidMIDIFile(uri.toString())
        }

        val opus_manager = this.get_opus_manager()
        opus_manager.project_change_midi(midi)
        val filename = this.parse_file_name(uri)
        opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: this.getString(R.string.default_imported_midi_title))
        opus_manager.clear_history()
        this.active_project = null
    }

    fun get_supported_preset_names(): HashMap<Pair<Int, Int>, String> {
        return HashMap()//this.view_model_ui_state.available_preset_names ?: this.get_general_midi_preset_names()
    }

    fun get_general_midi_preset_names(): HashMap<Pair<Int, Int>, String> {
        val output = HashMap<Pair<Int, Int>, String>()
        var program = 0
        for (name in this.resources.getStringArray(R.array.general_midi_presets)) {
            output[Pair(0, program++)] = name
        }
        return output
    }

    fun set_soundfont() {
        val file_path = this.configuration.soundfont
        if (file_path == null) {
            this.disable_soundfont()
            return
        }

        // Failed to change playback_state
        if (!this.editor_view_model.update_playback_state_soundfont(PlaybackState.Ready)) return

        val soundfont_directory = this.get_soundfont_directory()
        var soundfont_file = soundfont_directory
        for (segment in file_path.split("/")) {
            soundfont_file = soundfont_file.findFile(segment) ?: throw FileNotFoundException()
        }

        if (!soundfont_file.exists()) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg(this.getString(R.string.soundfont_not_found))
            throw FileNotFoundException()
        }

        try {
            val soundfont = SoundFont(this, soundfont_file.uri)
            this.editor_view_model.set_soundfont(soundfont)
        } catch (_: Riff.InvalidRiff) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg(this.getString(R.string.invalid_soundfont))
            return
        } catch (_: SoundFont.InvalidSoundFont) {
            // Possible if user puts the sf2 in their files manually
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg("Invalid Soundfont")
            return
        }

        this.editor_view_model.get_soundfont()?.let {
            //this.view_model_ui_state.populate_preset_names(it)
        }

        this.reinit_playback_device()
        this.connect_feedback_device()
        this.update_channel_instruments()
        this.runOnUiThread {
            this.setup_project_config_drawer_export_button()
            this.findViewById<ChannelOptionRecycler?>(R.id.rvActiveChannels)?.notify_soundfont_changed()
            when (this.get_opus_manager().cursor.mode) {
                CursorMode.Line,
                CursorMode.Channel -> { }
                else -> {}
            }
        }
    }

    fun get_soundfont(): SoundFont? {
        return this.editor_view_model.audio_interface.soundfont
    }

    fun disable_soundfont() {
        // TODO: Check this logic
        //if (!this.editor_view_model.update_playback_state_soundfont(PlaybackState.NotReady)) return
        this.editor_view_model.unset_soundfont()
        this.view_model_ui_state.unset_soundfont()
    }

    fun get_ui_facade(): ViewModelEditorState {
        return this.view_model_ui_state
    }

    fun dialog_color_picker(initial_color: Int, callback: (Int?) -> Unit) {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.color_picker,
                this._binding.root,
                false
            )

        val flColorDisplay = viewInflated.findViewById<FrameLayout>(R.id.flColorDisplay)
        val sbRed = viewInflated.findViewById<SeekBar>(R.id.sbRed)
        val sbGreen = viewInflated.findViewById<SeekBar>(R.id.sbGreen)
        val sbBlue = viewInflated.findViewById<SeekBar>(R.id.sbBlue)
        val rniRed = viewInflated.findViewById<RangedIntegerInput>(R.id.rniRed)
        val rniGreen = viewInflated.findViewById<RangedIntegerInput>(R.id.rniGreen)
        val rniBlue = viewInflated.findViewById<RangedIntegerInput>(R.id.rniBlue)
        val hex_value = viewInflated.findViewById<HexEditText>(R.id.hexValue)

        hex_value.setText("%02x".format(initial_color.red) + "%02x".format(initial_color.green) + "%02x".format(initial_color.blue))
        rniRed.set_value(initial_color.red)
        rniGreen.set_value(initial_color.green)
        rniBlue.set_value(initial_color.blue)

        sbRed.progress = initial_color.red
        sbGreen.progress = initial_color.green
        sbBlue.progress = initial_color.blue

        var lockout = false
        rniRed.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) return
                lockout = true
                sbRed.progress = p0.toString().toInt()
                lockout = false
            }
        })
        rniGreen.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) return
                lockout = true
                sbGreen.progress = p0.toString().toInt()
                lockout = false
            }
        })
        rniBlue.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) return
                lockout = true
                sbBlue.progress = p0.toString().toInt()
                lockout = false
            }
        })

        val seekbar_listener = object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (lockout) return
                lockout = true
                when (p0) {
                    sbRed -> rniRed.set_value(p1)
                    sbGreen -> rniGreen.set_value(p1)
                    sbBlue -> rniBlue.set_value(p1)
                }
                val new_color = Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0)
                flColorDisplay.setBackgroundColor(new_color)
                lockout = false
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) { }
        }

        sbRed.setOnSeekBarChangeListener(seekbar_listener)
        sbGreen.setOnSeekBarChangeListener(seekbar_listener)
        sbBlue.setOnSeekBarChangeListener(seekbar_listener)

        hex_value.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) { }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) { }

            override fun afterTextChanged(s: Editable?) {
                val string = s.toString()
                if (lockout || string.length == 6) return

                val red = string.substring(0, 2).toInt(16)
                val green = string.substring(2, 4).toInt(16)
                val blue = string.substring(4, 6).toInt(16)

                lockout = true
                rniRed.set_value(red)
                rniGreen.set_value(green)
                rniBlue.set_value(blue)
                sbRed.progress = red
                sbGreen.progress = green
                sbBlue.progress = blue
                flColorDisplay.setBackgroundColor(Color.rgb(red, green, blue))
                lockout = false
            }
        })

        flColorDisplay.setBackgroundColor(Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0))
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(this.getString(R.string.dlg_title_set_line_color))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val new_color = Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0)
                if (new_color != initial_color) {
                    callback(new_color)
                }
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(this.getString(R.string.color_picker_clear)) { dialog, _ ->
                callback(null)
                dialog.dismiss()
            }
            .show()
    }

    fun dialog_text_popup(title: String, default: String? = null, callback: (String) -> Unit) {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.text_input,
                this._binding.root,
                false
            )

        val text_input: EditText = viewInflated.findViewById(R.id.etText)
        text_input.setText(default ?: "")

        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                callback(text_input.text.toString())
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    fun dialog_name_and_notes_popup(default: Pair<String, String>? = null, callback: (String, String) -> Unit) {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.text_name_change,
                this._binding.root,
                false
            )

        val project_name_input: EditText = viewInflated.findViewById(R.id.etProjectName)
        project_name_input.setText(default?.first ?: "")

        val project_notes_input: EditText = viewInflated.findViewById(R.id.etProjectNotes)
        project_notes_input.setText(default?.second ?: "")

        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                callback(project_name_input.text.toString(), project_notes_input.text.toString())
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // TODO: fix code duplication in dialog_float/integer_input
    internal fun dialog_float_input(title: String, min_value: Float, max_value: Float, default: Float? = null, callback: (value: Float) -> Unit ) {
        val coerced_default_value = default ?: (this._float_dialog_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_float,
                this.window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedFloatInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setCustomTitle(this._build_dialog_title_view(title))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this._float_dialog_defaults[title] = output_value
                callback(output_value)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()

        number_input.set_range(min_value, max_value)
        number_input.setText("$coerced_default_value")
        number_input.setOnClickListener {
            number_input.selectAll()
        }

        number_input.value_set_callback = { value: Float? ->
            callback(value ?: coerced_default_value)
            dialog.dismiss()
        }

        number_input.requestFocus()
        number_input.selectAll()
    }

    internal fun dialog_number_input(title: String, min_value: Int, max_value: Int, default: Int? = null, callback: (value: Int) -> Unit ) {
        val coerced_default_value = default ?: (this._integer_dialog_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                this.window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedIntegerInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this._integer_dialog_defaults[title] = output_value
                callback(output_value)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()

        number_input.set_range(min_value, max_value)
        number_input.setText("$coerced_default_value")
        number_input.setOnClickListener {
            number_input.selectAll()
        }

        number_input.value_set_callback = { value: Int? ->
            callback(value ?: coerced_default_value)
            dialog.dismiss()
        }

        number_input.requestFocus()
        number_input.selectAll()
    }

    private fun needs_save(): Boolean {
        val opus_manager = this.get_opus_manager()

        if (this.active_project == null) return !opus_manager.history_cache.is_empty()
        if (DocumentFile.fromSingleUri(this, this.active_project!!)?.exists() != true) return true

        val input_stream = this.contentResolver.openInputStream(this.active_project!!)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content: ByteArray = reader.readText().toByteArray(Charsets.UTF_8)

        val other = OpusLayerBase()
        other.load(content)

        reader.close()
        input_stream?.close()

        return (opus_manager as OpusLayerBase) != other
    }

    fun dialog_save_project(callback: (Boolean) -> Unit) {
        if (this._initial_load) {
            callback(false)
        } else if (this.needs_save()) {
            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setTitle(R.string.dialog_save_warning_title)
                .setCancelable(true)
                .setPositiveButton(this.getString(R.string.dlg_confirm)) { dialog, _ ->
                    this@ActivityEditor.project_save()
                    dialog.dismiss()
                    callback(true)
                }
                .setNegativeButton(this.getString(R.string.dlg_decline)) { dialog, _ ->
                    dialog.dismiss()
                    callback(false)
                }
                .show()
        } else {
            callback(false)
        }
    }

    fun get_default_export_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return this.getString(R.string.default_export_name, now.format(formatter))
    }

    fun get_export_name(): String {
        val reserved_chars = "|\\?*<\":>+[]/'"
        var base_name: String = this.get_opus_manager().project_name ?: this.get_default_export_name()
        for (c in reserved_chars) {
            base_name = base_name.replace("$c", "_")
        }
        return base_name
    }
    fun export_wav_cancel() {
        this.editor_view_model.cancel_export()
    }

    fun export_midi_check() {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.get_percussion_channels().size > 1) {
            val text_view = TextView(this)
            text_view.text = this.getString(R.string.multiple_kit_warning)

            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setTitle(R.string.generic_warning)
                .setView(text_view)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    this.export_midi()
                    dialog.dismiss()
                }
                .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } else {
            this.export_midi()
        }
    }

    fun export_midi() {
    }

    fun export_project() {
    }

    fun reinit_playback_device() {
        this.editor_view_model.destroy_playback_device()

        if (this.get_soundfont() != null) {
            /*
             * TODO: Put the ignore envelope/lfo option somewhere better.
             * I don't think it should be in apres if theres a reasonable way to avoid it
             */
            this.editor_view_model.audio_interface.reset()
            this.editor_view_model.create_playback_device()
        }
    }

    fun in_playback(): Boolean {
        return PlaybackState.Playing in listOf(this.editor_view_model.playback_state_soundfont, this.editor_view_model.playback_state_midi)
    }

    fun has_notification_permission(): Boolean {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED )
    }

    private fun getNotificationPermission(): Boolean {
        if (! this.has_notification_permission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        return this.has_notification_permission()
    }

    fun set_playback_button(drawable: Int) {
        val play_pause_button = this._options_menu?.findItem(R.id.itmPlay) ?: return
        play_pause_button.icon = ContextCompat.getDrawable(this, drawable)
    }

    fun get_working_column(): Int {
        val cursor = this.get_opus_manager().cursor
        return when (cursor.mode) {
            CursorMode.Single,
            CursorMode.Column -> {
                cursor.beat
            }
            CursorMode.Range -> {
                cursor.get_ordered_range()!!.first.beat
            }
            else -> {
                val editor_table = this.findViewById<EditorTable?>(R.id.etEditorTable)
                editor_table?.get_first_visible_column_index() ?: 0
            }
        }
    }

    fun is_connected_to_physical_device(): Boolean {
        return this._midi_interface.output_devices_connected()
    }

    fun disconnect_feedback_device() {
        this.editor_view_model.audio_interface.disconnect_feedback_device()
    }

    fun connect_feedback_device() {
        if (this.get_soundfont() == null) return
        this.editor_view_model.audio_interface.connect_feedback_device()
    }

    fun get_notification(): NotificationCompat.Builder? {
        if (!this.has_notification_permission()) return null

        if (this._active_notification == null) {
            this.get_notification_channel()

            val cancel_export_flag = "com.qfs.pagan.CANCEL_EXPORT_WAV"
            val intent = Intent()
            intent.setAction(cancel_export_flag)
            intent.setPackage(this.packageName)

            val pending_cancel_intent = PendingIntent.getBroadcast(
                this,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, this.CHANNEL_ID)
                .setContentTitle(this.getString(R.string.export_wav_notification_title, this.get_opus_manager().project_name ?: "Untitled Project"))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.small_logo_rowan)
                .setSilent(true)
                .addAction(R.drawable.baseline_cancel_24, this.getString(android.R.string.cancel), pending_cancel_intent)

            this._active_notification = builder
        }

        return this._active_notification!!
    }

    fun get_notification_channel(): NotificationChannel? {
        return if (!this.has_notification_permission()) {
            null
        } else if (this._notification_channel == null) {
            val notification_manager = NotificationManagerCompat.from(this)
            // Create the NotificationChannel.
            val name = this.getString(R.string.export_wav_file_progress)
            val descriptionText = this.getString(R.string.export_wav_notification_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(this.CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notification_manager.createNotificationChannel(mChannel)
            mChannel
        } else {
            this._notification_channel!!
        }
    }

    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = this.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            this.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(200, 5))
        }
    }


    fun get_file_type(uri: Uri): CompatibleFileType {
        return this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val test_bytes = ByteArray(4)
            FileInputStream(it.fileDescriptor).read(test_bytes)
            if (test_bytes.contentEquals("MThd".toByteArray())) {
                CompatibleFileType.Midi1
            } else {
                CompatibleFileType.Pagan
            }
        } ?: throw FileNotFoundException(uri.toString())
    }

    fun get_action_interface(): ActionTracker {
        return this.editor_view_model.action_interface
    }

    fun save_actions() {
        val generated_code = this.get_action_interface().to_json().to_string()
        val timestamp = System.currentTimeMillis()
        val file_name = "${this.dataDir}/generated_$timestamp.json"

        val file = File(file_name)
        file.writeText(generated_code)
        this.get_action_interface().clear()
    }

    override fun on_crash() {
        if (this.is_debug_on()) {
            this.save_actions()
        }
        this.save_to_backup()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    internal fun dialog_popup_selection_offset() {
        val view_inflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_note_offset,
                this.window.decorView.rootView as ViewGroup,
                false
            )

        val np_octave = view_inflated.findViewById<NumberPicker>(R.id.npOctave)
        np_octave.maxValue = 14
        np_octave.minValue = 0
        np_octave.value = 7
        np_octave.wrapSelectorWheel = false
        np_octave.setFormatter { value: Int ->
            "${value - 7}"
        }

        val np_offset = view_inflated.findViewById<NumberPicker>(R.id.npOffset)
        val max_value = (this.get_opus_manager().get_radix() - 1)
        np_offset.maxValue = (max_value * 2)
        np_offset.minValue = 0
        np_offset.value = max_value
        np_offset.wrapSelectorWheel = false
        np_offset.setFormatter { value: Int ->
            "${value - max_value}"
        }

        this._popup_active = true
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setView(view_inflated)
            .setTitle(R.string.dialog_adjust_selection)
            .setOnDismissListener {
                this._popup_active = false
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val opus_manager = this.get_opus_manager()
                val radix = opus_manager.get_radix()
                val octave = np_octave.value - 7
                val offset = np_offset.value - (radix - 1)
                val real_delta = (octave * radix) + offset
                opus_manager.offset_selection(real_delta)

                this.get_action_interface().track(ActionTracker.TrackedAction.AdjustSelection, listOf(real_delta))
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // vv Formerly Fragment Functions ---------------------------------------------------------




    fun shortcut_dialog() {
        val view = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_shortcut,
                this._binding.root as ViewGroup,
                false
            )

        val scroll_bar = view.findViewById<SeekBar>(R.id.shortcut_scrollbar)!!
        val title_text = view.findViewById<TextView>(R.id.shortcut_title)!!
        val spinner = view.findViewById<Spinner>(R.id.shortcut_spinner)!!

        val opus_manager = this.get_opus_manager()
        scroll_bar.max = opus_manager.length - 1

        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        scroll_bar.progress = editor_table.get_first_visible_column_index()


        title_text.text = this.resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)
        title_text.contentDescription = this.resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = this@ActivityEditor.resources.getString(R.string.label_shortcut_scrollbar, p1)
                title_text.contentDescription = this@ActivityEditor.resources.getString(R.string.label_shortcut_scrollbar, p1)
                opus_manager.force_cursor_select_column(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog_builder = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
        dialog_builder.setView(view)
        val dialog = dialog_builder.show()

        if (opus_manager.marked_sections.isEmpty()) {
            spinner.visibility = View.GONE
        } else {
            spinner.visibility = View.VISIBLE
            val keys = opus_manager.marked_sections.keys.toList().sorted()
            val items = List(keys.size + 1) { i: Int ->
                if (i == 0) {
                    this.getString(R.string.jump_to_section)
                } else {
                    val section_name = opus_manager.marked_sections[keys[i - 1]]
                    if (section_name == null) {
                        this.getString(R.string.section_spinner_item, i, keys[i - 1])
                    } else {
                        "${keys[i - 1]}: $section_name"
                    }
                }
            }

            spinner.adapter = ArrayAdapter<String>(this, R.layout.spinner_list, items)
            spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
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


    // ^^ Formerly Fragment Functions ---------------------------------------------------------
    fun dialog_tuning_table() {
        val opus_manager = this.get_opus_manager()

        val viewInflated = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_tuning_map,
                this._binding.root,
                false
            )

        val etTranspose = viewInflated.findViewById<RangedIntegerInput>(R.id.etTranspose)
        etTranspose.set_range(0)
        etTranspose.set_value(opus_manager.transpose.first)

        val etTransposeRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etTransposeRadix)
        etTransposeRadix.set_range(1)
        etTransposeRadix.set_value(opus_manager.transpose.second)

        val rvTuningMap = viewInflated.findViewById<TuningMapRecycler>(R.id.rvTuningMap)
        rvTuningMap.adapter = TuningMapRecyclerAdapter(opus_manager.tuning_map.clone())


        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(R.string.dlg_tuning)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val tuning_map = (rvTuningMap.adapter as TuningMapRecyclerAdapter).tuning_map
                val transpose = Pair(etTranspose.get_value() ?: 0, etTransposeRadix.get_value() ?: tuning_map.size)
                this.get_action_interface()._track_tuning_map_and_transpose(tuning_map, transpose)
                opus_manager.set_tuning_map_and_transpose(tuning_map, transpose)
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()

        val default_value = opus_manager.tuning_map.size

        viewInflated.findViewById<RangedIntegerInput?>(R.id.etRadix)?.let { input_view ->
            input_view.set_value(default_value)
            input_view.set_range(
                this.resources.getInteger(R.integer.minimum_octave_size),
                this.resources.getInteger(R.integer.maximum_octave_size)
            )
            input_view.value_set_callback = { new_radix: Int? ->
                rvTuningMap.reset_tuning_map(new_radix)
            }
        }
    }

    override fun on_paganconfig_change(original: PaganConfiguration) {
        super.on_paganconfig_change(original)

        if (this.editor_view_model.active_midi_device == null) {
            if (this.configuration.soundfont != original.soundfont) {
                this.set_soundfont()
            } else if (this.configuration.sample_rate != original.sample_rate && this.configuration.soundfont != null) {
                this.set_soundfont()
            }
        }

        this.update_menu_options()
    }

    override fun on_project_delete(uri: Uri) {
        // TODO: Track
        this.drawer_close()
        super.on_project_delete(uri)
        this.update_menu_options()
        if (this.active_project == uri) {
            this.view_model.project_manager.delete_backup()
            this.setup_new()
        }
    }

    fun dialog_midi_device_management() {
        val options = mutableListOf<Triple<MidiDeviceInfo?, Int?, String>>(
            Triple(null, null, this.getString(R.string.device_menu_default_name))
        )

        for (device_info in this._midi_interface.poll_output_devices()) {
            options.add(
                Triple(
                    device_info,
                    null,
                    device_info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: this.getString( R.string.unknown_midi_device, device_info.id )
                )
            )
        }

        this.dialog_popup_menu(this.getString(R.string.playback_device), options, this.editor_view_model.active_midi_device) { i: Int, device: MidiDeviceInfo? ->
            this.set_active_midi_device(device)
        }
    }

    fun get_bottom_padding(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val projection_metrics = this.getSystemService(WindowManager::class.java).maximumWindowMetrics
            projection_metrics.bounds.height()
        } else {
            val display_metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            this.windowManager.defaultDisplay.getMetrics(display_metrics)
            display_metrics.heightPixels
        } / 3
    }

    fun get_effect_transition_icon(transition: EffectTransition): Int {
        return when (transition) {
            EffectTransition.Instant -> R.drawable.icon_transition_immediate
            EffectTransition.Linear -> R.drawable.icon_transition_linear
            EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
            EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
        }
    }

    fun set_active_midi_device(device_info: MidiDeviceInfo?) {
        val current_device_info = this.editor_view_model.active_midi_device
        if (device_info == current_device_info) return

        when (this.editor_view_model.playback_state_soundfont) {
            PlaybackState.Playing,
            PlaybackState.Queued -> {
                this.playback_stop()
            }
            else -> { /* pass */ }
        }

        when (this.editor_view_model.playback_state_midi) {
            PlaybackState.Playing,
            PlaybackState.Queued -> {
                this.playback_stop_midi_output()
            }
            else -> { /* pass */ }
        }
        if (current_device_info != null) {
            this._midi_interface.close_device(current_device_info)
        } else {
            this.disconnect_feedback_device()
        }

        this.editor_view_model.active_midi_device = device_info

        if (device_info != null) {
            this._midi_interface.open_output_device(device_info)
            this.editor_view_model.playback_state_midi = PlaybackState.Ready
            this.disable_soundfont()
            // These 2 otherwise get handled in set_soundfont()
            this.findViewById<ChannelOptionRecycler?>(R.id.rvActiveChannels)?.notify_soundfont_changed()
        } else {
            this.set_soundfont()
        }

        this.update_menu_options()
    }

    fun on_project_change_json(json_data: JSONHashMap) {
        if (! this.configuration.use_preferred_soundfont) return
        val original_soundfont = this.configuration.soundfont

        val sf_path = json_data.get_hashmap("d").get_stringn("sf") ?: return
        if (sf_path == original_soundfont) return

        this.configuration.soundfont = sf_path
        // Try opening the assigned soundfont, but if it fails for any reason, go back to the
        // Currently active one.
        try {
            this.set_soundfont()
        } catch (_: Exception) {
            this.configuration.soundfont = original_soundfont
            this.set_soundfont()
        }
        this.save_configuration()
    }

    fun on_project_change_new() {
        // set the default instrument to the first available in the soundfont (if applicable)
        val opus_manager = this.get_opus_manager()
        for ((c, channel) in opus_manager.channels.enumerate()) {
            if (!opus_manager.is_percussion(c)) continue

            // Need to prematurely update the channel instrument to find the lowest possible instrument
            this.update_channel_instruments(c)
            val i = this.editor_view_model.audio_interface.get_minimum_instrument_index(channel.get_preset())
            for (l in 0 until opus_manager.get_channel(c).size) {
                opus_manager.percussion_set_instrument(c, l, max(0, i - 27))
            }
        }
    }
}
