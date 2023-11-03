package com.qfs.pagan

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.Cursor
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
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
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.ActiveMidiAudioPlayer
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import com.qfs.pagan.InterfaceLayer as OpusManager


/**
 * Device Scanning
 * Methods are called by the system whenever the set of attached devices changes.
 */
class MainActivity : AppCompatActivity() {
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
    private var _midi_feedback_dispatcher = MidiFeedBackDispatcher()

    private lateinit var _app_bar_configuration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var _options_menu: Menu? = null
    private var _progress_bar: ProgressBar? = null

    private var _exporting_wav_handle: PaganPlaybackDevice? = null

    private var _export_wav_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        thread {
            if (result.resultCode == Activity.RESULT_OK && this._exporting_wav_handle == null) {
                val opus_manager = this.get_opus_manager()
                result?.data?.data?.also { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                        this._exporting_wav_handle = PaganPlaybackDevice(this, 44100)
                        this._exporting_wav_handle!!.export_wav(opus_manager.get_midi(), it)
                        this._exporting_wav_handle = null
                    }
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
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val path = this.get_opus_manager().path
        this.get_opus_manager().save("${applicationInfo.dataDir}/.bkp.json")
        // saving changes the path, need to change it back
        this.get_opus_manager().path = path

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (this._exporting_wav_handle != null) {
            this._exporting_wav_handle!!.export_wav_cancel()
            this._exporting_wav_handle = null
        }
        super.onDestroy()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.registerReceiver(
            object: BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        "com.qfs.pagan.CANCEL_EXPORT_WAV" -> {
                            this@MainActivity.export_wav_cancel()
                        }
                        else -> { }
                    }
                }
            },
            IntentFilter("com.qfs.pagan.CANCEL_EXPORT_WAV"),
            RECEIVER_NOT_EXPORTED
        )


        this._midi_interface = object: MidiController(this) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                thread {
                    Thread.sleep(300) // kludge

                    if (this@MainActivity._midi_feedback_device != null) {
                        this@MainActivity.playback_stop()
                        this@MainActivity._midi_interface.disconnect_virtual_output_device(
                            this@MainActivity._midi_feedback_device!!
                        )
                    }

                    this@MainActivity.runOnUiThread {
                        this@MainActivity.update_menu_options()
                    }
                }
            }
            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                this@MainActivity.runOnUiThread {
                    if (!this@MainActivity._midi_interface.output_devices_connected()) {
                        this@MainActivity.update_menu_options()
                        this@MainActivity.playback_stop()
                        if (this@MainActivity.configuration.soundfont != null) {
                            this@MainActivity.set_soundfont(this@MainActivity.configuration.soundfont)
                        }
                    }
                }
            }
        }

        this._midi_interface.connect_virtual_input_device(this._virtual_input_device)
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

        this.configuration = PaganConfiguration.from_path(this._config_path)
        this.binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(this.binding.root)
        setSupportActionBar(this.binding.appBarMain.toolbar)

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
                if (!this._midi_interface.output_devices_connected()) {
                    this._midi_playback_device = PaganPlaybackDevice(this)
                    this._midi_feedback_device = ActiveMidiAudioPlayer(
                        SampleHandleManager(
                            this._soundfont!!,
                            22050
                        )
                    )
                    this._midi_interface.connect_virtual_output_device(this._midi_feedback_device!!)
                }
            }
            this.update_channel_instruments()
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
                    this@MainActivity.dialog_save_project {
                        finish()
                    }
                } else {
                    navController.popBackStack()
                }

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
        if (item.itemId != R.id.itmPlay) {
            this.playback_stop()
        }

        when (item.itemId) {
            R.id.itmNewProject -> {
                this.dialog_save_project {
                    val fragment = this.get_active_fragment()
                    fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
                    if (fragment !is EditorFragment) {
                        this.navigate(R.id.EditorFragment)
                    }
                }
            }

            R.id.itmLoadProject -> {
                this.dialog_save_project {
                    this.navigate(R.id.LoadFragment)
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
                if (this._midi_playback_device?.is_playing() ?: (this._midi_interface.output_devices_connected() && this._virtual_input_device.playing)) {
                   this.playback_stop()
                } else {
                    this.playback_start()
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

    private fun playback_start() {
        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay)
        if (blocker_view != null) {
            blocker_view.visibility = View.VISIBLE
            blocker_view.setOnClickListener {
                this.playback_stop()
            }
        }

        this.runOnUiThread {
            val play_pause_button = this._options_menu!!.findItem(R.id.itmPlay)
            if (play_pause_button != null) {
                play_pause_button.icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_pause_24)
            }
        }

        val cursor = this.get_opus_manager().cursor
        val start_point = when (cursor.mode) {
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

        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (this._midi_interface.output_devices_connected()) {
            this.playback_start_midi_device(start_point)
        } else if (this._midi_playback_device != null) {
            this.playback_start_precached(start_point)
        }
    }

    private fun playback_start_precached(start_beat: Int) {
        thread {
            this._midi_playback_device?.play_midi(this.get_opus_manager().get_midi(start_beat))
        }
    }

    private fun playback_start_midi_device(start_point: Int = 0) {
        var opus_manager = this.get_opus_manager()
        val midi = opus_manager.get_midi(start_point)

        thread {
            try {
                this._virtual_input_device.play_midi(midi) {
                    this.runOnUiThread {
                        this.playback_stop()
                    }
                }
            } catch (e: java.io.IOException) {
                this.runOnUiThread {
                    this.playback_stop()
                }
            }
        }
    }

    internal fun playback_stop() {
        this.runOnUiThread {
            val play_pause_button = this._options_menu?.findItem(R.id.itmPlay) ?: return@runOnUiThread
            play_pause_button.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_play_arrow_24)
        }
        this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (this._virtual_input_device.playing) {
            this._virtual_input_device.stop()
        }

        if (this._midi_playback_device != null) {
            this._midi_playback_device!!.kill()
        }

        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay) ?: return
        blocker_view.visibility = View.GONE
    }

    fun get_new_project_path(): String {
        return this.project_manager.get_new_path()
    }

    // Ui Wrappers ////////////////////////////////////////////
    private fun drawer_close() {
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()
    }

    fun drawer_lock() {
        this.binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun drawer_unlock() {
        try {
            this.binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } catch (e: UninitializedPropertyAccessException) {
            // pass, if it's not initialized, it's not locked
        }
    }

    fun feedback_msg(msg: String) {
        this.runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun loading_reticle_show() {
        runBlocking {
            this@MainActivity.runOnUiThread {
                if (this@MainActivity._progress_bar == null) {
                    this@MainActivity._progress_bar = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleLarge)
                }
                val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(50, 50)
                params.addRule(RelativeLayout.CENTER_IN_PARENT)
                val parent = this@MainActivity._progress_bar!!.parent
                if (parent != null) {
                    (parent as ViewGroup).removeView(this@MainActivity._progress_bar)
                }
                this@MainActivity.binding.root.addView(this@MainActivity._progress_bar, params)
            }
        }
    }

    fun loading_reticle_hide() {
        this.runOnUiThread {
            val progressBar = this._progress_bar ?: return@runOnUiThread
            if (progressBar.parent != null) {
                (progressBar.parent as ViewGroup).removeView(progressBar)
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
        this.set_title_text(this.get_opus_manager().project_name)
    }

    fun set_title_text(new_text: String) {
        this.binding.appBarMain.toolbar.title = new_text
    }

    fun update_menu_options() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        if (this._options_menu == null) {
            return
        }
        this._options_menu?.setGroupDividerEnabled(true)
        when (navHost?.childFragmentManager?.fragments?.get(0)) {
            is EditorFragment -> {
                this._options_menu!!.findItem(R.id.itmLoadProject).isVisible = this.has_projects_saved()
                this._options_menu!!.findItem(R.id.itmUndo).isVisible = true
                this._options_menu!!.findItem(R.id.itmNewProject).isVisible = true
                this._options_menu!!.findItem(R.id.itmPlay).isVisible = (this._soundfont != null || this._midi_interface.output_devices_connected())
                this._options_menu!!.findItem(R.id.itmImportMidi).isVisible = true
                this._options_menu!!.findItem(R.id.itmImportProject).isVisible = true
                this._options_menu!!.findItem(R.id.itmSettings).isVisible = true
            }

            else -> {
                this._options_menu!!.findItem(R.id.itmLoadProject).isVisible = false
                this._options_menu!!.findItem(R.id.itmUndo).isVisible = false
                this._options_menu!!.findItem(R.id.itmNewProject).isVisible = false
                this._options_menu!!.findItem(R.id.itmPlay).isVisible = false
                this._options_menu!!.findItem(R.id.itmImportMidi).isVisible = false
                this._options_menu!!.findItem(R.id.itmImportProject).isVisible = false
                this._options_menu!!.findItem(R.id.itmSettings).isVisible = false
            }
        }
    }

    fun setup_project_config_drawer() {
        val opus_manager = this.get_opus_manager()
        val rvActiveChannels: ChannelOptionRecycler = this.findViewById(R.id.rvActiveChannels)
        ChannelOptionAdapter(opus_manager, rvActiveChannels)

        val tvChangeProjectName: TextView = this.findViewById(R.id.tvChangeProjectName)
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

        val btnTranspose: TextView = this.findViewById(R.id.btnTranspose)
        btnTranspose.text = this.getString(
            R.string.label_transpose,
            get_number_string(opus_manager.transpose, opus_manager.radix, 1)
        )

        btnTranspose.setOnClickListener {
            this.dialog_number_input("Transpose", 0, this.get_opus_manager().radix - 1) { value: Int ->
                opus_manager.set_transpose(value)
            }
        }

        val btnRadix: TextView = this.findViewById(R.id.btnRadix)
        btnRadix.text = this.getString( R.string.label_radix, opus_manager.radix )
        btnRadix.setOnClickListener {
            this.dialog_number_input(
                getString(R.string.dlg_set_radix),
                2,
                24,
                opus_manager.radix
            ) { radix: Int ->
                opus_manager.set_radix(radix)

            }
        }

        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            opus_manager.new_channel()
        }

        this.findViewById<View>(R.id.btnExportProject).setOnClickListener {
            var export_options = if (this.get_opus_manager().radix == 12) {
                listOf(
                    Pair(0, "Midi File"),
                    Pair(1, "Wav File")
                )
            } else {
                listOf( Pair(1, "Wav File") )
            }
            this.dialog_popup_menu("Export Project to...", export_options, default = null) { index: Int, value: Int ->
                when (value) {
                    0 -> this.export_midi()
                    1 -> this.export_wav()
                }
            }
        }

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

        val drawer_layout = this.findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        drawer_layout.addDrawerListener(object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                this@MainActivity.playback_stop()
            }
        })
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

    fun play_event(channel: Int, event_value: Int, velocity: Int = 64) {
        val midi_channel = this._opus_manager.channels[channel].midi_channel

        val (note, bend) = if (this._opus_manager.is_percussion(channel)) { // Ignore the event data and use percussion map
            Pair(event_value + 27, 0)
        } else {
            val radix = this._opus_manager.radix
            val octave = (event_value + this._opus_manager.transpose) / radix
            val offset = (event_value + this._opus_manager.transpose) % radix

            val std_offset = (offset.toDouble() * 12.0 / radix.toDouble())
            val bend = ((std_offset - floor(std_offset)) * 512.0).toInt()
            val new_note = (octave * 12) + std_offset.toInt() + 21

            Pair( new_note, bend )
        }

        this._midi_feedback_dispatcher.play_note(midi_channel, note, bend)
    }

    fun import_project(path: String) {
        this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this._opus_manager.load(bytes, this.project_manager.get_new_path())
        }
    }

    fun import_midi(path: String) {
        this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()

            val midi = try {
                Midi.from_bytes(bytes)
            } catch (e: Exception) {
                throw InvalidMIDIFile(path)
            }

            val filename = this.parse_file_name(Uri.parse(path))
            val new_path = this.project_manager.get_new_path()

            this._opus_manager.import_midi(midi)
            this._opus_manager.path = new_path
            this._opus_manager.set_project_name(filename ?: getString(R.string.default_imported_midi_title))
            this._opus_manager.clear_history()
        }
        this.loading_reticle_hide()
    }

    fun has_projects_saved(): Boolean {
        return this.project_manager.has_projects_saved()
    }

    fun is_fluid_soundfont_available(): Boolean {
        val filename = "FluidR3_GM_GS.sf2"
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

        this.configuration.soundfont = filename
        val path = "${this.getExternalFilesDir(null)}/SoundFonts/$filename"
        this._soundfont = SoundFont(path)
        if (this._midi_feedback_device != null) {
            this._midi_interface.disconnect_virtual_output_device(this._midi_feedback_device!!)
        }
        this._midi_feedback_device = ActiveMidiAudioPlayer(SampleHandleManager(this._soundfont!!, 11025))
        this._midi_playback_device = PaganPlaybackDevice(this)

        this._midi_interface.connect_virtual_output_device(this._midi_feedback_device!!)

        this.update_channel_instruments()
        val rvActiveChannels: ChannelOptionRecycler = this.findViewById(R.id.rvActiveChannels)
        if (rvActiveChannels.adapter != null) {
            (rvActiveChannels.adapter as ChannelOptionAdapter).set_soundfont(this._soundfont!!)
        }
        if (this.get_opus_manager().channels.size > 0) {
            this.populate_active_percussion_names()
        }
    }

    fun get_soundfont(): SoundFont? {
        return this._soundfont
    }

    fun disable_soundfont() {
        val rvActiveChannels: ChannelOptionRecycler = this.findViewById(R.id.rvActiveChannels)
        if (rvActiveChannels.adapter != null) {
            (rvActiveChannels.adapter as ChannelOptionAdapter).unset_soundfont()
        }
        this.update_channel_instruments()
        this._soundfont = null
        this.configuration.soundfont = null
        this._midi_playback_device = null
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
        this.configuration.save(this._config_path)
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

        AlertDialog.Builder(main_fragment.context, R.style.AlertDialog)
            .setTitle(getString(R.string.dlg_change_name))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                opus_manager.set_project_name(input.text.toString())
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    internal fun <T> dialog_popup_menu(title: String, options: List<Pair<T, String>>, default: T? = null, callback: (index: Int, value: T) -> Unit ) {
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

        close_button.setOnClickListener {
            dialog.dismiss()
        }

        val adapter =
            PopupMenuRecyclerAdapter<T>(recycler, options, default) { index: Int, value: T ->
                dialog.dismiss()
                callback(index, value)
            }

        adapter.notifyDataSetChanged()

        val windowMetrics = this.windowManager.currentWindowMetrics
        val max_width: Int = (windowMetrics.bounds.width().toFloat() * .9).toInt()
        val max_height: Int = (windowMetrics.bounds.height().toFloat() * .8).toInt()

        dialog.window!!.setLayout(max_width, max_height)
    }

    internal fun dialog_number_input(title: String, min_value: Int, max_value: Int, default: Int? = null, callback: (value: Int) -> Unit ) {
        val coerced_default_value = default ?: (this._number_selector_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                window.decorView.rootView as ViewGroup,
                false
            )
        val number_input = viewInflated.findViewById<EditText>(R.id.etNumber)
        number_input.setText("$coerced_default_value")
        number_input.setOnClickListener {
            number_input.selectAll()
        }

        /*
            Use filters to ensure a number is input
            Then the listener to ensure the value is <= the maximum_value
            THEN on close, return the max(min_value, output)
         */
        number_input.filters = arrayOf(object: InputFilter {
            override fun filter(
                source: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                try {
                    "${dest.substring(0, dstart)}$source${dest.substring(dend)}".toInt()
                    return null
                } catch (nfe: NumberFormatException) {
                }
                return ""
            }
        })

        number_input.addTextChangedListener( object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (p0 == null || p0.toString() == "") {
                    number_input.setText("$min_value")
                    number_input.selectAll()
                } else {
                    val value = p0.toString().toInt()
                    if (value > max_value) {
                        number_input.setText("$max_value")
                        number_input.selectAll()
                    }
                }
            }
        })

        AlertDialog.Builder(this, R.style.AlertDialog)
            .setTitle(title)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = try {
                    max(min_value, number_input.text.toString().toInt())
                } catch (nfe: NumberFormatException) {
                    coerced_default_value
                }
                this._number_selector_defaults[title] = value
                callback(value)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .show()
        number_input.requestFocus()
        number_input.selectAll()
    }
    private fun dialog_save_project(callback: () -> Unit) {
        if (this.get_opus_manager().has_changed_since_save()) {
            AlertDialog.Builder(this, R.style.AlertDialog)
                .setTitle(getString(R.string.dialog_save_warning_title))
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
        } else {
            callback()
        }
    }
    private fun dialog_delete_project() {
        val main_fragment = this.get_active_fragment()

        val title = this.get_opus_manager().project_name

        AlertDialog.Builder(main_fragment!!.context, R.style.AlertDialog)
            .setTitle(resources.getString(R.string.dlg_delete_title, title))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                this@MainActivity.project_delete()
                dialog.dismiss()
                this@MainActivity.drawer_close()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
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

    fun export_wav() {
        val opus_manager = this.get_opus_manager()

        var name = opus_manager.path
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
            name = name.substring(0, name.lastIndexOf("."))
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = MimeTypes.AUDIO_WAV
        intent.putExtra(Intent.EXTRA_TITLE, "$name.wav")
        this._export_wav_intent_launcher.launch(intent)
    }

    fun export_wav_cancel() {
        if (this._exporting_wav_handle == null) {
            return
        }
        this._exporting_wav_handle!!.export_wav_cancel()
        this.feedback_msg("Export Cancelled")
    }

    fun export_midi() {
        val opus_manager = this.get_opus_manager()

        var name = opus_manager.path
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
            name = name.substring(0, name.lastIndexOf("."))
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = MimeTypes.AUDIO_MIDI
        intent.putExtra(Intent.EXTRA_TITLE, "$name.mid")

        this._export_midi_intent_launcher.launch(intent)
    }

    fun export_project() {
        var name = _opus_manager.path
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
            name = name.substring(0, name.lastIndexOf("."))
        }

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
        this._midi_playback_device = PaganPlaybackDevice(this)
    }

    fun validate_percussion_visibility() {
        val opus_manager = this.get_opus_manager()
        if (!this.configuration.show_percussion && opus_manager.has_percussion()) {
            this.configuration.show_percussion = true
        }
    }
}
