package com.qfs.pagan

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModel
import androidx.media3.common.MimeTypes
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.MidiController
import com.qfs.apres.MidiPlayer
import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.Riff
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.databinding.ActivityMainBinding
import com.qfs.pagan.generalizers.OpusManagerGeneralizer
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.OpusManagerCursor
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager


class MainActivity : AppCompatActivity() {
    enum class PlaybackState {
        NotReady,
        Ready,
        Playing,
        Queued,
        Stopping
    }

    class MainViewModel: ViewModel() {
        var export_handle: WavConverter? = null
        var color_map = ColorMap()
        var opus_manager = OpusManager()
        var show_percussion = true

        fun export_wav(sample_handle_manager: SampleHandleManager, target_file: File, handler: WavConverter.ExporterEventHandler) {
            val frame_map = PlaybackFrameMap(this.opus_manager, sample_handle_manager)
            frame_map.parse_opus()
            val start_frame = frame_map.get_marked_frames()[0]

            // Prebuild the first buffer's worth of sample handles, the rest happen in the get_new_handles()
            for (i in start_frame .. start_frame + sample_handle_manager.buffer_size) {
                frame_map.check_frame(i)
            }

            this.export_handle = WavConverter(sample_handle_manager) // Not accessing Cache *YET*, don't need to match buffer sizes

            this.export_handle?.export_wav(frame_map, target_file, handler)
            this.export_handle = null
        }

        fun cancel_export() {
            val handle = this.export_handle ?: return
            handle.cancel_flagged = true
        }

        fun is_exporting(): Boolean {
            return this.export_handle != null
        }
    }

    val view_model: MainViewModel by viewModels()
    // flag to indicate that the landing page has been navigated away from for navigation management
    private var _has_seen_front_page = false
    private lateinit var _project_manager: ProjectManager
    lateinit var configuration: PaganConfiguration
    private lateinit var _config_path: String
    private var integer_dialog_defaults = HashMap<String, Int>()
    private var float_dialog_defaults = HashMap<String, Float>()
    var active_percussion_names = HashMap<Int, String>()

    private var _virtual_input_device = MidiPlayer()
    private lateinit var _midi_interface: MidiController
    private var _soundfont: SoundFont? = null
    private var sample_handle_manager: SampleHandleManager? = null
    private var _feedback_sample_manager: SampleHandleManager? = null
    private var _midi_playback_device: PlaybackDevice? = null
    private var _midi_feedback_dispatcher = MidiFeedbackDispatcher()

    private lateinit var _app_bar_configuration: AppBarConfiguration
    private lateinit var _binding: ActivityMainBinding
    private var _options_menu: Menu? = null
    private var _progress_bar: ProgressBar? = null
    var playback_state_soundfont: PlaybackState = PlaybackState.NotReady
    var playback_state_midi: PlaybackState = PlaybackState.NotReady
    private var _forced_title_text: String? = null
    private val _temporary_feedback_devices = Array<FeedbackDevice?>(4) {
        null
    }
    private var _current_feedback_device: Int = 0

    // Notification shiz -------------------------------------------------
    var NOTIFICATION_ID = 0
    val CHANNEL_ID = "com.qfs.pagan"
    private var _notification_channel: NotificationChannel? = null
    private var _active_notification: NotificationCompat.Builder? = null
    // -------------------------------------------------------------------

    private var _export_wav_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (this._soundfont == null) {
            // Throw Error. Currently unreachable by ui
            return@registerForActivityResult
        }

