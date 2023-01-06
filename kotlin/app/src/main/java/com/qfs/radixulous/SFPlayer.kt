package com.qfs.radixulous

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.qfs.radixulous.apres.NoteOff
import com.qfs.radixulous.apres.NoteOn
import com.qfs.radixulous.apres.VirtualMIDIDevice
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sin

class MIDIPlaybackDevice(var context: Context): VirtualMIDIDevice() {
    private var sampleRate = 44100
    private var sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)

    private val active_notes: MutableSet<Int> = mutableSetOf()

    private val sample_map = HashMap<Int, List<Double>>()
    private val audio_tracks = HashMap<Int, AudioTrack>()
    //   var handler: Handler = Handler()
    var BASE_FREQ = 8.175798915643707

    init {
        for (note in 27 .. 100) {
            var freq = 2.toDouble().pow(note.toDouble() / 12.0) * this.BASE_FREQ

            var k = sampleRate.toDouble() / freq
            var generated: MutableList<Double> = mutableListOf()
            for (i in 0 until (k * freq).toInt()) {
                var ratio = i.toDouble() / k
                generated.add(sin(2 * Math.PI * ratio))
            }
            sample_map[note] = generated

            //val byte_val = (dVal * 32768).toInt()
            //generated.add(Pair(
            //    (byte_val and 0xFF),
            //    ((byte_val shr 8) and 0xFF)
            //))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onNoteOn(event: NoteOn) {
        //this.sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT) *
        var audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(this.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(this.sampleRate * 2)
            .build()

        val duration = 2
        val generatedSnd = FloatArray(sample_map[event.note]!!.size * 2)


        this.audio_tracks[event.note] = audioTrack
        thread {
            audioTrack.play()

            while (this.audio_tracks.containsKey(event.note)) {
                for (s in 0 until generatedSnd.size) {
                    var value = this.sample_map[event.note]!![s % sampleSize]
                    generatedSnd[s] = value.toFloat()
                }
                audioTrack.write(generatedSnd, 0, generatedSnd.size, AudioTrack.WRITE_BLOCKING)
            }

            audioTrack.stop()
        }
    }

    override fun onNoteOff(event: NoteOff) {
        if (this.audio_tracks.containsKey(event.note)) {
            this.audio_tracks.remove(event.note)
        }
    }

    private fun getWaveInfo(index: Int): List<Double>? {
        return this.sample_map[index]
    }

}

