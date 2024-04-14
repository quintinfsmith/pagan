package com.qfs.pagan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.ColorMap.Palette
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

        val switch_relative_mode = view.findViewById<PaganSwitch>(R.id.sRelativeEnabled)
        switch_relative_mode.isChecked = main.configuration.relative_mode
        switch_relative_mode.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.configuration.relative_mode = enabled
            main.save_configuration()
        }

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
                main.configuration.sample_rate = options[seekbar!!.progress]
                main.set_sample_rate(main.configuration.sample_rate)
                main.save_configuration()
            }
        })

        // Palette Shiz ----------------------------
        main.loading_reticle_show()
        val color_map = main.view_model.color_map
        val sCustomPalette = view.findViewById<PaganSwitch>(R.id.sCustomPalette)
        val btnClearPalette = view.findViewById<ButtonStd>(R.id.btnClearPalette)
        btnClearPalette.setOnClickListener {
            main.dialog_confirm(getString(R.string.dialog_reset_colors)) {
                main.view_model.color_map.unpopulate()
                btnClearPalette.visibility = View.GONE
                sCustomPalette.isChecked = false
                sCustomPalette.isChecked = true
            }
        }

        val llColorPalette = view.findViewById<LinearLayout>(R.id.llColorPalette)

        sCustomPalette.setOnCheckedChangeListener { _: CompoundButton, is_checked: Boolean ->
            if (is_checked) {
                btnClearPalette.visibility = View.VISIBLE
                color_map.use_palette = true
                if (!main.view_model.color_map.is_set()) {
                    main.view_model.color_map.populate()
                }
                btnClearPalette.visibility = View.VISIBLE
                llColorPalette.visibility = View.VISIBLE

                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_title_bar), Palette.TitleBar))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_title_bar_text), Palette.TitleBarText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_background), Palette.Background))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_foreground), Palette.Foreground))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_button), Palette.Button))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_button_text), Palette.ButtonText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_button_alt), Palette.ButtonAlt))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_button_alt_text), Palette.ButtonAltText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_lines), Palette.Lines))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_column_label), Palette.ColumnLabel))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_column_label_text), Palette.ColumnLabelText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_channel_even), Palette.ChannelEven))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_channel_even_text), Palette.ChannelEvenText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_channel_odd), Palette.ChannelOdd))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_channel_odd_text), Palette.ChannelOddText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_selection), Palette.Selection))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_selection_text), Palette.SelectionText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf), Palette.Leaf))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_text), Palette.LeafText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_selected), Palette.LeafSelected))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_selected_text), Palette.LeafSelectedText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_link), Palette.Link))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_link_text), Palette.LinkText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_link_empty), Palette.LinkEmpty))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_link_selected), Palette.LinkSelected))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_link_selected_text), Palette.LinkSelectedText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_link_empty_selected), Palette.LinkEmptySelected))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_invalid), Palette.LeafInvalid))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_invalid_text), Palette.LeafInvalidText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_invalid_selected), Palette.LeafInvalidSelected))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_leaf_invalid_selected_text), Palette.LeafInvalidSelectedText))

                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_ctl_line_selection), Palette.CtlLineSelection))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_ctl_line_selection_text), Palette.CtlLineSelectionText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_ctl_leaf), Palette.CtlLeaf))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_ctl_leaf_text), Palette.CtlLeafText))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_ctl_leaf_selected), Palette.CtlLeafSelected))
                llColorPalette.addView(InlineColorPicker(main, getString(R.string.palette_ctl_leaf_selected_text), Palette.CtlLeafSelectedText))
            } else {
                color_map.use_palette = false
                llColorPalette.visibility = View.GONE
                llColorPalette.removeAllViews()
                btnClearPalette.visibility = View.GONE
            }
            main.save_configuration()
        }
        sCustomPalette.isChecked = color_map.is_set() && main.configuration.use_palette
        main.loading_reticle_hide()
    }

    private fun interact_btnChooseSoundFont() {
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