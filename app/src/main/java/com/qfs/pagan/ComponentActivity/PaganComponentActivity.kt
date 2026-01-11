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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.pagan.DialogChain
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogCard
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.PaganTheme
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.ScaffoldWithTopBar
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.projectmanager.ProjectManager
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

abstract class PaganComponentActivity: ComponentActivity() {
    companion object {
        // Sizes in Portrait
        val SIZE_XL = Pair(960.dp, 720.dp)
        val SIZE_L = Pair(640.dp, 480.dp)
        val SIZE_M = Pair(470.dp, 320.dp)
        val SIZE_S = Pair(426.dp, 320.dp)
    }

    @Composable
    abstract fun LayoutXLargePortrait()
    @Composable
    abstract fun LayoutLargePortrait()
    @Composable
    abstract fun LayoutMediumPortrait()
    @Composable
    abstract fun LayoutSmallPortrait()
    @Composable
    abstract fun LayoutXLargeLandscape()
    @Composable
    abstract fun LayoutLargeLandscape()
    @Composable
    abstract fun LayoutMediumLandscape()
    @Composable
    abstract fun LayoutSmallLandscape()
    @Composable
    abstract fun RowScope.TopBar()
    @Composable
    abstract fun Drawer(modifier: Modifier = Modifier)

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
            val is_night_mode = when (this.view_model.night_mode.value) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemInDarkTheme()
            }

            PaganTheme(is_night_mode) {
                Box(
                    Modifier
                        .background(MaterialTheme.colorScheme.scrim)
                        .systemBarsPadding()
                ) {
                    ScaffoldWithTopBar(
                        modifier = Modifier.wrapContentWidth(),
                        top_app_bar = { this.TopBar() },
                        drawerState = this@PaganComponentActivity.drawer_state,
                        gesturesEnabled = this@PaganComponentActivity.drawer_gesture_enabled.value,
                        drawerContent = { this@PaganComponentActivity.Drawer() },
                        content = {
                            BoxWithConstraints(modifier = Modifier.padding(it)) {
                                view_model.set_layout_size(this.maxWidth, this.maxHeight)
                                Box(
                                    modifier = Modifier
                                        .padding(32.dp)
                                        .fillMaxSize()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rowanleaf_no_padding),
                                        tint = colorResource(R.color.main_background_etching),
                                        contentDescription = "",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Popup Dialogs --------------------------------
                                var current_dialog = view_model.dialog_queue.value
                                val dialogs = mutableListOf<DialogChain>()
                                while (current_dialog != null) {
                                    dialogs.add(current_dialog)
                                    current_dialog = current_dialog.parent
                                }
                                for (dialog in dialogs.reversed()) {
                                    Dialog(onDismissRequest = { view_model.dialog_queue.value = dialog.parent }) {
                                        DialogCard(
                                            // TODO: These are just roughed in. need to put more thought in and check later
                                            modifier = when (view_model.get_layout_size()) {
                                                ViewModelPagan.LayoutSize.SmallPortrait,
                                                ViewModelPagan.LayoutSize.SmallLandscape,
                                                ViewModelPagan.LayoutSize.MediumPortrait -> Modifier

                                                ViewModelPagan.LayoutSize.LargePortrait,
                                                ViewModelPagan.LayoutSize.MediumLandscape -> {
                                                    when (dialog.size) {
                                                        ViewModelPagan.DialogSize.Unbounded -> Modifier
                                                        ViewModelPagan.DialogSize.Small -> Modifier.width(200.dp)
                                                        ViewModelPagan.DialogSize.Medium -> Modifier.width(400.dp)
                                                    }
                                                }

                                                ViewModelPagan.LayoutSize.XLargePortrait,
                                                ViewModelPagan.LayoutSize.XLargeLandscape,
                                                ViewModelPagan.LayoutSize.LargeLandscape -> {
                                                    when (dialog.size) {
                                                        ViewModelPagan.DialogSize.Unbounded -> Modifier
                                                        ViewModelPagan.DialogSize.Small -> Modifier.width(300.dp)
                                                        ViewModelPagan.DialogSize.Medium -> Modifier.width(SIZE_L.second)
                                                    }
                                                }
                                            },
                                            content = dialog.dialog
                                        )
                                    }
                                }
                                // -----------------------------------------------
                                println(" --- ${view_model.get_layout_size()} ---")
                                when (view_model.get_layout_size()) {
                                    ViewModelPagan.LayoutSize.SmallPortrait -> LayoutSmallPortrait()
                                    ViewModelPagan.LayoutSize.MediumPortrait -> LayoutMediumPortrait()
                                    ViewModelPagan.LayoutSize.LargePortrait -> LayoutLargePortrait()
                                    ViewModelPagan.LayoutSize.XLargePortrait -> LayoutXLargePortrait()
                                    ViewModelPagan.LayoutSize.SmallLandscape -> LayoutSmallLandscape()
                                    ViewModelPagan.LayoutSize.MediumLandscape -> LayoutMediumLandscape()
                                    ViewModelPagan.LayoutSize.LargeLandscape -> LayoutLargeLandscape()
                                    ViewModelPagan.LayoutSize.XLargeLandscape -> LayoutXLargeLandscape()
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
        this.view_model.set_project_manager(ProjectManager(this, this.view_model.configuration.project_directory))
        AppCompatDelegate.setDefaultNightMode(this.view_model.configuration.night_mode)
        this.requestedOrientation = this.view_model.configuration.force_orientation
        this.view_model.requires_soundfont.value = !this.is_soundfont_available()
    }


    fun get_existing_soundfonts(): List<Uri> {
        return this.get_existing_uris(this.view_model.configuration.soundfont_directory)
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
        val file_path = this.view_model.configuration.soundfont ?: return null
        var soundfont_file = this.get_soundfont_directory()
        for (segment in file_path.split("/")) {
            soundfont_file = soundfont_file.findFile(segment) ?: throw FileNotFoundException()
        }
        return soundfont_file.uri
    }

    fun get_soundfont_directory(): DocumentFile {
        return if (this.view_model.configuration.soundfont_directory != null) {
            DocumentFile.fromTreeUri(this,this.view_model.configuration.soundfont_directory!!)!!
        } else {
            val soundfont_dir = this.applicationContext.getDir("SoundFonts", MODE_PRIVATE)
            if (!soundfont_dir.exists()) {
                soundfont_dir.mkdirs()
            }

            DocumentFile.fromFile(soundfont_dir)
        }
    }

    private fun create_project_card_dialog(title: String, uri: Uri) {
        this.view_model.create_medium_dialog(level = 1) { close ->
            @Composable {
                Column {
                    ProjectCard(modifier = Modifier.fillMaxWidth(), uri = uri)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .height(dimensionResource(R.dimen.dialog_bar_button_height))
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = close,
                            content = { SText(android.R.string.cancel) }
                        )
                        Spacer(Modifier.width(4.dp))
                        Button(
                            modifier = Modifier.fillMaxHeight(),
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
                            modifier = Modifier.fillMaxHeight(),
                            onClick = close,
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
                    Text(
                        // modifier = Modifier.combinedClickable(
                        //     onClick = {},
                        //     onLongClick = { this@PaganComponentActivity.create_project_card_dialog(title, uri) }
                        // ),
                        text = title
                    )
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
            onClick = load_callback,
            onLongClick = {
                for ((uri, title) in project_list) {
                    if (uri != it) continue
                    this.create_project_card_dialog(title, it)
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
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                if (other_project.project_name == null) {
                    SText(R.string.untitled_opus)
                } else {
                    Text(other_project.project_name!!)
                }
            }
            Spacer(Modifier.height(padding))

            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SText(R.string.last_modified)
                    Spacer(Modifier.width(padding))
                    Text(formatter.format(time))
                }
            }
            Spacer(Modifier.height(padding))
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
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

            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                Row(
                    Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(4.dp)
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
