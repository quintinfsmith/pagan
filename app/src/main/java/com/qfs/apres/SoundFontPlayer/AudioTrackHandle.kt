package com.qfs.apres.SoundFontPlayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.max

class AudioTrackHandle {
    class HandleStoppedException() : Exception()
    companion object {
        const val sample_rate = 22010

        val buffer_size = max(
            sample_rate / 5, // .2 seconds. arbitrary but feels good enough
            AudioTrack.getMinBufferSize(
                sample_rate,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioFormat.CHANNEL_OUT_STEREO
            ) * 4
        )
        val buffer_size_in_bytes: Int = buffer_size * 4
        private const val maxkey = 0xFFFFFFFF
    }

    private var audioTrack: AudioTrack = AudioTrack.Builder()
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
        this.audioTrack.pause()
    }

    fun play() {
        this.audioTrack.play()
    }
    fun write(shorts: ShortArray) {
        if (this.audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
            try {
                this.audioTrack.write(
                    shorts,
                    0,
                    shorts.size,
                    AudioTrack.WRITE_BLOCKING
                )
            } catch (e: IllegalStateException) {
                // Shouldn't need to do anything. the audio track was released and this should stop on its own
            }
        }
    }
    fun stop() {
        this.audioTrack.stop()
    }
}
