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

enum class PlaybackState {

    NotReady,
    Ready,
    Playing,
    Queued,
    Stopping
}
fun get_next_playback_state(input_state: PlaybackState, next_state: PlaybackState): PlaybackState? {
    return when (input_state) {
        PlaybackState.NotReady -> {
            when (next_state) {
                PlaybackState.NotReady,
                PlaybackState.Ready -> next_state
                else -> null
            }
        }
        PlaybackState.Ready -> {
            when (next_state) {
                PlaybackState.NotReady,
                PlaybackState.Ready,
                PlaybackState.Queued -> next_state
                else -> null
            }
        }
        PlaybackState.Playing -> {
            when (next_state) {
                PlaybackState.Ready,
                PlaybackState.Stopping -> next_state
                else -> null
            }
        }
        PlaybackState.Queued -> {
            when (next_state) {
                PlaybackState.Ready,
                PlaybackState.Playing -> next_state
                else -> null
            }
        }
        PlaybackState.Stopping -> {
            when (next_state) {
                PlaybackState.Ready -> next_state
                else -> null
            }
        }
    }
}
