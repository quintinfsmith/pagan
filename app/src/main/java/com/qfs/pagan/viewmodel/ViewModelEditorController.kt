package com.qfs.pagan.viewmodel

import android.media.midi.MidiDeviceInfo
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.apres.MidiPlayer
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.AudioInterface
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PlaybackDevice
import com.qfs.pagan.PlaybackFrameMap
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.enumerate
import com.qfs.pagan.get_next_playback_state
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import java.io.DataOutputStream
import java.io.File

class ViewModelEditorController(): ViewModel() {
    var action_interface = ActionTracker(this)
    var opus_manager = OpusLayerInterface(this)
    var active_midi_device: MidiDeviceInfo? = null
    var virtual_midi_device = MidiPlayer()
    var midi_devices_connected: Int = 0
    var audio_interface = AudioInterface()
    var playback_device: PlaybackDevice? = null
    var playback_state_soundfont: PlaybackState = PlaybackState.NotReady
    var playback_state_midi: PlaybackState = PlaybackState.NotReady
    var move_mode: MutableState<PaganConfiguration.MoveMode> = mutableStateOf(PaganConfiguration.MoveMode.COPY)
    var export_handle: WavConverter? = null
    var active_project: Uri? = null
    var project_exists: MutableState<Boolean> = mutableStateOf(false)
    var active_soundfont_relative_path: String? = null

    fun update_channel_preset(channel: Int, bank: Int, program: Int) {
        this.audio_interface.update_channel_preset(channel, bank, program)
        this.virtual_midi_device.send_event(BankSelect(channel, bank))
        this.virtual_midi_device.send_event(ProgramChange(channel, program))
    }

    fun set_active_midi_device(device: MidiDeviceInfo?) {
        this.active_midi_device = device
        this.update_playback_state_midi(PlaybackState.Ready)
        this.update_midi_instruments()
    }

    fun set_project_exists(value: Boolean) {
        this.project_exists.value = value
    }

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
        this.audio_interface.unset_soundfont()
        this.destroy_playback_device()
        this.opus_manager.vm_state.populate_presets()
        this.opus_manager.vm_state.update_channel_names()
        this.opus_manager.vm_state.available_instruments.clear()
        this.active_soundfont_relative_path = null
    }


    fun get_soundfont(): SoundFont? {
        return this.audio_interface.soundfont
    }

    fun set_soundfont(soundfont: SoundFont) {
        this.audio_interface.set_soundfont(soundfont)
        this.create_playback_device()

        val vm_state = this.opus_manager.vm_state
        vm_state.populate_presets(soundfont)
        vm_state.update_channel_names()
    }

    fun create_playback_device() {
        this.playback_device = PlaybackDevice(
            this.opus_manager,
            this.audio_interface.playback_sample_handle_manager!!,
            WaveGenerator.StereoMode.Stereo
        )
        this.update_soundfont_instruments()
    }

    fun destroy_playback_device() {
        this.playback_device?.kill()
        this.playback_device = null
    }

    fun in_playback(): Boolean {
        return this.playback_state_midi == PlaybackState.Playing || this.playback_state_soundfont == PlaybackState.Playing
    }

    fun update_playback_state_soundfont(next_state: PlaybackState): Boolean {
        this.playback_state_soundfont = get_next_playback_state(this.playback_state_soundfont, next_state) ?: return false
        this.opus_manager.vm_state.playback_state_soundfont.value = this.playback_state_soundfont
        return true
    }

    fun update_playback_state_midi(next_state: PlaybackState): Boolean {
        this.playback_state_midi = get_next_playback_state(this.playback_state_midi, next_state) ?: return false
        return true
    }

    fun set_move_mode(move_mode: PaganConfiguration.MoveMode) {
        this.move_mode.value = move_mode
    }

    fun attach_state_model(model: ViewModelEditorState) {
        this.opus_manager.attach_state_model(model)
    }

    fun update_soundfont_instruments() {
        for ((i, channel) in this.opus_manager.channels.enumerate()) {
            val midi_channel = this.opus_manager.get_midi_channel(i)
            val (midi_bank, midi_program) = channel.get_preset()
            this.audio_interface.update_channel_preset(midi_channel, midi_bank, midi_program)
        }
    }

    fun update_midi_instruments() {
        for ((i, channel) in this.opus_manager.channels.enumerate()) {
            val midi_channel = this.opus_manager.get_midi_channel(i)
            val (midi_bank, midi_program) = channel.get_preset()
            this.virtual_midi_device.send_event(BankSelect(midi_channel, midi_bank))
            this.virtual_midi_device.send_event(ProgramChange(midi_channel, midi_program))
        }
    }

}