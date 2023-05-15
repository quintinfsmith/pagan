package com.qfs.pagan

import android.widget.SeekBar
import com.qfs.pagan.apres.SongPositionPointer
import com.qfs.pagan.apres.VirtualMIDIDevice

class MIDIScroller(var main_activity: MainActivity, var seekbar: SeekBar): VirtualMIDIDevice() {
    override fun onSongPositionPointer(event: SongPositionPointer) {
        this.main_activity.runOnUiThread {
            this.seekbar.progress = event.beat
        }
        //this.activity.scroll_to_beat(event.beat, true)
    }
}

