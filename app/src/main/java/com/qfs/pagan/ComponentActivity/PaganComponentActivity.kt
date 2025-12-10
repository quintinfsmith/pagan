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
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.pagan.DialogChain
import com.qfs.pagan.MenuDialogEventHandler
import com.qfs.pagan.R
import com.qfs.pagan.composable.Card
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.ScaffoldWithTopBar
import com.qfs.pagan.composable.SortableMenu
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.enumerate
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
    abstract fun TopBar(modifier: Modifier = Modifier)
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
        // TODO: Figure out why this.applicationContext.cacheDir is being cleared on boot
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
                        Box(modifier = Modifier
                            .padding(32.dp)
                            .fillMaxSize()) {
                            Icon(
                                painter = painterResource(R.drawable.rowanleaf_no_padding),
                                tint = colorResource(R.color.main_background_etching),
                                contentDescription = "",
                                modifier = Modifier.fillMaxSize()
                            )
                        }


                        var current_dialog = view_model.dialog_queue.value
                        val dialogs = mutableListOf<DialogChain>()
                        while (current_dialog != null) {
                            dialogs.add(current_dialog)
                            current_dialog = current_dialog.parent
                        }

                        for (dialog in dialogs.reversed()) {
                            Dialog(onDismissRequest = { view_model.dialog_queue.value = dialog.parent }) {
                                ProvideContentColorTextStyle(MaterialTheme.colorScheme.onBackground) {
                                    Card(
                                        modifier = Modifier
                                            .padding(dimensionResource(R.dimen.dialog_padding))
                                            .background(
                                                MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(corner = CornerSize(12.dp))
                                            ),
                                        content = dialog.dialog
                                    )
                                }
                            }
                        }

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


    internal fun <T> dialog_popup_sortable_menu(title: Int, options: List<Triple<T, Int?, String>>, default: T? = null, sort_options: List<Pair<String, (List<Triple<T, Int?, String>>) -> List<Triple<T, Int?, String>>>>, default_sort_option: Int = 0, event_handler: MenuDialogEventHandler<T>) {
        this.view_model.create_dialog { close ->
            @Composable {
                Column {
                    Row { DialogSTitle(title, modifier = Modifier.weight(1F)) }
                    Row {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for ((index, entry) in options.enumerate()) {
                                Text(
                                    entry.third,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                close()
                                                event_handler.on_submit(index, entry.first)
                                            },
                                            onLongClick = {
                                                close()
                                                event_handler.on_long_click_item(index, entry.first)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    Row {
                        com.qfs.pagan.composable.button.OutlinedButton(
                            modifier = Modifier.fillMaxWidth().weight(1F),
                            onClick = { close() },
                            content = { SText(android.R.string.cancel) }
                        )
                    }
                }
            }
        }

        // if (options.size > 1) {
        //     viewInflated.findViewById<View>(R.id.spinner_sort_options_wrapper).visibility = View.VISIBLE
        // }
        // val spinner = viewInflated.findViewById<Spinner>(R.id.spinner_sort_options)
        // val sortable_labels = List(sort_options.size * 2) { i: Int ->
        //     if (i % 2 == 0) {
        //         sort_options[i / 2].first
        //     } else {
        //         this.getString(R.string.sorted_list_desc, sort_options[i / 2].first)
        //     }
        // }

        // val recycler = viewInflated.findViewById<RecyclerView>(R.id.rvOptions)
        // val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
        //     .setTitle(title)
        //     .setView(viewInflated)
        //     .setOnDismissListener {
        //         this._popup_active = false
        //     }
        //     .setNegativeButton(this.getString(android.R.string.cancel)) { dialog, _ ->
        //         dialog.dismiss()
        //     }
        //     .show()

        // val adapter = PopupMenuRecyclerAdapter<T>(
        //     recycler,
        //     sort_options[default_sort_option].second(options),
        //     default,
        //     event_handler
        // )
        // event_handler.dialog = dialog

        // spinner.adapter = object: ArrayAdapter<String>(this, R.layout.spinner_list, sortable_labels) {
        //     override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //         val view = super.getView(position, convertView, parent)
        //         (view as TextView).gravity = Gravity.END
        //         return view
        //     }
        // }

        // spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
        //     override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        //         adapter.set_items(
        //             if (position % 2 == 1) {
        //                 sort_options[position / 2].second(options).asReversed()
        //             } else {
        //                 sort_options[position / 2].second(options)
        //             }
        //         )
        //     }
        //     override fun onNothingSelected(parent: AdapterView<*>?) {}
        // }

        // adapter.notifyDataSetChanged()

        // return dialog
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
        val items = mutableListOf<Pair<Uri, @Composable () -> Unit>>()
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

        this.view_model.create_dialog {  close ->
            @Composable {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row { DialogSTitle(R.string.menu_item_load_project, modifier = Modifier.weight(1F)) }
                    SortableMenu(
                        modifier = Modifier.weight(1F),
                        default_menu = items,
                        sort_options = sort_options,
                        selected_sort = 0,
                        onLongClick = {
                            for ((uri, title) in project_list) {
                                if (uri != it) continue
                                this@PaganComponentActivity.create_project_card_dialog(title, it)
                                break
                            }
                        },
                        onClick = {
                            close()
                            load_callback(it)
                        }
                    )
                    DialogBar(neutral = close)
                }
            }
        }
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
