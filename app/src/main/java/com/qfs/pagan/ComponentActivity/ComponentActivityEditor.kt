package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.apres.InvalidMIDIFile
import com.qfs.apres.Midi
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.Activity.ActivityAbout
import com.qfs.pagan.Activity.ActivitySettings
import com.qfs.pagan.Activity.PaganActivity.Companion.EXTRA_ACTIVE_PROJECT
import com.qfs.pagan.CompatibleFileType
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
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
import com.qfs.pagan.uibill.UIFacade
import com.qfs.pagan.viewmodel.ViewModelEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader

class ComponentActivityEditor: PaganComponentActivity() {
    val model_editor: ViewModelEditor by this.viewModels()
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
            this.view_model.project_manager?.change_project_path(tree_uri, this.model_editor.active_project.value)

            this._project_save()
        }

    internal var result_launcher_settings =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri = result.data?.getStringExtra(EXTRA_ACTIVE_PROJECT)?.toUri() ?: return@registerForActivityResult
            this.model_editor.active_project.value = uri
        }

    //internal var result_launcher_import =
    //    this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    //        if (result.resultCode == RESULT_OK) {
    //            result?.data?.data?.also { uri ->
    //                this.handle_uri(uri)
    //            }
    //        }
    //    }



    override fun onCreate(savedInstanceState: Bundle?) {
        val action_interface = this.model_editor.action_interface
        this.model_editor.opus_manager.project_change_new()
        action_interface.attach_activity(this)
        action_interface.attach_opus_manager(this.model_editor.opus_manager)
        super.onCreate(savedInstanceState)

        this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val that = this@ComponentActivityEditor
                val opus_manager = that.model_editor.opus_manager
                val ui_facade = opus_manager.ui_facade
                if (ui_facade.active_cursor.value != null) {
                    action_interface.cursor_clear()
                } else {
                    TODO()
                }

                // val drawer_layout = that.findViewById<DrawerLayout>(R.id.drawer_layout)
                // if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                //     //TODO()
                //     that.drawer_close()
                // } else if (opus_manager.cursor.mode != CursorMode.Unset) {
                //     opus_manager.cursor_clear()
                // } else {
                //     that.dialog_save_project {
                //         that.save_to_backup()
                //         that.finish()
                //     }
                // }
            }
        })

        if (savedInstanceState != null) {
            // if the activity is forgotten, the opus_manager is be uninitialized
            if (this.model_editor.opus_manager.is_initialized()) {
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
            this.setup_new()
        } else if (this.view_model.project_manager?.contains(this.intent.data!!) == true) {
            this.load_project(this.intent.data!!)
        } else {
            this.handle_uri(this.intent.data!!)
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

    fun import_project(uri: Uri) {
        this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            val bytes = FileInputStream(it.fileDescriptor).readBytes()
            this.model_editor.opus_manager.load(bytes)
            this.model_editor.active_project.value = null
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

        val opus_manager = this.model_editor.opus_manager
        opus_manager.project_change_midi(midi)
        val filename = this.parse_file_name(uri)
        opus_manager.set_project_name(filename?.substring(0, filename.lastIndexOf(".")) ?: this.getString( R.string.default_imported_midi_title))
        opus_manager.clear_history()
        this.model_editor.active_project.value = null
    }

    fun parse_file_name(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = this.contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
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

        this.dialog_save_project {
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
        this.model_editor.action_interface.detach_opus_manager()
        this.model_editor.action_interface.detach_activity()
        super.onDestroy()
    }

    @Composable
    fun ContextMenuPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
        val cursor = ui_facade.active_cursor.value
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLinePrimary(ui_facade, dispatcher)
            CursorMode.Column -> ContextMenuColumnPrimary(ui_facade, dispatcher)
            CursorMode.Single -> ContextMenuSinglePrimary(ui_facade, dispatcher)
            CursorMode.Range -> ContextMenuRangePrimary(ui_facade, dispatcher)
            CursorMode.Channel -> ContextMenuChannelPrimary(ui_facade, dispatcher)
            CursorMode.Unset,
            null -> Text("TODO")
        }
    }
    @Composable
    fun ContextMenuSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
        val cursor = ui_facade.active_cursor.value
        when (cursor?.type) {
            CursorMode.Line -> ContextMenuLineSecondary(ui_facade, dispatcher)
            CursorMode.Column -> ContextMenuColumnSecondary(ui_facade, dispatcher)
            CursorMode.Single -> ContextMenuSingleSecondary(ui_facade, dispatcher)
            CursorMode.Range -> ContextMenuRangeSecondary(ui_facade, dispatcher)
            CursorMode.Channel -> ContextMenuChannelSecondary(ui_facade, dispatcher)
            CursorMode.Unset,
            null -> Text("TODO")
        }
    }

    @Composable
    fun MainTable(ui_facade: UIFacade, dispatcher: ActionTracker, length: MutableState<Int>) {
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
                Column(
                    Modifier
                        .horizontalScroll(scroll_state_h)
                ) {
                    Row {
                        for ((x, width) in column_widths.enumerate()) {
                            Box(
                                Modifier
                                    .width(leaf_width * width)
                                    .height(line_height)
                            ) {
                                BeatLabelView(x, ui_facade, dispatcher)
                            }
                        }
                    }
                }
            }
            Row {
                Column(Modifier.verticalScroll(scroll_state_v)) {
                    for (y in ui_facade.line_data.indices) {
                        val use_height = if (ui_facade.line_data[y].ctl_type != null) {
                            ctl_line_height
                        } else {
                            line_height
                        }
                        Row(
                            Modifier
                                .height(use_height)
                                .width(dimensionResource(R.dimen.line_label_width))
                        ) {
                            LineLabelView(modifier = Modifier.fillMaxSize(), y, ui_facade, dispatcher)
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(scroll_state_v)
                        .horizontalScroll(scroll_state_h)
                ) {
                    for ((y, line) in ui_facade.cell_map.enumerate()) {
                        val use_height = if (ui_facade.line_data[y].ctl_type != null) {
                            ctl_line_height
                        } else {
                            line_height
                        }

                        Row(Modifier.height(use_height)) {
                            for (x in 0 until length.value) {
                                Column(Modifier.width(leaf_width * column_widths[x])) {
                                    CellView(ui_facade, dispatcher, y, x)
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
        Icon(
            modifier = Modifier
                .combinedClickable(
                    onClick = { TODO() },
                    onLongClick = {
                        dispatcher.cursor_select_column(0)
                        scope.launch { scroll_state.animateScrollTo(0) }
                    }
                )
                .width(dimensionResource(R.dimen.line_label_width))
                .height(dimensionResource(R.dimen.line_height)),
            painter = painterResource(R.drawable.icon_shortcut),
            contentDescription = stringResource(R.string.jump_to_section)
        )
    }

    @Composable
    fun LineLabelView(modifier: Modifier = Modifier, y: Int, ui_facade: UIFacade, dispatcher: ActionTracker) {
        val line_info = ui_facade.line_data[y]
        when (line_info.ctl_type) {
            null -> {
                val label = if (line_info.assigned_offset != null) {
                    "!${line_info.channel}|${line_info.assigned_offset!!}"
                } else {
                    "${line_info.channel}|${line_info.line_offset}"
                }
                Text(label, modifier = modifier.combinedClickable(
                    onClick = {
                        dispatcher.cursor_select_line_std(line_info.channel!!, line_info.line_offset!!)
                    }
                ))
            }
            else -> {
                val (drawable_id, description_id) = when (line_info.ctl_type) {
                    EffectType.Tempo -> Pair(R.drawable.icon_tempo, R.string.ctl_desc_tempo)
                    EffectType.Velocity -> Pair(R.drawable.icon_velocity, R.string.ctl_desc_velocity)
                    EffectType.Volume -> Pair(R.drawable.icon_volume, R.string.ctl_desc_volume)
                    EffectType.Delay -> Pair(R.drawable.icon_echo, R.string.ctl_desc_delay)
                    EffectType.Pan -> Pair(R.drawable.icon_pan, R.string.ctl_desc_pan)
                    EffectType.LowPass -> TODO()
                    EffectType.Reverb -> TODO()
                    null -> throw Exception("Something terrible has happened...")
                }
                Icon(
                    modifier = modifier.combinedClickable(
                        onClick = {
                            if (line_info.line_offset != null) {
                                dispatcher.cursor_select_line_ctl_line(line_info.ctl_type!!, line_info.channel!!, line_info.line_offset!!)
                            } else if (line_info.channel != null) {
                                dispatcher.cursor_select_channel_ctl_line(line_info.ctl_type!!, line_info.channel!!)
                            } else {
                                dispatcher.cursor_select_global_ctl_line(line_info.ctl_type!!)
                            }
                        }
                    ),
                    painter = painterResource(drawable_id),
                    contentDescription = stringResource(description_id)
                )
            }
        }
    }

    @Composable
    fun BeatLabelView(x: Int, ui_facade: UIFacade, dispatcher: ActionTracker) {
        Button(modifier = Modifier.fillMaxSize(), onClick = { dispatcher.cursor_select_column(x) }) {
            Text("$x")
        }
    }

    @Composable
    fun <T: OpusEvent> LeafView(ui_facade: UIFacade, dispatcher: ActionTracker, y: Int, x: Int, path: List<Int>, event: T?, modifier: Modifier = Modifier) {
        val cursor = ui_facade.active_cursor.value
        val cell_selected = cursor != null && ui_facade.is_cell_selected(cursor, y, x)
        val leaf_selected = cursor != null && ui_facade.is_leaf_selected(cursor, y, x, path)
        val background_color = if (leaf_selected) {
            when (event) {
                is InstrumentEvent -> colorResource(R.color.leaf_selected)
                is EffectEvent -> colorResource(R.color.ctl_leaf_selected)
                else -> colorResource(R.color.leaf_empty_selected)
            }
        } else if (cell_selected) {
            when (event) {
                is InstrumentEvent -> colorResource(R.color.leaf_secondary)
                is EffectEvent -> colorResource(R.color.ctl_leaf_secondary)
                else -> colorResource(R.color.leaf_empty_selected)
            }
        } else {
            when (event) {
                is InstrumentEvent -> colorResource(R.color.leaf_main)
                is EffectEvent -> colorResource(R.color.ctl_leaf)
                else -> Color(0x10000000)
            }
        }

        Box(
            modifier
                .padding(1.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    // .dropShadow(
                    //     shape = RoundedCornerShape(8.dp),
                    //     shadow = Shadow(
                    //         radius = 4.dp,
                    //         spread = 2.dp,
                    //         color = Color(0x10000000),
                    //         offset = DpOffset(x = 4.dp, 4.dp)
                    //     )
                    // )
                    .background(color = background_color)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (event) {
                    is AbsoluteNoteEvent -> {
                        val octave = event.note / ui_facade.radix.value
                        val offset = event.note % ui_facade.radix.value
                        Text(
                            AnnotatedString.fromHtml("<sub>$octave</sub>$offset")
                        )
                    }

                    is RelativeNoteEvent -> {}
                    is PercussionEvent -> SText(R.string.percussion_label)
                    is OpusVolumeEvent -> Text("${event.value}")
                    is OpusPanEvent -> {}
                    is DelayEvent -> {}
                    is OpusTempoEvent -> Text("${event.value} BPM")
                    is OpusVelocityEvent -> {}
                    null -> {}
                }
            }
        }
    }

    @Composable
    fun CellView(ui_facade: UIFacade, dispatcher: ActionTracker, y: Int, x: Int, modifier: Modifier = Modifier) {
        val cell = ui_facade.cell_map[y][x].value
        val line_info = ui_facade.line_data[y]
        Row(modifier.fillMaxSize()) {
            composable_traverse(cell, listOf()) { tree, path, event ->
                if (tree.is_leaf()) {
                    this@ComponentActivityEditor.LeafView(
                        ui_facade, dispatcher, y, x, path, event,
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    if (line_info.ctl_type == null) {
                                        dispatcher.cursor_select(BeatKey(line_info.channel!!, line_info.line_offset!!, x), path)
                                    } else if (line_info.line_offset != null) {
                                        dispatcher.cursor_select_ctl_at_line(line_info.ctl_type!!, BeatKey(line_info.channel!!, line_info.line_offset!!, x), path)
                                    } else if (line_info.channel != null) {
                                        dispatcher.cursor_select_ctl_at_channel(line_info.ctl_type!!, line_info.channel!!, x, path)
                                    } else {
                                        dispatcher.cursor_select_ctl_at_global(line_info.ctl_type!!, x, path)
                                    }
                                },
                                onLongClick = {
                                    if (line_info.ctl_type == null) {
                                        dispatcher.cursor_select_range_next(BeatKey(line_info.channel!!, line_info.line_offset!!, x))
                                    } else if (line_info.line_offset != null) {
                                        dispatcher.cursor_select_line_ctl_range_next(line_info.ctl_type!!, BeatKey(line_info.channel!!, line_info.line_offset!!, x))
                                    } else if (line_info.channel != null) {
                                        dispatcher.cursor_select_channel_ctl_range_next(line_info.ctl_type!!, line_info.channel!!, x)
                                    } else {
                                        dispatcher.cursor_select_global_ctl_range_next(line_info.ctl_type!!, x)
                                    }
                                }
                            )
                            .weight(tree.weighted_size.toFloat())
                    )
                }
            }
        }
    }

    @Composable
    fun <T> composable_traverse(tree: ReducibleTree<T>, path: List<Int>, callback: @Composable (ReducibleTree<T>, List<Int>, T?) -> Unit) {
        if (! tree.is_leaf()) {
            for ((i, child) in tree.divisions) {
                this.composable_traverse(child, path + listOf(i), callback)
            }
        }
        callback(tree, path, tree.event)
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
        val view_model = this.model_editor
        val ui_facade = this.model_editor.opus_manager.ui_facade
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.fillMaxSize()) {
                MainTable(ui_facade, view_model.action_interface,  ui_facade.beat_count)
            }
            if (ui_facade.active_cursor.value?.type != CursorMode.Unset) {
                Box(Modifier) {
                    Column {
                        Row { ContextMenuPrimary(ui_facade, view_model.action_interface) }
                        Row { ContextMenuSecondary(ui_facade, view_model.action_interface) }
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


    private fun _project_save() {
        this.view_model.project_manager?.let {
            it.save(
                this.model_editor.opus_manager,
                this.model_editor.active_project.value,
                this.view_model.configuration.indent_json
            )
            this.model_editor.opus_manager.ui_facade.set_project_exists(true)
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
                this@ComponentActivityEditor.model_editor.active_project.value?.let {
                    this.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                }
            }
        )
    }

    fun open_about() {
        this.startActivity(Intent(this, ActivityAbout::class.java))
    }

    fun dialog_save_project(callback: (Boolean) -> Unit) {
        if (!this.needs_save()) {
            callback(false)
            return
        }

        this.view_model.create_dialog { close ->
            @Composable {
                Column {
                    SText(R.string.dialog_save_warning_title)
                    Row {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1F),
                            onClick = {
                                close()
                                callback(false)
                            },
                            content = { SText(android.R.string.no) }
                        )
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1F),
                            onClick = {
                                this@ComponentActivityEditor.project_save()
                                close()
                                callback(true)
                            },
                            content = { SText(android.R.string.ok) }
                        )
                    }
                }
            }
        }
    }

    private fun needs_save(): Boolean {
        val opus_manager = this.model_editor.opus_manager

        val active_project = this.model_editor.active_project.value ?: return !opus_manager.history_cache.is_empty()
        if (DocumentFile.fromSingleUri(this, active_project)?.exists() != true) return true

        val input_stream = this.contentResolver.openInputStream(active_project)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content: ByteArray = reader.readText().toByteArray(Charsets.UTF_8)

        val other = OpusLayerBase()
        other.load(content)

        reader.close()
        input_stream?.close()

        return (opus_manager as OpusLayerBase) != other
    }

    fun load_project(uri: Uri) {
        val input_stream = this.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText().toByteArray(Charsets.UTF_8)

        reader.close()
        input_stream?.close()

        this.model_editor.opus_manager.load(content) {
            this.model_editor.active_project.value = uri
        }
    }

    fun setup_new() {
        this.model_editor.opus_manager.project_change_new()

        // TODO:: Not sure this should be here
        // val opus_manager = this.model_editor.opus_manager
        // set the default instrument to the first available in the soundfont (if applicable)
        // val ui_facade = opus_manager.ui_facade
        // for (c in opus_manager.channels.indices) {
        //     if (!opus_manager.is_percussion(c)) continue

        //     // Need to prematurely update the channel instrument to find the lowest possible instrument
        //     this.update_channel_instruments(c)
        //     val percussion_keys = ui_facade.instrument_names[c]?.keys?.sorted() ?: continue

        //     for (l in 0 until opus_manager.get_channel(c).size) {
        //         opus_manager.percussion_set_instrument(c, l, max(0, percussion_keys.first() - 27))
        //     }
        // }
    }
}