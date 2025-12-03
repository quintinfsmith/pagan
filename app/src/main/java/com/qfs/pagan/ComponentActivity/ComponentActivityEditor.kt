package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.apres.soundfont2.Riff
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.Activity.ActivityAbout
import com.qfs.pagan.Activity.ActivityEditor.PlaybackState
import com.qfs.pagan.Activity.ActivitySettings
import com.qfs.pagan.Activity.PaganActivity.Companion.EXTRA_ACTIVE_PROJECT
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.BetterButton
import com.qfs.pagan.composable.button.ConfigDrawerBottomButton
import com.qfs.pagan.composable.button.ConfigDrawerChannelLeftButton
import com.qfs.pagan.composable.button.ConfigDrawerChannelRightButton
import com.qfs.pagan.composable.button.ConfigDrawerTopButton
import com.qfs.pagan.composable.button.TopBarIcon
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
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
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
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import kotlin.math.abs

class ComponentActivityEditor: PaganComponentActivity() {
    val controller_model: ViewModelEditorController by this.viewModels()
    val state_model: ViewModelEditorState by this.viewModels()
    val menu_items: List<Pair<Int, () -> Unit>> = listOf(
        Pair(R.string.menu_item_new_project) {
            this.controller_model.action_interface.save_before {
                this.controller_model.action_interface.new_project()
            }
        },
        Pair(R.string.menu_item_load_project) {
            this.load_menu_dialog { uri ->
                this.load_project(uri)
            }
        },
        Pair(R.string.menu_item_import) {
            this.result_launcher_import.launch(
                Intent().apply {
                    this.setAction(Intent.ACTION_GET_CONTENT)
                    this.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                }
            )
        },
        Pair(R.string.menu_item_settings) {
            this.result_launcher_settings.launch(
                Intent(this, ComponentActivitySettings::class.java)
            )
        },
        Pair(R.string.menu_item_about) { this.startActivity(Intent(this, ComponentActivityAbout::class.java)) },
    )
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

