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
import com.qfs.radixulous.apres.ProgramChange
import com.qfs.radixulous.apres.VirtualMIDIDevice
import java.lang.Integer.max
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.M)
class MIDIPlaybackDevice(var context: Context, var soundFont: SoundFont): VirtualMIDIDevice() {
    private var sampleRate = 44100
    private var sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)

    private val active_notes: MutableSet<Int> = mutableSetOf()
    lateinit var audioTrack: AudioTrack
    private val preset_channel_map = HashMap<Int, Int>()
    private val audio_tracks = HashMap<Int, AudioTrack>()

    init { }

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

    fun get_channel_preset(channel: Int): Int {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else {
            0
        }
    }

    override fun onNoteOn(event: NoteOn) {
        //TODO: Handle Bank
        var preset = this.soundFont.get_preset(this.get_channel_preset(event.channel))
        var instrument = this.soundFont.get_instrument(preset.instruments[0]!!.instrumentIndex)
        var isample = instrument.get_sample(event.note, event.velocity)

        if (isample != null) {
            var sample = this.soundFont.get_sample(isample.sampleIndex)
            Log.e("AAA", sample.name)

            var sample_data = this.soundFont.get_sample_data(sample.start, sample.end)!!

            var minSize = AudioTrack.getMinBufferSize(
                sample.sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            var track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sample.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(max(minSize, sample_data.size * 2))
                .build()

            var towrite = ByteArray(sample_data.size * 2)
            for (i in 0 until sample_data.size / 2) {
                towrite[i * 2] = sample_data[i * 2]
                towrite[(i * 2) + 1] = sample_data[(i * 2) + 1]
                towrite[(i * 2) + 2] = sample_data[i * 2]
                towrite[(i * 2) + 3] = sample_data[(i * 2) + 1]
            }
            track.write(towrite, 0, towrite.size, AudioTrack.WRITE_BLOCKING)
            track.play()
        }

        this.active_notes.add(event.note)
    }

    override fun onNoteOff(event: NoteOff) {
        if(event.channel == 9) {
            return
        }
        this.active_notes.remove(event.note)
    }

    override fun onProgramChange(event: ProgramChange) {
        if (event.channel == 9) {
            return
        }
        this.preset_channel_map[event.channel] = event.program
    }
}

