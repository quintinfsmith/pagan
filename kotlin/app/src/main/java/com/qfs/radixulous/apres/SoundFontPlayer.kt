package com.qfs.radixulous.apres

import android.content.Context
import android.media.*
import android.util.Log
import com.qfs.radixulous.apres.riffreader.toUInt
import kotlin.concurrent.thread
import kotlin.math.pow

class Mutex(var timeout: Int = 1000) {
    companion object {
        var locker_id_gen = 0 // Used for debug
        fun new_locker_id(): Int {
            return locker_id_gen++
        }
    }
    var gen_value = 0
    var lock_value = 0
    var locker_id = Mutex.new_locker_id()
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
        var wait = 0
        while (waiting_number != this.lock_value && wait < this.timeout) {
            Thread.sleep(5)
            wait += 5
        }
        if (wait >= this.timeout) {
            Log.d("XXA", "QUEUE @ $waiting_number TIMEOUT")
        }
    }

    fun release() {
        this.lock_value += 1
    }

    fun <T> withLock(callback: () -> T): T {
        this.enter_queue()
        val output: T = callback()
        this.release()

        return output
    }
}
class AudioTrackHandle() {
    companion object {
        const val sample_rate = 44100
    }
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

    private var buffer_size_in_bytes: Int
    private var buffer_size_in_frames: Int
    private var chunk_size_in_frames: Int
    private var chunk_size_in_bytes: Int
    private var chunk_ratio: Int = 3

    private var audioTrack: AudioTrack
    private var sample_handles = HashMap<Int, SampleHandle>()
    private var sample_handles_mutex = Mutex()
    private var keygen: Int = 0
    private val maxkey = 0xFFFFFFFF

    private var is_playing = false
    private var stop_called_from_write = false // play *may*  be called between a write_next_chunk() finding an incomplete chunk and finishing the call

    private var volume_divisor = 3

    init {
        this.buffer_size_in_bytes = AudioTrack.getMinBufferSize(
            AudioTrackHandle.sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        )

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
                    .setSampleRate(AudioTrackHandle.sample_rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(this.buffer_size_in_bytes)
            .build()

        //val playbacklistener = Listener(this)
        //this.audioTrack.setPlaybackPositionUpdateListener( playbacklistener )

        //this.audioTrack.positionNotificationPeriod = this.buffer_size_in_frames
    }

    fun set_volume_divisor(n: Int) {
        this.volume_divisor = n
    }

    fun play() {
        this.stop_called_from_write = false
        if (this.is_playing) {
            return
        }
        this.is_playing = true

        thread {
            this.write_loop()
        }

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
        return this.sample_handles_mutex.withLock {
            var newkey = this.genkey()
            this.sample_handles[newkey] = handle
            newkey
        }
    }

    fun add_sample_handles(handles: Set<SampleHandle>): Set<Int> {
        var output = mutableSetOf<Int>()
        for (handle in handles) {
            output.add(this.add_sample_handle(handle))
        }


        return output
    }

    fun remove_sample_handle(key: Int): SampleHandle? {
        return this.sample_handles_mutex.withLock {
            this.sample_handles.remove(key)
        }
    }

    fun release_sample_handle(key: Int) {
        this.sample_handles_mutex.withLock {
            this.sample_handles[key]?.release_note()
        }
    }

    fun write_empty_chunk() {
        val use_bytes = ByteArray(this.buffer_size_in_bytes) { _ -> 0 }
        this.audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
    }

    fun write_next_chunk() {
        val use_bytes = ByteArray(this.buffer_size_in_bytes) { _ -> 0 }
        val kill_handles = mutableSetOf<Int>()
        var cut_point: Int? = null

        val sample_handles = this.sample_handles_mutex.withLock {
            this.sample_handles.toList()
        }

        if (this.sample_handles.isEmpty()) {
            this.stop_called_from_write = true
        } else {
            for (x in 0 until this.buffer_size_in_frames) {
                val left_values = mutableListOf<Short>()
                val right_values = mutableListOf<Short>()
                for ((key, sample_handle) in sample_handles) {
                    if (key in kill_handles) {
                        continue
                    }
                    val v: Short? = sample_handle.get_next_frame()
                    if (v == null) {
                        kill_handles.add(key)
                        if (kill_handles.size == sample_handles.size && cut_point == null) {
                            this.stop_called_from_write = true
                            cut_point = x
                        }
                        continue
                    }

                    // TODO: Implement ROM stereo modes
                    when (sample_handle.stereo_mode and 7) {
                        1 -> { // mono
                            left_values.add(v)
                            right_values.add(v)
                        }
                        2 -> { // right
                            right_values.add(v)
                        }
                        4 -> { // left
                            left_values.add(v)
                        }
                        else -> {}
                    }
                }

                if (cut_point == null) {
                    val right = right_values.sum() / volume_divisor
                    val left = left_values.sum() / volume_divisor
                    use_bytes[(4 * x)] = (right and 0xFF).toByte()
                    use_bytes[(4 * x) + 1] = ((right and 0xFF00) shr 8).toByte()

                    use_bytes[(4 * x) + 2] = (left and 0xFF).toByte()
                    use_bytes[(4 * x) + 3] = ((left and 0xFF00) shr 8).toByte()
                } else {
                    break
                }
            }

            if (this.audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
                try {
                    this.audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
                } catch (e: IllegalStateException) {
                    // Shouldn't need to do anything. the audio track was released and this should stop on its own
                }
            }
        }

        for (key in kill_handles) {
            this.remove_sample_handle(key)
        }

        if (this.stop_called_from_write) {
            this.is_playing = false
        }
    }


    fun write_loop() {
        this.audioTrack.play()
        var i = 0
        while (this.is_playing) {
            this.write_next_chunk()
        }
        this.audioTrack.stop()
    }
}


class SampleHandle(var event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset) {
    var pitch_shift: Float = 1F
    var current_position: Int = 0
    var decay_position: Int? = null
    val loop_points: Pair<Int, Int>?
    var data: ByteArray
    var stereo_mode: Int
    var is_pressed = true
    var delay_frames: Int = 0
    var current_delay_position: Int = 0
    var attack_byte_count: Int = 0
    var hold_byte_count: Int = 0
    var decay_byte_count: Int = 0
    var sustain_volume: Int = 0 // TODO
    var release_mask: Array<Double>
    var current_release_position: Int = 0

