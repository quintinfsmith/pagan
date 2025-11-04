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
import android.database.Observable
import android.graphics.Color
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.DocumentsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
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
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
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
import androidx.core.view.isVisible
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
import com.qfs.apres.soundfont2.Riff
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WavConverter
import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionAdapter
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionRecycler
import com.qfs.pagan.EditorTable
import com.qfs.pagan.FeedbackDevice
import com.qfs.pagan.HexEditText
import com.qfs.pagan.MidiFeedbackDispatcher
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganBroadcastReceiver
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PlaybackDevice
import com.qfs.pagan.PlaybackFrameMap
import com.qfs.pagan.R
import com.qfs.pagan.TuningMapRecycler
import com.qfs.pagan.TuningMapRecyclerAdapter
import com.qfs.pagan.contextmenu.ContextMenuChannel
import com.qfs.pagan.contextmenu.ContextMenuColumn
import com.qfs.pagan.contextmenu.ContextMenuControlLeaf
import com.qfs.pagan.contextmenu.ContextMenuControlLeafRange
import com.qfs.pagan.contextmenu.ContextMenuControlLine
import com.qfs.pagan.contextmenu.ContextMenuLeaf
import com.qfs.pagan.contextmenu.ContextMenuLeafPercussion
import com.qfs.pagan.contextmenu.ContextMenuLine
import com.qfs.pagan.contextmenu.ContextMenuRange
import com.qfs.pagan.contextmenu.ContextMenuView
import com.qfs.pagan.controlwidgets.ControlWidgetDelay
import com.qfs.pagan.controlwidgets.ControlWidgetPan
import com.qfs.pagan.controlwidgets.ControlWidgetTempo
import com.qfs.pagan.controlwidgets.ControlWidgetVelocity
import com.qfs.pagan.controlwidgets.ControlWidgetVolume
import com.qfs.pagan.databinding.ActivityEditorBinding
import com.qfs.pagan.numberinput.RangedFloatInput
import com.qfs.pagan.numberinput.RangedIntegerInput
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
        var active_project: Uri? = null
        var active_midi_device: MidiDeviceInfo? = null

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

    val editor_view_model: MainViewModel by this.viewModels()
    private var _initial_load = true // Used to prevent save dialog from popping up on first load/new/import
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

    private lateinit var _binding: ActivityEditorBinding
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

    var active_project: Uri?
        get() = this.editor_view_model.active_project
        set(value) {
            this.editor_view_model.active_project = value
        }

    class MultiExporterEventHandler(var activity: ActivityEditor, var total_count: Int): WavConverter.ExporterEventHandler {
        var working_y = 0
        var file_uri: Uri? = null
        var cancelled = false
        val timeout_millis= 5000L
        val MAX_PROGRESS = 100

        val notification_manager = NotificationManagerCompat.from(this.activity)

        override fun on_start() {
            if (this.working_y != 0) {
                return
            }

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
                    .setProgress(this.MAX_PROGRESS, 0, false)
                    .setTimeoutAfter(this.timeout_millis)
                    .setSilent(false)
                    .setContentIntent(pending_go_to_intent)

                @SuppressLint("MissingPermission")
                if (this.activity.has_notification_permission()) {
                    this.notification_manager.notify(this.activity.NOTIFICATION_ID, builder.build())
                }
            }

            this.activity.feedback_msg(this.activity.getString(R.string.export_wav_feedback_complete))

            this.activity.runOnUiThread {
                this.activity.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
                this.activity.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
            }
            this.activity._active_notification = null
        }

        override fun on_cancel() {
            this.cancelled = true
            this.activity.feedback_msg(this.activity.getString(R.string.export_cancelled))
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
            this.activity._active_notification = null
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

    internal var result_launcher_settings =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.getStringExtra(EXTRA_ACTIVE_PROJECT)?.toUri()?.also { uri ->
                    this.active_project = uri
                }
            }
        }

    internal var result_launcher_import =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.also { uri ->
                    this.handle_uri(uri)
                }
            }
        }

    private val _result_launcher_set_project_directory =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.also { result_data ->
                    result_data.data?.also { tree_uri  ->
                        val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        this.contentResolver.takePersistableUriPermission(tree_uri, new_flags)
                        this.configuration.project_directory = tree_uri
                        this.save_configuration()
                        // No need to update the active_project here. using this intent launcher implies the active_project will be changed in the ucheck
                        this.get_project_manager().change_project_path(tree_uri, this.active_project)
                        this._project_save()
                    }
                }
            }
        }

    private var _result_launcher_export_multi_line_wav =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (this._soundfont == null) {
                // Throw Error. Currently unreachable by ui
                return@registerForActivityResult
            }

            this.getNotificationPermission()
            thread {
                if (result.resultCode == RESULT_OK) {
                    result?.data?.data?.also { tree_uri ->
                        if (this.editor_view_model.export_handle != null) {
                            return@thread
                        }
                        val directory = DocumentFile.fromTreeUri(this, tree_uri) ?: return@thread
                        val opus_manager_copy = OpusLayerBase()
                        opus_manager_copy.project_change_json(this.get_opus_manager().to_json())

                        var line_count = 0
                        val skip_lines = mutableSetOf<Pair<Int, Int>>()

                        opus_manager_copy.get_all_channels()
                            .forEachIndexed channel_loop@{ i: Int, channel: OpusChannelAbstract<*, *> ->
                                line_loop@ for (j in 0 until channel.lines.size) {
                                    val line = channel.lines[j]
                                    if (line.muted || channel.muted) {
                                        skip_lines.add(Pair(i, j))
                                        continue
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
                        outer@ for (c in opus_manager_copy.get_all_channels().indices) {
                            val channel = opus_manager_copy.get_channel(c)
                            for (l in channel.lines.indices) {
                                if (skip_lines.contains(Pair(c, l))) {
                                    continue
                                }

                                val file = directory.createFile("audio/wav",
                                    this.getString(R.string.export_wav_lines_filename, c, l)
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

                                for (c_b in opus_manager_copy.get_all_channels().indices) {
                                    val channel_copy = opus_manager_copy.get_channel(c_b)
                                    for (l_b in channel_copy.lines.indices) {
                                        val line_copy = channel_copy.get_line(l_b)
                                        if (c_b == c && l_b == l) {
                                            line_copy.unmute()
                                        } else {
                                            line_copy.mute()
                                        }
                                    }
                                }


                                val parcel_file_descriptor =
                                    this.applicationContext.contentResolver.openFileDescriptor(file_uri, "w")
                                        ?: continue@outer
                                val output_stream =
                                    FileOutputStream(parcel_file_descriptor.fileDescriptor)
                                val buffered_output_stream = BufferedOutputStream(output_stream)
                                val data_output_buffer = DataOutputStream(buffered_output_stream)

                                export_event_handler.update(y++, file_uri)
                                this.editor_view_model.export_wav(
                                    opus_manager_copy,
                                    exporter_sample_handle_manager,
                                    data_output_buffer,
                                    tmp_file,
                                    this.configuration,
                                    export_event_handler,
                                    ignore_global_effects = true,
                                    ignore_channel_effects = true,
                                    ignore_line_effects = false
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
                        }
                    }
                }
            }
        }

    private var _result_launcher_export_multi_channel_wav = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (this._soundfont == null) {
            // Throw Error. Currently unreachable by ui
            return@registerForActivityResult
        }

        this.getNotificationPermission()
        thread {
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.also { tree_uri ->
                    if (this.editor_view_model.export_handle != null) {
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
                            line_loop@ for (line in channel.lines) {
                                if (line.muted || !skip) {
                                    break
                                }

                                for (beat in line.beats) {
                                    if (!beat.is_eventless()) {
                                        skip = false
                                        continue@line_loop
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
                    outer@ for (c in opus_manager_copy.get_all_channels().indices) {
                        if (skip_channels.contains(c)) {
                            continue
                        }

                        val file = directory.createFile(
                            "audio/wav",
                            this.getString(R.string.export_wav_channels_filename, c)
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

                        for (c_b in opus_manager_copy.get_all_channels().indices) {
                            val channel_copy = opus_manager_copy.get_channel(c_b)
                            if (c_b == c) {
                                channel_copy.unmute()
                            } else {
                                channel_copy.mute()
                            }
                        }

                        val parcel_file_descriptor = this.applicationContext.contentResolver.openFileDescriptor(file_uri, "w") ?: continue@outer
                        val output_stream = FileOutputStream(parcel_file_descriptor.fileDescriptor)
                        val buffered_output_stream = BufferedOutputStream(output_stream)
                        val data_output_buffer = DataOutputStream(buffered_output_stream)

                        export_event_handler.update(y++, file_uri)
                        this.editor_view_model.export_wav(
                            opus_manager_copy,
                            exporter_sample_handle_manager,
                            data_output_buffer,
                            tmp_file,
                            this.configuration,
                            export_event_handler,
                            ignore_global_effects = true,
                            ignore_channel_effects = false,
                            ignore_line_effects = false
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
                }
            }
        }
    }

    private var _result_launcher_export_wav =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (this._soundfont == null) {
                // Throw Error. Currently unreachable by ui
                return@registerForActivityResult
            }

            this.getNotificationPermission()
            thread {
                if (result.resultCode == RESULT_OK) {
                    result?.data?.data?.also { uri ->
                        if (this.editor_view_model.export_handle != null) {
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
                            this.resources.getInteger(R.integer.EXPORTED_SAMPLE_RATE),
                            this.resources.getInteger(R.integer.EXPORTED_CHUNK_SIZE)
                        )

                        val parcel_file_descriptor =
                            this.applicationContext.contentResolver.openFileDescriptor(uri, "w")
                                ?: return@thread
                        val output_stream = FileOutputStream(parcel_file_descriptor.fileDescriptor)
                        val buffered_output_stream = BufferedOutputStream(output_stream)
                        val data_output_buffer = DataOutputStream(buffered_output_stream)

                        this.editor_view_model.export_wav(
                            this.get_opus_manager(),
                            exporter_sample_handle_manager,
                            data_output_buffer,
                            tmp_file,
                            this.configuration,
                            object : WavConverter.ExporterEventHandler {
                                val MAX_PROGRESS = 100
                                val timeout_millis = 5000L
                                val notification_manager = NotificationManagerCompat.from(this@ActivityEditor)

                                fun close_buffers() {
                                    data_output_buffer.close()
                                    buffered_output_stream.close()
                                    output_stream.close()
                                    parcel_file_descriptor.close()
                                    tmp_file.delete()
                                }

                                override fun on_start() {
                                    this@ActivityEditor.runOnUiThread {
                                        this@ActivityEditor.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.INVISIBLE
                                        this@ActivityEditor.findViewById<View>(R.id.clExportProgress)?.visibility = View.VISIBLE
                                        this@ActivityEditor.findViewById<ProgressBar>(R.id.export_progress_bar).progress = 0
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
                                            .setTimeoutAfter(this.timeout_millis)
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
                                        this@ActivityEditor.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
                                        this@ActivityEditor.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
                                    }
                                    this@ActivityEditor._active_notification = null
                                }

                                override fun on_cancel() {
                                    this.close_buffers()

                                    this@ActivityEditor.feedback_msg(this@ActivityEditor.getString(R.string.export_cancelled))
                                    this@ActivityEditor.runOnUiThread {
                                        this@ActivityEditor.findViewById<View>(R.id.clExportProgress)?.visibility = View.GONE
                                        this@ActivityEditor.findViewById<MaterialButton>(R.id.btnExportProject)?.visibility = View.VISIBLE
                                    }

                                    val builder = this@ActivityEditor.get_notification() ?: return
                                    builder.setContentText(this@ActivityEditor.getString(R.string.export_cancelled))
                                        .setProgress(0, 0, false)
                                        .setAutoCancel(true)
                                        .setTimeoutAfter(this.timeout_millis)
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
                                    val progress_rounded = (progress * this.MAX_PROGRESS).toInt()
                                    this@ActivityEditor.runOnUiThread {
                                        this@ActivityEditor.findViewById<ProgressBar>(R.id.export_progress_bar)?.progress = progress_rounded
                                    }

                                    val builder = this@ActivityEditor.get_notification() ?: return
                                    builder.setProgress(this.MAX_PROGRESS, progress_rounded, false)

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

    private var _result_launcher_export_project =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val opus_manager = this.get_opus_manager()
                result?.data?.data?.also { uri ->
                    this.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                        val json_string = opus_manager.to_json().to_string()
                        FileOutputStream(it.fileDescriptor).write(json_string.toByteArray())
                        this.feedback_msg(this.getString(R.string.feedback_exported))
                    }
                }
            }
        }

    private var _result_launcher_export_midi =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val opus_manager = this.get_opus_manager()
                result?.data?.data?.also { uri ->
                    this.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                        this.feedback_msg(this.getString(R.string.feedback_exported_to_midi))
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

    // Check if the soundfont was removed
    fun soundfont_file_check() {
        if (this.configuration.soundfont == null) {
            return
        }

        if (this.get_soundfont_uri() == null) {
            this.disable_soundfont()
            this.update_menu_options()
        }
    }

    override fun onResume() {
        this.loading_reticle_hide()
        super.onResume()
        this.drawer_lock()

        this.registerReceiver(
            this.broadcast_receiver,
            this.receiver_intent_filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RECEIVER_NOT_EXPORTED
            } else {
                0
            }
        )

        this.recheck_active_midi_device()
        this.update_title_text()
    }

    fun recheck_active_midi_device() {
        this.editor_view_model.active_midi_device?.let {
            if (this.is_connected_to_physical_device()) {
                this.playback_state_midi = PlaybackState.Ready
                this._midi_interface.open_output_device(it)
            }  else {
                this.set_active_midi_device(null)
            }
        }

        // Second check here if midi device wasn't found
        if (this.editor_view_model.active_midi_device == null) {
            if (this._midi_playback_device != null) {
                this.playback_state_soundfont = PlaybackState.Ready
            }
            this.soundfont_file_check()
        }
    }


    fun save_to_backup() {
        this.view_model.project_manager.save_to_backup(
            this.get_opus_manager(),
            this.active_project
        )
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

    fun load_project(uri: Uri) {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()

        val input_stream = this.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText().toByteArray(Charsets.UTF_8)

        reader.close()
        input_stream?.close()

        this.get_opus_manager().load(content) {
            this.active_project = uri
        }
    }

    fun load_from_bkp() {
        val editor_table = this.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.clear()
        val opus_manager = this.get_opus_manager()
        val project_manager = this.view_model.project_manager
        val (backup_uri, bytes) = project_manager.read_backup()

        opus_manager.load(bytes) {
            this.active_project = backup_uri
        }
    }

    private fun handle_uri(uri: Uri) {
        val type: CompatibleFileType? = try {
            this.get_file_type(uri)
        } catch (_: Exception) {
            null
        }

        val inner_callback: ((Uri) -> Unit) = when (type) {
            CompatibleFileType.Midi1 -> { uri -> this.import_midi(uri) }
            CompatibleFileType.Pagan -> { uri -> this.import_project(uri) }
            else -> { _ -> throw FileNotFoundException(uri.toString()) }
        }

        this.dialog_save_project {
            thread {
                this.loading_reticle_show()
                this.runOnUiThread {
                    this.findViewById<EditorTable>(R.id.etEditorTable).visibility = View.GONE
                    this.clear_context_menu()
                }

                val fallback_msg = try {
                    inner_callback(uri)
                    null
                } catch (_: Exception) {
                    when (type) {
                        CompatibleFileType.Midi1 -> this.getString(R.string.feedback_midi_fail)
                        CompatibleFileType.Pagan -> this.getString(R.string.feedback_import_fail)
                        null -> this.getString(R.string.feedback_file_not_found)
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

        this._midi_interface = object : MidiController(this, false) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.update_menu_options()
                }
            }

            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.update_menu_options()
                    if (device_info == this@ActivityEditor.editor_view_model.active_midi_device) {
                        this@ActivityEditor.set_active_midi_device(null)
                    }
                }
            }
        }

        this._midi_interface.connect_virtual_input_device(this._virtual_input_device)

        // Listens for SongPositionPointer (provided by midi) and scrolls to that beat
        this._midi_interface.connect_virtual_output_device(object : VirtualMidiOutputDevice {
            val FORCE_SCROLL = true
            override fun onSongPositionPointer(event: SongPositionPointer) {
                if (event.get_beat() >= this@ActivityEditor.get_opus_manager().length) {
                    return
                }
                this@ActivityEditor.get_opus_manager().cursor_select_column(event.get_beat())
                // Force scroll here, cursor_select_column doesn't scroll if the column is already visible
                this@ActivityEditor.runOnUiThread {
                    this@ActivityEditor.findViewById<EditorTable>(R.id.etEditorTable)?.scroll_to_position(x = event.get_beat(), force = this.FORCE_SCROLL)
                }
            }
        })

        this._midi_interface.connect_virtual_input_device(this._midi_feedback_dispatcher)

        this.requestedOrientation = this.configuration.force_orientation
        AppCompatDelegate.setDefaultNightMode(this.configuration.night_mode)

        this._binding = ActivityEditorBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)

        val toolbar = this._binding.toolbar
        toolbar.background = null

        toolbar.setOnLongClickListener {
            this.get_action_interface().set_project_name_and_notes()
            true
        }

        this.editor_view_model.action_interface.attach_activity(this)
        this.editor_view_model.opus_manager.attach_activity(this)

        //////////////////////////////////////////
        if (this.editor_view_model.active_midi_device == null) {
            this.get_soundfont_uri()?.let { uri ->
                val sf_file = DocumentFile.fromSingleUri(this, uri) ?: return@let
                if (!sf_file.exists()) return@let

                try {
                    this._soundfont = SoundFont(this, uri)
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
                } catch (_: Riff.InvalidRiff) {
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

        this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val that = this@ActivityEditor
                val opus_manager = that.get_opus_manager()
                val drawer_layout = that.findViewById<DrawerLayout>(R.id.drawer_layout)

                if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                    that.drawer_close()
                } else if (opus_manager.cursor.mode != CursorMode.Unset) {
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
                    savedInstanceState.getInt("x"),
                    savedInstanceState.getInt("y")
                )
            } else {
                this.load_from_bkp()
            }
        } else if (this.intent.getBooleanExtra("load_backup", false)) {
            this.load_from_bkp()
        } else if (this.intent.data == null) {
            this.setup_new()
        } else if (this.get_project_manager().contains(this.intent.data!!)) {
            this.load_project(this.intent.data!!)
        } else {
            this.handle_uri(this.intent.data!!)
        }

        this._initial_load = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main_options_menu, menu)
        this._options_menu = menu

        Handler(Looper.getMainLooper()).post {
            val view: View = this.findViewById<View?>(R.id.itmPlay) ?: return@post
            view.setOnLongClickListener {
                this@ActivityEditor.handle_play_press(true)
                true
            }
        }

        val output = super.onCreateOptionsMenu(menu)
        this.update_menu_options()
        return output
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.itmPlay) {
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
                this.dialog_load_project { uri: Uri ->
                    this.dialog_save_project {
                        val editor_table = this.findViewById<View?>(R.id.etEditorTable)
                        this.get_opus_manager().cursor_clear()
                        thread {
                            this.loading_reticle_show()
                            this.runOnUiThread { editor_table?.visibility = View.GONE }
                            this.get_action_interface().load_project(uri)
                            this.runOnUiThread { editor_table?.visibility = View.VISIBLE }
                            this.loading_reticle_hide()
                        }
                    }
                }
            }

            R.id.itmImportMidi -> {
                this.result_launcher_import.launch(
                    Intent().apply {
                        this.setAction(Intent.ACTION_GET_CONTENT)
                        this.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                    }
                )
            }

            R.id.itmUndo -> {
                this.get_action_interface().apply_undo()
            }

            R.id.itmPlay -> {
                this.handle_play_press(false)
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
            R.id.itmMidiDeviceInfo -> {
                this.dialog_midi_device_management()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun handle_play_press(long_press: Boolean) {
        if (this.editor_view_model.active_midi_device == null) {
            when (this.playback_state_soundfont) {
                PlaybackState.Ready -> {
                    this.playback_start(long_press)
                }

                PlaybackState.Queued,
                PlaybackState.Playing -> {
                    this.playback_stop()
                }

                else -> {}
            }
        } else {
            when (this.playback_state_midi) {
                PlaybackState.Ready -> {
                    this.playback_start_midi_output(long_press)
                }
                PlaybackState.Queued,
                PlaybackState.Playing -> {
                    this.playback_stop_midi_output()
                }
                else -> { /* pass */ }
            }
        }
    }

    fun open_settings() {
        this.loading_reticle_show()
        this.result_launcher_settings.launch(
            Intent(this, ActivitySettings::class.java).apply {
                this@ActivityEditor.active_project?.let {
                    this.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                }
            }
        )
    }

    fun open_about() {
        this.loading_reticle_show()
        this.startActivity(Intent(this, ActivityAbout::class.java))
    }

    private fun _project_save() {
        this.loading_reticle_show()
        this.active_project = this.get_project_manager().save(this.get_opus_manager(), this.active_project, this.configuration.indent_json)
        this.feedback_msg(this.getString(R.string.feedback_project_saved))
        this.update_menu_options()
        this.findViewById<View?>(R.id.btnDeleteProject)?.isEnabled = true
        this.findViewById<View?>(R.id.btnCopyProject)?.isEnabled = true

        this.drawer_close()
        this.loading_reticle_hide()
    }

    fun project_save() {
        if (this.configuration.project_directory == null || DocumentFile.fromTreeUri(this, this.configuration.project_directory!!)?.exists() != true) {
            this._result_launcher_set_project_directory.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                    intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    this.configuration.project_directory?.let {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                    }
                }
            )
        } else {
            this._project_save()
        }
    }

    fun project_move_to_copy() {
        this.dialog_save_project {
            val opus_manager = this.get_opus_manager()
            val old_title = opus_manager.project_name
            val new_title: String? = if (old_title == null) {
                null
            } else {
                "$old_title (Copy)"
            }
            opus_manager.project_name = new_title

            this.active_project = null
            this.update_title_text()
            this.feedback_msg(this.getString(R.string.feedback_on_copy))

            this.findViewById<View>(R.id.btnDeleteProject).isEnabled = false
            this.findViewById<View>(R.id.btnCopyProject).isEnabled = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun _enable_blocker_view() {
        this.findViewById<LinearLayout?>(R.id.llClearOverlay)?.let { blocker_view ->
            if (blocker_view.isVisible) {
                return
            }

            blocker_view.setOnTouchListener { _, motion_event ->
                /* Allow Scrolling on the y axis when scrolling in the main_recycler */
                if (motion_event?.action == 1) {
                    this._blocker_scroll_y = null
                } else if (motion_event?.action == MotionEvent.ACTION_MOVE) {
                    val scroll_view = this.findViewById<EditorTable>(R.id.etEditorTable).get_scroll_view()
                    val rel_y = (motion_event.y - scroll_view.y)
                    scroll_view.scrollBy(0, ((this._blocker_scroll_y ?: rel_y) - rel_y).toInt())
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

    private fun playback_start(loop_playback: Boolean = false) {
        if (!this.update_playback_state_soundfont(PlaybackState.Queued)) {
            this.feedback_msg(this.getString(R.string.playback_failed))
            return
        }

        this._enable_blocker_view()
        this.runOnUiThread {
            this.set_playback_button(R.drawable.baseline_play_disabled_24)
            this.force_title_text(this.getString(R.string.reticle_msg_start_playback))
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

            this._midi_playback_device?.play_opus(start_point, loop_playback)
        }
    }

    private fun playback_start_midi_output(loop_playback: Boolean = false) {
        if (!this.update_playback_state_midi(PlaybackState.Queued)) {
            this.feedback_msg(this.getString(R.string.playback_failed))
            return
        }

        if (this.editor_view_model.active_midi_device == null) {
            this.feedback_msg(this.getString(R.string.midi_device_unset))
            return
        }

        this.force_title_text(this.getString(R.string.reticle_msg_start_playback))
        this.loading_reticle_show()
        this._enable_blocker_view()

        var start_point = this.get_working_column()
        val opus_manager = this.get_opus_manager()
        if (start_point >= opus_manager.length - 1) {
            start_point = 0
        }

        val midi = opus_manager.get_midi(start_point, include_pointers = true)

        this.loading_reticle_hide()
        this.runOnUiThread {
            this.clear_forced_title()
            this.set_playback_button(if (loop_playback) R.drawable.icon_pause_loop else R.drawable.icon_pause)
        }

        if (!this.update_playback_state_midi(PlaybackState.Playing)) {
            this.restore_midi_playback_state()
            return
        }

        thread {
            try {
                this._midi_interface.open_output_device(this.editor_view_model.active_midi_device!!)
                this._virtual_input_device.play_midi(midi, loop_playback) {
                    this.runOnUiThread {
                        this.playback_stop_midi_output()
                    }
                }
            } catch (_: IOException) {
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
                this.set_playback_button(R.drawable.icon_play)
                this._disable_blocker_view()
            }
        }
    }

    fun restore_midi_playback_state() {
        if (this.update_playback_state_midi(PlaybackState.Ready)) {
            this.runOnUiThread {
                this.set_playback_button(R.drawable.icon_play)
                this._disable_blocker_view()
            }
        }
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
        } catch (_: UninitializedPropertyAccessException) {
            // pass, if it's not initialized, it's not locked
        }
    }

    fun update_title_text() {
        this.set_title_text(
            if (this._forced_title_text != null) {
                this._forced_title_text!!
            } else {
                this.get_opus_manager().project_name ?: this.getString(R.string.untitled_opus)
            }
        )
    }

    fun set_title_text(new_text: String) {
        this.runOnUiThread {
            this._binding.toolbar.title = new_text
        }
    }

    fun force_title_text(msg: String) {
        this._forced_title_text = msg
        this.update_title_text()
    }

    fun clear_forced_title() {
        this._forced_title_text = null
        this.update_title_text()
    }

    override fun on_reticle_hide() {
        super.on_reticle_hide()
        this.clear_forced_title()
    }

    override fun on_reticle_show() {
        super.on_reticle_show()
        this.force_title_text(this.getString(R.string.reticle_msg_load))
    }

    fun update_menu_options() {
        val options_menu = this._options_menu ?: return
        val show_midi_devices = (this._midi_interface.output_devices_connected() && this.get_opus_manager().is_tuning_standard())

        options_menu.findItem(R.id.itmLoadProject).isVisible = this.has_projects_saved()
        options_menu.findItem(R.id.itmPlay).isVisible = (this._soundfont != null) || (this.editor_view_model.active_midi_device != null)
        options_menu.findItem(R.id.itmMidiDeviceInfo).isVisible = show_midi_devices
        options_menu.findItem(R.id.itmDebug).isVisible = this.is_debug_on()
    }

    fun setup_project_config_drawer() {
        val opus_manager = this.get_opus_manager()

        this.findViewById<MaterialButton?>(R.id.btnChangeProjectName)?.setOnClickListener {
            this.get_action_interface().set_project_name_and_notes()
        }
        //-------------------------------------------
        this.findViewById<MaterialButton?>(R.id.btnRadix)?.setOnClickListener {
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


        val file_exists = this.active_project != null

        this.findViewById<View?>(R.id.btnDeleteProject)?.let { button ->
            button.isEnabled = file_exists
            button.setOnClickListener {
                if (it.isEnabled) {
                    this.active_project?.let { uri ->
                        this.dialog_delete_project(uri)
                    }
                }
            }
        }

        this.findViewById<View?>(R.id.btnCopyProject)?.let { button ->
            button.isEnabled = file_exists
            button.setOnClickListener {
                if (it.isEnabled) {
                    this.get_action_interface().project_copy()
                }
            }
        }

        this.findViewById<View>(R.id.export_progress_cancel).setOnClickListener {
            this.export_wav_cancel()
        }
    }

    internal fun _build_dialog_title_view(text: String): TextView {
        val output = TextView(ContextThemeWrapper(this, R.style.dialog_title))
        output.text = text
        return output
    }

    internal fun setup_project_config_drawer_export_button() {
        this.findViewById<ConstraintLayout>(R.id.clExportProgress)?.let { export_progress_wrapper ->
            export_progress_wrapper.visibility = if (!this.editor_view_model.is_exporting()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        this.findViewById<View>(R.id.btnExportProject).let {
            val export_options = this.get_exportable_options()
            it.visibility = if (export_options.isEmpty() || this.editor_view_model.is_exporting()) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            it.setOnClickListener {
                this.dialog_popup_menu(this.getString(R.string.dlg_export), export_options, default = null) { _: Int, value: Int ->
                    when (value) {
                        0 -> this.export_project()
                        1 -> this.export_midi_check()
                        2 -> this.export_wav()
                        3 -> this.export_multi_lines_wav()
                        4 -> this.export_multi_channels_wav()
                    }
                }
            }
        }

    }

    private fun get_exportable_options(): List<Triple<Int, Int?, String>> {
        val export_options = mutableListOf<Triple<Int, Int?, String>>(
            Triple(0, null, this.getString(R.string.export_option_json))
        )
        if (this.get_opus_manager().is_tuning_standard()) {
            export_options.add( Triple(1, null, this.getString(R.string.export_option_midi)) )
        }

        if (this.get_soundfont() != null) {
            export_options.add( Triple(2, null, this.getString(R.string.export_option_wav)) )
            export_options.add( Triple(3, null, this.getString(R.string.export_option_wav_lines)) )
            export_options.add( Triple(4, null, this.getString(R.string.export_option_wav_channels)) )
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
        if (this._sample_handle_manager == null || this.editor_view_model.active_midi_device != null) {
            return this._get_default_drum_options()
        }

        val opus_manager = this.get_opus_manager()
        val midi_channel = opus_manager.get_midi_channel(channel_index)

        val preset = try {
            this._sample_handle_manager!!.get_preset(midi_channel) ?: return this._get_default_drum_options()
        } catch (_: SoundFont.InvalidPresetIndex) {
            return this._get_default_drum_options()
        }

        val available_drum_keys = mutableSetOf<Pair<String, Int>>()
        for ((_, preset_instrument) in preset.instruments) {
            if (preset_instrument.instrument == null) continue
            val instrument_range = preset_instrument.key_range ?: Pair(0, 127)

            for (sample_directive in preset_instrument.instrument!!.sample_directives.values) {
                val key_range = sample_directive.key_range ?: Pair(0, 127)
                val usable_range = max(key_range.first, instrument_range.first)..min(key_range.second, instrument_range.second)

                var name = sample_directive.sample!!.first().name
                if (name.contains("(")) {
                    name = name.substring(0, name.indexOf("("))
                }

                for (key in usable_range) {
                    val use_name = if (usable_range.first != usable_range.last) {
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
    // NOTE: Not conforming to GM
    fun update_channel_instruments(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (index == null) {
            this._feedback_sample_manager?.let { handle_manager: SampleHandleManager ->
                for (i in opus_manager.channels.indices) {
                    val channel = opus_manager.channels[i]
                    val midi_channel = opus_manager.get_midi_channel(i)
                    val (midi_bank, midi_program) = channel.get_instrument()
                    this._midi_interface.broadcast_event(BankSelect(midi_channel, midi_bank))
                    this._midi_interface.broadcast_event(ProgramChange(midi_channel, midi_program))

                    handle_manager.select_bank(midi_channel, midi_bank)
                    handle_manager.change_program(midi_channel, midi_program)
                }
            }

            this._sample_handle_manager?.let { handle_manager: SampleHandleManager ->
                // Don't need to update anything but percussion here
                for ((i, channel) in opus_manager.get_percussion_channels()) {
                    val midi_channel = opus_manager.get_midi_channel(i)
                    val (midi_bank, midi_program) = channel.get_instrument()
                    handle_manager.select_bank(midi_channel, midi_bank)
                    handle_manager.change_program(midi_channel, midi_program)
                }
            }
        } else {
            val opus_channel = opus_manager.get_channel(index)
            this.update_channel_instrument(
                opus_manager.get_midi_channel(index),
                opus_channel.get_instrument()
            )
        }
    }

    fun get_opus_manager(): OpusLayerInterface {
        return this.editor_view_model.opus_manager
    }

    fun play_event(channel: Int, event_value: Int, velocity: Float = .5F) {
        if (event_value < 0) return // No sound to play

        val opus_manager = this.get_opus_manager()
        val midi_channel = opus_manager.get_midi_channel(channel)

        val radix = opus_manager.tuning_map.size
        val (note, bend) = if (opus_manager.is_percussion(channel)) { // Ignore the event data and use percussion map
            Pair(event_value + 27, 0)
        } else {
            val octave = event_value / radix
            val offset = opus_manager.tuning_map[event_value % radix]

            val transpose_offset = 12.0 * opus_manager.transpose.first.toDouble() / opus_manager.transpose.second.toDouble()
            val std_offset = 12.0 * offset.first.toDouble() / offset.second.toDouble()

            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            val new_note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21

            Pair(new_note, bend)
        }

        if (note > 127) return

        this._feedback_sample_manager?.let { handle_manager : SampleHandleManager ->
            if (this._temporary_feedback_devices[this._current_feedback_device] == null) {
                this._temporary_feedback_devices[this._current_feedback_device] = FeedbackDevice(this._feedback_sample_manager!!)
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
                    !opus_manager.is_tuning_standard()
                )
            } catch (_: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But i'm sure that'll come back to bite me
            }
        }()
    }

    fun import_project(uri: Uri) {
        this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.get_opus_manager().load(bytes)
            this.active_project = null
        }
    }

    fun import_midi(uri: Uri) {
        val bytes = this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).readBytes()
        } ?: throw InvalidMIDIFile(uri.toString())

        val midi = try {
            Midi.Companion.from_bytes(bytes)
        } catch (_: Exception) {
            throw InvalidMIDIFile(uri.toString())
        }

        val opus_manager = this.get_opus_manager()
        opus_manager.project_change_midi(midi)
        val filename = this.parse_file_name(uri)
        opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: this.getString(
            R.string.default_imported_midi_title)
        )
        opus_manager.clear_history()
        this.active_project = null
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
        val file_path = this.configuration.soundfont
        if (file_path == null) {
            this.disable_soundfont()
            return
        }

        // Failed to change playback_state
        if (!this.update_playback_state_soundfont(PlaybackState.Ready)) return

        val soundfont_directory = this.get_soundfont_directory()
        var soundfont_file = soundfont_directory
        for (segment in file_path.split("/")) {
            soundfont_file = soundfont_file.findFile(segment) ?: throw FileNotFoundException()
        }

        if (!soundfont_file.exists()) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg(this.getString(R.string.soundfont_not_found))
            throw FileNotFoundException()
        }

        try {
            this._soundfont = SoundFont(this, soundfont_file.uri)
        } catch (_: Riff.InvalidRiff) {
            // Possible if user puts the sf2 in their files manually
            this.feedback_msg(this.getString(R.string.invalid_soundfont))
            return
        } catch (_: SoundFont.InvalidSoundFont) {
            // Possible if user puts the sf2 in their files manually
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
            this.findViewById<ChannelOptionRecycler?>(R.id.rvActiveChannels)?.notify_soundfont_changed()
            when (this.get_opus_manager().cursor.mode) {
                CursorMode.Line,
                CursorMode.Channel -> {
                    this.refresh_context_menu()
                }
                else -> {}
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
        rniGreen.set_value(initial_color.green)
        rniBlue.set_value(initial_color.blue)

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
                val string = s.toString()

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
            .setTitle(this.getString(R.string.dlg_title_set_line_color))
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
            .setNegativeButton(this.getString(R.string.color_picker_clear)) { dialog, _ ->
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
                this.window.decorView.rootView as ViewGroup,
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
                this.window.decorView.rootView as ViewGroup,
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

        if (this.active_project == null) {
            return !opus_manager.history_cache.is_empty()
        }

        if (DocumentFile.fromSingleUri(this, this.active_project!!)?.exists() != true) {
            return true
        }

        val input_stream = this.contentResolver.openInputStream(this.active_project!!)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content: ByteArray = reader.readText().toByteArray(Charsets.UTF_8)

        val other = OpusLayerBase()
        other.load(content)

        reader.close()
        input_stream?.close()

        return (opus_manager as OpusLayerBase) != other
    }

    fun dialog_save_project(callback: (Boolean) -> Unit) {
        if (this._initial_load) {
            callback(false)
        } else if (this.needs_save()) {
            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setTitle(R.string.dialog_save_warning_title)
                .setCancelable(true)
                .setPositiveButton(this.getString(R.string.dlg_confirm)) { dialog, _ ->
                    this@ActivityEditor.project_save()
                    dialog.dismiss()
                    callback(true)
                }
                .setNegativeButton(this.getString(R.string.dlg_decline)) { dialog, _ ->
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
        return this.getString(R.string.default_export_name, now.format(formatter))
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
        this._result_launcher_export_multi_line_wav.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also {
                it.putExtra(Intent.EXTRA_TITLE, this.get_export_name())
            }
        )
    }

    fun export_multi_channels_wav() {
        this._result_launcher_export_multi_channel_wav.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also {
                it.putExtra(Intent.EXTRA_TITLE, this.get_export_name())
            }
        )
    }

    fun export_wav() {
        this._result_launcher_export_wav.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).also {
                it.addCategory(Intent.CATEGORY_OPENABLE)
                it.type = "audio/wav"
                it.putExtra(Intent.EXTRA_TITLE, "${this.get_export_name()}.wav")
            }
        )
    }

    fun export_wav_cancel() {
        this.editor_view_model.cancel_export()
    }

    fun export_midi_check() {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.get_percussion_channels().size > 1) {
            val text_view: TextView = TextView(this)
            text_view.text = this.getString(R.string.multiple_kit_warning)

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
        this._result_launcher_export_midi.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).also {
                it.addCategory(Intent.CATEGORY_OPENABLE)
                it.type = "audio/midi"
                it.putExtra(Intent.EXTRA_TITLE, "${this.get_export_name()}.mid")
            }
        )
    }

    fun export_project() {
        this._result_launcher_export_project.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).also {
                it.addCategory(Intent.CATEGORY_OPENABLE)
                it.type = "application/json"
                it.putExtra(Intent.EXTRA_TITLE, "${this.get_export_name()}.json")
            }
        )
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

    fun get_working_column(): Int {
        val cursor = this.get_opus_manager().cursor
        return when (cursor.mode) {
            CursorMode.Single,
            CursorMode.Column -> {
                cursor.beat
            }
            CursorMode.Range -> {
                cursor.get_ordered_range()!!.first.beat
            }
            else -> {
                val editor_table = this.findViewById<EditorTable?>(R.id.etEditorTable)
                editor_table?.get_first_visible_column_index() ?: 0
            }
        }
    }

    fun is_connected_to_physical_device(): Boolean {
        return this._midi_interface.output_devices_connected()
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

            val builder = NotificationCompat.Builder(this, this.CHANNEL_ID)
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
            val mChannel = NotificationChannel(this.CHANNEL_ID, name, importance)
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
            this.getSystemService(VIBRATOR_SERVICE) as Vibrator
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
        return this.editor_view_model.action_interface
    }

    fun save_actions() {
        val generated_code = this.get_action_interface().to_json().to_string()
        val timestamp = System.currentTimeMillis()
        val file_name = "${this.dataDir}/generated_$timestamp.json"

        val file = File(file_name)
        file.writeText(generated_code)
        this.get_action_interface().clear()
    }

    override fun on_crash() {
        if (this.is_debug_on()) {
            this.save_actions()
        }
        this.save_to_backup()
    }

    override fun onDestroy() {
        this._sample_handle_manager?.destroy()
        this._feedback_sample_manager?.destroy()
        super.onDestroy()
    }

    internal fun dialog_popup_selection_offset() {
        val view_inflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_note_offset,
                this.window.decorView.rootView as ViewGroup,
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
        this.active_context_menu?.let { context_menu: ContextMenuView ->
            context_menu.destroy()
            context_menu.primary?.parent?.let {
                (it as ViewGroup).removeAllViews()
            }
            context_menu.secondary?.parent?.let {
                (it as ViewGroup).removeAllViews()
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
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (!(this.active_context_menu?.matches_cursor(cursor) ?: false)) {
            this.clear_context_menu()
            val is_initial_event = true
            val widget = when (cursor.ctl_type!!) {
                EffectType.Tempo -> {
                    ControlWidgetTempo(cursor.ctl_level!!, is_initial_event, this) { event: OpusTempoEvent ->
                        opus_manager.set_initial_event(event)
                    }
                }
                EffectType.Volume -> {
                    ControlWidgetVolume(cursor.ctl_level!!, is_initial_event, this) { event: OpusVolumeEvent ->
                        opus_manager.set_initial_event(event)
                    }
                }
                EffectType.Velocity -> {
                    ControlWidgetVelocity(cursor.ctl_level!!, is_initial_event, this) { event: OpusVelocityEvent ->
                        opus_manager.set_initial_event(event)
                    }
                }

                EffectType.Pan -> {
                    ControlWidgetPan(cursor.ctl_level!!, is_initial_event, this) { event: OpusPanEvent ->
                        opus_manager.set_initial_event(event)
                    }
                }

                EffectType.Delay -> {
                    ControlWidgetDelay(cursor.ctl_level!!, is_initial_event, this) { event: DelayEvent ->
                        opus_manager.set_initial_event(event)
                    }
                }

                EffectType.Reverb -> TODO()
                EffectType.LowPass -> TODO()
            }


            this.active_context_menu = ContextMenuControlLine(
                widget,
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        } else {
            this.active_context_menu?.refresh()
        }

        this.show_context_menus()
    }

    internal fun set_context_menu_line_control_leaf() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (!(this.active_context_menu?.matches_cursor(cursor) ?: false)) {
            this.clear_context_menu()
            val is_initial_event = false
            val widget = when (cursor.ctl_type!!) {
                EffectType.Tempo -> {
                    ControlWidgetTempo(cursor.ctl_level!!, is_initial_event, this) { event: OpusTempoEvent ->
                        opus_manager.set_event_at_cursor(event)
                    }
                }

                EffectType.Volume -> {
                    ControlWidgetVolume(cursor.ctl_level!!, is_initial_event, this) { event: OpusVolumeEvent ->
                        opus_manager.set_event_at_cursor(event)
                    }
                }

                EffectType.Velocity -> {
                    ControlWidgetVelocity(cursor.ctl_level!!, is_initial_event, this) { event: OpusVelocityEvent ->
                        opus_manager.set_event_at_cursor(event)
                    }
                }

                EffectType.Pan -> {
                    ControlWidgetPan(cursor.ctl_level!!, is_initial_event, this) { event: OpusPanEvent ->
                        opus_manager.set_event_at_cursor(event)
                    }
                }

                EffectType.Delay -> {
                    ControlWidgetDelay(cursor.ctl_level!!, is_initial_event, this) { event: DelayEvent ->
                        opus_manager.set_event_at_cursor(event)
                    }
                }

                EffectType.Reverb -> TODO()
                EffectType.LowPass -> TODO()
            }

            this.active_context_menu = ContextMenuControlLeaf(
                widget,
                this.findViewById<LinearLayout>(R.id.llContextMenuPrimary),
                this.findViewById<LinearLayout>(R.id.llContextMenuSecondary)
            )
        } else {
            this.active_context_menu?.refresh()
        }
        this.show_context_menus()
    }

    internal fun set_context_menu_line_control_leaf_b() {
        if (!this.refresh_or_clear_context_menu<ContextMenuControlLeafRange>()) {
            this.active_context_menu = ContextMenuControlLeafRange(
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


        title_text.text = this.resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)
        title_text.contentDescription = this.resources.getString(R.string.label_shortcut_scrollbar, scroll_bar.progress)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = this@ActivityEditor.resources.getString(R.string.label_shortcut_scrollbar, p1)
                title_text.contentDescription = this@ActivityEditor.resources.getString(R.string.label_shortcut_scrollbar, p1)
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
                        this.getString(R.string.section_spinner_item, i, keys[i - 1])
                    } else {
                        "${keys[i - 1]}: $section_name"
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

    private inline fun <reified T: ContextMenuView?> refresh_or_clear_context_menu(): Boolean {
        if (this.active_context_menu !is T) {
            this.clear_context_menu()
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

        val etTranspose = viewInflated.findViewById<RangedIntegerInput>(R.id.etTranspose)
        etTranspose.set_range(0)
        etTranspose.set_value(opus_manager.transpose.first)

        val etTransposeRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etTransposeRadix)
        etTransposeRadix.set_range(1)
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

        val etRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etRadix)
        etRadix.set_value(default_value)
        etRadix.set_range(
            this.resources.getInteger(R.integer.minimum_octave_size),
            this.resources.getInteger(R.integer.maximum_octave_size)
        )
        etRadix.value_set_callback = { new_radix: Int? ->
            rvTuningMap.reset_tuning_map(new_radix)
        }
    }

    override fun on_paganconfig_change(original: PaganConfiguration) {
        super.on_paganconfig_change(original)

        if (this.editor_view_model.active_midi_device == null) {
            if (this.configuration.soundfont != original.soundfont) {
                this.set_soundfont()
            } else if (this.configuration.sample_rate != original.sample_rate && this.configuration.soundfont != null) {
                this.set_soundfont()
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

    override fun on_project_delete(uri: Uri) {
        // TODO: Track
        this.drawer_close()
        super.on_project_delete(uri)
        this.update_menu_options()
        if (this.active_project == uri) {
            this.view_model.project_manager.delete_backup()
            this.setup_new()
        }
    }

    fun dialog_midi_device_management() {

        val options = mutableListOf<Triple<MidiDeviceInfo?, Int?, String>>(
            Triple(null, null, this.getString(R.string.device_menu_default_name))
        )

        for (device_info in this._midi_interface.poll_output_devices()) {
            options.add(
                Triple(
                    device_info,
                    null,
                    device_info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: this.getString( R.string.unknown_midi_device, device_info.id )
                )
            )
        }

        this.dialog_popup_menu(this.getString(R.string.playback_device), options, this.editor_view_model.active_midi_device) { i: Int, device: MidiDeviceInfo? ->
            this.set_active_midi_device(device)
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

    fun get_effect_transition_icon(transition: EffectTransition): Int {
        return when (transition) {
            EffectTransition.Instant -> R.drawable.icon_transition_immediate
            EffectTransition.Linear -> R.drawable.icon_transition_linear
            EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
            EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
        }
    }

    fun set_active_midi_device(device_info: MidiDeviceInfo?) {
        val current_device_info = this.editor_view_model.active_midi_device
        if (device_info == current_device_info) {
            return
        }

        when (this.playback_state_soundfont) {
            PlaybackState.Playing,
            PlaybackState.Queued -> {
                this.playback_stop()
            }
            else -> { /* pass */ }
        }

        when (this.playback_state_midi) {
            PlaybackState.Playing,
            PlaybackState.Queued -> {
                this.playback_stop_midi_output()
            }
            else -> { /* pass */ }
        }
        if (current_device_info != null) {
            this._midi_interface.close_device(current_device_info)
        } else {
            this.disconnect_feedback_device()
        }

        this.editor_view_model.active_midi_device = device_info

        if (device_info != null) {
            this._midi_interface.open_output_device(device_info)
            this.playback_state_midi = PlaybackState.Ready
            this.disable_soundfont()
            // These 2 otherwise get handled in set_soundfont()
            this.populate_supported_soundfont_instrument_names()
            this.findViewById<ChannelOptionRecycler?>(R.id.rvActiveChannels)?.notify_soundfont_changed()
        } else {
            this.set_soundfont()
        }

        this.update_menu_options()
    }

}
