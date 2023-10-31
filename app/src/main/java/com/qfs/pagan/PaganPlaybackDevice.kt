package com.qfs.pagan

import com.qfs.apres.soundfontplayer.CachedMidiAudioPlayer
import com.qfs.apres.soundfontplayer.SampleHandleManager

class PaganPlaybackDevice(var activity: MainActivity): CachedMidiAudioPlayer(SampleHandleManager(activity.get_soundfont()!!, activity.configuration.sample_rate)) {
    override fun on_stop() {
        this.activity.runOnUiThread {
            this.activity.playback_stop()
        }
    }
    override fun on_beat_signal(beat: Int) {
        this.activity.runOnUiThread {
            this.activity.get_opus_manager().cursor_select_column(beat, true)
        }
    }
}