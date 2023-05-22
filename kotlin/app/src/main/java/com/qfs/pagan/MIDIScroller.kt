package com.qfs.pagan

import android.widget.SeekBar
import com.qfs.pagan.apres.SongPositionPointer
import com.qfs.pagan.apres.SoundFontPlayer.AudioTrackHandle
import com.qfs.pagan.apres.VirtualMIDIDevice

class MIDIScroller(var main_activity: MainActivity, var seekbar: SeekBar): VirtualMIDIDevice() {
    override fun onSongPositionPointer(event: SongPositionPointer) {
        this.main_activity.runOnUiThread {
            //Thread.sleep((AudioTrackHandle.base_delay_in_frames * 4000 / AudioTrackHandle.sample_rate).toLong())
            this.seekbar.progress = event.beat
        }
        //this.activity.scroll_to_beat(event.beat, true)
    }
}

