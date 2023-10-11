package com.qfs.pagan

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
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
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.MidiController
import com.qfs.apres.MidiPlayer
import com.qfs.apres.VirtualMidiDevice
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.SoundFontWavPlayer
import com.qfs.pagan.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread
import com.qfs.pagan.InterfaceLayer as OpusManager

/**
 * Device Scanning
 * Methods are called by the system whenever the set of attached devices changes.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var _app_bar_configuration: AppBarConfiguration
    lateinit var midi_interface: MidiController
    private lateinit var binding: ActivityMainBinding
    private lateinit var _config_path: String
    lateinit var configuration: PaganConfiguration
    private var _midi_playback_device: SoundFontWavPlayer? = null
    private var _soundfont: SoundFont? = null
    var active_percussion_names = HashMap<Int, String>()

    private var _opus_manager = OpusManager(this)

    private var _playback_handle: SoundFontWavPlayer.PlaybackInterface? = null
    private var _options_menu: Menu? = null
    internal lateinit var project_manager: ProjectManager
    private var _progress_bar: ProgressBar? = null

    private var _number_selector_defaults = HashMap<String, Int>()
    // flag to indicate that the landing page has been navigated away from for navigation management
    private var _has_seen_front_page = false

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

    var import_project_intent_launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data?.also { uri ->
                    val fragment = this.getActiveFragment()
                    fragment?.setFragmentResult(
                        IntentFragmentToken.ImportProject.name,
                        bundleOf(Pair("URI", uri.toString()))
                    )
                    if (fragment !is EditorFragment) {
                        this.navTo(R.id.EditorFragment)
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

    var import_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                val fragment = this.getActiveFragment()

                    fragment?.setFragmentResult(
                            IntentFragmentToken.ImportMidi.name,
                            bundleOf(Pair("URI", uri.toString()))
                            )
                    if (fragment !is EditorFragment) {
                        this.navTo(R.id.EditorFragment)
                    }
            }
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        this.recreate()
    }

    override fun onPause() {
        this.stop_playback()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val path = this.get_opus_manager().path
        this.get_opus_manager().save("${applicationInfo.dataDir}/.bkp.json")
        // saving changes the path, need to change it back
        this.get_opus_manager().path = path

        super.onSaveInstanceState(outState)
    }

    private var virtual_input_device = MidiPlayer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.midi_interface = object: MidiController(this) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                this@MainActivity.runOnUiThread {
                    this@MainActivity.update_menu_options()
                }
            }
            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                this@MainActivity.runOnUiThread {
                    this@MainActivity.stop_playback()
                    this@MainActivity.update_menu_options()
                }
            }
        }

        val midi_observer = object: VirtualMidiDevice() {
            override fun onSongPositionPointer(event: SongPositionPointer) {
                this@MainActivity.runOnUiThread {
                    this@MainActivity.get_opus_manager().cursor_select_column(event.beat, true)
                }
            }
        }

        this.midi_interface.connect_virtual_input_device(this.virtual_input_device)
        this.midi_interface.connect_virtual_output_device(midi_observer)

        this.project_manager = ProjectManager(this.getExternalFilesDir(null).toString())
        // Move files from applicationInfo.data to externalfilesdir (pre v1.1.2 location)
        val old_projects_dir = File("${applicationInfo.dataDir}/projects")
        if (old_projects_dir.isDirectory()) {
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
        //this.update_menu_options()

        //////////////////////////////////////////
        // TODO: clean up the file -> riff -> soundfont -> midi playback device process

        if (this.configuration.soundfont != null) {
            val path =
                "${this.getExternalFilesDir(null)}/SoundFonts/${this.configuration.soundfont}"
            val sf_file = File(path)
            if (sf_file.exists()) {
                this._soundfont = SoundFont(path)
                this._midi_playback_device = SoundFontWavPlayer(this._soundfont!!)
            }
            this.update_channel_instruments()
        }
        ///////////////////////////////////////////

        when (navController.currentDestination?.id) {
            R.id.EditorFragment -> {
                this.unlockDrawer()
            }

            else -> {
                this.lockDrawer()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.EditorFragment) {
                    this@MainActivity.save_dialog {
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
        return navController.navigateUp(_app_bar_configuration)
                || super.onSupportNavigateUp()
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
            this.stop_playback()
        }

        when (item.itemId) {
            R.id.itmNewProject -> {
                this.save_dialog {
                    val fragment = this.getActiveFragment()
                    fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
                    if (fragment !is EditorFragment) {
                        this.navTo(R.id.EditorFragment)
                    }
                }
            }

            R.id.itmLoadProject -> {
                this.save_dialog {
                    this.navTo(R.id.LoadFragment)
                }
            }

            R.id.itmImportMidi -> {
                this.save_dialog {
                    val intent = Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)
                    this.import_midi_intent_launcher.launch(intent)
                }
            }

            R.id.itmUndo -> {
                this.get_opus_manager().apply_undo()
            }

            R.id.itmPlay -> {
                if (this._playback_handle != null || this.virtual_input_device.playing) {
                   this.stop_playback()
                } else {
                    this.start_playback()
                }
            }

            R.id.itmImportProject -> {
                this.save_dialog {
                    val intent = Intent()
                        .setType("application/json")
                        .setAction(Intent.ACTION_GET_CONTENT)
                    this.import_project_intent_launcher.launch(intent)
                }
            }

            R.id.itmSettings -> {
                this.navTo(R.id.SettingsFragment)
            }
            R.id.itmMidiTest -> {
                for (device in this.midi_interface.poll_input_devices()) {
                    this.midi_interface.open_input_device(device)
                }
                for (device in this.midi_interface.poll_output_devices()) {
                    this.midi_interface.open_output_device(device)
                }
                AlertDialog.Builder(this, R.style.AlertDialog).apply {
                    setTitle("M COUNT: ${this@MainActivity.midi_interface.poll_input_devices().size}")
                    setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ -> }
                    show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////

    fun update_channel_instruments(channel: Int) {
        val opus_channel = this.get_opus_manager().channels[channel]
        this._midi_playback_device?.select_bank(opus_channel.midi_channel, opus_channel.midi_bank)
        this._midi_playback_device?.change_program(opus_channel.midi_channel, opus_channel.midi_program)
    }

    fun update_channel_instruments() {
        for (channel in _opus_manager.channels) {
            this._midi_playback_device?.select_bank(channel.midi_channel, channel.midi_bank)
            this._midi_playback_device?.change_program(channel.midi_channel, channel.midi_program)
        }
    }


    private fun save_dialog(callback: () -> Unit) {

        if (this.get_opus_manager().has_changed_since_save()) {
            AlertDialog.Builder(this, R.style.AlertDialog).apply {
                setTitle(getString(R.string.dialog_save_warning_title))
                setCancelable(true)
                setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ ->
                    this@MainActivity.save_current_project()
                    dialog.dismiss()
                    callback()
                }
                setNegativeButton(getString(R.string.dlg_decline)) { dialog, _ ->
                    dialog.dismiss()
                    callback()
                }
                show()
            }
        } else {
            callback()
        }
    }


    fun setup_config_drawer() {
        val opus_manager = this.get_opus_manager()
        val rvActiveChannels: ChannelOptionRecycler = this.findViewById(R.id.rvActiveChannels)
        ChannelOptionAdapter(opus_manager, rvActiveChannels)

        val tvChangeProjectName: TextView = this.findViewById(R.id.tvChangeProjectName)
        tvChangeProjectName.setOnClickListener {
            this.change_name_dialog()
        }

        val tvTempo: TextView = this.findViewById(R.id.tvTempo)
        tvTempo.text = this.getString(R.string.label_bpm, opus_manager.tempo.toInt())
        tvTempo.setOnClickListener {
            this.popup_number_dialog(
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
            get_number_string(opus_manager.transpose, opus_manager.RADIX, 1)
        )
        btnTranspose.setOnClickListener {
            this.popup_transpose_dialog()
        }

        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            opus_manager.new_channel()
        }

        this.findViewById<View>(R.id.btnExportProject).setOnClickListener {
            this.export_midi()
        }

        this.findViewById<View>(R.id.btnSaveProject).setOnClickListener {
            this.save_current_project()
        }
        this.findViewById<View>(R.id.btnSaveProject).setOnLongClickListener {
            this.export_current_project()
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
            this.delete_project_dialog()
        }
        btnCopyProject.setOnClickListener {
            this.copy_project()
            this.closeDrawer()
        }

        val drawer_layout = this.findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        drawer_layout.addDrawerListener(object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                this@MainActivity.stop_playback()
            }
        })
    }

    private fun change_name_dialog() {
        val main_fragment = this.getActiveFragment()

        val viewInflated: View = LayoutInflater.from(main_fragment!!.context)
            .inflate(
                R.layout.text_name_change,
                main_fragment.view as ViewGroup,
                false
            )

        val input: EditText = viewInflated.findViewById(R.id.etProjectName)
        input.setText(this.get_opus_manager().project_name)

        val opus_manager = this.get_opus_manager()
        AlertDialog.Builder(main_fragment.context, R.style.AlertDialog).apply {
            setTitle(getString(R.string.dlg_change_name))
            setView(viewInflated)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                opus_manager.set_project_name(input.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    private fun scroll_to_beat(beat: Int) {
        val fragment = this.getActiveFragment()
        if (fragment is EditorFragment) {
            fragment.scroll_to_beat(beat)
        }
    }

    private fun save_current_project() {
        this.project_manager.save(this._opus_manager)
        this.feedback_msg(getString(R.string.feedback_project_saved))
        this.update_menu_options()
    }

    private fun export_current_project() {
        var name = _opus_manager.path
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
            name = name.substring(0, name.lastIndexOf("."))
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "$name.json")
        }

        this._export_project_intent_launcher.launch(intent)

    }

    fun get_opus_manager(): OpusManager {
        return this._opus_manager
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
                this._options_menu!!.findItem(R.id.itmPlay).isVisible = (this._soundfont != null || this.midi_interface.output_devices_connected())
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

    fun update_title_text() {
        this.set_title_text(this.get_opus_manager().project_name)
    }

    fun set_title_text(new_text: String) {
        this.binding.appBarMain.toolbar.title = new_text
    }

    fun play_event(channel: Int, event_value: Int, velocity: Int = 64) {
        val midi_channel = this._opus_manager.channels[channel].midi_channel
        val note = if (this._opus_manager.is_percussion(channel)) {
            event_value + 27
        } else {
            event_value + 21 + this._opus_manager.transpose
        }

        if (this.midi_interface.output_devices_connected()) {
            thread {
                this.midi_interface.broadcast_event(NoteOn(channel, note, velocity))
                Thread.sleep(300)
                this.midi_interface.broadcast_event(NoteOff(channel, note, velocity))
            }
        } else {
            this@MainActivity.runOnUiThread {
                this._midi_playback_device?.play_note(midi_channel, note, velocity, 200)
            }
        }
    }

    private fun play_midi( midi: Midi, callback: (position: Float) -> Unit): SoundFontWavPlayer.PlaybackInterface? {
        return if (this.midi_interface.output_devices_connected()) {
            try {
                this.virtual_input_device.play_midi(midi) {
                    this.runOnUiThread {
                        this.stop_playback()
                    }
                }
            } catch (e: java.io.IOException) {
                this.runOnUiThread {
                    this.stop_playback()
                }
            }
            null
        } else {
            this._midi_playback_device?.play(midi, callback)
        }
    }

    private fun export_midi() {
        val opus_manager = this.get_opus_manager()

        var name = opus_manager.path
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
            name = name.substring(0, name.lastIndexOf("."))
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/midi"
            putExtra(Intent.EXTRA_TITLE, "$name.mid")
        }

        this._export_midi_intent_launcher.launch(intent)
    }

    private fun delete_project_dialog() {
        val main_fragment = this.getActiveFragment()

        val title = this.get_opus_manager().project_name

        AlertDialog.Builder(main_fragment!!.context, R.style.AlertDialog).apply {
            setTitle(resources.getString(R.string.dlg_delete_title, title))

            setPositiveButton(android.R.string.ok) { dialog, _ ->
                this@MainActivity.delete_project()
                dialog.dismiss()
                this@MainActivity.closeDrawer()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    private fun delete_project() {
        this.project_manager.delete(this._opus_manager)

        val fragment = this.getActiveFragment()
        fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
        this.navTo(R.id.EditorFragment)


        this.feedback_msg(resources.getString(R.string.feedback_delete, this.title))
    }

    private fun copy_project() {
        this.project_manager.copy(this._opus_manager)
        this.feedback_msg(getString(R.string.feedback_on_copy))
    }

    private fun closeDrawer() {
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()
    }

    fun navTo(fragment: Int) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (fragment == R.id.EditorFragment) {
            this._has_seen_front_page = true
            this.unlockDrawer()
        } else {
            this.lockDrawer()
        }
        navController.navigate(fragment)
    }

    fun getActiveFragment(): Fragment? {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        return navHost?.childFragmentManager?.fragments?.get(0)
    }

    fun feedback_msg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // Can't use the general popup_number_dialog. We want a picker using radix-N
    private fun popup_transpose_dialog() {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                window.decorView.rootView as ViewGroup,
                false
            )

        val npOnes = viewInflated.findViewById<NumberPicker>(R.id.npOnes)
        npOnes.minValue = 0
        npOnes.maxValue = 11
        npOnes.displayedValues = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B")
        npOnes.value = this.get_opus_manager().transpose

        val npTens = viewInflated.findViewById<NumberPicker>(R.id.npTens)
        npTens.visibility = View.GONE
        val npHundreds = viewInflated.findViewById<NumberPicker>(R.id.npHundreds)
        npHundreds.visibility = View.GONE

        AlertDialog.Builder(this, R.style.AlertDialog)
            .setTitle(getString(R.string.dlg_transpose))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = npOnes.value
                this._opus_manager.set_transpose(value)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    internal fun <T> popup_menu_dialog(
        title: String,
        options: List<Pair<T, String>>,
        default: T? = null,
        callback: (index: Int, value: T) -> Unit
    ) {
        if (options.isEmpty()) {
            return
        }
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_menu,
                window.decorView.rootView as ViewGroup,
                false
            )

        val recycler = viewInflated as RecyclerView
        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setTitle(title)
            .setView(viewInflated)
            .show()

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

    internal fun popup_number_dialog(
        title: String,
        min_value: Int,
        max_value: Int,
        default: Int? = null,
        callback: (value: Int) -> Unit
    ) {
        val coerced_default_value = default ?: (this._number_selector_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                window.decorView.rootView as ViewGroup,
                false
            )

        val ones_min = if (min_value > 9 || max_value > 9) {
            0
        } else {
            min_value
        }

        val ones_max = if (max_value > 9) {
            9
        } else {
            max_value % 10
        }

        val npOnes = viewInflated.findViewById<NumberPicker>(R.id.npOnes)
        npOnes.minValue = ones_min
        npOnes.maxValue = ones_max

        val tens_min = if (min_value / 10 > 9 || max_value / 10 > 9) {
            0
        } else {
            (min_value / 10) % 10
        }

        val tens_max = if (max_value / 10 > 9) {
            9
        } else {
            (max_value / 10)
        }

        val npTens = viewInflated.findViewById<NumberPicker>(R.id.npTens)
        npTens.minValue = tens_min
        npTens.maxValue = tens_max

        val hundreds_min = if (min_value / 100 > 9 || max_value / 100 > 9) {
            0
        } else {
            (min_value / 100) % 10
        }

        val hundreds_max = if (max_value / 100 > 9) {
            9
        } else {
            (max_value / 100)
        }

        val npHundreds = viewInflated.findViewById<NumberPicker>(R.id.npHundreds)
        npHundreds.maxValue = hundreds_max
        npHundreds.minValue = hundreds_min

        npHundreds.value = (coerced_default_value / 100) % 10
        npTens.value = (coerced_default_value / 10) % 10
        npOnes.value = coerced_default_value % 10

        if (hundreds_max == 0) {
            npHundreds.visibility = View.GONE
            if (tens_max == 0) {
                npTens.visibility = View.GONE
            }
        }

        AlertDialog.Builder(this, R.style.AlertDialog)
            .setTitle(title)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = (npHundreds.value * 100) + (npTens.value * 10) + npOnes.value
                this._number_selector_defaults[title] = value
                callback(value)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .show()
    }

    fun loading_reticle() {
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

    fun cancel_reticle() {
        this.runOnUiThread {
            val progressBar = this._progress_bar ?: return@runOnUiThread
            if (progressBar.parent != null) {
                (progressBar.parent as ViewGroup).removeView(progressBar)
            }
        }
    }

    fun lockDrawer() {
        this.binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun unlockDrawer() {
        try {
            this.binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } catch (e: UninitializedPropertyAccessException) {
            // pass, if it's not initialized, it's not locked
        }
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
        this.cancel_reticle()
    }

    fun get_timestring_at_beat(beat: Int): String {
        val opus_manager = this.get_opus_manager()
        val tempo = opus_manager.tempo
        val milliseconds_per_beat = 60000F / tempo
        var milliseconds = (beat * milliseconds_per_beat).toInt()
        var seconds = milliseconds / 1000
        val minutes = seconds / 60
        seconds %= 60
        milliseconds %= 1000
        val centiseconds = milliseconds / 10
        val minute_string = minutes.toString().padStart(2, '0')
        val second_string = seconds.toString().padStart(2, '0')
        val centi_string = centiseconds.toString().padStart(2, '0')
        return "$minute_string:$second_string.$centi_string"
    }

    fun start_playback() {
        if (this._playback_handle != null) {
            this.stop_playback()
        }

        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay)
        if (blocker_view != null) {
            blocker_view.visibility = View.VISIBLE
            blocker_view.setOnClickListener {
                this.stop_playback()
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
        val x = when (cursor.mode) {
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

        val beat_count = this.get_opus_manager().opus_beat_count.toFloat()
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        thread {
            this._playback_handle = this.play_midi(_opus_manager.get_midi(x)) {
                if (it == 1F) { // Song is over, return to start state
                    this.runOnUiThread {
                        this.get_opus_manager().cursor_select_column(0, true)
                        this.stop_playback()
                    }
                } else {
                    val position = ((beat_count - x) * it).toInt() + x
                    if (this.get_opus_manager().cursor.beat != position) {
                        this.runOnUiThread {
                            this.get_opus_manager().cursor_select_column(position, true)
                        }
                    }
                }
            }
        }
    }

    fun stop_playback() {
        this.runOnUiThread {
            val play_pause_button = this._options_menu?.findItem(R.id.itmPlay) ?: return@runOnUiThread
            play_pause_button.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_play_arrow_24)
        }
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (this.virtual_input_device.playing) {
            this.virtual_input_device.stop()
        }

        if (this._playback_handle != null) {
            this._playback_handle!!.stop()
            this._playback_handle = null
        }

        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay) ?: return
        blocker_view.visibility = View.GONE
    }

    fun has_projects_saved(): Boolean {
        return this.project_manager.has_projects_saved()
    }

    fun has_fluid_soundfont(): Boolean {
        val filename = "FluidR3_GM_GS.sf2"
        val soundfont_dir = this.get_soundfont_directory()
        val fluid_file = File("${soundfont_dir.path}/$filename")
        return fluid_file.exists()
    }

    fun has_soundfont(): Boolean {
        val soundfont_dir = this.get_soundfont_directory()
        return soundfont_dir.listFiles()?.isNotEmpty() ?: false
    }

    fun get_drum_options(): List<Pair<String, Int>> {
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

    fun set_soundfont(filename: String?) {
        if (filename == null) {
            this.disable_soundfont()
            return
        }

        this.configuration.soundfont = filename
        val path = "${this.getExternalFilesDir(null)}/SoundFonts/$filename"
        this._soundfont = SoundFont(path)
        this._midi_playback_device = SoundFontWavPlayer(this._soundfont!!)
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

    fun convert_opus_to_absolute() {
        this.get_opus_manager().convert_all_events_to_absolute()
    }
}
