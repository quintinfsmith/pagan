package com.qfs.apres.soundfontplayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.max

class AudioTrackHandle {
    companion object {
        const val sample_rate = 22050
        //const val sample_rate = 44100

        val buffer_size = max(
            sample_rate / 8, // 1/8 seconds. arbitrary but feels good enough
            AudioTrack.getMinBufferSize(
                sample_rate,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioFormat.CHANNEL_OUT_STEREO
            ) * 4
        )
        val buffer_size_in_bytes: Int = buffer_size * 4
    }

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
        .setBufferSizeInBytes(buffer_size_in_bytes)
        .build()

    fun pause() {
        this.audio_track.pause()
    }

    fun play() {
        this.audio_track.play()
    }
    fun write(shorts: ShortArray) {
        if (this.audio_track.state != AudioTrack.STATE_UNINITIALIZED) {
            try {
                this.audio_track.write(
                    shorts,
                    0,
                    shorts.size,
                    AudioTrack.WRITE_BLOCKING
                )
                this.audio_track.flush()
            } catch (e: IllegalStateException) {
                // Shouldn't need to do anything. the audio track was released and this should stop on its own
            }
        }
    }
    fun stop() {
        try {
            this.audio_track.stop()
            this.audio_track.release()
        } catch (e: IllegalStateException) {
            Log.w("AudioTrackHandle", "Attempted to stop stopped track")
        }

    }
}
