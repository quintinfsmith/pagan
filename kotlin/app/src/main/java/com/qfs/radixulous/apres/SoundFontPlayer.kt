package com.qfs.radixulous.apres

import android.content.Context
import android.media.*
import android.util.Range
import com.qfs.radixulous.apres.riffreader.toUInt
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.pow

class ActiveNoteHandle(var event: NoteOn, var preset: Preset) {
    class ActiveSample(
        var sample_right: InstrumentSample,
        var sample_left: InstrumentSample?,
        var instrument: PresetInstrument,
        var preset: Preset,
        var event: NoteOn
    ) {


        var current_frame_right = 0
        var current_frame_left = 0
        private var buffer_size_in_frames: Int
        private var buffer_size_in_bytes: Int
        var chunk_ratio: Int = 3
        private var chunk_size_in_frames: Int
        private var chunk_size_in_bytes: Int
        var is_pressed: Boolean = false
        private var audioTrack: AudioTrack? = null
        var volumeShaper: VolumeShaper? = null
        var volume: Float = 1F
        var in_attack_hold_decay = false

        constructor(sample: InstrumentSample, instrument: PresetInstrument, preset: Preset, event: NoteOn): this(sample, null, instrument, preset, event)

        init {
            val sample_rate = this.sample_right.sample!!.sampleRate


            while (this.buffer_size_in_bytes < sample_rate) {
                this.buffer_size_in_bytes *= 2
            }

            this.buffer_size_in_frames = buffer_size_in_bytes / 4

            this.chunk_size_in_frames = this.buffer_size_in_frames / this.chunk_ratio
            this.chunk_size_in_bytes = this.buffer_size_in_bytes / this.chunk_ratio

            this.audioTrack = this.buildAudioTrack()
        }

        private fun buildAudioTrack(): AudioTrack {
            val sample_rate = this.sample_right.sample!!.sampleRate

            //val original_note = this.instrument.instrument!!.global_sample!!.root_key ?: this.sample_right.sample!!.originalPitch
            val original_note = this.sample_right.root_key ?: this.sample_right.sample!!.originalPitch
            var shift = 1F
            if (original_note != this.event.note) {
                val originalPitch = original_note.toFloat()
                val samplePitch = 2F.pow(originalPitch / 12F)
                val requiredPitch = 2F.pow(this.event.note.toFloat() / 12F)
                shift = (requiredPitch / samplePitch)
            }

            var tuning_cent = (this.sample_right.tuning_cent ?: this.instrument.tuning_cent ?: this.preset.global_zone?.tuning_cent ?: 0).toFloat()
            var tuning_semi = (this.sample_right.tuning_semi ?: this.instrument.tuning_semi ?: this.preset.global_zone?.tuning_semi ?: 0).toFloat()
            if (tuning_cent != 0F || tuning_semi != 0F) {
                shift *= 2F.pow((12 + (tuning_semi + (tuning_cent / 1200))) / 12)
            }

            if (shift != 1F) {
                audioTrack.playbackParams = PlaybackParams().setPitch(shift)
            }

            val playbacklistener = SFPlaybackListener(this)

            audioTrack.setPlaybackPositionUpdateListener( playbacklistener )
            audioTrack.positionNotificationPeriod = this.chunk_size_in_frames
            return audioTrack
        }


        private fun apply_decay_shaper(): Long {
            val volumeShaper: VolumeShaper = this.volumeShaper ?: return 0

            var vol_env_release = this.sample_right.vol_env_release
            if (vol_env_release == null) {
                if (instrument.instrument == null || instrument.instrument!!.global_sample == null) {
                    return 0
                }

                vol_env_release = instrument.instrument!!.global_sample!!.vol_env_release ?: return 0
            }

            var delay = (vol_env_release * 1000F).toLong()
            try {
                val newConfig = VolumeShaper.Configuration.Builder()
                    .setDuration(delay)
                    .setCurve(floatArrayOf(0f, 1f), floatArrayOf(this.volume, 0f))
                    .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
                    .build()

                volumeShaper.replace(newConfig, VolumeShaper.Operation.PLAY, true)
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

            // TODO: Implement vol_env attack/hold/decay/sustain
            // this.in_attack_hold_decay = true
           // var curve = mutableListOf<Double>()
           // curve.add(
           //     this.sample_right.vol_env_delay ?: this.instrument.vol_env_delay ?: 0.toDouble()
           // )
           // curve.add(
           //     this.sample_right.vol_env_attack ?: this.instrument.vol_env_attack ?: 0.toDouble()
           // )
           // curve.add(
           //     this.sample_right.vol_env_hold ?: this.instrument.vol_env_hold ?: 0.toDouble()
           // )
           // var vol_env_sustain = this.sample_right.vol_env_sustain ?: this.instrument.vol_env_sustain
           // if (vol_env_sustain != null && vol_env_sustain > 0) {
           //     curve.add(
           //         this.sample_right.vol_env_decay ?: this.instrument.vol_env_decay ?: 0.toDouble()
           //     )
           // }
           // var volume_curve = mutableListOf<Float>(this.volume)
           // var time_curve = mutableListOf<Float>(0F)
           // var total = 0f
           // for (v in curve) {
           //     if (v == 0.toDouble()) {
           //         continue
           //     }
           //     total += v.toFloat()
           //     volume_curve.add(this.volume) // For now, don't actually attenuate
           //     time_curve.add(total)
           // }


            val config = VolumeShaper.Configuration.Builder()
                //.setCurve(time_curve.toFloatArray(), volume_curve.toFloatArray())
                .setCurve(floatArrayOf(0f, 1f), floatArrayOf(this.volume, this.volume))
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
    var active_samples: Set<ActiveSample>
    init {
        this.active_samples = this.gen_active_samples()
    }


    // Stop and return ttl
    fun stop(immediate: Boolean = false): Long {
        var max_ttl: Long = 0
        for (sample in this.active_samples) {
            if (immediate) {
                sample.really_stop()
            } else {
                max_ttl = max(max_ttl, sample.stop())
            }
        }
        return max_ttl
    }

}

class AudioTrackHandle() {
    class Listener(private var handle: AudioTrackHandle): AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(p0: AudioTrack?) {
            //
        }
        override fun onPeriodicNotification(p0: AudioTrack?) {
            if (p0 != null) {
                this.handle.write_next_chunk()
            }
        }
    }

    private var sample_rate = 44100
    private var buffer_size_in_bytes: Int
    private var buffer_size_in_frames: Int
    private var chunk_size_in_frames: Int
    private var chunk_size_in_bytes: Int
    private var chunk_ratio: Int = 3

    private var audioTrack: AudioTrack
    private var sample_handles = HashMap<Int, SampleHandle>()
    private var keygen: Int = 0
    private val maxkey = 0xFFFFFFFF

    init {
        this.buffer_size_in_bytes = AudioTrack.getMinBufferSize(
            this.sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        )
        this.buffer_size_in_frames = buffer_size_in_bytes / 4
        this.chunk_size_in_frames = this.buffer_size_in_frames / this.chunk_ratio
        this.chunk_size_in_bytes = this.buffer_size_in_bytes / this.chunk_ratio

        this.audioTrack = AudioTrack.Builder()
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
            .setBufferSizeInBytes(this.buffer_size_in_bytes)
            .build()

        val playbacklistener = Listener(this)
        this.audioTrack.setPlaybackPositionUpdateListener( playbacklistener )
        this.audioTrack.positionNotificationPeriod = this.chunk_size_in_frames
    }

    private fun play() {
        if (this.sample_handles.isEmpty()) {
            return
        }

        for (i in 0 until this.chunk_ratio) {
            this.write_next_chunk()
        }

        // TODO: Implement vol_env attack/hold/decay/sustain
        // this.in_attack_hold_decay = true
        // var curve = mutableListOf<Double>()
        // curve.add(
        //     this.sample_right.vol_env_delay ?: this.instrument.vol_env_delay ?: 0.toDouble()
        // )
        // curve.add(
        //     this.sample_right.vol_env_attack ?: this.instrument.vol_env_attack ?: 0.toDouble()
        // )
        // curve.add(
        //     this.sample_right.vol_env_hold ?: this.instrument.vol_env_hold ?: 0.toDouble()
        // )
        // var vol_env_sustain = this.sample_right.vol_env_sustain ?: this.instrument.vol_env_sustain
        // if (vol_env_sustain != null && vol_env_sustain > 0) {
        //     curve.add(
        //         this.sample_right.vol_env_decay ?: this.instrument.vol_env_decay ?: 0.toDouble()
        //     )
        // }
        // var volume_curve = mutableListOf<Float>(this.volume)
        // var time_curve = mutableListOf<Float>(0F)
        // var total = 0f
        // for (v in curve) {
        //     if (v == 0.toDouble()) {
        //         continue
        //     }
        //     total += v.toFloat()
        //     volume_curve.add(this.volume) // For now, don't actually attenuate
        //     time_curve.add(total)
        // }


       //     val config = VolumeShaper.Configuration.Builder()
       //         //.setCurve(time_curve.toFloatArray(), volume_curve.toFloatArray())
       //         .setCurve(floatArrayOf(0f, 1f), floatArrayOf(this.volume, this.volume))
       //         .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
       //         .build()

       //     this.volumeShaper = this.audioTrack!!.createVolumeShaper(config)
       //     this.volumeShaper!!.apply(VolumeShaper.Operation.PLAY)


        this.audioTrack.play()
    }

    private fun stop() {
        this.audioTrack.stop()
    }

    private fun genkey(): Int {
        val output = this.keygen

        this.keygen += 1
        if (this.maxkey <= this.keygen) {
            this.keygen = 0
        }

        return output
    }

    fun add_sample_handle(handle: SampleHandle, autoplay: Boolean = true): Int {
        var newkey = this.genkey()
        this.sample_handles[newkey] = handle
        if (autoplay) {
            this.play()
        }
        return newkey
    }

    fun add_sample_handles(handles: Set<SampleHandle>, autoplay: Boolean = true): Set<Int> {
        var output = mutableSetOf<Int>()
        for (handle in handles) {
            output.add(this.add_sample_handle(handle, false))
        }

        if (autoplay) {
            this.play()
        }

        return output
    }


    fun remove_sample_handle(key: Int) {
        this.sample_handles.remove(key)
        if (this.sample_handles.isEmpty()) {
            this.stop()
        }
    }

    fun write_next_chunk() {
        val use_bytes = ByteArray(this.chunk_size_in_bytes) { _ -> 0 }
        for (x in 0 until this.chunk_size_in_frames) {
            var left = 0
            var right = 0
            for ((_, sample_handle) in sample_handles) {
                // TODO: *Maybe* detach handle if null is returned here?
                val v = sample_handle.get_next_frame() ?: continue
                // TODO: Implement ROM stereo modes
                when (sample_handle.stereo_mode) {
                    1 -> { // mono
                        left += v
                        right += v
                    }
                    2 -> { // right
                        right += v
                    }
                    4 -> { // left
                        left += v
                    }
                    else -> {

                    }
                }
            }

            use_bytes[(4 * x)] = (right % 256).toByte()
            use_bytes[(4 * x) + 1] = (right / 256).toByte()
            use_bytes[(4 * x) + 2] = (left % 256).toByte()
            use_bytes[(4 * x) + 3] = (left / 256).toByte()
        }

        val audioTrack = this.audioTrack

        if (audioTrack != null && audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
            try {
                audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
            } catch (e: IllegalStateException) {
                // Shouldn't need to do anything. the audio track was released and this should stop on its own
            }
        }

    }
}

enum class SamplePhase {
    Delay,
    Attack,
    Decay,
    Sustain,
    Release
}

class SampleHandle(var event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset) {
    var pitch_shift: Float = 1F
    var current_position: Int = 0
    val loop_points: Pair<Int, Int>?
    var data: ByteArray
    var phase_map = HashMap<SamplePhase, Pair<Int, Int>>()
    var stereo_mode: Int

    init {
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            val tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            val requiredPitch = 2F.pow((this.event.note.toFloat() + (tuning_semi + (tuning_cent / 1200))) / 12F)
            this.pitch_shift = requiredPitch / original_pitch
        }

        data = this.resample(sample.sample!!.data)

        this.stereo_mode = sample.sample!!.sampleType
        this.loop_points = if (sample.sampleMode!! and 1 == 1) {
            Pair(
                (sample.sample!!.loopStart.toFloat() / this.pitch_shift).toInt(),
                (sample.sample!!.loopEnd.toFloat() / this.pitch_shift).toInt()
            )
        } else {
            null
        }
    }


    // TODO: VVVVVVVVVVVVVVVVVVVVVVVVV
    fun resample(sample_data: ByteArray): ByteArray {
        return sample_data
    }

    fun get_next_frame(): Int? {
        if (this.current_position >= this.data.size) {
            return null
        }

        val a = toUInt(this.data[this.current_position])
        val b = toUInt(this.data[this.current_position + 1])
        this.current_position += 2
        if (this.loop_points != null) {
            if (this.current_position >= this.loop_points.second) {
                this.current_position = this.loop_points.first
            }
        }

        return ((b * 256) + a)
    }
}


class MIDIPlaybackDevice(var context: Context, var soundFont: SoundFont): VirtualMIDIDevice() {
    private val preset_channel_map = HashMap<Int, Int>()
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val audio_track_handle = AudioTrackHandle()
    private val active_handle_keys = HashMap<Pair<Int, Int>, Set<Int>>()

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

