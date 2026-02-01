package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.pagan.DialogChain
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogCard
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.PaganTheme
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.ScaffoldWithTopBar
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.projectmanager.ProjectManager
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.roundToInt

abstract class PaganComponentActivity: ComponentActivity() {
    @Composable
    abstract fun LayoutXLargePortrait(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutLargePortrait(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutMediumPortrait(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutSmallPortrait(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutXLargeLandscape(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutLargeLandscape(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutMediumLandscape(modifier: Modifier = Modifier)
    @Composable
    abstract fun LayoutSmallLandscape(modifier: Modifier = Modifier)
    @Composable
    abstract fun Drawer(modifier: Modifier = Modifier)

    open val top_bar_wrapper: (@Composable RowScope.() -> Unit)? = null

    abstract fun on_back_press_check(): Boolean

    suspend fun open_drawer() {
        this.drawer_state.open()
    }
    suspend fun close_drawer() {
        this.drawer_state.close()
    }

    val view_model: ViewModelPagan by this.viewModels()
    lateinit var drawer_state: DrawerState
    val drawer_gesture_enabled = mutableStateOf(false)
    var debug_mode = false

    init {
        System.loadLibrary("pagan")
    }

    override fun onResume() {
        super.onResume()
    }

    open fun on_crash() { }

    /**
     * Save text file in storage of a crash report.
     * To be copied and saved somewhere accessible on reload.
     */
    fun bkp_crash_report(e: Throwable) {
        val file = File("${this.dataDir}/bkp_crashreport.log")
        file.writeText(e.stackTraceToString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.debug_mode = this.packageName == "com.qfs.pagandev"
        super.onCreate(savedInstanceState)

        if (!this.debug_mode) {
            Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable ->
                Log.d("pagandebug", "$paramThrowable")
                this.bkp_crash_report(paramThrowable)
                this.on_crash()

                val ctx = this.applicationContext
                val pm = ctx.packageManager
                val intent = pm.getLaunchIntentForPackage(ctx.packageName) ?: return@setDefaultUncaughtExceptionHandler
                ctx.startActivity(
                    Intent.makeRestartActivityTask(intent.component)
                )
                Runtime.getRuntime().exit(0)
            }
        }

        val view_model = this.view_model
        view_model.load_config("${this.applicationContext.filesDir}/pagan.cfg")
        this.on_config_load()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = 0x000000,
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = 0x000000,
                darkScrim = 0x000000
            )
        )

        setContent {
            this.drawer_state = rememberDrawerState(DrawerValue.Closed) { state ->
                this.drawer_gesture_enabled.value = state == DrawerValue.Open
                true
            }
            val scope = rememberCoroutineScope()
            BackHandler(enabled = true) {
                if (this.drawer_state.isOpen) {
                    scope.launch { this@PaganComponentActivity.close_drawer() }
                } else if (this.on_back_press_check()) {
                    this.finish()
                }
            }
            val is_night_mode = when (this.view_model.configuration.night_mode.value) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemInDarkTheme()
            }

            view_model.set_layout_size(
                LocalWindowInfo.current.containerDpSize.width,
                LocalWindowInfo.current.containerDpSize.height,
            )

            PaganTheme(is_night_mode) {
                // Popup Dialogs --------------------------------
                var current_dialog = view_model.dialog_queue.value
                val dialogs = mutableListOf<DialogChain>()
                while (current_dialog != null) {
                    dialogs.add(current_dialog)
                    current_dialog = current_dialog.parent
                }
                val keyboard_controller = LocalSoftwareKeyboardController.current
                val focus_manager = LocalFocusManager.current
                for (dialog in dialogs.reversed()) {
                    Dialog( onDismissRequest = { view_model.dialog_queue.value = dialog.parent } ) {
                        DialogCard(
                            modifier = Modifier
                                // Allow click-away from text fields
                                .wrapContentSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        keyboard_controller?.hide()
                                        focus_manager.clearFocus()
                                    }
                                },
                            content = dialog.dialog
                        )
                    }
                }
                // -----------------------------------------------

                Box(
                    Modifier
                        .imePadding()
                        .background(MaterialTheme.colorScheme.scrim)
                        .systemBarsPadding()
                ) {
                    ScaffoldWithTopBar(
                        modifier = Modifier
                            .wrapContentWidth(),
                        top_app_bar = this@PaganComponentActivity.top_bar_wrapper,
                        drawerState = this@PaganComponentActivity.drawer_state,
                        gesturesEnabled = this@PaganComponentActivity.drawer_gesture_enabled.value,
                        drawerContent = { this@PaganComponentActivity.Drawer() },
                        content = {
                            Box(modifier = Modifier.padding(it)) {
                                Canvas(
                                    Modifier
                                        .background(MaterialTheme.colorScheme.background)
                                        .fillMaxSize()
                                ) {
                                    val gap_width = Dimensions.Background.Gap.toPx()
                                    val bar_width = Dimensions.Background.BarWidth.toPx()
                                    val bar_height_small = Dimensions.Background.BarSmallHeight.toPx()
                                    val bar_height_large = Dimensions.Background.BarLargeHeight.toPx()
                                    clipRect {
                                        for (x in 0 until ceil(this.size.width / (bar_width + gap_width)).toInt()) {
                                            var y_offset = if (x % 2 == 0) {
                                                bar_height_large
                                            } else {
                                                bar_height_small
                                            } / -2F
                                            var y = 0
                                            while (y_offset < this.size.height) {
                                                val bar_height = if (x % 2 == 0) {
                                                    if (y % 2 == 0) {
                                                        bar_height_large
                                                    } else {
                                                        bar_height_small
                                                    }
                                                } else if (y % 2 == 0) {
                                                    bar_height_small
                                                } else {
                                                    bar_height_large
                                                }
                                                drawRoundRect(
                                                    color = Color(0x10888888),
                                                    topLeft = Offset(
                                                        x = (x * (bar_width + gap_width)) - (bar_width / 2F),
                                                        y = y_offset
                                                    ),
                                                    size = Size(
                                                        width = bar_width,
                                                        height = bar_height
                                                    ),
                                                    cornerRadius = CornerRadius(Dimensions.Background.Radius.toPx())
                                                )

                                                y_offset += bar_height + gap_width
                                                y++
                                            }
                                        }
                                    }
                                }

                                val layout_size = view_model.get_layout_size()
                                val modifier = Modifier.fillMaxSize()
                                when (layout_size) {
                                    LayoutSize.SmallPortrait -> LayoutSmallPortrait(modifier)
                                    LayoutSize.MediumPortrait -> LayoutMediumPortrait(modifier)
                                    LayoutSize.LargePortrait -> LayoutLargePortrait(modifier)
                                    LayoutSize.XLargePortrait -> LayoutXLargePortrait(modifier)
                                    LayoutSize.SmallLandscape -> LayoutSmallLandscape(modifier)
                                    LayoutSize.MediumLandscape -> LayoutMediumLandscape(modifier)
                                    LayoutSize.LargeLandscape -> LayoutLargeLandscape(modifier)
                                    LayoutSize.XLargeLandscape -> LayoutXLargeLandscape(modifier)
                                }

                            }
                        }
                    )
                }
            }
        }
    }

    fun reload_config() {
        this.view_model.reload_config()
        this.on_config_load()
    }

    open fun on_config_load() {
        this.view_model.set_project_manager(ProjectManager(this, this.view_model.configuration.project_directory.value))
        AppCompatDelegate.setDefaultNightMode(this.view_model.configuration.night_mode.value)
        this.requestedOrientation = this.view_model.configuration.force_orientation.value
        this.view_model.requires_soundfont.value = !this.is_soundfont_available()
    }


    fun get_existing_soundfonts(): List<Uri> {
        return this.get_existing_uris(this.view_model.configuration.soundfont_directory.value)
    }

    internal fun get_existing_uris(top_uri: Uri?): List<Uri> {
        if (top_uri == null) return listOf()

        val document_id = DocumentsContract.getTreeDocumentId(top_uri)
        val uri_tree = DocumentsContract.buildChildDocumentsUriUsingTree(top_uri, document_id)

        val existing_uris = mutableListOf<Uri>()
        val stack = mutableListOf<Uri>(uri_tree)
        while (stack.isNotEmpty()) {
            val working_uri = stack.removeAt(0)
            this.contentResolver.query(working_uri, arrayOf(COLUMN_MIME_TYPE, COLUMN_DOCUMENT_ID), null, null, null)?.let { cursor ->
                while (cursor.moveToNext()) {
                    val mime_index = cursor.getColumnIndex(COLUMN_MIME_TYPE)
                    val id_index = cursor.getColumnIndex(COLUMN_DOCUMENT_ID)
                    if (cursor.getString(mime_index) != MIME_TYPE_DIR) {
                        val uri = DocumentsContract.buildDocumentUri(working_uri.authority, cursor.getString(id_index))
                        val new_uri = "${top_uri.scheme}://${top_uri.authority}${top_uri.encodedPath}${uri.encodedPath}".toUri()
                        existing_uris.add(new_uri)
                    } else {
                        val uri = DocumentsContract.buildChildDocumentsUri(top_uri.authority, cursor.getString(id_index))
                        val new_uri = "${top_uri.scheme}://${top_uri.authority}${top_uri.encodedPath}${uri.encodedPath}".toUri()
                        stack.add(new_uri)
                    }
                }
                cursor.close()
            }
        }
        return existing_uris
    }
    fun coerce_soundfont_uri(): Uri? {
        val file_path = this.view_model.configuration.soundfont.value ?: return null
        var soundfont_file = this.get_soundfont_directory()
        for (segment in file_path.split("/")) {
            soundfont_file = soundfont_file.findFile(segment) ?: throw FileNotFoundException()
        }
        return soundfont_file.uri
    }

    fun get_soundfont_directory(): DocumentFile {
        return if (this.view_model.configuration.soundfont_directory.value != null) {
            DocumentFile.fromTreeUri(this,this.view_model.configuration.soundfont_directory.value!!)!!
        } else {
            val soundfont_dir = this.applicationContext.getDir("SoundFonts", MODE_PRIVATE)
            if (!soundfont_dir.exists()) {
                soundfont_dir.mkdirs()
            }

            DocumentFile.fromFile(soundfont_dir)
        }
    }

    private fun create_project_card_dialog(title: String, uri: Uri, load_callback: (Uri) -> Unit) {
        this.view_model.create_dialog(level = 1) { close ->
            @Composable {
                Column {
                    ProjectCard(modifier = Modifier.fillMaxWidth(), uri = uri)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            modifier = Modifier.height(Dimensions.ButtonHeight.Small),
                            onClick = close,
                            content = { SText(android.R.string.cancel) }
                        )
                        Spacer(Modifier.width(4.dp))
                        Button(
                            modifier = Modifier.height(Dimensions.ButtonHeight.Small),
                            onClick = {
                                close()
                                this@PaganComponentActivity.view_model.create_dialog { close_subdialog ->
                                    @Composable {
                                        Column {
                                            Row { DialogTitle(stringResource(R.string.dlg_delete_title, title), modifier = Modifier.weight(1F)) }
                                            Row {
                                                Button(
                                                    onClick = close_subdialog,
                                                    content = { SText(android.R.string.cancel) }
                                                )
                                                Button(
                                                    onClick = {
                                                        close_subdialog()
                                                        this@PaganComponentActivity.view_model.project_manager?.delete(
                                                            uri
                                                        )
                                                    },
                                                    content = { SText(android.R.string.cancel) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            content = {
                                Icon(
                                    painter = painterResource(R.drawable.icon_trash),
                                    contentDescription = stringResource(R.string.delete_project)
                                )
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Button(
                            modifier = Modifier.height(Dimensions.ButtonHeight.Small),
                            onClick = {
                                close()
                                load_callback(uri)
                            },
                            content = { SText(R.string.details_load_project) }
                        )
                    }
                }
            }
        }
    }

    fun load_menu_dialog(load_callback: (Uri) -> Unit) {
        val project_list = this.view_model.project_manager?.get_project_list() ?: return
        val items = mutableListOf<Pair<Uri, @Composable RowScope.() -> Unit>>()
        for ((uri, title) in project_list) {
            items.add(
                Pair(uri, {
                    Text(text = title)
                })
            )
        }

        val sort_options = listOf(
            Pair(R.string.sort_option_abc) { a: Int, b: Int -> project_list[a].second.lowercase().compareTo(project_list[b].second.lowercase()) },
            Pair(R.string.sort_option_date_modified) { a: Int, b: Int ->
                val df_a = DocumentFile.fromSingleUri(this, project_list[a].first) ?: return@Pair -1
                val df_b = DocumentFile.fromSingleUri(this, project_list[b].first) ?: return@Pair 1
                df_a.lastModified().compareTo(df_b.lastModified())
            },
        )

        this.view_model.sortable_list_dialog(
            R.string.menu_item_load_project,
            items,
            sort_options,
            selected_sort = 0,
            onClick = load_callback,
            onLongClick = { it, close_callback ->
                for ((uri, title) in project_list) {
                    if (uri != it) continue
                    this.create_project_card_dialog(title, it) {
                        close_callback()
                        load_callback(it)
                    }
                    break
                }
            }
        )
    }

    @Composable
    fun ProjectCard(modifier: Modifier = Modifier, uri: Uri) {
        val other_project = this.view_model.project_manager?.open_project(uri) ?: return
        val document_file = DocumentFile.fromSingleUri(this@PaganComponentActivity, uri) ?: return
        val time = Date(document_file.lastModified())
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val initial_tempo = other_project.get_global_controller<OpusTempoEvent>(EffectType.Tempo).initial_event.value

        val padding = 8.dp
        Column(modifier) {
            ProvideTextStyle(Typography.DialogTitle) {
                if (other_project.project_name == null) {
                    SText(R.string.untitled_opus)
                } else {
                    Text(other_project.project_name!!)
                }
            }
            Spacer(Modifier.height(padding))

            ProvideTextStyle(Typography.DialogBody) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SText(R.string.last_modified)
                    Spacer(Modifier.width(padding))
                    Text(formatter.format(time))
                }
                Spacer(Modifier.height(padding))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.project_info_beat_count, other_project.length))
                    Spacer(Modifier.width(padding))
                    Text(stringResource(R.string.project_info_channel_count, other_project.channels.size))
                    Spacer(Modifier.width(padding))
                    Text(stringResource(R.string.project_info_tempo, initial_tempo.roundToInt()))
                }
            }

            Spacer(Modifier.height(padding))

            ProvideTextStyle(Typography.ProjectNotes) {
                Row(
                    Modifier
                        .dashed_border(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                            width = 1.dp
                        )
                ) {
                    Text(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth(),
                        text = if (other_project.project_notes != null) {
                            other_project.project_notes!!
                        } else {
                            stringResource(R.string.no_project_notes)
                        }
                    )
                }

            }
        }
    }

    fun is_soundfont_available(): Boolean {
        return this.get_existing_soundfonts().isNotEmpty()
    }
}
