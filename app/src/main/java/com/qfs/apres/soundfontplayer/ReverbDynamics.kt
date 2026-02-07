/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres.soundfontplayer

data class ReverbDynamics(
    var room_size: Float = 10f,
    var decay: Float = .2f
) {
    companion object {
        const val SPEED_OF_SOUND: Float = 343f
    }

    var delay = (this.room_size * 2f) / ReverbDynamics.SPEED_OF_SOUND
    var bounces = (this.decay / this.delay).toInt()
    var factor = this.delay  / this.decay
}