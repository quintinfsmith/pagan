package com.qfs.radixulous

//import com.qfs.radixulous.MIDIPlaybackDevice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.qfs.radixulous.apres.*
import com.qfs.radixulous.databinding.ActivityMainBinding
import com.qfs.radixulous.opusmanager.HistoryLayer
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread


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

    private var opus_manager = HistoryLayer()
    var working_path: String? = null

    private lateinit var optionsMenu: Menu

    var export_midi_intent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            var opus_manager = this.getOpusManager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                    Toast.makeText(this, "Exported to midi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(this.binding.root)

        setSupportActionBar(this.binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        this.appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, this.appBarConfiguration)

        //////////////////////////////////////////
        // TODO: clean up the file -> riff -> soundfont -> midi playback device process
        var soundfont = SoundFont(Riff(assets.open("freepats-general-midi.sf2")))
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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        when (item.itemId) {
            R.id.itmNewProject -> {
                // TODO: Save or discard popup dialog
                var navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                var main_fragment = navHost?.childFragmentManager?.fragments?.get(0)
                if (main_fragment is MainFragment) {
                    main_fragment.takedownCurrent()
                    this.newProject()

                    this.getOpusManager().get_working_dir()?.let {
                        this.set_title_text(
                            it.substring(it.lastIndexOf("/") + 1)
                        )
                    }

                    main_fragment.setContextMenu(ContextMenu.Leaf)
                    main_fragment.tick()
                }
            }
            R.id.itmLoadProject -> {
                navController.navigate(R.id.action_MainFragment_to_LoadFragment)
            }
            R.id.itmSaveProject -> {
                this.save()
            }
            R.id.itmExportMidi -> this.export_midi()
            R.id.itmUndo -> {
                var navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                var main_fragment = navHost?.childFragmentManager?.fragments?.get(0)
                if (main_fragment is MainFragment) {
                    main_fragment.undo()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun getOpusManager(): HistoryLayer {
        return this.opus_manager
    }

    fun update_menu_options() {
        var navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        var fragment = navHost?.childFragmentManager?.fragments?.get(0)
        when (fragment) {
            is MainFragment -> {
                this.optionsMenu.findItem(R.id.itmLoadProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmSaveProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmUndo).isVisible = true
                this.optionsMenu.findItem(R.id.itmNewProject).isVisible = true
                this.optionsMenu.findItem(R.id.itmExportMidi).isVisible = true
            }
            //is LoadFragment -> { }
            //is ConfigFragment -> { }
            else -> {
                this.optionsMenu.findItem(R.id.itmLoadProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmSaveProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmUndo).isVisible = false
                this.optionsMenu.findItem(R.id.itmNewProject).isVisible = false
                this.optionsMenu.findItem(R.id.itmExportMidi).isVisible = false
            }
        }
    }

    fun newProject() {
        this.opus_manager.new()

        var projects_dir = "/data/data/com.qfs.radixulous/projects"
        var i = 0
        while (File("$projects_dir/opus$i").isFile) {
            i += 1
        }
        this.opus_manager.path = "$projects_dir/opus$i"
    }

    private fun save() {
        this.opus_manager.save()
        Toast.makeText(this, "Project Saved", Toast.LENGTH_SHORT).show()
    }

    fun set_title_text(new_text: String) {
        this.binding.toolbar!!.title = new_text
    }

    fun play_event(channel: Int, event_value: Int) {
        this.midi_input_device.sendEvent(NoteOn(channel, event_value + 21, 64))
        thread {
            Thread.sleep(200)
            this.midi_input_device.sendEvent(NoteOff(channel, event_value + 21, 64))
        }
    }

    fun play_midi(midi: MIDI) {
        this.midi_player.play_midi(midi)
    }

    fun export_midi() {
        var opus_manager = this.getOpusManager()

        var name = opus_manager.get_working_dir()
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/midi"
            putExtra(Intent.EXTRA_TITLE, "$name.mid")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
        }

        this.export_midi_intent.launch(intent)
    }

}

class RadMidiController(context: Context): MIDIController(context) { }

class MIDIInputDevice: VirtualMIDIDevice() {}
