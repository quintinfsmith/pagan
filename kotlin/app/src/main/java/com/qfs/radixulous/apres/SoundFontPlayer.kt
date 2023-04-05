package com.qfs.radixulous.apres

import android.content.Context
import android.media.*
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

        class SFPlaybackListener(private var active_sample: ActiveSample): AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(p0: AudioTrack?) {
                //
            }

            override fun onPeriodicNotification(p0: AudioTrack?) {
                if (p0 != null) {
                    this.active_sample.write_next_chunk()
                }
            }
        }

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

        private fun buildAudioTrack(): AudioTrack {
            val sample_rate = this.sample_right.sample!!.sampleRate

            val audioTrack = AudioTrack.Builder()
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

        fun write_next_chunk() {
            val sample_right = this.sample_right.sample!!
            var sample_left = this.sample_left?.sample ?: sample_right
            var call_stop = false

            val use_bytes = ByteArray(this.chunk_size_in_bytes) { _ -> 0 }
            for (x in 0 until this.chunk_size_in_frames) {
                // If sample is a looping sample AND note is being held
                // TODO: Differentiate between sampleMode 1 and 3 here
                if (this.is_pressed || this.in_attack_hold_decay) {
                    if (this.sample_right.sampleMode != null && this.sample_right.sampleMode!! and 1 == 1) {
                        if (this.current_frame_right > sample_right.loopEnd) {
                            this.current_frame_right = sample_right.loopStart
                        }
                        if (this.current_frame_left > sample_left.loopEnd) {
                            this.current_frame_left = sample_left.loopStart
                        }
                    }
                }

                if (this.current_frame_right < sample_right.data.size / 2 && this.current_frame_left < sample_left.data.size / 2) {
                    var j = this.current_frame_right * 2
                    use_bytes[(4 * x)] = sample_right.data[j]
                    use_bytes[(4 * x) + 1] = sample_right.data[j + 1]

                    j = this.current_frame_left * 2
                    use_bytes[(4 * x) + 2] = sample_left.data[j]
                    use_bytes[(4 * x) + 3] = sample_left.data[j + 1]
                } else if (!this.is_pressed) {
                    call_stop = true
                    break
                } else {
                    break
                }

                this.current_frame_right += 1
                this.current_frame_left += 1
            }

            val audioTrack = this.audioTrack

            if (audioTrack != null && audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
                try {
                    audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
                } catch (e: IllegalStateException) {
                    // Shouldn't need to do anything. the audio track was released and this should stop on its own
                }
            }

            if (call_stop) {
                this.really_stop()
            }
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

    fun gen_active_samples(): Set<ActiveSample> {
        val output = mutableSetOf<ActiveSample>()
        val potential_instruments = this.preset.get_instruments(this.event.note, this.event.velocity)
        for (p_instrument in potential_instruments) {
            val samples = p_instrument.instrument!!.get_samples(
                this.event.note,
                this.event.velocity
            ).toList()

            if (samples.isEmpty()) {
                continue
            } else {
                if (samples.size == 2) {
                    if (samples[0].sample!!.sampleType == 2) {
                        output.add(
                            ActiveSample(
                                samples[0],
                                samples[1],
                                p_instrument,
                                this.preset,
                                this.event
                            )
                        )
                    } else {
                        output.add(
                            ActiveSample(
                                samples[1],
                                samples[0],
                                p_instrument,
                                this.preset,
                                this.event
                            )
                        )
                    }
                } else {
                    output.add(
                        ActiveSample(
                            samples[0],
                            p_instrument,
                            this.preset,
                            this.event
                        )
                    )
                }
            }
        }
        return output
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

    fun play() {
        for (sample in this.active_samples) {
            sample.play(this.event.velocity)

        }
    }
}


class MIDIPlaybackDevice(var context: Context, var soundFont: SoundFont): VirtualMIDIDevice() {
    private val active_handles = HashMap<Pair<Int, Int>, ActiveNoteHandle>()
    private val preset_channel_map = HashMap<Int, Int>()
    private val loaded_presets = HashMap<Pair<Int, Int>, Preset>()
    private val decaying_handles = HashMap<Long, Pair<ActiveNoteHandle, Int>>()
    private val channels_turning_off = mutableSetOf<Int>()

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
        val currently_active_sample: ActiveNoteHandle? = this.active_handles[Pair(event.note, event.channel)]
        currently_active_sample?.stop()

        // TODO: Kinda kludgey. if AllSoundOff is being applied, dont add a new note
        if (this.channels_turning_off.contains(event.channel)) {
            return
        }

        // TODO: Handle Bank
        val bank = if (event.channel == 9) {
            128
        } else {
            0
        }

        val preset = this.loaded_presets[Pair(bank, this.get_channel_preset(event.channel))]!!
        try {
            val active_handle = ActiveNoteHandle(event, preset)
            this.active_handles[Pair(event.note, event.channel)] = active_handle
            active_handle.play()
        } catch (e: Exception) {

        }
    }

    override fun onNoteOff(event: NoteOff) {
        if (!this.channels_turning_off.contains(event.channel)) {
            val handle = this.active_handles.remove(Pair(event.note, event.channel)) ?: return
            val ttl = handle.stop()
            val ts = System.currentTimeMillis()
            if (ttl > 0) {
                this.decaying_handles[ts] = Pair(handle, event.channel)
                thread {
                    Thread.sleep(ttl)
                    if (!this.channels_turning_off.contains(event.channel)) {
                        this.decaying_handles.remove(ts)
                    }
                }
            }
        }
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

    override fun onAllSoundOff(event: AllSoundOff) {
        this.channels_turning_off.add(event.channel)
        // Keep trying to turn off until succeeds
        while (true) {
            val to_pop = mutableSetOf<Long>()
            try {
                for ((key, handle) in this.active_handles.filterKeys { k -> event.channel == k.second }) {
                    handle.stop(true)
                    this.active_handles.remove(key)
                }

                // toList because these are volite and the handles may have been stopped already
                for ((ts, pair) in this.decaying_handles.filterValues { v -> v.second == event.channel }) {
                    pair.first.stop(true)
                    to_pop.add(ts)
                }
            } catch (e: java.util.ConcurrentModificationException) {
                continue
            }

            for (ts in to_pop) {
                this.decaying_handles.remove(ts)
            }

            break
        }

        this.channels_turning_off.remove(event.channel)
    }
}

