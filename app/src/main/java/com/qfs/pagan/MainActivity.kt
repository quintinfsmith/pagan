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
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.drawable.LayerDrawable
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
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
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.MidiController
import com.qfs.apres.MidiPlayer
import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.ActiveMidiAudioPlayer
import com.qfs.apres.soundfontplayer.MidiConverter
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.databinding.ActivityMainBinding
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.LoadedJSONData0
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.roundToInt
import com.qfs.pagan.InterfaceLayer as OpusManager


class MainActivity : AppCompatActivity() {
    enum class PlaybackState {
        NotReady,
        Ready,
        Playing,
        Queued,
        Stopping
    }

    class MainViewModel: ViewModel() {
        var export_handle: MidiConverter? = null
        var palette: ColorPalette? = null

        fun export_wav(activity: MainActivity, midi: Midi, target_file: File, handler: MidiConverter.ExporterEventHandler) {
            this.export_handle = MidiConverter(SampleHandleManager(activity.get_soundfont()!!, 44100))
            this.export_handle?.export_wav(midi, target_file, handler)
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
    private var _opus_manager = OpusManager(this)
    private lateinit var project_manager: ProjectManager
    lateinit var configuration: PaganConfiguration
    private lateinit var _config_path: String
    private var _number_selector_defaults = HashMap<String, Int>()
    var active_percussion_names = HashMap<Int, String>()

    private var _virtual_input_device = MidiPlayer()
    private lateinit var _midi_interface: MidiController
    private var _soundfont: SoundFont? = null
    private var _midi_playback_device: PaganPlaybackDevice? = null
    private var _midi_feedback_device: ActiveMidiAudioPlayer? = null
    private var _midi_feedback_dispatcher = MidiFeedbackDispatcher()

    private lateinit var _app_bar_configuration: AppBarConfiguration
    private lateinit var _binding: ActivityMainBinding
    private var _options_menu: Menu? = null
    private var _progress_bar: ProgressBar? = null
    var playback_state_soundfont: PlaybackState = PlaybackState.NotReady
    var playback_state_midi: PlaybackState = PlaybackState.NotReady
    private var _forced_title_text: String? = null

    // Notification shiz -------------------------------------------------
    var NOTIFICATION_ID = 0
    val CHANNEL_ID = "com.qfs.pagan"
    var notification_channel: NotificationChannel? = null
    var active_notification: NotificationCompat.Builder? = null
    // -------------------------------------------------------------------

    private var _export_wav_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        this.getNotificationPermission()
        thread {
            if (result.resultCode == Activity.RESULT_OK) {
                val opus_manager = this.get_opus_manager()
                result?.data?.data?.also { uri ->
                    if (this.view_model.export_handle != null) {
                        return@thread
                    }

                    val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                    if (tmp_file.exists()) {
                        tmp_file.delete()
                    }
                    tmp_file.deleteOnExit()

                    this.view_model.export_wav(this, opus_manager.get_midi(), tmp_file, object : MidiConverter.ExporterEventHandler {
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
                                tvExportProgress.text = "$progress_rounded%"
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
                    val json_string = Json.encodeToString(opus_manager.to_json())
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
                    if (fragment !is EditorFragment) {
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
                if (fragment is EditorFragment) {
                    fragment.test_flag = true
                }
                fragment?.setFragmentResult(
                    IntentFragmentToken.ImportMidi.name,
                    bundleOf(Pair("URI", uri.toString()))
                )

                if (fragment !is EditorFragment) {
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
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (this._midi_playback_device != null) {
            this.playback_state_soundfont = PlaybackState.Ready
        }

        if (this._midi_interface.output_devices_connected()) {
            this.playback_state_midi = PlaybackState.Ready
        }

    }

    fun save_to_backup() {
        val path = this.get_opus_manager().path
        this.get_opus_manager().save("${applicationInfo.dataDir}/.bkp.json")
        // saving changes the path, need to change it back
        this.get_opus_manager().path = path
    }

    override fun onSaveInstanceState(outState: Bundle) {
        this.save_to_backup()
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        "com.qfs.pagan.CANCEL_EXPORT_WAV" -> {
                            this@MainActivity.export_wav_cancel()
                        }

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

                    else -> { /* pass */
                    }
                }

                this@MainActivity.runOnUiThread {
                    this@MainActivity.update_menu_options()
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

                    else -> { /* pass */
                    }
                }

                // Kludge. need a sleep to give output devices a chance to disconnect
                Thread.sleep(1000)

                this@MainActivity.runOnUiThread {
                    this@MainActivity.update_menu_options()
                }
            }
        }

        this._midi_interface.connect_virtual_input_device(this._virtual_input_device)

        // Listens for SongPositionPointer (provided by midi) and scrolls to that beat
        this._midi_interface.connect_virtual_output_device(object : VirtualMidiOutputDevice {
            override fun onSongPositionPointer(event: SongPositionPointer) {
                this@MainActivity.get_opus_manager().cursor_select_column(event.get_beat(), true)
            }
        })

        this._midi_interface.connect_virtual_input_device(this._midi_feedback_dispatcher)


        this.project_manager = ProjectManager(this.getExternalFilesDir(null).toString())
        // Move files from applicationInfo.data to externalfilesdir (pre v1.1.2 location)
        val old_projects_dir = File("${applicationInfo.dataDir}/projects")
        if (old_projects_dir.isDirectory) {
            for (f in old_projects_dir.listFiles()!!) {
                val new_file_name = this.project_manager.get_new_path()
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
        this._binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(this._binding.root)
        setSupportActionBar(this._binding.appBarMain.toolbar)

        this.view_model.palette = this.configuration.palette ?: this.get_default_palette()
        this._binding.appBarMain.toolbar.background = null
        this._binding.appBarMain.toolbar.setTitleTextColor(this.view_model.palette!!.title_bar_text)
        this._binding.appBarMain.toolbar.setBackgroundColor(this.view_model.palette!!.title_bar)
        this._binding.appBarMain.toolbar.setSubtitleTextColor(this.view_model.palette!!.title_bar_text)
        this._binding.appBarMain.toolbar.overflowIcon?.setTint(this.view_model.palette!!.title_bar_text)
        this._binding.drawerLayout.setBackgroundColor(this.view_model.palette!!.background)

        /*
            TODO: I think this setOf may be making my navigation more complicated
            than it needs to be. Needs investigation.
         */
        this._app_bar_configuration = AppBarConfiguration(
            setOf(
                R.id.FrontFragment,
                R.id.EditorFragment
            )
        )

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        setupActionBarWithNavController(navController, this._app_bar_configuration)

        //////////////////////////////////////////
        if (this.configuration.soundfont != null) {
            val path = "${this.getExternalFilesDir(null)}/SoundFonts/${this.configuration.soundfont}"
            val sf_file = File(path)
            if (sf_file.exists()) {
                this._soundfont = SoundFont(path)
                this._midi_playback_device = PaganPlaybackDevice(this)
                if (!this._midi_interface.output_devices_connected()) {
                    this._midi_feedback_device = ActiveMidiAudioPlayer(
                        SampleHandleManager(
                            this._soundfont!!,
                            this.configuration.sample_rate
                        )
                    )
                    this._midi_interface.connect_virtual_output_device(this._midi_feedback_device!!)
                }
            }
            this.update_channel_instruments()

            val channel_recycler = this@MainActivity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            val soundfont = this@MainActivity.get_soundfont()
            if (channel_recycler != null && channel_recycler.adapter != null && soundfont != null) {
                (channel_recycler.adapter as ChannelOptionAdapter).set_soundfont(soundfont)
            }
        }
        ///////////////////////////////////////////

        when (navController.currentDestination?.id) {
            R.id.EditorFragment -> {
                this.drawer_unlock()
            }

            else -> {
                this.drawer_lock()
            }
        }

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
                    ChannelOptionAdapter(this@MainActivity._opus_manager, channel_recycler)
                }
                val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                if (channel_adapter.itemCount == 0) {
                    channel_adapter.setup()
                }
                super.onDrawerOpened(drawerView)

                this@MainActivity.playback_stop()
                this@MainActivity.playback_stop_midi_output()
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
                if (fragment is EditorFragment && this._binding.root.getDrawerLockMode(this.findViewById(R.id.config_drawer)) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                    this.drawer_open()
                }
            }

            R.id.itmNewProject -> {
                this.dialog_save_project {
                    val fragment = this.get_active_fragment()
                    fragment?.clearFragmentResult(IntentFragmentToken.Resume.name)
                    fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
                    if (fragment !is EditorFragment) {
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
                    else -> { /* pass  */ }
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
        this.project_manager.save(this._opus_manager)
        this.feedback_msg(getString(R.string.feedback_project_saved))
        this.update_menu_options()
    }

    private fun project_delete() {
        this.project_manager.delete(this._opus_manager)

        val fragment = this.get_active_fragment()
        fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
        this.navigate(R.id.EditorFragment)

        this.feedback_msg(resources.getString(R.string.feedback_delete, this.title))
    }

    private fun project_move_to_copy() {
        this.project_manager.move_to_copy(this._opus_manager)
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

        val start_point = this.get_working_column()
        // Currently, Midi2.0 output is not supported. will be needed for N-radix projects
        thread {
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

        val start_point = this.get_working_column()
        val opus_manager = this.get_opus_manager()
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
        return this.project_manager.get_new_path()
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
        if (title_msg != null) {
            this.force_title_text(title_msg)
        }
        this.runOnUiThread {
            if (this@MainActivity._progress_bar == null) {
                this@MainActivity._progress_bar = PaganProgressBar(this@MainActivity)
            }
            this@MainActivity._progress_bar!!.isClickable = true
            val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(50, 50)
            params.addRule(RelativeLayout.CENTER_IN_PARENT)
            val parent = this@MainActivity._progress_bar!!.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(this@MainActivity._progress_bar)
            }

            this@MainActivity._binding.root.addView(this@MainActivity._progress_bar, params)
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
            this.drawer_unlock()
        } else {
            this.drawer_lock()
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
                    is GlobalSettingsFragment -> {
                        resources.getString(R.string.settings_fragment_label)
                    }
                    is LandingPageFragment -> {
                        "${getString(R.string.app_name)} ${getString(R.string.app_version)}"
                    }
                    else -> {
                        this.get_opus_manager().project_name
                    }
                }
            }
        )
    }

    fun set_title_text(new_text: String) {
        this._binding.appBarMain.toolbar.title = new_text
        if (this._binding.appBarMain.toolbar.navigationIcon !is DrawerArrowDrawable) {
            if (this.get_active_fragment() !is LandingPageFragment) {
                this._binding.appBarMain.toolbar.setNavigationIcon(R.drawable.hamburger_32)
                this._binding.appBarMain.toolbar.navigationIcon?.setTint(this.view_model.palette!!.title_bar_text)
            }
        } else {
            (this._binding.appBarMain.toolbar.navigationIcon as DrawerArrowDrawable).color = this.view_model.palette!!.title_bar_text
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

    fun update_menu_options() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val options_menu = this._options_menu ?: return
        options_menu.setGroupDividerEnabled(true)
        when (navHost?.childFragmentManager?.fragments?.get(0)) {
            is EditorFragment -> {
                val play_midi_visible = (this._midi_interface.output_devices_connected() && this.get_opus_manager().is_tuning_standard())
                options_menu.findItem(R.id.itmLoadProject).isVisible = this.has_projects_saved()
                options_menu.findItem(R.id.itmUndo).isVisible = true
                options_menu.findItem(R.id.itmNewProject).isVisible = true
                options_menu.findItem(R.id.itmPlay).isVisible = this._soundfont != null && ! play_midi_visible
                options_menu.findItem(R.id.itmPlayMidiOutput).isVisible = play_midi_visible
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

        val tvTempo: TextView = this.findViewById(R.id.tvTempo)
        tvTempo.text = this.getString(R.string.label_bpm, opus_manager.tempo.toInt())
        tvTempo.setOnClickListener {
            this.dialog_number_input(
                getString(R.string.dlg_set_tempo),
                1,
                999,
                opus_manager.tempo.toInt()
            ) { tempo: Int ->
                opus_manager.set_tempo(tempo.toFloat())
            }
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

            val etRadix = viewInflated.findViewById<RangedNumberInput>(R.id.etRadix)
            val etTranspose = viewInflated.findViewById<RangedNumberInput>(R.id.etTranspose)
            etTranspose.set_range(0, radix - 1)
            etTranspose.set_value(opus_manager.transpose)

            val rvTuningMap = viewInflated.findViewById<TuningMapRecycler>(R.id.rvTuningMap)
            rvTuningMap.adapter = TuningMapRecyclerAdapter(rvTuningMap, opus_manager.tuning_map.clone())


            val dialog = AlertDialog.Builder(main_fragment.context, R.style.AlertDialog)
                .setCustomTitle(this._build_dialog_title_view(
                     resources.getString(R.string.dlg_tuning)
                ))
                .setView(viewInflated)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    opus_manager.remember {
                        opus_manager.set_tuning_map(
                            (rvTuningMap.adapter as TuningMapRecyclerAdapter).tuning_map
                        )
                        opus_manager.set_transpose(etTranspose.get_value() ?: 0)
                    }

                    dialog.dismiss()
                }
                .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()

            this._adjust_dialog_colors(dialog)


            val default_value = opus_manager.tuning_map.size

            etRadix.set_value(default_value)
            etRadix.value_set_callback = {
                val new_radix = it.get_value()
                rvTuningMap.reset_tuning_map(new_radix)
                etTranspose.set_value(0)
                etTranspose.set_range(0, (new_radix ?: 12) - 1)
            }
        }
        //-------------------------------------------

        //btnRadix.text = this.getString(R.string.label_radix, radix)
        //btnRadix.setOnClickListener {
        //    this.dialog_number_input(
        //        getString(R.string.dlg_set_radix),
        //        2,
        //        24,
        //        radix
        //    ) { radix: Int ->
        //        opus_manager.set_radix(radix)
        //    }
        //}

        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            opus_manager.new_channel()
        }

        this.setup_project_config_drawer_export_button()
        this.findViewById<View>(R.id.btnSaveProject).setOnClickListener {
            this.project_save()
        }
        this.findViewById<View>(R.id.btnSaveProject).setOnLongClickListener {
            this.export_project()
            true
        }

        val btnDeleteProject = this.findViewById<View>(R.id.btnDeleteProject)
        val btnCopyProject = this.findViewById<View>(R.id.btnCopyProject)
        if (opus_manager.path != null && File(opus_manager.path!!).isFile) {
            btnDeleteProject.visibility = View.VISIBLE
            btnCopyProject.visibility = View.VISIBLE
        } else {
            btnDeleteProject.visibility = View.GONE
            btnCopyProject.visibility = View.GONE
        }
        btnDeleteProject.setOnClickListener {
            this.dialog_delete_project()
        }
        btnCopyProject.setOnClickListener {
            this.project_move_to_copy()
            this.drawer_close()
        }
    }
    private fun _build_dialog_title_view(text: String): TextView {
        val output = PaganTextView(ContextThemeWrapper(this, R.style.dialog_title))
        output.text = text
        return output
    }
    internal fun _adjust_dialog_colors(dialog: AlertDialog) {
        val palette = this.view_model.palette!!

        dialog.window!!.decorView.background.setTint(palette.background)
        dialog.window!!.decorView.background.setTint(palette.background)
        val padding = this.resources.getDimension(R.dimen.alert_padding).roundToInt()
        dialog.window!!.decorView.setPadding(padding, padding, padding, padding)

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
            (this.layoutParams as MarginLayoutParams).marginEnd = this@MainActivity.resources.getDimension(R.dimen.alert_padding).toInt()
            this.backgroundTintList = null
            this.background = this@MainActivity.getDrawable(R.drawable.button)
            (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
                .setTint(palette.button_alt)
            (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_stroke)
                .setTint(palette.button_alt_stroke)
            this.setTextColor(palette.button_alt_text)
        }

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).apply {
            this.background = null
            this.setTextColor(palette.foreground)
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).apply {
            this.backgroundTintList = null
            this.background = this@MainActivity.getDrawable(R.drawable.button)
            (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
                .setTint(palette.button)
            (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_stroke)
                .setTint(palette.button_stroke)
            this.setTextColor(palette.button_text)
        }
    }


    private fun setup_project_config_drawer_export_button() {
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

    private fun get_drum_options(): List<Pair<String, Int>> {
        if (this._soundfont == null) {
            return listOf()
        }
        val opus_manager = this.get_opus_manager()

        val (bank, program) = opus_manager.get_channel_instrument(opus_manager.channels.size - 1)
        val preset = try {
            this._soundfont!!.get_preset(program, bank)
        } catch (e: SoundFont.InvalidPresetIndex) {
            return listOf()
        }
        val available_drum_keys = mutableSetOf<Pair<String, Int>>()

        for (preset_instrument in preset.instruments) {
            if (preset_instrument.instrument == null) {
                continue
            }

            for (sample in preset_instrument.instrument!!.samples) {
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

    fun update_channel_instruments(index: Int? = null) {
        if (index == null) {
            for (channel in this._opus_manager.channels) {
                this._midi_interface.broadcast_event(BankSelect(channel.midi_channel, channel.midi_bank))
                this._midi_interface.broadcast_event(ProgramChange(channel.midi_channel, channel.midi_program))
            }
        } else {
            val opus_channel = this.get_opus_manager().channels[index]
            this._midi_interface.broadcast_event(BankSelect(opus_channel.midi_channel, opus_channel.midi_bank))
            this._midi_interface.broadcast_event(ProgramChange(opus_channel.midi_channel, opus_channel.midi_program))
        }
    }

    fun get_opus_manager(): OpusManager {
        return this._opus_manager
    }

    fun play_event(channel: Int, event_value: Int, velocity: Int) {
        if (!this._midi_interface.output_devices_connected()) {
            this.connect_feedback_device()
        } else {
            this.disconnect_feedback_device()
            this._midi_interface.open_output_devices()
        }

        val opus_manager = this._opus_manager
        val midi_channel = opus_manager.channels[channel].midi_channel

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

        this._midi_feedback_dispatcher.play_note(
            midi_channel,
            note,
            bend,
            velocity,
            !opus_manager.is_tuning_standard() || ! this.is_connected_to_physical_device()
        )
    }

    fun import_project(path: String) {
        this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this._opus_manager.load(bytes, this.project_manager.get_new_path())
        }
    }

    fun import_midi(path: String) {
        val bytes = this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            FileInputStream(it.fileDescriptor).readBytes()
        } ?: return // TODO. throw error?

        val midi = try {
            Midi.from_bytes(bytes)
        } catch (e: Exception) {
            throw InvalidMIDIFile(path)
        }
        this._opus_manager.import_midi(midi)
        val filename = this.parse_file_name(Uri.parse(path))
        val new_path = this.project_manager.get_new_path()

        this._opus_manager.path = new_path
        this._opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: getString(R.string.default_imported_midi_title))
        this._opus_manager.clear_history()
    }

    fun has_projects_saved(): Boolean {
        return this.project_manager.has_projects_saved()
    }

    fun is_fluid_soundfont_available(): Boolean {
        val filename = getString(R.string.fluid_font_filename)
        val soundfont_dir = this.get_soundfont_directory()
        val fluid_file = File("${soundfont_dir.path}/$filename")
        return fluid_file.exists()
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

        if (this._midi_feedback_device != null) {
            this.disconnect_feedback_device()
        }

        val path = "${this.getExternalFilesDir(null)}/SoundFonts/$filename"
        this.configuration.soundfont = filename
        this._soundfont = SoundFont(path)

        this._midi_playback_device = PaganPlaybackDevice(this)

        this._midi_feedback_device = ActiveMidiAudioPlayer(
            SampleHandleManager(
                this._soundfont!!,
                this.configuration.sample_rate
            )
        )

        this._midi_interface.connect_virtual_output_device(this._midi_feedback_device!!)

        this.update_channel_instruments()

        if (this.get_opus_manager().channels.size > 0) {
            this.populate_active_percussion_names()
        }

        this.setup_project_config_drawer_export_button()

        val channel_recycler = this.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        // Should always be null since this can only be changed from a different menu
        if (channel_recycler.adapter != null) {
            val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
            channel_adapter.set_soundfont(this._soundfont!!)
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
        if (this._midi_feedback_device != null) {
            this.disconnect_feedback_device()
        }
        this._soundfont = null
        this.configuration.soundfont = null
        this._midi_playback_device = null
        this._midi_feedback_device = null

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
            this.configuration.save(this._config_path)
        } catch (e: FileNotFoundException) {
            // TODO: ?Feedback? only happens on devices not properly put together (realme)
        }
    }

    fun get_drum_name(index: Int): String? {
        if (this.active_percussion_names.isEmpty()) {
            this.populate_active_percussion_names()
        }
        return this.active_percussion_names[index + 27]
    }

    fun populate_active_percussion_names() {
        this.active_percussion_names.clear()
        for ((name, note) in this.get_drum_options()) {
            // TODO: *Maybe* Allow drum instruments below 27? not sure what the standard is.
            //  I thought 27 was the lowest, but i'll come up with something later
            if (note >= 27) {
                this.active_percussion_names[note] = name
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
        val close_button = viewInflated.findViewById<TextView>(R.id.btnClose)

        title_view.text = title

        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setView(viewInflated)
            .show()
        this._adjust_dialog_colors(dialog)

        close_button.setOnClickListener {
            dialog.dismiss()
        }

        val adapter = PopupMenuRecyclerAdapter<T>(recycler, options, default) { index: Int, value: T ->
            dialog.dismiss()
            callback(index, value)
        }

        adapter.notifyDataSetChanged()

        val windowMetrics = this.windowManager.currentWindowMetrics
        val max_width: Int = (windowMetrics.bounds.width().toFloat() * .8).toInt()
        val max_height: Int = (windowMetrics.bounds.height().toFloat() * .8).toInt()

        dialog.window!!.setLayout(max_width, max_height)
    }

    internal fun dialog_load_project() {
        val json = Json {
            ignoreUnknownKeys = true
        }
        val project_list = mutableListOf<Pair<String, String>>()
        for (json_file in this.get_project_directory().listFiles()!!) {
            val content = json_file.readText(Charsets.UTF_8)
            val json_obj: LoadedJSONData = try {
                json.decodeFromString(content)
            } catch (e: Exception) {
                val old_data = json.decodeFromString<LoadedJSONData0>(content)
                this.get_opus_manager().convert_old_fmt(old_data)
            }
            project_list.add(Pair(json_file.path, json_obj.name))
        }
        project_list.sortBy { it.second }
        this.dialog_popup_menu("Load Project", project_list) { index: Int, path: String ->
            val fragment = this.get_active_fragment() ?: return@dialog_popup_menu
            this.loading_reticle_show(getString(R.string.reticle_msg_load_project))

            fragment.setFragmentResult(
                IntentFragmentToken.Load.name,
                bundleOf(Pair("PATH", path))
            )

            if (fragment !is EditorFragment) {
                this.navigate(R.id.EditorFragment)
            }
        }
    }

    internal fun dialog_number_input(title: String, min_value: Int, max_value: Int, default: Int? = null, callback: (value: Int) -> Unit ) {
        val coerced_default_value = default ?: (this._number_selector_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedNumberInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setCustomTitle(this._build_dialog_title_view(title))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this._number_selector_defaults[title] = output_value
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

        number_input.value_set_callback = {
            callback(it.get_value() ?: coerced_default_value)
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


    private fun dialog_save_project(callback: () -> Unit) {
        if (this.get_opus_manager().has_changed_since_save()) {
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

        val title = this.get_opus_manager().project_name
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
        val fragment = this.get_active_fragment()
        if (fragment is EditorFragment) {
            fragment.view_model.resume_block = true
        }
        this._import_midi_intent_launcher.launch(intent)
    }

    fun select_project_file() {
        val intent = Intent()
            .setType("application/json")
            .setAction(Intent.ACTION_GET_CONTENT)
        this._import_project_intent_launcher.launch(intent)
    }

    fun get_export_name(): String {
        val reserved_chars = "|\\?*<\":>+[]/'"
        var base_name = this.get_opus_manager().project_name
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

    fun get_project_directory(): File {
        return this.project_manager.get_directory()
    }

    fun set_sample_rate(new_sample_rate: Int) {
        this.configuration.sample_rate = new_sample_rate

        this._midi_playback_device?.kill()

        if (this.get_soundfont() != null) {
            this._midi_playback_device = PaganPlaybackDevice(this)
        } else {
           this._midi_playback_device = null
        }
    }

    fun validate_percussion_visibility() {
        val opus_manager = this.get_opus_manager()
        if (!this.configuration.show_percussion && opus_manager.has_percussion()) {
            this.configuration.show_percussion = true
            this.save_configuration()
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
                cursor.range!!.first.beat
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
        if (this._midi_feedback_device == null || !this._midi_interface.is_connected(this._midi_feedback_device!!)) {
            return
        }

        this._midi_feedback_dispatcher.close()
        this._midi_interface.disconnect_virtual_output_device( this._midi_feedback_device!! )
    }

    fun connect_feedback_device() {
        if (this._midi_feedback_device == null && this.configuration.soundfont != null) {
            this.set_soundfont(this.configuration.soundfont)
        }

        if (this._midi_feedback_device == null || this._midi_interface.is_connected(this._midi_feedback_device!!)) {
            return
        }

        this._midi_interface.connect_virtual_output_device( this._midi_feedback_device!! )
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

        if (this.active_notification == null) {
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

            this.active_notification = builder
        }

        return this.active_notification!!
    }

    fun get_notification_channel(): NotificationChannel? {
        return if (this.has_notification_permission()) {
            null
        } else if (this.notification_channel == null) {
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
            this.notification_channel!!
        }
    }

    fun dialog_color_picker(red: Int, green: Int, blue: Int, callback: (Int, Int, Int) -> Unit) {
        val main_fragment = this.get_active_fragment()

        val sbRed = this.findViewById<SeekBar>(R.id.sbRed)
        val sbGreen = this.findViewById<SeekBar>(R.id.sbGreen)
        val sbBlue = this.findViewById<SeekBar>(R.id.sbBlue)
        val rniRed = this.findViewById<RangedNumberInput>(R.id.rniRed)
        val rniGreen = this.findViewById<RangedNumberInput>(R.id.rniGreen)
        val rniBlue = this.findViewById<RangedNumberInput>(R.id.rniBlue)
        val hex_view = this.findViewById<EditText>(R.id.etHexView)

        rniRed.set_value(red)
        rniGreen.set_value(green)
        rniBlue.set_value(blue)

        sbRed.progress = red
        sbGreen.progress = green
        sbBlue.progress = blue

        fun update_hex_view() {
            val hex_r = Integer.toHexString(sbRed.progress)
            val hex_g = Integer.toHexString(sbGreen.progress)
            val hex_b = Integer.toHexString(sbBlue.progress)
            hex_view.setText("#$hex_r$hex_g$hex_b")
        }

        val change_listener = object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                when (p0) {
                    sbRed -> rniRed.set_value(p1)
                    sbGreen -> rniGreen.set_value(p1)
                    sbBlue -> rniBlue.set_value(p1)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                update_hex_view()
            }
        }

        sbRed.setOnSeekBarChangeListener(change_listener)
        sbGreen.setOnSeekBarChangeListener(change_listener)
        sbBlue.setOnSeekBarChangeListener(change_listener)


        AlertDialog.Builder(main_fragment!!.context, R.style.AlertDialog)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                callback(rniRed.get_value()!!, rniGreen.get_value()!!, rniBlue.get_value()!!)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    fun is_night_mode(): Boolean {
        // defaults to night mode since it's better
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> true
        }
    }

    fun get_default_palette(): ColorPalette {
        val night_mode = this.is_night_mode()
        return ColorPalette(
            "Default",
            background = this.getColor(if (night_mode) R.color.dark_main_bg else R.color.light_main_bg),
            foreground = this.getColor(if (night_mode) R.color.dark_main_fg else R.color.light_main_fg),
            lines = this.getColor(if (night_mode) R.color.dark_table_lines else R.color.light_table_lines),
            leaf = this.getColor(R.color.leaf),
            leaf_text = this.getColor(R.color.leaf_text),
            leaf_selected = this.getColor(R.color.leaf_selected),
            leaf_selected_text = this.getColor(R.color.leaf_selected_text),
            link = this.getColor(R.color.leaf_linked),
            link_text = this.getColor(R.color.leaf_linked_text),
            link_selected = this.getColor(R.color.leaf_linked_selected),
            link_selected_text = this.getColor(R.color.leaf_linked_selected_text),
            link_empty = this.getColor(R.color.empty_linked),
            link_empty_selected = this.getColor(R.color.empty_linked_selected),
            selection = this.getColor(R.color.empty_selected),
            label_selected = this.getColor(R.color.empty_selected),
            label_selected_text = this.getColor(R.color.empty_selected_text),
            channel_even = this.getColor(if (night_mode) R.color.dark_channel_even else R.color.light_channel_even),
            channel_odd = this.getColor(if (night_mode) R.color.dark_channel_odd else R.color.light_channel_odd),
            channel_even_text = this.getColor(if (night_mode) R.color.dark_channel_even_text else R.color.light_channel_even_text),
            channel_odd_text = this.getColor(if (night_mode) R.color.dark_channel_odd_text else R.color.light_channel_odd_text),
            column_label = this.getColor(if (night_mode) R.color.dark_main_bg else R.color.light_main_bg),
            column_label_text = this.getColor(if (night_mode) R.color.dark_main_fg else R.color.light_main_fg),
            button = this.getColor(if (night_mode) R.color.dark_button else R.color.light_button),
            button_alt = this.getColor(if (night_mode) R.color.dark_button_alt else R.color.light_button_alt),
            button_selected = this.getColor(if (night_mode) R.color.dark_button_selected else R.color.light_button_selected),
            button_stroke = this.getColor(if (night_mode) R.color.dark_button_stroke else R.color.light_button_stroke),
            button_alt_stroke = this.getColor(if (night_mode) R.color.dark_button_alt_stroke else R.color.light_button_alt_stroke),
            button_selected_stroke = this.getColor(if (night_mode) R.color.dark_button_selected_stroke else R.color.light_button_selected_stroke),
            button_text = this.getColor(if (night_mode) R.color.dark_button_text else R.color.light_button_text),
            button_alt_text = this.getColor(if (night_mode) R.color.dark_button_alt_text else R.color.light_button_alt_text),
            button_selected_text = this.getColor(if (night_mode) R.color.dark_button_selected_text else R.color.light_button_selected_text),
            title_bar = this.getColor(if (night_mode) R.color.dark_primary else R.color.light_primary),
            title_bar_text = this.getColor(if (night_mode) R.color.dark_primary_text else R.color.light_primary_text),


        )

        //    leaf_text = Color.parseColor("#000000"),
        //    leaf_selected = Color.parseColor("#000000"),
        //    leaf_selected_text = Color.parseColor("#000000"),
        //    link = Color.parseColor("#000000"),
        //    link_text = Color.parseColor("#000000"),
        //    link_selected = Color.parseColor("#000000"),
        //    link_selected_text = Color.parseColor("#000000"),
        //    label_selected = Color.parseColor("#000000"),
        //    label_selected_text = Color.parseColor("#000000"),
        //    channel_even = Color.parseColor("#000000"),
        //    channel_even_text = Color.parseColor("#000000"),
        //    channel_odd = Color.parseColor("#000000"),
        //    channel_odd_text = Color.parseColor("#000000")
        //)
    }

}
