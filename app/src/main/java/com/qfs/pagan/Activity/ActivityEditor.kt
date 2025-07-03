package com.qfs.pagan.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModel
import com.google.android.material.button.MaterialButton
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.MidiController
import com.qfs.apres.MidiPlayer
import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.Riff
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.ChannelOptionAdapter
import com.qfs.pagan.ChannelOptionRecycler
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.ContextMenu.ContextMenuChannel
import com.qfs.pagan.ContextMenu.ContextMenuColumn
import com.qfs.pagan.ContextMenu.ContextMenuControlLeaf
import com.qfs.pagan.ContextMenu.ContextMenuControlLeafB
import com.qfs.pagan.ContextMenu.ContextMenuControlLine
import com.qfs.pagan.ContextMenu.ContextMenuLeaf
import com.qfs.pagan.ContextMenu.ContextMenuLeafPercussion
import com.qfs.pagan.ContextMenu.ContextMenuLine
import com.qfs.pagan.ContextMenu.ContextMenuRange
import com.qfs.pagan.ContextMenu.ContextMenuView
import com.qfs.pagan.ControlWidgetPan
import com.qfs.pagan.ControlWidgetReverb
import com.qfs.pagan.ControlWidgetTempo
import com.qfs.pagan.ControlWidgetVolume
import com.qfs.pagan.EditorTable
import com.qfs.pagan.FeedbackDevice
import com.qfs.pagan.HexEditText
import com.qfs.pagan.MidiFeedbackDispatcher
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganActivity
import com.qfs.pagan.PaganBroadcastReceiver
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PlaybackDevice
import com.qfs.pagan.PlaybackFrameMap
import com.qfs.pagan.R
import com.qfs.pagan.RangedFloatInput
import com.qfs.pagan.RangedIntegerInput
import com.qfs.pagan.TuningMapRecycler
import com.qfs.pagan.TuningMapRecyclerAdapter
import com.qfs.pagan.databinding.ActivityMainBinding
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusChannelAbstract
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLineAbstract
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusPanEvent
import com.qfs.pagan.opusmanager.OpusReverbEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.view.isGone

class ActivityEditor : PaganActivity() {
    companion object {
        init {
            System.loadLibrary("pagan")
        }
    }

    enum class PlaybackState {
        NotReady,
        Ready,
        Playing,
        Queued,
        Stopping
    }

    class MainViewModel: ViewModel() {
        var export_handle: WavConverter? = null
        var action_interface = ActionTracker()
        var opus_manager = OpusLayerInterface()

        fun export_wav(
            opus_manager: OpusLayerBase,
            sample_handle_manager: SampleHandleManager,
            target_output_stream: DataOutputStream,
            tmp_file: File, configuration: PaganConfiguration? = null,
            handler: WavConverter.ExporterEventHandler,
            ignore_global_effects: Boolean = false,
            ignore_channel_effects: Boolean = false,
            ignore_line_effects: Boolean = false,
        ) {
            val frame_map = PlaybackFrameMap(opus_manager, sample_handle_manager)
            frame_map.clip_same_line_release = configuration?.clip_same_line_release != false
            frame_map.parse_opus(
                ignore_global_effects,
                ignore_channel_effects,
                ignore_line_effects
            )

            val start_frame = frame_map.get_marked_frames()[0]

            // Prebuild the first buffer's worth of sample handles, the rest happen in the get_new_handles()
            for (i in start_frame .. start_frame + sample_handle_manager.buffer_size) {
                frame_map.check_frame(i)
            }

            this.export_handle = WavConverter(sample_handle_manager)
            this.export_handle?.export_wav(frame_map, target_output_stream, tmp_file, handler)
            this.export_handle = null
        }

        fun cancel_export() {
            val handle = this.export_handle ?: return
            handle.cancel_flagged = true
        }

        fun is_exporting(): Boolean {
            return this.export_handle != null
        }
    }

    val view_model: MainViewModel by viewModels()
    private var initial_load = true // Used to prevent save dialog from popping up on first load/new/import
    // flag to indicate that the landing page has been navigated away from for navigation management
    private var _integer_dialog_defaults = HashMap<String, Int>()
    private var _float_dialog_defaults = HashMap<String, Float>()
    var active_percussion_names = HashMap<Int, HashMap<Int, String>>()

    private var _virtual_input_device = MidiPlayer()
    private lateinit var _midi_interface: MidiController
    private var _soundfont: SoundFont? = null
    internal var _soundfont_supported_instrument_names = HashMap<Pair<Int, Int>, String>()
    private var _sample_handle_manager: SampleHandleManager? = null
    private var _feedback_sample_manager: SampleHandleManager? = null
    private var _midi_playback_device: PlaybackDevice? = null
    private var _midi_feedback_dispatcher = MidiFeedbackDispatcher()

    private lateinit var _binding: ActivityMainBinding
    private var _options_menu: Menu? = null
    var playback_state_soundfont: PlaybackState = PlaybackState.NotReady
    var playback_state_midi: PlaybackState = PlaybackState.NotReady
    private var _forced_title_text: String? = null
    private val _temporary_feedback_devices = Array<FeedbackDevice?>(4) {
        null
    }
    private var _current_feedback_device: Int = 0
    private var _blocker_scroll_y: Float? = null
    private var broadcast_receiver = PaganBroadcastReceiver()
    private var receiver_intent_filter = IntentFilter("com.qfs.pagan.CANCEL_EXPORT_WAV")

    // Notification shiz -------------------------------------------------
    var NOTIFICATION_ID = 0
    val CHANNEL_ID = "com.qfs.pagan" // TODO: Use String Resource
    private var _notification_channel: NotificationChannel? = null
    private var _active_notification: NotificationCompat.Builder? = null
    // -------------------------------------------------------------------
    var active_context_menu: ContextMenuView? = null

    class MultiExporterEventHandler(var activity: ActivityEditor, var total_count: Int): WavConverter.ExporterEventHandler {
        var working_y = 0
        var file_uri: Uri? = null
        var cancelled = false

        val notification_manager = NotificationManagerCompat.from(this.activity)

        override fun on_start() {
            if (this.working_y != 0) {
                return
            }

            this.activity.runOnUiThread {
                val btnExportProject = this.activity.findViewById<MaterialButton>(R.id.btnExportProject) ?: return@runOnUiThread
                btnExportProject.setIconResource(R.drawable.baseline_cancel_42)
                val llExportProgress = this.activity.findViewById<View>(R.id.llExportProgress) ?: return@runOnUiThread
                llExportProgress.visibility = View.VISIBLE
                val tvExportProgress = this.activity.findViewById<TextView>(R.id.tvExportProgress) ?: return@runOnUiThread
                tvExportProgress.text = "0%"
            }

            val builder = this.activity.get_notification() ?: return
            @SuppressLint("MissingPermission")
            if (this.activity.has_notification_permission()) {
                this.notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
            }
        }

        override fun on_complete() {
            if (this.working_y < this.total_count - 1) {
                return
            }

            val builder = this.activity.get_notification()
            if (builder != null) {
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
                    .setProgress(0, 0, false)
                    .setTimeoutAfter(5000)
                    .setSilent(false)
                    .setContentIntent(pending_go_to_intent)

                @SuppressLint("MissingPermission")
                if (this.activity.has_notification_permission()) {
                    this.notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
                }
            }

            this.activity.feedback_msg(this.activity.getString(R.string.export_wav_feedback_complete))

            this.activity.runOnUiThread {
                val llExportProgress = this.activity.findViewById<View>(R.id.llExportProgress) ?: return@runOnUiThread
                llExportProgress.visibility = View.GONE
                val btnExportProject = this.activity.findViewById<MaterialButton>(R.id.btnExportProject) ?: return@runOnUiThread
                btnExportProject.setIconResource(R.drawable.export)
            }
            this.activity._active_notification = null
        }

        override fun on_cancel() {
            this.cancelled = true
            this.activity.feedback_msg(this.activity.getString(R.string.export_cancelled))
            this.activity.runOnUiThread {
                val llExportProgress = this.activity.findViewById<View>(R.id.llExportProgress) ?: return@runOnUiThread
                llExportProgress.visibility = View.GONE
                val btnExportProject = this.activity.findViewById<MaterialButton>(R.id.btnExportProject) ?: return@runOnUiThread
                btnExportProject.setIconResource(R.drawable.export)
            }

            val builder = this.activity.get_notification() ?: return
            builder.setContentText(this.activity.getString(R.string.export_cancelled))
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setTimeoutAfter(5000)
                .clearActions()

            @SuppressLint("MissingPermission")
            if (this.activity.has_notification_permission()) {
                val notification_manager = NotificationManagerCompat.from(this.activity)
                notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
            }
            this.activity._active_notification = null
        }

        override fun on_progress_update(progress: Double) {
            val progress_rounded = ((progress + this.working_y) * 100.0 / this.total_count.toDouble()).roundToInt()
            this.activity.runOnUiThread {
                val tvExportProgress = this.activity.findViewById<TextView>(R.id.tvExportProgress) ?: return@runOnUiThread
                tvExportProgress.text = this.activity.getString(R.string.label_export_progress, progress_rounded)
            }

            val builder = this.activity.get_notification() ?: return
            builder.setProgress(100, progress_rounded, false)

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

    internal var import_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result?.data?.data?.also { uri ->
                this.handle_uri(uri)
            }
        }
    }

    private var _export_multi_line_wav_intent_launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (this._soundfont == null) {
            // Throw Error. Currently unreachable by ui
            return@registerForActivityResult
        }

