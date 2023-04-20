package com.qfs.radixulous

//import com.qfs.radixulous.MIDIPlaybackDevice

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.qfs.radixulous.apres.*
import com.qfs.radixulous.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var midi_controller: MIDIController
    lateinit var midi_playback_device: MIDIPlaybackDevice
    private var midi_input_device = VirtualMIDIDevice()
    private var midi_player = MIDIPlayer()
    private var midi_scroller = MIDIScroller(this)
    lateinit var soundfont: SoundFont

    private var current_project_title: String? = null
    private var opus_manager = OpusManager()

    private var in_play_back: Boolean = false

    private lateinit var optionsMenu: Menu
    internal lateinit var project_manager: ProjectManager
    private var progressBar: ProgressBar? = null

    class MIDIScroller(private var activity: MainActivity): VirtualMIDIDevice() {
        override fun onSongPositionPointer(event: SongPositionPointer) {
            this.activity.scroll_to_beat(event.beat, true)
        }
    }

    private var export_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val opus_manager = this.get_opus_manager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                    this.feedback_msg("Exported to midi")
                }
            }
        }
    }

    var import_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                // TODO: NAV TO MAIN FRAGMENT AND USE RESULT LISTENER
                val fragment = this.getActiveFragment()
                fragment?.setFragmentResult("IMPORT", bundleOf(Pair("URI", uri.toString())))
                this.navTo("main")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.project_manager = ProjectManager(applicationInfo.dataDir)
        this.binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(this.binding.root)
        setSupportActionBar(this.binding.appBarMain.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        this.appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, this.appBarConfiguration)
        //val drawerlayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        //drawerlayout.addDrawerListener( object: DrawerLayout.DrawerListener {
        //    override fun onDrawerClosed(drawerView: View) { }
        //    override fun onDrawerSlide(drawerView: View, slideOffset: Float) { }
        //    override fun onDrawerOpened(drawerView: View) { }
        //    override fun onDrawerStateChanged(newState: Int) { }
        //})

        this.lockDrawer()
        //////////////////////////////////////////
        // TODO: clean up the file -> riff -> soundfont -> midi playback device process
        this.soundfont = SoundFont(assets.open("FluidR3_GM.sf2"))
        this.midi_playback_device = MIDIPlaybackDevice(this, this.soundfont)

        this.midi_controller = MIDIController(window.decorView.rootView.context)
        this.midi_controller.registerVirtualDevice(this.midi_playback_device)
        this.midi_controller.registerVirtualDevice(this.midi_input_device)
        this.midi_controller.registerVirtualDevice(this.midi_player)
        this.midi_controller.registerVirtualDevice(this.midi_scroller)
        ///////////////////////////////////////////
    }

    fun update_channel_instruments(opus_manager: OpusManager) {
        for (channel in opus_manager.channels) {
            this.midi_input_device.sendEvent(ProgramChange(channel.midi_channel, channel.midi_instrument))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        this.optionsMenu = menu
        val output = super.onCreateOptionsMenu(menu)
        this.update_menu_options()
        return output
    }

    fun save_dialog(callback: () -> Unit) {
        var that = this
        AlertDialog.Builder(this, R.style.AlertDialog).apply {
            setTitle("Save Current Project First?")
            setCancelable(true)
            setNeutralButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            setPositiveButton("Yes") { dialog, _ ->
                that.save_current_project()
                dialog.dismiss()
                callback()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                callback()
            }
            show()
        }
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itmNewProject -> {
                // TODO: Save or discard popup dialog
                this.save_dialog {
                    val fragment = this.getActiveFragment()
                    fragment?.setFragmentResult("NEW", bundleOf())
                    this.navTo("main")
                }
            }
            R.id.itmLoadProject -> {
                this.save_dialog {
                    this.navTo("load")
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
                val main_fragment = this.getActiveFragment()
                if (main_fragment is MainFragment) {
                    main_fragment.undo()
                }
            }
            R.id.itmPlay -> {
                if (!this.in_play_back) {
                    this.playback()
                } else {
                    this.stop_playback()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun stop_playback() {
        this.midi_input_device.sendEvent(MIDIStop())
    }

    fun setup_config_drawer() {
        val opus_manager = this.get_opus_manager()
        val rvActiveChannels: RecyclerView = this.findViewById(R.id.rvActiveChannels)
        val channelAdapter = ChannelOptionAdapter(this, rvActiveChannels, this.soundfont)

        val tvChangeProjectName: TextView = this.findViewById(R.id.tvChangeProjectName)
        tvChangeProjectName.setOnClickListener {
            this.change_name_dialog()
        }

        val tvTempo: TextView = this.findViewById(R.id.tvTempo)
        tvTempo.text = "${opus_manager.tempo.toInt()} BPM"
        tvTempo.setOnClickListener {
            this.popup_number_dialog("Set Tempo (BPM)", 1, 999, this::set_tempo, opus_manager.tempo.toInt())
        }

        val btnTranspose: TextView = this.findViewById(R.id.btnTranspose)
        btnTranspose.text = "T: ${opus_manager.transpose}"
        btnTranspose.setOnClickListener {
            this.popup_transpose_dialog()
        }

        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            channelAdapter.addChannel()
        }

        this.findViewById<View>(R.id.btnExportProject).setOnClickListener {
            this.export_midi()
        }

        this.findViewById<View>(R.id.btnSaveProject).setOnClickListener {
            this.save_current_project()
        }

        val btnDeleteProject = this.findViewById<View>(R.id.btnDeleteProject)
        val btnCopyProject = this.findViewById<View>(R.id.btnCopyProject)
        if (opus_manager.path != null && File(opus_manager.path!!).isFile) {
            btnDeleteProject.setOnClickListener {
                this.delete_project_dialog()
            }
            btnCopyProject.setOnClickListener {
                this.copy_project()
                this.closeDrawer()
            }
            btnDeleteProject.visibility = View.VISIBLE
            btnCopyProject.visibility = View.VISIBLE
        } else {
            btnDeleteProject.visibility = View.GONE
            btnCopyProject.visibility = View.GONE
        }

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
        input.setText(this.get_current_project_title() ?: "Untitled Project")

        val that = this
        AlertDialog.Builder(main_fragment.context, R.style.AlertDialog).apply {
            setTitle("Change Project Name")
            setView(viewInflated)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                that.set_current_project_title(input.text.toString())
                if (that.opus_manager.path != null && File(that.opus_manager.path!!).isFile) {
                    that.project_manager.set_title(input.text.toString(), that.opus_manager)
                }
                dialog.dismiss()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    private fun playback() {
        if (this.in_play_back) {
            return
        }

        this.in_play_back = true
        val item = this.optionsMenu.findItem(R.id.itmPlay)
        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_pause_24)
        val opus_manager = this.get_opus_manager()
        val x = opus_manager.get_cursor().x

        thread {
            this.play_midi(opus_manager.get_midi(x))

            this.in_play_back = false

            this@MainActivity.runOnUiThread {
                item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_play_arrow_24)
            }
        }
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        this@MainActivity.runOnUiThread {
            val fragment = this.getActiveFragment()
            if (fragment is MainFragment) {
                fragment.scroll_to_beat(beat, select)
            }
        }
    }

    fun save_current_project() {
        this.project_manager.save(this.current_project_title!!, this.opus_manager)
        this.feedback_msg("Project Saved")
    }

    fun set_current_project_title(title: String) {
        this.current_project_title = title
        this.update_title_text()
    }

    private fun get_current_project_title(): String? {
        return this.current_project_title
    }

    fun get_opus_manager(): OpusManager {
        return this.opus_manager
    }

    fun update_menu_options() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        when (navHost?.childFragmentManager?.fragments?.get(0)) {
            is MainFragment -> {
                this.optionsMenu.findItem(R.id.itmLoadProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmUndo).isVisible = true
                this.optionsMenu.findItem(R.id.itmNewProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmPlay).isVisible = true
                this.optionsMenu.findItem(R.id.itmImportMidi).isVisible = true
            }
            else -> {
                this.optionsMenu.findItem(R.id.itmLoadProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmUndo).isVisible = false
                this.optionsMenu.findItem(R.id.itmNewProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmPlay).isVisible = false
                this.optionsMenu.findItem(R.id.itmImportMidi).isVisible = false
            }
        }
    }

    // Only called from MainFragment
    fun newProject() {
        this.stop_playback()

        this.opus_manager.new()
        val new_path = this.project_manager.get_new_path()
        this.set_current_project_title("New Opus")
        this.opus_manager.path = new_path
        this.setup_config_drawer()
    }

    fun update_title_text() {
        this.set_title_text(this.get_current_project_title() ?: "Untitled Opus")
    }

    fun set_title_text(new_text: String) {
        this.binding.appBarMain.toolbar.title = new_text
    }

    fun play_event(channel: Int, event_value: Int) {
        val midi_channel = this.opus_manager.channels[channel].midi_channel
        val note = if (this.opus_manager.is_percussion(channel)) {
            event_value + 27
        } else {
            event_value + 21
        }

        this@MainActivity.runOnUiThread {
            this.midi_input_device.sendEvent(NoteOn(midi_channel, note, 64))
            Thread.sleep(250)
            this.midi_input_device.sendEvent(NoteOff(midi_channel, note, 64))
        }
    }


    private fun play_midi(midi: MIDI) {
        this.midi_player.play_midi(midi)
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
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
        }

        this.export_midi_intent_launcher.launch(intent)
    }

    private fun delete_project_dialog() {
        val main_fragment = this.getActiveFragment()

        val title = this.get_current_project_title() ?: "Untitled Project"

        val that = this
        AlertDialog.Builder(main_fragment!!.context, R.style.AlertDialog).apply {
            setTitle("Really delete $title?")

            setPositiveButton(android.R.string.ok) { dialog, _ ->
                that.delete_project()
                dialog.dismiss()
                that.closeDrawer()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    private fun delete_project() {
        this.project_manager.delete(this.opus_manager)

        val fragment = this.getActiveFragment()
        fragment?.setFragmentResult("NEW", bundleOf())
        this.navTo("main")

        this.feedback_msg("Deleted \"$title\"")
    }

    private fun copy_project() {
        this.current_project_title = this.project_manager.copy(this.opus_manager)
        this.feedback_msg("Now working on copy")
        this.update_title_text()
    }

    private fun closeDrawer() {
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()
    }

    fun navTo(fragmentName: String) {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val fragment = navHost?.childFragmentManager?.fragments?.get(0)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        when (fragment) {
            is LoadFragment -> {
                when (fragmentName) {
                    "main" -> {
                        if (navController.graph.startDestinationId != R.id.MainFragment) {
                            this.reset_start_destination()
                        } else {
                            navController.navigate(R.id.action_LoadFragment_to_MainFragment)
                        }
                    }
                    else -> {}
                }

            }
            is MainFragment -> {
                this.in_play_back = false
                when (fragmentName) {
                    "load" -> {
                        navController.navigate(R.id.action_MainFragment_to_LoadFragment)
                    }
                    else -> {}
                }
            }
            is LandingPageFragment -> {
                when (fragmentName) {
                    "main" -> {
                        this.reset_start_destination()
                    }
                    "load" -> {
                        navController.navigate(R.id.action_FrontFragment_to_LoadFragment)
                    }
                    else -> {}
                }
            }
            else -> {}
        }
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
            .setTitle("Transpose")
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = npOnes.value
                this.set_transpose(value)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .show()
    }

    internal fun popup_number_dialog(title: String, min_value: Int, max_value: Int, callback: (value: Int) -> Unit, default: Int = min_value) {
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

        npHundreds.value = (default / 100) % 10
        npTens.value = (default / 10) % 10
        npOnes.value = default % 10

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
                callback(value)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .show()
    }

    private fun set_tempo(value: Int) {
        val opus_manager = this.get_opus_manager()

        opus_manager.tempo = value.toFloat()
        val tvTempo: TextView = this.findViewById(R.id.tvTempo)
        tvTempo.text = "${opus_manager.tempo.toInt()} BPM"
    }

    private fun set_transpose(value: Int) {
        val opus_manager = this.get_opus_manager()

        opus_manager.transpose = value
        val btnTranspose: TextView = this.findViewById(R.id.btnTranspose)
        btnTranspose.text = "T: ${get_number_string(value, opus_manager.RADIX, 2)}"
    }

    fun loading_reticle() {
        if (this.progressBar == null) {
            this.progressBar =
                ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleLarge)
        }
        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(50, 50)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        this.binding.root.addView(this.progressBar, params)
    }

    fun cancel_reticle() {
        val progressBar = this.progressBar ?: return
        (progressBar.parent as ViewGroup).removeView(progressBar)
    }

    fun lockDrawer() {
        this.binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun unlockDrawer() {
        this.binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    // Change the navcontroller graph so the user can't return the the main menu screen
    private fun reset_start_destination() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (navController.graph.startDestinationId != R.id.MainFragment) {
            val new_graph = navController.navInflater.inflate(R.navigation.nav_graph_b)
            navController.graph = new_graph

            this.appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, this.appBarConfiguration)
        }
    }

    fun import_midi(path: String) {
        this.stop_playback()
        this.applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            val midi = MIDI.from_bytes(bytes)
            var filename = java.net.URLDecoder.decode(path, "utf-8")
            filename = filename.substring(filename.lastIndexOf("/") + 1)
            filename = filename.substring(0, filename.lastIndexOf("."))

            this.opus_manager.import_midi(midi)
            val new_path = this.project_manager.get_new_path()
            this.opus_manager.path = new_path

            this.set_current_project_title(filename)
            this.update_menu_options()
            this.setup_config_drawer()

        }
        this.cancel_reticle()
    }
}
