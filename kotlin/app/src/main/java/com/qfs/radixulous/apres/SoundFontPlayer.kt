package com.qfs.radixulous.apres

import com.qfs.radixulous.apres.riffreader.toUInt
import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread
import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

val BASE_FREQ = 8.175798915643707F

@RequiresApi(Build.VERSION_CODES.M)
class ActiveSample(var event: NoteOn, var preset: Preset) {
    var current_frame = 0
    var buffer_size_in_frames: Int
    var buffer_size_in_bytes: Int
    var chunk_ratio: Int = 3
    var chunk_size_in_frames: Int
    var chunk_size_in_bytes: Int
    var is_pressed: Boolean = false
    var audioTrack: AudioTrack? = null
    var volumeShaper: VolumeShaper? = null
    var volume: Float = 1F
    var samples: List<Sample>
    var sample_rate: Int? = null
    var resample_ratio = 1F

    init {
        this.samples = this.get_samples().toList()
        if (this.samples.isEmpty()) {
            throw Exception("Instrument has no samples")
        }

        var sample_rate = this.get_sample_rate()

        //this.buffer_size_in_bytes = this.sample.sampleRate
        this.buffer_size_in_bytes = AudioTrack.getMinBufferSize(
            sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        )

        while (this.buffer_size_in_bytes < sample_rate) {
            this.buffer_size_in_bytes *= 2
        }

        this.buffer_size_in_frames = buffer_size_in_bytes / 4

        this.chunk_size_in_frames = this.buffer_size_in_frames / this.chunk_ratio
        this.chunk_size_in_bytes = this.buffer_size_in_bytes / this.chunk_ratio

        this.audioTrack = this.buildAudioTrack()
    }

    fun get_instrument(): Instrument {
        return this.preset.instruments[0].instrument!!
    }

    fun get_instrument_samples(): Set<InstrumentSample> {
        return this.get_instrument().get_samples(this.event.note, this.event.velocity)
    }

    fun get_samples(): Set<Sample> {
        var output = mutableSetOf<Sample>()
        for (sample in this.get_instrument_samples()) {
            output.add(sample.sample!!)
        }
        return output
    }

    fun get_sample_rate(): Int {
        var sample_rate: Int? = null
        if (this.sample_rate != null) {
            return this.sample_rate!!
        }

        if (samples.isEmpty()) {
            throw Exception("Can't get sample rate, no samples")
        }

        for (sample in samples) {
            if (sample_rate != null) {
                if (sample_rate != sample.sampleRate) {
                    throw Exception("Incompatible instrument samples")
                }
            }
            sample_rate = sample.sampleRate
        }


        var ratio = log2(44100F / sample_rate!!.toFloat())

        // Needs to be resampled
        if (ratio.toInt().toFloat() != ratio) {
            var target_rate = 22050
            this.resample_ratio = sample_rate.toFloat() / (target_rate.toFloat())
            sample_rate = target_rate
        }

        this.sample_rate = sample_rate

        return sample_rate!!
    }

    private fun buildAudioTrack(): AudioTrack {
        var instrument_samples = this.get_instrument_samples()
        var samples = this.get_samples()
        var sample_rate = this.get_sample_rate()

        var audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sample_rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(this.buffer_size_in_bytes)
            .build()

        var original_note = instrument_samples.first().root_key ?: samples.first().originalPitch!!

        if (original_note != this.event.note) {
            var originalPitch = original_note.toFloat()
            var samplePitch = 2F.pow(originalPitch / 12F)
            var requiredPitch = 2F.pow(this.event.note.toFloat() / 12F)
            var shift = (requiredPitch / samplePitch)
            if (this.resample_ratio != 1F) {
                shift *= this.resample_ratio
            }
            audioTrack.playbackParams = PlaybackParams().setPitch(shift)
        }

        var playbacklistener = SFPlaybackListener(this)

        audioTrack.setPlaybackPositionUpdateListener( playbacklistener )
        audioTrack.positionNotificationPeriod = this.chunk_size_in_frames
        return audioTrack
    }

