package com.qfs.pagan

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import kotlin.math.roundToInt

class MultiExporterEventHandler(var activity: ComponentActivityEditor, var total_count: Int): WavConverter.ExporterEventHandler {
    var working_y = 0
    var file_uri: Uri? = null
    var cancelled = false
    val timeout_millis = 5000L
    val MAX_PROGRESS = 100

    val notification_manager = NotificationManagerCompat.from(this.activity)

    override fun on_start() {
        if (this.working_y != 0) return

        this.activity.runOnUiThread {
            this.activity.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.INVISIBLE
            this.activity.findViewById<View>(R.id.clExportProgress)?.visibility = View.VISIBLE
            this.activity.findViewById<ProgressBar>(R.id.export_progress_bar).progress = 0
        }

        val builder = this.activity.get_notification() ?: return
        @SuppressLint("MissingPermission")
        if (this.activity.has_notification_permission()) {
            this.notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
        }
    }

    override fun on_complete() {
        if (this.working_y < this.total_count - 1) return

        this.activity.get_notification()?.let { builder ->
            // NON functional ATM, Open file from notification
            val go_to_file_intent = Intent()
            go_to_file_intent.action = Intent.ACTION_VIEW
            go_to_file_intent.setDataAndType(this.file_uri!!, "*/*")

            val pending_go_to_intent = PendingIntent.getActivity(
                this.activity,
                0,
                go_to_file_intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            builder.setContentText(this.activity.getString(R.string.export_wav_notification_complete))
                .clearActions()
                .setAutoCancel(true)
                .setProgress(this.MAX_PROGRESS, 0, false)
                .setTimeoutAfter(this.timeout_millis)
                .setSilent(false)
                .setContentIntent(pending_go_to_intent)

            @SuppressLint("MissingPermission")
            if (this.activity.has_notification_permission()) {
                this.notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
            }
        }

        Toast.makeText(this.activity, this.activity.getString(R.string.export_wav_feedback_complete), Toast.LENGTH_SHORT).show()

        this.activity.runOnUiThread {
            this.activity.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
            this.activity.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
        }
        this.activity.active_notification = null
    }

    override fun on_cancel() {
        this.cancelled = true
        Toast.makeText(this.activity, this.activity.getString(R.string.export_cancelled), Toast.LENGTH_SHORT).show()
        this.activity.runOnUiThread {
            this.activity.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
            this.activity.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
        }

        val builder = this.activity.get_notification() ?: return
        builder.setContentText(this.activity.getString(R.string.export_cancelled))
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setTimeoutAfter(this.timeout_millis)
            .clearActions()

        @SuppressLint("MissingPermission")
        if (this.activity.has_notification_permission()) {
            val notification_manager = NotificationManagerCompat.from(this.activity)
            notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
        }
        this.activity.active_notification = null
    }

    override fun on_progress_update(progress: Double) {
        val progress_rounded = ((progress + this.working_y) * this.MAX_PROGRESS / this.total_count.toDouble()).roundToInt()
        this.activity.runOnUiThread {
            val progress_bar = this.activity.findViewById<ProgressBar>(R.id.export_progress_bar) ?: return@runOnUiThread
            progress_bar.progress = progress_rounded
        }

        val builder = this.activity.get_notification() ?: return
        builder.setProgress(this.MAX_PROGRESS, progress_rounded, false)

        @SuppressLint("MissingPermission")
        if (this.activity.has_notification_permission()) {
            this.notification_manager.notify(
                this.activity.NOTIFICATION_ID,
                builder.build()
            )
        }
    }

    fun update(y: Int, file_uri: Uri) {
        this.working_y = y
        this.file_uri = file_uri
    }
}