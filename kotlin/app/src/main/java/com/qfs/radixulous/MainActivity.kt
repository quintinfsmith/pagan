package com.qfs.radixulous

//import com.qfs.radixulous.MIDIPlaybackDevice

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
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

    fun getOpusManager(): HistoryLayer {
        return this.opus_manager
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

    // method to inflate the options menu when
    // the user opens the menu for the first time
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // TODO: decide which menu based on active fragment?
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // methods to control the operations that will
    // happen when user clicks on the action buttons
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        when (item.itemId) {
            //R.id.itmNewProject -> this.newProject()
            R.id.itmLoadProject -> navController.navigate(R.id.action_MainFragment_to_LoadFragment)
            //R.id.itmSaveProject -> this.save()
            //R.id.itmExportMidi -> this.export_midi()
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

    override fun onBackPressed() {
        super.onBackPressed()
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}

class RadMidiController(context: Context): MIDIController(context) { }

class MIDIInputDevice: VirtualMIDIDevice() {}
