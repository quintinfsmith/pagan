package com.qfs.pagan.ComponentActivity

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.soundfont2.Riff
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.Activity.PaganActivity.Companion.EXTRA_ACTIVE_PROJECT
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.Exportable
import com.qfs.pagan.MultiExporterEventHandler
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.R
import com.qfs.pagan.SingleExporterEventHandler
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogCard
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.DropdownMenuItem
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ConfigDrawerBottomButton
import com.qfs.pagan.composable.button.ConfigDrawerChannelLeftButton
import com.qfs.pagan.composable.button.ConfigDrawerChannelRightButton
import com.qfs.pagan.composable.button.ConfigDrawerTopButton
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.TopBarIcon
import com.qfs.pagan.composable.cxtmenu.CMBoxBottom
import com.qfs.pagan.composable.cxtmenu.ContextMenuChannelPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuChannelSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuColumnPrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuColumnSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLinePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuLineSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuRangePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuRangeSecondary
import com.qfs.pagan.composable.cxtmenu.ContextMenuSinglePrimary
import com.qfs.pagan.composable.cxtmenu.ContextMenuSingleSecondary
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
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

class ComponentActivityEditor: PaganComponentActivity() {
    val controller_model: ViewModelEditorController by this.viewModels()
    val state_model: ViewModelEditorState by this.viewModels()

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
                        // FIXME: Move magic numbers
                        val exporter_sample_handle_manager = SampleHandleManager(soundfont, 44100, 22050)

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
                val exporter_sample_handle_manager = SampleHandleManager(soundfont, 44100, 22050)

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

                if (export_event_handler.cancelled) {
                    break@outer
                }
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

            this.view_model.configuration.project_directory = tree_uri
            this.view_model.save_configuration()

            // No need to update the active_project here. using this intent launcher implies the active_project will be changed in the ucheck
            this.view_model.project_manager?.change_project_path(tree_uri, this.controller_model.active_project)

