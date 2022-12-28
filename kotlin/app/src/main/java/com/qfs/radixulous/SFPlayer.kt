package com.qfs.radixulous

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlin.experimental.and
import kotlin.math.sin
import kotlin.math.pow

class MidiPlayer {
    private val duration = 1
    private val sampleRate = 8000
    private val numSamples = duration * sampleRate
    private val sample = DoubleArray(numSamples)
    private val generatedSnd = ByteArray( numSamples * 2)
    var handler: Handler = Handler()
    var BASE_FREQ = 8.175798915643707

    fun play(midi_note: Int) {
        var freq = 2.toDouble().pow(midi_note.toDouble() / 12.0) * this.BASE_FREQ
        this.genTone(freq)

        this.playSound()
    }

    private fun genTone(freq: Double) {
        var k = sampleRate.toDouble() / freq
        for (i in 0 until numSamples) {
            var ratio = i.toDouble() / k
            sample[i] = sin(2 * Math.PI * ratio)
        }

        var idx = 0
        for (dVal in sample) {
            val byte_val = (dVal * 32768).toInt()
            generatedSnd[idx++] = (byte_val and 0xFF).toByte()
            generatedSnd[idx++] = ((byte_val shr 8) and 0xFF).toByte()
        }
    }

    private fun playSound() {
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            generatedSnd.size,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
    }
}