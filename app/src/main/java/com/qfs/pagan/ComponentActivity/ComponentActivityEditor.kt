package com.qfs.pagan.ComponentActivity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.MidiController
import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.soundfont2.Riff
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.Exportable
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.MultiExporterEventHandler
import com.qfs.pagan.PaganBroadcastReceiver
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.R
import com.qfs.pagan.SingleExporterEventHandler
import com.qfs.pagan.Values
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.DrawerCard
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.DropdownMenuItem
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.composable.button.ConfigDrawerBottomButton
import com.qfs.pagan.composable.button.ConfigDrawerChannelLeftButton
import com.qfs.pagan.composable.button.ConfigDrawerChannelRightButton
import com.qfs.pagan.composable.button.ConfigDrawerTopButton
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.TopBarIcon
import com.qfs.pagan.composable.conditional_drag
import com.qfs.pagan.composable.cxtmenu.CMBoxBottom
import com.qfs.pagan.composable.cxtmenu.CMBoxEnd
import com.qfs.pagan.composable.cxtmenu.CMPadding
import com.qfs.pagan.composable.cxtmenu.ContextMenuChannelPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuChannelSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuColumnPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuColumnSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLeafCtlSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLeafPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLeafStdSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLinePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLineSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuRangeSecondary
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.composable.dragging_scroll
import com.qfs.pagan.composable.is_light
import com.qfs.pagan.composable.keyboardAsState
import com.qfs.pagan.composable.long_press
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class ComponentActivityEditor: PaganComponentActivity() {
    val controller_model: ViewModelEditorController by this.viewModels()
    val state_model: ViewModelEditorState by this.viewModels()
    lateinit var action_interface: ActionTracker

    private var broadcast_receiver = PaganBroadcastReceiver()
    private var receiver_intent_filter = IntentFilter("com.qfs.pagan.CANCEL_EXPORT_WAV")

    // Notification shiz -------------------------------------------------
    var NOTIFICATION_ID = 0
    val CHANNEL_ID = "com.qfs.pagan" // TODO: Use String Resource
    private var _notification_channel: NotificationChannel? = null
    var active_notification: NotificationCompat.Builder? = null
    // -------------------------------------------------------------------

    private var _result_launcher_export_multi_line_wav =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val soundfont = this.controller_model.audio_interface.soundfont ?: return@registerForActivityResult

            this.getNotificationPermission()
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val tree_uri = result.data?.data ?: return@registerForActivityResult
            if (this.controller_model.export_handle != null) return@registerForActivityResult
            val directory = DocumentFile.fromTreeUri(this, tree_uri) ?: return@registerForActivityResult
            thread {
                val opus_manager_copy = OpusLayerBase()

                this.controller_model.opus_manager.to_json().let {
                    opus_manager_copy.project_change_json(it)
                }

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

                val export_event_handler = MultiExporterEventHandler(this, this.state_model, line_count)

                var y = 0
                outer@ for (c in opus_manager_copy.get_all_channels().indices) {
                    val channel = opus_manager_copy.get_channel(c)
                    for (l in channel.lines.indices) {
                        if (skip_lines.contains(Pair(c, l))) continue

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
                        val exporter_sample_handle_manager = SampleHandleManager(soundfont, Values.ExportSampleRate, Values.ExportBufferSize)

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
                        this.controller_model.export_wav(
                            opus_manager_copy,
                            exporter_sample_handle_manager,
                            data_output_buffer,
                            tmp_file,
                            this.view_model.configuration,
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

                        if (export_event_handler.cancelled) break@outer
                    }
                }
            }
        }

    private var _result_launcher_export_multi_channel_wav = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val soundfont = this.controller_model.audio_interface.soundfont ?: return@registerForActivityResult
        val tree_uri = result.data?.data ?: return@registerForActivityResult
        this.getNotificationPermission()
        thread {
            if (this.controller_model.export_handle != null) return@thread
            val directory = DocumentFile.fromTreeUri(this, tree_uri) ?: return@thread

            val opus_manager_copy = OpusLayerBase()
            this.controller_model.opus_manager.to_json().let {
                opus_manager_copy.project_change_json(it)
            }

            var channel_count = 0
            val skip_channels = mutableSetOf<Int>()

            opus_manager_copy.get_all_channels().forEachIndexed channel_loop@{ i: Int, channel: OpusChannelAbstract<*, *> ->
                if (channel.muted) {
                    skip_channels.add(i)
                    return@channel_loop
                }

                var skip = true
                line_loop@ for (line in channel.lines) {
                    if (line.muted || !skip) break

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

            val export_event_handler = MultiExporterEventHandler(this, this.state_model, channel_count)

            var y = 0
            outer@ for (c in opus_manager_copy.get_all_channels().indices) {
                if (skip_channels.contains(c)) continue

                val file = directory.createFile("audio/wav", this.getString(R.string.export_wav_channels_filename, c)) ?: continue
                val file_uri = file.uri

                /* TMP file is necessary since we can't easily predict the exact frame count. */
                val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                if (tmp_file.exists()) {
                    tmp_file.delete()
                }

                tmp_file.deleteOnExit()
                val exporter_sample_handle_manager = SampleHandleManager(soundfont, Values.ExportSampleRate, Values.ExportBufferSize)

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
                this.controller_model.export_wav(
                    opus_manager_copy,
                    exporter_sample_handle_manager,
                    data_output_buffer,
                    tmp_file,
                    this.view_model.configuration,
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

                if (export_event_handler.cancelled) break@outer
            }
        }
    }

    private var _result_launcher_export_wav =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val soundfont = this.controller_model.audio_interface.soundfont ?: return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult

            this.getNotificationPermission()
            thread {
                if (this.controller_model.export_handle != null) return@thread

                /* TMP file is necessary since we can't easily predict the exact frame count. */
                val tmp_file = File("${this.filesDir}/.tmp_wav_data")
                if (tmp_file.exists()) {
                    tmp_file.delete()
                }

                tmp_file.deleteOnExit()
                val exporter_sample_handle_manager = SampleHandleManager(
                    soundfont,
                    this.resources.getInteger(R.integer.EXPORTED_SAMPLE_RATE),
                    this.resources.getInteger(R.integer.EXPORTED_CHUNK_SIZE)
                )

                val parcel_file_descriptor = this.applicationContext.contentResolver.openFileDescriptor(uri, "w") ?: return@thread
                val output_stream = FileOutputStream(parcel_file_descriptor.fileDescriptor)
                val buffered_output_stream = BufferedOutputStream(output_stream)
                val data_output_buffer = DataOutputStream(buffered_output_stream)

                this.controller_model.export_wav(
                    this.controller_model.opus_manager,
                    exporter_sample_handle_manager,
                    data_output_buffer,
                    tmp_file,
                    this.view_model.configuration,
                    SingleExporterEventHandler(this, this.state_model, uri) {
                        data_output_buffer.close()
                        buffered_output_stream.close()
                        output_stream.close()
                        parcel_file_descriptor.close()
                        tmp_file.delete()
                    }
                )


                exporter_sample_handle_manager.destroy()
            }
        }

    private var _result_launcher_export_project =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            val opus_manager = this.controller_model.opus_manager
            this.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                val json_string = opus_manager.to_json().to_string()
                FileOutputStream(it.fileDescriptor).write(json_string.toByteArray())
                Toast.makeText(this, this.getString(R.string.feedback_exported), Toast.LENGTH_SHORT)
            }
        }

    private var _result_launcher_export_midi =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            val opus_manager = this.controller_model.opus_manager
            this.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).write(opus_manager.get_midi().as_bytes())
                Toast.makeText(this, this.getString(R.string.feedback_exported_to_midi), Toast.LENGTH_SHORT)
            }
        }

    private val _result_launcher_set_project_directory_and_save =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val result_data = result.data ?: return@registerForActivityResult
            val tree_uri = result_data.data ?: return@registerForActivityResult

            val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            this.contentResolver.takePersistableUriPermission(tree_uri, new_flags)

            this.view_model.configuration.project_directory.value = tree_uri
            this.view_model.save_configuration()

            this.view_model.project_manager?.change_project_path(tree_uri, this.controller_model.active_project)
            this@ComponentActivityEditor.action_interface.save()

            this.reload_config()
        }

    internal var result_launcher_settings =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            this.reload_config()
        }

    internal var result_launcher_import = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            this.action_interface.save_before {
                this.handle_uri(uri)
            }
        }


    private lateinit var _midi_interface: MidiController
    //private var _sample_handle_manager: SampleHandleManager? = null
    // private var _feedback_sample_manager: SampleHandleManager? = null
    fun bind_midi_interface() {
        this._midi_interface = object : MidiController(this, false) {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                val that = this@ComponentActivityEditor
                that.controller_model.midi_devices_connected++
                that.state_model.midi_device_connected.value = true
            }
            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                val that = this@ComponentActivityEditor
                that.controller_model.midi_devices_connected--
                if (device_info == that.controller_model.active_midi_device) {
                    this.close_device(device_info)
                    that.controller_model.set_active_midi_device(null)
                    that.state_model.playback_state_midi.value = that.controller_model.playback_state_midi
                    that.state_model.set_use_midi_playback(false)
                }
                that.state_model.midi_device_connected.value = (that.controller_model.midi_devices_connected != 0)
            }
        }


        // Listens for SongPositionPointer (provided by midi) and scrolls to that beat
        this._midi_interface.connect_virtual_output_device(object : VirtualMidiOutputDevice {
            override fun onSongPositionPointer(event: SongPositionPointer) {
                val that = this@ComponentActivityEditor
                val opus_manager = that.controller_model.opus_manager
                if (event.get_beat() >= opus_manager.length) return
                opus_manager.cursor_select_column(event.get_beat())
            }
        })

        this._midi_interface.connect_virtual_input_device(this.controller_model.virtual_midi_device)
        val output_devices = this._midi_interface.poll_output_devices()
        this.state_model.midi_device_connected.value = output_devices.isNotEmpty()
        this.controller_model.midi_devices_connected = output_devices.size
    }

    fun unbind_midi_interface() {
        this._midi_interface.disconnect_virtual_input_device(this.controller_model.virtual_midi_device)
    }

    override fun on_back_press_check(): Boolean {
        val active_cursor = this.state_model.active_cursor.value
        return if (active_cursor != null && active_cursor.type != CursorMode.Unset) {
            this.action_interface.cursor_clear()
            false
        } else {
            true
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        action_interface = ActionTracker(this, this.controller_model)
        val dispatcher = this.action_interface
        this.controller_model.attach_state_model(this.state_model)

        this.registerReceiver(
            this.broadcast_receiver,
            this.receiver_intent_filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RECEIVER_NOT_EXPORTED
            } else {
                0
            }
        )

        dispatcher.attach_top_model(this.view_model)

        this.bind_midi_interface()
        super.onCreate(savedInstanceState)

        // TODO: remember density instead of specific width
        this.state_model.base_leaf_width.value = toPx(Dimensions.LeafBaseWidth)

        thread {
            if (savedInstanceState != null) {
                dispatcher.load_from_bkp()
            } else if (this.intent.getBooleanExtra("load_backup", false)) {
                dispatcher.load_from_bkp()
            } else if (this.intent.data == null) {
                dispatcher.new_project()
            } else if (this.view_model.project_manager?.contains(this.intent.data!!) == true) {
                this@ComponentActivityEditor.load_project(this@ComponentActivityEditor.intent.data!!)
            } else {
                this.handle_uri(this.intent.data!!)
            }
        }
    }

    override fun on_config_load() {
        super.on_config_load()
        // If the project directory moved, update the active project uri
        this.controller_model.active_project?.let { project_uri ->
            this.view_model.project_manager?.let { project_manager ->
                if (!project_manager.contains(project_uri)) {
                    this.controller_model.active_project = null
                    this.controller_model.project_exists.value = false
                }
            }
        }
        this.state_model.latest_input_indicator.value = this.view_model.configuration.latest_input_indicator.value
        this.set_soundfont()
    }

    fun set_soundfont() {
        // Ensure playback is stopped
        this.action_interface.stop_opus_midi()
        this.action_interface.stop_opus()

        val file_path = this.view_model.configuration.soundfont.value
        if (file_path == null) {
            this.controller_model.unset_soundfont()
            this.state_model.unset_soundfont()
            return
        }

        // Failed to change playback_state
        if (!this.controller_model.update_playback_state_soundfont(PlaybackState.Ready)) return

        val soundfont_directory = this.get_soundfont_directory()
        var soundfont_file = soundfont_directory
        for (segment in file_path.split("/")) {
            soundfont_file = soundfont_file.findFile(segment) ?: throw FileNotFoundException()
        }

        // Possible if user puts the sf2 in their files manually
        if (!soundfont_file.exists()) throw FileNotFoundException()

        try {
            val soundfont = SoundFont(this, soundfont_file.uri)
            this.controller_model.set_soundfont(soundfont)
            this.controller_model.playback_device?.activity = this
            this.controller_model.active_soundfont_relative_path = file_path
            this.state_model.enable_soundfont()
        } catch (_: Riff.InvalidRiff) {
            // Possible if user puts the sf2 in their files manually
            //this.feedback_msg(this.getString(R.string.invalid_soundfont))
            return
        } catch (_: SoundFont.InvalidSoundFont) {
            // Possible if user puts the sf2 in their files manually
            //this.feedback_msg("Invalid Soundfont")
            return
        }

        // TODO: Update percussion minimums
    }


    fun get_file_type(uri: Uri): CompatibleFileType {
        return this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val test_bytes = ByteArray(4)
            val input_stream = FileInputStream(it.fileDescriptor)
            input_stream.read(test_bytes)
            if (test_bytes.contentEquals("MThd".toByteArray())) {
                CompatibleFileType.Midi1
            } else {
                CompatibleFileType.Pagan
            }
        } ?: throw FileNotFoundException(uri.toString())
    }

    fun import_project(uri: Uri) {
        this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            this.state_model.ready.value = false
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.controller_model.opus_manager.load(bytes)
            this.controller_model.active_project = null
            this.controller_model.project_exists.value = false
        }
    }

    fun import_midi(uri: Uri) {
        this.state_model.ready.value = false
        val bytes = this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).readBytes()
        } ?: throw InvalidMIDIFile(uri.toString())

        val midi = try {
            Midi.from_bytes(bytes)
        } catch (_: Exception) {
            throw InvalidMIDIFile(uri.toString())
        }

        val opus_manager = this.controller_model.opus_manager
        opus_manager.project_change_midi(midi)
        val filename = this.parse_file_name(uri)
        opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: this.getString( R.string.default_imported_midi_title))
        opus_manager.clear_history()
        this.controller_model.active_project = null
        this.controller_model.project_exists.value = false
    }

    fun parse_file_name(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            this.contentResolver.query(uri, null, null, null, null)?.let { cursor ->
                if (cursor.moveToFirst()) {
                    val ci = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ci >= 0) {
                        result = cursor.getString(ci)
                    }
                }
                cursor.close()
            }
        }

        if (result == null && uri.path is String) {
            result = uri.path!!
            result = result.substring(result.lastIndexOf("/") + 1)
        }

        return result
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

        val fallback_msg = try {
            inner_callback(uri)
            null
        } catch (e: Exception) {
            when (type) {
                CompatibleFileType.Midi1 -> this.getString(R.string.feedback_midi_fail)
                CompatibleFileType.Pagan -> this.getString(R.string.feedback_import_fail)
                null -> this.getString(R.string.feedback_file_not_found)
            }
        }

        if (fallback_msg != null) {
            this.action_interface.new_project()
            runOnUiThread {
                this.toast(fallback_msg)
            }
        }
    }

    override fun onStop() {
        this.action_interface.stop_opus_midi()
        this.action_interface.stop_opus()
        super.onStop()
    }

    override fun onDestroy() {
        this.save_to_backup()
        this.controller_model.playback_device?.activity = null
        this.unregisterReceiver(this.broadcast_receiver)
        super.onDestroy()
    }

    @Composable
    fun NoPlayButton() {
        TopBarIcon(
            icon = R.drawable.icon_play,
            description = R.string.menu_item_playpause,
            onClick = {
                this.view_model.create_dialog { close ->
                    @Composable {
                        SText(
                            R.string.need_soundfont_playback_warning,
                            textAlign = TextAlign.Center
                        )
                        DialogBar(positive = close)
                    }
                }
            },
        )
    }

    @Composable
    fun PlaySFButton() {
        val dispatcher = this@ComponentActivityEditor.action_interface
        val scope = rememberCoroutineScope()
        TopBarIcon(
            icon = when (this@ComponentActivityEditor.state_model.playback_state_soundfont.value) {
                PlaybackState.Queued,
                PlaybackState.NotReady -> R.drawable.baseline_play_disabled_24

                PlaybackState.Ready -> R.drawable.icon_play
                PlaybackState.Stopping,
                PlaybackState.Playing -> if (this@ComponentActivityEditor.state_model.looping_playback.value) {
                    R.drawable.icon_pause_loop
                } else {
                    R.drawable.icon_pause
                }
            },
            description = R.string.menu_item_playpause,
            onClick = {
                scope.launch {
                    when (this@ComponentActivityEditor.controller_model.playback_state_soundfont) {
                        PlaybackState.Queued,
                        PlaybackState.Stopping,
                        PlaybackState.NotReady -> {}
                        PlaybackState.Ready -> {
                            dispatcher.play_opus(this, false)
                        }

                        PlaybackState.Playing -> {
                            dispatcher.stop_opus()
                        }
                    }
                }
            },
            onLongClick = {
                scope.launch {
                    when (this@ComponentActivityEditor.controller_model.playback_state_soundfont) {
                        PlaybackState.Queued,
                        PlaybackState.Stopping,
                        PlaybackState.NotReady -> {}
                        PlaybackState.Ready -> { dispatcher.play_opus(this, true) }
                        PlaybackState.Playing -> { dispatcher.stop_opus() }
                    }
                }
            }
        )
    }
    @Composable
    fun PlayMidiButton() {
        val dispatcher = this@ComponentActivityEditor.action_interface
        val scope = rememberCoroutineScope()
        TopBarIcon(
            icon = when (this@ComponentActivityEditor.state_model.playback_state_midi.value) {
                PlaybackState.Queued,
                PlaybackState.NotReady -> R.drawable.baseline_play_disabled_24

                PlaybackState.Ready -> R.drawable.icon_play
                PlaybackState.Stopping,
                PlaybackState.Playing -> if (this@ComponentActivityEditor.state_model.looping_playback.value) {
                    R.drawable.icon_pause_loop
                } else {
                    R.drawable.icon_pause
                }
            },
            description = R.string.menu_item_playpause,
            onClick = {
                scope.launch {
                    when (this@ComponentActivityEditor.controller_model.playback_state_midi) {
                        PlaybackState.Queued,
                        PlaybackState.Stopping,
                        PlaybackState.NotReady -> {}
                        PlaybackState.Ready -> {
                            dispatcher.play_opus_midi()
                        }

                        PlaybackState.Playing -> {
                            dispatcher.stop_opus_midi()
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun TopBarDropDown() {
        val vm_top = this@ComponentActivityEditor.view_model
        val vm_state = this@ComponentActivityEditor.state_model
        val vm_controller = this@ComponentActivityEditor.controller_model
        val dispatcher = this@ComponentActivityEditor.action_interface

        val menu_items: MutableList<Pair<Int, () -> Unit>> = mutableListOf(
            Pair(R.string.menu_item_new_project) {
                dispatcher.save_before {
                    dispatcher.new_project()
                }
            }
        )

        if (vm_top.has_saved_project.value) {
            menu_items.add(
                Pair(R.string.menu_item_load_project) {
                    this@ComponentActivityEditor.load_menu_dialog { uri ->
                        dispatcher.save_before {
                            this@ComponentActivityEditor.load_project(uri)
                        }
                    }
                }
            )
        }

        menu_items.add(
            Pair(R.string.menu_item_import) {
                this@ComponentActivityEditor.result_launcher_import.launch(
                    Intent().apply {
                        this.setAction(Intent.ACTION_GET_CONTENT)
                        this.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                    }
                )
            }
        )

        if (vm_state.midi_device_connected.value) {
            menu_items.add(
                Pair(R.string.playback_device) {
                    vm_top.create_dialog { close ->
                        @Composable {
                            val options = mutableListOf<Pair<MidiDeviceInfo?, @Composable RowScope.() -> Unit>>(
                                Pair(null) { SText(R.string.device_menu_default_name) }
                            )

                            for (device_info in this@ComponentActivityEditor._midi_interface.poll_output_devices()) {
                                options.add(
                                    Pair(device_info) {
                                        Text(
                                            device_info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                                ?: stringResource(R.string.unknown_midi_device, device_info.id)
                                        )
                                    }
                                )
                            }

                            DialogSTitle(R.string.playback_device)
                            UnSortableMenu<MidiDeviceInfo?>(
                                modifier = Modifier,
                                options = options,
                                default_value = vm_controller.active_midi_device
                            ) { device ->
                                close()
                                vm_controller.set_active_midi_device(device)

                                device?.let {
                                    this@ComponentActivityEditor._midi_interface.open_output_device(it)
                                }
                                vm_state.set_use_midi_playback(device != null)
                                vm_state.playback_state_midi.value = vm_controller.playback_state_midi
                            }
                            DialogBar(neutral = close)
                        }
                    }
                }
            )
        }

        menu_items.add(
            Pair(R.string.menu_item_settings) { this@ComponentActivityEditor.open_settings() }
        )
        menu_items.add(
            Pair(R.string.menu_item_about) { this@ComponentActivityEditor.open_about() }
        )

        Box {
            val expanded = remember { mutableStateOf(false) }
            TopBarIcon(
                icon = R.drawable.icon_kebab,
                description = R.string.menu_item_playpause,
                contentAlignment = Alignment.CenterEnd,
                onClick = { expanded.value = !expanded.value }
            )
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                for ((_, item) in menu_items.enumerate()) {
                    DropdownMenuItem(
                        text = { SText(item.first) },
                        onClick = {
                            expanded.value = false
                            item.second()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun RowScope.TopBarNoTitle() {
        val vm_state = this@ComponentActivityEditor.state_model
        val dispatcher = this@ComponentActivityEditor.action_interface

        Spacer(Modifier.weight(1F))

        if (vm_state.use_midi_playback.value) {
            PlayMidiButton()
        } else if (vm_state.soundfont_active.value != null) {
            PlaySFButton()
        } else {
            NoPlayButton()
        }
        Spacer(Modifier.weight(1F))

        TopBarIcon(
            icon = R.drawable.icon_undo,
            description = R.string.menu_item_undo,
            onClick = {
                if (vm_state.playback_state_midi.value != PlaybackState.Playing && vm_state.playback_state_soundfont.value != PlaybackState.Playing ) {
                    dispatcher.apply_undo()
                }
            }
        )
        Spacer(Modifier.weight(1F))
    }
    @Composable
    fun RowScope.TopBarWithTitle() {
        val vm_state = this@ComponentActivityEditor.state_model
        val dispatcher = this@ComponentActivityEditor.action_interface

        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
        if (vm_state.use_midi_playback.value) {
            PlayMidiButton()
        } else if (vm_state.soundfont_active.value != null) {
            PlaySFButton()
        } else {
            NoPlayButton()
        }
        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
        Row(
            modifier = Modifier.weight(1F),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            dispatcher.set_project_name_and_notes()
                        }
                    ),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                maxLines = 1,
                text = vm_state.project_name.value ?: stringResource(R.string.untitled_opus)
            )
        }

        TopBarIcon(
            icon = R.drawable.icon_undo,
            description = R.string.menu_item_undo,
            onClick = {
                if (vm_state.playback_state_midi.value != PlaybackState.Playing && vm_state.playback_state_soundfont.value != PlaybackState.Playing ) {
                    dispatcher.apply_undo()
                }
            }
        )
        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
    }

    @Composable
    fun RowScope.ActiveTopBar() {
        val vm_top = this@ComponentActivityEditor.view_model
        val scope = rememberCoroutineScope()

        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
        TopBarIcon(
            icon = R.drawable.icon_hamburger_32,
            description = R.string.song_configuration,
            contentAlignment = Alignment.CenterStart,
            onClick = {
                scope.launch {
                    this@ComponentActivityEditor.open_drawer()
                }
            }
        )

        when (vm_top.active_layout_size.value) {
            LayoutSize.SmallPortrait -> TopBarNoTitle()
            else -> TopBarWithTitle()
        }

        TopBarDropDown()
    }

    @Composable
    fun RowScope.LoadingTopBar() {
        Box(Modifier.fillMaxSize()) {}
    }

    override val top_bar_wrapper: @Composable (RowScope.() -> Unit) = {
        val vm_controller = this@ComponentActivityEditor.controller_model
        val vm_state = vm_controller.opus_manager.vm_state

        if (!vm_state.ready.value) {
            LoadingTopBar()
        } else {
            ActiveTopBar()
        }
    }

    fun get_context_menu_primary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize): (@Composable () -> Unit)? {
        if (ui_facade.playback_state_midi.value == PlaybackState.Playing || ui_facade.playback_state_soundfont.value == PlaybackState.Playing) return null
        if (ui_facade.dragging_line.value != null) return null

        val cursor = ui_facade.active_cursor.value
        return when (cursor?.type) {
            CursorMode.Line -> {
                @Composable { ContextMenuLinePrimary(modifier, ui_facade, dispatcher, layout) }
            }
            CursorMode.Column -> {
                @Composable { ContextMenuColumnPrimary(modifier, ui_facade, dispatcher, layout) }
            }
            CursorMode.Single -> {
                @Composable {
                    ContextMenuLeafPrimary(
                        modifier,
                        ui_facade,
                        dispatcher,
                        layout
                    )
                }
            }
            CursorMode.Channel -> {
                @Composable { ContextMenuChannelPrimary(modifier, ui_facade, dispatcher, layout) }
            }
            CursorMode.Range,
            CursorMode.Unset,
            null -> {
                null
            }
        }
    }

    fun get_context_menu_secondary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize): (@Composable () -> Unit)? {
        if (ui_facade.playback_state_midi.value == PlaybackState.Playing || ui_facade.playback_state_soundfont.value == PlaybackState.Playing) return null
        if (ui_facade.dragging_line.value != null) return null
        val cursor = ui_facade.active_cursor.value ?: return null
        if (cursor.type == CursorMode.Unset) return null

        return when (cursor.type) {
            CursorMode.Line -> {
                @Composable { ContextMenuLineSecondary(ui_facade, dispatcher, layout = layout) }
            }
            CursorMode.Single -> {
                val cursor = ui_facade.active_cursor.value ?: return null
                val line_data = ui_facade.line_data[cursor.ints[0]]

                @Composable {
                    key(ui_facade.active_event.value, ui_facade.active_event_descriptor.value) {
                        if (line_data.ctl_type.value == null) {
                            ContextMenuLeafStdSecondary(ui_facade, dispatcher, modifier, layout)
                        } else {
                            ContextMenuLeafCtlSecondary(ui_facade, dispatcher, modifier, layout)
                        }
                    }
                }
            }
            CursorMode.Range -> {
                @Composable {
                    ContextMenuRangeSecondary(
                        ui_facade,
                        dispatcher,
                        this@ComponentActivityEditor.view_model.configuration.move_mode.value
                    )
                }
            }

            CursorMode.Channel -> {
                @Composable { ContextMenuChannelSecondary(ui_facade, dispatcher, layout) }
            }
            CursorMode.Column -> {
                @Composable { ContextMenuColumnSecondary(modifier, ui_facade, dispatcher, layout) }
            }
            CursorMode.Unset -> null
        }
    }

    @Composable
    fun BoxScope.MainTableBackground() {
        Spacer(
            Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .width(dimensionResource(R.dimen.line_label_width)),
        )
        Row {
            Spacer(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .width(dimensionResource(R.dimen.line_label_width))
                    .height(dimensionResource(R.dimen.line_height)),
            )

            Spacer(
                Modifier
                    .weight(1F)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .height(dimensionResource(R.dimen.line_height)),
            )
        }
    }

    @Composable
    fun MainTable(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, length: MutableState<Int>, layout: LayoutSize) {
        val line_height = dimensionResource(R.dimen.line_height)
        val ctl_line_height = dimensionResource(R.dimen.ctl_line_height)
        val leaf_width = Dimensions.LeafBaseWidth
        val line_label_width = dimensionResource(R.dimen.line_label_width)
        val column_widths = Array(ui_facade.beat_count.value) { i ->
            ui_facade.column_data[i].top_weight.value
        }

        val channel_gap_height = dimensionResource(R.dimen.channel_gap_size)

        val scope = rememberCoroutineScope()
        val scroll_state_v = ui_facade.scroll_state_y.value
        val scroll_state_h = ui_facade.scroll_state_x.value

        Box(
            modifier,
            contentAlignment = Alignment.TopStart
        ) {
            if (ui_facade.ready.value) {
                MainTableBackground()
            }
            val (dragging_to_y, is_after) = ui_facade.calculate_dragged_to_line() ?: Pair(null, false)
            Row {
                ProvideContentColorTextStyle(contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                    Column(Modifier.width(line_label_width)) {
                        Column(Modifier.height(line_height)) {
                            ShortcutView(Modifier.weight(1F), dispatcher, scope, scroll_state_h)
                            TableLine(MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(
                            Modifier
                                .dragging_scroll(
                                    ui_facade.dragging_line.value != null,
                                    scroll_state_v
                                )
                        ) {
                            for (y in 0 until ui_facade.line_count.value) {
                                val use_height = if (ui_facade.line_data[y].ctl_type.value != null) {
                                    ctl_line_height
                                } else {
                                    line_height
                                }
                                val is_dragging = remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .draggable_line(y, dragging_to_y, is_after)
                                        .then(
                                            // Make std lines draggable
                                            if (ui_facade.line_data[y].ctl_type.value == null) {
                                                Modifier
                                                    .onPlaced { coordinates ->
                                                        if (y == ui_facade.dragging_line.value && ui_facade.dragging_abs_offset.value == null) {
                                                            ui_facade.dragging_abs_offset.value =
                                                                coordinates.positionInParent().y
                                                        }
                                                    }
                                                    .long_press(
                                                        onPress = { is_dragging.value = true },
                                                        onRelease = { is_dragging.value = false }
                                                    )
                                                    .conditional_drag(
                                                        is_dragging,
                                                        on_drag_start = { position ->
                                                            ui_facade.start_dragging(y, position)
                                                            ui_facade.update_line_map(this@ComponentActivityEditor.build_dragging_line_map())
                                                        },

                                                        on_drag_stop = {
                                                            dragging_to_y?.let {
                                                                val from_line =
                                                                    ui_facade.line_data[ui_facade.dragging_line.value!!]
                                                                val to_line = ui_facade.line_data[it]
                                                                if (ui_facade.is_dragging_channel()) {
                                                                    dispatcher.move_channel(
                                                                        from_line.channel.value!!,
                                                                        to_line.channel.value!!,
                                                                        !is_after
                                                                    )
                                                                } else if (to_line.line_offset.value != null) {
                                                                    dispatcher.move_line(
                                                                        from_line.channel.value!!,
                                                                        from_line.line_offset.value!!,
                                                                        to_line.channel.value!!,
                                                                        to_line.line_offset.value!!,
                                                                        !is_after
                                                                    )
                                                                }
                                                            }
                                                            ui_facade.stop_dragging()
                                                        },

                                                        on_drag = { delta ->
                                                            ui_facade.dragging_offset.value += delta
                                                        },
                                                        scroll_state = scroll_state_v
                                                    )

                                            } else {
                                                Modifier
                                            }
                                        )
                                        .height(use_height),
                                    content = {
                                        if (ui_facade.draw_top_line(y)) {
                                            TableLine(MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        key(ui_facade.line_data[y].hashCode()) {
                                            LineLabelView(
                                                modifier = Modifier
                                                    .weight(1F)
                                                    .fillMaxWidth(),
                                                dispatcher,
                                                ui_facade.line_data[y]
                                            )
                                        }
                                        TableLine(MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                )

                                if ((y == ui_facade.line_data.size - 1 || ui_facade.line_data[y].channel.value != ui_facade.line_data[y + 1].channel.value) && ui_facade.line_data[y].channel.value != null) {
                                    Spacer(
                                        modifier = Modifier
                                            .draggable_line(y, dragging_to_y, is_after, true)
                                            .width(dimensionResource(R.dimen.line_label_width))
                                            .height(channel_gap_height)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                            Row(
                                Modifier
                                    .height(line_height)
                                    .combinedClickable(
                                        onClick = { dispatcher.show_hidden_global_controller() }
                                    )
                            ) {
                                if (this@ComponentActivityEditor.state_model.has_global_effects_hidden.value) {
                                    Icon(
                                        modifier = Modifier
                                            .background(color = Color.Transparent, CircleShape)
                                            .padding(6.dp),
                                        painter = painterResource(R.drawable.icon_ctl),
                                        contentDescription = stringResource(R.string.cd_show_effect_controls)
                                    )
                                }
                            }
                            Spacer(Modifier.height(toDp(this@ComponentActivityEditor.state_model.table_bottom_padding.value)))
                        }
                    }
                }

                // Key to prevent incongruence between column_width size and content
                key(ui_facade.beat_count.value) {
                    LazyRow(
                        state = scroll_state_h,
                        contentPadding = PaddingValues(end = toDp(this@ComponentActivityEditor.state_model.table_side_padding.value)),
                        overscrollEffect = null
                    ) {
                        itemsIndexed(column_widths + listOf(1)) { x, width ->
                            if (x == column_widths.size) {
                                ProvideContentColorTextStyle(contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                                    Box(
                                        modifier = Modifier
                                            .width(Dimensions.LeafBaseWidth)
                                            .combinedClickable(
                                                onClick = { dispatcher.append_beats() },
                                                onLongClick = { dispatcher.append_beats() }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(6.dp),
                                            painter = painterResource(R.drawable.icon_add),
                                            contentDescription = stringResource(R.string.cd_insert_beat)
                                        )
                                    }
                                }

                                return@itemsIndexed
                            }

                            Column {
                                Column(
                                    Modifier
                                        .width(leaf_width * width)
                                        .height(line_height),
                                ) {
                                    BeatLabelView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1F),
                                        x = x,
                                        ui_facade = ui_facade,
                                        dispatcher = dispatcher,
                                        column_info = ui_facade.column_data[x],
                                        column_width = (leaf_width * width)
                                    )
                                    TableLine(MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(
                                    Modifier
                                        .verticalScroll(scroll_state_v, overscrollEffect = null)
                                        .width(leaf_width * width)
                                ) {
                                    for (y in 0 until ui_facade.line_count.value) {
                                        val cell = ui_facade.cell_map[y][x]
                                        Column(
                                            Modifier
                                                .draggable_line(y, dragging_to_y, is_after)
                                                .height(
                                                    if (ui_facade.line_data[y].ctl_type.value != null) ctl_line_height
                                                    else line_height
                                                )
                                        ) {
                                            if (ui_facade.draw_top_line(y)) {
                                                TableLine(MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            CellView(ui_facade, dispatcher, cell, y, x, Modifier.weight(1F))
                                            TableLine(MaterialTheme.colorScheme.onBackground)
                                        }

                                        if ((y == ui_facade.line_data.size - 1 || ui_facade.line_data[y].channel.value != ui_facade.line_data[y + 1].channel.value) && ui_facade.line_data[y].channel.value != null) {
                                            Row(
                                                Modifier
                                                    .draggable_line(y, dragging_to_y, is_after, true)
                                                    .fillMaxWidth()
                                                    .height(channel_gap_height)
                                                    .background(MaterialTheme.colorScheme.onBackground)
                                            ) { }
                                        }
                                    }
                                    Spacer(Modifier.height(line_height + toDp(this@ComponentActivityEditor.state_model.table_bottom_padding.value)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BoxScope.TableLine(color: Color) {
        Spacer(
            Modifier
                .background(color = color)
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(dimensionResource(R.dimen.table_line_stroke))
        )
    }

    @Composable
    fun RowScope.TableLine(color: Color) {
        Spacer(
            Modifier
                .background(color = color)
                .fillMaxHeight()
                .width(dimensionResource(R.dimen.table_line_stroke))
        )
    }

    @Composable
    fun ColumnScope.TableLine(color: Color) {
        Spacer(
            Modifier
                .background(color = color)
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.table_line_stroke))
        )
    }

    @Composable
    fun Modifier.draggable_line(y: Int, dragging_to_y: Int?, is_after: Boolean, is_channel_gap: Boolean = false): Modifier {
        val is_dragging = this@ComponentActivityEditor.state_model.line_data[y].is_dragging.value
        return this then Modifier
            .zIndex(
                if (is_dragging && !is_channel_gap) 2F
                else 0F
            )
            .offset {
                IntOffset(
                    0,
                    this@ComponentActivityEditor.get_dragged_offset(y, dragging_to_y, is_after, is_channel_gap)
                )
            }

    }

    @Composable
    fun ShortcutView(modifier: Modifier, dispatcher: ActionTracker, scope: CoroutineScope, scroll_state: LazyListState) {
        HalfBorderBox(
            modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RectangleShape)
                .combinedClickable(
                    onClick = { dispatcher.cursor_select_column() },
                    onLongClick = {
                        dispatcher.cursor_select_column(0)
                        scope.launch { scroll_state.scrollToItem(0) }
                    }
                ),
            border_color = MaterialTheme.colorScheme.onSurfaceVariant,
            content = {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.padding(7.dp),
                        painter = painterResource(R.drawable.icon_shortcut),
                        contentDescription = stringResource(R.string.jump_to_section)
                    )
                }
            }
        )
    }

    @Composable
    fun HalfBorderBox(
        modifier: Modifier = Modifier,
        border_width: Dp = dimensionResource(R.dimen.table_line_stroke),
        border_color: Color,
        content: @Composable BoxScope.() -> Unit) {
        Box(
            modifier,
            contentAlignment = Alignment.BottomEnd,
            content = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    content = content,
                    contentAlignment = Alignment.Center
                )
                Spacer(
                    Modifier
                        .width(border_width)
                        .background(border_color)
                        .fillMaxHeight()
                )
            }
        )
    }

    @Composable
    fun LineLabelView(
        modifier: Modifier = Modifier,
        dispatcher: ActionTracker,
        line_info: ViewModelEditorState.LineData
    ) {
        val ctl_type = line_info.ctl_type.value
        val (background, foreground) = if (!line_info.is_selected.value) {
            Pair(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        }

        ProvideContentColorTextStyle(foreground, Typography.LineLabel) {
            HalfBorderBox(
                modifier
                    .combinedClickable(
                        onClick = {
                            dispatcher.tap_line(
                                line_info.channel.value,
                                line_info.line_offset.value,
                                line_info.ctl_type.value
                            )
                        },
                        onLongClick = {
                            dispatcher.long_tap_line(
                                line_info.channel.value,
                                line_info.line_offset.value,
                                line_info.ctl_type.value,
                            )
                        }
                    )
                    .background(
                        shape = RectangleShape,
                        color = background
                    ),
                border_color = MaterialTheme.colorScheme.onSurfaceVariant,
                content = {
                    if (ctl_type == null) {
                        val (label_a, label_b) = if (line_info.assigned_offset.value != null) {
                            Pair("!${line_info.channel.value}", "${line_info.assigned_offset.value}")
                        } else {
                            Pair("${line_info.channel.value}", "${line_info.line_offset.value}")
                        }

                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(label_a, maxLines = 1)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = label_b,
                                    maxLines = 1,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    } else {
                        val (drawable_id, description_id) = when (ctl_type) {
                            EffectType.Tempo -> Pair(R.drawable.icon_tempo, R.string.ctl_desc_tempo)
                            EffectType.Velocity -> Pair(R.drawable.icon_velocity, R.string.ctl_desc_velocity)
                            EffectType.Volume -> Pair(R.drawable.icon_volume, R.string.ctl_desc_volume)
                            EffectType.Delay -> Pair(R.drawable.icon_echo, R.string.ctl_desc_delay)
                            EffectType.Pan -> Pair(R.drawable.icon_pan, R.string.ctl_desc_pan)
                            EffectType.LowPass -> TODO()
                            EffectType.Reverb -> TODO()
                        }
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.padding(4.dp),
                                painter = painterResource(drawable_id),
                                contentDescription = stringResource(description_id)
                            )
                        }
                    }
                }
            )
        }
    }

    @Composable
    fun BeatLabelView(
        modifier: Modifier = Modifier,
        x: Int,
        ui_facade: ViewModelEditorState,
        dispatcher: ActionTracker,
        column_info: ViewModelEditorState.ColumnData,
        column_width: Dp // Necessary for floating label
    ) {
        val (background, foreground) = if (!column_info.is_selected.value) {
            Pair(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        }

        ProvideContentColorTextStyle(foreground, Typography.BeatLabel) {
            HalfBorderBox(
                modifier
                    .background(
                        color = background,
                        shape = RectangleShape
                    )
                    .combinedClickable(
                        onClick = {
                            dispatcher.cursor_select_column(x)
                        },
                    )
                    .fillMaxSize(),
                border_color = MaterialTheme.colorScheme.onSurfaceVariant,
                content = {
                    if (state_model.active_wide_beat.value == x && LocalContext.current.toPx(Dimensions.LeafBaseWidth) * ui_facade.column_data[x].top_weight.value > ui_facade.scroll_state_x.value.layoutInfo.viewportSize.width * 1.5) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(ui_facade.scroll_state_x.value.layoutInfo.viewportSize.width.dp / 5)
                                .graphicsLayer {
                                    val width_px = column_width.toPx()
                                    val scroll_offset =
                                        ui_facade.scroll_state_x.value.firstVisibleItemScrollOffset.toFloat()
                                    val viewport_width = ui_facade.scroll_state_x.value.layoutInfo.viewportSize.width
                                    val floating_position = (width_px / -2F) + (viewport_width / 4) + scroll_offset
                                    translationX = floating_position
                                },
                            color = if (column_info.is_selected.value) {
                                MaterialTheme.colorScheme.onTertiary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            trackColor = if (column_info.is_selected.value) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            drawStopIndicator = {},
                            progress = { state_model.wide_beat_progress.value },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val width_px = column_width.toPx()
                                val viewport_width = ui_facade.scroll_state_x.value.layoutInfo.viewportSize.width
                                translationX = if (width_px >= viewport_width) {
                                    val visible_items = ui_facade.scroll_state_x.value.layoutInfo.visibleItemsInfo
                                    if (ui_facade.scroll_state_x.value.firstVisibleItemIndex == x) {
                                        val scroll_offset =
                                            ui_facade.scroll_state_x.value.firstVisibleItemScrollOffset.toFloat()
                                        val floating_position = ((viewport_width - width_px) / 2F) + scroll_offset
                                        val end_position = ((width_px - viewport_width) / 2F)
                                        if (floating_position < end_position) {
                                            state_model.active_wide_beat.value = x
                                            state_model.wide_beat_progress.value =
                                                scroll_offset / (width_px - viewport_width)
                                            floating_position
                                        } else {
                                            if (state_model.active_wide_beat.value == x) {
                                                state_model.active_wide_beat.value = null
                                            }
                                            end_position
                                        }
                                    } else if (visible_items.isNotEmpty() && visible_items.last().index == x) {
                                        if (state_model.active_wide_beat.value == x) {
                                            state_model.active_wide_beat.value = null
                                        }
                                        (viewport_width - width_px) / 2F
                                    } else {
                                        if (state_model.active_wide_beat.value == x) {
                                            state_model.active_wide_beat.value = null
                                        }
                                        0F
                                    }
                                } else {
                                    0F
                                }
                            }
                            .fillMaxHeight()
                            .widthIn(Dimensions.LeafBaseWidth - 6.dp)
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .then(
                                if (column_info.is_tagged.value) Modifier.dashed_border(
                                    foreground,
                                    Shapes.TaggedBeat,
                                    1.dp
                                )
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                        content = {
                            Text(text = "$x")
                        }
                    )
                }
            )
        }
    }

    @Composable
    fun LeafView(
        channel_data: ViewModelEditorState.ChannelData?,
        line_data: ViewModelEditorState.LineData,
        leaf_data: ViewModelEditorState.LeafData,
        radix: Int,
        modifier: Modifier = Modifier
    ) {
        val event = leaf_data.event.value
        val leaf_state = if (leaf_data.is_spillover.value) Colors.LeafState.Spill
        else if (event != null) Colors.LeafState.Active
        else Colors.LeafState.Empty

        val leaf_selection = if (leaf_data.is_selected.value) Colors.LeafSelection.Primary
        else if (leaf_data.is_secondary.value) Colors.LeafSelection.Secondary
        else Colors.LeafSelection.Unselected

        val (leaf_color, text_color) = Colors.get_leaf_color(
            line_data.palette.value ?: OpusColorPalette(),
            channel_data?.palette?.value ?: OpusColorPalette(),
            leaf_state,
            leaf_selection,
            line_data.ctl_type.value != null,
            line_data.is_mute.value || channel_data?.is_mute?.value == true,
            !MaterialTheme.colorScheme.is_light()
        )

        ProvideContentColorTextStyle(contentColor = text_color) {
            Box(
                modifier
                    .fillMaxHeight()
                    .background(color = leaf_color),
                contentAlignment = Alignment.Center
            ) {
                when (event) {
                    is AbsoluteNoteEvent -> {
                        val octave = event.note / radix
                        val offset = event.note % radix
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.Center) {
                                Spacer(Modifier.weight(4F))
                                ProvideTextStyle(Typography.Leaf.Octave) {
                                    Text("$octave")
                                }
                                Spacer(Modifier.weight(1F))
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                ProvideTextStyle(Typography.Leaf.Offset) {
                                    Text("$offset")
                                }
                            }
                        }
                    }

                    is RelativeNoteEvent -> {
                        val octave = abs(event.offset) / radix
                        val offset = abs(event.offset) % radix
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                                    Text(
                                        text = if (event.offset > 0) "+" else "-",
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                                ProvideTextStyle(Typography.Leaf.Octave) {
                                    Text(
                                        text = "$octave",
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(1.dp))
                            ProvideTextStyle(Typography.Leaf.Offset) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("$offset")
                                }
                            }
                        }
                    }

                    is PercussionEvent -> Icon(
                        modifier = Modifier.padding(8.dp),
                        painter = painterResource(R.drawable.percussion_indicator),
                        contentDescription = ""
                    )

                    is OpusVolumeEvent -> Text("${(event.value * 100F).toInt()}%", color = text_color)
                    is OpusPanEvent -> {
                        Text(
                            text = if (event.value < 0) {
                                "<${(abs(event.value) * 10).roundToInt()}"
                            } else if (event.value > 0) {
                                "${(abs(event.value) * 10).roundToInt()}>"
                            } else {
                                "-0-"
                            },
                            color = text_color
                        )
                    }

                    is DelayEvent -> {
                        if (event.echo == 0 || event.fade == 0F) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = text_color,
                                    radius = (size.height * .1F),
                                    center = Offset(size.width / 2F, size.height / 2F)
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(Dimensions.LeafBaseWidth)
                                ) {
                                    drawLine(
                                        start = Offset((.1F * size.width), (.65F * size.height)),
                                        end = Offset((size.width * .9F), (.35F * size.height)),
                                        color = text_color,
                                        strokeWidth = 1F
                                    )
                                }
                                ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        Column(
                                            verticalArrangement = Arrangement.Top,
                                            modifier = Modifier.fillMaxHeight(),
                                            content = { Text("${event.numerator}") }
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Column(
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier.fillMaxHeight(),
                                            content = { Text("${event.denominator}") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is OpusTempoEvent -> Text("${event.value.roundToInt()}", color = text_color)
                    is OpusVelocityEvent -> Text("${(event.value * 100F).toInt()}%", color = text_color)
                    null -> {}
                }
                TableLine(MaterialTheme.colorScheme.onBackground)
            }
        }
    }

    private fun <T> get_leaf_list(tree: ReducibleTree<T>): List<Triple<List<Int>, ReducibleTree<T>,Float>> {
        val output = mutableListOf<Triple<List<Int>, ReducibleTree<T>, Float>>()
        tree.weighted_traverse { subtree, event, path, weight ->
            if (subtree.is_leaf()) {
                output.add(Triple(path, subtree, weight))
            }
        }
        return output
    }

    @Composable
    fun CellView(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, cell: MutableState<ViewModelEditorState.TreeData>, y: Int, x: Int, modifier: Modifier = Modifier) {
        val line_info = ui_facade.line_data[y]
        key(cell.value.key.value, y) {
            Row(modifier.fillMaxSize()) {
                for ((path, leaf_data) in cell.value.leafs) {
                    this@ComponentActivityEditor.LeafView(
                        line_info.channel.value?.let { ui_facade.channel_data[it] },
                        line_info,
                        leaf_data.value,
                        ui_facade.radix.value,
                        Modifier
                            .weight(leaf_data.value.weight.floatValue)
                            .combinedClickable(
                                onClick = {
                                    dispatcher.tap_leaf(
                                        x,
                                        path,
                                        line_info.channel.value,
                                        line_info.line_offset.value,
                                        line_info.ctl_type.value
                                    )
                                },
                                onLongClick = {
                                    dispatcher.long_tap_leaf(
                                        x,
                                        path,
                                        line_info.channel.value,
                                        line_info.line_offset.value,
                                        line_info.ctl_type.value
                                    )
                                }
                            )
                    )
                }
            }
        }
    }

    @Composable
    fun RowScope.DrawerPadder() {
        Spacer(modifier = Modifier.width(Dimensions.ConfigDrawerPadding))
    }
    @Composable
    fun ColumnScope.DrawerPadder() {
        Spacer(modifier = Modifier.height(Dimensions.ConfigDrawerPadding))
    }

    @Composable
    override fun Drawer(modifier: Modifier) {
        val dispatcher = this.action_interface
        val state_model = this.state_model
        val top_model = this.view_model
        val scope = rememberCoroutineScope()

        val scroll_state = rememberScrollState()
        val dragging_row_index: MutableState<Int?> = remember { mutableStateOf(null) }
        val dragging_row_offset: MutableState<Float?> = remember { mutableStateOf(null) }

        val context = LocalContext.current

        DrawerCard(
            modifier
                .width(IntrinsicSize.Min)
                .imePadding()
        ) {
            if (top_model.active_layout_size.value == LayoutSize.SmallPortrait) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = {
                                dispatcher.set_project_name_and_notes()
                            }
                        ),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    text = state_model.project_name.value ?: stringResource(R.string.untitled_opus)
                )
            }

            Row(
                Modifier.padding(Dimensions.ConfigDrawerPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConfigDrawerTopButton(
                    onClick = { dispatcher.set_tuning_table_and_transpose() },
                    content = { SText(R.string.label_tuning) }
                )
                Spacer(Modifier.weight(1F))
                ConfigDrawerChannelLeftButton(
                    onClick = {
                        dispatcher.insert_percussion_channel(-1)
                    },
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.icon_add_bang),
                            contentDescription = stringResource(R.string.btn_cfg_add_kit_channel),
                        )
                    }
                )
                DrawerPadder()
                ConfigDrawerChannelRightButton(
                    onClick = { dispatcher.insert_channel(-1) },
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.icon_add_circle),
                            contentDescription = stringResource(R.string.btn_cfg_add_channel),
                        )
                    }
                )
            }
            DrawerPadder()
            Surface(
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1F)
            ) {
                if (state_model.ready.value) {
                    Column(
                        Modifier
                            .padding(Dimensions.ConfigDrawerPadding)
                            .dragging_scroll(
                                dragging_row_index.value != null,
                                scroll_state,
                            )
                    ) {
                        val row_height = Dimensions.ConfigChannelButtonHeight
                        val padding_height_px = this@ComponentActivityEditor.toPx(Dimensions.ConfigChannelSpacing)
                        val row_height_px = this@ComponentActivityEditor.toPx(row_height)
                        for (i in 0 until state_model.channel_count.value) {
                            val channel_data = state_model.channel_data[i]
                            val is_dragging = remember { mutableStateOf(false) }
                            key(channel_data.update_key.value) {

                                Row(
                                    Modifier
                                        .zIndex(
                                            if (is_dragging.value) {
                                                2F
                                            } else {
                                                0F
                                            }
                                        )
                                        .offset(
                                            x = 0.dp,
                                            y = if (dragging_row_index.value == null || dragging_row_offset.value == null) {
                                                0.dp
                                            } else if (dragging_row_index.value!! == i) {
                                                context.toDp(dragging_row_offset.value!!)
                                            } else if (dragging_row_index.value!! < i) {
                                                val dragged_position =
                                                    (padding_height_px * dragging_row_index.value!!) + (row_height_px * dragging_row_index.value!!) + dragging_row_offset.value!!
                                                if (((padding_height_px + row_height_px) * i) < dragged_position) {
                                                    (row_height * -1)
                                                } else {
                                                    0.dp
                                                }
                                            } else {
                                                val dragged_position =
                                                    (padding_height_px * dragging_row_index.value!!) + (row_height_px * dragging_row_index.value!!) + dragging_row_offset.value!!
                                                if (((padding_height_px + row_height_px) * i) > dragged_position) {
                                                    row_height
                                                } else {
                                                    0.dp
                                                }
                                            }
                                        )
                                        .long_press(
                                            onPress = { is_dragging.value = true },
                                            onRelease = { is_dragging.value = false }
                                        )
                                        .conditional_drag(
                                            is_dragging,
                                            on_drag_start = { position ->
                                                dragging_row_offset.value = 0F
                                                dragging_row_index.value = i
                                            },

                                            on_drag_stop = {
                                                dragging_row_index.value?.let {
                                                    val dragged_position =
                                                        (padding_height_px * it) + (row_height_px * it) + dragging_row_offset.value!!
                                                    val new_channel_position =
                                                        max(0F, ceil(dragged_position / row_height_px))
                                                    dispatcher.move_channel(i, new_channel_position.toInt(), true)
                                                }
                                                dragging_row_offset.value = null
                                                dragging_row_index.value = null
                                            },

                                            on_drag = { delta ->
                                                dragging_row_offset.value = dragging_row_offset.value!! + delta
                                            },
                                            scroll_state = scroll_state
                                        )
                                ) {
                                    ConfigDrawerChannelLeftButton(
                                        modifier = Modifier.weight(1F),
                                        onClick = { dispatcher.set_channel_preset(i) },
                                        content = {
                                            Row(
                                                Modifier.weight(1F),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (channel_data.percussion.value) "!%02d:".format(i)
                                                    else "%02d:".format(i),
                                                    modifier = Modifier
                                                        .padding(
                                                            vertical = 0.dp,
                                                            horizontal = 12.dp
                                                        )
                                                )
                                                Text(
                                                    channel_data.active_name.value
                                                        ?: this@ComponentActivityEditor.get_default_preset_name(
                                                            channel_data.instrument.value.first,
                                                            channel_data.instrument.value.second
                                                        ),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1F),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    )
                                    DrawerPadder()
                                    ConfigDrawerChannelRightButton(
                                        onClick = { dispatcher.remove_channel(i) },
                                        content = {
                                            Icon(
                                                painter = painterResource(
                                                    if (channel_data.percussion.value) {
                                                        R.drawable.icon_subtract_bang
                                                    } else {
                                                        R.drawable.icon_subtract_circle
                                                    }
                                                ),
                                                contentDescription = stringResource(R.string.remove_channel, i)
                                            )
                                        }
                                    )
                                }
                            }
                            DrawerPadder()
                        }
                    }
                }
            }
            DrawerPadder()
            Row(
                modifier = Modifier
                    .padding(Dimensions.ConfigDrawerPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfigDrawerBottomButton(
                    modifier = Modifier.weight(1F),
                    icon = R.drawable.icon_save,
                    description = R.string.btn_cfg_save,
                    onClick = {
                        scope.launch { this@ComponentActivityEditor.close_drawer() }
                        val configuration = this@ComponentActivityEditor.view_model.configuration
                        if (configuration.project_directory.value == null || DocumentFile.fromTreeUri(
                                this@ComponentActivityEditor,
                                configuration.project_directory.value!!
                            )?.exists() != true
                        ) {
                            this@ComponentActivityEditor._result_launcher_set_project_directory_and_save.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                                    intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
                                    intent.flags =
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    configuration.project_directory.value?.let {
                                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                                    }
                                }
                            )
                        } else {
                            dispatcher.save()
                        }
                    }
                )
                DrawerPadder()
                ConfigDrawerBottomButton(
                    modifier = Modifier.weight(1F),
                    icon = R.drawable.icon_ic_baseline_content_copy_24,
                    description = R.string.btn_cfg_copy,
                    enabled = this@ComponentActivityEditor.controller_model.project_exists.value,
                    onClick = {
                        scope.launch { this@ComponentActivityEditor.close_drawer() }
                        dispatcher.project_copy()
                    }
                )
                DrawerPadder()
                ConfigDrawerBottomButton(
                    modifier = Modifier.weight(1F),
                    icon = R.drawable.icon_trash,
                    description = R.string.btn_cfg_delete,
                    enabled = this@ComponentActivityEditor.controller_model.project_exists.value,
                    onClick = {
                        val controller_model = this@ComponentActivityEditor.controller_model
                        val opus_manager = controller_model.opus_manager
                        scope.launch { this@ComponentActivityEditor.close_drawer() }

                        this@ComponentActivityEditor.view_model.create_dialog { close ->
                            val project_name = opus_manager.project_name ?: "Project"
                            @Composable {
                                DialogTitle(stringResource(R.string.dlg_delete_title, project_name))
                                DialogBar(
                                    neutral = close,
                                    positive = {
                                        close()
                                        controller_model.active_project?.let { project ->
                                            this@ComponentActivityEditor.delete_project(project)
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
                DrawerPadder()
                if (!this@ComponentActivityEditor.state_model.export_in_progress.value) {
                    ConfigDrawerBottomButton(
                        modifier = Modifier.weight(1F),
                        icon = R.drawable.icon_export,
                        description = R.string.btn_cfg_export,
                        onClick = {
                            this@ComponentActivityEditor.export()
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1F)
                            .height(Dimensions.ConfigBottomButtonHeight)
                            .combinedClickable(
                                onClick = {
                                    this@ComponentActivityEditor.runOnUiThread {
                                        Toast.makeText(
                                            this@ComponentActivityEditor,
                                            getString(R.string.hold_to_cancel_export),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onLongClick = {
                                    this@ComponentActivityEditor.export_wav_cancel()
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { this@ComponentActivityEditor.state_model.export_progress.value },
                            color = ProgressIndicatorDefaults.linearColor,
                            trackColor = ProgressIndicatorDefaults.linearTrackColor,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                    }
                }
            }
        }
    }


    @Composable
    override fun LayoutXLargePortrait(modifier: Modifier) = LayoutLargePortrait(modifier)

    @Composable
    override fun LayoutXLargeLandscape(modifier: Modifier) = LayoutLargePortrait(modifier)

    @Composable
    override fun LayoutLargePortrait(modifier: Modifier) {
        val ui_facade = this.controller_model.opus_manager.vm_state
        if (!ui_facade.ready.value) {
            LoadingSpinnerPlaceHolder()
            return
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.fillMaxSize()) {
                MainTable(Modifier, ui_facade, action_interface,  ui_facade.beat_count, LayoutSize.LargePortrait)
            }

            val primary = this@ComponentActivityEditor.get_context_menu_primary(
                Modifier,
                ui_facade,
                action_interface,
                LayoutSize.LargePortrait
            )
            val secondary = this@ComponentActivityEditor.get_context_menu_secondary(
                Modifier,
                ui_facade,
                action_interface,
                LayoutSize.LargePortrait
            )

            AnimatedVisibility(primary != null || secondary != null) {
                CMBoxBottom(
                    Modifier
                        .update_bottom_padding()
                        .width(Dimensions.Layout.Large.short)
                ) {
                    primary?.invoke()
                    if (primary != null && secondary != null) {
                        CMPadding()
                    }
                    secondary?.invoke()
                }
            }

            LaunchedEffect(primary == null && secondary == null) {
                if (primary == null && secondary == null) {
                    this@ComponentActivityEditor.state_model.table_bottom_padding.value = 0F
                }
            }
            LaunchedEffect(Unit) {
                this@ComponentActivityEditor.state_model.table_side_padding.value = 0F
            }
        }
    }

    @Composable
    override fun LayoutLargeLandscape(modifier: Modifier) = LayoutLargePortrait(modifier)

    @Composable
    override fun LayoutMediumPortrait(modifier: Modifier) = LayoutSmallPortrait(modifier)

    @Composable
    override fun LayoutMediumLandscape(modifier: Modifier) = LayoutSmallLandscape(modifier)

    @Composable
    override fun LayoutSmallPortrait(modifier: Modifier) {
        val ui_facade = this.controller_model.opus_manager.vm_state
        if (!ui_facade.ready.value) {
            LoadingSpinnerPlaceHolder()
            return
        }

        val layout = this.view_model.get_layout_size()
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.fillMaxSize()) {
                MainTable(Modifier, ui_facade, action_interface,  ui_facade.beat_count, layout)
            }

            val primary = this@ComponentActivityEditor.get_context_menu_primary(
                Modifier,
                ui_facade,
                action_interface,
                layout
            )

            val secondary = this@ComponentActivityEditor.get_context_menu_secondary(
                Modifier,
                ui_facade,
                action_interface,
                layout
            )

            AnimatedVisibility(primary != null || secondary != null) {
                CMBoxBottom(Modifier.update_bottom_padding()) {
                    primary?.invoke()
                    if (primary != null && secondary != null) {
                        CMPadding()
                    }
                    secondary?.invoke()
                }
            }

            LaunchedEffect(primary == null && secondary == null) {
                if (primary == null && secondary == null) {
                    this@ComponentActivityEditor.state_model.table_bottom_padding.value = 0F
                }
            }
            LaunchedEffect(Unit) {
                this@ComponentActivityEditor.state_model.table_side_padding.value = 0F
            }
        }
    }

    @Composable
    override fun LayoutSmallLandscape(modifier: Modifier) {
        val ui_facade = this.controller_model.opus_manager.vm_state

        if (!ui_facade.ready.value) {
            LoadingSpinnerPlaceHolder()
            return
        }

        val layout = this.view_model.get_layout_size()
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            MainTable(Modifier.fillMaxSize(), ui_facade, action_interface, ui_facade.beat_count, layout)
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                AnimatedVisibility(ui_facade.active_cursor.value != null, Modifier.weight(1F)) {
                    this@ComponentActivityEditor.get_context_menu_secondary(
                        Modifier,
                        ui_facade,
                        action_interface,
                        layout
                    )?.let {
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .weight(1F),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            CMBoxBottom(
                                Modifier
                                    .update_bottom_padding()
                                    .then(
                                        if (layout != LayoutSize.SmallLandscape) {
                                            Modifier.width(Dimensions.Layout.Medium.long)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                content = { it() }
                            )
                        }
                    } ?: Spacer(Modifier.weight(1F))
                }

                AnimatedVisibility(!keyboardAsState().value && ui_facade.active_cursor.value != null) {
                    this@ComponentActivityEditor.get_context_menu_primary(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxHeight(),
                        ui_facade,
                        action_interface,
                        layout
                    )?.let {
                        Row(
                            Modifier
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            content = {
                                CMBoxEnd(
                                    Modifier.update_side_padding()
                                ) { it() }
                            }
                        )
                    }
                }
                LaunchedEffect(keyboardAsState().value || ui_facade.active_cursor.value == null) {
                    if (ui_facade.active_cursor.value == null) {
                        this@ComponentActivityEditor.state_model.table_side_padding.value = 0F
                    }
                }
                LaunchedEffect(ui_facade.active_cursor.value == null) {
                    if (ui_facade.active_cursor.value == null) {
                        this@ComponentActivityEditor.state_model.table_bottom_padding.value = 0F
                    }
                }
            }
        }
    }

    @Composable
    fun Modifier.update_bottom_padding(): Modifier {
        return this then Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                this@ComponentActivityEditor.state_model.table_bottom_padding.value = placeable.height.toFloat()
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
    }

    @Composable
    fun Modifier.update_side_padding(): Modifier {
        return this then Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                this@ComponentActivityEditor.state_model.table_side_padding.value = placeable.width.toFloat()
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
    }

    @Composable
    fun LoadingSpinnerPlaceHolder() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = { CircularProgressIndicator( ) }
        )
    }

    private fun get_default_preset_name(bank: Int, program: Int): String {
        return if (this.controller_model.active_midi_device != null || this.controller_model.audio_interface.soundfont == null) {
            if (bank == 128) {
                this.resources.getString(R.string.gm_kit)
            } else {
                val preset_names = this.resources.getStringArray(R.array.general_midi_presets)
                preset_names[program]
            }
        } else if (bank == 128) {
            this.resources.getString(R.string.unavailable_kit)
        } else {
            val preset_names = this.resources.getStringArray(R.array.general_midi_presets)
            this.resources.getString(R.string.unavailable_preset, preset_names[program])
        }
    }

    fun toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this.baseContext, id, length).show()
    }

    fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this.baseContext, msg, length).show()
    }

    fun open_settings() {
        this.result_launcher_settings.launch(
            Intent(this, ComponentActivitySettings::class.java)
        )
    }

    fun open_about() {
        this.startActivity(Intent(this, ComponentActivityAbout::class.java))
    }

    fun load_project(uri: Uri) {
        // Stop Playback First
        this.action_interface.stop_opus()
        this.action_interface.stop_opus_midi()

        this.controller_model.opus_manager.vm_state.ready.value = false
        val input_stream = this@ComponentActivityEditor.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText().toByteArray(Charsets.UTF_8)

        reader.close()
        input_stream?.close()

        this@ComponentActivityEditor.controller_model.opus_manager.load(content) { json_data ->
            this@ComponentActivityEditor.controller_model.active_project = uri
            this@ComponentActivityEditor.controller_model.project_exists.value = true
            if (this@ComponentActivityEditor.view_model.configuration.use_preferred_soundfont.value) {
                json_data.get_hashmap("d").get_stringn("sf")?.let {
                    val original = this@ComponentActivityEditor.view_model.configuration.soundfont.value
                    if (it != original) {
                        this@ComponentActivityEditor.view_model.configuration.soundfont.value = it
                        // Try opening the assigned soundfont, but if it fails for any reason, go back to the
                        // Currently active one.
                        try {
                            this.set_soundfont()
                        } catch (_: Exception) {
                            this@ComponentActivityEditor.view_model.configuration.soundfont.value = original
                            this@ComponentActivityEditor.set_soundfont()
                        }
                        this@ComponentActivityEditor.view_model.save_configuration()
                    }
                }
            }
        }
    }

    private fun get_export_name(): String? {
        return this.controller_model.opus_manager.get_safe_name() ?: this.get_default_export_name()
    }

    private fun get_default_export_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return this.resources.getString(R.string.default_export_name, now.format(formatter))
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

    override fun on_crash() {
        // if (this.is_debug_on()) {
        //     this.save_actions()
        // }
        this.state_model.clear()
        this.save_to_backup()
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
        this.controller_model.cancel_export()
    }

    fun export_midi_check() {
      val opus_manager = this.controller_model.opus_manager
      if (opus_manager.get_percussion_channels().size > 1) {
          this.view_model.create_dialog { close ->
              @Composable {
                  DialogSTitle(R.string.generic_warning)
                  SText(R.string.multiple_kit_warning)
                  DialogBar(
                      neutral = close,
                      positive = {
                          close()
                          this@ComponentActivityEditor.export_midi()
                      }
                  )
              }
          }
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

    fun has_notification_permission(): Boolean {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED )
    }

    private fun getNotificationPermission(): Boolean {
        if (! this.has_notification_permission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        return this.has_notification_permission()
    }

    fun get_notification(): NotificationCompat.Builder? {
        if (!this.has_notification_permission()) return null

        if (this.active_notification == null) {
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

            val opus_manager = this.controller_model.opus_manager
            val builder = NotificationCompat.Builder(this, this.CHANNEL_ID)
                .setContentTitle(this.getString(R.string.export_wav_notification_title, opus_manager.project_name ?: "Untitled Project"))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.small_logo_rowan)
                .setSilent(true)
                .addAction(R.drawable.baseline_cancel_24, this.getString(android.R.string.cancel), pending_cancel_intent)

            this.active_notification = builder
        }

        return this.active_notification!!
    }

    fun get_notification_channel(): NotificationChannel? {
        if (!this.has_notification_permission()) return null

        if (this._notification_channel == null) {
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
            this._notification_channel = mChannel
        }

        return this._notification_channel
    }

    fun export(type: Exportable? = null) {
        type?.let { it
            when (it) {
                Exportable.JSON -> { this.export_project() }
                Exportable.MIDI1 -> { this.export_midi_check() }
                Exportable.WAV_SINGLE -> { this.export_wav() }
                Exportable.WAV_LINES -> { this.export_multi_lines_wav() }
                Exportable.WAV_CHANNELS -> { this.export_multi_channels_wav() }
            }
            return
        }

        this.view_model.unsortable_list_dialog(R.string.dlg_export, this.get_exportable_options()) { export_type ->
            this.export(export_type)
        }
    }

    private fun get_exportable_options(): List<Pair<Exportable, @Composable RowScope.() -> Unit>> {
        val export_options = mutableListOf<Pair<Exportable, @Composable RowScope.() -> Unit>>()
        val opus_manager = this.controller_model.opus_manager

        export_options.add(
            Pair(
                Exportable.JSON,
                @Composable { SText(R.string.export_option_json) }
            )
        )

        if (opus_manager.is_tuning_standard()) {
            export_options.add(
                Pair(
                    Exportable.MIDI1,
                    @Composable { SText(R.string.export_option_midi) }
                )
            )
        }

        this.controller_model.audio_interface.soundfont?.let {
            export_options.add(
                Pair(
                    Exportable.WAV_SINGLE,
                    @Composable { SText(R.string.export_option_wav) }
                )
            )
            export_options.add(
                Pair(
                    Exportable.WAV_LINES,
                    @Composable { SText(R.string.export_option_wav_lines) }
                )
            )
            export_options.add(
                Pair(
                    Exportable.WAV_CHANNELS,
                    @Composable { SText(R.string.export_option_wav_channels) }
                )
            )
        }

        return export_options
    }

    override fun onSaveInstanceState(outState: Bundle) {
        this.save_to_backup()
        super.onSaveInstanceState(outState)
    }

    fun save_to_backup() {
        this.view_model.project_manager?.save_to_backup(
            this.controller_model.opus_manager,
            this.controller_model.active_project
        )
    }

    fun get_dragged_offset(y: Int, target_line: Int?, is_after: Boolean, is_spacer: Boolean = false): Int {
        if (this.state_model.dragging_line.value == null) return 0

        val line_height = this.resources.getDimension(R.dimen.line_height)
        val ctl_line_height = this.resources.getDimension(R.dimen.ctl_line_height)
        val is_dragging_channel = this.state_model.is_dragging_channel()

        if ((is_dragging_channel || !is_spacer) && this.state_model.line_data[y].is_dragging.value) return this.state_model.dragging_offset.value.roundToInt()
        if (target_line == null || this.state_model.line_data[y].channel.value == null) return 0

        val std_line_count = this.state_model.dragging_height.first.value
        val ctl_line_count = this.state_model.dragging_height.second.value

        val first_line = this.state_model.dragging_first_line.value!!
        val check_line = first_line + std_line_count + ctl_line_count

        val gap_size = if (is_dragging_channel) {
            (this.resources.getDimension(R.dimen.channel_gap_size) + (std_line_count * line_height) + (ctl_line_count * ctl_line_height)).toInt()
        } else {
            ((std_line_count * line_height) + (ctl_line_count * ctl_line_height)).toInt()
        }

        // Spacers get treated like lines when dragging channels
        return if (!is_dragging_channel && is_spacer) {
            when (y) {
                in target_line until first_line -> { gap_size }
                in first_line until target_line -> { 0 - gap_size }
                else -> { 0 }
            }
        } else {
            if ((first_line until check_line).contains(target_line)) {
                0
            } else if (y >= check_line) {
                if (target_line > y || (y == target_line && is_after)) {
                    0 - gap_size
                } else {
                    0
                }
            } else if ((y == target_line && !is_after) || (y > target_line)) {
                gap_size
            } else {
                0
            }
        }
    }

    fun get_channel_gap_dragged_offset(y: Int, target_line: Int?, is_after: Boolean): Int {
        return this.get_dragged_offset(y, target_line, is_after, true)
    }

    fun build_dragging_line_map(): List<Triple<ClosedFloatingPointRange<Float>, IntRange, Boolean>> {
        val line_height = this.resources.getDimension(R.dimen.line_height).roundToInt()
        val ctl_line_height = this.resources.getDimension(R.dimen.ctl_line_height).roundToInt()
        val gap_height = this.resources.getDimension(R.dimen.channel_gap_size).roundToInt()
        val is_dragging_channel = this.state_model.is_dragging_channel()

        val output = mutableListOf<Triple<ClosedFloatingPointRange<Float>, IntRange, Boolean>>()
        var start_line: Int = 0
        var end_line: Int = 0
        var start_position = 0F
        var running_position = 0f
        var working_channel: Int = 0
        var working_line_offset: Int = 0
        for (y in 0 until this.state_model.line_count.value) {
            val working_line = this.state_model.line_data[y]

            val current_delta = if (working_line.ctl_type.value == null) {
                line_height
            } else {
                ctl_line_height
            }

            if (working_line.channel.value != working_channel || (!is_dragging_channel && working_line_offset != working_line.line_offset.value)) {
                val diff = (running_position - start_position) / 2F
                var working_end_line = end_line
                if (!is_dragging_channel) {
                    while (working_end_line > 0 && this.state_model.line_data[working_end_line].line_offset.value == null) {
                        working_end_line -= 1
                    }
                }
                while (working_end_line > 0 && this.state_model.line_data[working_end_line].channel.value == null) {
                    working_end_line -= 1
                }

                output.add(Triple(start_position .. (running_position - diff), start_line .. working_end_line, false))
                output.add(Triple((start_position + diff) .. running_position, start_line .. working_end_line, true))

                start_position = running_position
                start_line = y
                running_position += gap_height
            }

            end_line = y
            running_position += current_delta
            working_line.line_offset.value?.let { working_line_offset = it }
            working_channel = working_line.channel.value ?: break
        }


        if (this.state_model.line_data[start_line].channel.value != null) {
            val diff = (running_position - start_position) / 2F
            if (!is_dragging_channel) {
                while (end_line > 0 && this.state_model.line_data[end_line].line_offset.value == null) {
                    end_line -= 1
                }
            }
            while (end_line > 0 && this.state_model.line_data[end_line].channel.value == null) {
                end_line -= 1
            }
            output.add(Triple(start_position..(running_position - diff), start_line..end_line, false))
            output.add(Triple((start_position + diff)..running_position, start_line..end_line, true))
        }

        return output
    }

    override fun on_delete_project(uri: Uri) {
        val is_current = uri == this.controller_model.active_project
        super.on_delete_project(uri)
        if (is_current) {
            this.action_interface.new_project()
        }
    }
}