    fun write_next_chunk() {
        var loop_start = this.samples.first().loopStart
        var loop_end = this.samples.first().loopEnd
        var call_stop = false
        var sample_size = this.samples.first().data.size
        var l_index = 0
        var r_index = if (this.samples.size == 1) {
            0
        } else {
            1
        }

        var use_bytes = ByteArray(this.chunk_size_in_bytes) { _ -> 0 }
        for (x in 0 until this.chunk_size_in_frames) {

            if (this.is_pressed && this.current_frame > loop_end) {
                this.current_frame -= loop_start
                this.current_frame %= loop_end - loop_start
            }

            if (this.current_frame < sample_size / 2) {
                var j = this.current_frame * 2

                use_bytes[(4 * x)] = this.samples[l_index].data[j]
                use_bytes[(4 * x) + 1] = this.samples[l_index].data[j + 1]
                use_bytes[(4 * x) + 2] = this.samples[r_index].data[j]
                use_bytes[(4 * x) + 3] = this.samples[r_index].data[j + 1]
            } else if (!this.is_pressed) {
                call_stop = true
                break
            } else {
                break
            }

            this.current_frame += 1
        }

        var audioTrack = this.audioTrack

        if (audioTrack != null && audioTrack!!.state != AudioTrack.STATE_UNINITIALIZED) {
            try {
                audioTrack!!.write( use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING )
            } catch (e: IllegalStateException) {
                // Shouldn't need to do anything. the audio track was released and this should stop on its own
            }
        }

        if (call_stop) {
            this.really_stop()
        }
    }

    fun apply_decay_shaper(): Long {
        var volumeShaper: VolumeShaper? = this.volumeShaper ?: return 0
        var instrument = this.get_instrument()
        var instrument_sample = this.get_instrument_samples().first()

        var vol_env_release = instrument_sample.vol_env_release
        if (vol_env_release == null) {
            if (instrument.global_sample == null) {
                return 0
            }

            vol_env_release = instrument.global_sample!!.vol_env_release

            if (vol_env_release == null) {
                return 0
            }
        }
        var delay = (vol_env_release!! * 1000F).toLong()

        try {
            val newConfig = VolumeShaper.Configuration.Builder()
                .setDuration(delay)
                .setCurve(floatArrayOf(0f, 1f), floatArrayOf(this.volume, 0f))
                .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
                .build()

            volumeShaper!!.replace(newConfig, VolumeShaper.Operation.PLAY, true)

            thread {
                Thread.sleep(delay)
                this.really_stop()
            }
        } catch (e: IllegalStateException) {
            delay = 0
            this.volumeShaper = null
        }
        return delay

    }

    fun play(velocity: Int) {
        this.is_pressed = true
        for (i in 0 until this.chunk_ratio) {
            this.write_next_chunk()
        }

        this.volume = velocity.toFloat() / 128F

        val config = VolumeShaper.Configuration.Builder()
            .setDuration(10)
            .setCurve(floatArrayOf(0f, 1f), floatArrayOf(0f, this.volume))
            .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
            .build()

        this.volumeShaper = this.audioTrack!!.createVolumeShaper(config)
        this.volumeShaper!!.apply(VolumeShaper.Operation.PLAY)

        this.audioTrack!!.play()
    }

    fun stop(): Long {
        this.is_pressed = false
        return this.apply_decay_shaper()
    }

    fun really_stop() {
        try {
            this.audioTrack!!.stop()
            this.audioTrack!!.release()
        } catch (e: Exception) { }
        this.audioTrack = null
        //if (this.audioTrack.state == AudioTrack.STATE_INITIALIZED) {
        //    if (this.audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
        //    }
        //    this.audioTrack.release()
        //}
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
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()

    init {
        this.loaded_presets[Pair(0, 0)] = this.soundFont.get_preset(0, 0)
        this.loaded_presets[Pair(128, 0)] = this.soundFont.get_preset(0,128)

    }

    fun get_channel_preset(channel: Int): Int {
        return if (this.preset_channel_map.containsKey(channel)) {
            this.preset_channel_map[channel]!!
        } else {
            0
        }
    }

    override fun onNoteOn(event: NoteOn) {
        var currently_active_sample: ActiveSample? = this.active_samples[Pair(event.note, event.channel)]
        currently_active_sample?.stop()

        //TODO: Handle Bank
        var bank = if (event.channel == 9) {
            128
        } else {
            0
        }

        var preset = this.loaded_presets[Pair(bank, this.get_channel_preset(event.channel))]!!
        try {
            var active_sample = ActiveSample(event, preset)
            this.active_samples[Pair(event.note, event.channel)] = active_sample
            active_sample.play(event.velocity)
        } catch (e: Exception) {

        }
    }

    override fun onNoteOff(event: NoteOff) {
        var sample = this.active_samples.remove(Pair(event.note, event.channel)) ?: return
        var ttl = sample.stop()

        //this.decaying_samples.add(sample)
    }

    override fun onProgramChange(event: ProgramChange) {
        if (event.channel == 9) {
            return
        }
        var key = Pair(0, event.program)
        if (this.loaded_presets[key] == null) {
            this.loaded_presets[key] = this.soundFont.get_preset(event.program, 0)
        }

        this.preset_channel_map[event.channel] = event.program
    }
}

