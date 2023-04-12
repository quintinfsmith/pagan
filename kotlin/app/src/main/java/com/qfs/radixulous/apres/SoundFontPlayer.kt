package com.qfs.radixulous.apres

import android.content.Context
import android.media.*
import android.util.Log
import com.qfs.radixulous.apres.riffreader.toUInt
import kotlin.concurrent.thread
import kotlin.experimental.and
import kotlin.math.pow

class Locker() {
    var gen_value = 0
    var lock_value = 0
    private val max_value = 0xFFFFFFFF

    fun pick_number(): Int {
        val output = this.gen_value
        this.gen_value += 1
        if (this.gen_value > this.max_value) {
            this.gen_value = 0
        }
        return output
    }

    fun enter_queue() {
        var waiting_number = this.pick_number()
        while (waiting_number != this.lock_value) {
            Thread.sleep(5)
        }
    }

    fun release() {
        this.lock_value += 1
    }
}

class AudioTrackHandle() {
    class Listener(private var handle: AudioTrackHandle): AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(p0: AudioTrack?) {
            //
        }
        override fun onPeriodicNotification(audioTrack: AudioTrack?) {
            if (audioTrack != null) {
                this.handle.write_next_chunk()
            }
        }
    }

    private var sample_rate = 22050
    private var buffer_size_in_bytes: Int
    private var buffer_size_in_frames: Int
    private var chunk_size_in_frames: Int
    private var chunk_size_in_bytes: Int
    private var chunk_ratio: Int = 3

    private var audioTrack: AudioTrack
    private var sample_handles = HashMap<Int, SampleHandle>()
    private var sample_locker = Locker()
    private var keygen: Int = 0
    private val maxkey = 0xFFFFFFFF

    private var is_playing = false

    init {
        Log.d("AAA", "AudioTrackHandle Init() Start")
        this.buffer_size_in_bytes = AudioTrack.getMinBufferSize(
            this.sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        ) * 2

        //while (this.buffer_size_in_bytes < this.sample_rate) {
        //    this.buffer_size_in_bytes *= 2
        //}

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
                    .setSampleRate(this.sample_rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(this.buffer_size_in_bytes)
            .build()

        //val playbacklistener = Listener(this)
        //this.audioTrack.setPlaybackPositionUpdateListener( playbacklistener )

        //this.audioTrack.positionNotificationPeriod = this.buffer_size_in_frames
    }

    private fun play() {
        if (this.is_playing) {
            return
        }
        thread {
            this.write_loop()
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
    }

    // Generate a sample handle key
    private fun genkey(): Int {
        val output = this.keygen

        this.keygen += 1
        if (this.maxkey <= this.keygen) {
            this.keygen = 0
        }

        return output
    }

    fun add_sample_handle(handle: SampleHandle): Int {
        Log.d("AAA", "ADDing SampleHandle $handle")
        this.sample_locker.enter_queue()
        var newkey = this.genkey()
        this.sample_handles[newkey] = handle
        this.sample_locker.release()
        return newkey
    }

    fun add_sample_handles(handles: Set<SampleHandle>): Set<Int> {
        var try_play = this.sample_handles.isEmpty()
        var output = mutableSetOf<Int>()
        for (handle in handles) {
            output.add(this.add_sample_handle(handle))
        }

        if (try_play) {
            this.play()
        }

        return output
    }


    fun remove_sample_handle(key: Int) {
        this.sample_locker.enter_queue()
        var handle = this.sample_handles.remove(key)
        Log.d("AAA", "Removing ${handle?.event?.note}/${handle?.event?.channel}")
        Log.d("AAA", "Remaining handles: ${this.sample_handles.size}")
        this.sample_locker.release()
    }

    fun release_sample_handle(key: Int) {
        Log.d("AAA", "Releasing $key")
        this.sample_locker.enter_queue()
        this.sample_handles[key]?.release_note()
        this.sample_locker.release()
    }

    fun write_empty_chunk() {
        val use_bytes = ByteArray(this.buffer_size_in_bytes) { _ -> 0 }
        this.audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
    }

    fun write_next_chunk() {
        val use_bytes = ByteArray(this.buffer_size_in_bytes) { _ -> 0 }
        val kill_handles = mutableSetOf<Int>()
        var cut_point: Int? = null

        this.sample_locker.enter_queue()
        var sample_handles = this.sample_handles.toList()
        this.sample_locker.release()

        for (x in 0 until this.buffer_size_in_frames) {
            var left: Short = 0
            var right: Short = 0
            // TODO: straight division of volume is too naive
            var volume_left_d = 0
            var volume_right_d = 0
            for ((key, sample_handle) in sample_handles) {
                if (key in kill_handles) {
                    continue
                }
                val v: Short? = sample_handle.get_next_frame()
                if (v == null) {
                    Log.d("AAA", "Killing $key")
                    kill_handles.add(key)
                    if (kill_handles.size == sample_handles.size && cut_point == null) {
                        cut_point = x
                    }
                    continue
                }

                // TODO: Implement ROM stereo modes
                when (sample_handle.stereo_mode and 7) {
                    1 -> { // mono
                        left = (left + v).toShort()
                        right = (right + v).toShort()
                        volume_left_d += 1
                        volume_right_d += 1
                    }
                    2 -> { // right
                        right = (right + v).toShort()
                        volume_right_d += 1
                    }
                    4 -> { // left
                        left = (left + v).toShort()
                        volume_left_d += 1
                    }
                    else -> {

                    }
                }
            }

            if (volume_left_d > 0) {
                left = (left.toFloat() / volume_left_d.toFloat()).toInt().toShort()
            }
            if (volume_right_d > 0) {
                right = (right.toFloat() / volume_right_d.toFloat()).toInt().toShort()
            }

            if (cut_point == null) {
                use_bytes[(4 * x)] = (right and 0xFF).toByte()
                use_bytes[(4 * x) + 1] = ((toUInt(right) and 0xFF00) shr 8).toByte()

                use_bytes[(4 * x) + 2] = (left and 0xFF).toByte()
                use_bytes[(4 * x) + 3] = ((toUInt(left) and 0xFF00) shr 8).toByte()
            }
        }


        val audioTrack = this.audioTrack

       audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)

        if (cut_point != null) {
            this.is_playing = false
        }

        //if (audioTrack != null && audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
        //    try {
        //    } catch (e: IllegalStateException) {
        //        // Shouldn't need to do anything. the audio track was released and this should stop on its own
        //    }
        //}

        for (key in kill_handles) {
            this.remove_sample_handle(key)
        }
    }

    fun write_loop() {
        this.is_playing = true
        this.audioTrack.play()
        while (this.is_playing) {
            this.write_next_chunk()
        }
        this.audioTrack.stop()
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
    var is_pressed = true

    init {
        println("SAMPLE: ${sample.sample!!.name} R: ${sample.sample!!.sampleRate}")
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            val tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            val requiredPitch = 2F.pow((this.event.note.toFloat() + (tuning_semi + (tuning_cent / 1200))) / 12F)
            this.pitch_shift = requiredPitch / original_pitch
        }

        if (sample.sample!!.sampleRate != 22050) {
            this.pitch_shift *= (22050F / sample.sample!!.sampleRate.toFloat())
        }

        this.data = this.resample(sample.sample!!.data)
        println("DATA: ${this.data.size}")

        this.stereo_mode = sample.sample!!.sampleType
        this.loop_points = if (sample.sampleMode == null || sample.sampleMode!! and 1 == 1) {
            Pair(
                (sample.sample!!.loopStart.toFloat() / this.pitch_shift).toInt(),
                (sample.sample!!.loopEnd.toFloat() / this.pitch_shift).toInt()
            )
        } else {
            null
        }
    }

    fun resample(sample_data: ByteArray): ByteArray {
        // TODO: This is VERY Niave. Look into actual resampling algorithms
        var new_size = (sample_data.size / this.pitch_shift).toInt()
        if (new_size % 2 == 1) {
            new_size -= 1
        }

        var new_sample = ByteArray(new_size) { _ -> 0 }

        for (i in 0 until new_size / 2) {
            var i_offset = ((i * 2).toFloat() * this.pitch_shift).toInt()
            if (i_offset % 2 == 1) {
                i_offset -= 1
            }
            new_sample[i * 2] = sample_data[i_offset]
            new_sample[(i * 2) + 1] = sample_data[i_offset + 1]
        }

        return new_sample
    }

    fun get_next_frame(): Short? {
        if (this.current_position > this.data.size - 2) {
            return null
        }

        val a = toUInt(this.data[this.current_position])
        val b = toUInt(this.data[this.current_position + 1]) * 256
        var frame: Short = (a + b).toShort()

        this.current_position += 2
        if (this.loop_points != null && this.is_pressed) {
            if (this.current_position >= this.loop_points.second) {
                this.current_position = this.loop_points.first
            }
        }

        return frame
    }

    fun release_note() {
        this.is_pressed = false
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
        var keys = this.active_handle_keys[Pair(note, channel)] ?: return
        for (key in keys) {
            this.audio_track_handle.release_sample_handle(key)
        }
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

    override fun onAllSoundOff(event: AllSoundOff) {
        for ((key, handle) in this.active_handle_keys.filterKeys { k -> k.second == event.channel }) {
            this.kill_note(key.first, key.second)
        }
    }

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

