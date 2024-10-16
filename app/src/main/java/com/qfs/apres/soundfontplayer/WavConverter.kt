package com.qfs.apres.soundfontplayer

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

// Ended up needing to split the active and cache Midi Players due to different fundemental requirements
open class WavConverter(val sample_handle_manager: SampleHandleManager) {
    interface ExporterEventHandler {
        fun on_start()
        fun on_complete()
        fun on_cancel()
        fun on_progress_update(progress: Double)
    }
    var cancel_flagged = false
    private var _generating = false

    // Tmp_file is a Kludge until I can figure out how to quickly precalculate file sizes
    fun export_wav(midi_frame_map: FrameMap, target_output_stream: DataOutputStream, tmp_file: File, handler: ExporterEventHandler) {
        handler.on_start()
        this._generating = true
        this.cancel_flagged = false
        val sample_rate = this.sample_handle_manager.sample_rate
        val buffer_size = this.sample_handle_manager.buffer_size
        val wave_generator = WaveGenerator(midi_frame_map, sample_rate, buffer_size)
        val total_chunk_count = midi_frame_map.get_size().toDouble() / buffer_size
        var chunk_count = 0
        val min_delta = 500

        val output_stream = FileOutputStream(tmp_file)
        val buffered_output_stream = BufferedOutputStream(output_stream)
        val data_output_stream = DataOutputStream(buffered_output_stream)

        val empty_chunk = FloatArray(buffer_size * 2) { 0F }
        var current_ts = System.currentTimeMillis()
        while (!this.cancel_flagged) {
            val chunk = try {
                wave_generator.generate()
            } catch (e: WaveGenerator.EmptyException) {
                empty_chunk
            } catch (e: WaveGenerator.DeadException) {
                break
            }

            for (float_value in chunk) {
                val b = (float_value * (Short.MAX_VALUE + 1).toFloat()).toInt()
                data_output_stream.writeByte((b and 0xFF))
                data_output_stream.writeByte((b shr 8))
            }

            val now = System.currentTimeMillis()
            if (now - current_ts > min_delta) {
                handler.on_progress_update(chunk_count.toDouble() / total_chunk_count)
                current_ts = now
            }

            chunk_count += 1
        }

        wave_generator.frame = 0

        val data_byte_count = chunk_count * buffer_size * 4

        if (!this.cancel_flagged) {
            // 00 Riff
            target_output_stream.write("RIFF".toByteArray(), 0, 4)
            // 04 File size
            target_output_stream.writeInt(Integer.reverseBytes(4 + 24 + 8 + data_byte_count))
            // 08 WAVE
            target_output_stream.writeBytes("WAVE")

            // 12 'fmt '
            target_output_stream.writeBytes("fmt ")
            // 16 chunk size (always 16)
            target_output_stream.writeInt(Integer.reverseBytes(16))
            // 20 (WAVE_FORMAT_PCM code == 1)
            target_output_stream.writeShort(0x0100)
            // 22 Channel Count
            target_output_stream.writeShort(0x0200)
            // 24 Sample rate
            target_output_stream.writeInt(Integer.reverseBytes(sample_rate))
            // 28 byte rate
            target_output_stream.writeInt(Integer.reverseBytes(sample_rate * 4))
            // 32 Block Alignment
            target_output_stream.writeByte(0x04)
            target_output_stream.writeByte(0x00)
            // 34 Bits per sample
            target_output_stream.writeByte(0x10)
            target_output_stream.writeByte(0x00)
            // 36 "data"
            target_output_stream.writeBytes("data")
            // 40 Chunk size
            target_output_stream.writeInt(Integer.reverseBytes(data_byte_count))
        }

        data_output_stream.close()
        buffered_output_stream.close()
        output_stream.close()

        val input_stream = tmp_file.inputStream()
        input_stream.copyTo(target_output_stream)
        input_stream.close()

        this._generating = false
        if (this.cancel_flagged) {
            handler.on_cancel()
        } else {
            handler.on_complete()
        }
    }
}
