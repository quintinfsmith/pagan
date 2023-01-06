package com.qfs.radixulous

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.qfs.radixulous.apres.NoteOff
import com.qfs.radixulous.apres.NoteOn
import com.qfs.radixulous.apres.VirtualMIDIDevice
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.M)
class MIDIPlaybackDevice(var context: Context): VirtualMIDIDevice() {
    private var sampleRate = 44100
    private var sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)

    private val active_notes: MutableSet<Int> = mutableSetOf()
    lateinit var audioTrack: AudioTrack
    private val sample_map = HashMap<Int, List<Float>>()
    private val audio_tracks = HashMap<Int, AudioTrack>()
    //   var handler: Handler = Handler()
    var BASE_FREQ = 8.175798915643707

    init {
        for (note in 27 .. 100) {
            var freq = 2.toDouble().pow(note.toDouble() / 12.0) * this.BASE_FREQ

            var k = sampleRate.toDouble() / freq
            var generated: MutableList<Float> = mutableListOf()
            for (i in 0 until (k * freq).toInt()) {
                var ratio = i.toDouble() / k
                generated.add(sin(2 * Math.PI * ratio).toFloat())
            }
            sample_map[note] = generated

        }

        this.audioTrack = AudioTrack.Builder()
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
    }

    fun start_play() {
        thread {
            audioTrack.play()
            while (this.active_notes.isNotEmpty()) {
                var avg_data = FloatArray(this.sampleSize * 2)
                var note_count = 0
                var active_notes_copy = this.active_notes.toList()
                for (note in active_notes_copy) {
                    for (s in 0 until this.sampleSize) {
                        avg_data[2 * s] += this.sample_map[note]!![s % this.sampleSize]
                    }
                    note_count += 1 //using 'this.active_notes' could technically change for averaging
                }

                for (s in 0 until this.sampleSize) {
                    avg_data[(2 * s)] = avg_data[(2 * s)] / note_count
                    avg_data[(2 * s) + 1] = avg_data[(2 * s)]
                }

                audioTrack.write(avg_data, 0, avg_data.size, AudioTrack.WRITE_BLOCKING)
            }
            audioTrack.stop()
        }
    }

    override fun onNoteOn(event: NoteOn) {
        if(event.channel == 9) {
            return
        }
        //this.sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT) *
        this.active_notes.add(event.note)
        if (this.active_notes.size == 1) {
            this.start_play()
        }
    }

    override fun onNoteOff(event: NoteOff) {
        if(event.channel == 9) {
            return
        }
        this.active_notes.remove(event.note)
    }
}

