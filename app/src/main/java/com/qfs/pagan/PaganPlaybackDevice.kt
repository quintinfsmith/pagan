package com.qfs.pagan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qfs.apres.Midi
import com.qfs.apres.soundfontplayer.FiniteMidiDevice
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.min
class PaganPlaybackDevice(var activity: MainActivity, sample_rate: Int = activity.configuration.sample_rate): FiniteMidiDevice(SampleHandleManager(activity.get_soundfont()!!, sample_rate, buffer_size = sample_rate)) {
    /*
        All of this notification stuff is used with the understanding that the PaganPlaybackDevice
        used to export wavs will be discarded after a single use. It'll need to be cleaned up to
        handle anything more.
     */
    var NOTIFICATION_ID = 0
    val CHANNEL_ID = "com.qfs.pagan"
    var notification_channel: NotificationChannel? = null
    var active_notification: NotificationCompat.Builder? = null
    var export_wav_thread: Job? = null
    var current_relative_beat = 0
    var start_beat = 0
    override fun on_stop() {
        this.activity.restore_playback_state()
    }

    override fun on_start() {
        this.activity.playback_queued = false
        this.activity.runOnUiThread {
            this.activity.loading_reticle_hide()
            this.activity.set_playback_button(R.drawable.ic_baseline_pause_24)
        }
    }

    override fun on_beat(i: Int) {
        var i = this.current_relative_beat++ + this.start_beat
        var opus_manager = this.activity.get_opus_manager()
        if (i >= opus_manager.beat_count) {
            return
        }
        opus_manager.cursor_select_column(i, true)
    }

    override fun on_cancelled() {
        this.activity.restore_playback_state()
    }

    fun play_opus(start_beat: Int) {
        var midi = this.activity.get_opus_manager().get_midi(start_beat)
        this.start_beat = start_beat
        this.current_relative_beat = 0
        this.play_midi(midi)
    }

    fun export_wav_cancel() {
        var builder = this.get_notification()
        if (builder != null) {
            builder.setContentText("Cancelled")
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setTimeoutAfter(5000)
                .clearActions()
            val notification_manager = NotificationManagerCompat.from(this.activity)
            notification_manager.notify(NOTIFICATION_ID, builder.build())
        }
        this.export_wav_thread?.cancel()
    }

    fun get_notification_channel(): NotificationChannel? {
        return if (this.activity.has_notification_permission()) {
            null
        } else if (this.notification_channel == null) {
            val notification_manager = NotificationManagerCompat.from(this.activity)
            // Create the NotificationChannel.
            val name = "Export Wav File Progress"
            val descriptionText = "Converting Pagan project to .wav"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notification_manager.createNotificationChannel(mChannel)
            mChannel
        } else {
            this.notification_channel!!
        }
    }

    fun get_notification(): NotificationCompat.Builder? {
         if (this.activity.has_notification_permission()) {
             null
         }

        if (this.active_notification == null) {
            this.get_notification_channel()
            var cancel_export_flag = "com.qfs.pagan.CANCEL_EXPORT_WAV"
            var pending_cancel_intent = PendingIntent.getBroadcast(
                this.activity,
                0,
                Intent( cancel_export_flag),
                PendingIntent.FLAG_IMMUTABLE
            )

            var builder = NotificationCompat.Builder(this.activity, CHANNEL_ID)
                .setContentTitle("Exporting ${this.activity.get_opus_manager().project_name}")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.mipmap.logo_round)
                .setSilent(true)
                .addAction(R.drawable.baseline_cancel_24, "Cancel", pending_cancel_intent)

            this.active_notification = builder
        }

        return this.active_notification!!
    }

    fun export_wav(midi: Midi, file_descriptor: ParcelFileDescriptor) {
        this.activity.feedback_msg("Exporting to wav")

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

        var notification_manager = NotificationManagerCompat.from(this@PaganPlaybackDevice.activity)
        var builder = this@PaganPlaybackDevice.get_notification()

        runBlocking {
            this@PaganPlaybackDevice.export_wav_thread = launch {
                var data_byte_count = 0
                var ts_deltas = mutableListOf<Int>()
                var est_chunk_count = (this@PaganPlaybackDevice.approximate_frame_count / this@PaganPlaybackDevice.sample_handle_manager.buffer_size)
                var chunk_count = 0

                if (builder != null) {
                    builder.setProgress(est_chunk_count, chunk_count, false)
                    notification_manager.notify(NOTIFICATION_ID, builder.build())
                }

                var notification_ts = System.currentTimeMillis()
                while (true) {
                    ensureActive()
                    try {
                        val g_ts = System.currentTimeMillis()
                        val chunk =
                            this@PaganPlaybackDevice.wave_generator.generate(this@PaganPlaybackDevice.sample_handle_manager.buffer_size)
                        ts_deltas.add((System.currentTimeMillis() - g_ts).toInt())
                        chunk_count += 1

                        for (b in chunk) {
                            data_output_stream.writeByte((b.toInt() and 0xFF))
                            data_output_stream.writeByte((b.toInt() shr 8))
                            data_byte_count += 2
                        }

                        if (builder != null && System.currentTimeMillis() - notification_ts > 500) {
                            builder.setProgress(
                                est_chunk_count,
                                min(est_chunk_count, chunk_count),
                                false
                            )
                            notification_manager.notify(NOTIFICATION_ID, builder.build())
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
                data_output_stream.writeInt(Integer.reverseBytes(this@PaganPlaybackDevice.sample_handle_manager.sample_rate))
                // 28 byte rate
                data_output_stream.writeInt(Integer.reverseBytes(this@PaganPlaybackDevice.sample_handle_manager.sample_rate * 2))
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

                val input_stream = tmp_file.inputStream()
                input_stream.copyTo(data_output_stream)

                input_stream.close()

                if (builder != null) {
                    builder.setContentText("Done")
                        .setProgress(0, 0, false)
                        .setAutoCancel(true)
                        .clearActions()
                        .setTimeoutAfter(5000)
                        .setSilent(false)

                    notification_manager.notify(NOTIFICATION_ID, builder.build())
                }
                (this@PaganPlaybackDevice.activity).feedback_msg("Done Export")
            }
        }

        data_output_stream.close()
        tmp_file.delete()

        this.buffer_delay = original_delay
        this.wave_generator.frame = 0
    }

}