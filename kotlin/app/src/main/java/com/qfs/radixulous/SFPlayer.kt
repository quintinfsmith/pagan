package com.qfs.radixulous

import android.content.Context
import android.media.*
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

val BASE_FREQ = 8.175798915643707

@RequiresApi(Build.VERSION_CODES.M)
class ActiveSample(var note: Int, var instrument_sample: InstrumentSample, var soundFont: SoundFont) {
    var current_frame = 0
    var buffer_size_in_frames: Int
    var buffer_size_in_bytes: Int
    var chunk_ratio: Int = 3
    var chunk_size_in_frames: Int
    var chunk_size_in_bytes: Int
    var is_pressed: Boolean = false
    var audioTrack: AudioTrack
    var sample: Sample
    var sampleData: ByteArray
    var volume_decay_shaper: MutableList<Float>? = null

    init {
        this.sample = this.soundFont.get_sample(instrument_sample.sampleIndex)
        this.sampleData = this.soundFont.get_sample_data(this.sample.start, this.sample.end)!!

        this.buffer_size_in_bytes = sample.sampleRate
        this.buffer_size_in_frames = buffer_size_in_bytes / 4


        this.chunk_size_in_frames = this.buffer_size_in_frames / this.chunk_ratio
        this.chunk_size_in_bytes = this.buffer_size_in_bytes / this.chunk_ratio

        this.audioTrack = AudioTrack.Builder()
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
            .setBufferSizeInBytes(this.buffer_size_in_bytes)
            .build()

        if (this.sample.originalPitch != this.note) {
            var samplePitch = BASE_FREQ * 2F.pow(this.sample.originalPitch!!.toFloat() / 12.toFloat())
            var requiredPitch = BASE_FREQ * 2F.pow(this.note.toFloat() / 12.toFloat())
            this.audioTrack.playbackParams =
                PlaybackParams().setPitch((requiredPitch / samplePitch).toFloat())
        }

        var playbacklistener = SFPlaybackListener(this)
        this.audioTrack.setPlaybackPositionUpdateListener( playbacklistener )
        this.audioTrack.positionNotificationPeriod = this.chunk_size_in_frames

    }

    fun write_next_chunk() {
        var loop_start = this.sample.loopStart - this.sample.start
        var loop_end = this.sample.loopEnd - this.sample.start

        var use_bytes = ByteArray(this.chunk_size_in_bytes) { _ -> 0 }
        for (x in 0 until this.chunk_size_in_frames) {
            if (this.is_pressed && this.current_frame > loop_end) {
                this.current_frame -= loop_start
                this.current_frame %= loop_end  - loop_start
            }

            if (this.current_frame < sampleData.size / 2) {
                var j = this.current_frame * 2
                var byte_pair = if (this.volume_decay_shaper == null) {
                    Pair(sampleData[j], sampleData[j + 1])
                } else {
                    var short = toUInt(sampleData[j]) + (toUInt(sampleData[j + 1]) * 256)
                    var altered: Int = (short.toFloat() * this.volume_decay_shaper!!.removeFirst()).toInt()
                    Pair(
                        (altered and 0xFF).toByte(),
                        ((altered and 0xFF00) shr 8).toByte()
                    )
                }

                use_bytes[(4 * x)] =    byte_pair.first
                use_bytes[(4 * x) + 1] = byte_pair.second
                use_bytes[(4 * x) + 2] = byte_pair.first
                use_bytes[(4 * x) + 3] = byte_pair.second

            }
            this.current_frame += 1
        }

        this.audioTrack.write(
            use_bytes,
            0,
            use_bytes.size,
            AudioTrack.WRITE_BLOCKING
        )
    }

    fun play() {
        for (i in 0 until this.chunk_ratio) {
            this.write_next_chunk()
        }
        this.audioTrack.play()
    }

    fun stop() {
        // TODO: set volume_decay_shaper instead of just calling really_stop()
        this.really_stop()
        //this.volume_decay_shaper = Array(
    }

    private fun really_stop() {
        this.audioTrack.stop()
        this.audioTrack.release()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class SFPlaybackListener(var active_sample: ActiveSample): AudioTrack.OnPlaybackPositionUpdateListener {
    override fun onMarkerReached(p0: AudioTrack?) {
        //
    }

    override fun onPeriodicNotification(p0: AudioTrack?) {
        if (p0 != null) {
            this.active_sample.write_next_chunk()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class MIDIPlaybackDevice(var context: Context, var soundFont: SoundFont): VirtualMIDIDevice() {

    private val active_samples = HashMap<Pair<Int, Int>, ActiveSample>()
    private val preset_channel_map = HashMap<Int, Int>()

    fun get_channel_preset(channel: Int): Int {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else {
            0
        }
    }

    override fun onNoteOn(event: NoteOn) {
        //TODO: Handle Bank
        var bank = if (event.channel == 9) {
            128
        } else {
            0
        }
        Log.e("AAA", "${this.soundFont.presets}")
        var preset = this.soundFont.get_preset(this.get_channel_preset(event.channel), bank) ?: return
        var instrument = this.soundFont.get_instrument(preset.instruments[0]!!.instrumentIndex)
        var isample = instrument.get_sample(event.note, event.velocity)

        if (isample != null) {
            var active_sample = ActiveSample(event.note, isample, this.soundFont)
            this.active_samples[Pair(event.note, event.channel)] = active_sample
            active_sample.play()
        }
    }

    override fun onNoteOff(event: NoteOff) {
        this.active_samples.remove(Pair(event.note, event.channel))?.stop()
    }

    override fun onProgramChange(event: ProgramChange) {
        if (event.channel == 9) {
            return
        }
        this.preset_channel_map[event.channel] = event.program
    }
}

