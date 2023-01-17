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

class ActiveSample(var sample: Sample) {
    var current_frame = 0
    var is_pressed = true
}

@RequiresApi(Build.VERSION_CODES.M)
class MIDIPlaybackDevice(var context: Context, var soundFont: SoundFont): VirtualMIDIDevice() {
    private var sampleRate = 44100
    private var sampleSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 5

    private val active_notes: MutableSet<Pair<Int, Int>> = mutableSetOf()
    private val active_samples = HashMap<Pair<Int, Int>, ActiveSample>()
    lateinit var audioTrack: AudioTrack
    var track_is_playing: Boolean = false

    private val preset_channel_map = HashMap<Int, Int>()
    private val audio_tracks = HashMap<Int, AudioTrack>()

    init {
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(this.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(this.sampleSize)
            .build()
    }

    fun start_play() {
        thread {
            this.track_is_playing = true
            audioTrack.play()
            while (this.active_samples.size > 0) {
                var avg_data = Array(this.sampleSize / 2) { _ -> 0 }
                var avg_count = Array(this.sampleSize / 2) { _ -> 0 }

                var inactive_samples: MutableList<Pair<Int,Int>> = mutableListOf()
                for ((keypair, active_sample) in this.active_samples) {
                    var sample = active_sample.sample
                    var sample_data = this.soundFont.get_sample_data(sample.start, sample.end)!!
                    var offset = active_sample.current_frame
                    if (active_sample.is_pressed) {
                        Log.e("AAA", "${sample.loopEnd}, ${sample.loopStart}")
                    }
                    for (x in 0 until this.sampleSize / 2) {
                        var i = x * 2

                        var j = if (active_sample.is_pressed && i + offset > sample.loopEnd) {
                            ((i + offset) - sample.loopStart) % (sample.loopEnd - sample.loopStart)
                        } else {
                            i + offset
                        }

                        if (j < sample_data.size) {
                            var short = toUInt(sample_data[j]) + (toUInt(sample_data[j + 1]) * 256)
                            avg_data[x] += short
                        }
                        avg_count[x] += 1
                    }
                    if (active_sample.is_pressed) {
                        active_sample.current_frame += (this.sampleSize / 2)
                        if (active_sample.current_frame > sample.loopEnd) {
                            Log.e("AAA", "LOOP!")
                            active_sample.current_frame -= sample.loopStart
                            active_sample.current_frame = active_sample.current_frame % (sample.loopEnd - sample.loopStart)
                        }
                    } else if ((active_sample.current_frame * 2) + this.sampleSize < sample_data.size) {
                        active_sample.current_frame += this.sampleSize / 2
                    } else {
                        inactive_samples.add(keypair)
                    }
                }

                // Remove completed samples
                for (kp in inactive_samples) {
                    this.active_samples.remove(kp)
                }

                var use_bytes = ByteArray(this.sampleSize * 2) { _ -> 0 }
                avg_data.forEachIndexed { i, value ->
                    var c = avg_count[i]
                    var byte_pair: Pair<Byte, Byte> = if (c > 0) {
                        var avg_value = value / c
                        //Pair(
                        //    ((avg_value and 0x00FF)).toByte(),
                        //    ((avg_value and 0xFF00) shr 8).toByte()
                        //)
                        Pair(
                            ((value and 0x00FF)).toByte(),
                            ((value and 0xFF00) shr 8).toByte()
                        )
                    } else {
                        Pair(0,0)
                    }
                    // TODO: using Mono, use linked sample
                    use_bytes[(4 * i)] = byte_pair.first
                    use_bytes[(4 * i) + 1] = byte_pair.second
                    use_bytes[(4 * i) + 2] = byte_pair.first
                    use_bytes[(4 * i) + 3] = byte_pair.second
                }

                audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
            }
            audioTrack.stop()
            this.track_is_playing = false
        }
    }

    fun get_channel_preset(channel: Int): Int {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else {
            26
        }
    }

    override fun onNoteOn(event: NoteOn) {
        //TODO: Handle Bank
        var preset = this.soundFont.get_preset(this.get_channel_preset(event.channel))
        var instrument = this.soundFont.get_instrument(preset.instruments[0]!!.instrumentIndex)
        var isample = instrument.get_sample(event.note, event.velocity)

        if (isample != null) {
            var sample = this.soundFont.get_sample(isample.sampleIndex)
            this.active_samples[Pair(event.note, event.channel)] = ActiveSample(sample)
        }

        if (!this.track_is_playing) {
            this.start_play()
        }
    }

    override fun onNoteOff(event: NoteOff) {
        Log.e("AAA", "Release ${event.note} ${event.channel}")
        if (this.active_samples.containsKey(Pair(event.note, event.channel))) {
            this.active_samples[Pair(event.note, event.channel)]!!.is_pressed = false
        }
    }

    override fun onProgramChange(event: ProgramChange) {
        if (event.channel == 9) {
            return
        }
        this.preset_channel_map[event.channel] = event.program
    }
}

