package com.qfs.pagan.Activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.PaganActivity
import com.qfs.pagan.R
import com.qfs.pagan.databinding.ActivitySettingsBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import androidx.core.net.toUri

class ActivitySettings : PaganActivity() {
    private lateinit var _binding: ActivitySettingsBinding

    var _import_soundfont_intent_listener = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result?.data?.data?.also { uri ->
                if (uri.path != null) {
                    val soundfont_dir = this.get_soundfont_directory()
                    val file_name = this.parse_file_name(uri)

                    val new_file = File("${soundfont_dir}/$file_name")
                    this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
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
                        this.configuration.soundfont = file_name
                    } catch (_: Exception) {
                        this.feedback_msg(getString(R.string.feedback_invalid_sf2_file))
                        new_file.delete()
                        return@registerForActivityResult
                    }

                    this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
                    this.set_soundfont_button_text()
                    this.update_result()
                } else {
                    throw FileNotFoundException()
                }
            }
        }
    }

    private fun update_result() {
        // RESULT_OK lets the other activities know they need to reload the configuration
        this.save_configuration()
        this.setResult(RESULT_OK, Intent())
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        this._binding = ActivitySettingsBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)
        this._binding.root.setBackgroundColor(resources.getColor(R.color.main_bg))

        val toolbar = this._binding.toolbar
        toolbar.background = null

        this.findViewById<TextView>(R.id.btnChooseSoundFont).let {
            it.setOnClickListener {
                this.interact_btnChooseSoundFont()
            }
            it.setOnLongClickListener {
                this.dialog_remove_soundfont()
                true
            }
        }
        this.set_soundfont_button_text()

        this.findViewById<SwitchCompat>(R.id.sRelativeEnabled).let {
            it.isChecked = this.configuration.relative_mode
            it.setOnCheckedChangeListener { _, enabled: Boolean ->
                this.configuration.relative_mode = enabled
                this.update_result()
            }
        }

        this.findViewById<SwitchCompat>(R.id.sClipSameLineRelease).let {
            it.isChecked = this.configuration.clip_same_line_release
            it.setOnCheckedChangeListener { _, enabled: Boolean ->
                this.configuration.clip_same_line_release = enabled
                this.update_result()
            }
        }

        //val switch_stereo_playback = view.findViewById<SwitchCompat>(R.id.sPlaybackStereo)
        //switch_stereo_playback.isChecked = this.configuration.playback_stereo_mode == WaveGenerator.StereoMode.Stereo
        //switch_stereo_playback.setOnCheckedChangeListener { _, enabled: Boolean ->
        //    this.configuration.playback_stereo_mode = if (enabled) {
        //        WaveGenerator.StereoMode.Stereo
        //    } else {
        //        WaveGenerator.StereoMode.Mono
        //    }
        //    this.save_configuration()
        //    this.reinit_playback_device()
        //}

        //val switch_limit_samples = view.findViewById<SwitchCompat>(R.id.sLimitSamples)
        //switch_limit_samples.isChecked = this.configuration.playback_sample_limit != null
        //switch_limit_samples.setOnCheckedChangeListener { _, enabled: Boolean ->
        //    this.configuration.playback_sample_limit = if (enabled) {
        //        1
        //    } else {
        //        null
        //    }
        //    this.save_configuration()
        //    this.reinit_playback_device()
        //}

        val sample_rate_value_text = this.findViewById<TextView>(R.id.tvSampleRate)
        sample_rate_value_text.text = getString(R.string.config_label_sample_rate, this.configuration.sample_rate)
        this.findViewById<SeekBar>(R.id.sbPlaybackQuality).let {
            val options = listOf(8000, 11025, 22050, 32000, 44100, 48000)
            var index = 0
            for (i in options.indices) {
                if (options[i] >= this.configuration.sample_rate) {
                    index = i
                    break
                }
            }

            it.max = options.size - 1
            it.progress = index
            it.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    val v = options[p1]
                    sample_rate_value_text.text = getString(R.string.config_label_sample_rate, v)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(seekbar: SeekBar?) {
                    if (seekbar != null) {
                        this@ActivitySettings.configuration.sample_rate = options[seekbar.progress]
                        this@ActivitySettings.update_result()
                    }
                }
            })
        }
        this.findViewById<SwitchCompat>(R.id.sUsePreferredSF).let {
            it.isChecked = this.configuration.use_preferred_soundfont
            it.setOnCheckedChangeListener { _, enabled: Boolean ->
                this.configuration.use_preferred_soundfont = enabled
                this.update_result()
            }
        }

        this.findViewById<SwitchCompat>(R.id.sEnableMidi).let {
            it.isChecked = this.configuration.allow_midi_playback
            it.setOnCheckedChangeListener { _, enabled: Boolean ->
                this.configuration.allow_midi_playback = enabled
                this.update_result()
            }
        }

        this.findViewById<SwitchCompat>(R.id.sAllowStdPercussion).let {
            it.isChecked = this.configuration.allow_std_percussion
            it.setOnCheckedChangeListener { _, enabled: Boolean ->
                this.configuration.allow_std_percussion = enabled
                this.update_result()
            }
        }


        this.findViewById<RadioGroup>(R.id.rgLockOrientation).let {
            it.check(when (this.configuration.force_orientation) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> R.id.rbOrientationLandscape
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> R.id.rbOrientationPortrait
                else -> R.id.rbOrientationUser

            })

            it.setOnCheckedChangeListener { _, value: Int ->
                this.set_forced_orientation(
                    when (value) {
                        R.id.rbOrientationLandscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        R.id.rbOrientationPortrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else -> ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                )
            }
        }

        if (this.is_soundfont_available()) {
            this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
        } else {
            this.findViewById<TextView>(R.id.tvFluidUrl).setOnClickListener {
                val url = getString(R.string.url_fluid)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = url.toUri()
                startActivity(intent)
            }
        }

        this.setResult(RESULT_CANCELED)
    }

    fun is_soundfont_available(): Boolean {
        val soundfont_dir = this.get_soundfont_directory()
        return soundfont_dir.listFiles()?.isNotEmpty() == true
    }

    fun set_forced_orientation(value: Int) {
        this.configuration.force_orientation = value
        this.requestedOrientation = value
        this.update_result()
    }

    fun import_soundfont(uri: Uri? = null) {
        // TODO: Track action
        if (uri == null) {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this._import_soundfont_intent_listener.launch(intent)
        } else {
            TODO("would only be used for debug atm anyway.")
        }
    }

    fun set_soundfont_button_text() {
        this.findViewById<TextView>(R.id.btnChooseSoundFont).text = when (this.configuration.soundfont) {
            null -> getString(R.string.no_soundfont)
            else -> {
                val soundfont_dir = this.get_soundfont_directory()
                val filecheck = File("${soundfont_dir}/${this.configuration.soundfont}")
                if (filecheck.exists()) {
                    this.configuration.soundfont
                } else {
                    getString(R.string.no_soundfont)
                }
            }
        }
    }


    private fun interact_btnChooseSoundFont() {
        val soundfont_dir = this.get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        if (file_list.isEmpty()) {
            this.import_soundfont()
            return
        }

        val soundfonts = mutableListOf<Pair<String, String>>()
        for (file in file_list) {
            soundfonts.add(Pair(file.name, file.name))
        }

        val sort_options = listOf(
            Pair(getString(R.string.sort_option_abc)) { original: List<Pair<String, String>> ->
                original.sortedBy { item: Pair<String, String> -> item.first }
            }
        )

        val dialog = this.dialog_popup_sortable_menu(getString(R.string.dialog_select_soundfont), soundfonts, null, sort_options, 0) { _: Int, path: String ->
            this.configuration.soundfont = path
            this.set_soundfont_button_text()
            this.update_result()
        }

        dialog?.findViewById<ViewGroup>(R.id.menu_wrapper)?.let { menu_wrapper ->
            val pre_menu: View = LayoutInflater.from(this)
                .inflate(
                    R.layout.soundfont_pre_menu,
                    menu_wrapper,
                    false
                )

            menu_wrapper.addView(pre_menu, 0)
            menu_wrapper.findViewById<Button>(R.id.sf_menu_mute).setOnClickListener {
                this.configuration.soundfont = null
                this.update_result()
                this.set_soundfont_button_text()
                menu_wrapper.findViewById<Button>(R.id.sf_menu_mute).text
                dialog.dismiss()
            }

            menu_wrapper.findViewById<Button>(R.id.sf_menu_import).setOnClickListener {
                this.import_soundfont()
                dialog.dismiss()
            }
        }
    }

    fun get_soundfont_directory(): File {
        val soundfont_dir = File("${this.getExternalFilesDir(null)}/SoundFonts")
        if (!soundfont_dir.exists()) {
            soundfont_dir.mkdirs()
        }

        return soundfont_dir
    }

    private fun dialog_remove_soundfont() {
        val soundfont_dir = this.get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        val soundfonts = mutableListOf<Pair<String, String>>( )

        for (file in file_list) {
            soundfonts.add(Pair(file.name, file.name))
        }
        this.dialog_popup_menu(getString(R.string.dialog_remove_soundfont_title), soundfonts) { _: Int, filename: String ->
            this.dialog_confirm(getString(R.string.dialog_remove_soundfont_text, filename)) {
                this._delete_soundfont(filename)
            }
        }
    }

    private fun _delete_soundfont(filename: String) {
        if (this.configuration.soundfont == filename) {
            this.configuration.soundfont = null
        }

        val soundfont_dir = this.get_soundfont_directory()
        val file = File("${soundfont_dir.absolutePath}/${filename}")
        if (file.exists()) {
            file.delete()
        }

        this.set_soundfont_button_text()
    }
}