            this._project_save()
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
            } catch (_: Exception) {
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
        super.onDestroy()
    }

    @Composable
    override fun TopBar(modifier: Modifier) {
        val ui_facade = this.controller_model.opus_manager.vm_state
        val dispatcher = this.controller_model.action_interface
        val scope = rememberCoroutineScope()
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
                icon = R.drawable.icon_undo,
                description = R.string.menu_item_undo,
                callback = { dispatcher.apply_undo() }
            )
            TopBarIcon(
                icon = R.drawable.icon_play, // TODO: Play state
                description = R.string.menu_item_playpause,
                callback = { dispatcher.playback() }
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
                    for ((_, item) in this@ComponentActivityEditor.menu_items.enumerate()) {
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
        val cursor = ui_facade.active_cursor.value
        when (cursor?.type) {
            CursorMode.Line -> {
                ContextMenuLineSecondary(ui_facade, dispatcher)
            }
            CursorMode.Column -> ContextMenuColumnSecondary(ui_facade, dispatcher)
            CursorMode.Single -> ContextMenuSingleSecondary(ui_facade, dispatcher)
            CursorMode.Range -> {
                ContextMenuRangeSecondary(ui_facade, dispatcher, this@ComponentActivityEditor.controller_model.move_mode.value)
            }
            CursorMode.Channel -> {
                ContextMenuChannelSecondary(ui_facade, dispatcher)
            }
            CursorMode.Unset,
            null -> Text("TODO")
        }
    }

    @Composable
    fun MainTable(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, length: MutableState<Int>) {
        val line_height = dimensionResource(R.dimen.line_height)
        val ctl_line_height = dimensionResource(R.dimen.ctl_line_height)
        val leaf_width = dimensionResource(R.dimen.base_leaf_width)
        val column_widths = Array(ui_facade.beat_count.value) { i ->
            Array(ui_facade.cell_map.size) { j -> ui_facade.cell_map[j][i].value.weighted_size }.max()
        }

        val scope = rememberCoroutineScope()
        val scroll_state_v = rememberScrollState()
        val scroll_state_h = rememberScrollState()

        Column {
            Row {
                Column {
                    ShortcutView(dispatcher, scope, scroll_state_h)
                }
                Column(Modifier.horizontalScroll(scroll_state_h)) {
                    Row {
                        for ((x, width) in column_widths.enumerate()) {
                            Box(
                                Modifier
                                    .width(leaf_width * width)
                                    .height(line_height)
                            ) {
                                BeatLabelView(x, ui_facade, dispatcher, ui_facade.column_data[x])
                            }
                        }
                    }
                }
            }
            Row {
                Column(Modifier.verticalScroll(scroll_state_v)) {
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
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(scroll_state_v)
                        .horizontalScroll(scroll_state_h)
                ) {
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

                        Row(Modifier.height(use_height)) {
                            for (x in 0 until length.value) {
                                Column(Modifier.width(leaf_width * column_widths[x])) {
                                    val cell = ui_facade.cell_map[y][x]
                                    CellView(ui_facade, dispatcher, cell, y, x)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ShortcutView(dispatcher: ActionTracker, scope: CoroutineScope, scroll_state: ScrollState) {
        Box(
            Modifier.padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            BetterButton(
                onClick = {},
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier
                    .width(dimensionResource(R.dimen.line_label_width) - (2.dp))
                    .height(dimensionResource(R.dimen.line_height) - 2.dp),
                content = {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = { TODO() },
                                onLongClick = {
                                    dispatcher.cursor_select_column(0)
                                    scope.launch { scroll_state.animateScrollTo(0) }
                                }
                            ),
                        painter = painterResource(R.drawable.icon_shortcut),
                        contentDescription = stringResource(R.string.jump_to_section)
                    )
                }
            )
        }
    }

    @Composable
    fun LineLabelView(modifier: Modifier = Modifier, dispatcher: ActionTracker, line_info: ViewModelEditorState.LineData) {
        val (background_color, content_color) = if (line_info.is_selected.value) {
            Pair(R.color.label_selected, R.color.label_selected_text)
        } else {
            Pair(R.color.line_label, R.color.line_label_text)
        }

        val ctl_type = line_info.ctl_type.value
        Box(Modifier.padding(1.dp)) {
            if (ctl_type == null) {
                val (label_a, label_b) = if (line_info.assigned_offset.value != null) {
                    Pair("!${line_info.channel.value}", "${line_info.assigned_offset.value}")
                } else {
                    Pair("${line_info.channel.value}", "${line_info.line_offset.value}")
                }
                BetterButton(
                    onClick = {
                        dispatcher.cursor_select_line(line_info.channel.value, line_info.line_offset.value, line_info.ctl_type.value)
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {}
                        ),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonColors(
                        containerColor = colorResource(background_color),
                        contentColor = colorResource(content_color),
                        disabledContentColor = Color.Magenta,
                        disabledContainerColor = Color.Magenta
                    ),
                    content = {
                        Row {
                            Column(
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1F),
                                content = { Text(label_a) }
                            )
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1F),
                                content = { Text(label_b) }
                            )
                        }
                    }
                )
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
                BetterButton(
                    onClick = {},
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonColors(
                        containerColor = colorResource(background_color),
                        contentColor = colorResource(content_color),
                        disabledContentColor = Color.Magenta,
                        disabledContainerColor = Color.Magenta
                    ),
                    content = {
                        Icon(
                            modifier = modifier.combinedClickable(
                                onClick = {
                                    if (line_info.line_offset.value != null) {
                                        dispatcher.cursor_select_line_ctl_line(
                                            ctl_type,
                                            line_info.channel.value!!,
                                            line_info.line_offset.value!!
                                        )
                                    } else if (line_info.channel.value != null) {
                                        dispatcher.cursor_select_channel_ctl_line(ctl_type, line_info.channel.value!!)
                                    } else {
                                        dispatcher.cursor_select_global_ctl_line(ctl_type)
                                    }
                                }
                            ),
                            painter = painterResource(drawable_id),
                            contentDescription = stringResource(description_id)
                        )
                    }
                )
            }
        }
    }

    @Composable
    fun BeatLabelView(x: Int, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, column_info: ViewModelEditorState.ColumnData) {
        val modifier = Modifier

        if (!column_info.is_tagged.value) {
            modifier.border(width = 1.dp, color = Color.Red)
        }
        val (background_color, content_color) = if (column_info.is_selected.value) {
            Pair(
                R.color.label_selected,
                R.color.label_selected_text
            )
        } else {
            Pair(
                R.color.line_label,
                R.color.line_label_text
            )
        }

        Box(
            modifier = Modifier
                .padding(1.dp)
                .fillMaxSize()
        ) {
            BetterButton(
                shape = RoundedCornerShape(4.dp),
                colors = ButtonColors(
                    containerColor = colorResource(background_color),
                    contentColor = colorResource(content_color),
                    disabledContentColor = Color.Magenta,
                    disabledContainerColor = Color.Magenta
                ),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                onClick = { dispatcher.cursor_select_column(x) },
                content = {
                    Text(
                        text = "$x",
                        modifier = modifier
                            .padding(0.dp)
                    )
                }
            )
        }
    }

    @Composable
    fun <T: OpusEvent> LeafView(line_data: ViewModelEditorState.LineData, leaf_data: ViewModelEditorState.LeafData, event: T?, radix: Int, modifier: Modifier = Modifier) {
        val corner_radius = when (event) {
            is InstrumentEvent -> 12.dp
            else -> 4.dp
        }
        val (leaf_color, text_color) = if (leaf_data.is_selected.value) {
            when (event) {
                is InstrumentEvent -> Pair(colorResource(R.color.leaf_selected), colorResource(R.color.leaf_selected_text))
                is EffectEvent -> Pair(colorResource(R.color.ctl_leaf_selected), colorResource(R.color.ctl_leaf_selected_text))
                else -> Pair(colorResource(R.color.leaf_empty_selected), colorResource(R.color.leaf_empty_selected_foreground))
            }
        } else if (leaf_data.is_secondary.value) {
            when (event) {
                is InstrumentEvent -> Pair(colorResource(R.color.leaf_secondary), colorResource(R.color.leaf_secondary_text))
                is EffectEvent -> Pair(colorResource(R.color.ctl_leaf_secondary), colorResource(R.color.ctl_leaf_selected_text))
                else -> Pair(colorResource(R.color.leaf_empty_selected), colorResource(R.color.leaf_empty_selected_foreground))
            }
        } else {
            when (event) {
                is InstrumentEvent -> Pair(colorResource(R.color.leaf_main), colorResource(R.color.leaf_text))
                is EffectEvent -> Pair(colorResource(R.color.ctl_leaf), colorResource(R.color.ctl_leaf_text))
                else -> Pair(Color(0x10000000), Color(0x000000))
            }
        }
        val background_color = if (line_data.ctl_type.value != null) {
            colorResource(R.color.ctl_line)
        } else {
            colorResource(R.color.table_background)
        }

        Box(
            modifier
                .fillMaxSize()
                .background(color = background_color),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(1.dp)
                    .clip(RoundedCornerShape(corner_radius))
                    .background(color = leaf_color)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (event) {
                    is AbsoluteNoteEvent -> {
                        val octave = event.note / radix
                        val offset = event.note % radix
                        Text(
                            AnnotatedString.fromHtml("<sub>$octave</sub>$offset"),
                            color = text_color
                        )
                    }

                    is RelativeNoteEvent -> {
                        val octave = abs(event.offset) / radix
                        val offset = abs(event.offset) % radix
                        Row {
                            Column(modifier = Modifier
                                .fillMaxSize()
                                .weight(1F)) {
                                Text(if (event.offset > 0) "+" else "-" , color = text_color)
                                Text("$octave", color = text_color)
                            }
                            Column(modifier = Modifier
                                .fillMaxSize()
                                .weight(1F)) {
                                Text("$offset", color = text_color)
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
    }

    @Composable
    fun CellView(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, cell: MutableState<ReducibleTree<Pair<ViewModelEditorState.LeafData, OpusEvent?>>>, y: Int, x: Int, modifier: Modifier = Modifier) {
        val line_info = ui_facade.line_data[y]
        Row(modifier.fillMaxSize()) {
            composable_traverse(cell.value, listOf()) { tree, path, event, weight ->
                if (tree.is_leaf()) {
                    this@ComponentActivityEditor.LeafView(
                        line_info,
                        event!!.first,
                        event.second,
                        ui_facade.radix.value,
                        Modifier
                            .width(dimensionResource(R.dimen.base_leaf_width))
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
    }

    @Composable
    fun <T> composable_traverse(tree: ReducibleTree<T>, path: List<Int>, weight: Float = 1F, callback: @Composable (ReducibleTree<T>, List<Int>, T?, Float) -> Unit) {
        if (! tree.is_leaf()) {
            val new_weight = weight / tree.size.toFloat()
            for ((i, child) in tree.divisions) {
                this.composable_traverse(child, path + listOf(i), new_weight, callback)
            }
        }
        callback(tree, path, tree.event, weight)
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
                    Box(Modifier) {
                        Column {
                            Row { ContextMenuPrimary(ui_facade, view_model.action_interface) }
                            Row { ContextMenuSecondary(ui_facade, view_model.action_interface) }
                        }
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

        Card(modifier.wrapContentWidth()) {
            Column(
                modifier = Modifier.padding(12.dp).wrapContentWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(42.dp)
                ) {
                    Column(modifier = Modifier.weight(1F)) {
                        ConfigDrawerTopButton(
                            onClick = { dispatcher.set_tuning_table_and_transpose() },
                            content = { SText(R.string.label_tuning) }
                        )
                    }
                    Column {
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
                }

                Row(modifier = Modifier.weight(1F)) {
                    Column {
                        for (i in 0 until state_model.channel_count.value) {
                            val channel_data = state_model.channel_data[i]
                            Row {
                                ConfigDrawerChannelLeftButton(
                                    modifier = Modifier.weight(1F),
                                    onClick = { dispatcher.set_channel_preset(i) },
                                    content = { Text(channel_data.active_name.value) }
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
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ConfigDrawerBottomButton(
                        icon = R.drawable.icon_save,
                        description = R.string.btn_cfg_save,
                        onClick = {
                            scope.launch { this@ComponentActivityEditor.close_drawer() }
                            dispatcher.save()
                        }
                    )
                    ConfigDrawerBottomButton(
                        icon = R.drawable.icon_ic_baseline_content_copy_24,
                        description = R.string.btn_cfg_copy,
                        onClick = {
                            scope.launch { this@ComponentActivityEditor.close_drawer() }
                            dispatcher.project_copy()
                        }
                    )
                    ConfigDrawerBottomButton(
                        icon = R.drawable.icon_trash,
                        description = R.string.btn_cfg_delete,
                        onClick = {
                            scope.launch { this@ComponentActivityEditor.close_drawer() }
                            dispatcher.delete()
                        }
                    )
                    ConfigDrawerBottomButton(
                        icon = R.drawable.icon_export,
                        description = R.string.btn_cfg_export,
                        onClick = { 
                            this@ComponentActivityEditor.view_model.create_dialog { close ->
                                @Composable {
                                    BetterButton(onClick = close, content = { Text("TODO") })
                                }
                            }
                        }
                    )
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


    private fun _project_save() {
        this.view_model.project_manager?.let {
            it.save(
                this.controller_model.opus_manager,
                this.controller_model.active_project,
                this.view_model.configuration.indent_json
            )
            this.controller_model.opus_manager.vm_state.set_project_exists(true)
            this.toast(R.string.feedback_project_saved)
        }
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
        } else {
            this._project_save()
        }
    }

    fun toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, id, length).show()
    }

    fun open_settings() {
        this.result_launcher_settings.launch(
            Intent(this, ActivitySettings::class.java).apply {
                this@ComponentActivityEditor.controller_model.active_project?.let {
                    this.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                }
            }
        )
    }

    fun open_about() {
        this.startActivity(Intent(this, ActivityAbout::class.java))
    }

    fun load_project(uri: Uri) {
        val input_stream = this.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText().toByteArray(Charsets.UTF_8)

        reader.close()
        input_stream?.close()

        this.controller_model.opus_manager.load(content) {
            this.controller_model.active_project = uri
        }
    }

}
