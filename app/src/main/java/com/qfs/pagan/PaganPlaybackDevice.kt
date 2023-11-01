package com.qfs.pagan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qfs.apres.Midi
import com.qfs.apres.soundfontplayer.CachedMidiAudioPlayer
import com.qfs.apres.soundfontplayer.SampleHandleManager
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.min

class PaganPlaybackDevice(var activity: MainActivity, sample_rate: Int = activity.configuration.sample_rate): CachedMidiAudioPlayer(SampleHandleManager(activity.get_soundfont()!!, sample_rate)) {
    val CHANNEL_ID = "EXPORTWAV"
    override fun on_stop() {
        this.activity.runOnUiThread {
            this.activity.playback_stop()
        }
    }
    override fun on_beat_signal(beat: Int) {
        this.activity.runOnUiThread {
            this.activity.get_opus_manager().cursor_select_column(beat, true)
        }
    }

    fun export_wav(midi: Midi, file_descriptor: ParcelFileDescriptor) {
        var notification_manager = NotificationManagerCompat.from(this.activity)

        // Create the NotificationChannel.
        val name = "export_wave"
        val descriptionText = "exporting wave in pagan opus editor"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        notification_manager.createNotificationChannel(mChannel)

        var original_delay = this.buffer_delay
        this.buffer_delay = 0
        this.parse_midi(midi)

        var tmp_file = File("${this.activity.filesDir}/.tmp_wav_data")
        if (tmp_file.exists()) {
            tmp_file.delete()
        }

        var output_stream: OutputStream = FileOutputStream(tmp_file)
        var buffered_output_stream = BufferedOutputStream(output_stream)
        var data_output_stream = DataOutputStream(buffered_output_stream)

        var data_byte_count = 0
        var ts_deltas = mutableListOf<Int>()
        var est_chunk_count = (this.frame_count / this.sample_handle_manager.buffer_size)
        var chunk_count = 0

        var builder = NotificationCompat.Builder(this.activity, "CUNT")
            .setContentTitle("Exporting ${this.activity.get_opus_manager().project_name}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_baseline_pause_24)
            .setSilent(true)

        var notification_id = 0
        builder.setProgress(est_chunk_count, chunk_count, false)
        notification_manager.notify(notification_id, builder.build())

        var notification_ts = System.currentTimeMillis()
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
                if (System.currentTimeMillis() - notification_ts > 200) {
                    builder.setProgress(est_chunk_count, min(est_chunk_count, chunk_count), false)
                    notification_manager.notify(notification_id, builder.build())
                    notification_ts = System.currentTimeMillis()
                }

            } catch (e: Exception) {
                break
            }
        }

        data_output_stream.close()


        output_stream = FileOutputStream(file_descriptor.fileDescriptor)
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


        builder.setContentText("Done")
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setTimeoutAfter(5000)
            .setSilent(false)

        notification_manager.notify(notification_id, builder.build())

    }
}