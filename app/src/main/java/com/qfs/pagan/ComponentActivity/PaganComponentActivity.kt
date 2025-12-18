package com.qfs.pagan.ComponentActivity

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.pagan.DialogChain
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogCard
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.ScaffoldWithTopBar
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.projectmanager.ProjectManager
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.viewmodel.ViewModelPagan
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

abstract class PaganComponentActivity: ComponentActivity() {
    companion object {
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
    abstract fun RowScope.TopBar(modifier: Modifier = Modifier)
    @Composable
    abstract fun Drawer(modifier: Modifier = Modifier)



    suspend fun open_drawer() {
        this.drawer_state.open()
    }
    suspend fun close_drawer() {
        this.drawer_state.close()
    }

    val view_model: ViewModelPagan by this.viewModels()
    lateinit var drawer_state: DrawerState
    val drawer_gesture_enabled = mutableStateOf(false)

    init {
        System.loadLibrary("pagan")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view_model = this.view_model
        view_model.load_config("${this.applicationContext.filesDir}/pagan.cfg")
        this.on_config_load()

        this.setContent {
            this.drawer_state = rememberDrawerState(DrawerValue.Closed) { state ->
                this.drawer_gesture_enabled.value = state == DrawerValue.Open
                true
            }

            ModalNavigationDrawer(
                modifier = Modifier
                    .wrapContentWidth()
                    .safeContentPadding(),
                drawerState = this.drawer_state,
                gesturesEnabled = this.drawer_gesture_enabled.value,
                drawerContent = { this.Drawer() }
            ) {
                ScaffoldWithTopBar(
                    top_app_bar = { this.TopBar() },
                    this@PaganComponentActivity.view_model.night_mode
                ) {
                    BoxWithConstraints(modifier = Modifier.padding(it)) {
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
                                DialogCard(content = dialog.dialog )
                            }
                        }
                        // -----------------------------------------------

                        if (this.maxWidth >= this.maxHeight) {
                            if (this.maxWidth >= SIZE_XL.first && this.maxHeight >= SIZE_XL.second) {
                                println("LANDSCAPE XL")
                                LayoutXLargeLandscape()
                            } else if (this.maxWidth >= SIZE_L.first && this.maxHeight >= SIZE_L.second) {
                                println("LANDSCAPE L")
                                LayoutLargeLandscape()
                            } else if (this.maxWidth >= SIZE_M.first && this.maxHeight >= SIZE_M.second) {
                                println("LANDSCAPE M")
                                LayoutMediumLandscape()
                            } else {
                                println("LANDSCAPE S")
                                LayoutSmallLandscape()
                            }
                        } else {
                            if (this.maxWidth >= SIZE_XL.second && this.maxHeight >= SIZE_XL.first) {
                                println("PORTRAIT XL")
                                LayoutXLargePortrait()
                            } else if (this.maxWidth >= SIZE_L.second && this.maxHeight >= SIZE_L.first) {
                                println("PORTRAIT L")
                                LayoutLargePortrait()
                            } else if (this.maxWidth >= SIZE_M.second && this.maxHeight >= SIZE_M.first) {
                                println("PORTRAIT M")
                                LayoutMediumPortrait()
                            } else {
                                println("PORTRAIT S")
                                LayoutSmallPortrait()
                            }
                        }
                    }
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
        this.view_model.create_dialog(level = 1) { close ->
            @Composable {
                Column {
                    Row { ProjectCard(uri) }
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = close,
                            content = { SText(android.R.string.cancel) }
                        )
                        Button(
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
                            content = { SText(R.string.delete_project) }
                        )
                        Button(
                            onClick = {
                                close()
                                TODO()
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
    fun ProjectCard(uri: Uri) {
        val other_project = this.view_model.project_manager?.open_project(uri) ?: return
        val document_file = DocumentFile.fromSingleUri(this@PaganComponentActivity, uri) ?: return
        val time = Date(document_file.lastModified())
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val initial_tempo = other_project.get_global_controller<OpusTempoEvent>(EffectType.Tempo).initial_event.value

        Column {
            Row {
                if (other_project.project_name == null) {
                    SText(R.string.untitled_opus)
                } else {
                    Text(other_project.project_name!!)
                }
            }
            Row {
                SText(R.string.last_modified)
                Text(formatter.format(time))
            }
            Row {
                Text(stringResource(R.string.project_info_beat_count, other_project.length))
                Text(stringResource(R.string.project_info_channel_count, other_project.channels.size))
                Text(stringResource(R.string.project_info_tempo, initial_tempo.roundToInt()))
            }

            other_project.project_notes?.let {
                Row { Text(it) }
            }
        }
    }

    fun is_soundfont_available(): Boolean {
        return this.get_existing_soundfonts().isNotEmpty()
    }
}
