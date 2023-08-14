package com.qfs.pagan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.databinding.FragmentGlobalSettingsBinding
import java.io.File
import java.io.FileInputStream

class GlobalSettingsFragment : PaganFragment() {
    // Boiler Plate //
    private var _binding: FragmentGlobalSettingsBinding? = null
    private val binding get() = _binding!!
    //////////////////

    var import_soundfont_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                if (uri.path != null) {
                    val main = this.get_main()
                    val soundfont_dir = main.get_soundfont_directory()
                    val file_name = main.parse_file_name(uri)

                    val new_file = File("${soundfont_dir}/$file_name")
                    main.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
                        new_file.writeBytes(
                            FileInputStream(it.fileDescriptor).readBytes()
                        )
                    }

                    try {
                        SoundFont(new_file.path)
                        this.set_soundfont(new_file.name)
                    } catch (e: Exception) {
                        this.get_main().feedback_msg(getString(R.string.feedback_invalid_sf2_file))
                        new_file.delete()
                        return@registerForActivityResult
                    }

                    // Hide the warning
                    if (main.has_soundfont()) {
                        main.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
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
        _binding = FragmentGlobalSettingsBinding.inflate(inflater, container, false)
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
        val main = this.get_main()

        if (main.has_soundfont()) {
            main.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
        }  else {
            main.findViewById<TextView>(R.id.tvFluidUrl).setOnClickListener {
                val url = getString(R.string.url_fluid)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }

        val btnChooseSoundFont = view.findViewById<Button>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.setOnClickListener {
            this.interact_btnChooseSoundFont(it)
        }

        val soundfont_filename = this.get_main().configuration.soundfont

        btnChooseSoundFont.text = when (soundfont_filename) {
            null -> {
                getString(R.string.no_soundfont)
            }
            else -> {
                val soundfont_dir = main.get_soundfont_directory()
                val filecheck = File("${soundfont_dir}/$soundfont_filename")
                if (filecheck.exists()) {
                    soundfont_filename
                } else {
                    getString(R.string.no_soundfont)
                }
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
        popupMenu.menu.add(0, 0, 0, getString(R.string.no_soundfont))

        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()
        file_list.forEachIndexed { i: Int, file: File ->
            popupMenu.menu.add(1, i, i, file.name)
        }

        // +3 to the order to account for preceding menu entries
        popupMenu.menu.add(2, 0, file_list.size + 3, getString(R.string.option_import_soundfont))

        popupMenu.setOnMenuItemClickListener {
            when (it.groupId) {
                0 -> {
                    this.disable_soundfont()
                }
                1 -> {
                    this.set_soundfont(file_list[it.order].name)
                }
                2 -> {
                    this.import_soundfont()
                }
                else -> { }
            }
            false
        }

        popupMenu.show()
    }

    fun disable_soundfont() {
        val btnChooseSoundFont = this.get_main().findViewById<Button>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.text = getString(R.string.no_soundfont)
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