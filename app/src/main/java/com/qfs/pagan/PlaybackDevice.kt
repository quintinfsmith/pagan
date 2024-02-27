package com.qfs.pagan

import com.qfs.apres.soundfontplayer.MappedPlaybackDevice
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlin.math.max

class PlaybackDevice(var activity: MainActivity, sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(
    PlaybackFrameMap(activity.get_opus_manager(), sample_handle_manager),
    sample_handle_manager.sample_rate,
    sample_handle_manager.buffer_size
) {
    /*
        All of this notification stuff is used with the understanding that the PaganPlaybackDevice
        used to export wavs will be discarded after a single use. It'll need to be cleaned up to
        handle anything more.
     */
    override fun on_buffer() {
        super.on_buffer()
        this.activity.runOnUiThread {
            this.activity.loading_reticle_show("BUFFERING...")
        }
    }

    override fun on_buffer_done() {
        super.on_buffer_done()
        this.activity.runOnUiThread {
            this.activity.loading_reticle_hide()
        }
    }

    override fun on_stop() {
        this.activity.restore_playback_state()
    }

    override fun on_start() {
        this.activity.update_playback_state_soundfont(MainActivity.PlaybackState.Playing)
        this.activity.runOnUiThread {
            this.activity.loading_reticle_hide()
            this.activity.set_playback_button(R.drawable.ic_baseline_pause_24)
        }
    }

    override fun on_beat(i: Int) {
        if (!this.is_playing || this.play_cancelled) {
            return
        }

        val opus_manager = this.activity.get_opus_manager()
        if (i >= opus_manager.beat_count) {
            return
        }

        opus_manager.cursor_select_column(max(i, 0))

        // Force scroll here, cursor_select_column doesn't scroll if the column is already visible
        this.activity.runOnUiThread {
            val editor_table = this.activity.findViewById<EditorTable?>(R.id.etEditorTable)
            editor_table?.scroll_to_position(x = max(i, 0), force = true)
        }
    }

    override fun on_cancelled() {
        this.activity.restore_playback_state()
    }

    fun play_opus(start_beat: Int) {
        (this.sample_frame_map as PlaybackFrameMap).parse_opus(true)
        val start_frame = this.sample_frame_map.get_beat_frames()[start_beat]?.first ?: 0

        // Prebuild the first buffer's worth of sample handles, the rest happen in the get_new_handles()
        for (i in start_frame .. start_frame + buffer_size) {
            (this.sample_frame_map as PlaybackFrameMap).check_frame(i)
        }

        this.play(start_frame)
    }
}