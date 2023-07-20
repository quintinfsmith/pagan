package com.qfs.pagan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.databinding.FragmentSettingsBinding
import java.io.File
import java.io.FileInputStream

class SettingsFragment : PaganFragment() {
    // Boiler Plate //
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    //////////////////

    var import_soundfont_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                if (uri.path != null) {
                    val soundfont_dir = this.get_main().get_soundfont_directory()
                    var file_name = uri.toString().substring(uri.toString().lastIndexOf("%3A") + 3)
                    var new_file = File("${soundfont_dir}/$file_name")
                    this.get_main().applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
                        new_file.writeBytes(
                            FileInputStream(it.fileDescriptor).readBytes()
                        )
                    }

                    try {
                        SoundFont(new_file.path)
                        this.set_soundfont(new_file.name)
                    } catch (e: Exception) {
                        this.get_main().feedback_msg("Invalid sf2 File")
                        new_file.delete()
                        return@registerForActivityResult
                    }
                } else {
                    // TODO
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        this.get_main().lockDrawer()
        this.get_main().update_menu_options()
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        this.get_main().update_menu_options()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnChooseSoundFont = view.findViewById<Button>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.setOnClickListener {
            this.interact_btnChooseSoundFont(it)
        }
        var soundfont_filename = this.get_main().configuration.soundfont
        btnChooseSoundFont.text = when (soundfont_filename) {
            null -> {
                "No Soundfont"
            }
            else -> {
                soundfont_filename
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun interact_btnChooseSoundFont(view: View) {
        val wrapper = ContextThemeWrapper(this.activity, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, view)

        val soundfont_dir = this.get_main().get_soundfont_directory()
        popupMenu.menu.add(0, 0, 0, "No Soundfont")
        var x = 1
        if (! this.get_main().has_fluid_soundfont()) {
            popupMenu.menu.add(0, 1, 1, "Download Fluid Soundfont...")
        }
        soundfont_dir.listFiles()?.forEachIndexed { i: Int, file: File ->
            popupMenu.menu.add(0, i + 1, i + 1, file.name)
            x += 1
        }
        popupMenu.menu.add(0, x, x, "Import...")

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                0 -> {
                    this.disable_soundfont()
                }
                1 -> {
                    if (this.get_main().has_fluid_soundfont()) {
                        this.set_soundfont(soundfont_dir.listFiles()[it.itemId - 1].name)
                    } else {
                        this.get_main().download_fluid()
                    }
                }
                x -> {
                    this.import_soundfont()
                }
                else -> {
                    this.set_soundfont(soundfont_dir.listFiles()[it.itemId - 1].name)
                }
            }
            false
        }

        popupMenu.show()
    }

    fun disable_soundfont() {
        val btnChooseSoundFont = this.get_main().findViewById<Button>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.text = "No Soundfont"
        this.get_main().disable_soundfont()
        this.get_main().save_configuration()
    }

    fun set_soundfont(filename: String) {
        val btnChooseSoundFont = this.get_main().findViewById<Button>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.text = filename
        this.get_main().set_soundfont(filename)
        this.get_main().save_configuration()
    }

    fun import_soundfont() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)
        this.import_soundfont_launcher.launch(intent)
    }
}