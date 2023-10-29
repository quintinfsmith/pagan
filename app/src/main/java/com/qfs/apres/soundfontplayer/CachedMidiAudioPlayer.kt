package com.qfs.apres.soundfontplayer

import android.util.Log
import com.qfs.apres.Midi
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.SetTempo
import com.qfs.apres.soundfont.SoundFont
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

open class CachedMidiAudioPlayer(sample_rate: Int, sound_font: SoundFont): MidiPlaybackDevice(
    sample_rate = sample_rate,
    cache_size_limit = 10,
    sound_font = sound_font) {
    var frame_count: Int = 0
    init {
        this.buffer_delay = 5
    }
    private fun parse_midi(midi: Midi) {
        var start_frame = this.wave_generator.frame
        var frames_per_tick = ((500_000 / midi.get_ppqn()) * this.sample_rate) / 1_000_000
        var last_tick = 0
        for ((tick, events) in midi.get_all_events_grouped()) {
            last_tick = tick
            val tick_frame = (tick * frames_per_tick) + start_frame
            this.wave_generator.place_events(events, tick_frame)

            // Need to set Tempo
            for (event in events) {
                when (event) {
                    is SetTempo -> {
                        frames_per_tick = ((event.get_uspqn() / midi.get_ppqn()) * this.sample_rate) / 1_000_000
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
        this.buffer_delay = 1
        this.parse_midi(midi)
        //var tmp_file = File("$path.tmp")
        var file = File(path)
        if (file.exists()){
            file.delete()
        }
        var output_stream: OutputStream = FileOutputStream(file)
        var buffered_output_stream = BufferedOutputStream(output_stream)

        var data_output_stream = DataOutputStream(buffered_output_stream)

        // 00
        data_output_stream.writeBytes("RIFF")
        // 04
        data_output_stream.writeInt(Integer.reverseBytes(4 + 24 + 8 + (this.frame_count * 2)))
        // 08
        data_output_stream.writeBytes("WAVE")

        // 12
        data_output_stream.writeBytes("fmt ")
        // 16
        data_output_stream.writeInt(Integer.reverseBytes(16))
        // 20
        data_output_stream.writeShort(0x0100)
        // 22
        data_output_stream.writeShort(0x0200)
        // 24
        data_output_stream.writeInt(Integer.reverseBytes(this.sample_rate))
        // 28
        data_output_stream.writeInt(Integer.reverseBytes(this.sample_rate * 4))
        // 32
        data_output_stream.writeByte(0x04)
        data_output_stream.writeByte(0x00)
        // 34
        data_output_stream.writeByte(0x01)
        data_output_stream.writeByte(0x00)
        // 36
        data_output_stream.writeBytes("data")
        // 40
        data_output_stream.writeInt(
            Integer.reverseBytes(this.frame_count * 2)
        )

        var frame_count = this.frame_count
        while (frame_count > 0) {
            try {
                for (b in this.wave_generator.generate().first) {
                    data_output_stream.writeByte((b.toInt() and 0xFF))
                    data_output_stream.writeByte((b.toInt() shr 8))
                    frame_count -= 1
                }
            } catch (e: Exception) {
                break
            }
        }

        Log.d("AAA", "DONE !${this.frame_count} - $frame_count")

        data_output_stream.flush()
        data_output_stream.close()


        this.buffer_delay = original_delay
    }

    fun play_midi(midi: Midi) {
        this.parse_midi(midi)
        this.start_playback()
    }

}

