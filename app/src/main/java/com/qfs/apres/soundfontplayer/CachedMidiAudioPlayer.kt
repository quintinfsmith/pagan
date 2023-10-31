package com.qfs.apres.soundfontplayer

import android.util.Log
import com.qfs.apres.Midi
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.SetTempo
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

open class CachedMidiAudioPlayer(sample_handle_manager: SampleHandleManager): MidiPlaybackDevice(
    sample_handle_manager,
    cache_size_limit = 10) {
    var frame_count: Int = 0
    init {
        this.buffer_delay = 5
    }
    private fun parse_midi(midi: Midi) {
        var start_frame = this.wave_generator.frame
        var frames_per_tick = ((500_000 / midi.get_ppqn()) * this.sample_handle_manager.sample_rate) / 1_000_000
        var last_tick = 0
        for ((tick, events) in midi.get_all_events_grouped()) {
            last_tick = tick
            val tick_frame = (tick * frames_per_tick) + start_frame
            this.wave_generator.place_events(events, tick_frame)

            // Need to set Tempo
            for (event in events) {
                when (event) {
                    is SetTempo -> {
                        frames_per_tick = ((event.get_uspqn() / midi.get_ppqn()) * this.sample_handle_manager.sample_rate) / 1_000_000
                    }
                }
            }
        }
        val tick_frame = (last_tick * frames_per_tick) + start_frame
        this.wave_generator.place_event(MIDIStop(), tick_frame)
        this.frame_count = tick_frame
    }


    fun export_wav(midi: Midi, path: String) {
        var original_delay = this.buffer_delay
        this.buffer_delay = 0
        this.parse_midi(midi)

        var tmp_file = File("$path.tmp")
        if (tmp_file.exists()) {
            tmp_file.delete()
        }

        var output_stream: OutputStream = FileOutputStream(tmp_file)
        var buffered_output_stream = BufferedOutputStream(output_stream)
        var data_output_stream = DataOutputStream(buffered_output_stream)

        var data_byte_count = 0
        var ts_deltas = mutableListOf<Int>()
        var chunk_count = 0
        var full_ts = System.currentTimeMillis()

        while (true) {
            try {
                val g_ts = System.currentTimeMillis()
                val chunk = this.wave_generator.generate(this.sample_handle_manager.buffer_size).first
                ts_deltas.add((System.currentTimeMillis() - g_ts).toInt())
                chunk_count += 1

                for (b in chunk) {
                    data_output_stream.writeByte((b.toInt() and 0xFF))
                    data_output_stream.writeByte((b.toInt() shr 8))
                    data_byte_count += 2
                }
            } catch (e: Exception) {
                break
            }
        }

        Log.d("AAA", "Chunk count: $chunk_count, avg delta = ${ts_deltas.average()}")
        Log.d("AAA", "Total Dur ${System.currentTimeMillis() - full_ts}")

        data_output_stream.close()


        var file = File(path)
        if (file.exists()) {
            file.delete()
        }

        output_stream = FileOutputStream(file)
        buffered_output_stream = BufferedOutputStream(output_stream)
        data_output_stream = DataOutputStream(buffered_output_stream)

        // 00 Riff
        data_output_stream.writeBytes("RIFF")
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
        data_output_stream.writeInt(Integer.reverseBytes(this.sample_handle_manager.sample_rate))
        // 28 byte rate
        data_output_stream.writeInt(Integer.reverseBytes(this.sample_handle_manager.sample_rate * 2))
        // 32 Block Alignment
        data_output_stream.writeByte(0x04)
        data_output_stream.writeByte(0x00)
        // 34 Bits per sample
        data_output_stream.writeByte(0x10)
        data_output_stream.writeByte(0x00)
        // 36 "data"
        data_output_stream.writeBytes("data")
        // 40 Chunk size
        data_output_stream.writeInt( Integer.reverseBytes(data_byte_count) )

        val input_stream = tmp_file.inputStream()
        input_stream.copyTo(data_output_stream)

        input_stream.close()
        data_output_stream.close()

        tmp_file.delete()

        this.buffer_delay = original_delay
        this.wave_generator.frame = 0
    }

    fun play_midi(midi: Midi) {
        this.parse_midi(midi)
        this.start_playback()
    }
}

