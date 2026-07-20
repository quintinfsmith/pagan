/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

object Values {
    const val ExportSampleRate = 44100
    const val ExportBufferSize = 22050

    object Defaults {
        const val Radix = 12
        const val ZoomSensitivity = .8F
        const val SlideDenominator = 4
        const val VolumeCurve = 1.2F
    }

    object DialogInput {
        const val Split = 2
        const val InsertLeaf = 1
        const val InsertLine = 1
        const val RemoveLine = 1
        const val InsertBeat = 1
        const val RemoveBeat = 1
        const val Duration = 1
        object Max {
            const val Split = 128
            const val InsertLeaf = 127
            const val InsertLine = 16
            const val InsertBeat = 1024
        }
        object Min {
            const val InsertLeaf = 1
            const val InsertLine = 1
            const val Split = 2
            const val Duration = 1
        }
    }

    const val OctaveCount = 8
    const val OffsetModulo = 12
    const val MaximumOctaveSize = 36
    const val MinimumOctaveSize = 1

    const val LowPassMinimum = 0F
    const val LowPassMaximum = 20000F
    const val LowPassDefault = 880F

    const val TempoMinimum = 1F
    const val TempoMaximum = 1024F

    const val DisabledTopBarIconAlpha = .3F
}