    private fun release_note(note: Int, channel: Int) {
        this.kill_note(note, channel)
    }

    private fun kill_note(note: Int, channel: Int) {
        var keys = this.active_handle_keys.remove(Pair(note, channel)) ?: return
        for (key in keys) {
            this.audio_track_handle.remove_sample_handle(key)
        }
    }

    private fun press_note(event: NoteOn) {
        // TODO: Handle Bank
        val bank = if (event.channel == 9) {
            128
        } else {
            0
        }

        val preset = this.loaded_presets[Pair(bank, this.get_channel_preset(event.channel))]!!
        var audio_track_keys = mutableSetOf<Int>()
        this.active_handle_keys[Pair(event.note, event.channel)] = this.audio_track_handle.add_sample_handles(this.gen_sample_handles(event, preset))
    }

    override fun onNoteOn(event: NoteOn) {
        this.kill_note(event.note, event.channel)
        this.press_note(event)
    }

    override fun onNoteOff(event: NoteOff) {
        this.release_note(event.note, event.channel)
    }

    override fun onProgramChange(event: ProgramChange) {
        if (event.channel == 9) {
            return
        }
        val key = Pair(0, event.program)
        if (this.loaded_presets[key] == null) {
            this.loaded_presets[key] = this.soundFont.get_preset(event.program, 0)
        }

        this.preset_channel_map[event.channel] = event.program
    }

    override fun onAllSoundOff(event: AllSoundOff) { }

    fun gen_sample_handles(event: NoteOn, preset: Preset): Set<SampleHandle> {
        val output = mutableSetOf<SampleHandle>()
        val potential_instruments = preset.get_instruments(event.note, event.velocity)

        for (p_instrument in potential_instruments) {
            val samples = p_instrument.instrument!!.get_samples(
                event.note,
                event.velocity
            ).toList()

            for (sample in samples) {
                output.add(
                    SampleHandle(event, sample, p_instrument, preset)
                )
            }
        }
        return output
    }
}

