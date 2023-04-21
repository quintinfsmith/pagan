package com.qfs.radixulous

import android.widget.SeekBar
import android.widget.TextView
import com.qfs.radixulous.apres.SongPositionPointer
import com.qfs.radixulous.apres.VirtualMIDIDevice

class MIDIScroller(var main_activity: MainActivity, var seekbar: SeekBar): VirtualMIDIDevice() {
    override fun onSongPositionPointer(event: SongPositionPointer) {
        this.main_activity.runOnUiThread {
            this.seekbar.progress = event.beat
        }
        //this.activity.scroll_to_beat(event.beat, true)
    }
}