            TODO()
        }

    internal var result_launcher_settings =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            result.data?.getStringExtra(EXTRA_ACTIVE_PROJECT)?.toUri()?.let { uri ->
                this.controller_model.active_project = uri
            }
            this.reload_config()
        }

    internal var result_launcher_import = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            result.data?.data?.also { uri ->
                this.handle_uri(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val action_interface = this.controller_model.action_interface
        this.state_model.base_leaf_width.value = this.resources.getDimension(R.dimen.base_leaf_width)
        this.controller_model.attach_state_model(this.state_model)
        action_interface.attach_top_model(this.view_model)

        super.onCreate(savedInstanceState)

        action_interface.new_project()

        this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val that = this@ComponentActivityEditor
                val opus_manager = that.controller_model.opus_manager
                val ui_facade = opus_manager.vm_state
                if (this@ComponentActivityEditor.drawer_state.isOpen) {
                    this@ComponentActivityEditor.lifecycleScope.launch {
                        this@ComponentActivityEditor.close_drawer()
                    }
                } else if (ui_facade.active_cursor.value != null) {
                    action_interface.cursor_clear()
                } else {
                    action_interface.save_before {
                        action_interface.save()
                        that.finish()
                    }
                }
            }
        })

        if (savedInstanceState != null) {
            // if the activity is forgotten, the opus_manager is be uninitialized
            if (this.controller_model.opus_manager.is_initialized()) {
                //this.refresh(
                //    savedInstanceState.getInt("x"),
                //    savedInstanceState.getInt("y")
                //)
            } else {
                // this.load_from_bkp()
            }
        } else if (this.intent.getBooleanExtra("load_backup", false)) {
            // this.load_from_bkp()
        } else if (this.intent.data == null) {
           // this.setup_new()
        } else if (this.view_model.project_manager?.contains(this.intent.data!!) == true) {
            this.load_project(this.intent.data!!)
        } else {
            this.handle_uri(this.intent.data!!)
        }
    }

    override fun on_config_load() {
        super.on_config_load()
        this.set_soundfont()
    }

    fun set_soundfont() {
        val file_path = this.view_model.configuration.soundfont
        if (file_path == null) {
            this.controller_model.unset_soundfont()
            return
        }

        // Failed to change playback_state
        if (!this.controller_model.update_playback_state_soundfont(PlaybackState.Ready)) return

        val soundfont_directory = this.get_soundfont_directory()
        var soundfont_file = soundfont_directory
        for (segment in file_path.split("/")) {
            soundfont_file = soundfont_file.findFile(segment) ?: throw FileNotFoundException()
        }

        if (!soundfont_file.exists()) {
            // Possible if user puts the sf2 in their files manually
            //this.feedback_msg(this.getString(R.string.soundfont_not_found))
            throw FileNotFoundException()
        }

        try {
            this.controller_model.set_soundfont(SoundFont(this, soundfont_file.uri))
            this.controller_model.playback_device?.activity = this
        } catch (_: Riff.InvalidRiff) {
            // Possible if user puts the sf2 in their files manually
            //this.feedback_msg(this.getString(R.string.invalid_soundfont))
            return
        } catch (_: SoundFont.InvalidSoundFont) {
            // Possible if user puts the sf2 in their files manually
            // Possible if user puts the sf2 in their files manually
            //this.feedback_msg("Invalid Soundfont")
            return
        }

        // TODO: Update percussion minimums
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

    fun import_project(uri: Uri) {
        this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.controller_model.opus_manager.load(bytes)
            this.controller_model.active_project = null
            this.controller_model.project_exists.value = false
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

        this.controller_model.action_interface.save_before {
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
                TODO()
                // if (!this.get_opus_manager().is_initialized()) {
                //     this.setup_new()
                // }
                // this.feedback_msg(fallback_msg)
            }
        }
    }

    override fun onDestroy() {
        this.controller_model.playback_device?.activity = null
        super.onDestroy()
    }

    @Composable
    override fun TopBar(modifier: Modifier) {
        val ui_facade = this.controller_model.opus_manager.vm_state
        val dispatcher = this.controller_model.action_interface
        val scope = rememberCoroutineScope()
        val menu_items: MutableList<Pair<Int, () -> Unit>> = mutableListOf(
            Pair(R.string.menu_item_new_project) {
                dispatcher.save_before {
                    dispatcher.new_project()
                }
            }
        )
        if (this@ComponentActivityEditor.view_model.has_saved_project.value) {
            menu_items.add(
                Pair(R.string.menu_item_load_project) {
                    this@ComponentActivityEditor.load_menu_dialog { uri ->
                        this@ComponentActivityEditor.load_project(uri)
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
        menu_items.add(
            Pair(R.string.menu_item_settings) { this.open_settings() }
        )
        menu_items.add(
            Pair(R.string.menu_item_about) { this.open_about() }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarIcon(
                icon = R.drawable.icon_hamburger_32,
                description = R.string.song_configuration,
                callback = {
                    scope.launch {
                        this@ComponentActivityEditor.open_drawer()
                    }
                }
            )
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = { dispatcher.set_project_name_and_notes() }
                    ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                text = ui_facade.project_name.value ?: stringResource(R.string.untitled_opus)
            )
            TopBarIcon(
                icon = when (this@ComponentActivityEditor.state_model.playback_state_soundfont.value) {
                    PlaybackState.Queued,
                    PlaybackState.NotReady -> R.drawable.baseline_play_disabled_24
                    PlaybackState.Ready -> R.drawable.icon_play
                    PlaybackState.Stopping,
                    PlaybackState.Playing -> R.drawable.icon_pause
                },
                description = R.string.menu_item_playpause,
                callback = {
                    scope.launch {
                        when (this@ComponentActivityEditor.controller_model.playback_state_soundfont) {
                            PlaybackState.Queued -> TODO()
                            PlaybackState.Stopping -> TODO()
                            PlaybackState.NotReady -> TODO()
                            PlaybackState.Ready -> { dispatcher.play_opus(this) }
                            PlaybackState.Playing -> { dispatcher.stop_opus() }
                        }
                    }
                }
            )
            TopBarIcon(
                icon = R.drawable.icon_undo,
                description = R.string.menu_item_undo,
                callback = { dispatcher.apply_undo() }
            )
            Box {
                val expanded = remember { mutableStateOf(false) }
                TopBarIcon(
                    icon = R.drawable.kebab,
                    description = R.string.menu_item_playpause,
                    callback = { expanded.value = !expanded.value }
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
    }


    @Composable
    fun ContextMenuPrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
        val cursor = ui_facade.active_cursor.value
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLinePrimary(ui_facade, dispatcher)
            CursorMode.Column -> ContextMenuColumnPrimary(ui_facade, dispatcher)
            CursorMode.Single -> {
                val show_relative_input = this@ComponentActivityEditor.view_model.configuration.relative_mode
                ContextMenuSinglePrimary(ui_facade, dispatcher, show_relative_input)
            }
            CursorMode.Range -> ContextMenuRangePrimary(ui_facade, dispatcher)
            CursorMode.Channel -> ContextMenuChannelPrimary(ui_facade, dispatcher)
            CursorMode.Unset,
            null -> Text("TODO")
        }
    }
    @Composable
    fun ContextMenuSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
        val cursor = ui_facade.active_cursor.value ?: return
        if (cursor.type == CursorMode.Unset) return

        val modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_secondary_height))
        Row {
            when (cursor.type) {
                CursorMode.Line -> ContextMenuLineSecondary(ui_facade, dispatcher, modifier)
                CursorMode.Column -> ContextMenuColumnSecondary(ui_facade, dispatcher, modifier)
                CursorMode.Single -> ContextMenuSingleSecondary(ui_facade, dispatcher, modifier)
                CursorMode.Range -> {
                    ContextMenuRangeSecondary(
                        ui_facade,
                        dispatcher,
                        this@ComponentActivityEditor.controller_model.move_mode.value
                    )
                }

                CursorMode.Channel -> ContextMenuChannelSecondary(ui_facade, dispatcher, modifier)

                CursorMode.Unset -> TODO("This shouldn't be reachable")
            }
        }
    }

    @Composable
    fun MainTable(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, length: MutableState<Int>) {
        val window_height =  LocalConfiguration.current.screenHeightDp.dp
        val line_height = dimensionResource(R.dimen.line_height)
        val ctl_line_height = dimensionResource(R.dimen.ctl_line_height)
        val leaf_width = dimensionResource(R.dimen.base_leaf_width)
        val column_widths = Array(ui_facade.beat_count.value) { i ->
            Array(ui_facade.cell_map.size) { j -> ui_facade.cell_map[j][i].value.weighted_size }.max()
        }

        val scope = rememberCoroutineScope()
        val scroll_state_v = ui_facade.scroll_state_y.value
        val scroll_state_h = ui_facade.scroll_state_x.value

        Row(Modifier.fillMaxWidth()) {
            Column {
                ShortcutView(dispatcher, scope, scroll_state_h)
                Column(Modifier.verticalScroll(scroll_state_v, overscrollEffect = null)) {
                    var working_channel: Int? = 0
                    for (y in 0 until ui_facade.line_count.value) {
                        if (ui_facade.line_data[y].channel.value != working_channel) {
                            Row(Modifier.height(dimensionResource(R.dimen.channel_gap_size))) { }
                        }

                        working_channel = ui_facade.line_data[y].channel.value
                        val use_height = if (ui_facade.line_data[y].ctl_type.value != null) {
                            ctl_line_height
                        } else {
                            line_height
                        }

                        Row(
                            Modifier
                                .height(use_height)
                                .width(dimensionResource(R.dimen.line_label_width))
                        ) {
                            LineLabelView(modifier = Modifier.fillMaxSize(), dispatcher, ui_facade.line_data[y])
                        }
                    }
                    Row(
                        Modifier
                            .height(line_height)
                            .combinedClickable(
                                onClick = { dispatcher.show_hidden_global_controller() }
                            )
                    ) {
                        Icon(
                            modifier = Modifier
                                .background(color = Color.Transparent, CircleShape)
                                .padding(4.dp),
                            painter = painterResource(R.drawable.icon_ctl),
                            contentDescription = stringResource(R.string.cd_show_effect_controls)
                        )
                    }
                    Spacer(Modifier.height(window_height / 2))
                }
            }
            LazyRow(
                state = scroll_state_h,
                overscrollEffect = null
            ) {
                itemsIndexed(column_widths + listOf(1)) { x, width ->
                    if (x == column_widths.size) {
                        Icon(
                            modifier = Modifier
                                .width(dimensionResource(R.dimen.base_leaf_width))
                                .combinedClickable(
                                    onClick = { dispatcher.append_beats(1) },
                                    onLongClick = { dispatcher.append_beats() }
                                ),
                            painter = painterResource(R.drawable.icon_add_channel),
                            contentDescription = stringResource(R.string.cd_insert_beat)
                        )

                        return@itemsIndexed
                    }

                    Column {
                        BeatLabelView(
                            modifier = Modifier
                                .width(leaf_width * width)
                                .height(line_height),
                            x = x,
                            ui_facade = ui_facade,
                            dispatcher = dispatcher,
                            column_info = ui_facade.column_data[x]
                        )
                        Column(
                            Modifier
                                .verticalScroll(scroll_state_v, overscrollEffect = null)
                                .width(leaf_width * column_widths[x])
                        ) {
                            var working_channel: Int? = 0
                            for (y in 0 until ui_facade.line_count.value) {
                                if (ui_facade.line_data[y].channel.value != working_channel) {
                                    Row(Modifier.height(dimensionResource(R.dimen.channel_gap_size))) { }
                                }
                                working_channel = ui_facade.line_data[y].channel.value

                                val cell = ui_facade.cell_map[y][x]
                                Row(
                                    Modifier
                                        .height(
                                            if (ui_facade.line_data[y].ctl_type.value != null) ctl_line_height
                                            else line_height
                                        )
                                ) {
                                    CellView(ui_facade, dispatcher, cell, y, x)
                                }
                            }
                            Spacer(Modifier.height(window_height / 2))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ShortcutView(dispatcher: ActionTracker, scope: CoroutineScope, scroll_state: LazyListState) {
        Box(
            Modifier
                .background(colorResource(R.color.line_label), shape = RectangleShape)
                .width(dimensionResource(R.dimen.line_label_width))
                .height(dimensionResource(R.dimen.line_height))
                .combinedClickable(
                    onClick = { dispatcher.cursor_select_column() },
                    onLongClick = {
                        dispatcher.cursor_select_column(0)
                        scope.launch { scroll_state.scrollToItem(0) }
                    }
                ),
            contentAlignment = Alignment.Center,
            content = {
                Icon(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxSize(),
                    painter = painterResource(R.drawable.icon_shortcut),
                    contentDescription = stringResource(R.string.jump_to_section)
                )
            }
        )
    }

    @Composable
    fun LineLabelView(modifier: Modifier = Modifier, dispatcher: ActionTracker, line_info: ViewModelEditorState.LineData) {
        val background_color = R.color.line_label
        val ctl_type = line_info.ctl_type.value

        ProvideContentColorTextStyle(colorResource(R.color.line_label_text)) {
            Box(
                Modifier
                    .padding(
                        horizontal = 0.dp,
                        vertical = 1.dp
                    )
                    .combinedClickable(
                        onClick = {
                            dispatcher.cursor_select_line(
                                line_info.channel.value,
                                line_info.line_offset.value,
                                line_info.ctl_type.value
                            )
                        },
                        onLongClick = {}
                    )
                    .background(
                        shape = RectangleShape,
                        color = colorResource(background_color),
                    )
                    .fillMaxSize(),
                content = {
                    if (ctl_type == null) {
                        val (label_a, label_b) = if (line_info.assigned_offset.value != null) {
                            Pair("!${line_info.channel.value}", "${line_info.assigned_offset.value}")
                        } else {
                            Pair("${line_info.channel.value}", "${line_info.line_offset.value}")
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (line_info.is_selected.value) {
                                        Modifier.border(2.dp, colorResource(R.color.selected_primary))
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 2.dp)
                        ) {
                            Box(modifier = Modifier.align(Alignment.TopStart)) {
                                Text(label_a, maxLines = 1)
                            }
                            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
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
                        Icon(
                            modifier = modifier
                                .then(
                                    if (line_info.is_selected.value) {
                                        Modifier.border(2.dp, colorResource(R.color.selected_primary))
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(2.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (line_info.line_offset.value != null) {
                                            dispatcher.cursor_select_line_ctl_line(
                                                ctl_type,
                                                line_info.channel.value!!,
                                                line_info.line_offset.value!!
                                            )
                                        } else if (line_info.channel.value != null) {
                                            dispatcher.cursor_select_channel_ctl_line(
                                                ctl_type,
                                                line_info.channel.value!!
                                            )
                                        } else {
                                            dispatcher.cursor_select_global_ctl_line(ctl_type)
                                        }
                                    }
                                ),
                            painter = painterResource(drawable_id),
                            contentDescription = stringResource(description_id)
                        )
                    }
                }
            )
        }
    }

    @Composable
    fun BeatLabelView(modifier: Modifier = Modifier, x: Int, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, column_info: ViewModelEditorState.ColumnData) {
        if (!column_info.is_tagged.value) {
            modifier.border(width = 1.dp, color = Color.Red)
        }

        ProvideContentColorTextStyle(colorResource(R.color.line_label_text)) {
            Box(
                modifier = modifier
                    .padding(top = 0.dp, end = 1.dp, bottom = 0.dp, start = 1.dp)
                    .then(
                        if (column_info.is_selected.value) {
                            Modifier.border(2.dp, colorResource(R.color.selected_primary))
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        color = colorResource(R.color.line_label),
                        shape = RectangleShape
                    )
                    .combinedClickable(
                        onClick = { dispatcher.cursor_select_column(x) },
                    )
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = {
                    if (column_info.is_tagged.value) {
                        Text(text = "$x", modifier = Modifier.border(2.dp, Color.Red, CircleShape))
                    } else {
                        Text(text = "$x")
                    }
                }
            )
        }
    }

    private fun mix_colors(first: Long, second: Long, numer_a: Int, numer_b: Int): Long {
        val denominator = numer_a + numer_b
        val alpha = (((first and 0xFF000000) shr 24) * numer_a / denominator) + (((second and 0xFF000000) shr 24) * numer_b / denominator)
        val red = (((first and 0xFF0000) shr 16) * numer_a / denominator) + (((second and 0xFF0000) shr 16) * numer_b / denominator)
        val green = (((first and 0xFF00) shr 8) * numer_a / denominator) + (((second and 0xFF00) shr 8) * numer_b / denominator)
        val blue = ((first and 0xFF) * numer_a / denominator) + ((second and 0xFF) * numer_b / denominator)
        return (alpha shl 24) + (red shl 16) + (green shl 8) + blue
    }

    @Composable
    fun <T: OpusEvent> LeafView(line_data: ViewModelEditorState.LineData, leaf_data: ViewModelEditorState.LeafData, event: T?, radix: Int, modifier: Modifier = Modifier) {
        val corner_radius = when (event) {
            is InstrumentEvent -> 12.dp
            else -> 4.dp
        }

        val channel_colors = this.view_model.configuration.channel_colors

        val base_color = line_data.channel.value?.let {
            channel_colors[it % channel_colors.size]
        } ?: 0xFF000090L

        // alternate slight shading
        val adjusted_base_color = line_data.line_offset.value?.let {
            if (it % 2 == 0) {
                base_color
            } else {
                this.mix_colors(0xFFFFFFFF, base_color, 3, 7)
            }
        } ?: base_color

        val spill_color = this.mix_colors(0xFF000000, adjusted_base_color, 1, 9)
        val empty_color = this.mix_colors(adjusted_base_color, 0x11888888, 1, 1)
        val leaf_color = if (leaf_data.is_spillover.value) {
            spill_color
        } else {
            when (event) {
                is EffectEvent,
                is InstrumentEvent -> adjusted_base_color
                else -> empty_color
            }
        }

        val avg = (((adjusted_base_color / (256 * 256)) and 0xFF) + ((adjusted_base_color / 256) and 0xFF) + (adjusted_base_color and 0xFF)) / 3

        val text_color = if (avg > 0x88) {
            Color(0xFF000000)
        } else {
            Color(0xFFFFFFFF)
        }

        Box(
            modifier = modifier
                .padding(1.dp)
                .background(
                    color = Color(leaf_color),
                    RoundedCornerShape(corner_radius)
                )
                .then(
                    if (leaf_data.is_selected.value) {
                        modifier.border(2.dp, colorResource(R.color.selected_primary), RoundedCornerShape(corner_radius))
                    } else if (leaf_data.is_secondary.value) {
                        modifier.border(
                            2.dp,
                            colorResource(R.color.selected_secondary),
                            RoundedCornerShape(corner_radius)
                        )
                    } else if (!leaf_data.is_valid.value) {
                        modifier.border(2.dp, colorResource(R.color.leaf_invalid), RoundedCornerShape(corner_radius))
                    } else {
                        modifier
                    }
                )
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            when (event) {
                is AbsoluteNoteEvent -> {
                    val octave = event.note / radix
                    val offset = event.note % radix
                    Row(horizontalArrangement = Arrangement.Center) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Spacer(modifier = Modifier.weight(.30F))
                            ProvideTextStyle(TextStyle(fontSize = 14.sp, color = text_color)) {
                                Text("$octave", modifier = Modifier.weight(.5F))
                            }
                            Spacer(modifier = Modifier.weight(.1F))
                        }
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            ProvideTextStyle(TextStyle(fontSize = 20.sp, color = text_color)) {
                                Text("$offset")
                            }
                        }
                    }
                }

                is RelativeNoteEvent -> {
                    val octave = abs(event.offset) / radix
                    val offset = abs(event.offset) % radix
                    Row(horizontalArrangement = Arrangement.Center) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.weight(.4F),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                ProvideTextStyle(TextStyle(fontSize = 14.sp)) {
                                    Text(
                                        if (event.offset > 0) "+" else "-",
                                        color = text_color
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.weight(.4F),
                                verticalAlignment = Alignment.Top
                            ) {
                                ProvideTextStyle(TextStyle(fontSize = 14.sp)) {
                                    Text("$octave", color = text_color)
                                }
                            }
                            Spacer(modifier = Modifier.weight(.1F))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ProvideTextStyle(TextStyle(fontSize = 20.sp)) {
                                Text("$offset", color = text_color)
                            }
                        }
                    }
                }
                is PercussionEvent -> SText(R.string.percussion_label, color = text_color)
                is OpusVolumeEvent -> Text("${event.value}", color = text_color)
                is OpusPanEvent -> {}
                is DelayEvent -> {}
                is OpusTempoEvent -> Text("${event.value} BPM", color = text_color)
                is OpusVelocityEvent -> {}
                null -> {}
            }
        }
    }

    private fun <T> get_leaf_list(tree: ReducibleTree<T>): List<Triple<List<Int>, T?,Float>> {
        val output = mutableListOf<Triple<List<Int>, T?, Float>>()
        tree.weighted_traverse { tree, event, path, weight ->
            if (tree.is_leaf()) {
                output.add(Triple(path, event, weight))
            }
        }
        return output
    }

    @Composable
    fun CellView(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, cell: MutableState<ReducibleTree<Pair<ViewModelEditorState.LeafData, OpusEvent?>>>, y: Int, x: Int, modifier: Modifier = Modifier) {
        val line_info = ui_facade.line_data[y]
        Row(modifier.fillMaxSize()) {
            for ((path, event, weight) in this@ComponentActivityEditor.get_leaf_list(cell.value)) {
                this@ComponentActivityEditor.LeafView(
                    line_info,
                    event!!.first,
                    event.second,
                    ui_facade.radix.value,
                    Modifier
                        .weight(weight)
                        .combinedClickable(
                            onClick = {
                                val cursor = ui_facade.active_cursor.value
                                val selecting_range = ui_facade.active_cursor.value?.type == CursorMode.Range
                                if (line_info.ctl_type.value == null) {
                                    if (selecting_range && ui_facade.line_data[cursor!!.ints[0]].ctl_type.value == null) {
                                        dispatcher.move_selection_to_beat(
                                            BeatKey(line_info.channel.value!!, line_info.line_offset.value!!, x)
                                        )
                                    } else {
                                        dispatcher.cursor_select(
                                            BeatKey(line_info.channel.value!!, line_info.line_offset.value!!, x), path
                                        )
                                    }
                                } else if (line_info.line_offset.value != null) {
                                    if (selecting_range && ui_facade.line_data[cursor!!.ints[0]].ctl_type.value == line_info.ctl_type.value) {
                                        dispatcher.move_line_ctl_to_beat(
                                            BeatKey(
                                                line_info.channel.value!!,
                                                line_info.line_offset.value!!,
                                                x
                                            )
                                        )
                                    } else {
                                        dispatcher.cursor_select_ctl_at_line(
                                            line_info.ctl_type.value!!,
                                            BeatKey(line_info.channel.value!!, line_info.line_offset.value!!, x),
                                            path
                                        )
                                    }
                                } else if (line_info.channel.value != null) {
                                    if (selecting_range && ui_facade.line_data[cursor!!.ints[0]].ctl_type.value == line_info.ctl_type.value) {
                                        dispatcher.move_channel_ctl_to_beat(line_info.channel.value!!, x)
                                    } else {
                                        dispatcher.cursor_select_ctl_at_channel(
                                            line_info.ctl_type.value!!,
                                            line_info.channel.value!!,
                                            x,
                                            path
                                        )
                                    }
                                } else {
                                    if (selecting_range && ui_facade.line_data[cursor!!.ints[0]].ctl_type.value == line_info.ctl_type.value) {
                                        dispatcher.move_global_ctl_to_beat(x)
                                    } else {
                                        dispatcher.cursor_select_ctl_at_global(line_info.ctl_type.value!!, x, path)
                                    }
                                }
                            },
                            onLongClick = {
                                if (line_info.ctl_type.value == null) {
                                    dispatcher.cursor_select_range_next(
                                        BeatKey(
                                            line_info.channel.value!!,
                                            line_info.line_offset.value!!,
                                            x
                                        )
                                    )
                                } else if (line_info.line_offset.value != null) {
                                    dispatcher.cursor_select_line_ctl_range_next(
                                        line_info.ctl_type.value!!,
                                        BeatKey(line_info.channel.value!!, line_info.line_offset.value!!, x)
                                    )
                                } else if (line_info.channel.value != null) {
                                    dispatcher.cursor_select_channel_ctl_range_next(
                                        line_info.ctl_type.value!!,
                                        line_info.channel.value!!,
                                        x
                                    )
                                } else {
                                    dispatcher.cursor_select_global_ctl_range_next(line_info.ctl_type.value!!, x)
                                }
                            }
                        )
                )
            }
        }
    }

    @Composable
    override fun LayoutXLargePortrait() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutLargePortrait() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutMediumPortrait() {
        val view_model = this.controller_model
        val ui_facade = this.controller_model.opus_manager.vm_state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.fillMaxSize()) {
                MainTable(ui_facade, view_model.action_interface,  ui_facade.beat_count)
            }
            if (ui_facade.active_cursor.value?.type != CursorMode.Unset) {
                AnimatedVisibility(ui_facade.active_cursor.value != null) {
                    CMBoxBottom {
                        ContextMenuPrimary(ui_facade, view_model.action_interface)
                        ContextMenuSecondary(ui_facade, view_model.action_interface)
                    }
                }
            }
        }
    }

    @Composable
    override fun Drawer(modifier: Modifier) {
        val dispatcher = this.controller_model.action_interface
        val state_model = this.state_model
        val scope = rememberCoroutineScope()

        DialogCard(
            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            modifier = modifier.wrapContentWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1F)) {
                        ConfigDrawerTopButton(
                            onClick = { dispatcher.set_tuning_table_and_transpose() },
                            content = { SText(R.string.label_tuning) }
                        )
                    }
                    Row {
                        ConfigDrawerTopButton(
                            onClick = { dispatcher.insert_percussion_channel() },
                            content = {
                                Icon(
                                    painter = painterResource(R.drawable.icon_add_channel_kit),
                                    contentDescription = stringResource(R.string.btn_cfg_add_kit_channel),
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ConfigDrawerTopButton(
                            onClick = { dispatcher.insert_channel() },
                            content = {
                                Icon(
                                    painter = painterResource(R.drawable.icon_add_channel),
                                    contentDescription = stringResource(R.string.btn_cfg_add_channel),
                                )
                            }
                        )
                    }
                }

                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1F)
                ) {
                    for (i in 0 until state_model.channel_count.value) {
                        val channel_data = state_model.channel_data[i]
                        Row {
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
                                            modifier = Modifier.padding(vertical = 0.dp, horizontal = 12.dp)
                                        )
                                        Text(
                                            channel_data.active_name.value ?: this@ComponentActivityEditor.get_default_preset_name(channel_data.instrument.value.first, channel_data.instrument.value.second),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1F)
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            ConfigDrawerChannelRightButton(
                                onClick = { dispatcher.remove_channel(i) },
                                content = {
                                    Icon(
                                        painter = painterResource(R.drawable.icon_delete_channel),
                                        contentDescription = stringResource(R.string.remove_channel, i)
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConfigDrawerBottomButton(
                        modifier = Modifier.weight(1F),
                        icon = R.drawable.icon_save,
                        description = R.string.btn_cfg_save,
                        onClick = {
                            scope.launch { this@ComponentActivityEditor.close_drawer() }
                            dispatcher.save()
                        }
                    )
                    Spacer(Modifier.weight(.2F))
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
                    Spacer(Modifier.weight(.2F))
                    ConfigDrawerBottomButton(
                        modifier = Modifier.weight(1F),
                        icon = R.drawable.icon_trash,
                        description = R.string.btn_cfg_delete,
                        enabled = this@ComponentActivityEditor.controller_model.project_exists.value,
                        onClick = {
                            scope.launch { this@ComponentActivityEditor.close_drawer() }
                            dispatcher.delete()
                        }
                    )
                    Spacer(Modifier.weight(.2F))
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
                        CircularProgressIndicator(
                            progress = { this@ComponentActivityEditor.state_model.export_progress.value },
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    this@ComponentActivityEditor.runOnUiThread {
                                        Toast.makeText(
                                            this@ComponentActivityEditor,
                                            "Hold to cancel Export",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onLongClick = {
                                     this@ComponentActivityEditor.export_wav_cancel()
                                }
                            ),
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
    override fun LayoutSmallPortrait() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutXLargeLandscape() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutLargeLandscape() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutMediumLandscape() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun LayoutSmallLandscape() {
        TODO("Not yet implemented")
    }

    private fun get_default_preset_name(bank: Int, program: Int): String {
        val preset_names = this.resources.getStringArray(R.array.general_midi_presets)
        return this.resources.getString(R.string.unavailable_preset, preset_names[program])
    }

    fun project_save() {
        val configuration = this.view_model.configuration
        if (configuration.project_directory == null || DocumentFile.fromTreeUri(this, configuration.project_directory!!)?.exists() != true) {
            this._result_launcher_set_project_directory_and_save.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                    intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    configuration.project_directory?.let {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                    }
                }
            )
        }
    }

    fun toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, id, length).show()
    }

    fun open_settings() {
        this.result_launcher_settings.launch(
            Intent(this, ComponentActivitySettings::class.java).apply {
                this@ComponentActivityEditor.controller_model.active_project?.let {
                    this.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                }
            }
        )
    }

    fun open_about() {
        this.startActivity(Intent(this, ComponentActivityAbout::class.java))
    }

    fun load_project(uri: Uri) {
        val input_stream = this.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText().toByteArray(Charsets.UTF_8)

        reader.close()
        input_stream?.close()

        this.controller_model.opus_manager.load(content) {
            this.controller_model.active_project = uri
            this.controller_model.project_exists.value = true
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
            val text_view = TextView(this)
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

        this.view_model.create_dialog { close ->
            @Composable {
                Row{
                    DialogSTitle(R.string.dlg_export)
                }
                Row {
                    UnSortableMenu(Modifier, this@ComponentActivityEditor.get_exportable_options()) { export_type ->
                        close()
                        this@ComponentActivityEditor.export(export_type)
                    }
                }
                DialogBar(neutral = close)
            }
        }
    }

    private fun get_exportable_options(): List<Pair<Exportable, @Composable () -> Unit>> {
        val export_options = mutableListOf<Pair<Exportable, @Composable () -> Unit>>()
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


}
