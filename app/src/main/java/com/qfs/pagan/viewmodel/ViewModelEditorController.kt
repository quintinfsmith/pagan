/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.viewmodel

import android.media.midi.MidiDeviceInfo
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.apres.MidiPlayer
import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.AudioInterface
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PlaybackDevice
import com.qfs.pagan.PlaybackFrameMap
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.PresetKey
import com.qfs.pagan.enumerate
import com.qfs.pagan.get_next_playback_state
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import java.io.DataOutputStream
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class ViewModelEditorController(): ViewModel() {
    var opus_manager = OpusLayerInterface(this)
    var active_midi_device: MidiDeviceInfo? = null
    var virtual_midi_device = MidiPlayer()
    var midi_devices_connected: Int = 0
    var audio_interface = AudioInterface()
    var playback_device: PlaybackDevice? = null
    var playback_state_soundfont: PlaybackState = PlaybackState.NotReady
    var playback_state_midi: PlaybackState = PlaybackState.NotReady
    var export_handle: WavConverter? = null
    var active_project: Uri? = null
    var project_exists: MutableState<Boolean> = mutableStateOf(false)
    var active_soundfont_relative_paths: List<String> = listOf()


    fun update_channel_preset(channel: Int, soundfont_index: Int, bank: Int, program: Int) {
        this.audio_interface.update_channel_preset(channel, soundfont_index, bank, program)
        this.virtual_midi_device.send_event(BankSelect(channel, bank))
        this.virtual_midi_device.send_event(ProgramChange(channel, program))
    }

    fun set_active_midi_device(device: MidiDeviceInfo?) {
        this.playback_device?.kill()
        this.virtual_midi_device.stop()

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
        this.audio_interface.unset_soundfonts()
        this.destroy_playback_device()
        this.opus_manager.vm_state.clear_presets()
        this.opus_manager.vm_state.update_channel_names()
        this.active_soundfont_relative_paths = listOf()
    }

    fun set_soundfonts(vararg soundfonts: SoundFont) {
        this.audio_interface.unset_soundfonts()
        this.audio_interface.add_soundfont(*soundfonts)
        this.create_playback_device()

        val vm_state = this.opus_manager.vm_state

        vm_state.preset_names.clear()
        for ((i, soundfont) in soundfonts.enumerate()) {
            vm_state.populate_presets(i, soundfont)
        }
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

    fun set_sample_rate(new_rate: Int) {
        this.audio_interface.set_sample_rate(new_rate)
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

    fun attach_state_model(model: ViewModelEditorState) {
        this.opus_manager.attach_state_model(model)
    }

    fun update_soundfont_instruments() {
        for ((i, channel) in this.opus_manager.channels.enumerate()) {
            val midi_channel = this.opus_manager.get_midi_channel(i)
            val (soundfont_index, midi_bank, midi_program) = channel.get_preset()
            this.audio_interface.update_channel_preset(midi_channel, soundfont_index, midi_bank, midi_program)
        }
    }

    fun update_midi_instruments() {
        for ((i, channel) in this.opus_manager.channels.enumerate()) {
            val midi_channel = this.opus_manager.get_midi_channel(i)
            val (soundfont_index, midi_bank, midi_program) = channel.get_preset()
            this.virtual_midi_device.send_event(BankSelect(midi_channel, midi_bank))
            this.virtual_midi_device.send_event(ProgramChange(midi_channel, midi_program))
        }
    }

    fun stop_opus_midi() {
        this.update_playback_state_midi(PlaybackState.Stopping)
        this.virtual_midi_device.stop()
        this.update_playback_state_midi(PlaybackState.Ready)
        this.opus_manager.vm_state.playback_state_midi.value = this.playback_state_midi
    }

    fun play_events(preset: PresetKey, is_percussion: Boolean, event_values: List<Triple<Int, Int, Float>>) {
        if (this.in_playback()) return // disable feedback during playback
        if (!this.opus_manager.vm_state.soundfont_ready.value) return // TODO this check should be somewhere else
        if (this.active_midi_device != null || !this.audio_interface.has_soundfont()) return

        val events = List<AudioInterface.FeedbackRevolver.Event>(event_values.size) { i ->
            val (event_value, duration, f_vel) = event_values[i]
            val (note, bend) = if (is_percussion) {
                Pair(event_value + 27, 0)
            } else {
                this.opus_manager.calculate_note_bend(0, event_value)
            }

            AudioInterface.FeedbackRevolver.Event(
                channel = 0,
                note = max(0, min(127, note)),
                bend = bend,
                velocity = (f_vel * 127F).toInt() shl 8,
                duration = duration
            )
        }

        this.audio_interface.play_feedback(events, preset)
    }
    fun play_events(channel: Int, event_values: List<Triple<Int, Int, Float>>) {
        if (this.in_playback()) return // disable feedback during playback
        if (!this.opus_manager.vm_state.soundfont_ready.value) return // TODO this check should be somewhere else

        val midi_channel = this.opus_manager.get_midi_channel(channel)
        if (this.active_midi_device != null) {
            try {
                thread {
                    for ((event_value, duration, f_vel) in event_values) {
                        val (note, bend) = if (this.opus_manager.is_percussion(channel)) {
                            Pair(event_value + 27, 0)
                        } else {
                            this.opus_manager.calculate_note_bend(channel, event_value)
                        }
                        this.virtual_midi_device.play_note(
                            midi_channel,
                            note,
                            bend,
                            duration.toLong(),
                            (f_vel * 127F).toInt(),
                            !opus_manager.is_tuning_standard()
                        )
                        Thread.sleep(duration.toLong())
                    }
                }
            } catch (_: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But I'm sure that'll come back to bite me
            }
        } else if (this.audio_interface.has_soundfont()) {
            val events = List<AudioInterface.FeedbackRevolver.Event>(event_values.size) { i ->
                val (event_value, duration, f_vel) = event_values[i]
                val (note, bend) = if (this.opus_manager.is_percussion(channel)) {
                    Pair(event_value + 27, 0)
                } else {
                    this.opus_manager.calculate_note_bend(channel, event_value)
                }

                AudioInterface.FeedbackRevolver.Event(
                    channel = midi_channel,
                    note = max(0, min(127, note)),
                    bend = bend,
                    velocity = (f_vel * 127F).toInt() shl 8,
                    duration = duration
                )
            }

            //this.audio_interface.play_feedback(midi_channel, note, bend, (velocity * 127F).toInt() shl 8)
            this.audio_interface.play_feedback(events)
        }
    }

    fun play_event(preset: PresetKey, is_percussion: Boolean, event_value: Int, velocity: Float = .5f) {
        if (event_value < 0) return // No sound to play
        if (this.in_playback()) return // disable feedback during playback
        if (this.active_midi_device != null || !this.audio_interface.has_soundfont()) return

        if (!this.opus_manager.vm_state.soundfont_ready.value) return // TODO this check should be somewhere else

        val (note, bend) = if (is_percussion) {
            Pair(event_value + 27, 0)
        } else {
            this.opus_manager.calculate_note_bend(0, event_value)
        }

        if (note > 127) return

        this.audio_interface.play_feedback(preset, note, bend, (velocity * 127F).toInt() shl 8)
    }

    fun play_event(channel: Int, event_value: Int, velocity: Float = .5F) {
        if (event_value < 0) return // No sound to play
        if (this.in_playback()) return // disable feedback during playback

        val (note, bend) = if (this.opus_manager.is_percussion(channel)) {
            Pair(event_value + 27, 0)
        } else {
            this.opus_manager.calculate_note_bend(channel, event_value)
        }

        if (note > 127) return

        val midi_channel = this.opus_manager.get_midi_channel(channel)
        if (this.active_midi_device != null) {
            try {
                this.virtual_midi_device.play_note(
                    midi_channel,
                    note,
                    bend,
                    400L,
                    (velocity * 127F).toInt(),
                    !opus_manager.is_tuning_standard()
                )
            } catch (_: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But I'm sure that'll come back to bite me
            }
        } else if (this.audio_interface.has_soundfont()) {
            this.audio_interface.play_feedback(midi_channel, note, bend, (velocity * 127F).toInt() shl 8)
        }
    }
}