/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import android.app.Activity
import android.view.WindowManager
import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WaveGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.math.max
import com.qfs.pagan.OpusLayerInterface as OpusManager

class PlaybackDevice(
    val opus_manager: OpusManager,
    sample_handle_manager: SampleHandleManager,
    stereo_mode: WaveGenerator.StereoMode = WaveGenerator.StereoMode.Stereo
): MappedPlaybackDevice(
    PlaybackFrameMap(opus_manager, sample_handle_manager),
    sample_handle_manager.sample_rate,
    sample_handle_manager.buffer_size,
    stereo_mode = stereo_mode
) {
    private var _first_beat_passed = false
    private var _buffering_cancelled = false
    private var _buffering_mutex = Mutex()
    var activity: Activity? = null
    /*
        All of this notification stuff is used with the understanding that the PaganPlaybackDevice
        used to export wavs will be discarded after a single use. It'll need to be cleaned up to
        handle anything more.
     */
    override fun on_buffer() {
        val vm_state = this.opus_manager.vm_state
        vm_state.set_is_buffering(true)
    }

    override fun on_buffer_done() {
        val vm_state = this.opus_manager.vm_state
        vm_state.set_is_buffering(false)
    }

    fun restore_playback_state() {
        val vm_controller = this.opus_manager.vm_controller
        vm_controller.update_playback_state_soundfont(PlaybackState.Ready)
        (this.sample_frame_map as PlaybackFrameMap).clear()

        this.activity?.let {
            it.runOnUiThread {
                it.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun on_stop() {
        this.restore_playback_state()
    }

    override fun on_start() {
        val vm_controller = this.opus_manager.vm_controller
        vm_controller.update_playback_state_soundfont(PlaybackState.Playing)


        this.activity?.let {
            it.runOnUiThread {
                it.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun on_mark(i: Int) {
        if (!this.is_playing || this.play_cancelled) return

        // used to hide the loading reticle at on_start, but first beat prevents
        // hiding it, then [potentially] waiting to buffer
        if (! this._first_beat_passed) {
            //this.activity?.let { activity ->
            //    activity.runOnUiThread {
            //        activity.loading_reticle_hide()
            //        activity.clear_forced_title()
            //        activity.set_playback_button(if ((this.sample_frame_map as PlaybackFrameMap).is_looping) R.drawable.icon_pause_loop else R.drawable.icon_pause)
            //    }
            //}
            this._first_beat_passed = true
        }


        if (!(this.sample_frame_map as PlaybackFrameMap).is_looping && i >= this.opus_manager.length) {
            this.kill()
            return
        }

        this.opus_manager.cursor_select_column(max(i % this.opus_manager.length, 0))
    }

    override fun on_cancelled() {
        this.restore_playback_state()
    }

    fun play_opus(start_beat: Int, play_in_loop: Boolean = false) {
        val vm_controller = this.opus_manager.vm_controller
        vm_controller.update_playback_state_soundfont(PlaybackState.Queued)

        this._first_beat_passed = false
        val sample_frame_map = this.sample_frame_map as PlaybackFrameMap

        sample_frame_map.is_looping = play_in_loop
        sample_frame_map.parse_opus()
        val start_frame = sample_frame_map.get_marked_frame(start_beat)!!

        // Prebuild the first buffer's worth of sample handles, the rest happen in the get_new_handles()
        for (i in start_frame .. start_frame + this.buffer_size) {
            sample_frame_map.check_frame(i)
        }

        if (play_in_loop && start_frame != 0) {
            sample_frame_map.shift_before_frame(start_frame)
        }
        this.play(start_frame)
    }

    fun set_clip_same_line_release(value: Boolean) {
        (this.sample_frame_map as PlaybackFrameMap).clip_same_line_release = value
    }
}
