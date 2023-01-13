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
class MIDIPlaybackDevice(var context: Context, var soundFont: SoundFont): VirtualMIDIDevice() {
    private var sampleRate = 44100
    private var sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)

    private val active_notes: MutableSet<Int> = mutableSetOf()
    lateinit var audioTrack: AudioTrack
    private val instrument_sample_map = HashMap<Int, HashMap<Int, Sample>>()
    private val instrument_channel_map = HashMap<Int, Int>()
    private val audio_tracks = HashMap<Int, AudioTrack>()

    init {
    }

    //fun start_play() {
    //    thread {
    //        audioTrack.play()
    //        while (this.active_notes.isNotEmpty()) {
    //            var avg_data = FloatArray(this.sampleSize * 2)
    //            var note_count = 0
    //            var active_notes_copy = this.active_notes.toList()
    //            for (note in active_notes_copy) {
    //                for (s in 0 until this.sampleSize) {
    //                    avg_data[2 * s] += this.sample_map[note]!![s % this.sampleSize]
    //                }
    //                note_count += 1 //using 'this.active_notes' could technically change for averaging
    //            }

    //            for (s in 0 until this.sampleSize) {
    //                avg_data[(2 * s)] = avg_data[(2 * s)] / note_count
    //                avg_data[(2 * s) + 1] = avg_data[(2 * s)]
    //            }

    //            audioTrack.write(avg_data, 0, avg_data.size, AudioTrack.WRITE_BLOCKING)
    //        }
    //        audioTrack.stop()
    //    }
    //}

    override fun onNoteOn(event: NoteOn) {
        var preset = this.soundFont.get_preset(event.note, 0) // TODO: Handle Bank
        if (preset == null) {
            return
        }

        for (instrument in preset!!.get_instruments(event.note, event.velocity)) {
            var real_instrument = this.soundFont.instruments[instrument.instrumentIndex]
            for (sample in real_instrument.get_samples(event.note, event.velocity)) {
                var real_sample = this.soundFont.samples[sample.sampleIndex]
                var sample_data = this.soundFont.get_sample_data(real_sample.start, real_sample.end)!!

                var track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(real_sample.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                    .setBufferSizeInBytes(sample_data.size)
                    .build()


                track.write(sample_data, 0, sample_data.size, AudioTrack.WRITE_BLOCKING)
                track.play()
                track.stop()

                this.active_notes.add(event.note)
            }
        }

    }

    override fun onNoteOff(event: NoteOff) {
        if(event.channel == 9) {
            return
        }
        this.active_notes.remove(event.note)
    }
}

