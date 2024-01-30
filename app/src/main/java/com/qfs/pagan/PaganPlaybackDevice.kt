package com.qfs.pagan

import com.qfs.apres.soundfontplayer.FiniteMidiDevice
import com.qfs.apres.soundfontplayer.SampleHandleManager
class PaganPlaybackDevice(var activity: MainActivity, sample_rate: Int = activity.configuration.sample_rate): FiniteMidiDevice(SampleHandleManager(activity.get_soundfont()!!, sample_rate, buffer_size = sample_rate)) {
    /*
        All of this notification stuff is used with the understanding that the PaganPlaybackDevice
        used to export wavs will be discarded after a single use. It'll need to be cleaned up to
        handle anything more.
     */
    var start_beat = 0
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

    override fun on_beat(x: Int) {
        if (!this.is_playing || this.play_cancelled) {
            return
        }
        val i = x + this.start_beat
        val opus_manager = this.activity.get_opus_manager()
        if (i >= opus_manager.beat_count) {
            return
        }
        opus_manager.cursor_select_column(i)

        // Force scroll here, cursor_select_column doesn't scroll if the column is already visible
        this.activity.runOnUiThread {
            val editor_table = this.activity?.findViewById<EditorTable>(R.id.etEditorTable)
            if (editor_table != null) {
                editor_table.scroll_to_position(x = i, force = true)
            }
        }
    }

    override fun on_cancelled() {
        this.activity.restore_playback_state()
    }

    fun play_opus(start_beat: Int) {
        val midi = this.activity.get_opus_manager().get_midi(start_beat)
        this.start_beat = start_beat
        this.play_midi(midi)
    }
}