        this.getNotificationPermission()
        thread {
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data?.also { uri ->
                    if (this.view_model.export_handle != null) {
                        return@thread
                    }

                    /* TMP file is necessary since we can't easily predict the exact frame count. */
                    val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                    if (tmp_file.exists()) {
                        tmp_file.delete()
                    }
                    tmp_file.deleteOnExit()
                    val exporter_sample_handle_manager = SampleHandleManager(
                        this._soundfont!!,
                        44100,
                        44100,
                        sample_limit = this.configuration.playback_sample_limit
                    )
                    this.view_model.export_wav(exporter_sample_handle_manager, tmp_file, object : WavConverter.ExporterEventHandler {
                        val notification_manager = NotificationManagerCompat.from(this@MainActivity)

                        override fun on_start() {
                            this@MainActivity.runOnUiThread {
                                val btnExportProject = this@MainActivity.findViewById<ImageView>(R.id.btnExportProject) ?: return@runOnUiThread
                                btnExportProject.setImageResource(R.drawable.baseline_cancel_42)
                                val llExportProgress = this@MainActivity.findViewById<View>(R.id.llExportProgress) ?: return@runOnUiThread
                                llExportProgress.visibility = View.VISIBLE

                                val tvExportProgress = this@MainActivity.findViewById<TextView>(R.id.tvExportProgress) ?: return@runOnUiThread
                                tvExportProgress.text = "0%"
                            }
                            this@MainActivity.feedback_msg(this@MainActivity.getString(R.string.export_wav_feedback))
                            val builder = this@MainActivity.get_notification() ?: return
                            this.notification_manager.notify(this@MainActivity.NOTIFICATION_ID, builder.build())

                        }

                        override fun on_complete() {
                            val parcel_file_descriptor = applicationContext.contentResolver.openFileDescriptor(uri, "w") ?: return
                            val output_stream = FileOutputStream(parcel_file_descriptor.fileDescriptor)
                            val buffered_output_stream = BufferedOutputStream(output_stream)

                            val input_stream = tmp_file.inputStream()
                            input_stream.copyTo(buffered_output_stream)
                            input_stream.close()

                            buffered_output_stream.close()
                            output_stream.close()
                            parcel_file_descriptor.close()
                            val builder = this@MainActivity.get_notification()
                            if (builder != null) {
                                builder.setContentText(this@MainActivity.getString(R.string.export_wav_notification_complete))
                                    .setProgress(0, 0, false)
                                    .setAutoCancel(true)
                                    .clearActions()
                                    .setTimeoutAfter(5000)
                                    .setSilent(false)

                                this.notification_manager.notify(this@MainActivity.NOTIFICATION_ID, builder.build())
                            }

                            this@MainActivity.feedback_msg(this@MainActivity.getString(R.string.export_wav_feedback_complete))

                            this@MainActivity.runOnUiThread {
                                val llExportProgress =
                                    this@MainActivity.findViewById<View>(R.id.llExportProgress)
                                        ?: return@runOnUiThread
                                llExportProgress.visibility = View.GONE
                                val btnExportProject = this@MainActivity.findViewById<ImageView>(R.id.btnExportProject) ?: return@runOnUiThread
                                btnExportProject.setImageResource(R.drawable.export)
                            }
                        }

                        override fun on_cancel() {
                            this@MainActivity.feedback_msg(this@MainActivity.getString(R.string.export_cancelled))
                            this@MainActivity.runOnUiThread {
                                val llExportProgress = this@MainActivity.findViewById<View>(R.id.llExportProgress)
                                        ?: return@runOnUiThread
                                llExportProgress.visibility = View.GONE
                                val btnExportProject = this@MainActivity.findViewById<ImageView>(R.id.btnExportProject) ?: return@runOnUiThread
                                btnExportProject.setImageResource(R.drawable.export)
                            }

                            val builder = this@MainActivity.get_notification() ?: return
                            builder.setContentText(this@MainActivity.getString(R.string.export_cancelled))
                                .setProgress(0, 0, false)
                                .setAutoCancel(true)
                                .setTimeoutAfter(5000)
                                .clearActions()
                            val notification_manager = NotificationManagerCompat.from(this@MainActivity)
                            notification_manager.notify(this@MainActivity.NOTIFICATION_ID, builder.build())
                        }

                        override fun on_progress_update(progress: Double) {
                            val progress_rounded = (progress * 100.0).roundToInt()
                            this@MainActivity.runOnUiThread {
                                val tvExportProgress = this@MainActivity.findViewById<TextView>(R.id.tvExportProgress) ?: return@runOnUiThread
                                tvExportProgress.text = getString(R.string.label_export_progress, progress_rounded)
                            }

                            val builder = this@MainActivity.get_notification() ?: return
                            builder.setProgress(100, progress_rounded, false)
                            this.notification_manager.notify(this@MainActivity.NOTIFICATION_ID, builder.build())
                        }

                    })
                }
            }
        }
    }

    private var _export_project_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val opus_manager = this.get_opus_manager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    val json_string = OpusManagerGeneralizer.generalize(opus_manager).to_string()
                    FileOutputStream(it.fileDescriptor).write(json_string.toByteArray())
                    this.feedback_msg(getString(R.string.feedback_exported))
                }
            }
        }
    }

    private var _import_project_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data?.also { uri ->
                    val fragment = this.get_active_fragment()
                    fragment?.clearFragmentResult(IntentFragmentToken.Resume.name)
                    fragment?.setFragmentResult(
                        IntentFragmentToken.ImportProject.name,
                        bundleOf(Pair("URI", uri.toString()))
                    )
                    if (fragment !is FragmentEditor) {
                        this.navigate(R.id.EditorFragment)
                    }
                }
            }
        }

    private var _export_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val opus_manager = this.get_opus_manager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                    this.feedback_msg(getString(R.string.feedback_exported_to_midi))
                }
            }
        }
    }

    private var _import_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                val fragment = this.get_active_fragment()
                if (fragment is FragmentEditor) {
                    fragment.project_change_flagged = true
                }
                fragment?.setFragmentResult(
                    IntentFragmentToken.ImportMidi.name,
                    bundleOf(Pair("URI", uri.toString()))
                )

                if (fragment !is FragmentEditor) {
                    this.navigate(R.id.EditorFragment)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        this.recreate()
    }

    override fun onPause() {
        this.playback_stop()
        this.playback_stop_midi_output()
        this._midi_interface.close_connected_devices()
        this._binding.appBarMain.toolbar.hideOverflowMenu()
        super.onPause()
    }

    override fun onKeyDown(key_code: Int, event: KeyEvent?): Boolean {
        val active_fragment = this.get_active_fragment()
        val cancel_super = if (event != null) {
            when (active_fragment) {
                is FragmentEditor -> active_fragment.keyboard_input_interface?.input(key_code, event) ?: false
                else -> false
            }
        } else {
            false
        }

        return if (cancel_super) {
            true
        } else {
            super.onKeyDown(key_code, event)
        }
    }

    override fun onResume() {
        super.onResume()
        this.drawer_lock()
        this.view_model.color_map.set_fallback_palette(
            if (this.is_night_mode()) {
                this.get_night_palette()
            } else {
                this.get_day_palette()
            }
        )

        if (this._midi_playback_device != null) {
            this.playback_state_soundfont = PlaybackState.Ready
        }

        if (this._midi_interface.output_devices_connected()) {
            this.playback_state_midi = PlaybackState.Ready
        }
    }

    fun save_to_backup() {
        val opus_manager = this.get_opus_manager()
        val path = opus_manager.path
        if (path != null) {
            val path_file = File("${applicationInfo.dataDir}/.bkp_path")
            path_file.writeText(path)
        }
        opus_manager.save("${applicationInfo.dataDir}/.bkp.json")

        // saving changes the path, need to change it back
        opus_manager.path = path
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Can't reliably put json in outstate. there is a size limit
        this.save_to_backup()
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        "com.qfs.pagan.CANCEL_EXPORT_WAV" -> this@MainActivity.export_wav_cancel()
                        else -> {}
                    }
                }
            },
            IntentFilter("com.qfs.pagan.CANCEL_EXPORT_WAV"),
            RECEIVER_NOT_EXPORTED
        )

        this._midi_interface = object : MidiController(this) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                if (!this@MainActivity.update_playback_state_midi(PlaybackState.Ready)) {
                    return
                }

                when (this@MainActivity.playback_state_soundfont) {
                    PlaybackState.Playing,
                    PlaybackState.Queued -> {
                        this@MainActivity.playback_stop()
                    }

                    else -> { /* pass */ }
                }

                this@MainActivity.runOnUiThread {
                    this@MainActivity.update_menu_options()
                    this@MainActivity.setup_project_config_drawer_export_button()

                    val channel_recycler = this@MainActivity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    // Should always be null since this can only be changed from a different menu
                    if (channel_recycler.adapter != null) {
                        val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
                        channel_adapter.set_soundfont(null)
                    }
                    this@MainActivity.populate_active_percussion_names(true)
                }

                if (this@MainActivity.get_opus_manager().is_tuning_standard()) {
                    this@MainActivity.disconnect_feedback_device()
                }
            }

            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                when (this@MainActivity.playback_state_midi) {
                    PlaybackState.Playing,
                    PlaybackState.Queued -> {
                        this@MainActivity.playback_stop_midi_output()
                    }

                    else -> { /* pass */ }
                }

                // Kludge. need a sleep to give output devices a chance to disconnect
                Thread.sleep(1000)

                this@MainActivity.runOnUiThread {
                    this@MainActivity.update_menu_options()
                    if (!this@MainActivity.is_connected_to_physical_device()) {
                        this@MainActivity.setup_project_config_drawer_export_button()

                        val channel_recycler = this@MainActivity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        // Should always be null since this can only be changed from a different menu
                        if (channel_recycler.adapter != null) {
                            val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
                            channel_adapter.set_soundfont(this@MainActivity._soundfont)
                        }

                        this@MainActivity.populate_active_percussion_names(true)
                    }
                }
            }
        }

        this._midi_interface.connect_virtual_input_device(this._virtual_input_device)

        // Listens for SongPositionPointer (provided by midi) and scrolls to that beat
        this._midi_interface.connect_virtual_output_device(object : VirtualMidiOutputDevice {
            override fun onSongPositionPointer(event: SongPositionPointer) {
                this@MainActivity.get_opus_manager().cursor_select_column(event.get_beat())
                // Force scroll here, cursor_select_column doesn't scroll if the column is already visible
                this@MainActivity.runOnUiThread {
                    this@MainActivity.findViewById<EditorTable>(R.id.etEditorTable)
                        ?.scroll_to_position(x = event.get_beat(), force = true)
                }
            }
        })

        this._midi_interface.connect_virtual_input_device(this._midi_feedback_dispatcher)

        this._project_manager = ProjectManager(this.getExternalFilesDir(null).toString())
        // Move files from applicationInfo.data to externalfilesdir (pre v1.1.2 location)
        val old_projects_dir = File("${applicationInfo.dataDir}/projects")
        if (old_projects_dir.isDirectory) {
            for (f in old_projects_dir.listFiles()!!) {
                val new_file_name = this._project_manager.get_new_path()
                f.copyTo(File(new_file_name))
            }
            old_projects_dir.deleteRecursively()
        }

        this._config_path = "${this.getExternalFilesDir(null)}/pagan.cfg"
        // [Re]move config file from < v1.1.2
        val old_config_file = File("${applicationInfo.dataDir}/pagan.cfg")
        val new_config_file = File(this._config_path)
        if (old_config_file.exists()) {
            if (!new_config_file.exists()) {
                old_config_file.copyTo(new_config_file)
            }
            old_config_file.delete()
        }

        this.configuration = try {
            PaganConfiguration.from_path(this._config_path)
        } catch (e: Exception) {
            PaganConfiguration()
        }

        // Temporary while i figure out why mono playback is more complicated than I assumed, force stereo & no limit on samples
        this.configuration.playback_stereo_mode = WaveGenerator.StereoMode.Stereo
        this.configuration.playback_sample_limit = null


        this._binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(this._binding.root)
        setSupportActionBar(this._binding.appBarMain.toolbar)

        this.view_model.opus_manager.attach_activity(this)

        this.view_model.color_map.use_palette = this.configuration.use_palette
        this.view_model.color_map.set_fallback_palette(
            if (this.is_night_mode()) {
                this.get_night_palette()
            } else {
                this.get_day_palette()
            }
        )

        if (this.configuration.palette != null) {
            this.view_model.color_map.set_palette(this.configuration.palette!!)
        }

        val color_map = this.view_model.color_map
        val toolbar = this._binding.appBarMain.toolbar

        toolbar.background = null
        toolbar.setTitleTextColor(color_map[Palette.TitleBarText])
        toolbar.setBackgroundColor(color_map[Palette.TitleBar])
        toolbar.setSubtitleTextColor(color_map[Palette.TitleBarText])
        toolbar.overflowIcon?.setTint(color_map[Palette.TitleBarText])

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        //////////////////////////////////////////
        if (this.configuration.soundfont != null) {
            val path = "${this.getExternalFilesDir(null)}/SoundFonts/${this.configuration.soundfont}"
            val sf_file = File(path)
            if (sf_file.exists()) {
                try {
                    this._soundfont = SoundFont(path)
                    this.sample_handle_manager = SampleHandleManager(
                        this._soundfont!!,
                        this.configuration.sample_rate,
                        this.configuration.sample_rate, // Use Large buffer
                        sample_limit = this.configuration.playback_sample_limit
                    )

                    this._midi_playback_device = PlaybackDevice(
                        this,
                        this.sample_handle_manager!!,
                        this.configuration.playback_stereo_mode
                    )

                    if (!this._midi_interface.output_devices_connected()) {
                        this._feedback_sample_manager = SampleHandleManager(
                            this._soundfont!!,
                            this.configuration.sample_rate
                        )
                    }
                } catch (e: Riff.InvalidRiff) {
                    this.configuration.soundfont = null
                    // Invalid soundfont somehow set
                }
            }
        }

        this.populate_active_percussion_names()
        ///////////////////////////////////////////

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.EditorFragment) {
                    if (this@MainActivity.get_opus_manager().cursor.mode != OpusManagerCursor.CursorMode.Unset) {
                        this@MainActivity.get_opus_manager().cursor_clear()
                    } else {
                        this@MainActivity.dialog_save_project {
                            finish()
                        }
                    }
                } else {
                    navController.popBackStack()
                }
            }
        })

        val drawer_layout = this.findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        drawer_layout.addDrawerListener(object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                val channel_recycler = this@MainActivity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                if (channel_recycler.adapter == null) {
                    ChannelOptionAdapter(this@MainActivity.get_opus_manager(), channel_recycler)
                }
                val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                if (channel_adapter.itemCount == 0) {
                    channel_adapter.setup()
                }
                super.onDrawerOpened(drawerView)

                this@MainActivity.playback_stop()
                this@MainActivity.playback_stop_midi_output()
                this@MainActivity.drawer_unlock() // So the drawer can be closed with a swipe
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                this@MainActivity.drawer_lock() // so the drawer can't be opened with a swipe
            }
        })

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(this._app_bar_configuration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        this._options_menu = menu
        val output = super.onCreateOptionsMenu(menu)
        this.update_menu_options()
        return output
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.itmPlay && item.itemId != R.id.itmPlayMidiOutput) {
            this.playback_stop()
            this.playback_stop_midi_output()
        }

        when (item.itemId) {
            android.R.id.home -> {
                val fragment = this.get_active_fragment()
                if (fragment is FragmentEditor) {
                    this.drawer_open()
                } else {
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    navController.popBackStack()
                }
            }

            R.id.itmNewProject -> {
                this.dialog_save_project {
                    val fragment = this.get_active_fragment()
                    fragment?.clearFragmentResult(IntentFragmentToken.Resume.name)
                    fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
                    if (fragment !is FragmentEditor) {
                        this.navigate(R.id.EditorFragment)
                    }
                }
            }

            R.id.itmLoadProject -> {
                this.dialog_save_project {
                    this.dialog_load_project()
                }
            }

            R.id.itmImportMidi -> {
                this.dialog_save_project {
                    this.select_midi_file()
                }
            }

            R.id.itmUndo -> {
                this.get_opus_manager().apply_undo()
            }

            R.id.itmPlay -> {
                when (this.playback_state_soundfont) {
                    PlaybackState.Ready -> {
                        this.playback_start()
                    }
                    PlaybackState.Queued,
                    PlaybackState.Playing -> {
                        this.playback_stop()
                    }

                    else -> {}
                }
            }

            R.id.itmPlayMidiOutput -> {
                when (this.playback_state_midi) {
                    PlaybackState.Ready -> {
                        this.playback_start_midi_output()
                    }
                    PlaybackState.Queued,
                    PlaybackState.Playing -> {
                        this.playback_stop_midi_output()
                    }
                    else -> { /* pass */ }
                }
            }

            R.id.itmImportProject -> {
                this.dialog_save_project {
                    this.select_project_file()
                }
            }

            R.id.itmSettings -> {
                this.navigate(R.id.SettingsFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun project_save() {
        this._project_manager.save(this.get_opus_manager())
        this.feedback_msg(getString(R.string.feedback_project_saved))
        this.update_menu_options()
    }

    private fun project_delete() {
        val title = this.get_opus_manager().project_name ?: getString(R.string.untitled_opus)
        this._project_manager.delete(this.get_opus_manager())

        val fragment = this.get_active_fragment()
        fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
        if (fragment !is FragmentEditor) {
            this.navigate(R.id.EditorFragment)
        }

        this.feedback_msg(resources.getString(R.string.feedback_delete, title))
    }

    private fun project_move_to_copy() {
        this._project_manager.move_to_copy(this.get_opus_manager())
        this.feedback_msg(getString(R.string.feedback_on_copy))
    }

    private fun _enable_blocker_view() {
        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay)
        if (blocker_view != null && blocker_view.visibility != View.VISIBLE) {
            blocker_view.setOnClickListener {
                this.playback_stop()
                this.playback_stop_midi_output()
            }
            blocker_view.visibility = View.VISIBLE
            this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun _disable_blocker_view() {
        // Assume playback states have been updated BEFORE calling this function
        // Need to check both since they both use the same blocker, though it should never be an issue
        if (this.playback_state_midi == PlaybackState.Playing || this.playback_state_soundfont == PlaybackState.Playing) {
            return
        }
        this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay) ?: return
        blocker_view.visibility = View.GONE
    }

    private fun playback_start() {
        if (!this.update_playback_state_soundfont(PlaybackState.Queued)) {
            this.feedback_msg("Playback Failed")
            return
        }

        this._enable_blocker_view()
        this.runOnUiThread {
            this.set_playback_button(R.drawable.baseline_play_disabled_24)
            this.loading_reticle_show(getString(R.string.reticle_msg_start_playback))
        }

        var start_point = this.get_working_column()
        if (start_point >= this.get_opus_manager().beat_count - 1) {
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

            this._midi_playback_device?.play_opus(start_point)
        }
    }

    private fun playback_start_midi_output() {
        if (!this.update_playback_state_midi(PlaybackState.Queued)) {
            this.feedback_msg("Playback Failed")
            return
        }

        this.loading_reticle_show(getString(R.string.reticle_msg_start_playback))
        this._enable_blocker_view()

        var start_point = this.get_working_column()
        val opus_manager = this.get_opus_manager()
        if (start_point >= opus_manager.beat_count - 1) {
            start_point = 0
        }

        val midi = opus_manager.get_midi(start_point)

        this.runOnUiThread {
            this.loading_reticle_hide()
            this.set_midi_playback_button(R.drawable.ic_baseline_pause_24)
        }

        if (!this.update_playback_state_midi(PlaybackState.Playing)) {
            this.restore_midi_playback_state()
            return
        }
        thread {
            try {
                this._midi_interface.open_connected_devices()
                this._virtual_input_device.play_midi(midi) {
                    this.runOnUiThread {
                        this.playback_stop_midi_output()
                    }
                }
            } catch (e: java.io.IOException) {
                this.runOnUiThread {
                    this.playback_stop_midi_output()
                }
            }
        }
    }

    internal fun playback_stop() {
        if (this.update_playback_state_soundfont(PlaybackState.Stopping)) {
            this.loading_reticle_hide()
            this._midi_playback_device?.kill()
        }
    }

    internal fun playback_stop_midi_output() {
        if (this.update_playback_state_midi(PlaybackState.Stopping)) {
            this.loading_reticle_hide()
            this._virtual_input_device.stop()
            this.restore_midi_playback_state()
        }
    }

    fun restore_playback_state() {
        if (this.update_playback_state_soundfont(PlaybackState.Ready)) {
            this.runOnUiThread {
                this.set_playback_button(R.drawable.ic_baseline_play_arrow_24)
                this._disable_blocker_view()
            }
        }
    }

    fun restore_midi_playback_state() {
        if (this.update_playback_state_midi(PlaybackState.Ready)) {
            this.runOnUiThread {
                this.set_midi_playback_button(R.drawable.ic_baseline_play_arrow_24)
                this._disable_blocker_view()
            }
        }
    }

    fun get_new_project_path(): String {
        return this._project_manager.get_new_path()
    }

    // Ui Wrappers ////////////////////////////////////////////
    private fun drawer_close() {
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()
    }

    private fun drawer_open() {
        findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(GravityCompat.START)
    }

    fun drawer_lock() {
        this._binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun drawer_unlock() {
        try {
            this._binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            this.findViewById<PaganDrawerLayout>(R.id.config_drawer)?.refreshDrawableState()
        } catch (e: UninitializedPropertyAccessException) {
            // pass, if it's not initialized, it's not locked
        }
    }

    fun feedback_msg(msg: String) {
        this.runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun loading_reticle_show(title_msg: String? = null) {
        this.runOnUiThread {
            if (title_msg != null) {
                this.force_title_text(title_msg)
            }

            if (this._progress_bar == null) {
                this._progress_bar = PaganProgressBar(this)
            }

            this._progress_bar!!.isClickable = true
            val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(100, 100)
            params.addRule(RelativeLayout.CENTER_IN_PARENT)
            val parent = this._progress_bar!!.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(this._progress_bar)
            }

            try {
                this._binding.root.addView(this._progress_bar, params)
            } catch (e: UninitializedPropertyAccessException) {
                // pass
            }
        }
    }

    fun loading_reticle_hide() {
        thread {
            this.runOnUiThread {
                this.clear_forced_title()
                val progressBar = this._progress_bar ?: return@runOnUiThread
                if (progressBar.parent != null) {
                    (progressBar.parent as ViewGroup).removeView(progressBar)
                }
            }
        }
    }

    fun navigate(fragment: Int) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (fragment == R.id.EditorFragment) {
            this._has_seen_front_page = true
        }

        navController.navigate(fragment)
    }


    fun get_active_fragment(): Fragment? {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        return navHost?.childFragmentManager?.fragments?.get(0)
    }

    fun update_title_text() {
        this.set_title_text(
            if (this._forced_title_text != null) {
                this._forced_title_text!!
            } else {
                when (this.get_active_fragment()) {
                    is FragmentGlobalSettings -> {
                        resources.getString(R.string.settings_fragment_label)
                    }
                    is FragmentLandingPage -> {
                        "${getString(R.string.app_name)} ${getString(R.string.app_version)}"
                    }
                    else -> {
                        this.get_opus_manager().project_name ?: getString(R.string.untitled_opus)
                    }
                }
            }
        )
        this.refresh_toolbar()
    }

    fun set_title_text(new_text: String) {
        this._binding.appBarMain.toolbar.title = new_text
    }

    fun force_title_text(msg: String) {
        this._forced_title_text = msg
        this.update_title_text()
    }

    fun clear_forced_title() {
        this._forced_title_text = null
        this.update_title_text()
    }

    fun update_menu_options() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val options_menu = this._options_menu ?: return
        options_menu.setGroupDividerEnabled(true)
        val text_color = this.view_model.color_map[Palette.TitleBarText]

        when (navHost?.childFragmentManager?.fragments?.get(0)) {
            is FragmentEditor -> {
                val play_midi_visible = (this._midi_interface.output_devices_connected() && this.get_opus_manager().is_tuning_standard())
                options_menu.findItem(R.id.itmLoadProject).isVisible = this.has_projects_saved()
                options_menu.findItem(R.id.itmUndo).isVisible = true
                options_menu.findItem(R.id.itmUndo).setIconTintList(ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_enabled)),
                    intArrayOf(text_color)
                ))
                options_menu.findItem(R.id.itmNewProject).isVisible = true
                options_menu.findItem(R.id.itmPlay).isVisible = this._soundfont != null && ! play_midi_visible
                options_menu.findItem(R.id.itmPlay).setIconTintList(ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_enabled)),
                    intArrayOf(text_color)
                ))
                options_menu.findItem(R.id.itmPlayMidiOutput).isVisible = play_midi_visible
                options_menu.findItem(R.id.itmPlayMidiOutput).setIconTintList(ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_enabled)),
                    intArrayOf(text_color)
                ))

                options_menu.findItem(R.id.itmImportMidi).isVisible = true
                options_menu.findItem(R.id.itmImportProject).isVisible = true
                options_menu.findItem(R.id.itmSettings).isVisible = true
            }
            else -> {
                options_menu.findItem(R.id.itmLoadProject).isVisible = false
                options_menu.findItem(R.id.itmUndo).isVisible = false
                options_menu.findItem(R.id.itmNewProject).isVisible = false
                options_menu.findItem(R.id.itmPlay).isVisible = false
                options_menu.findItem(R.id.itmPlayMidiOutput).isVisible = false
                options_menu.findItem(R.id.itmImportMidi).isVisible = false
                options_menu.findItem(R.id.itmImportProject).isVisible = false
                options_menu.findItem(R.id.itmSettings).isVisible = false
            }
        }
    }

    fun setup_project_config_drawer() {
        val opus_manager = this.get_opus_manager()
        val tvChangeProjectName: TextView = this.findViewById(R.id.btnChangeProjectName)
        tvChangeProjectName.setOnClickListener {
            this.dialog_project_name()
        }

        val radix = this.get_opus_manager().tuning_map.size

        //-------------------------------------------
        val btnRadix: TextView = this.findViewById(R.id.btnRadix)
        btnRadix.setOnClickListener {
            val main_fragment = this.get_active_fragment() ?: return@setOnClickListener
            val viewInflated: View = LayoutInflater.from(main_fragment.context)
                .inflate(
                    R.layout.dialog_tuning_map,
                    main_fragment.view as ViewGroup,
                    false
                )

            val etRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etRadix)
            val etTranspose = viewInflated.findViewById<RangedIntegerInput>(R.id.etTranspose)
            etTranspose.set_range(0, radix - 1)
            etTranspose.set_value(opus_manager.transpose)

            val rvTuningMap = viewInflated.findViewById<TuningMapRecycler>(R.id.rvTuningMap)
            rvTuningMap.adapter = TuningMapRecyclerAdapter(opus_manager.tuning_map.clone())


            val dialog = AlertDialog.Builder(main_fragment.context, R.style.AlertDialog)
                .setCustomTitle(this._build_dialog_title_view(
                     resources.getString(R.string.dlg_tuning)
                ))
                .setView(viewInflated)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    opus_manager.set_tuning_map_and_transpose(
                        (rvTuningMap.adapter as TuningMapRecyclerAdapter).tuning_map,
                        etTranspose.get_value() ?: 0
                    )

                    dialog.dismiss()
                }
                .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()

            this._adjust_dialog_colors(dialog)


            val default_value = opus_manager.tuning_map.size

            etRadix.set_value(default_value)
            etRadix.set_range(2, 36)
            etRadix.value_set_callback = { new_radix: Int? ->
                rvTuningMap.reset_tuning_map(new_radix)
                etTranspose.set_value(0)
                etTranspose.set_range(0, (new_radix ?: 12) - 1)
            }
        }
        //-------------------------------------------
        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            opus_manager.new_channel()
        }

        this.setup_project_config_drawer_export_button()
        this.findViewById<View>(R.id.btnSaveProject).setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }

            this.project_save()
        }
        this.findViewById<View>(R.id.btnSaveProject).setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }

            this.export_project()
            true
        }

        val btnDeleteProject = this.findViewById<View>(R.id.btnDeleteProject)
        val btnCopyProject = this.findViewById<View>(R.id.btnCopyProject)
        val file_exists = (opus_manager.path != null && File(opus_manager.path!!).isFile)
        btnDeleteProject.isEnabled = file_exists
        btnCopyProject.isEnabled = file_exists

        btnDeleteProject.setOnClickListener {
            if (it.isEnabled) {
                this.dialog_delete_project()
            }
        }

        btnCopyProject.setOnClickListener {
            if (it.isEnabled) {
                this.project_move_to_copy()
                this.drawer_close()
            }
        }
    }

    internal fun _build_dialog_title_view(text: String): TextView {
        val output = PaganTextView(ContextThemeWrapper(this, R.style.dialog_title))
        output.text = text
        return output
    }
    internal fun _adjust_dialog_colors(dialog: AlertDialog) {
        val color_map = this.view_model.color_map

        dialog.window!!.decorView.background.setTint(color_map[Palette.Background])
        val padding = this.resources.getDimension(R.dimen.alert_padding).roundToInt()
        dialog.window!!.decorView.setPadding(padding, padding, padding, padding)

        val resources = this.resources

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
            (this.layoutParams as MarginLayoutParams).marginEnd = this@MainActivity.resources.getDimension(R.dimen.alert_padding).toInt()
            this.layoutParams.height = resources.getDimension(R.dimen.alert_button_height).roundToInt()
            this.backgroundTintList = null
            this.background = AppCompatResources.getDrawable(this@MainActivity, R.drawable.button)
            this.setPadding(
                resources.getDimension(R.dimen.alert_button_padding_left).roundToInt(),
                resources.getDimension(R.dimen.alert_button_padding_top).roundToInt(),
                resources.getDimension(R.dimen.alert_button_padding_right).roundToInt(),
                resources.getDimension(R.dimen.alert_button_padding_bottom).roundToInt()
            )
            for (i in 0 until (this.background as StateListDrawable).stateCount) {
                val background = ((this.background as StateListDrawable).getStateDrawable(i) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
                background?.setTint(color_map[Palette.ButtonAlt])
            }
            this.setTextColor(color_map[Palette.ButtonAltText])
        }

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).apply {
            this.background = null
            this.setTextColor(color_map[Palette.Foreground])
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).apply {
            this.backgroundTintList = null
            this.background = AppCompatResources.getDrawable(this@MainActivity, R.drawable.button)
            this.layoutParams.height = resources.getDimension(R.dimen.alert_button_height).roundToInt()
            this.setPadding(
                resources.getDimension(R.dimen.alert_button_padding_left).roundToInt(),
                resources.getDimension(R.dimen.alert_button_padding_top).roundToInt(),
                resources.getDimension(R.dimen.alert_button_padding_right).roundToInt(),
                resources.getDimension(R.dimen.alert_button_padding_bottom).roundToInt()
            )
            for (i in 0 until (this.background as StateListDrawable).stateCount) {
                val background = ((this.background as StateListDrawable).getStateDrawable(i) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
                background?.setTint(color_map[Palette.Button])
            }
            this.setTextColor(color_map[Palette.ButtonText])
        }
    }

    internal fun setup_project_config_drawer_export_button() {
        val export_options = this.get_exportable_options()
        val export_button = this.findViewById<ImageView>(R.id.btnExportProject) ?: return
        val export_progress_wrapper = this.findViewById<LinearLayout>(R.id.llExportProgress) ?: return
        if (!this.view_model.is_exporting()) {
            export_button.setImageResource(R.drawable.export)
            export_progress_wrapper.visibility = View.GONE
        } else {
            export_button.setImageResource(R.drawable.baseline_cancel_42)
            export_progress_wrapper.visibility = View.VISIBLE
        }

        if (export_options.isNotEmpty()) {
            export_button.setOnClickListener {
                if (!this.view_model.is_exporting()) {
                    this.dialog_popup_menu(
                        getString(R.string.dlg_export),
                        export_options,
                        default = null
                    ) { _: Int, value: Int ->
                        when (value) {
                            0 -> this.export_midi()
                            1 -> this.export_wav()
                        }
                    }
                } else {
                    this.export_wav_cancel()
                }
            }
            export_button.visibility = View.VISIBLE
        } else {
            export_button.visibility = View.GONE
        }
    }

    private fun get_exportable_options(): List<Pair<Int, String>> {
        val export_options = mutableListOf<Pair<Int, String>>()
        if (this.get_opus_manager().is_tuning_standard()) {
            export_options.add( Pair(0, getString(R.string.export_option_midi)) )
        }

        if (this.get_soundfont() != null) {
            export_options.add( Pair(1, getString(R.string.export_option_wav)) )
        }

        return export_options
    }

    // Ui Wrappers End ////////////////////////////////////////

    private fun _get_default_drum_options(): List<Pair<String, Int>> {
        val midi_drums = this.resources.getStringArray(R.array.midi_drums)
        return List(midi_drums.size) { i: Int ->
            Pair(midi_drums[i]!!, i + 27)
        }
    }

    private fun get_drum_options(): List<Pair<String, Int>> {
        if (this.sample_handle_manager == null || this.is_connected_to_physical_device()) {
            return this._get_default_drum_options()
        }

        val preset = try {
            this.sample_handle_manager!!.get_preset(9) ?: return this._get_default_drum_options()
        } catch (e: SoundFont.InvalidPresetIndex) {
            return this._get_default_drum_options()
        }

        val available_drum_keys = mutableSetOf<Pair<String, Int>>()

        for ((_, preset_instrument) in preset.instruments) {
            if (preset_instrument.instrument == null) {
                continue
            }

            for (sample in preset_instrument.instrument!!.samples.values) {
                if (sample.key_range != null) {
                    var name = sample.sample!!.name
                    if (name.contains("(")) {
                        name = name.substring(0, name.indexOf("("))
                    }
                    available_drum_keys.add(Pair(name, sample.key_range!!.first))
                }
            }
        }

        return available_drum_keys.sortedBy {
            it.second
        }
    }

    // Update peripheral device instruments, ie feedback device and midi devices
    fun update_channel_instruments(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (index == null) {
            if (this._feedback_sample_manager != null) {
                for (channel in opus_manager.get_all_channels()) {
                    val midi_channel = channel.get_midi_channel()
                    val (midi_bank, midi_program) = channel.get_instrument()
                    this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
                    this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))

                    this._feedback_sample_manager!!.select_bank(
                        midi_channel,
                        midi_bank,
                    )
                    this._feedback_sample_manager!!.change_program(
                        midi_channel,
                        midi_program,
                    )
                }
            }
            // Don't need to update anything but percussion here
            val midi_channel = opus_manager.percussion_channel.get_midi_channel()
            val (midi_bank, midi_program) = opus_manager.percussion_channel.get_instrument()

            if (this.sample_handle_manager != null) {
                this.sample_handle_manager!!.select_bank(
                    midi_channel,
                    midi_bank
                )
                this.sample_handle_manager!!.change_program(
                    midi_channel,
                    midi_program
                )
            }
        } else {
            val opus_channel = opus_manager.get_channel(index)
            val midi_channel = opus_channel.get_midi_channel()
            val (midi_bank, midi_program) = opus_channel.get_instrument()
            this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
            this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))
            if (this._feedback_sample_manager != null) {
                this._feedback_sample_manager!!.select_bank(
                    midi_channel,
                    midi_bank,
                )
                this._feedback_sample_manager!!.change_program(
                    midi_channel,
                    midi_program,
                )
            }

            // Don't need to update anything but percussion here
            if (this.sample_handle_manager != null) {
                this.sample_handle_manager!!.select_bank(
                    midi_channel,
                    midi_bank
                )
                this.sample_handle_manager!!.change_program(
                    midi_channel,
                    midi_program
                )
            }
        }
    }

    fun get_opus_manager(): OpusManager {
        return this.view_model.opus_manager
    }

    fun play_event(channel: Int, event_value: Int, velocity: Int) {
        if (!this._midi_interface.output_devices_connected()) {
            if (this._feedback_sample_manager == null) {
                this.connect_feedback_device()
                this.update_channel_instruments()
            }
        } else {
            this.disconnect_feedback_device()
            this._midi_interface.open_output_devices()
        }

        val opus_manager = this.get_opus_manager()
        val midi_channel = opus_manager.get_channel(channel).get_midi_channel()

        val radix = opus_manager.tuning_map.size
        val (note, bend) = if (opus_manager.is_percussion(channel)) { // Ignore the event data and use percussion map
            Pair(event_value + 27, 0)
        } else {
            val octave = event_value/ radix
            val offset = opus_manager.tuning_map[event_value % radix]

            val transpose_offset = 12.0 * opus_manager.transpose.toDouble() / radix.toDouble()
            val std_offset = 12.0 * offset.first.toDouble() / offset.second.toDouble()

            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            val new_note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21

            Pair(new_note, bend)
        }

        if (note > 127) {
            return
        }

        if (this._feedback_sample_manager != null) {
            if (this._temporary_feedback_devices[this._current_feedback_device] == null) {
                this._temporary_feedback_devices[this._current_feedback_device] = FeedbackDevice(this._feedback_sample_manager!!)
            }

            this._temporary_feedback_devices[this._current_feedback_device]!!.new_event(
                NoteOn79(
                    index=0,
                    channel=midi_channel,
                    note=note,
                    bend=bend,
                    velocity = velocity shl 8,
                ),
                250
            )
            this._current_feedback_device = (this._current_feedback_device + 1) % this._temporary_feedback_devices.size
        } else {
            try {
                this._midi_feedback_dispatcher.play_note(
                    midi_channel,
                    note,
                    bend,
                    velocity,
                    !opus_manager.is_tuning_standard() || !this.is_connected_to_physical_device()
                )
            } catch (e: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But i'm sure that'll come back to bite me
            }
        }
    }

    fun import_project(path: String) {
        this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.get_opus_manager().load(bytes, this._project_manager.get_new_path())
        }
    }

    fun import_midi(path: String) {
        val bytes = this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            FileInputStream(it.fileDescriptor).readBytes()
        } ?: throw InvalidMIDIFile(path)

        val midi = try {
            Midi.from_bytes(bytes)
        } catch (e: Exception) {
            throw InvalidMIDIFile(path)
        }

        val opus_manager = this.get_opus_manager()
        opus_manager.import_midi(midi)
        val filename = this.parse_file_name(Uri.parse(path))
        val new_path = this._project_manager.get_new_path()

        opus_manager.path = new_path
        opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: getString(R.string.default_imported_midi_title))
        opus_manager.clear_history()
    }

    fun has_projects_saved(): Boolean {
        return this._project_manager.has_projects_saved()
    }

    fun is_soundfont_available(): Boolean {
        val soundfont_dir = this.get_soundfont_directory()
        return soundfont_dir.listFiles()?.isNotEmpty() ?: false
    }

    fun set_soundfont(filename: String?) {
        if (filename == null) {
            this.disable_soundfont()
            return
        }

        if (!this.update_playback_state_soundfont(PlaybackState.Ready)) {
            // Failed to change playback_state
            return
        }


        val path = "${this.getExternalFilesDir(null)}/SoundFonts/$filename"
        try {
            this._soundfont = SoundFont(path)
        } catch (e: Riff.InvalidRiff) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg("Invalid Soundfont")
            return
        }
        this.configuration.soundfont = filename

        this.reinit_playback_device()
        this.connect_feedback_device()

        this.update_channel_instruments()
        this.populate_active_percussion_names()
        this.runOnUiThread {
            this.setup_project_config_drawer_export_button()

            val channel_recycler = this.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            // Should always be null since this can only be changed from a different menu
            if (channel_recycler.adapter != null) {
                val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
                channel_adapter.set_soundfont(this._soundfont!!)
            }
        }
    }

    fun update_playback_state_soundfont(next_state: PlaybackState): Boolean {
        this.playback_state_soundfont = this._update_playback_state(this.playback_state_soundfont, next_state) ?: return false
        return true
    }

    fun update_playback_state_midi(next_state: PlaybackState): Boolean {
        this.playback_state_midi = this._update_playback_state(this.playback_state_midi, next_state) ?: return false
        return true
    }

    private fun _update_playback_state(input_state: PlaybackState, next_state: PlaybackState): PlaybackState? {
        return when (input_state) {
            PlaybackState.NotReady -> {
                when (next_state) {
                    PlaybackState.NotReady,
                    PlaybackState.Ready -> next_state
                    else -> null
                }
            }
            PlaybackState.Ready -> {
                when (next_state) {
                    PlaybackState.NotReady,
                    PlaybackState.Ready,
                    PlaybackState.Queued -> next_state
                    else -> null
                }
            }
            PlaybackState.Playing -> {
                when (next_state) {
                    PlaybackState.Ready,
                    PlaybackState.Stopping -> next_state
                    else -> null
                }
            }
            PlaybackState.Queued -> {
                when (next_state) {
                    PlaybackState.Ready,
                    PlaybackState.Playing -> next_state
                    else -> null
                }
            }
            PlaybackState.Stopping -> {
                when (next_state) {
                    PlaybackState.Ready -> next_state
                    else -> null
                }
            }
        }
    }

    fun get_soundfont(): SoundFont? {
        return this._soundfont
    }

    fun disable_soundfont() {
        if (!this.update_playback_state_soundfont(PlaybackState.NotReady)) {
            return
        }

        this.update_channel_instruments()
        if (this._feedback_sample_manager != null) {
            this.disconnect_feedback_device()
        }

        this._soundfont = null
        this.sample_handle_manager = null
        this.configuration.soundfont = null
        this._midi_playback_device = null
        this._feedback_sample_manager = null

        this.populate_active_percussion_names()
    }

    fun get_soundfont_directory(): File {
        val soundfont_dir = File("${this.getExternalFilesDir(null)}/SoundFonts")
        if (!soundfont_dir.exists()) {
            soundfont_dir.mkdirs()
        }

        return soundfont_dir
    }

    fun save_configuration() {
        try {
            this.configuration.palette = this.view_model.color_map.get_palette()
            this.configuration.use_palette = this.view_model.color_map.use_palette
            this.configuration.save(this._config_path)
        } catch (e: FileNotFoundException) {
            this.feedback_msg(resources.getString(R.string.config_file_not_found))
        }
    }

    fun get_drum_name(index: Int): String? {
        this.populate_active_percussion_names(false)
        return this.active_percussion_names[index + 27]
    }

    fun populate_active_percussion_names(force: Boolean = true) {
        if (force || this.active_percussion_names.isEmpty()) {
            this.active_percussion_names.clear()
            val drums = this.get_drum_options()
            for ((name, note) in drums) {
                // TODO: *Maybe* Allow drum instruments below 27? not sure what the standard is.
                //  I thought 27 was the lowest, but i'll come up with something later
                if (note >= 27) {
                    this.active_percussion_names[note] = name
                }
            }
        }
    }

    fun parse_file_name(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val ci = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ci >= 0) {
                        result = cursor.getString(ci)
                    }
                }
                cursor.close()
            }
        }

        if (result == null && uri.path is String) {
            result = uri.path!!
            result = result.substring(result.lastIndexOf("/") + 1)
        }
        return result
    }

    private fun dialog_project_name() {
        val main_fragment = this.get_active_fragment()

        val viewInflated: View = LayoutInflater.from(main_fragment!!.context)
            .inflate(
                R.layout.text_name_change,
                main_fragment.view as ViewGroup,
                false
            )

        val input: EditText = viewInflated.findViewById(R.id.etProjectName)
        input.setText(this.get_opus_manager().project_name)

        val opus_manager = this.get_opus_manager()
        this._adjust_dialog_colors(
            AlertDialog.Builder(main_fragment.context, R.style.AlertDialog)
                .setCustomTitle(this._build_dialog_title_view(getString(R.string.dlg_change_name)))
                .setView(viewInflated)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    opus_manager.set_project_name(input.text.toString())
                    dialog.dismiss()
                }
                .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        )
    }

    internal fun <T> dialog_popup_menu(title: String, options: List<Pair<T, String>>, default: T? = null, callback: (index: Int, value: T) -> Unit) {
        if (options.isEmpty()) {
            return
        }
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_menu,
                window.decorView.rootView as ViewGroup,
                false
            )

        val recycler = viewInflated.findViewById<RecyclerView>(R.id.rvOptions)
        val title_view = viewInflated.findViewById<TextView>(R.id.tvTitle)

        title_view.text = title

        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setView(viewInflated)
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        this._adjust_dialog_colors(dialog)


        val adapter = PopupMenuRecyclerAdapter<T>(recycler, options, default) { index: Int, value: T ->
            dialog.dismiss()
            callback(index, value)
        }

        adapter.notifyDataSetChanged()
    }

    internal fun dialog_load_project() {
        val project_list = this._project_manager.get_project_list()

        this.dialog_popup_menu("Load Project", project_list) { _: Int, path: String ->
            val fragment = this.get_active_fragment() ?: return@dialog_popup_menu
            this.loading_reticle_show(getString(R.string.reticle_msg_load_project))

            fragment.setFragmentResult(
                IntentFragmentToken.Load.name,
                bundleOf(Pair("PATH", path))
            )

            if (fragment !is FragmentEditor) {
                this.navigate(R.id.EditorFragment)
            }
        }
    }
    // TODO: fix code duplication in dialog_float/integer_input
    internal fun dialog_float_input(title: String, min_value: Float, max_value: Float, default: Float? = null, callback: (value: Float) -> Unit ) {
        val coerced_default_value = default ?: (this.float_dialog_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_float,
                window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedFloatInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setCustomTitle(this._build_dialog_title_view(title))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this.float_dialog_defaults[title] = output_value
                callback(output_value)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()
        this._adjust_dialog_colors(dialog)

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
        val coerced_default_value = default ?: (this.integer_dialog_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedIntegerInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setCustomTitle(this._build_dialog_title_view(title))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this.integer_dialog_defaults[title] = output_value
                callback(output_value)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()
        this._adjust_dialog_colors(dialog)

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

    fun dialog_confirm(title: String, callback: () -> Unit) {
        this._adjust_dialog_colors(
        AlertDialog.Builder(this, R.style.AlertDialog)
            .setCustomTitle(this._build_dialog_title_view(title))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ ->
                dialog.dismiss()
                callback()
            }
            .setNegativeButton(getString(R.string.dlg_decline)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        )
    }

    private fun needs_save(): Boolean {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.path == null) {
            return true
        }

        if (!File(opus_manager.path!!).exists()) {
            return !opus_manager.history_cache.isEmpty()
        }

        val other = OpusLayerLinks()
        other.load_path(opus_manager.path!!)

        return opus_manager != other
    }

    private fun dialog_save_project(callback: () -> Unit) {
        if (this.needs_save()) {
            this._adjust_dialog_colors(
                AlertDialog.Builder(this, R.style.AlertDialog)
                    .setCustomTitle(this._build_dialog_title_view(getString(R.string.dialog_save_warning_title)))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ ->
                        this@MainActivity.project_save()
                        dialog.dismiss()
                        callback()
                    }
                    .setNegativeButton(getString(R.string.dlg_decline)) { dialog, _ ->
                        dialog.dismiss()
                        callback()
                    }
                    .show()
            )
        } else {
            callback()
        }
    }

    private fun dialog_delete_project() {
        val main_fragment = this.get_active_fragment()

        val title = this.get_opus_manager().project_name ?: getString(R.string.untitled_opus)
        this._adjust_dialog_colors(
            AlertDialog.Builder(main_fragment!!.context, R.style.AlertDialog)
                .setCustomTitle(this._build_dialog_title_view(resources.getString(R.string.dlg_delete_title, title)))
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    this@MainActivity.project_delete()
                    dialog.dismiss()
                    this@MainActivity.drawer_close()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        )
    }

    fun select_midi_file() {
        val intent = Intent()
            .setType(MimeTypes.AUDIO_MIDI)
            .setAction(Intent.ACTION_GET_CONTENT)
        this._import_midi_intent_launcher.launch(intent)
    }

    fun select_project_file() {
        val intent = Intent()
            .setType("application/json")
            .setAction(Intent.ACTION_GET_CONTENT)
        this._import_project_intent_launcher.launch(intent)
    }

    fun get_default_export_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return "Pagan Op. ${now.format(formatter)}"
    }

    fun get_export_name(): String {
        val reserved_chars = "|\\?*<\":>+[]/'"
        var base_name: String = this.get_opus_manager().project_name ?: this.get_default_export_name()
        for (c in reserved_chars) {
            base_name = base_name.replace("$c", "_")
        }
        return base_name
    }

    fun export_wav() {
        val name = this.get_export_name()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = MimeTypes.AUDIO_WAV
        intent.putExtra(Intent.EXTRA_TITLE, "$name.wav")
        this._export_wav_intent_launcher.launch(intent)
    }

    fun export_wav_cancel() {
        this.view_model.cancel_export()
    }

    fun export_midi() {
        val name = this.get_export_name()

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = MimeTypes.AUDIO_MIDI
        intent.putExtra(Intent.EXTRA_TITLE, "$name.mid")

        this._export_midi_intent_launcher.launch(intent)
    }

    fun export_project() {
        val name = this.get_export_name()

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, "$name.json")

        this._export_project_intent_launcher.launch(intent)
    }

    fun set_sample_rate(new_sample_rate: Int) {
        this.configuration.sample_rate = new_sample_rate
        this.save_configuration()
        this.reinit_playback_device()
    }

    fun reinit_playback_device() {
        this._midi_playback_device?.kill()

        if (this.get_soundfont() != null) {
            this.sample_handle_manager = SampleHandleManager(
                this._soundfont!!,
                this.configuration.sample_rate,
                this.configuration.sample_rate,
                sample_limit = this.configuration.playback_sample_limit
            )

            this._midi_playback_device = PlaybackDevice(
                this,
                this.sample_handle_manager!!,
                this.configuration.playback_stereo_mode,
            )
        } else {
            this._midi_playback_device = null
        }
    }

    fun validate_percussion_visibility() {
        val opus_manager = this.get_opus_manager()
        if (!this.view_model.show_percussion && opus_manager.has_percussion()) {
            this.view_model.show_percussion = true
        }
    }

    fun in_playback(): Boolean {
        return PlaybackState.Playing in listOf(this.playback_state_soundfont, this.playback_state_midi)
    }

    fun has_notification_permission(): Boolean {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED )
    }

    private fun getNotificationPermission(): Boolean {
        if (! this.has_notification_permission()) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        return this.has_notification_permission()
    }

    fun set_playback_button(drawable: Int) {
        val play_pause_button = this._options_menu?.findItem(R.id.itmPlay) ?: return
        play_pause_button.icon = ContextCompat.getDrawable(this, drawable)
    }

    fun set_midi_playback_button(drawable: Int) {
        val play_pause_button = this._options_menu?.findItem(R.id.itmPlayMidiOutput) ?: return
        play_pause_button.icon = ContextCompat.getDrawable(this, drawable)
    }

    fun get_working_column(): Int {
        val cursor = this.get_opus_manager().cursor
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                cursor.beat
            }
            OpusManagerCursor.CursorMode.Range -> {
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
        this._temporary_feedback_devices.forEachIndexed { i: Int, device: FeedbackDevice? ->
            device?.kill()
            this._temporary_feedback_devices[i] = null
        }
        this._feedback_sample_manager = null
    }

    fun connect_feedback_device() {
        if (this._soundfont == null) {
            return
        }

        this.disconnect_feedback_device()
        this._feedback_sample_manager = SampleHandleManager(
            this._soundfont!!,
            this.configuration.sample_rate
        )
    }

    fun block_physical_midi_output() {
        this._midi_interface.block_physical_devices = true
    }

    fun enable_physical_midi_output() {
        this._midi_interface.block_physical_devices = false
    }

    fun get_notification(): NotificationCompat.Builder? {
        if (!this.has_notification_permission()) {
            return null
        }

        if (this._active_notification == null) {
            this.get_notification_channel()
            val cancel_export_flag = "com.qfs.pagan.CANCEL_EXPORT_WAV"
            val pending_cancel_intent = PendingIntent.getBroadcast(
                this,
                0,
                Intent( cancel_export_flag),
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(this.getString(R.string.export_wav_notification_title, this.get_opus_manager().project_name))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.small_logo)
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
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notification_manager.createNotificationChannel(mChannel)
            mChannel
        } else {
            this._notification_channel!!
        }
    }

    fun dialog_color_picker(initial_color: Long, callback: (Int) -> Unit) {
        val main_fragment = this.get_active_fragment()
        val c = Color.toArgb(initial_color)

        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.color_picker,
                main_fragment!!.view as ViewGroup,
                false
            )
        val flColorDisplay = viewInflated.findViewById<FrameLayout>(R.id.flColorDisplay)
        val sbRed = viewInflated.findViewById<SeekBar>(R.id.sbRed)
        val sbGreen = viewInflated.findViewById<SeekBar>(R.id.sbGreen)
        val sbBlue = viewInflated.findViewById<SeekBar>(R.id.sbBlue)
        val rniRed = viewInflated.findViewById<RangedIntegerInput>(R.id.rniRed)
        val rniGreen = viewInflated.findViewById<RangedIntegerInput>(R.id.rniGreen)
        val rniBlue = viewInflated.findViewById<RangedIntegerInput>(R.id.rniBlue)

        rniRed.set_value(c.red)
        rniGreen.set_value(c.green)
        rniBlue.set_value(c.blue)

        sbRed.progress = c.red
        sbGreen.progress = c.green
        sbBlue.progress = c.blue

        var lockout = false
        rniRed.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                sbRed.progress = p0.toString().toInt()
                lockout = false
            }
        })
        rniGreen.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                sbGreen.progress = p0.toString().toInt()
                lockout = false
            }
        })

        rniBlue.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                sbBlue.progress = p0.toString().toInt()
                lockout = false
            }
        })

        val seekbar_listener = object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (lockout) {
                    return
                }
                lockout = true
                when (p0) {
                    sbRed -> rniRed.set_value(p1)
                    sbGreen -> rniGreen.set_value(p1)
                    sbBlue -> rniBlue.set_value(p1)
                }
                flColorDisplay.setBackgroundColor(Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0))
                lockout = false
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) { }
        }

        sbRed.setOnSeekBarChangeListener(seekbar_listener)
        sbGreen.setOnSeekBarChangeListener(seekbar_listener)
        sbBlue.setOnSeekBarChangeListener(seekbar_listener)


        flColorDisplay.setBackgroundColor(Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0))

        this._adjust_dialog_colors(
            AlertDialog.Builder(main_fragment.context, R.style.AlertDialog)
                .setView(viewInflated)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val new_color = Color.argb(255, rniRed.get_value()!!, rniGreen.get_value()!!, rniBlue.get_value()!!)
                    callback(new_color)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        )
    }

    fun is_night_mode(): Boolean {
        // defaults to night mode since it's better
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> true
        }
    }

    fun refresh_toolbar() {
        val color_map = this.view_model.color_map
        val toolbar = this._binding.appBarMain.toolbar
        val text_color = color_map[Palette.TitleBarText]
        toolbar.setTitleTextColor(text_color)
        toolbar.setBackgroundColor(color_map[Palette.TitleBar])
        toolbar.setSubtitleTextColor(text_color)
        toolbar.overflowIcon?.setTint(text_color)


        when (this.get_active_fragment()) {
            is FragmentEditor -> {
                toolbar.setNavigationIcon(R.drawable.hamburger_32)
                toolbar.navigationIcon?.setTint(text_color)
            }
            is FragmentLandingPage -> {
                toolbar.navigationIcon = null
            }
            else -> {
                toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
                toolbar.navigationIcon?.setTint(text_color)
            }
        }
    }

    fun get_night_palette(): HashMap<Palette, Int> {
        return hashMapOf<Palette, Int>(
            Pair(Palette.Background, this.getColor(R.color.dark_main_bg)),
            Pair(Palette.Foreground, this.getColor(R.color.dark_main_fg)),
            Pair(Palette.Lines, this.getColor(R.color.dark_table_lines)),
            Pair(Palette.Leaf, this.getColor(R.color.leaf)),
            Pair(Palette.LeafText, this.getColor(R.color.leaf_text)),
            Pair(Palette.LeafInvalid, this.getColor(R.color.leaf_invalid)),
            Pair(Palette.LeafInvalidText, this.getColor(R.color.leaf_invalid_text)),
            Pair(Palette.LeafInvalidSelected, this.getColor(R.color.leaf_invalid_selected)),
            Pair(Palette.LeafInvalidSelectedText, this.getColor(R.color.leaf_invalid_selected_text)),
            Pair(Palette.LeafSelected, this.getColor(R.color.leaf_selected)),
            Pair(Palette.LeafSelectedText, this.getColor(R.color.leaf_selected_text)),
            Pair(Palette.Link, this.getColor(R.color.leaf_linked)),
            Pair(Palette.LinkText, this.getColor(R.color.leaf_linked_text)),
            Pair(Palette.LinkSelected, this.getColor(R.color.leaf_linked_selected)),
            Pair(Palette.LinkSelectedText, this.getColor(R.color.leaf_linked_selected_text) ),
            Pair(Palette.LinkEmpty, this.getColor(R.color.dark_empty_linked)),
            Pair(Palette.LinkEmptySelected, this.getColor(R.color.dark_empty_linked_selected)),
            Pair(Palette.Selection, this.getColor(R.color.empty_selected)),
            Pair(Palette.SelectionText, this.getColor(R.color.empty_selected_text)),
            Pair(Palette.ChannelEven, this.getColor(R.color.dark_channel_even)),
            Pair(Palette.ChannelEvenText, this.getColor(R.color.dark_channel_even_text)),
            Pair(Palette.ChannelOdd, this.getColor(R.color.dark_channel_odd)),
            Pair(Palette.ChannelOddText, this.getColor(R.color.dark_channel_odd_text)),
            Pair(Palette.ColumnLabel, this.getColor(R.color.dark_main_bg)),
            Pair(Palette.ColumnLabelText, this.getColor(R.color.dark_main_fg)),
            Pair(Palette.Button, this.getColor(R.color.dark_button)),
            Pair(Palette.ButtonText, this.getColor(R.color.dark_button_text)),
            Pair(Palette.ButtonAlt, this.getColor(R.color.dark_button_alt)),
            Pair(Palette.ButtonAltText, this.getColor(R.color.dark_button_alt_text)),
            Pair(Palette.TitleBar, this.getColor(R.color.dark_primary)),
            Pair(Palette.TitleBarText, this.getColor(R.color.dark_primary_text)),
            Pair(Palette.CtlLine, this.getColor(R.color.dark_ctl_line)),
            Pair(Palette.CtlLineText, this.getColor(R.color.dark_ctl_line_text)),
            Pair(Palette.CtlLineSelection, this.getColor(R.color.ctl_line_selected)),
            Pair(Palette.CtlLineSelectionText, this.getColor(R.color.ctl_line_selected_text)),
            Pair(Palette.CtlLeaf, this.getColor(R.color.ctl_leaf)),
            Pair(Palette.CtlLeafText, this.getColor(R.color.ctl_leaf_text)),
            Pair(Palette.CtlLeafSelected, this.getColor(R.color.ctl_leaf_selected)),
            Pair(Palette.CtlLeafSelectedText, this.getColor(R.color.ctl_leaf_selected_text)),
        )
    }

    private fun get_day_palette(): HashMap<Palette, Int> {
        return hashMapOf<Palette, Int>(
            Pair(Palette.Background, this.getColor(R.color.light_main_bg)),
            Pair(Palette.Foreground, this.getColor(R.color.light_main_fg)),
            Pair(Palette.Lines, this.getColor(R.color.light_table_lines)),
            Pair(Palette.Leaf, this.getColor(R.color.leaf)),
            Pair(Palette.LeafText, this.getColor(R.color.leaf_text)),
            Pair(Palette.LeafInvalid, this.getColor(R.color.leaf_invalid)),
            Pair(Palette.LeafInvalidText, this.getColor(R.color.leaf_invalid_text)),
            Pair(Palette.LeafInvalidSelected, this.getColor(R.color.leaf_invalid_selected)),
            Pair(Palette.LeafInvalidSelectedText, this.getColor(R.color.leaf_invalid_selected_text)),
            Pair(Palette.LeafSelected, this.getColor(R.color.leaf_selected)),
            Pair(Palette.LeafSelectedText, this.getColor(R.color.leaf_selected_text)),
            Pair(Palette.Link, this.getColor(R.color.leaf_linked)),
            Pair(Palette.LinkText, this.getColor(R.color.leaf_linked_text)),
            Pair(Palette.LinkSelected, this.getColor(R.color.leaf_linked_selected)),
            Pair(Palette.LinkEmpty, this.getColor(R.color.light_empty_linked)),
            Pair(Palette.LinkEmptySelected, this.getColor(R.color.light_empty_linked_selected)),
            Pair(Palette.LinkSelectedText, this.getColor(R.color.leaf_linked_selected_text)),
            Pair(Palette.Selection, this.getColor(R.color.empty_selected)),
            Pair(Palette.SelectionText, this.getColor(R.color.empty_selected_text)),
            Pair(Palette.ChannelEven, this.getColor(R.color.light_channel_even)),
            Pair(Palette.ChannelEvenText, this.getColor(R.color.light_channel_even_text)),
            Pair(Palette.ChannelOdd, this.getColor(R.color.light_channel_odd)),
            Pair(Palette.ChannelOddText, this.getColor(R.color.light_channel_odd_text)),
            Pair(Palette.ColumnLabel, this.getColor(R.color.light_main_bg)),
            Pair(Palette.ColumnLabelText, this.getColor(R.color.light_main_fg)),
            Pair(Palette.Button, this.getColor(R.color.light_button)),
            Pair(Palette.ButtonText, this.getColor(R.color.light_button_text)),
            Pair(Palette.ButtonAlt, this.getColor(R.color.light_button_alt)),
            Pair(Palette.ButtonAltText, this.getColor(R.color.light_button_alt_text)),
            Pair(Palette.TitleBar, this.getColor(R.color.light_primary)),
            Pair(Palette.TitleBarText, this.getColor(R.color.light_primary_text)),
            Pair(Palette.CtlLine, this.getColor(R.color.light_ctl_line)),
            Pair(Palette.CtlLineText, this.getColor(R.color.light_ctl_line_text)),
            Pair(Palette.CtlLineSelection, this.getColor(R.color.ctl_line_selected)),
            Pair(Palette.CtlLineSelectionText, this.getColor(R.color.ctl_line_selected_text)),
            Pair(Palette.CtlLeaf, this.getColor(R.color.ctl_leaf)),
            Pair(Palette.CtlLeafText, this.getColor(R.color.ctl_leaf_text)),
            Pair(Palette.CtlLeafSelected, this.getColor(R.color.ctl_leaf_selected)),
            Pair(Palette.CtlLeafSelectedText, this.getColor(R.color.ctl_leaf_selected_text)),
        )
    }

}
