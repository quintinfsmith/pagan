package com.qfs.radixulous.apres

import android.content.Context
import android.media.*
import android.util.Log
import com.qfs.radixulous.apres.riffreader.toUInt
import java.lang.Math.max
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.ceil

class Mutex(var timeout: Int = 100) {
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
        val buffer_size_in_bytes: Int = AudioTrack.getMinBufferSize(
            AudioTrackHandle.sample_rate,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_OUT_STEREO
        )
        val buffer_size_in_frames: Int = buffer_size_in_bytes / 4
        private const val maxkey = 0xFFFFFFFF
    }

    private var audioTrack: AudioTrack = AudioTrack.Builder()
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
        .setBufferSizeInBytes(AudioTrackHandle.buffer_size_in_bytes)
        .build()
    private var sample_handles = HashMap<Int, SampleHandle>()
    private var sample_handles_mutex = Mutex()
    private var keygen: Int = 0

    private var is_playing = false
    private var stop_called_from_write = false // play *may*  be called between a write_next_chunk() finding an incomplete chunk and finishing the call

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
        if (AudioTrackHandle.maxkey <= this.keygen) {
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
        val use_bytes = ByteArray(AudioTrackHandle.buffer_size_in_bytes) { _ -> 0 }
        this.audioTrack.write(use_bytes, 0, use_bytes.size, AudioTrack.WRITE_BLOCKING)
    }

    fun write_next_chunk() {
        val use_bytes = ByteArray(AudioTrackHandle.buffer_size_in_bytes) { _ -> 0 }
        val kill_handles = mutableSetOf<Int>()
        var cut_point: Int? = null

        val sample_handles = this.sample_handles_mutex.withLock {
            this.sample_handles.toList()
        }

        val short_max = Short.MAX_VALUE.toFloat()

        if (this.sample_handles.isEmpty()) {
            this.stop_called_from_write = true
        } else {
            var control_sample_left = IntArray(AudioTrackHandle.buffer_size_in_frames) { _ -> 0 }
            var control_sample_right = IntArray(AudioTrackHandle.buffer_size_in_frames) { _ -> 0 }
            var max_left = 0
            var max_right = 0
            for ((key, sample_handle) in sample_handles) {
                when (sample_handle.stereo_mode and 7) {
                    1 -> { // mono
                        var next_max = sample_handle.get_next_max(AudioTrackHandle.buffer_size_in_frames) ?: continue
                        max_left += next_max
                        max_right += next_max
                    }
                    2 -> { // right
                        max_right += sample_handle.get_next_max(AudioTrackHandle.buffer_size_in_frames) ?: continue
                    }
                    4 -> { // left
                        max_left += sample_handle.get_next_max(AudioTrackHandle.buffer_size_in_frames) ?: continue
                    }
                    else -> {}
                }
            }

            for (x in 0 until AudioTrackHandle.buffer_size_in_frames) {
                var left = 0
                var right = 0
                for ((key, sample_handle) in sample_handles) {
                    if (key in kill_handles) {
                        continue
                    }
                    var v: Short? = sample_handle.get_next_frame()
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
                            left += v
                            right += v
                        }
                        2 -> { // right
                            right += v
                        }
                        4 -> { // left
                            left += v
                        }
                        else -> {}
                    }
                }
                control_sample_left[x] = left
                control_sample_right[x] = right
            }


            var gain_factor_left = if (max_left > Short.MAX_VALUE) {
                short_max / max_left.toFloat()
            } else {
                1F
            }

            var gain_factor_right = if (max_right > Short.MAX_VALUE) {
                short_max / max_right.toFloat()
            } else {
                1F
            }

            for (x in 0 until control_sample_left.size) {
                var right = (control_sample_right[x] * gain_factor_right).toInt()
                var left = (control_sample_left[x] * gain_factor_left).toInt()

                use_bytes[(4 * x)] = (right and 0xFF).toByte()
                use_bytes[(4 * x) + 1] = ((right and 0xFF00) shr 8).toByte()

                use_bytes[(4 * x) + 2] = (left and 0xFF).toByte()
                use_bytes[(4 * x) + 3] = ((left and 0xFF00) shr 8).toByte()
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

class SampleHandleGenerator {
    // Hash ignores velocity since velocity isn't baked into sample data
    data class MapKey(
        var note: Int,
        var sample: Int,
        var instrument: Int,
        var preset: Int
    )

    var sample_data_map = HashMap<MapKey, SampleHandle>()

    fun get(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        var mapkey = MapKey(event.note, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(mapkey)) {
            this.sample_data_map[mapkey] = this.generate_new(event, sample, instrument, preset)
        }
        return SampleHandle(this.sample_data_map[mapkey]!!)
    }

    fun cache_new(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset) {
        var mapkey = MapKey(event.note, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(mapkey)) {
            this.sample_data_map[mapkey] = this.generate_new(event, sample, instrument, preset)
        }
    }

    fun generate_new(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        var pitch_shift = 1F
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            val tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            val requiredPitch = 2F.pow((event.note.toFloat() + (tuning_semi + (tuning_cent / 1200))) / 12F)
            pitch_shift = requiredPitch / original_pitch
        }

        if (sample.sample!!.sampleRate != AudioTrackHandle.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toFloat() / AudioTrackHandle.sample_rate.toFloat())
        }

        var data = this.resample(sample.sample!!.data!!, pitch_shift)



        var vol_env_delay: Double = preset.global_zone?.vol_env_delay
            ?: instrument.instrument?.global_sample?.vol_env_delay
            ?: instrument.vol_env_delay
            ?: sample.vol_env_delay
            ?: 0.0
        var vol_env_attack: Double = preset.global_zone?.vol_env_attack
            ?: instrument.instrument?.global_sample?.vol_env_attack
            ?: instrument.vol_env_attack
            ?: sample.vol_env_attack
            ?: 0.0

        var vol_env_hold: Double = preset.global_zone?.vol_env_hold
            ?: instrument.instrument?.global_sample?.vol_env_hold
            ?: instrument.vol_env_hold
            ?: sample.vol_env_hold
            ?: 0.0

        var vol_env_decay: Double = preset.global_zone?.vol_env_decay
            ?: instrument.instrument?.global_sample?.vol_env_decay
            ?: instrument.vol_env_decay
            ?: sample.vol_env_decay
            ?: 0.0

        var vol_env_release: Double = preset.global_zone?.vol_env_release
            ?: instrument.instrument?.global_sample?.vol_env_release
            ?: instrument.vol_env_release
            ?: sample.vol_env_release
            ?: 0.0
        var release_mask_size = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_release) / 4.0).toInt()

        var divisions = ceil(data.size.toFloat() / (AudioTrackHandle.buffer_size_in_bytes.toFloat() / 2F)).toInt() * 2
        var maximum_map = Array<Int>(divisions) { _ -> 0 }
        for (i in 0 until data.size / 2) {
            val a = toUInt(data[(2 * i)])
            val b = toUInt(data[(2 * i) + 1]) * 256
            var frame = (a + b).toShort().toInt()
            var mapped_position = (i * divisions / (data.size / 2)).toInt()
            maximum_map[mapped_position] = max(abs(frame), maximum_map[mapped_position])
        }

        return SampleHandle(
            data = data,
            stereo_mode = sample.sample!!.sampleType,
            loop_points = if (sample.sampleMode != null && sample.sampleMode!! and 1 == 1) {
                // Need to be even due to 2-byte words in sample
                var new_start = (sample.sample!!.loopStart.toFloat() / pitch_shift).toInt()
                if (new_start % 2 == 1) {
                    new_start -= 1
                }

                var new_end = (sample.sample!!.loopEnd.toFloat() / pitch_shift).toInt()
                if (new_end % 2 == 1) {
                    new_end -= 1
                }

                Pair(new_start, new_end)

            } else {
                null
            },
            delay_frames = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_delay ) / 4.0).toInt(),
            attack_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_attack ) / 2.0).toInt(),
            hold_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_hold ) / 2.0).toInt(),
            decay_byte_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_decay ) / 2.0).toInt(),
            release_mask = Array(release_mask_size) {
                    i -> (release_mask_size - i - 1).toDouble() / release_mask_size.toDouble()
            },
            maximum_map = maximum_map
        )
    }

    fun resample(sample_data: ByteArray, pitch_shift: Float): ByteArray {
        // TODO: This is VERY Niave. Look into actual resampling algorithms
        var new_size = (sample_data.size / pitch_shift).toInt()
        if (new_size % 2 == 1) {
            new_size -= 1
        }

        var new_sample = ByteArray(new_size) { _ -> 0 }

        for (i in 0 until new_size / 2) {
            var i_offset = ((i * 2).toFloat() * pitch_shift).toInt()
            if (i_offset % 2 == 1) {
                i_offset -= 1
            }
            new_sample[i * 2] = sample_data[i_offset]
            new_sample[(i * 2) + 1] = sample_data[i_offset + 1]
        }

        return new_sample
    }

    fun clear_cache() {
        this.sample_data_map.clear()
    }
}

