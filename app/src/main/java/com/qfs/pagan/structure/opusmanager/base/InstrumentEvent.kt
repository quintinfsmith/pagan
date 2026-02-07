/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.base

abstract class InstrumentEvent(duration: Int = 1): OpusEvent(duration) {
    abstract override fun copy(): InstrumentEvent
}

abstract class TunedInstrumentEvent(duration: Int): InstrumentEvent(duration)

class AbsoluteNoteEvent(var note: Int, duration: Int = 1): TunedInstrumentEvent(duration) {
    override fun copy(): AbsoluteNoteEvent {
        val output = AbsoluteNoteEvent(this.note)
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsoluteNoteEvent && this.note == other.note && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.note)
    }
}

class RelativeNoteEvent(var offset: Int, duration: Int = 1): TunedInstrumentEvent(duration) {
    override fun copy(): RelativeNoteEvent {
        val output = RelativeNoteEvent(this.offset)
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is RelativeNoteEvent && this.offset == other.offset && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.offset)
    }
}

class PercussionEvent(duration: Int = 1): InstrumentEvent(duration) {
    override fun hashCode(): Int {
        return (super.hashCode() shl 1) + 1
    }

    override fun copy(): PercussionEvent {
        val output = PercussionEvent()
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is PercussionEvent && super.equals(other)
    }
}
