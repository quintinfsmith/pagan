package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOn79
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class MidiConverter(var sample_handle_manager: SampleHandleManager) {
    interface ExporterEventHandler {
        abstract fun on_start()
        abstract fun on_complete()
        abstract fun on_cancel()
        abstract fun on_progress_update(progress: Double)
    }
    internal var wave_generator = WaveGenerator(sample_handle_manager)
    var cancel_flagged = false
    var generating = false
    var approximate_frame_count: Int = 0

    internal fun parse_midi(midi: Midi) {
        var start_frame = this.wave_generator.frame
        var ticks_per_beat = (500_000 / midi.get_ppqn())
        var frames_per_tick = (ticks_per_beat * this.sample_handle_manager.sample_rate) / 1_000_000
        var last_tick = 0
        for ((tick, events) in midi.get_all_events_grouped()) {
            last_tick = tick
            val tick_frame = (tick * frames_per_tick) + start_frame
            this.wave_generator.place_events(events, tick_frame)

            // Need to handle some functions so the sample handles are created before the playback
            // & Need to set Tempo
            for (event in events) {
                when (event) {
                    is ProgramChange -> {
                        this.sample_handle_manager.change_program(event.channel, event.get_program())
                    }
                    is BankSelect -> {
                        this.sample_handle_manager.select_bank(event.channel, event.value)
                    }
                    is NoteOn -> {
                        this.sample_handle_manager.gen_sample_handles(event)
                    }
                    is NoteOn79 -> {
                        this.sample_handle_manager.gen_sample_handles(event)
                    }
                    is SetTempo -> {
                        ticks_per_beat = (event.get_uspqn() / midi.get_ppqn())
                        frames_per_tick = (ticks_per_beat * this.sample_handle_manager.sample_rate) / 1_000_000
                    }
                    is SongPositionPointer -> { }
                }
            }
        }

        val tick_frame = ((last_tick) * frames_per_tick) + start_frame
        this.approximate_frame_count = tick_frame
    }

    // Tmp_file is a Kludge until I can figure out how to quickly precalculate file sizes
    fun export_wav(midi: Midi, target_file: File, handler: ExporterEventHandler) {
        handler.on_start()
        this.generating = true
        this.cancel_flagged = false
        this.parse_midi(midi)
        val data_chunks = mutableListOf<ShortArray>()
        val total_chunk_count = this@MidiConverter.approximate_frame_count.toDouble() / this@MidiConverter.sample_handle_manager.buffer_size
        var chunk_count = 0.0
        val min_delta = 500

        var current_ts = System.currentTimeMillis()
        while (!this.cancel_flagged) {
            try {
                val chunk = try {
                    wave_generator.generate(sample_handle_manager.buffer_size)
                } catch (e: WaveGenerator.EmptyException) {
                    ShortArray(sample_handle_manager.buffer_size * 2)
                }
                data_chunks.add(chunk)
                val now = System.currentTimeMillis()
                if (now - current_ts > min_delta) {
                    handler.on_progress_update(chunk_count / total_chunk_count)
                    current_ts = now
                }
                chunk_count += 1.0
            } catch (e: Exception) {
                break
            }
        }

        this.wave_generator.frame = 0

        val output_stream = FileOutputStream(target_file)
        val buffered_output_stream = BufferedOutputStream(output_stream)
        val data_output_stream = DataOutputStream(buffered_output_stream)
        val data_byte_count = data_chunks.size * sample_handle_manager.buffer_size * 4

        if (!this.cancel_flagged) {
            // 00 Riff
            output_stream.write("RIFF".toByteArray(), 0, 4)
            // 04 File size
            data_output_stream.writeInt(Integer.reverseBytes(4 + 24 + 8 + data_byte_count))
            // 08 WAVE
            data_output_stream.writeBytes("WAVE")

            // 12 'fmt '
            data_output_stream.writeBytes("fmt ")
            // 16 chunk size (always 16)
            data_output_stream.writeInt(Integer.reverseBytes(16))
            // 20 (WAVE_FORMAT_PCM code == 1)
            data_output_stream.writeShort(0x0100)
            // 22 Channel Count
            data_output_stream.writeShort(0x0200)
            // 24 Sample rate
            data_output_stream.writeInt(Integer.reverseBytes(sample_handle_manager.sample_rate))
            // 28 byte rate
            data_output_stream.writeInt(Integer.reverseBytes(sample_handle_manager.sample_rate * 2))
            // 32 Block Alignment
            data_output_stream.writeByte(0x04)
            data_output_stream.writeByte(0x00)
            // 34 Bits per sample
            data_output_stream.writeByte(0x10)
            data_output_stream.writeByte(0x00)
            // 36 "data"
            data_output_stream.writeBytes("data")
            // 40 Chunk size
            data_output_stream.writeInt(Integer.reverseBytes(data_byte_count))
        }

        for (chunk in data_chunks) {
            if (this.cancel_flagged) {
                break
            }
            for (b in chunk) {
                data_output_stream.writeByte((b.toInt() and 0xFF))
                data_output_stream.writeByte((b.toInt() shr 8))
            }
        }

        data_output_stream.close()
        buffered_output_stream.close()
        output_stream.close()

        this.generating = false
        if (this.cancel_flagged) {
            handler.on_cancel()
        } else {
            handler.on_complete()
        }
    }
}