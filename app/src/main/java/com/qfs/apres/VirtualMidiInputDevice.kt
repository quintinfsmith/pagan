/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres

import com.qfs.apres.event.GeneralMIDIEvent

abstract class VirtualMidiInputDevice {
    class DisconnectedException: Exception()
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

    fun send_event(event: GeneralMIDIEvent) {
        if (!this.is_connected()) throw DisconnectedException()

        this.midi_controller!!.broadcast_event(event)
    }
}
