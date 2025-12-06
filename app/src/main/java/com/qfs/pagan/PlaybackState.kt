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
