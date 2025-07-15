package com.qfs.pagan.Activity

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.MenuDialogEventHandler
import com.qfs.pagan.PaganActivity
import com.qfs.pagan.R
import com.qfs.pagan.databinding.ActivitySettingsBinding
import java.io.FileInputStream
import java.io.FileNotFoundException
import kotlin.concurrent.thread

class ActivitySettings : PaganActivity() {
    private lateinit var _binding: ActivitySettingsBinding

    var _import_soundfont_intent_listener = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result?.data?.data?.also { uri ->
                if (uri.path != null) {

                    // Check if this selected file is within the soundfont_directory ////
                    // We can directory is set here.
                    val parent_segments = this.configuration.soundfont_directory!!.pathSegments!!.last()!!.split("/")
                    val child_segments = uri.pathSegments!!.last()!!.split("/")
                    val is_within_soundfont_directory = parent_segments.size < child_segments.size && parent_segments == child_segments.subList(0, parent_segments.size)
                    //-----------------------------------------------------
                    if (is_within_soundfont_directory) {
                        this.configuration.soundfont = child_segments.subList(parent_segments.size, child_segments.size).joinToString("/")
                        this.set_soundfont_button_text()
                        this.update_result()
                    } else {
                        thread {
                            val soundfont_dir = this.get_soundfont_directory()
                            val file_name = this.parse_file_name(uri)!!

                            this.loading_reticle_show()
                            soundfont_dir.createFile("*/*", file_name)?.let { new_file ->
                                this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
                                    try {
                                        val output_stream = this.contentResolver.openOutputStream(new_file.uri, "wt")
                                        val input_stream = FileInputStream(it.fileDescriptor)

                                        val buffer = ByteArray(1024)
                                        while (true) {
                                            val read_size = input_stream.read(buffer)
                                            if (read_size == -1) {
                                                break
                                            }
                                            output_stream?.write(buffer, 0, read_size)
                                        }

                                        input_stream.close()
                                        output_stream?.flush()
                                        output_stream?.close()

                                    } catch (e: FileNotFoundException) {
                                        // TODO:  Feedback? Only breaks on devices without properly implementation (realme RE549c)
                                    }
                                }

                                try {
                                    SoundFont(this, new_file.uri)
                                    this.configuration.soundfont = this@ActivitySettings.coax_relative_soundfont_path(new_file.uri)
                                    this.loading_reticle_hide()
                                } catch (e: Exception) {
                                    this.feedback_msg(getString(R.string.feedback_invalid_sf2_file))
                                    new_file.delete()
                                    this.loading_reticle_hide()
                                    return@thread
                                }
                            }


                            runOnUiThread {
                                this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
                            }
                            this.set_soundfont_button_text()
                            this.update_result()
                        }
                    }

                } else {
                    throw FileNotFoundException()
                }
            }
        }
    }


    internal var _set_soundfont_directory_and_import_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result?.data?.also { result_data ->
                result_data.data?.also { uri  ->
                    val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    this.contentResolver.takePersistableUriPermission(uri, new_flags)
                    this.set_soundfont_directory(uri)
                    this.set_soundfont_directory_button_text()
                    if (!this.is_soundfont_available()) {
                        this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.VISIBLE
                        this._import_soundfont_intent_listener.launch(
                            Intent()
                                .setType("*/*")
                                .setAction(Intent.ACTION_GET_CONTENT)
                        )
                    } else {
                        this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
                        this.dialog_select_soundfont()
                    }
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

        val toolbar = this._binding.toolbar
        toolbar.background = null

        //toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)

        this.findViewById<TextView>(R.id.btnChooseSoundFont).let {
            it.setOnClickListener {
                this.dialog_select_soundfont()
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
            val options = listOf(8000, 22050, 32000, 44100, 48000)
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

        this.findViewById<Button>(R.id.btn_set_project_directory).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            this.configuration.project_directory?.let {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
            }
            this._set_project_directory_intent_launcher.launch(intent)
        }

        this.findViewById<Button>(R.id.btn_settings_set_soundfont_directory).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra(Intent.EXTRA_TITLE, "Soundfonts")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            this.configuration.soundfont_directory?.let {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
            }
            this._set_soundfont_directory_intent_launcher.launch(intent)
        }

        this.findViewById<View>(R.id.ll_external_projects_warning).visibility = if (this.get_project_manager().has_external_storage_projects()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        this.set_project_directory_button_text()
        this.set_soundfont_directory_button_text()



        this.setResult(RESULT_CANCELED)
    }

    fun set_project_directory_button_text() {
        this.findViewById<Button>(R.id.btn_set_project_directory).text = if (this.configuration.project_directory == null) {
            this.getString(R.string.btn_settings_set_project_directory)
        } else {
            val last_segment = this.configuration.project_directory!!.pathSegments.last()
            last_segment.substring(last_segment.indexOf(":") + 1, last_segment.length)
        }
    }

    fun set_soundfont_directory_button_text() {
        this.findViewById<Button>(R.id.btn_settings_set_soundfont_directory).text = if (this.configuration.soundfont_directory == null) {
            this.getString(R.string.btn_settings_set_soundfont_directory)
        } else {
            val last_segment = this.configuration.soundfont_directory!!.pathSegments.last()
            last_segment.substring(last_segment.indexOf(":") + 1, last_segment.length)
        }
    }

    fun set_forced_orientation(value: Int) {
        this.configuration.force_orientation = value
        this.requestedOrientation = value
        this.update_result()
    }

    fun import_soundfont(uri: Uri? = null) {
        if (this.configuration.soundfont_directory == null) {
            this.initial_dialog_select_soundfont_directory()
        } else if (uri == null) {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this._import_soundfont_intent_listener.launch(intent)
        } else {
            TODO("would only be used for debug atm anyway.")
        }
    }

    fun set_soundfont_button_text() {
        this.findViewById<TextView>(R.id.btnChooseSoundFont).text = when (this.get_soundfont_uri()) {
            null -> getString(R.string.no_soundfont)
            else -> {
                this.configuration.soundfont!!.split("/").last()
            }
        }
    }


    private fun dialog_select_soundfont() {
        val soundfont_dir = this.get_soundfont_directory()
        val file_list: MutableList<Uri> = mutableListOf()
        val stack = mutableListOf(soundfont_dir)
        while (stack.isNotEmpty()) {
            val current_document_file = stack.removeAt(0)
            for (child in current_document_file.listFiles()) {
                if (child.isDirectory) {
                    stack.add(child)
                } else if (child.isFile) {
                    file_list.add(child.uri)
                }
            }
        }


        if (file_list.isEmpty()) {
            this.import_soundfont()
            return
        }

        val soundfonts = mutableListOf<Pair<Uri, String>>()
        for (uri in file_list) {
            val relative_path_segments = uri.pathSegments.last().split("/")
            val relative_path = relative_path_segments.subList(1, relative_path_segments.size).joinToString("/")
            soundfonts.add(Pair(uri, relative_path))
        }

        val sort_options = listOf(
            Pair(getString(R.string.sort_option_abc)) { original: List<Pair<Uri, String>> ->
                original.sortedBy { item: Pair<Uri, String> -> item.second }
            }
        )

        val dialog = this.dialog_popup_sortable_menu(getString(R.string.dialog_select_soundfont), soundfonts, null, sort_options, 0, object: MenuDialogEventHandler<Uri>() {
            override fun on_submit(index: Int, value: Uri) {
                this@ActivitySettings.configuration.soundfont = this@ActivitySettings.coax_relative_soundfont_path(value)
                this@ActivitySettings.set_soundfont_button_text()
                this@ActivitySettings.update_result()
            }
        })

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun initial_dialog_select_soundfont_directory() {
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setMessage(this.getString(R.string.settings_need_soundfont_directory))
            .setOnDismissListener {
                this._popup_active = false
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                this._set_soundfont_directory_and_import_intent_launcher.launch(intent)
            }
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun on_soundfont_directory_set(uri: Uri) {
        this.set_soundfont_directory_button_text()

        this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = if (this.is_soundfont_available()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun on_project_directory_set(uri: Uri) {
        this.set_project_directory_button_text()

        this.findViewById<View>(R.id.ll_external_projects_warning).visibility = if (this.get_project_manager().has_external_storage_projects()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
