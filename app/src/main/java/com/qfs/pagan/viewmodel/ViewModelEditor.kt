package com.qfs.pagan.viewmodel

import android.media.midi.MidiDeviceInfo
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.Activity.ActivityEditor.PlaybackState
import com.qfs.pagan.AudioInterface
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PlaybackDevice
import com.qfs.pagan.PlaybackFrameMap
import com.qfs.pagan.projectmanager.ProjectManager
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import java.io.DataOutputStream
import java.io.File

class ViewModelEditor: ViewModel() {
    companion object {
        fun get_next_playback_state(input_state: PlaybackState, next_state: PlaybackState): PlaybackState? {
            return when (input_state) {
                PlaybackState.NotReady -> {
                    when (next_state) {
                        PlaybackState.NotReady,
                        PlaybackState.Ready -> next_state
                        else -> null
                    }
                }
                PlaybackState.Ready -> {
                    when (next_state) {
                        PlaybackState.NotReady,
                        PlaybackState.Ready,
                        PlaybackState.Queued -> next_state
                        else -> null
                    }
                }
                PlaybackState.Playing -> {
                    when (next_state) {
                        PlaybackState.Ready,
                        PlaybackState.Stopping -> next_state
                        else -> null
                    }
                }
                PlaybackState.Queued -> {
                    when (next_state) {
                        PlaybackState.Ready,
                        PlaybackState.Playing -> next_state
                        else -> null
                    }
                }
                PlaybackState.Stopping -> {
                    when (next_state) {
                        PlaybackState.Ready -> next_state
                        else -> null
                    }
                }
            }
        }
    }

    var export_handle: WavConverter? = null
    var action_interface = ActionTracker()
    var opus_manager = OpusLayerInterface()
    var active_project: Uri? = null
    var audio_interface = AudioInterface()
    var available_preset_names: HashMap<Pair<Int, Int>, String>? = null
    var project_manager: ProjectManager? = null

    var active_midi_device: MidiDeviceInfo? = null
    var playback_device: PlaybackDevice? = null
    var playback_state_soundfont: ActivityEditor.PlaybackState = ActivityEditor.PlaybackState.NotReady
    var playback_state_midi: ActivityEditor.PlaybackState = ActivityEditor.PlaybackState.NotReady
    var active_dialog: MutableState<@Composable (() -> Unit)?> =  mutableStateOf(null)

    fun export_wav(
        opus_manager: OpusLayerBase,
        sample_handle_manager: SampleHandleManager,
        target_output_stream: DataOutputStream,
        tmp_file: File, configuration: PaganConfiguration? = null,
        handler: WavConverter.ExporterEventHandler,
        ignore_global_effects: Boolean = false,
        ignore_channel_effects: Boolean = false,
        ignore_line_effects: Boolean = false,
    ) {
        val frame_map = PlaybackFrameMap(opus_manager, sample_handle_manager)
        frame_map.clip_same_line_release = configuration?.clip_same_line_release != false
        frame_map.parse_opus(
            ignore_global_effects,
            ignore_channel_effects,
            ignore_line_effects
        )

        val start_frame = frame_map.get_marked_frames()[0]

        // Prebuild the first buffer's worth of sample handles, the rest happen in the get_new_handles()
        for (i in start_frame .. start_frame + sample_handle_manager.buffer_size) {
            frame_map.check_frame(i)
        }

        this.export_handle = WavConverter(sample_handle_manager)
        this.export_handle?.export_wav(frame_map, target_output_stream, tmp_file, handler)
        this.export_handle = null
    }

    fun cancel_export() {
        val handle = this.export_handle ?: return
        handle.cancel_flagged = true
    }

    fun is_exporting(): Boolean {
        return this.export_handle != null
    }

    fun unset_soundfont() {
        this.opus_manager.ui_facade.clear_instrument_names()
        this.audio_interface.unset_soundfont()
        this.available_preset_names = null
        this.destroy_playback_device()
    }

    fun populate_active_percussion_names(channel_index: Int) {
        val midi_channel = this.opus_manager.get_midi_channel(channel_index)
        val instrument_options = this.audio_interface.get_instrument_options(midi_channel)
        this.opus_manager.ui_facade.set_instrument_names(channel_index, instrument_options)
    }

    fun set_soundfont(soundfont: SoundFont) {
        this.opus_manager.ui_facade.clear_instrument_names()
        this.audio_interface.set_soundfont(soundfont)
        this.available_preset_names = HashMap()
        for ((name, program, bank) in soundfont.get_available_presets()) {
            this.available_preset_names?.set(Pair(bank, program), name)
        }

        this.create_playback_device()
    }

    fun set_sample_rate(new_rate: Int) {
        this.audio_interface.set_sample_rate(new_rate)
        this.create_playback_device()
    }

    fun create_playback_device() {
        this.playback_device = PlaybackDevice(
            this.opus_manager,
            this.audio_interface.playback_sample_handle_manager!!,
            WaveGenerator.StereoMode.Stereo
        )
    }
    fun destroy_playback_device() {
        this.playback_device?.kill()
        this.playback_device = null
    }
    fun in_playback(): Boolean {
        return this.playback_state_midi == ActivityEditor.PlaybackState.Playing || this.playback_state_soundfont == ActivityEditor.PlaybackState.Playing
    }

    fun update_playback_state_soundfont(next_state: PlaybackState): Boolean {
        this.playback_state_soundfont = ViewModelEditor.get_next_playback_state(this.playback_state_soundfont, next_state) ?: return false
        return true
    }

    fun update_playback_state_midi(next_state: PlaybackState): Boolean {
        this.playback_state_midi = ViewModelEditor.get_next_playback_state(this.playback_state_midi, next_state) ?: return false
        return true
    }

    fun save_project(indent: Boolean = false) {
        this.active_project = this.project_manager?.save(this.opus_manager, this.active_project, indent)
        this.opus_manager.ui_facade.set_project_exists(true)
    }
}