class SampleHandle(
        var data: ByteArray,
        val loop_points: Pair<Int, Int>?,
        var stereo_mode: Int,
        var delay_frames: Int = 0,
        var attack_byte_count: Int = 0,
        var hold_byte_count: Int = 0,
        var decay_byte_count: Int = 0,
        var release_mask: Array<Double>,
        var maximum_map: Array<Int>
    ) {

    constructor(original: SampleHandle): this(
        original.data,
        original.loop_points,
        original.stereo_mode,
        original.delay_frames,
        original.attack_byte_count,
        original.hold_byte_count,
        original.decay_byte_count,
        original.release_mask,
        original.maximum_map
    )

    var is_pressed = true
    var current_position: Int = 0
    var current_attack_position: Int = 0
    var current_hold_position: Int = 0
    var current_decay_position: Int = 0
    var current_delay_position: Int = 0
    var decay_position: Int? = null
    var sustain_volume: Int = 0 // TODO
    var current_release_position: Int = 0
    var current_volume: Double = 0.5
    var bytes_called: Int = 0 // Will not loop like current_position
    // Kludge to handle high tempo songs
    val minimum_duration: Int = (AudioTrackHandle.sample_rate * .2).toInt()

    fun get_max_in_range(x: Int, size: Int): Int {
        var index = x * this.maximum_map.size / (this.data.size / 2)
        var mapped_size =  size * this.maximum_map.size / (this.data.size / 2)
        var output = 0
        for (i in 0 until mapped_size) {
            output = max(output, this.maximum_map[index])
            index = (index + 1) % this.maximum_map.size
        }
        return output
    }

    fun get_next_max(buffer_size: Int): Int {
        return (this.get_max_in_range(this.current_position / 2, buffer_size).toDouble() * this.current_volume).toInt()
    }

    fun get_next_frame(): Short? {
        //if (this.current_delay_position < this.delay_frames) {
        //    var output = 0.toShort()
        //    this.current_delay_position += 1
        //    return output
        //}

        if (this.current_position > this.data.size - 2) {
            return null
        }

        val a = toUInt(this.data[this.current_position])
        val b = toUInt(this.data[this.current_position + 1]) * 256
        var frame: Short = (a + b).toShort()

        frame = (frame.toDouble() * this.current_volume).toInt().toShort()

        this.current_position += 2
        this.bytes_called += 2
        if (this.current_attack_position < this.attack_byte_count) {
            this.current_attack_position += 2
        } else if (this.current_hold_position < this.hold_byte_count) {
            this.current_hold_position += 2
        } else if (this.current_decay_position < this.decay_byte_count) {
            this.current_decay_position += 2
        }

        //if (! this.is_pressed && this.current_decay_position >= this.decay_byte_count) {
        if (! this.is_pressed && this.bytes_called >= this.minimum_duration - this.release_mask.size) {
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
    private val sample_handle_generator = SampleHandleGenerator()

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
        var preset = this.get_preset(event)
        this.active_handle_mutex.withLock {
            this.active_handle_keys[Pair(event.note, event.channel)] = this.audio_track_handle.add_sample_handles(
                this.gen_sample_handles(event, preset)
            )
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
                var new_handle = this.sample_handle_generator.get(event, sample, p_instrument, preset)
                new_handle.current_volume = event.velocity.toDouble() / 128.toDouble()
                output.add( new_handle )
            }
        }
        return output
    }

    fun get_preset(event: NoteOn): Preset {
        // TODO: Handle Bank
        val bank = if (event.channel == 9) {
            128
        } else {
            0
        }
        return this.loaded_presets[Pair(bank, this.get_channel_preset(event.channel))]!!
    }

    fun precache_midi(midi: MIDI) {
        for ((tick, events) in midi.get_all_events_grouped()) {
            for (event in events) {
                if (event is NoteOn) {
                    var preset = this.get_preset(event)
                    val potential_instruments = preset.get_instruments(event.note, event.velocity)
                    for (p_instrument in potential_instruments) {
                        val samples = p_instrument.instrument!!.get_samples(
                            event.note,
                            event.velocity
                        ).toList()
                        for (sample in samples) {
                            this.sample_handle_generator.cache_new(event, sample, p_instrument, preset)
                        }
                    }
                }
            }

        }
    }

    fun clear_sample_cache() {
        this.sample_handle_generator.clear_cache()
    }
}

