package com.qfs.apres.soundfontplayer

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class WavConverter(val sample_handle_manager: SampleHandleManager) {
    interface ExporterEventHandler {
        abstract fun on_start()
        abstract fun on_complete()
        abstract fun on_cancel()
        abstract fun on_progress_update(progress: Double)
    }
    var cancel_flagged = false
    var generating = false

    // Tmp_file is a Kludge until I can figure out how to quickly precalculate file sizes
    fun export_wav(midi_frame_map: FrameMap, target_file: File, handler: ExporterEventHandler) {
        handler.on_start()
        this.generating = true
        this.cancel_flagged = false
        val sample_rate = this.sample_handle_manager.sample_rate
        val buffer_size = this.sample_handle_manager.buffer_size
        val wave_generator = WaveGenerator(midi_frame_map, sample_rate, buffer_size)
        val data_chunks = mutableListOf<FloatArray>()
        val total_chunk_count = midi_frame_map.get_size().toDouble() / buffer_size
        var chunk_count = 0.0
        val min_delta = 500
        val empty_chunk = FloatArray(buffer_size * 2) { 0f }

        var current_ts = System.currentTimeMillis()
        while (!this.cancel_flagged) {
            try {
                val chunk = try {
                    wave_generator.generate()
                } catch (e: WaveGenerator.EmptyException) {
                    empty_chunk
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

        wave_generator.frame = 0

        val output_stream = FileOutputStream(target_file)
        val buffered_output_stream = BufferedOutputStream(output_stream)
        val data_output_stream = DataOutputStream(buffered_output_stream)
        val data_byte_count = data_chunks.size * buffer_size * 4

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
            data_output_stream.writeInt(Integer.reverseBytes(sample_rate))
            // 28 byte rate
            data_output_stream.writeInt(Integer.reverseBytes(sample_rate * 2))
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
            for (float_value in chunk) {
                val b = (float_value * (Short.MAX_VALUE + 1).toFloat()).toInt()
                data_output_stream.writeByte((b and 0xFF))
                data_output_stream.writeByte((b shr 8))
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
