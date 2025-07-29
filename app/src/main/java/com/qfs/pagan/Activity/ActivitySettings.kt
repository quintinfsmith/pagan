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
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.pagan.MenuDialogEventHandler
import com.qfs.pagan.R
import com.qfs.pagan.databinding.ActivitySettingsBinding
import java.io.FileInputStream
import java.io.FileNotFoundException
import kotlin.concurrent.thread

class ActivitySettings : PaganActivity() {
    private lateinit var _binding: ActivitySettingsBinding

    var result_intent = Intent()

    var result_launcher_import_soundfont =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
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

                                            val buffer = ByteArray(4096)
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
                                        this.feedback_msg(this.getString(R.string.feedback_invalid_sf2_file))
                                        new_file.delete()
                                        this.loading_reticle_hide()
                                        return@thread
                                    }
                                }

                                this.runOnUiThread {
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

    internal var result_launcher_set_project_directory =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.also { result_data ->
                    result_data.data?.also { uri  ->
                        val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        this.contentResolver.takePersistableUriPermission(uri, new_flags)
                        this.configuration.project_directory = uri

                        this.get_project_manager().change_project_path(uri, this.intent.data)?.let {
                            this.result_intent.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                        }

                        this.update_result()
                        this.on_project_directory_set(uri)
                    }
                }
            }
        }

    internal var result_launcher_set_soundfont_directory_and_import =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.also { result_data ->
                    result_data.data?.also { uri  ->
                        val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        this.contentResolver.takePersistableUriPermission(uri, new_flags)
                        this.set_soundfont_directory(uri)
                        this.set_soundfont_directory_button_text()

                        if (!this.is_soundfont_available()) {
                            this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.VISIBLE
                            this.result_launcher_import_soundfont.launch(
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
        this.setResult(RESULT_OK, this.result_intent)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        this._binding = ActivitySettingsBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)

        val toolbar = this._binding.toolbar
        toolbar.background = null

        this.intent.data = this.intent.getStringExtra(EXTRA_ACTIVE_PROJECT)?.toUri()

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

        val sample_rate_value_text = this.findViewById<TextView>(R.id.tvSampleRate)
        sample_rate_value_text.text = this.getString(R.string.config_label_sample_rate, this.configuration.sample_rate)
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
                    sample_rate_value_text.text = this@ActivitySettings.getString(R.string.config_label_sample_rate, v)
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

        this.findViewById<RadioGroup>(R.id.rgNightMode).let {
            it.check(when (this.configuration.night_mode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.rbNightModeNo
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.rbNightModeYes
                else -> R.id.rbNightModeSystem
            })

            it.setOnCheckedChangeListener { _, value: Int ->
                this.set_night_mode(
                    when (value) {
                        R.id.rbNightModeNo -> AppCompatDelegate.MODE_NIGHT_NO
                        R.id.rbNightModeYes -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
            }
        }

        if (this.is_soundfont_available()) {
            this.findViewById<LinearLayout>(R.id.llSFWarning).visibility = View.GONE
        } else {
            this.findViewById<TextView>(R.id.tvFluidUrl).setOnClickListener {
                this.startActivity(
                    Intent(Intent.ACTION_VIEW).also {
                        it.data = this.getString(R.string.url_fluid).toUri()
                    }
                )
            }
        }

        this.findViewById<Button>(R.id.btn_set_project_directory).setOnClickListener {
            this.result_launcher_set_project_directory.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                    intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    this.configuration.project_directory?.let {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                    }
                }
            )
        }

        this.findViewById<Button>(R.id.btn_settings_set_soundfont_directory).setOnClickListener {
            this._set_soundfont_directory_intent_launcher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                    intent.putExtra(Intent.EXTRA_TITLE, "Soundfonts")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    this.configuration.soundfont_directory?.let {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                    }
                }
            )
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

    fun set_night_mode(value: Int) {
        this.configuration.night_mode = value
        AppCompatDelegate.setDefaultNightMode(value)
        this.update_result()
    }

    fun import_soundfont(uri: Uri? = null) {
        if (this.configuration.soundfont_directory == null) {
            this.initial_dialog_select_soundfont_directory()
        } else if (uri == null) {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this.result_launcher_import_soundfont.launch(intent)
        } else {
            TODO("would only be used for debug atm anyway.")
        }
    }

    fun set_soundfont_button_text() {
        this.findViewById<TextView>(R.id.btnChooseSoundFont).text = when (this.get_soundfont_uri()) {
            null -> this.getString(R.string.no_soundfont)
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

        val soundfonts = mutableListOf<Triple<Uri, Int?, String>>()
        for (uri in file_list) {
            val relative_path_segments = uri.pathSegments.last().split("/")
            val relative_path = relative_path_segments.subList(1, relative_path_segments.size).joinToString("/")
            soundfonts.add(Triple(uri, null, relative_path))
        }

        val sort_options = listOf(
            Pair(this.getString(R.string.sort_option_abc)) { original: List<Triple<Uri, Int?, String>> ->
                original.sortedBy { item: Triple<Uri, Int?, String> -> item.third }
            }
        )

        val dialog = this.dialog_popup_sortable_menu(this.getString(R.string.dialog_select_soundfont), soundfonts, null, sort_options, 0, object: MenuDialogEventHandler<Uri>() {
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
                this.result_launcher_set_soundfont_directory_and_import.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also {
                        it.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    }
                )
            }
            .setPositiveButton(this.getString(android.R.string.ok)) { dialog, _ ->
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
