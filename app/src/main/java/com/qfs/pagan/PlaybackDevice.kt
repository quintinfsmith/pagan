package com.qfs.pagan

import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.Activity.ActivityEditor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class PlaybackDevice(var activity: ActivityEditor, sample_handle_manager: SampleHandleManager, stereo_mode: WaveGenerator.StereoMode = WaveGenerator.StereoMode.Stereo): MappedPlaybackDevice(
    PlaybackFrameMap(activity.get_opus_manager(), sample_handle_manager),
    sample_handle_manager.sample_rate,
    sample_handle_manager.buffer_size,
    stereo_mode = stereo_mode
) {
    private var _first_beat_passed = false
    private var _buffering_cancelled = false
    private var _buffering_mutex = Mutex()
    /*
        All of this notification stuff is used with the understanding that the PaganPlaybackDevice
        used to export wavs will be discarded after a single use. It'll need to be cleaned up to
        handle anything more.
     */
    override fun on_buffer() {
        this.activity.runOnUiThread {
            Thread.sleep(200)
            val cancelled = runBlocking {
                this@PlaybackDevice._buffering_mutex.withLock {
                    this@PlaybackDevice._buffering_cancelled
                }
            }

            if (!cancelled) {
                this.activity.loading_reticle_show()
                this.activity.force_title_text(this.activity.getString(R.string.title_msg_buffering))
            } else {
                runBlocking {
                    this@PlaybackDevice._buffering_mutex.withLock {
                        this@PlaybackDevice._buffering_cancelled = false
                    }
                }
            }
        }
    }

    override fun on_buffer_done() {
        runBlocking {
            this@PlaybackDevice._buffering_mutex.withLock {
                this@PlaybackDevice._buffering_cancelled = true
            }
        }
        this.activity.runOnUiThread {
            this.activity.loading_reticle_hide()
        }
    }

    override fun on_stop() {
        this.activity.restore_playback_state()
        (this.sample_frame_map as PlaybackFrameMap).clear()
    }

    override fun on_start() {
        this.activity.update_playback_state_soundfont(ActivityEditor.PlaybackState.Playing)
    }

    override fun on_mark(i: Int) {
        if (!this.is_playing || this.play_cancelled) return

        // used to hide the loading reticle at on_start, but first beat prevents
        // hiding it, then [potentially] waiting to buffer
        if (! this._first_beat_passed) {
            this.activity.runOnUiThread {
                this.activity.loading_reticle_hide()
                this.activity.clear_forced_title()
                this.activity.set_playback_button(if ((this.sample_frame_map as PlaybackFrameMap).is_looping) R.drawable.icon_pause_loop else R.drawable.icon_pause)
            }
            this._first_beat_passed = true
        }

        val opus_manager = this.activity.get_opus_manager()

        if (!(this.sample_frame_map as PlaybackFrameMap).is_looping && i >= opus_manager.length) {
            this.kill()
            return
        }

        opus_manager.cursor_select_column(max(i % opus_manager.length, 0))
    }

    override fun on_cancelled() {
        this.activity.restore_playback_state()
        (this.sample_frame_map as PlaybackFrameMap).clear()
    }

    fun play_opus(start_beat: Int, play_in_loop: Boolean = false) {
        this._first_beat_passed = false
        val sample_frame_map = this.sample_frame_map as PlaybackFrameMap
        sample_frame_map.clip_same_line_release = this.activity.configuration.clip_same_line_release
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
}
