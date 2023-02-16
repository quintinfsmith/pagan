package com.qfs.radixulous

//import com.qfs.radixulous.MIDIPlaybackDevice

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.apres.*
import com.qfs.radixulous.databinding.ActivityMainBinding
import com.qfs.radixulous.opusmanager.BeatKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager


enum class ContextMenu {
    Leaf,
    Line,
    Beat,
    Linking,
    None
}

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var midi_controller: MIDIController
    lateinit var midi_playback_device: MIDIPlaybackDevice
    var midi_input_device = MIDIInputDevice()
    private var midi_player = MIDIPlayer()

    private var current_project_title: String? = null
    private var opus_manager = OpusManager()
    private var project_manager = ProjectManager("/data/data/com.qfs.radixulous/projects")

    private var in_play_back: Boolean = false
    private var ticking: Boolean = false

    private lateinit var optionsMenu: Menu
    // TODO: Convert focus booleans to 1 enum SINGLE, ROW, COLUMN
    var focus_row: Boolean = false
    var focus_column: Boolean = false

    var export_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val opus_manager = this.getOpusManager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                    this.feedback_msg("Exported to midi")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(this.binding.root)

        setSupportActionBar(this.binding.appBarMain.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        this.appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, this.appBarConfiguration)


        //////////////////////////////////////////
        // TODO: clean up the file -> riff -> soundfont -> midi playback device process
        val soundfont = SoundFont(Riff(assets.open("freepats-general-midi.sf2")))
        this.midi_playback_device = MIDIPlaybackDevice(this, soundfont)

        this.midi_controller = RadMidiController(window.decorView.rootView.context)
        this.midi_controller.registerVirtualDevice(this.midi_playback_device)
        this.midi_controller.registerVirtualDevice(this.midi_input_device)
        this.midi_controller.registerVirtualDevice(this.midi_player)
        ///////////////////////////////////////////
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
    // method to inflate the options menu when
    // the user opens the menu for the first time
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // TODO: decide which menu based on active fragment?
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        this.optionsMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    // methods to control the operations that will
    // happen when user clicks on the action buttons
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itmNewProject -> {
                // TODO: Save or discard popup dialog
                val main_fragment = this.getActiveFragment()
                if (main_fragment is MainFragment) {
                    main_fragment.takedownCurrent()
                    this.newProject()
                    this.update_title_text()
                    main_fragment.setContextMenu(ContextMenu.Leaf)
                    this.tick()
                }
            }
            R.id.itmLoadProject -> {
                this.navTo("load")
            }
            R.id.itmSaveProject -> {
                this.save_current_project()
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
                    this.in_play_back = false
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setup_config_drawer() {
        val opus_manager = this.getOpusManager()
        val rvActiveChannels: RecyclerView = this.findViewById(R.id.rvActiveChannels)
        val channelAdapter = ChannelOptionAdapter(this, rvActiveChannels)

        val tvChangeProjectName: TextView = this.findViewById(R.id.tvChangeProjectName)
        tvChangeProjectName.setOnClickListener {
            this.change_name_dialog()
        }

        val etTempo: EditText = this.findViewById(R.id.etTempo)
        etTempo.setText(opus_manager.tempo.toString())
        etTempo.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(editable: Editable?) {
                try {
                    opus_manager.tempo = editable.toString().toFloat()
                } catch (exception: Exception) { }
            }
        })
        etTempo.filters = arrayOf(RangeFilter(1F, 999F))


        (this.findViewById(R.id.btnAddChannel) as TextView).setOnClickListener {
            channelAdapter.addChannel()
            this.tick()
        }

        (this.findViewById(R.id.btnExportProject) as TextView).setOnClickListener {
            this.export_midi()
        }

        var btnDeleteProject: TextView = this.findViewById(R.id.btnDeleteProject)
        var btnCopyProject: TextView = this.findViewById(R.id.btnCopyProject)
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
        AlertDialog.Builder(main_fragment!!.context).apply {
            setTitle("Change Project Name")
            setView(viewInflated)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                that.set_current_project_title(input.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    fun playback() {
        if (this.in_play_back) {
            return
        }
        this.in_play_back = true
        val main_fragment = this.getActiveFragment()
        val item = this.optionsMenu.findItem(R.id.itmPlay)
        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_pause_24)
        thread {
            val opus_manager = this.getOpusManager()
            val beat = opus_manager.get_cursor().x
            for (i in beat until opus_manager.opus_beat_count) {
                if (!this.in_play_back) {
                    break
                }

                if (main_fragment is MainFragment) {
                    this@MainActivity.runOnUiThread {
                        main_fragment.scroll_to_beat(i, true)
                    }
                }

                thread {
                    this.play_beat(i)
                }

                val delay = (60F / opus_manager.tempo) * 1000
                Thread.sleep(delay.toLong())
            }
            this.in_play_back = false
            this@MainActivity.runOnUiThread {
                val item = this.optionsMenu.findItem(R.id.itmPlay)
                item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_play_arrow_24)
            }

        }
    }

    private fun play_beat(beat: Int) {
        val opus_manager = this.getOpusManager()
        val midi = opus_manager.get_midi(beat, beat + 1)
        this.play_midi(midi)
    }


    private fun save_current_project() {
        this.project_manager.save(this.current_project_title!!, this.opus_manager)
        this.feedback_msg("Project Saved")
    }

    fun set_current_project_title(title: String) {
        this.current_project_title = title
        if (this.opus_manager.path != null && File(this.opus_manager.path!!).isFile) {
            this.project_manager.save(title, this.opus_manager)
        }
        this.update_title_text()
    }
    fun get_current_project_title(): String? {
        return this.current_project_title
    }

    fun getOpusManager(): OpusManager {
        return this.opus_manager
    }

    fun update_menu_options() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        when (navHost?.childFragmentManager?.fragments?.get(0)) {
            is MainFragment -> {
                this.optionsMenu.findItem(R.id.itmLoadProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmSaveProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmUndo).isVisible = true
                this.optionsMenu.findItem(R.id.itmNewProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmPlay).isVisible = true
            }
            //is LoadFragment -> { }
            else -> {
                this.optionsMenu.findItem(R.id.itmLoadProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmSaveProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmUndo).isVisible = false
                this.optionsMenu.findItem(R.id.itmNewProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmPlay).isVisible = false
            }
        }
    }

    fun newProject() {
        this.opus_manager.new()
        var new_path = this.project_manager.get_new_path()
        this.set_current_project_title("New Opus")
        this.opus_manager.path = new_path
    }

    fun update_title_text() {
        this.set_title_text(this.get_current_project_title() ?: "Untitled Opus")
    }

    fun set_title_text(new_text: String) {
        this.binding.appBarMain.toolbar!!.title = new_text
    }

    fun play_event(channel: Int, event_value: Int) {
        val midi_channel = this.opus_manager.channels[channel].midi_channel
        val note = if (this.opus_manager.is_percussion(channel)) {
            event_value + 35
        } else {
            event_value + 21
        }
        this.midi_input_device.sendEvent(NoteOn(midi_channel, note, 64))
        thread {
            Thread.sleep(200)
            this.midi_input_device.sendEvent(NoteOff(midi_channel, note, 64))
        }
    }


    fun play_midi(midi: MIDI) {
        this.midi_player.play_midi(midi)
    }

    fun export_midi() {
        val opus_manager = this.getOpusManager()

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

        var title = this.get_current_project_title() ?: "Untitled Project"

        val that = this
        AlertDialog.Builder(main_fragment!!.context).apply {
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

    fun delete_project() {
        var title = this.get_current_project_title() ?: "Untitled Project"
        this.project_manager.delete(this.opus_manager)
        var main_fragment = this.getActiveFragment()

        if (main_fragment is MainFragment) {
            main_fragment.takedownCurrent()
            this.newProject()
            this.update_title_text()
            main_fragment.setContextMenu(ContextMenu.Leaf)
            this.tick()
        }

        this.feedback_msg("Deleted \"$title\"")
    }

    fun copy_project() {
        this.current_project_title = this.project_manager.copy(this.opus_manager)
        this.feedback_msg("Now working on copy")
        this.update_title_text()
    }

    fun closeDrawer() {
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
                        navController.navigate(R.id.action_LoadFragment_to_MainFragment)
                    }
                    else -> {}
                }

            }
            is MainFragment -> {
                when (fragmentName) {
                    "load" -> {
                        navController.navigate(R.id.action_MainFragment_to_LoadFragment)
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

    fun tick() {
        if (!this.ticking) {
            this.ticking = true
            this.tick_unapply_focus()
            this.tick_manage_lines()
            this.tick_manage_beats() // new/pop
            this.tick_update_beats() // changes
            this.tick_validate_leafs()
            this.tick_apply_focus()
            this.ticking = false
        }
    }

    private fun tick_validate_leafs() {
        val opus_manager = this.getOpusManager()
        val main_fragment = this.getActiveFragment()
        while (true) {
            val (beatkey, position) = opus_manager.fetch_flag_absolute_value() ?: break
            if (main_fragment !is MainFragment) {
                continue
            }
            var y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)
            var abs_value = opus_manager.get_absolute_value(beatkey, position) ?: continue
            main_fragment.validate_leaf(y, beatkey.beat, position, abs_value in 0..95)
        }
    }

    private fun tick_manage_lines() {
        val opus_manager = this.getOpusManager()
        val main_fragment = this.getActiveFragment()
        while (true) {
            val (channel, index, operation) = opus_manager.fetch_flag_line() ?: break
            if (main_fragment !is MainFragment) {
                continue
            }

            when (operation) {
                0 -> {
                    val counts = opus_manager.get_channel_line_counts()
                    var y = 0
                    for (i in 0 until channel) {
                        y += counts[i]
                    }
                    main_fragment.line_remove(y + index)
                }
                1 -> {
                    val y = opus_manager.get_y(channel, index)
                    main_fragment.line_new(y, opus_manager.opus_beat_count)
                }
                2 -> {
                    val y = opus_manager.get_y(channel, index)
                    main_fragment.line_new(y, 0)
                    //for (i in 0 until opus_manager.opus_beat_count) {
                    //    opus_manager.flag_beat_change(BeatKey(channel, index, i))
                    //}
                }
            }
        }

        if (main_fragment is MainFragment) {
            main_fragment.line_update_labels(opus_manager)
        }
    }

    private fun tick_manage_beats() {
        val opus_manager = this.getOpusManager()
        val updated_beats: MutableSet<Int> = mutableSetOf()
        var min_changed = opus_manager.opus_beat_count
        val main_fragment = this.getActiveFragment()

        while (true) {
            val (index, operation) = opus_manager.fetch_flag_beat() ?: break
            min_changed = Integer.min(min_changed, index)

            if (main_fragment !is MainFragment) {
                continue
            }
            when (operation) {
                1 -> {
                    main_fragment.beat_new(index)
                    updated_beats.add(index)
                }
                0 -> {
                    main_fragment.beat_remove(index)
                }
            }
        }

        if (main_fragment is MainFragment) {
            main_fragment.tick_resize_beats(updated_beats.toList())
            for (index in min_changed until opus_manager.opus_beat_count) {
                main_fragment.update_column_label_size(index)
            }
        }
    }

    private fun tick_update_beats() {
        val opus_manager = this.getOpusManager()
        val main_fragment = this.getActiveFragment()
        val updated_beats: MutableSet<Int> = mutableSetOf()

        while (true) {
            val beatkey = opus_manager.fetch_flag_change() ?: break
            if (main_fragment !is MainFragment) {
                continue
            }

            val y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)

            main_fragment.beat_update(y, beatkey.beat)

            for (linked_beatkey in opus_manager.get_all_linked(beatkey)) {
                updated_beats.add(linked_beatkey.beat)
            }
        }

        if (main_fragment is MainFragment) {
            main_fragment.tick_resize_beats(updated_beats.toList())
            for (b in updated_beats) {
                main_fragment.update_column_label_size(b)
            }
        }
    }

    private fun tick_apply_focus() {
        val opus_manager = this.getOpusManager()
        val cursor = opus_manager.get_cursor()
        val focused: MutableSet<Pair<BeatKey, List<Int>>> = mutableSetOf()
        if (this.focus_column) {
            for (y in 0 until opus_manager.line_count()) {
                val (channel, index) = opus_manager.get_channel_index(y)
                focused.add(
                    Pair(
                        BeatKey(channel, index, cursor.get_beatkey().beat),
                        listOf()
                    )
                )
            }
        } else if (this.focus_row) {
            for (x in 0 until opus_manager.opus_beat_count) {
                val beatkey = cursor.get_beatkey()
                focused.add(
                    Pair(
                        BeatKey(beatkey.channel, beatkey.line_offset, x),
                        listOf()
                    )
                )
            }
        } else {
            focused.add(Pair(cursor.get_beatkey(), cursor.get_position()))
        }

        var active_fragment = this.getActiveFragment()
        if (active_fragment is MainFragment) {
            active_fragment.apply_focus(focused, opus_manager)
        }
    }

    private fun tick_unapply_focus() {
        var active_fragment = this.getActiveFragment()
        if (active_fragment is MainFragment) {
            active_fragment.unapply_focus(this.getOpusManager())
        }
    }

    fun feedback_msg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

class RadMidiController(context: Context): MIDIController(context) { }

class MIDIInputDevice: VirtualMIDIDevice() {}
