package com.qfs.pagan

import android.annotation.SuppressLint
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
import androidx.appcompat.widget.SwitchCompat
import com.qfs.pagan.databinding.FragmentGlobalSettingsBinding
import java.io.File

class FragmentGlobalSettings : FragmentPagan<FragmentGlobalSettingsBinding>() {
    override fun inflate( inflater: LayoutInflater, container: ViewGroup?): FragmentGlobalSettingsBinding {
        return FragmentGlobalSettingsBinding.inflate(inflater, container, false)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = this.get_activity()

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
            this.get_activity().get_action_interface().delete_soundfont()
            true
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

        val switch_relative_mode = view.findViewById<SwitchCompat>(R.id.sRelativeEnabled)
        switch_relative_mode.isChecked = main.configuration.relative_mode
        switch_relative_mode.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.get_action_interface().set_relative_mode_visibility(enabled)
        }

        val switch_clip_release = view.findViewById<SwitchCompat>(R.id.sClipSameLineRelease)
        switch_clip_release.isChecked = main.configuration.clip_same_line_release
        switch_clip_release.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.get_action_interface().set_clip_same_line_notes(enabled)
        }


        //val switch_stereo_playback = view.findViewById<SwitchCompat>(R.id.sPlaybackStereo)
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

        //val switch_limit_samples = view.findViewById<SwitchCompat>(R.id.sLimitSamples)
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

        val sample_rate_value_text = view.findViewById<TextView>(R.id.tvSampleRate)
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
                }
            }
        })


        val switch_use_preferred_sf = view.findViewById<SwitchCompat>(R.id.sUsePreferredSF)
        switch_use_preferred_sf.isChecked = main.configuration.use_preferred_soundfont
        switch_use_preferred_sf.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.configuration.use_preferred_soundfont = enabled
            main.save_configuration()
        }

        val switch_enable_midi_playback = view.findViewById<SwitchCompat>(R.id.sEnableMidi)
        switch_enable_midi_playback.isChecked = main.configuration.allow_midi_playback
        switch_enable_midi_playback.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.get_action_interface().allow_midi_playback(enabled)
        }

        val switch_allow_std_percussion = view.findViewById<SwitchCompat>(R.id.sAllowStdPercussion)
        switch_allow_std_percussion.isChecked = main.configuration.allow_std_percussion
        switch_allow_std_percussion.setOnCheckedChangeListener { _, enabled: Boolean ->
            main.get_action_interface().allow_std_percussion(enabled)
        }


        val lock_orientation_group = view.findViewById<RadioGroup>(R.id.rgLockOrientation)
        lock_orientation_group.check(when (main.configuration.force_orientation) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> R.id.rbOrientationLandscape
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> R.id.rbOrientationPortrait
            else -> R.id.rbOrientationUser

        })
        lock_orientation_group.setOnCheckedChangeListener { _, value: Int ->
            main.set_forced_orientation(
                when (value) {
                    R.id.rbOrientationLandscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    R.id.rbOrientationPortrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else -> ActivityInfo.SCREEN_ORIENTATION_USER
                }
            )
        }

    }

    private fun interact_btnChooseSoundFont() {
        val activity = this.get_activity()
        val soundfont_dir = activity.get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        if (file_list.isEmpty()) {
            val tracker = activity.get_action_interface()
            tracker.import_soundfont()
            return
        }

        val soundfonts = mutableListOf<Pair<String, String>>()
        for (file in file_list) {
            soundfonts.add(Pair(file.name, file.name))
        }

        val sort_options = listOf(
            Pair(
                activity.getString(R.string.sort_option_abc),
                { original: List<Pair<String, String>> ->
                    original.sortedBy { item: Pair<String, String> ->
                        item.first
                    }
                }
            )
        )

        val dialog = this.get_activity().dialog_popup_sortable_menu(getString(R.string.dialog_select_soundfont), soundfonts, null, sort_options, 0) { _: Int, path: String ->
            val tracker = activity.get_action_interface()
            tracker.set_soundfont(path)
        }

        if (dialog != null) {
            val menu_wrapper = dialog.findViewById<ViewGroup>(R.id.menu_wrapper)
            val pre_menu: View = LayoutInflater.from(activity)
                .inflate(
                    R.layout.soundfont_pre_menu,
                    menu_wrapper,
                    false
                )

            menu_wrapper.addView(pre_menu, 0)
            menu_wrapper.findViewById<Button>(R.id.sf_menu_mute).setOnClickListener {
                dialog.dismiss()
                val tracker = activity.get_action_interface()
                tracker.disable_soundfont()
            }
            menu_wrapper.findViewById<Button>(R.id.sf_menu_import).setOnClickListener {
                dialog.dismiss()
                val tracker = activity.get_action_interface()
                tracker.import_soundfont()
            }
        }
    }

    private fun dialog_remove_soundfont() {
        val main = this.get_activity()
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
        val main = this.get_activity()
        if (main.configuration.soundfont != null && main.configuration.soundfont!! == filename) {
            main.get_action_interface().ignore().disable_soundfont()
        }
        val soundfont_dir = main.get_soundfont_directory()
        val file = File("${soundfont_dir.absolutePath}/${filename}")
        if (file.exists()) {
            file.delete()
        }
    }
}
