package com.qfs.apres.soundfontplayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
class AudioTrackHandle(sample_rate: Int, buffer_size: Int) {
    private var audio_track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sample_rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
        )
        .setBufferSizeInBytes(buffer_size * 4)
        .build()

    fun pause() {
        this.audio_track.pause()
    }

    fun offset_next_notification_position(next: Int) {
        try {
            this.audio_track.notificationMarkerPosition =
                next + (this.audio_track.notificationMarkerPosition ?: 0)
        } catch (e: IllegalStateException) {
            // pass
        }
    }

    fun play(update_listener: AudioTrack.OnPlaybackPositionUpdateListener? = null) {
        if (update_listener != null) {
            this.audio_track.setPlaybackPositionUpdateListener(update_listener)
        }
        this.audio_track.play()
    }

    fun write(shorts: ShortArray) {
        try {
            this.audio_track.write(
                shorts,
                0,
                shorts.size,
                AudioTrack.WRITE_BLOCKING
            )
        } catch (e: IllegalStateException) {
            // Shouldn't need to do anything. the audio track was released and this should stop on its own
        }
    }
    fun stop() {
        try {
            this.audio_track.stop()
            this.audio_track.flush()
            this.audio_track.release()
        } catch (e: IllegalStateException) {
            Log.w("AudioTrackHandle", "Attempted to stop stopped track")
        }

    }
}
