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

import com.qfs.pagan.structure.rationaltree.ReducibleTree

class OpusLinePercussion(var instrument: Int, beats: MutableList<ReducibleTree<PercussionEvent>>): OpusLineAbstract<PercussionEvent>(beats){
    constructor(instrument: Int, beat_count: Int) : this(instrument, Array<ReducibleTree<PercussionEvent>>(beat_count) { ReducibleTree() }.toMutableList())

    override fun equals(other: Any?): Boolean {
        return other is OpusLinePercussion
                && other.instrument == this.instrument
                &&super.equals(other)
    }
}