    init {
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            val tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            val requiredPitch = 2F.pow((this.event.note.toFloat() + (tuning_semi + (tuning_cent / 1200))) / 12F)
            this.pitch_shift = requiredPitch / original_pitch
        }

        if (sample.sample!!.sampleRate != AudioTrackHandle.sample_rate) {
            this.pitch_shift *= (sample.sample!!.sampleRate.toFloat() / AudioTrackHandle.sample_rate.toFloat())
        }

        this.data = this.resample(sample.sample!!.data)

        this.stereo_mode = sample.sample!!.sampleType
        this.loop_points = if (sample.sampleMode != null && sample.sampleMode!! and 1 == 1) {
            // Need to be even due to 2-byte words in sample
            var new_start = (sample.sample!!.loopStart.toFloat() / this.pitch_shift).toInt()
            if (new_start % 2 == 1) {
                new_start -= 1
            }

            var new_end = (sample.sample!!.loopEnd.toFloat() / this.pitch_shift).toInt()
            if (new_end % 2 == 1) {
                new_end -= 1
            }

            Pair(new_start, new_end)

        } else {
            null
        }

        var vol_env_delay: Double = preset.global_zone?.vol_env_delay
            ?: instrument.instrument?.global_sample?.vol_env_delay
            ?: instrument.vol_env_delay
            ?: sample.vol_env_delay
            ?: 0.0
        this.delay_frames = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_delay ) / 4.0).toInt()

        this.attack_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * (sample.vol_env_attack ?: instrument.vol_env_attack ?: 0.0)) / 2.0).toInt()
        var vol_env_attack: Double = preset.global_zone?.vol_env_attack
            ?: instrument.instrument?.global_sample?.vol_env_attack
            ?: instrument.vol_env_attack
            ?: sample.vol_env_attack
            ?: 0.0
        this.attack_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_attack ) / 2.0).toInt()

        var vol_env_hold: Double = preset.global_zone?.vol_env_hold
            ?: instrument.instrument?.global_sample?.vol_env_hold
            ?: instrument.vol_env_hold
            ?: sample.vol_env_hold
            ?: 0.0
        this.hold_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_hold ) / 2.0).toInt()

        var vol_env_decay: Double = preset.global_zone?.vol_env_decay
            ?: instrument.instrument?.global_sample?.vol_env_decay
            ?: instrument.vol_env_decay
            ?: sample.vol_env_decay
            ?: 0.0
        this.decay_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_decay ) / 2.0).toInt()

        var vol_env_release: Double = preset.global_zone?.vol_env_release
            ?: instrument.instrument?.global_sample?.vol_env_release
            ?: instrument.vol_env_release
            ?: sample.vol_env_release
            ?: 0.0
        var release_mask_size = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_release) / 4.0).toInt()
        this.release_mask = Array(release_mask_size) {
            i -> (release_mask_size - i - 1).toDouble() / release_mask_size.toDouble()
        }

        this.current_release_position = 0
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
        if (this.current_delay_position < this.delay_frames) {
            var output = 0.toShort()
            this.current_delay_position += 1
            return output
        }

        if (this.current_position > this.data.size - 2) {
            return null
        }

        val a = toUInt(this.data[this.current_position])
        val b = toUInt(this.data[this.current_position + 1]) * 256
        var frame: Short = (a + b).toShort()

        this.current_position += 2

        if (! this.is_pressed) {
            if (this.current_release_position < this.release_mask.size) {
                frame = (frame * this.release_mask[this.current_release_position]).toInt().toShort()
                this.current_release_position += 1
            } else {
                return null
            }
        } else if (this.loop_points != null) {
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
    private val active_handle_mutex = Mutex()
    private var sample_handle_queue = mutableSetOf<Pair<NoteOn,Set<SampleHandle>>>()
    private var enqueueing_sample_handles = false

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
        val keys = this.active_handle_mutex.withLock {
            this.active_handle_keys[Pair(note, channel)]
        } ?: return

        for (key in keys) {
            this.audio_track_handle.release_sample_handle(key)
        }
    }

    private fun kill_note(note: Int, channel: Int) {
        val keys = this.active_handle_mutex.withLock {
            this.active_handle_keys.remove(Pair(note, channel))
        } ?: return

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
        this.active_handle_mutex.withLock {
            this.active_handle_keys[Pair(event.note, event.channel)] = this.audio_track_handle.add_sample_handles(this.gen_sample_handles(event, preset))
        }
        this.audio_track_handle.play()
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
        val to_kill = mutableListOf<Int>()
        this.active_handle_mutex.withLock {
            for ((key, _) in this.active_handle_keys.filterKeys { k -> k.second == event.channel }) {
                to_kill.add(key.first)
            }
        }

        for (note in to_kill) {
            this.kill_note(note, event.channel)
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

    fun set_active_line_count(n: Int) {
        this.audio_track_handle.set_volume_divisor(n)
    }
}

