package com.qfs.apres

import com.qfs.apres.event.MIDIEvent

abstract class VirtualMidiInputDevice {
    private var midi_controller: MidiController? = null
    fun set_midi_controller(midi_controller: MidiController) {
        this.midi_controller = midi_controller
    }

    fun unset_midi_controller() {
        this.midi_controller = null
    }

    fun is_connected(): Boolean {
        return this.midi_controller != null
    }

    fun send_event(event: MIDIEvent) {
        // TODO: Throw error?
        if (is_connected()) {
            this.midi_controller!!.broadcast_event(event)
        }
    }
}
