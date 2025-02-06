package com.qfs.pagan

import android.annotation.SuppressLint
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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.concurrent.thread

class FragmentGlobalSettings : FragmentPagan<FragmentGlobalSettingsBinding>() {
    private var _import_soundfont_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                if (uri.path != null) {
                    val main = this.get_main()
                    val soundfont_dir = main.get_soundfont_directory()
                    val file_name = main.parse_file_name(uri)

                    val new_file = File("${soundfont_dir}/$file_name")
                    main.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
                        try {
                            new_file.outputStream().use { output_stream: FileOutputStream ->
                                FileInputStream(it.fileDescriptor).use { input_stream: FileInputStream ->
                                    input_stream.copyTo(output_stream, 4096 * 4)
                                }
                            }
                        } catch (e: FileNotFoundException) {
                            // TODO:  Feedback? Only breaks on devices without properly implementation (realme RE549c)
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
                    throw FileNotFoundException()
                }
            }
        }
    }

    override fun inflate( inflater: LayoutInflater, container: ViewGroup?): FragmentGlobalSettingsBinding {
        return FragmentGlobalSettingsBinding.inflate(inflater, container, false)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = this.get_main()

        if (main.is_soundfont_available()) {
            this.binding.root.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
        } else {
            this.binding.root.findViewById<TextView>(R.id.tvFluidUrl).setOnClickListener {
                val url = getString(R.string.url_fluid)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }

        val btnChooseSoundFont = view.findViewById<TextView>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.setOnClickListener {
            this.interact_btnChooseSoundFont()
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

        //val switch_stereo_playback = view.findViewById<Switch>(R.id.sPlaybackStereo)
        //switch_stereo_playback.isChecked = main.configuration.playback_stereo_mode == WaveGenerator.StereoMode.Stereo
        //switch_stereo_playback.setOnCheckedChangeListener { _, enabled: Boolean ->
        //    main.configuration.playback_stereo_mode = if (enabled) {
        //        WaveGenerator.StereoMode.Stereo
        //    } else {
        //        WaveGenerator.StereoMode.Mono
        //    }
        //    main.save_configuration()
        //    main.reinit_playback_device()
        //}

        //val switch_limit_samples = view.findViewById<Switch>(R.id.sLimitSamples)
        //switch_limit_samples.isChecked = main.configuration.playback_sample_limit != null
        //switch_limit_samples.setOnCheckedChangeListener { _, enabled: Boolean ->
        //    main.configuration.playback_sample_limit = if (enabled) {
        //        1
        //    } else {
        //        null
        //    }
        //    main.save_configuration()
        //    main.reinit_playback_device()
        //}

        val sample_rate_value_text = view.findViewById<PaganTextView>(R.id.tvSampleRate)
        sample_rate_value_text.text = getString(R.string.config_label_sample_rate, main.configuration.sample_rate)
        val slider_playback_quality = view.findViewById<SeekBar>(R.id.sbPlaybackQuality)
        val options = listOf(
            8000,
            11025,
            22050,
            32000,
            44100,
            48000
        )
        var index = 0
        for (i in options.indices) {
            if (options[i] >= main.configuration.sample_rate) {
                index = i
                break
            }
        }
        slider_playback_quality.max = options.size - 1
        slider_playback_quality.progress = index
        slider_playback_quality.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val v = options[p1]
                sample_rate_value_text.text = getString(R.string.config_label_sample_rate, v)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (seekbar != null) {
                    main.get_action_interface().set_sample_rate(options[seekbar.progress])
                    main.set_sample_rate(options[seekbar.progress])
                }
            }
        })
    }

    private fun interact_btnChooseSoundFont() {
        // TODO Track disable, set and import actions. not the popup (at least for now)
        val soundfont_dir = this.get_main().get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        val soundfonts = mutableListOf<Pair<Pair<Int, String?>, String>>( Pair(Pair(0, null), this.resources.getString(R.string.no_soundfont)) )
        soundfonts.add(Pair(Pair(2, null), getString(R.string.option_import_soundfont)))

        for (file in file_list) {
            soundfonts.add(Pair(Pair(1, file.name), file.name))
        }

        this.get_main().dialog_popup_menu(getString(R.string.dialog_select_soundfont), soundfonts) { _: Int, pair: Pair<Int, String?> ->
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

        for (file in file_list) {
            soundfonts.add(Pair(file.name, file.name))
        }
        main.dialog_popup_menu(getString(R.string.dialog_remove_soundfont_title), soundfonts) { _: Int, filename: String ->
            main.dialog_confirm(getString(R.string.dialog_remove_soundfont_text, filename)) {
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
        val main = this.get_main()
        thread {
            main.loading_reticle_show(getString(R.string.loading_new_soundfont))
            this.get_main().set_soundfont(filename)

            // Check that it set
            if (filename == main.configuration.soundfont) {
                btnChooseSoundFont.text = filename
                this.get_main().save_configuration()
            }
            main.loading_reticle_hide()
        }
    }

    private fun _import_soundfont() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)
        this._import_soundfont_launcher.launch(intent)
    }
}
