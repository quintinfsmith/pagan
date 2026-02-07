/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
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
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

class MultiExporterEventHandler(var activity: ComponentActivityEditor, val state_model: ViewModelEditorState, var total_count: Int): WavConverter.ExporterEventHandler {
    var working_y = 0
    var file_uri: Uri? = null
    var cancelled = false
    val timeout_millis = 5000L
    val MAX_PROGRESS = 100

    val notification_manager = NotificationManagerCompat.from(this.activity)

    override fun on_start() {
        if (this.working_y != 0) return

        this.state_model.export_progress.value = 0F
        this.state_model.export_in_progress.value = true

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

        this.activity.runOnUiThread {
            Toast.makeText(this.activity, this.activity.getString(R.string.export_wav_feedback_complete), Toast.LENGTH_SHORT).show()
        }

        this.activity.active_notification = null

        this.state_model.export_in_progress.value = false
        this.state_model.export_progress.value = 0F
    }

    override fun on_cancel() {
        this.cancelled = true
        this.activity.runOnUiThread {
            Toast.makeText(this.activity, this.activity.getString(R.string.export_cancelled), Toast.LENGTH_SHORT).show()
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
        this.state_model.export_in_progress.value = false
        this.state_model.export_progress.value = 0F
    }

    override fun on_progress_update(progress: Double) {
        val progress_relative = (progress + this.working_y) / this.total_count.toDouble()
        val progress_rounded = (progress_relative * this.MAX_PROGRESS).roundToInt()
        this.state_model.export_progress.value = progress_relative.toFloat()

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