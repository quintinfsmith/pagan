package com.qfs.pagan

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor

class SingleExporterEventHandler(val context: ComponentActivityEditor, val uri: Uri, val callback: () -> Unit): WavConverter.ExporterEventHandler {
    val MAX_PROGRESS = 100
    val timeout_millis = 5000L
    val notification_manager = NotificationManagerCompat.from(this.context)

    override fun on_start() {
        this.context.runOnUiThread {
            this.context.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.INVISIBLE
            this.context.findViewById<View>(R.id.clExportProgress)?.visibility = View.VISIBLE
            this.context.findViewById<ProgressBar>(R.id.export_progress_bar).progress = 0
        }
        this.context.feedback_msg(this.context.getString(R.string.export_wav_feedback))
        val builder = this.context.get_notification() ?: return
        @SuppressLint("MissingPermission")
        if (this.context.has_notification_permission()) {
            this.notification_manager.notify(
                this.context.NOTIFICATION_ID,
                builder.build()
            )
        }
    }

    override fun on_complete() {
        this.callback()

        this.context.get_notification()?.let { builder ->
            // NON functional ATM, Open file from notification
            val go_to_file_intent = Intent()
            go_to_file_intent.action = Intent.ACTION_VIEW
            go_to_file_intent.setDataAndType(uri, "*/*")

            val pending_go_to_intent = PendingIntent.getActivity(
                this.context,
                0,
                go_to_file_intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            builder.setContentText(this.context.getString(R.string.export_wav_notification_complete))
                .clearActions()
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .setTimeoutAfter(this.timeout_millis)
                .setSilent(false)
                .setContentIntent(pending_go_to_intent)

            @SuppressLint("MissingPermission")
            if (this.context.has_notification_permission()) {
                this.notification_manager.notify(
                    this.context.NOTIFICATION_ID,
                    builder.build()
                )
            }
        }

        this.context.feedback_msg(this.context.getString(R.string.export_wav_feedback_complete))

        this.context.runOnUiThread {
            this.context.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
            this.context.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
        }
        this.context._active_notification = null
    }

    override fun on_cancel() {
        this.callback()

        this.context.feedback_msg(this.context.getString(R.string.export_cancelled))
        this.context.runOnUiThread {
            this.context.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
            this.context.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
        }

        val builder = this.context.get_notification() ?: return
        builder.setContentText(this.context.getString(R.string.export_cancelled))
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setTimeoutAfter(this.timeout_millis)
            .clearActions()

        @SuppressLint("MissingPermission")
        if (this.context.has_notification_permission()) {
            val notification_manager = NotificationManagerCompat.from(this.context)
            notification_manager.notify(this.context.NOTIFICATION_ID, builder.build())
        }

        this.context._active_notification = null
    }

    override fun on_progress_update(progress: Double) {
        val progress_rounded = (progress * this.MAX_PROGRESS).toInt()
        this.context.runOnUiThread {
            this.context.findViewById<ProgressBar>(R.id.export_progress_bar)?.progress = progress_rounded
        }

        val builder = this.context.get_notification() ?: return
        builder.setProgress(this.MAX_PROGRESS, progress_rounded, false)

        @SuppressLint("MissingPermission")
        if (this.context.has_notification_permission()) {
            this.notification_manager.notify(this.context.NOTIFICATION_ID, builder.build())
        }
    }
}