        this.getNotificationPermission()
        thread {
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.also { tree_uri ->
                    if (this.view_model.export_handle != null) {
                        return@thread
                    }
                    val directory = DocumentFile.fromTreeUri(this, tree_uri) ?: return@thread
                    val opus_manager_copy = OpusLayerBase()
                    opus_manager_copy.project_change_json(this.get_opus_manager().to_json())

                    var line_count = 0
                    val skip_lines = mutableSetOf<Pair<Int, Int>>()

                    opus_manager_copy.get_all_channels()
                        .forEachIndexed channel_loop@{ i: Int, channel: OpusChannelAbstract<*, *> ->
                            channel.lines.forEachIndexed line_loop@{ j: Int, line: OpusLineAbstract<*> ->
                                if (line.muted || channel.muted) {
                                    skip_lines.add(Pair(i, j))
                                    return@line_loop
                                }

                                var skip = true
                                for (beat in line.beats) {
                                    if (!beat.is_eventless()) {
                                        skip = false
                                        break
                                    }
                                }

                                if (skip) {
                                    skip_lines.add(Pair(i, j))
                                } else {
                                    line_count += 1
                                }
                            }
                        }

                    val export_event_handler = MultiExporterEventHandler(this, line_count)

                    var y = 0
                    var c = 0
                    outer@ for (channel in opus_manager_copy.get_all_channels()) {
                        for (l in channel.lines.indices) {
                            if (skip_lines.contains(Pair(c, l))) {
                                continue
                            }

                            val file = directory.createFile(
                                "audio/wav",
                                getString(R.string.export_wav_lines_filename, c, l)
                            ) ?: continue
                            val file_uri = file.uri

                            /* TMP file is necessary since we can't easily predict the exact frame count. */
                            val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                            if (tmp_file.exists()) {
                                tmp_file.delete()
                            }

                            tmp_file.deleteOnExit()
                            val exporter_sample_handle_manager =
                                SampleHandleManager(this._soundfont!!, 44100, 22050)

                            var c_b = 0
                            for (channel_copy in opus_manager_copy.get_all_channels()) {
                                var l_b = 0
                                for (line_copy in channel_copy.lines) {
                                    if (c_b == c && l_b == l) {
                                        line_copy.unmute()
                                    } else {
                                        line_copy.mute()
                                    }
                                    l_b += 1
                                }
                                c_b += 1
                            }


                            val parcel_file_descriptor =
                                applicationContext.contentResolver.openFileDescriptor(file_uri, "w")
                                    ?: continue@outer
                            val output_stream =
                                FileOutputStream(parcel_file_descriptor.fileDescriptor)
                            val buffered_output_stream = BufferedOutputStream(output_stream)
                            val data_output_buffer = DataOutputStream(buffered_output_stream)

                            export_event_handler.update(y++, file_uri)
                            this.view_model.export_wav(
                                opus_manager_copy,
                                exporter_sample_handle_manager,
                                data_output_buffer,
                                tmp_file,
                                this.configuration,
                                export_event_handler,
                                true,
                                true,
                                false
                            )

                            data_output_buffer.close()
                            buffered_output_stream.close()
                            output_stream.close()
                            parcel_file_descriptor.close()
                            tmp_file.delete()

                            if (export_event_handler.cancelled) {
                                break@outer
                            }
                        }
                        c++
                    }
                }
            }
        }
    }

    private var _export_multi_channel_wav_intent_launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (this._soundfont == null) {
            // Throw Error. Currently unreachable by ui
            return@registerForActivityResult
        }

        this.getNotificationPermission()
        thread {
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.also { tree_uri ->
                    if (this.view_model.export_handle != null) {
                        return@thread
                    }
                    val directory = DocumentFile.fromTreeUri(this, tree_uri) ?: return@thread
                    val opus_manager_copy = OpusLayerBase()
                    opus_manager_copy.project_change_json(this.get_opus_manager().to_json())

                    var channel_count = 0
                    val skip_channels = mutableSetOf<Int>()

                    opus_manager_copy.get_all_channels()
                        .forEachIndexed channel_loop@{ i: Int, channel: OpusChannelAbstract<*, *> ->
                            if (channel.muted) {
                                skip_channels.add(i)
                                return@channel_loop
                            }

                            var skip = true
                            channel.lines.forEachIndexed line_loop@{ j: Int, line: OpusLineAbstract<*> ->
                                if (line.muted || !skip) {
                                    return@line_loop
                                }

                                for (beat in line.beats) {
                                    if (!beat.is_eventless()) {
                                        skip = false
                                        return@line_loop
                                    }
                                }
                            }

                            if (skip) {
                                skip_channels.add(i)
                            } else {
                                channel_count += 1
                            }
                        }

                    val export_event_handler = MultiExporterEventHandler(this, channel_count)

                    var y = 0
                    var c = 0
                    outer@ for (channel in opus_manager_copy.get_all_channels()) {
                        if (skip_channels.contains(c)) {
                            continue
                        }

                        val file = directory.createFile(
                            "audio/wav",
                            getString(R.string.export_wav_channels_filename, c)
                        ) ?: continue
                        val file_uri = file.uri

                        /* TMP file is necessary since we can't easily predict the exact frame count. */
                        val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                        if (tmp_file.exists()) {
                            tmp_file.delete()
                        }

                        tmp_file.deleteOnExit()
                        val exporter_sample_handle_manager =
                            SampleHandleManager(this._soundfont!!, 44100, 22050)

                        var c_b = 0
                        for (channel_copy in opus_manager_copy.get_all_channels()) {
                            if (c_b == c) {
                                channel_copy.unmute()
                            } else {
                                channel_copy.mute()
                            }
                            c_b += 1
                        }


                        val parcel_file_descriptor =
                            applicationContext.contentResolver.openFileDescriptor(file_uri, "w")
                                ?: continue@outer
                        val output_stream = FileOutputStream(parcel_file_descriptor.fileDescriptor)
                        val buffered_output_stream = BufferedOutputStream(output_stream)
                        val data_output_buffer = DataOutputStream(buffered_output_stream)

                        export_event_handler.update(y++, file_uri)
                        this.view_model.export_wav(
                            opus_manager_copy,
                            exporter_sample_handle_manager,
                            data_output_buffer,
                            tmp_file,
                            this.configuration,
                            export_event_handler,
                            true,
                            false,
                            false
                        )

                        data_output_buffer.close()
                        buffered_output_stream.close()
                        output_stream.close()
                        parcel_file_descriptor.close()
                        tmp_file.delete()

                        if (export_event_handler.cancelled) {
                            break@outer
                        }
                        c++
                    }
                }
            }
        }
    }

    private var _export_wav_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (this._soundfont == null) {
            // Throw Error. Currently unreachable by ui
            return@registerForActivityResult
        }

        this.getNotificationPermission()
        thread {
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.also { uri ->
                    if (this.view_model.export_handle != null) {
                        return@thread
                    }

                    /* TMP file is necessary since we can't easily predict the exact frame count. */
                    val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                    if (tmp_file.exists()) {
                        tmp_file.delete()
                    }

                    tmp_file.deleteOnExit()
                    val exporter_sample_handle_manager = SampleHandleManager(
                        this._soundfont!!,
                        44100,
                        22050
                    )

                    val parcel_file_descriptor =
                        applicationContext.contentResolver.openFileDescriptor(uri, "w")
                            ?: return@thread
                    val output_stream = FileOutputStream(parcel_file_descriptor.fileDescriptor)
                    val buffered_output_stream = BufferedOutputStream(output_stream)
                    val data_output_buffer = DataOutputStream(buffered_output_stream)

                    this.view_model.export_wav(
                        this.get_opus_manager(),
                        exporter_sample_handle_manager,
                        data_output_buffer,
                        tmp_file,
                        this.configuration,
                        object : WavConverter.ExporterEventHandler {
                            val notification_manager =
                                NotificationManagerCompat.from(this@ActivityEditor)

                            fun close_buffers() {
                                data_output_buffer.close()
                                buffered_output_stream.close()
                                output_stream.close()
                                parcel_file_descriptor.close()
                                tmp_file.delete()
                            }

                            override fun on_start() {
                                this@ActivityEditor.runOnUiThread {
                                    val btnExportProject =
                                        this@ActivityEditor.findViewById<MaterialButton>(R.id.btnExportProject)
                                            ?: return@runOnUiThread
                                    btnExportProject.setIconResource(R.drawable.baseline_cancel_42)
                                    val llExportProgress =
                                        this@ActivityEditor.findViewById<View>(R.id.llExportProgress)
                                            ?: return@runOnUiThread
                                    llExportProgress.visibility = View.VISIBLE

                                    val tvExportProgress =
                                        this@ActivityEditor.findViewById<TextView>(R.id.tvExportProgress)
                                            ?: return@runOnUiThread
                                    tvExportProgress.text = "0%"
                                }
                                this@ActivityEditor.feedback_msg(this@ActivityEditor.getString(R.string.export_wav_feedback))
                                val builder = this@ActivityEditor.get_notification() ?: return
                                @SuppressLint("MissingPermission")
                                if (this@ActivityEditor.has_notification_permission()) {
                                    this.notification_manager.notify(
                                        this@ActivityEditor.NOTIFICATION_ID,
                                        builder.build()
                                    )
                                }
                            }

                            override fun on_complete() {
                                this.close_buffers()

                                val builder = this@ActivityEditor.get_notification()
                                if (builder != null) {
                                    // NON functional ATM, Open file from notification
                                    val go_to_file_intent = Intent()
                                    go_to_file_intent.action = Intent.ACTION_VIEW
                                    go_to_file_intent.setDataAndType(uri, "*/*")

                                    val pending_go_to_intent = PendingIntent.getActivity(
                                        this@ActivityEditor,
                                        0,
                                        go_to_file_intent,
                                        PendingIntent.FLAG_IMMUTABLE
                                    )

                                    builder.setContentText(this@ActivityEditor.getString(R.string.export_wav_notification_complete))
                                        .clearActions()
                                        .setAutoCancel(true)
                                        .setProgress(0, 0, false)
                                        .setTimeoutAfter(5000)
                                        .setSilent(false)
                                        .setContentIntent(pending_go_to_intent)

                                    @SuppressLint("MissingPermission")
                                    if (this@ActivityEditor.has_notification_permission()) {
                                        this.notification_manager.notify(
                                            this@ActivityEditor.NOTIFICATION_ID,
                                            builder.build()
                                        )
                                    }
                                }

                                this@ActivityEditor.feedback_msg(this@ActivityEditor.getString(R.string.export_wav_feedback_complete))

                                this@ActivityEditor.runOnUiThread {
                                    val llExportProgress =
                                        this@ActivityEditor.findViewById<View>(R.id.llExportProgress)
                                            ?: return@runOnUiThread
                                    llExportProgress.visibility = View.GONE
                                    val btnExportProject =
                                        this@ActivityEditor.findViewById<MaterialButton>(R.id.btnExportProject)
                                            ?: return@runOnUiThread
                                    btnExportProject.setIconResource(R.drawable.export)
                                }
                                this@ActivityEditor._active_notification = null
                            }

                            override fun on_cancel() {
                                this.close_buffers()

                                this@ActivityEditor.feedback_msg(this@ActivityEditor.getString(R.string.export_cancelled))
                                this@ActivityEditor.runOnUiThread {
                                    val llExportProgress =
                                        this@ActivityEditor.findViewById<View>(R.id.llExportProgress)
                                            ?: return@runOnUiThread
                                    llExportProgress.visibility = View.GONE
                                    val btnExportProject =
                                        this@ActivityEditor.findViewById<MaterialButton>(R.id.btnExportProject)
                                            ?: return@runOnUiThread
                                    btnExportProject.setIconResource(R.drawable.export)
                                }

                                val builder = this@ActivityEditor.get_notification() ?: return
                                builder.setContentText(this@ActivityEditor.getString(R.string.export_cancelled))
                                    .setProgress(0, 0, false)
                                    .setAutoCancel(true)
                                    .setTimeoutAfter(5000)
                                    .clearActions()

                                @SuppressLint("MissingPermission")
                                if (this@ActivityEditor.has_notification_permission()) {
                                    val notification_manager =
                                        NotificationManagerCompat.from(this@ActivityEditor)
                                    notification_manager.notify(
                                        this@ActivityEditor.NOTIFICATION_ID,
                                        builder.build()
                                    )
                                }
                                this@ActivityEditor._active_notification = null
                            }

                            override fun on_progress_update(progress: Double) {
                                val progress_rounded = (progress * 100.0).roundToInt()
                                this@ActivityEditor.runOnUiThread {
                                    val tvExportProgress =
                                        this@ActivityEditor.findViewById<TextView>(R.id.tvExportProgress)
                                            ?: return@runOnUiThread
                                    tvExportProgress.text =
                                        getString(R.string.label_export_progress, progress_rounded)
                                }

                                val builder = this@ActivityEditor.get_notification() ?: return
                                builder.setProgress(100, progress_rounded, false)

                                @SuppressLint("MissingPermission")
                                if (this@ActivityEditor.has_notification_permission()) {
                                    this.notification_manager.notify(
                                        this@ActivityEditor.NOTIFICATION_ID,
                                        builder.build()
                                    )
                                }
                            }
                        })

                    exporter_sample_handle_manager.destroy()
                }
            }
        }
    }

    private var _export_project_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val opus_manager = this.get_opus_manager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    val json_string = opus_manager.to_json().to_string()
                    FileOutputStream(it.fileDescriptor).write(json_string.toByteArray())
                    this.feedback_msg(getString(R.string.feedback_exported))
                }
            }
        }
    }

    private var _export_midi_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val opus_manager = this.get_opus_manager()
            result?.data?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                    this.feedback_msg(getString(R.string.feedback_exported_to_midi))
                }
            }
        }
    }

    override fun onPause() {
        this.playback_stop()
        this.unregisterReceiver(this.broadcast_receiver)
        this.playback_stop_midi_output()
        this._midi_interface.close_connected_devices()
        this._binding.toolbar.hideOverflowMenu()
        super.onPause()
    }

    // TODO: Rewrite this withour fragment
    //override fun onKeyDown(key_code: Int, event: KeyEvent?): Boolean {
    //    val active_fragment = this.get_active_fragment()
    //    val cancel_super = if (event != null) {
    //        when (active_fragment) {
    //            is FragmentEditor ->
    //                try {
    //                    active_fragment.keyboard_input_interface?.input(key_code, event) ?: false
    //                } catch (e: Exception) {
    //                    true
    //                }
    //            else -> false
    //        }
    //    } else {
    //        false
    //    }

    //    return if (cancel_super) {
    //        true
    //    } else {
    //        super.onKeyDown(key_code, event)
    //    }
    //}

    override fun onResume() {
        super.onResume()
        this.drawer_lock()

        registerReceiver(
            this.broadcast_receiver,
            this.receiver_intent_filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RECEIVER_NOT_EXPORTED
            } else {
                0
            }
        )

        if (this._midi_playback_device != null) {
            this.playback_state_soundfont = PlaybackState.Ready
        }

        if (this.is_connected_to_physical_device()) {
            this.playback_state_midi = PlaybackState.Ready
        }
        this.update_title_text()
    }

    fun delete_backup() {
        File("${applicationInfo.dataDir}/.bkp.json").let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        File("${applicationInfo.dataDir}/.bkp_path").let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }
    fun save_to_backup() {
        val opus_manager = this.get_opus_manager()
        val path = opus_manager.path
        if (path != null) {
            val path_file = File("${applicationInfo.dataDir}/.bkp_path")
            path_file.writeText(path)
        }
        opus_manager.save("${applicationInfo.dataDir}/.bkp.json")

        // saving changes the path, need to change it back
        opus_manager.path = path
    }


    override fun onSaveInstanceState(outState: Bundle) {
        // Can't reliably put json in outstate. there is a size limit
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        outState.putInt("x", editor_table.get_scroll_x())
        outState.putInt("y", editor_table.get_scroll_y())
        this.save_to_backup()
        super.onSaveInstanceState(outState)
    }
    fun refresh(x: Int, y: Int) {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()

        this.get_opus_manager().project_refresh()
        this.runOnUiThread {
            editor_table?.table_ui?.scroll(x, y)
        }

    }

    fun setup_new() {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()
        this.get_opus_manager().project_change_new()
    }

    fun load_project(path: String) {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()
        this.get_opus_manager().load_path(path)
    }

    fun load_from_bkp() {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()

        val opus_manager = this.get_opus_manager()
        val bkp_json_path = "${this.applicationInfo.dataDir}/.bkp.json"
        val bytes = FileInputStream(bkp_json_path).readBytes()
        val backup_path: String = File("${this.applicationInfo.dataDir}/.bkp_path").readText()
        opus_manager.load(bytes, backup_path)
    }

    private fun handle_uri(uri: Uri) {
        val path_string = uri.toString()

        val type: CompatibleFileType? = try {
            this.get_file_type(uri)
        } catch (e: Exception) {
            null
        }

        val inner_callback: ((String) -> Unit) = when (type) {
            CompatibleFileType.Midi1 -> { path_string -> this.import_midi(path_string) }
            CompatibleFileType.Pagan -> { path_string -> this.import_project(path_string) }
            else -> { _ -> throw FileNotFoundException(path_string) }
        }

        this.dialog_save_project {
            thread {
                this.loading_reticle_show()
                this.runOnUiThread {
                    this.findViewById<EditorTable>(R.id.etEditorTable).visibility = View.GONE
                    this.clear_context_menu()
                }

                val fallback_msg = try {
                    inner_callback(path_string)
                    null
                } catch (e: Exception) {
                    when (type) {
                        CompatibleFileType.Midi1 -> getString(R.string.feedback_midi_fail)
                        CompatibleFileType.Pagan -> getString(R.string.feedback_import_fail)
                        null -> getString(R.string.feedback_file_not_found)
                    }
                }

                if (fallback_msg != null) {
                    if (!this.get_opus_manager().is_initialized()) {
                        this.setup_new()
                    }
                    this.feedback_msg(fallback_msg)
                }

                this.loading_reticle_hide()
                this.runOnUiThread {
                    this.clear_forced_title()
                    this.findViewById<EditorTable>(R.id.etEditorTable).visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable ->
            Log.d("pagandebug", "$paramThrowable")
            if (this@ActivityEditor.is_debug_on()) {
                this@ActivityEditor.save_actions()
            }
            this@ActivityEditor.save_to_backup()
            this@ActivityEditor.bkp_crash_report(paramThrowable)

            val ctx = applicationContext
            val pm = ctx.packageManager
            val intent = pm.getLaunchIntentForPackage(ctx.packageName)
            val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
            ctx.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)

        }

        this._midi_interface = object : MidiController(this) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                if (!this@ActivityEditor.update_playback_state_midi(PlaybackState.Ready)) {
                    return
                }

                when (this@ActivityEditor.playback_state_soundfont) {
                    PlaybackState.Playing,
                    PlaybackState.Queued -> {
                        this@ActivityEditor.playback_stop()
                    }

                    else -> { /* pass */ }
                }

                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.update_menu_options()
                    if (!this@ActivityEditor.configuration.allow_midi_playback) {
                        return@runOnUiThread
                    }

                    this@ActivityEditor.setup_project_config_drawer_export_button()

                    val channel_recycler = this@ActivityEditor.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    // Should always be null since this can only be changed from a different menu
                    if (channel_recycler.adapter != null) {
                        val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
                        channel_adapter.notify_soundfont_changed()
                    }
                    this@ActivityEditor.active_percussion_names.clear()
                }

                if (this@ActivityEditor.get_opus_manager().is_tuning_standard()) {
                    this@ActivityEditor.disconnect_feedback_device()
                }
            }

            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                when (this@ActivityEditor.playback_state_midi) {
                    PlaybackState.Playing,
                    PlaybackState.Queued -> {
                        this@ActivityEditor.playback_stop_midi_output()
                    }

                    else -> { /* pass */ }
                }

                // Kludge. need a sleep to give output devices a chance to disconnect
                Thread.sleep(1000)

                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.update_menu_options()
                    if (!this@ActivityEditor.is_connected_to_physical_device()) {
                        this@ActivityEditor.setup_project_config_drawer_export_button()

                        val channel_recycler = this@ActivityEditor.findViewById<ChannelOptionRecycler>(
                            R.id.rvActiveChannels)
                        // Should always be null since this can only be changed from a different menu
                        if (channel_recycler.adapter != null) {
                            val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
                            channel_adapter.notify_soundfont_changed()
                        }

                        this@ActivityEditor.active_percussion_names.clear()
                    }
                }
            }
        }

        if (!this.configuration.allow_midi_playback) {
            this.block_physical_midi_output()
        }

        this._midi_interface.connect_virtual_input_device(this._virtual_input_device)

        // Listens for SongPositionPointer (provided by midi) and scrolls to that beat
        this._midi_interface.connect_virtual_output_device(object : VirtualMidiOutputDevice {
            override fun onSongPositionPointer(event: SongPositionPointer) {
                if (event.get_beat() >= this@ActivityEditor.get_opus_manager().length) {
                    return
                }
                this@ActivityEditor.get_opus_manager().cursor_select_column(event.get_beat())
                // Force scroll here, cursor_select_column doesn't scroll if the column is already visible
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.findViewById<EditorTable>(R.id.etEditorTable)
                        ?.scroll_to_position(x = event.get_beat(), force = true)
                }
            }
        })

        this._midi_interface.connect_virtual_input_device(this._midi_feedback_dispatcher)

        this.requestedOrientation = this.configuration.force_orientation

        this._binding = ActivityMainBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)
        this._binding.root.setBackgroundColor(resources.getColor(R.color.main_bg))

        this.view_model.action_interface.attach_activity(this)
        this.view_model.opus_manager.attach_activity(this)

        val toolbar = this._binding.toolbar
        toolbar.background = null

        toolbar.setOnLongClickListener {
            this.get_action_interface().set_project_name_and_notes()
            true
        }

        //////////////////////////////////////////
        if (this.configuration.soundfont != null) {
            val path = "${this.getExternalFilesDir(null)}/SoundFonts/${this.configuration.soundfont}"
            val sf_file = File(path)
            if (sf_file.exists()) {
                try {
                    this._soundfont = SoundFont(path)
                    this.populate_supported_soundfont_instrument_names()
                    this._sample_handle_manager = SampleHandleManager(
                        this._soundfont!!,
                        this.configuration.sample_rate,
                        this.configuration.sample_rate, // Use Large buffer
                        ignore_lfo = true
                    )

                    this._midi_playback_device = PlaybackDevice(
                        this,
                        this._sample_handle_manager!!,
                        WaveGenerator.StereoMode.Stereo
                    )

                    if (!this.is_connected_to_physical_device()) {
                        val buffer_size = this.configuration.sample_rate / 4
                        this._feedback_sample_manager = SampleHandleManager(
                            this._soundfont!!,
                            this.configuration.sample_rate,
                            buffer_size - 2 + (if (buffer_size % 2 == 0) {
                                2
                            } else {
                                1
                            })
                            //sample_limit = this.configuration.playback_sample_limit,
                            //ignore_envelopes_and_lfo = true
                        )
                    }
                } catch (e: Riff.InvalidRiff) {
                    this.configuration.soundfont = null
                    // Invalid soundfont somehow set
                }
            }
        }

        // this.update_channel_instruments(this.get_opus_manager().channels.size)
        ///////////////////////////////////////////

        val drawer_layout = this.findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        drawer_layout.addDrawerListener(
            object : ActionBarDrawerToggle( this, drawer_layout, R.string.drawer_open, R.string.drawer_close) {
                override fun onDrawerOpened(drawerView: View) {
                    this@ActivityEditor.get_action_interface().track(ActionTracker.TrackedAction.DrawerOpen)
                    val channel_recycler = this@ActivityEditor.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter == null) {
                        ChannelOptionAdapter(
                            this@ActivityEditor.get_opus_manager(),
                            channel_recycler
                        )
                    }
                    super.onDrawerOpened(drawerView)

                    val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                    if (channel_adapter.itemCount == 0) {
                        channel_adapter.setup()
                    }

                    this@ActivityEditor.playback_stop()
                    this@ActivityEditor.playback_stop_midi_output()
                    this@ActivityEditor.drawer_unlock() // So the drawer can be closed with a swipe
                }

                override fun onDrawerClosed(drawerView: View) {
                    this@ActivityEditor.get_action_interface().track(ActionTracker.TrackedAction.DrawerClose)
                    super.onDrawerClosed(drawerView)
                    this@ActivityEditor.drawer_lock() // so the drawer can't be opened with a swipe
                }
            }
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val that = this@ActivityEditor
                val opus_manager = that.get_opus_manager()
                val drawer_layout = that.findViewById<DrawerLayout>(R.id.drawer_layout)

                if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                    that.drawer_close()
                } else if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Unset) {
                    opus_manager.cursor_clear()
                } else {
                    that.dialog_save_project {
                        that.save_to_backup()
                        that.finish()
                    }
                }
            }
        })

        if (savedInstanceState != null) {
            // if the activity is forgotten, the opus_manager is be uninitialized
            if (this.get_opus_manager().is_initialized()) {
                this.refresh(
                    savedInstanceState.getInt("x") ?: 0,
                    savedInstanceState.getInt("y") ?: 0
                )
            } else {
                this.load_from_bkp()
            }
        } else if (this.intent.data == null) {
            this.setup_new()
        } else if (this.is_bkp(this.intent.data!!)) {
            this.load_from_bkp()
        } else if (this.project_manager.contains(this.intent.data!!)) {
            this.load_project(this.intent.data!!.toString())
        } else {
            this.handle_uri(this.intent.data!!)
        }

        this.initial_load = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        this._options_menu = menu
        val output = super.onCreateOptionsMenu(menu)
        this.update_menu_options()
        return output
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.itmPlay && item.itemId != R.id.itmPlayMidiOutput) {
            this.playback_stop()
            this.playback_stop_midi_output()
        }

        when (item.itemId) {
            android.R.id.home -> {
                this.drawer_open()
            }

            R.id.itmNewProject -> {
                this.dialog_save_project {
                    this.get_action_interface().new_project()
                }
            }

            R.id.itmLoadProject -> {
                this.dialog_load_project { path: String ->
                    this.dialog_save_project {
                        this.get_action_interface().load_project(path)
                    }
                }
            }

            R.id.itmImportMidi -> {
                this.import_intent_launcher.launch(
                    Intent().apply {
                        setAction(Intent.ACTION_GET_CONTENT)
                        setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                    }
                )
            }

            R.id.itmUndo -> {
                this.get_action_interface().apply_undo()
            }

            R.id.itmPlay -> {
                when (this.playback_state_soundfont) {
                    PlaybackState.Ready -> {
                        this.playback_start()
                    }
                    PlaybackState.Queued,
                    PlaybackState.Playing -> {
                        this.playback_stop()
                    }

                    else -> { }
                }
            }

            R.id.itmPlayMidiOutput -> {
                when (this.playback_state_midi) {
                    PlaybackState.Ready -> {
                        this.playback_start_midi_output()
                    }
                    PlaybackState.Queued,
                    PlaybackState.Playing -> {
                        this.playback_stop_midi_output()
                    }
                    else -> { /* pass */ }
                }
            }

            R.id.itmSettings -> {
                this.get_action_interface().open_settings()
            }
            R.id.itmAbout -> {
                this.get_action_interface().open_about()
            }
            R.id.itmDebug -> {
                this.save_actions()
                this.feedback_msg("SAVED ACTIONS")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun open_settings() {
        startActivity(Intent(this, ActivitySettings::class.java))
    }

    fun open_about() {
        startActivity(Intent(this, ActivityAbout::class.java))
    }

    fun project_save() {
        this.project_manager.save(this.get_opus_manager())
        this.feedback_msg(getString(R.string.feedback_project_saved))
        this.update_menu_options()
    }

    fun project_delete() {
        this.project_manager.delete(this.get_opus_manager())
    }

    fun project_move_to_copy() {
        this.dialog_save_project {
            this.project_manager.move_to_copy(this.get_opus_manager())
            this.update_title_text()
            this.feedback_msg(getString(R.string.feedback_on_copy))

            val btnDeleteProject = this.findViewById<View>(R.id.btnDeleteProject)
            val btnCopyProject = this.findViewById<View>(R.id.btnCopyProject)
            btnDeleteProject.isEnabled = false
            btnCopyProject.isEnabled = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun _enable_blocker_view() {
        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay)
        if (blocker_view != null && blocker_view.visibility != View.VISIBLE) {
            blocker_view.setOnTouchListener { _, motion_event ->
                /* Allow Scrolling on the y axis when scrolling in the main_recycler */
                if (motion_event == null) {
                } else if (motion_event.action == 1) {
                    this._blocker_scroll_y = null
                } else if (motion_event.action != MotionEvent.ACTION_MOVE) {
                } else {
                    val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
                    val scroll_view = editor_table.get_scroll_view()

                    if (this._blocker_scroll_y == null) {
                        this._blocker_scroll_y = (motion_event.y - scroll_view.y)
                    }

                    val rel_y = (motion_event.y - scroll_view.y)
                    val delta_y = this._blocker_scroll_y!! - rel_y

                    scroll_view.scrollBy(0, delta_y.toInt())
                    this._blocker_scroll_y = rel_y
                }
                true
            }

            blocker_view.visibility = View.VISIBLE
            this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun _disable_blocker_view() {
        // Assume playback states have been updated BEFORE calling this function
        // Need to check both since they both use the same blocker, though it should never be an issue
        if (this.playback_state_midi == PlaybackState.Playing || this.playback_state_soundfont == PlaybackState.Playing) {
            return
        }
        this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val blocker_view = this.findViewById<LinearLayout>(R.id.llClearOverlay) ?: return
        blocker_view.visibility = View.GONE
    }

    private fun playback_start() {
        if (!this.update_playback_state_soundfont(PlaybackState.Queued)) {
            this.feedback_msg(getString(R.string.playback_failed))
            return
        }

        this._enable_blocker_view()
        this.runOnUiThread {
            this.set_playback_button(R.drawable.baseline_play_disabled_24)
            this.force_title_text(getString(R.string.reticle_msg_start_playback))
            this.loading_reticle_show()
        }

        var start_point = this.get_working_column()
        if (start_point >= this.get_opus_manager().length - 1) {
            start_point = 0
        }
        // Currently, Midi2.0 output is not supported. will be needed for N-radix projects
        thread {
            // May Not need to set instruments at beginning of playback, commenting for now
            //val opus_manager = this.get_opus_manager()
            //opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            //    val (bank, program) = channel.get_instrument()
            //    opus_manager.sample_handle_manager?.select_bank(channel.midi_channel, bank)
            //    opus_manager.sample_handle_manager?.change_program(channel.midi_channel, program)
            //}

            this._midi_playback_device?.play_opus(start_point)
        }
    }

    private fun playback_start_midi_output() {
        if (!this.update_playback_state_midi(PlaybackState.Queued)) {
            this.feedback_msg(getString(R.string.playback_failed))
            return
        }

        this.force_title_text(getString(R.string.reticle_msg_start_playback))
        this.loading_reticle_show()
        this._enable_blocker_view()

        var start_point = this.get_working_column()
        val opus_manager = this.get_opus_manager()
        if (start_point >= opus_manager.length - 1) {
            start_point = 0
        }

        val midi = opus_manager.get_midi(start_point)

        this.runOnUiThread {
            this.loading_reticle_hide()
            this.clear_forced_title()
            this.set_midi_playback_button(R.drawable.ic_baseline_pause_24)
        }

        if (!this.update_playback_state_midi(PlaybackState.Playing)) {
            this.restore_midi_playback_state()
            return
        }

        thread {
            try {
                this._midi_interface.open_connected_devices()
                this._virtual_input_device.play_midi(midi) {
                    this.runOnUiThread {
                        this.playback_stop_midi_output()
                    }
                }
            } catch (e: IOException) {
                this.runOnUiThread {
                    this.playback_stop_midi_output()
                }
            }
        }
    }

    internal fun playback_stop() {
        if (this.update_playback_state_soundfont(PlaybackState.Stopping)) {
            this.loading_reticle_hide()
            this.clear_forced_title()
            this._midi_playback_device?.kill()
        }
    }

    internal fun playback_stop_midi_output() {
        if (this.update_playback_state_midi(PlaybackState.Stopping)) {
            this.loading_reticle_hide()
            this.clear_forced_title()
            this._virtual_input_device.stop()
            this.restore_midi_playback_state()
        }
    }

    fun restore_playback_state() {
        if (this.update_playback_state_soundfont(PlaybackState.Ready)) {
            this.runOnUiThread {
                this.set_playback_button(R.drawable.ic_baseline_play_arrow_24)
                this._disable_blocker_view()
            }
        }
    }

    fun restore_midi_playback_state() {
        if (this.update_playback_state_midi(PlaybackState.Ready)) {
            this.runOnUiThread {
                this.set_midi_playback_button(R.drawable.ic_baseline_play_arrow_24)
                this._disable_blocker_view()
            }
        }
    }

    fun get_new_project_path(): String {
        return this.project_manager.get_new_path()
    }

    // Ui Wrappers ////////////////////////////////////////////
    private fun drawer_close() {
        this.get_action_interface().drawer_close()
    }

    private fun drawer_open() {
        this.get_action_interface().drawer_open()
    }

    fun drawer_lock() {
        this._binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun drawer_unlock() {
        try {
            this._binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            this.findViewById<LinearLayout>(R.id.config_drawer)?.refreshDrawableState()
        } catch (e: UninitializedPropertyAccessException) {
            // pass, if it's not initialized, it's not locked
        }
    }


    fun navigate(fragment: Int) {
        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        //if (fragment == R.id.EditorFragment) {
        //    this._has_seen_front_page = true
        //}

        //navController.navigate(fragment)
    }

    //fun get_active_fragment(): Fragment? {
    //    val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
    //    return navHost?.childFragmentManager?.fragments?.get(0)
    //}

    fun update_title_text() {
        this.set_title_text(
            if (this._forced_title_text != null) {
                this._forced_title_text!!
            } else {
                this.get_opus_manager().project_name ?: getString(R.string.untitled_opus)
            }
        )
    }

    fun set_title_text(new_text: String) {
        this._binding.toolbar.title = new_text
    }

    fun force_title_text(msg: String) {
        this._forced_title_text = msg
        this.update_title_text()
    }

    fun clear_forced_title() {
        this._forced_title_text = null
        this.update_title_text()
    }

    fun update_menu_options() {
        val options_menu = this._options_menu ?: return
        val play_midi_visible = this.configuration.allow_midi_playback && (this._midi_interface.output_devices_connected() && this.get_opus_manager().is_tuning_standard())
        options_menu.findItem(R.id.itmLoadProject).isVisible = this.has_projects_saved()
        options_menu.findItem(R.id.itmPlay).isVisible = this._soundfont != null && ! play_midi_visible
        options_menu.findItem(R.id.itmPlayMidiOutput).isVisible = play_midi_visible
        options_menu.findItem(R.id.itmDebug).isVisible = this.is_debug_on()
    }

    fun setup_project_config_drawer() {
        val opus_manager = this.get_opus_manager()
        val tvChangeProjectName: MaterialButton = this.findViewById(R.id.btnChangeProjectName)
        tvChangeProjectName.setOnClickListener {
            this.get_action_interface().set_project_name_and_notes()
        }

        //-------------------------------------------
        val btnRadix: MaterialButton = this.findViewById(R.id.btnRadix)
        btnRadix.setOnClickListener {
            this.get_action_interface().set_tuning_table_and_transpose()
        }
        //-------------------------------------------
        this.findViewById<View>(R.id.btnAddChannel).setOnClickListener {
            this.get_action_interface().insert_channel(opus_manager.channels.size)
        }

        this.findViewById<View>(R.id.btnAddPercussion).setOnClickListener {
            this.get_action_interface().insert_percussion_channel(opus_manager.channels.size)
        }

        this.setup_project_config_drawer_export_button()
        this.findViewById<View>(R.id.btnSaveProject).setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.get_action_interface().save()
        }
        this.findViewById<View>(R.id.btnSaveProject).setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }

            this.export_project()
            true
        }

        val btnDeleteProject = this.findViewById<View>(R.id.btnDeleteProject)
        val btnCopyProject = this.findViewById<View>(R.id.btnCopyProject)

        val file_exists = (opus_manager.path != null && File(opus_manager.path!!).isFile)
        btnDeleteProject.isEnabled = file_exists
        btnCopyProject.isEnabled = file_exists

        btnDeleteProject.setOnClickListener {
            if (it.isEnabled) {
                this.dialog_delete_project(this.get_opus_manager())
            }
        }

        btnCopyProject.setOnClickListener {
            if (it.isEnabled) {
                this.get_action_interface().project_copy()
            }
        }
    }

    internal fun _build_dialog_title_view(text: String): TextView {
        val output = TextView(ContextThemeWrapper(this, R.style.dialog_title))
        output.text = text
        return output
    }

    internal fun setup_project_config_drawer_export_button() {
        val export_options = this.get_exportable_options()
        val export_button = this.findViewById<MaterialButton>(R.id.btnExportProject) ?: return
        val export_progress_wrapper = this.findViewById<LinearLayout>(R.id.llExportProgress) ?: return
        if (!this.view_model.is_exporting()) {
            export_button.setIconResource(R.drawable.export)
            export_progress_wrapper.visibility = View.GONE
        } else {
            export_button.setIconResource(R.drawable.baseline_cancel_42)
            export_progress_wrapper.visibility = View.VISIBLE
        }

        if (export_options.isNotEmpty()) {
            export_button.setOnClickListener {
                if (!this.view_model.is_exporting()) {
                    this.dialog_popup_menu(
                        getString(R.string.dlg_export),
                        export_options,
                        default = null
                    ) { _: Int, value: Int ->
                        when (value) {
                            0 -> this.export_project()
                            1 -> this.export_midi_check()
                            2 -> this.export_wav() // DEBUG
                            3 -> this.export_multi_lines_wav() // DEBUG
                            4 -> this.export_multi_channels_wav() // DEBUG
                        }
                    }
                } else {
                    this.export_wav_cancel()
                }
            }
            export_button.visibility = View.VISIBLE
        } else {
            export_button.visibility = View.GONE
        }
    }

    private fun get_exportable_options(): List<Pair<Int, String>> {
        val export_options = mutableListOf<Pair<Int, String>>(
            Pair(0, getString(R.string.export_option_json))
        )
        if (this.get_opus_manager().is_tuning_standard()) {
            export_options.add( Pair(1, getString(R.string.export_option_midi)) )
        }

        if (this.get_soundfont() != null) {
            export_options.add( Pair(2, getString(R.string.export_option_wav)) )
            export_options.add( Pair(3, getString(R.string.export_option_wav_lines)) )
            export_options.add( Pair(4, getString(R.string.export_option_wav_channels)) )
        }

        return export_options
    }

    // Ui Wrappers End ////////////////////////////////////////

    private fun _get_default_drum_options(): List<Pair<String, Int>> {
        val midi_drums = this.resources.getStringArray(R.array.midi_drums)
        return List(midi_drums.size) { i: Int ->
            Pair(midi_drums[i]!!, i + 27)
        }
    }

    private fun get_drum_options(channel_index: Int): List<Pair<String, Int>> {
        if (this._sample_handle_manager == null || this.is_connected_to_physical_device()) {
            return this._get_default_drum_options()
        }

        val channel = this.get_opus_manager().get_channel(channel_index)

        val preset = try {
            this._sample_handle_manager!!.get_preset(channel.get_midi_channel()) ?: return this._get_default_drum_options()
        } catch (e: SoundFont.InvalidPresetIndex) {
            return this._get_default_drum_options()
        }

        val available_drum_keys = mutableSetOf<Pair<String, Int>>()
        for ((_, preset_instrument) in preset.instruments) {
            if (preset_instrument.instrument == null) {
                continue
            }

            val instrument_range = preset_instrument.key_range ?: Pair(0, 127)

            for (sample_directive in preset_instrument.instrument!!.sample_directives.values) {
                val key_range = sample_directive.key_range ?: Pair(0, 127)
                val usable_range = max(key_range.first, instrument_range.first)..min(key_range.second, instrument_range.second)

                var name = sample_directive.sample!!.first().name
                if (name.contains("(")) {
                    name = name.substring(0, name.indexOf("("))
                }

                for (key in usable_range) {
                    var use_name = if (usable_range.first != usable_range.last) {
                        "$name - ${(key - usable_range.first) + 1}"
                    } else {
                        name
                    }
                    available_drum_keys.add(Pair(use_name, key))
                }
            }
        }

        return available_drum_keys.sortedBy {
            it.second
        }
    }

    fun update_channel_instrument(midi_channel: Int, instrument: Pair<Int, Int>) {
        val (midi_bank, midi_program) = instrument
        this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
        this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))
        if (this._feedback_sample_manager != null) {
            this._feedback_sample_manager!!.select_bank(
                midi_channel,
                midi_bank,
            )
            this._feedback_sample_manager!!.change_program(
                midi_channel,
                midi_program,
            )
        }

        // Don't need to update anything but percussion in the sample_handle_manager
        if (this._sample_handle_manager != null) {
            this._sample_handle_manager!!.select_bank(
                midi_channel,
                midi_bank
            )
            this._sample_handle_manager!!.change_program(
                midi_channel,
                midi_program
            )
        }
    }

    // Update peripheral device instruments, ie feedback device and midi devices
    fun update_channel_instruments(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (index == null) {
            this._feedback_sample_manager?.let { handle_manager: SampleHandleManager ->
                for (channel in opus_manager.get_all_channels()) {
                    val midi_channel = channel.get_midi_channel()
                    val (midi_bank, midi_program) = channel.get_instrument()
                    this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
                    this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))

                    handle_manager.select_bank(midi_channel, midi_bank)
                    handle_manager.change_program(midi_channel, midi_program)
                }
            }

            this._sample_handle_manager?.let { handle_manager: SampleHandleManager ->
                // Don't need to update anything but percussion here
                for ((_, channel) in opus_manager.get_percussion_channels()) {
                    val midi_channel = channel.get_midi_channel()
                    val (midi_bank, midi_program) = channel.get_instrument()
                    handle_manager.select_bank(midi_channel, midi_bank)
                    handle_manager.change_program(midi_channel, midi_program)
                }
            }
        } else {
            val opus_channel = opus_manager.get_channel(index)
            this.update_channel_instrument(
                opus_channel.get_midi_channel(),
                opus_channel.get_instrument()
            )
        }
    }

    fun get_opus_manager(): OpusLayerInterface {
        return this.view_model.opus_manager
    }

    fun play_event(channel: Int, event_value: Int, velocity: Float = .6F) {
        if (event_value < 0) {
            return // No sound to play
        }
        if (!this.is_connected_to_physical_device()) {
            if (this._feedback_sample_manager == null) {
                this.connect_feedback_device()
                this.update_channel_instruments()
            }
        } else {
            this.disconnect_feedback_device()
            this._midi_interface.open_output_devices()
        }

        val opus_manager = this.get_opus_manager()
        val midi_channel = opus_manager.get_channel(channel).get_midi_channel()

        val radix = opus_manager.tuning_map.size
        val (note, bend) = if (opus_manager.is_percussion(channel)) { // Ignore the event data and use percussion map
            Pair(event_value + 27, 0)
        } else {
            val octave = event_value / radix
            val offset = opus_manager.tuning_map[event_value % radix]

            val transpose_offset = 12.0 * opus_manager.transpose.first.toDouble() / opus_manager.transpose.second.toDouble()
            val std_offset = 12.0 * offset.first.toDouble() / offset.second.toDouble()

            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(
                transpose_offset
            ))) * 512.0).toInt()
            val new_note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21

            Pair(new_note, bend)
        }

        if (note > 127) {
            return
        }


        this._feedback_sample_manager?.let { handle_manager : SampleHandleManager ->
            if (this._temporary_feedback_devices[this._current_feedback_device] == null) {
                this._temporary_feedback_devices[this._current_feedback_device] =
                    FeedbackDevice(this._feedback_sample_manager!!)
            }

            val event = NoteOn79(
                index = 0,
                channel = midi_channel,
                note = note,
                bend = bend,
                velocity = (velocity * 127F).toInt() shl 8,
            )

            this._temporary_feedback_devices[this._current_feedback_device]!!.new_event(event, 250)
            this._current_feedback_device = (this._current_feedback_device + 1) % this._temporary_feedback_devices.size
        } ?: {
            try {
                this._midi_feedback_dispatcher.play_note(
                    midi_channel,
                    note,
                    bend,
                    (velocity * 127F).toInt(),
                    !opus_manager.is_tuning_standard() || !this.is_connected_to_physical_device()
                )
            } catch (e: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But i'm sure that'll come back to bite me
            }
        }()
    }

    fun import_project(path: String) {
        this.applicationContext.contentResolver.openFileDescriptor(path.toUri(), "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.get_opus_manager().load(bytes, this.project_manager.get_new_path())
        }
    }

    fun import_midi(path: String) {
        val bytes = this.applicationContext.contentResolver.openFileDescriptor(path.toUri(), "r")?.use {
            FileInputStream(it.fileDescriptor).readBytes()
        } ?: throw InvalidMIDIFile(path)

        val midi = try {
            Midi.Companion.from_bytes(bytes)
        } catch (e: Exception) {
            throw InvalidMIDIFile(path)
        }

        val opus_manager = this.get_opus_manager()
        opus_manager.project_change_midi(midi)
        val filename = this.parse_file_name(path.toUri())
        opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: getString(
            R.string.default_imported_midi_title))
        opus_manager.clear_history()
    }

    fun populate_supported_soundfont_instrument_names() {
        // populate a cache of available soundfont names so se don't have to open up the soundfont data
        // every time
        this._soundfont_supported_instrument_names.clear()
        val soundfont = this._soundfont
        if (soundfont != null) {
            for ((name, program, bank) in soundfont.get_available_presets()) {
                this._soundfont_supported_instrument_names[Pair(bank, program)] = name
            }
        } else {
            var program = 0
            for (name in this.resources.getStringArray(R.array.midi_instruments)) {
                this._soundfont_supported_instrument_names[Pair(0, program++)] = name
            }
        }
    }

    fun get_supported_instrument_names(): HashMap<Pair<Int, Int>, String> {
        if (this._soundfont_supported_instrument_names.isEmpty()) {
            this.populate_supported_soundfont_instrument_names()
        }

        return this._soundfont_supported_instrument_names
    }

    fun set_soundfont() {
        val filename = this.configuration.soundfont
        if (filename == null) {
            this.disable_soundfont()
            return
        }

        if (!this.update_playback_state_soundfont(PlaybackState.Ready)) {
            // Failed to change playback_state
            return
        }

        val path = "${this.getExternalFilesDir(null)}/SoundFonts/$filename"
        try {
            this._soundfont = SoundFont(path)
        } catch (e: Riff.InvalidRiff) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg(getString(R.string.invalid_soundfont))
            return
        } catch (e: SoundFont.InvalidSoundFont) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg("Invalid Soundfont")
            return
        }

        this.populate_supported_soundfont_instrument_names()

        this.reinit_playback_device()
        this.connect_feedback_device()
        this.update_channel_instruments()
        this.active_percussion_names.clear()
        this.runOnUiThread {
            this.setup_project_config_drawer_export_button()
            val channel_recycler = this.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            // Should always be null since this can only be changed from a different menu
            if (channel_recycler.adapter != null) {
                val channel_adapter = channel_recycler.adapter as ChannelOptionAdapter
                channel_adapter.notify_soundfont_changed()
            }
        }
    }

    fun shift_up_percussion_names(channel: Int) {
        val keys = this.active_percussion_names.keys.sorted().reversed()
        for (k in keys) {
            if (k < channel) {
                continue
            }
            this.active_percussion_names[k + 1] = this.active_percussion_names.remove(k)!!
        }
    }

    fun shift_down_percussion_names(channel: Int) {
        val keys = this.active_percussion_names.keys.sorted()
        for (k in keys) {
            if (k > channel) {
                this.active_percussion_names[k - 1] = this.active_percussion_names.remove(k)!!
            } else if (k == channel) {
                this.active_percussion_names.remove(k)
            }
        }
    }

    fun swap_percussion_channels(channel_a: Int, channel_b: Int) {
        val a_names = this.active_percussion_names[channel_a]
        if (this.active_percussion_names[channel_b] != null) {
            this.active_percussion_names[channel_a] = this.active_percussion_names[channel_b]!!
        }
        if (a_names != null) {
            this.active_percussion_names[channel_b] = a_names
        }
    }

    fun update_playback_state_soundfont(next_state: PlaybackState): Boolean {
        this.playback_state_soundfont = this._update_playback_state(this.playback_state_soundfont, next_state) ?: return false
        return true
    }

    fun update_playback_state_midi(next_state: PlaybackState): Boolean {
        this.playback_state_midi = this._update_playback_state(this.playback_state_midi, next_state) ?: return false
        return true
    }

    private fun _update_playback_state(input_state: PlaybackState, next_state: PlaybackState): PlaybackState? {
        return when (input_state) {
            PlaybackState.NotReady -> {
                when (next_state) {
                    PlaybackState.NotReady,
                    PlaybackState.Ready -> next_state
                    else -> null
                }
            }
            PlaybackState.Ready -> {
                when (next_state) {
                    PlaybackState.NotReady,
                    PlaybackState.Ready,
                    PlaybackState.Queued -> next_state
                    else -> null
                }
            }
            PlaybackState.Playing -> {
                when (next_state) {
                    PlaybackState.Ready,
                    PlaybackState.Stopping -> next_state
                    else -> null
                }
            }
            PlaybackState.Queued -> {
                when (next_state) {
                    PlaybackState.Ready,
                    PlaybackState.Playing -> next_state
                    else -> null
                }
            }
            PlaybackState.Stopping -> {
                when (next_state) {
                    PlaybackState.Ready -> next_state
                    else -> null
                }
            }
        }
    }

    fun get_soundfont(): SoundFont? {
        return this._soundfont
    }

    fun disable_soundfont() {
        if (!this.update_playback_state_soundfont(PlaybackState.NotReady)) {
            return
        }

        if (this._feedback_sample_manager != null) {
            this.disconnect_feedback_device()
        }
        this._sample_handle_manager?.destroy()

        this._soundfont?.destroy()
        this._soundfont = null
        this._sample_handle_manager = null
        this._midi_playback_device = null
        this._soundfont_supported_instrument_names.clear()

        this.update_channel_instruments()
        this.active_percussion_names.clear()
    }

    fun get_drum_name(channel: Int, index: Int): String? {
        this.populate_active_percussion_names(channel, false)
        return if (! this.active_percussion_names.containsKey(channel)) {
            null
        } else {
            this.active_percussion_names[channel]!![index + 27]
        }
    }

    fun populate_active_percussion_names(channel_index: Int, force: Boolean = true) {
        if (force || !this.active_percussion_names.containsKey(channel_index)) {
            this.active_percussion_names[channel_index] = HashMap()
            val drums = this.get_drum_options(channel_index)
            for ((name, note) in drums) {
                if (note >= 27) {
                    this.active_percussion_names[channel_index]!![note] = name
                }
            }
        }
    }

    fun dialog_color_picker(initial_color: Int, callback: (Int?) -> Unit) {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.color_picker,
                this._binding.root,
                false
            )

        val flColorDisplay = viewInflated.findViewById<FrameLayout>(R.id.flColorDisplay)
        val sbRed = viewInflated.findViewById<SeekBar>(R.id.sbRed)
        val sbGreen = viewInflated.findViewById<SeekBar>(R.id.sbGreen)
        val sbBlue = viewInflated.findViewById<SeekBar>(R.id.sbBlue)
        val rniRed = viewInflated.findViewById<RangedIntegerInput>(R.id.rniRed)
        val rniGreen = viewInflated.findViewById<RangedIntegerInput>(R.id.rniGreen)
        val rniBlue = viewInflated.findViewById<RangedIntegerInput>(R.id.rniBlue)
        val hex_value = viewInflated.findViewById<HexEditText>(R.id.hexValue)

        hex_value.setText("%02x".format(initial_color.red) + "%02x".format(initial_color.green) + "%02x".format(initial_color.blue))
        rniRed.set_value(initial_color.red)
        rniRed.set_range(0, 255)
        rniGreen.set_value(initial_color.green)
        rniGreen.set_range(0, 255)
        rniBlue.set_value(initial_color.blue)
        rniBlue.set_range(0, 255)

        sbRed.progress = initial_color.red
        sbGreen.progress = initial_color.green
        sbBlue.progress = initial_color.blue

        var lockout = false
        rniRed.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                sbRed.progress = p0.toString().toInt()
                lockout = false
            }
        })
        rniGreen.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                sbGreen.progress = p0.toString().toInt()
                lockout = false
            }
        })
        rniBlue.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                sbBlue.progress = p0.toString().toInt()
                lockout = false
            }
        })

        val seekbar_listener = object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (lockout) {
                    return
                }
                lockout = true
                when (p0) {
                    sbRed -> rniRed.set_value(p1)
                    sbGreen -> rniGreen.set_value(p1)
                    sbBlue -> rniBlue.set_value(p1)
                }
                val new_color = Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0)
                flColorDisplay.setBackgroundColor(new_color)
                lockout = false
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) { }
        }

        sbRed.setOnSeekBarChangeListener(seekbar_listener)
        sbGreen.setOnSeekBarChangeListener(seekbar_listener)
        sbBlue.setOnSeekBarChangeListener(seekbar_listener)

        hex_value.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) { }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) { }

            override fun afterTextChanged(s: Editable?) {
                var string = s.toString()

                if (!lockout) {
                    if (string.length == 6) {
                        val red = string.substring(0, 2).toInt(16)
                        val green = string.substring(2, 4).toInt(16)
                        val blue = string.substring(4, 6).toInt(16)

                        lockout = true
                        rniRed.set_value(red)
                        rniGreen.set_value(green)
                        rniBlue.set_value(blue)
                        sbRed.progress = red
                        sbGreen.progress = green
                        sbBlue.progress = blue
                        flColorDisplay.setBackgroundColor(Color.rgb(red, green, blue))
                        lockout = false
                    }
                }
            }
        })


        flColorDisplay.setBackgroundColor(Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0))
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(getString(R.string.dlg_title_set_line_color))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val new_color = Color.rgb(rniRed.get_value() ?: 0, rniGreen.get_value() ?: 0, rniBlue.get_value() ?: 0)
                if (new_color != initial_color) {
                    callback(new_color)
                }
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.color_picker_clear)) { dialog, _ ->
                callback(null)
                dialog.dismiss()
            }
            .show()
    }

    fun dialog_text_popup(title: String, default: String? = null, callback: (String) -> Unit) {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.text_input,
                this._binding.root,
                false
            )

        val text_input: EditText = viewInflated.findViewById(R.id.etText)
        text_input.setText(default ?: "")

        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                callback(text_input.text.toString())
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    fun dialog_name_and_notes_popup(default: Pair<String, String>? = null, callback: (String, String) -> Unit) {
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.text_name_change,
                this._binding.root,
                false
            )

        val project_name_input: EditText = viewInflated.findViewById(R.id.etProjectName)
        project_name_input.setText(default?.first ?: "")

        val project_notes_input: EditText = viewInflated.findViewById(R.id.etProjectNotes)
        project_notes_input.setText(default?.second ?: "")

        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                callback(project_name_input.text.toString(), project_notes_input.text.toString())
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // TODO: fix code duplication in dialog_float/integer_input
    internal fun dialog_float_input(title: String, min_value: Float, max_value: Float, default: Float? = null, callback: (value: Float) -> Unit ) {
        val coerced_default_value = default ?: (this._float_dialog_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_float,
                window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedFloatInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setCustomTitle(this._build_dialog_title_view(title))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this._float_dialog_defaults[title] = output_value
                callback(output_value)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()

        number_input.set_range(min_value, max_value)
        number_input.setText("$coerced_default_value")
        number_input.setOnClickListener {
            number_input.selectAll()
        }

        number_input.value_set_callback = { value: Float? ->
            callback(value ?: coerced_default_value)
            dialog.dismiss()
        }

        number_input.requestFocus()
        number_input.selectAll()
    }

    internal fun dialog_number_input(title: String, min_value: Int, max_value: Int, default: Int? = null, callback: (value: Int) -> Unit ) {
        val coerced_default_value = default ?: (this._integer_dialog_defaults[title] ?: min_value)
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_split,
                window.decorView.rootView as ViewGroup,
                false
            )

        val number_input = viewInflated.findViewById<RangedIntegerInput>(R.id.etNumber)

        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val output_value = number_input.get_value() ?: coerced_default_value
                this._integer_dialog_defaults[title] = output_value
                callback(output_value)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()

        number_input.set_range(min_value, max_value)
        number_input.setText("$coerced_default_value")
        number_input.setOnClickListener {
            number_input.selectAll()
        }

        number_input.value_set_callback = { value: Int? ->
            callback(value ?: coerced_default_value)
            dialog.dismiss()
        }

        number_input.requestFocus()
        number_input.selectAll()
    }

    private fun needs_save(): Boolean {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.path == null) {
            return true
        }

        if (!File(opus_manager.path!!).exists()) {
            return !opus_manager.history_cache.isEmpty()
        }

        val other = OpusLayerBase()
        other.load_path(opus_manager.path!!)

        return opus_manager != other
    }

    fun dialog_save_project(callback: (Boolean) -> Unit) {
        if (this.initial_load) {
            callback(false)
        } else if (this.needs_save()) {
            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setTitle(R.string.dialog_save_warning_title)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ ->
                    this@ActivityEditor.project_save()
                    dialog.dismiss()
                    callback(true)
                }
                .setNegativeButton(getString(R.string.dlg_decline)) { dialog, _ ->
                    dialog.dismiss()
                    callback(false)
                }
                .show()
        } else {
            callback(false)
        }
    }

    fun get_default_export_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return "Pagan Op. ${now.format(formatter)}"
    }

    fun get_export_name(): String {
        val reserved_chars = "|\\?*<\":>+[]/'"
        var base_name: String = this.get_opus_manager().project_name ?: this.get_default_export_name()
        for (c in reserved_chars) {
            base_name = base_name.replace("$c", "_")
        }
        return base_name
    }

    fun export_multi_lines_wav() {
        val name = this.get_export_name()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.putExtra(Intent.EXTRA_TITLE, name)
        this._export_multi_line_wav_intent_launcher.launch(intent)
    }

    fun export_multi_channels_wav() {
        val name = this.get_export_name()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.putExtra(Intent.EXTRA_TITLE, name)
        this._export_multi_channel_wav_intent_launcher.launch(intent)
    }

    fun export_wav() {
        val name = this.get_export_name()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "audio/wav"
        intent.putExtra(Intent.EXTRA_TITLE, "$name.wav")
        this._export_wav_intent_launcher.launch(intent)
    }

    fun export_wav_cancel() {
        this.view_model.cancel_export()
    }

    fun export_midi_check() {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.get_percussion_channels().size > 1) {
            val text_view: TextView = TextView(this)
            text_view.text = getString(R.string.multiple_kit_warning)

            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setTitle(R.string.generic_warning)
                .setView(text_view)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    this.export_midi()
                    dialog.dismiss()
                }
                .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } else {
            this.export_midi()
        }
    }

    fun export_midi() {
        val name = this.get_export_name()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        //intent.type = MimeTypes.AUDIO_MIDI
        intent.type = "audio/midi"
        intent.putExtra(Intent.EXTRA_TITLE, "$name.mid")

        this._export_midi_intent_launcher.launch(intent)
    }

    fun export_project() {
        val name = this.get_export_name()

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, "$name.json")

        this._export_project_intent_launcher.launch(intent)
    }

    /**
     * Save text file in storage of a crash report.
     * To be copied and saved somewhere accessible on reload.
     */
    fun bkp_crash_report(e: Throwable) {
        val path = this.getExternalFilesDir(null).toString()
        val file = File("$path/bkp_crashreport.log")
        file.writeText(e.stackTraceToString())
    }

    fun is_bkp(uri: Uri): Boolean {
        val result = uri == "${applicationInfo.dataDir}/.bkp.json".toUri()
        return result
    }

    fun reinit_playback_device() {
        this._midi_playback_device?.kill()

        if (this.get_soundfont() != null) {
            /*
             * TODO: Put the ignore envelope/lfo option somewhere better.
             * I don't think it should be in apres if theres a reasonable way to avoid it
             */
            this._sample_handle_manager?.destroy()
            this._sample_handle_manager = SampleHandleManager(
                this._soundfont!!,
                this.configuration.sample_rate,
                this.configuration.sample_rate,
                ignore_lfo = true
            )

            this._midi_playback_device = PlaybackDevice(
                this,
                this._sample_handle_manager!!,
                WaveGenerator.StereoMode.Stereo
            )
        } else {
            this._midi_playback_device = null
        }
    }

    fun in_playback(): Boolean {
        return PlaybackState.Playing in listOf(this.playback_state_soundfont, this.playback_state_midi)
    }

    fun has_notification_permission(): Boolean {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED )
    }

    private fun getNotificationPermission(): Boolean {
        if (! this.has_notification_permission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        return this.has_notification_permission()
    }

    fun set_playback_button(drawable: Int) {
        val play_pause_button = this._options_menu?.findItem(R.id.itmPlay) ?: return
        play_pause_button.icon = ContextCompat.getDrawable(this, drawable)
    }

    fun set_midi_playback_button(drawable: Int) {
        val play_pause_button = this._options_menu?.findItem(R.id.itmPlayMidiOutput) ?: return
        play_pause_button.icon = ContextCompat.getDrawable(this, drawable)
    }

    fun get_working_column(): Int {
        val cursor = this.get_opus_manager().cursor
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                cursor.beat
            }
            OpusManagerCursor.CursorMode.Range -> {
                cursor.get_ordered_range()!!.first.beat
            }
            else -> {
                val editor_table = this.findViewById<EditorTable?>(R.id.etEditorTable)
                editor_table?.get_first_visible_column_index() ?: 0
            }
        }
    }

    fun is_connected_to_physical_device(): Boolean {
        return this.configuration.allow_midi_playback && this._midi_interface.output_devices_connected()
    }

    fun disconnect_feedback_device() {
        this._temporary_feedback_devices.forEachIndexed { i: Int, device: FeedbackDevice? ->
            device?.kill()
            device?.destroy()
            this._temporary_feedback_devices[i] = null
        }
        this._feedback_sample_manager?.destroy()
        this._feedback_sample_manager = null
    }

    fun connect_feedback_device() {
        if (this._soundfont == null) {
            return
        }

        this.disconnect_feedback_device()

        val buffer_size = this.configuration.sample_rate / 2
        this._feedback_sample_manager = SampleHandleManager(
            this._soundfont!!,
            this.configuration.sample_rate,
            buffer_size - 2 + (if (buffer_size % 2 == 0) {
                2
            } else {
                1
            })
        )
        this._current_feedback_device = 0
    }

    fun block_physical_midi_output() {
        this._midi_interface.block_physical_devices = true
        this._midi_interface.close_connected_devices()
        this.playback_state_midi = PlaybackState.NotReady

        if (this._feedback_sample_manager == null) {
            this.connect_feedback_device()
        }
    }

    fun enable_physical_midi_output() {
        this._midi_interface.block_physical_devices = false
        this._midi_interface.open_connected_devices()
        this.playback_state_midi = PlaybackState.Ready

        if (this.is_connected_to_physical_device()) {
            this.disconnect_feedback_device()
        }
    }

    fun get_notification(): NotificationCompat.Builder? {
        if (!this.has_notification_permission()) {
            return null
        }

        if (this._active_notification == null) {
            this.get_notification_channel()

            val cancel_export_flag = "com.qfs.pagan.CANCEL_EXPORT_WAV"
            val intent = Intent()
            intent.setAction(cancel_export_flag)
            intent.setPackage(this.packageName)

            val pending_cancel_intent = PendingIntent.getBroadcast(
                this,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(this.getString(R.string.export_wav_notification_title, this.get_opus_manager().project_name ?: "Untitled Project"))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.small_logo_rowan)
                .setSilent(true)
                .addAction(R.drawable.baseline_cancel_24, this.getString(android.R.string.cancel), pending_cancel_intent)

            this._active_notification = builder
        }

        return this._active_notification!!
    }

    fun get_notification_channel(): NotificationChannel? {
        return if (!this.has_notification_permission()) {
            null
        } else if (this._notification_channel == null) {
            val notification_manager = NotificationManagerCompat.from(this)
            // Create the NotificationChannel.
            val name = this.getString(R.string.export_wav_file_progress)
            val descriptionText = this.getString(R.string.export_wav_notification_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notification_manager.createNotificationChannel(mChannel)
            mChannel
        } else {
            this._notification_channel!!
        }
    }

    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = this.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(200, 5))
        }
    }


    fun get_file_type(uri: Uri): CompatibleFileType {
        return this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val test_bytes = ByteArray(4)
            FileInputStream(it.fileDescriptor).read(test_bytes)
            if (test_bytes.contentEquals("MThd".toByteArray())) {
                CompatibleFileType.Midi1
            } else {
                CompatibleFileType.Pagan
            }
        } ?: throw FileNotFoundException(uri.toString())
    }

    fun get_action_interface(): ActionTracker {
        return this.view_model.action_interface
    }

    fun save_actions() {
        val generated_code = this.get_action_interface().to_json().to_string()
        val path = this.getExternalFilesDir(null).toString()
        val timestamp = System.currentTimeMillis()
        val file_name = "$path/generated_$timestamp.json"
        val file = File(file_name)
        file.writeText(generated_code)
        this.get_action_interface().clear()
    }

    fun is_debug_on(): Boolean {
        return this.packageName.contains("pagandev")
    }

    override fun onDestroy() {
        this._sample_handle_manager?.destroy()
        this._feedback_sample_manager?.destroy()
        super.onDestroy()
    }

    fun set_forced_orientation(value: Int) {
        this.requestedOrientation = value
    }


    internal fun dialog_popup_selection_offset() {
        val view_inflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_note_offset,
                window.decorView.rootView as ViewGroup,
                false
            )

        val np_octave = view_inflated.findViewById<NumberPicker>(R.id.npOctave)
        np_octave.maxValue = 14
        np_octave.minValue = 0
        np_octave.value = 7
        np_octave.wrapSelectorWheel = false
        np_octave.setFormatter { value: Int ->
            "${value - 7}"
        }

        val np_offset = view_inflated.findViewById<NumberPicker>(R.id.npOffset)
        val radix = (this.get_opus_manager().tuning_map.size - 1)
        np_offset.maxValue = (radix * 2)
        np_offset.minValue = 0
        np_offset.value = radix
        np_offset.wrapSelectorWheel = false
        np_offset.setFormatter { value: Int ->
            "${value - radix}"
        }

        this._popup_active = true
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setView(view_inflated)
            .setTitle(R.string.dialog_adjust_selection)
            .setOnDismissListener {
                this._popup_active = false
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val opus_manager = this.get_opus_manager()
                val radix = opus_manager.tuning_map.size
                val octave = np_octave.value - 7
                val offset = np_offset.value - (radix - 1)
                val real_delta = (octave * radix) + offset
                opus_manager.offset_selection(real_delta)

                this.get_action_interface().track(ActionTracker.TrackedAction.AdjustSelection, listOf(real_delta))
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // vv Formerly Fragment Functions ---------------------------------------------------------
    fun clear_context_menu() {
        this.hide_context_menus()
        if (this.active_context_menu == null) {
            return
        }

        if (this.active_context_menu!!.primary != null) {
            if (this.active_context_menu!!.primary!!.parent != null) {
                (this.active_context_menu!!.primary!!.parent as ViewGroup).removeAllViews()
            }
        }

        if (this.active_context_menu!!.secondary != null) {
            if (this.active_context_menu!!.secondary!!.parent != null) {
                (this.active_context_menu!!.secondary!!.parent as ViewGroup).removeAllViews()
            }
        }

        this.active_context_menu = null
    }

    private fun hide_context_menus() {
        this.findViewById<LinearLayout>(R.id.llContextMenuPrimary)?.visibility = View.GONE
        this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)?.visibility = View.GONE
    }

    fun on_show_context_menus(a: View, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int, i: Int): Boolean {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.force_scroll_to_cursor_vertical()
        return false
    }

    private fun show_context_menus() {
        val primary = this.findViewById<LinearLayout>(R.id.llContextMenuPrimary)
        primary.removeOnLayoutChangeListener(this::on_show_context_menus)
        primary.visibility = if (primary.isNotEmpty()) {
            primary.addOnLayoutChangeListener(this::on_show_context_menus)
            View.VISIBLE
        } else {
            View.GONE
        }

        val secondary = this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        secondary.removeOnLayoutChangeListener(this::on_show_context_menus)
        secondary.visibility = if (secondary.isNotEmpty()) {
            if (primary.isEmpty()) {
                secondary.addOnLayoutChangeListener(this::on_show_context_menus)
            }
            View.VISIBLE
        } else {
            View.GONE
        }

    }

    internal fun set_context_menu_control_line() {
        // KLUDGE: due to the Generics, i need a better way of checking type here. for now i'm forcing refresh
        this.clear_context_menu()

        val opus_manager = this.get_opus_manager()
        val channels = opus_manager.get_all_channels()

        val cursor = opus_manager.cursor
        val controller_set = when (cursor.ctl_level!!) {
            CtlLineLevel.Line -> {
                channels[cursor.channel].lines[cursor.line_offset].controllers
            }
            CtlLineLevel.Channel -> {
                val channel = cursor.channel
                channels[channel].controllers
            }
            CtlLineLevel.Global -> {
                opus_manager.controllers
            }
        }

        val widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> {
                val controller = controller_set.get_controller<OpusTempoEvent>(cursor.ctl_type!!)
                ControlWidgetTempo(
                    controller.initial_event,
                    cursor.ctl_level!!,
                    true,
                    this
                ) { event: OpusTempoEvent ->
                    opus_manager.set_initial_event(event)
                }
            }
            ControlEventType.Volume -> {
                val controller = controller_set.get_controller<OpusVolumeEvent>(cursor.ctl_type!!)
                ControlWidgetVolume(
                    controller.initial_event,
                    cursor.ctl_level!!,
                    true,
                    this
                ) { event: OpusVolumeEvent ->
                    opus_manager.set_initial_event(event)
                }
            }
            ControlEventType.Reverb -> {
                val controller = controller_set.get_controller<OpusReverbEvent>(cursor.ctl_type!!)
                ControlWidgetReverb(
                    controller.initial_event,
                    cursor.ctl_level!!,
                    true,
                    this
                ) { event: OpusReverbEvent ->
                    opus_manager.set_initial_event(event)
                }
            }

            ControlEventType.Pan -> {
                val controller = controller_set.get_controller<OpusPanEvent>(cursor.ctl_type!!)
                ControlWidgetPan(
                    controller.initial_event,
                    cursor.ctl_level!!,
                    true,
                    this
                ) { event: OpusPanEvent ->
                    opus_manager.set_initial_event(event)
                }
            }
        }


        this.active_context_menu = ContextMenuControlLine(
            widget,
            this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
            this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        )

        this.show_context_menus()
    }

    internal fun set_context_menu_line_control_leaf() {
        // KLUDGE: due to the Generics, i need a better way of checking type here. for now i'm forcing refresh
        this.clear_context_menu()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val controller_set = opus_manager.get_active_active_control_set() ?: return

        val controller = controller_set.get_controller<OpusControlEvent>(cursor.ctl_type!!)
        val default = controller.get_latest_event(cursor.beat, cursor.get_position())?.copy() ?: controller.initial_event.copy()


        val (actual_beat, actual_position) = controller.get_blocking_position(cursor.beat, cursor.get_position()) ?: Pair(cursor.beat, cursor.get_position())
        val tree = controller.get_tree(actual_beat, actual_position)
        if (!tree.is_event()) {
            default.duration = 1
        }

        val widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> {
                ControlWidgetTempo(
                    default as OpusTempoEvent,
                    cursor.ctl_level!!,
                    false,
                    this
                ) { event: OpusTempoEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }
            ControlEventType.Volume -> {
                ControlWidgetVolume(
                    default as OpusVolumeEvent,
                    cursor.ctl_level!!,
                    false,
                    this
                ) { event: OpusVolumeEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }
            ControlEventType.Reverb -> {
                ControlWidgetReverb(
                    default as OpusReverbEvent,
                    cursor.ctl_level!!,
                    false,
                    this
                ) { event: OpusReverbEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }

            ControlEventType.Pan -> {
                ControlWidgetPan(
                    default as OpusPanEvent,
                    cursor.ctl_level!!,
                    false,
                    this
                ) { event: OpusPanEvent ->
                    opus_manager.set_event_at_cursor(event)
                }
            }
        }

        this.active_context_menu = ContextMenuControlLeaf(
            widget,
            this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
            this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
        )
        this.show_context_menus()
    }

    internal fun set_context_menu_line_control_leaf_b() {
        if (!this.refresh_or_clear_context_menu<ContextMenuControlLeafB>()) {
            this.active_context_menu = ContextMenuControlLeafB(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_range() {
        if (!this.refresh_or_clear_context_menu<ContextMenuRange>()) {
            this.active_context_menu = ContextMenuRange(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_column() {
        if (this.in_playback()) {
            this.clear_context_menu()
            return
        }

        if (!this.refresh_or_clear_context_menu<ContextMenuColumn>()) {
            this.active_context_menu = ContextMenuColumn(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    fun refresh_context_menu() {
        this.active_context_menu?.refresh() ?: this.hide_context_menus()
    }

    internal fun set_context_menu_line() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLine>()) {
            this.active_context_menu = ContextMenuLine(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_channel() {
        if (!this.refresh_or_clear_context_menu<ContextMenuChannel>()) {
            this.active_context_menu = ContextMenuChannel(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }


    internal fun set_context_menu_leaf() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLeaf>()) {
            this.active_context_menu = ContextMenuLeaf(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_leaf_percussion() {
        if (!this.refresh_or_clear_context_menu<ContextMenuLeafPercussion>()) {
            this.active_context_menu = ContextMenuLeafPercussion(
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        }
        this.show_context_menus()
    }

    fun shortcut_dialog() {
        val view = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_shortcut,
                this._binding.root as ViewGroup,
                false
            )

        val scroll_bar = view.findViewById<SeekBar>(R.id.shortcut_scrollbar)!!
        val title_text = view.findViewById<TextView>(R.id.shortcut_title)!!
        val spinner = view.findViewById<Spinner>(R.id.shortcut_spinner)!!

        val opus_manager = this.get_opus_manager()
        scroll_bar.max = opus_manager.length - 1

        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        scroll_bar.progress = editor_table.get_first_visible_column_index()


        title_text.text = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)
        title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = resources.getString(R.string.label_shortcut_scrollbar, p1)
                title_text.contentDescription = resources.getString(R.string.label_shortcut_scrollbar, p1)
                opus_manager.force_cursor_select_column(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog_builder = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
        dialog_builder.setView(view)
        val dialog = dialog_builder.show()

        if (opus_manager.marked_sections.isEmpty()) {
            spinner.visibility = View.GONE
        } else {
            spinner.visibility = View.VISIBLE
            val keys = opus_manager.marked_sections.keys.toList().sorted()
            val items = List(keys.size + 1) { i: Int ->
                if (i == 0) {
                    this.getString(R.string.jump_to_section)
                } else {
                    val section_name = opus_manager.marked_sections[keys[i - 1]]
                    if (section_name == null) {
                        getString(R.string.section_spinner_item, i, keys[i - 1])
                    } else {
                        "${keys[i - 1]}: ${section_name}"
                    }
                }
            }

            spinner.adapter = ArrayAdapter<String>(this, R.layout.spinner_list, items)
            spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        opus_manager.force_cursor_select_column(keys[position - 1])
                        dialog.dismiss()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun _get_start_column(): Int {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                cursor.beat
            }
            OpusManagerCursor.CursorMode.Range -> {
                cursor.range!!.first.beat
            }
            else -> {
                val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
                editor_table.get_first_visible_column_index()
            }
        }
    }

    private inline fun <reified T: ContextMenuView?> refresh_or_clear_context_menu(): Boolean {
        val llContextMenu = this.findViewById<LinearLayout>(R.id.llContextMenuPrimary)
        val llContextMenuSecondary = this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)

        if (this.active_context_menu !is T) {
            llContextMenu.removeAllViews()
            llContextMenu.visibility = View.GONE
            llContextMenuSecondary.removeAllViews()
            llContextMenuSecondary.visibility = View.GONE
            this.active_context_menu = null
            return false
        }

        this.active_context_menu?.refresh()

        return true
    }

    // ^^ Formerly Fragment Functions ---------------------------------------------------------

    fun dialog_tuning_table() {
        val opus_manager = this.get_opus_manager()

        val viewInflated = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_tuning_map,
                this._binding.root,
                false
            )

        val etRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etRadix)
        val etTranspose = viewInflated.findViewById<RangedIntegerInput>(R.id.etTranspose)
        etTranspose.set_range(0, 99999999)
        etTranspose.set_value(opus_manager.transpose.first)

        val etTransposeRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etTransposeRadix)
        etTransposeRadix.set_range(1, 99999999)
        etTransposeRadix.set_value(opus_manager.transpose.second)

        val rvTuningMap = viewInflated.findViewById<TuningMapRecycler>(R.id.rvTuningMap)
        rvTuningMap.adapter = TuningMapRecyclerAdapter(opus_manager.tuning_map.clone())


        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(R.string.dlg_tuning)
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val tuning_map = (rvTuningMap.adapter as TuningMapRecyclerAdapter).tuning_map
                val transpose = Pair(etTranspose.get_value() ?: 0, etTransposeRadix.get_value() ?: tuning_map.size)
                this.get_action_interface()._track_tuning_map_and_transpose(tuning_map, transpose)
                opus_manager.set_tuning_map_and_transpose(tuning_map, transpose)
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()

        val default_value = opus_manager.tuning_map.size

        etRadix.set_value(default_value)
        etRadix.set_range(2, 36)
        etRadix.value_set_callback = { new_radix: Int? ->
            rvTuningMap.reset_tuning_map(new_radix)
        }
    }

    override fun on_paganconfig_change(original: PaganConfiguration) {
        super.on_paganconfig_change(original)

        if (this.configuration.soundfont != original.soundfont) {
            this.set_soundfont()
        } else if (this.configuration.sample_rate != original.sample_rate && this.configuration.soundfont != null) {
            this.set_soundfont()
        }

        if (original.allow_midi_playback != this.configuration.allow_midi_playback) {
            if (this.configuration.allow_midi_playback) {
                this.enable_physical_midi_output()
            } else {
                this.block_physical_midi_output()
            }
        }

        if (original.allow_std_percussion != this.configuration.allow_std_percussion) {
            this.populate_supported_soundfont_instrument_names()
        }

        if (original.relative_mode != this.configuration.relative_mode) {
            if (this.active_context_menu is ContextMenuLeaf) {
                this.active_context_menu!!.refresh()
            }
        }

        this.update_menu_options()
    }

    override fun on_project_delete(project: OpusLayerBase) {
        // TODO: Track
        this.drawer_close()
        super.on_project_delete(project)
        this.update_menu_options()
        if (this.get_opus_manager().path == project.path) {
            this.delete_backup()
            this.setup_new()
        }
    }

    fun get_bottom_padding(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val projection_metrics = this.getSystemService(WindowManager::class.java).maximumWindowMetrics
            projection_metrics.bounds.height()
        } else {
            val display_metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            this.windowManager.defaultDisplay.getMetrics(display_metrics)
            display_metrics.heightPixels
        } / 3
    }

}