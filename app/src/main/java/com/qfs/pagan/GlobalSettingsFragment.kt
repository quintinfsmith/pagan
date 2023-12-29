package com.qfs.pagan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.databinding.FragmentGlobalSettingsBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class GlobalSettingsFragment : PaganFragment<FragmentGlobalSettingsBinding>() {
    private var _import_soundfont_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                if (uri.path != null) {
                    val main = this.get_main()
                    val soundfont_dir = main.get_soundfont_directory()
                    val file_name = main.parse_file_name(uri)

                    val new_file = File("${soundfont_dir}/$file_name")
                    main.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
                        new_file.outputStream().use { output_stream: FileOutputStream ->
                            FileInputStream(it.fileDescriptor).use { input_stream: FileInputStream ->
                                input_stream.copyTo(output_stream, 4096 * 4)
                            }
                        }
                    }

                    try {
                        SoundFont(new_file.path)
                        this._set_soundfont(new_file.name)
                    } catch (e: Exception) {
                        this.get_main().feedback_msg(getString(R.string.feedback_invalid_sf2_file))
                        new_file.delete()
                        return@registerForActivityResult
                    }

                    // Hide the warning
                    if (main.is_soundfont_available()) {
                        main.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
                    }
                } else {
                    // TODO
                }
            }
        }
    }

    override fun inflate( inflater: LayoutInflater, container: ViewGroup?): FragmentGlobalSettingsBinding {
        return FragmentGlobalSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = this.get_main()

        if (main.is_soundfont_available()) {
            this.binding.root.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
        }  else {
            this.binding.root.findViewById<TextView>(R.id.tvFluidUrl).setOnClickListener {
                val url = getString(R.string.url_fluid)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }

        val btnChooseSoundFont = view.findViewById<TextView>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.setOnClickListener {
            this.interact_btnChooseSoundFont(it)
        }
        btnChooseSoundFont.setOnLongClickListener {
            this.dialog_remove_soundfont()
            false
        }

        val soundfont_filename = main.configuration.soundfont

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

        val switch_relative_mode = view.findViewById<Switch>(R.id.sRelativeEnabled)
        switch_relative_mode.isChecked = main.configuration.relative_mode
        switch_relative_mode.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.configuration.relative_mode = enabled
            main.save_configuration()
        }

        val slider_playback_quality = view.findViewById<SeekBar>(R.id.sbPlaybackQuality)
        val min_sample_rate = 11025
        val max_sample_rate = 44100
        slider_playback_quality.progress = (main.configuration.sample_rate - min_sample_rate) * slider_playback_quality.max / (max_sample_rate - min_sample_rate)
        slider_playback_quality.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) { }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                main.configuration.sample_rate = (seekbar!!.progress * (max_sample_rate - min_sample_rate) / seekbar.max) + min_sample_rate
                main.set_sample_rate(main.configuration.sample_rate)
                main.save_configuration()
            }
        })
    }

    private fun interact_btnChooseSoundFont(view: View) {
        val soundfont_dir = this.get_main().get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        val soundfonts = mutableListOf<Pair<Pair<Int, String?>, String>>( Pair(Pair(0, null), "No SoundFont") )

        file_list.forEachIndexed { i: Int, file: File ->
            soundfonts.add(Pair(Pair(1, file.name), file.name))
        }

        soundfonts.add(Pair(Pair(2, null), getString(R.string.option_import_soundfont)))

        this.get_main().dialog_popup_menu("Select Soundfont", soundfonts) { index: Int, pair: Pair<Int, String?> ->
            val (mode, path) = pair
            when (mode) {
                0 -> this._disable_soundfont()
                1 -> this._set_soundfont(path!!)
                2 -> this._import_soundfont()
            }
        }
    }

    private fun dialog_remove_soundfont() {
        val main = this.get_main()
        val soundfont_dir = main.get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        val soundfonts = mutableListOf<Pair<String, String>>( )

        file_list.forEachIndexed { i: Int, file: File ->
            soundfonts.add(Pair(file.name, file.name))
        }
        main.dialog_popup_menu("Choose Soundfont to Remove", soundfonts) { i: Int, filename: String ->
            main.dialog_confirm("Really Delete $filename?") {
                this._delete_soundfont(filename)
            }
        }
    }

    private fun _delete_soundfont(filename: String) {
        val main = this.get_main()
        if (main.configuration.soundfont != null && main.configuration.soundfont!! == filename) {
            this._disable_soundfont()
        }
        val soundfont_dir = main.get_soundfont_directory()
        val file = File("${soundfont_dir.absolutePath}/${filename}")
        if (file.exists()) {
            file.delete()
        }
    }

    private fun _disable_soundfont() {
        val btnChooseSoundFont = this.get_main().findViewById<TextView>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.text = getString(R.string.no_soundfont)
        this.get_main().disable_soundfont()
        this.get_main().save_configuration()
    }

    private fun _set_soundfont(filename: String) {
        val btnChooseSoundFont = this.get_main().findViewById<TextView>(R.id.btnChooseSoundFont)
        this.get_main().set_soundfont(filename)

        btnChooseSoundFont.text = filename
        this.get_main().save_configuration()
    }

    private fun _import_soundfont() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)
        this._import_soundfont_launcher.launch(intent)
    }
}