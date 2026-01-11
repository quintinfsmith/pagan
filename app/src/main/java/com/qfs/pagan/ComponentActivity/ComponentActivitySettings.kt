package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.pagan.Activity.PaganActivity.Companion.EXTRA_ACTIVE_PROJECT
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.DropdownMenuItem
import com.qfs.pagan.composable.RadioMenu
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.MenuPadder
import com.qfs.pagan.composable.SettingsColumn
import com.qfs.pagan.composable.SettingsRow
import com.qfs.pagan.composable.SoundFontWarning
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.TopBarIcon
import com.qfs.pagan.enumerate
import java.io.FileInputStream
import java.io.FileNotFoundException

class ComponentActivitySettings: PaganComponentActivity() {
    override val top_bar_wrapper: @Composable (RowScope.() -> Unit)? = {
        TopBarIcon(
            icon = R.drawable.baseline_arrow_back_24,
            description = R.string.go_back,
            onClick = { this@ComponentActivitySettings.finish() }
        )
        Text(
            modifier = Modifier.weight(1F),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.settings_fragment_label)
        )
    }

    val options_orientation: List<Pair<Int, @Composable RowScope.() -> Unit>> = listOf(
        Pair(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) @Composable {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.settings_radio_height)),
                painter = painterResource(R.drawable.icon_landscape),
                contentDescription = stringResource(R.string.settings_orientation_landscape),
            )
        },
        Pair(ActivityInfo.SCREEN_ORIENTATION_USER) @Composable {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.settings_radio_height)),
                painter = painterResource(R.drawable.icon_orientation_system),
                contentDescription = stringResource(R.string.settings_orientation_system),
            )
        },
        Pair(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) @Composable {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.settings_radio_height)),
                painter = painterResource(R.drawable.icon_portrait),
                contentDescription = stringResource(R.string.settings_orientation_portrait),
            )
        }
    )
    val options_nightmode: List<Pair<Int, @Composable RowScope.() -> Unit>> = listOf(
        Pair(AppCompatDelegate.MODE_NIGHT_YES) @Composable {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.settings_radio_height)),
                painter = painterResource(R.drawable.icon_night_mode),
                contentDescription = stringResource(R.string.settings_night_mode_yes),
            )
        },
        Pair(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) @Composable {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.settings_radio_height)),
                painter = painterResource(R.drawable.icon_night_mode_system),
                contentDescription = stringResource(R.string.settings_night_mode_system)
            )
        },
        Pair(AppCompatDelegate.MODE_NIGHT_NO) @Composable {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.settings_radio_height)),
                painter = painterResource(R.drawable.icon_day_mode),
                contentDescription = stringResource(R.string.settings_night_mode_no)
            )
        }
    )

    private var _set_soundfont_directory_intent_launcher =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val result_data = result.data ?: return@registerForActivityResult
            val uri = result_data.data ?: return@registerForActivityResult
            val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            this.contentResolver.takePersistableUriPermission(uri, new_flags)
            this.view_model.configuration.soundfont = null
            this.view_model.configuration.soundfont_directory = uri
            this.view_model.soundfont_directory.value = uri
            this.view_model.save_configuration()
            this.view_model.requires_soundfont.value = !this.is_soundfont_available()
        }

    private var result_launcher_import_soundfont =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            val path = uri.path ?: throw FileNotFoundException()

            // Check if this selected file is within the soundfont_directory ////
            // We can directory is set here.
            val configuration = this.view_model.configuration
            val parent_segments = configuration.soundfont_directory!!.pathSegments!!.last()!!.split("/")
            val child_segments = uri.pathSegments!!.last()!!.split("/")
            val is_within_soundfont_directory = parent_segments.size < child_segments.size && parent_segments == child_segments.subList(0, parent_segments.size)
            //-----------------------------------------------------
            if (is_within_soundfont_directory) {
                configuration.soundfont = child_segments.subList(parent_segments.size, child_segments.size).joinToString("/")
                this.view_model.soundfont_name.value = this.view_model.configuration.soundfont
            } else {
                val soundfont_dir = this@ComponentActivitySettings.get_soundfont_directory()
                val file_name = this.parse_file_name(uri)!!

                soundfont_dir.createFile("*/*", file_name)?.let { new_file ->
                    this.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
                        try {
                            val output_stream = this.contentResolver.openOutputStream(new_file.uri, "wt")
                            val input_stream = FileInputStream(it.fileDescriptor)

                            val buffer = ByteArray(4096)
                            while (true) {
                                val read_size = input_stream.read(buffer)
                                if (read_size == -1) {
                                    break
                                }
                                output_stream?.write(buffer, 0, read_size)
                            }

                            input_stream.close()
                            output_stream?.flush()
                            output_stream?.close()

                        } catch (e: FileNotFoundException) {
                            // TODO:  Feedback? Only breaks on devices without properly implementation (realme RE549c)
                        }
                    }

                    try {
                        SoundFont(this, new_file.uri)
                        this.view_model.configuration.soundfont = this.view_model.coerce_relative_soundfont_path(new_file.uri)
                        this.view_model.save_configuration()
                        this.view_model.soundfont_name.value = this.view_model.configuration.soundfont
                        this.view_model.requires_soundfont.value = false
                    } catch (e: Exception) {
                        //this.feedback_msg(this.getString(R.string.feedback_invalid_sf2_file))
                        new_file.delete()
                       // this.loading_reticle_hide()
                       // return@thread
                    }
                }

                this.update_result()
            }
        }

    private var result_launcher_set_soundfont_directory_and_import =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val result_data = result.data ?: return@registerForActivityResult
            val uri = result_data.data ?: return@registerForActivityResult
            val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            this.contentResolver.takePersistableUriPermission(uri, new_flags)

            this.view_model.configuration.soundfont = null
            this.view_model.configuration.soundfont_directory = uri
            this.view_model.soundfont_directory.value = uri
            this.view_model.save_configuration()
            this.view_model.requires_soundfont.value = !this.is_soundfont_available()

            this.update_result()

            this@ComponentActivitySettings.show_soundfont_menu()
        }

    private var result_launcher_set_project_directory =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val result_data = result.data ?: return@registerForActivityResult
            val uri = result_data.data ?: return@registerForActivityResult

            val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            this.contentResolver.takePersistableUriPermission(uri, new_flags)
            this.view_model.configuration.project_directory = uri
            this.view_model.save_configuration()

            this.view_model.project_manager?.change_project_path(uri, this.intent.data)?.let {
                this.result_intent.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
            }
            this.view_model.project_directory.value = uri

            this.update_result()
        }

    var result_intent = Intent()
    private fun update_result() {
        // RESULT_OK lets the other activities know they need to reload the configuration
        this.setResult(RESULT_OK, this.result_intent)
    }

    private fun parse_file_name(uri: Uri): String? {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun on_config_load() {
        super.on_config_load()
    }

    @Composable
    override fun Drawer(modifier: Modifier) { }

    override fun on_back_press_check(): Boolean {
        return true
    }

    //fun set_soundfont_directory(uri: Uri) {
    //    this.view_model.configuration.soundfont_directory = uri
    //    this.save_configuration()
    //    this.ucheck_move_soundfonts()
    //}


    @Composable
    override fun LayoutXLargePortrait(modifier: Modifier) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.sf_menu_padding))
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                ActiveSoundfontButton(Modifier.weight(1F))
                SoundFontWarningWrapper(Modifier.weight(1F).padding(start = dimensionResource(R.dimen.sf_menu_padding)))
            }
            MenuPadder()
            Row {
                ActiveSoundfontDirectoryButton(Modifier.weight(1F))
                MenuPadder()
                ProjectsDirectoryButton(Modifier.weight(1F))
            }
            MenuPadder()

            Row {
                OptionNightMode(Modifier.weight(1F))
                MenuPadder()
                OptionOrientation(Modifier.weight(1F))
            }
            MenuPadder()
            SettingsSectionB()
            MenuPadder()
        }
    }

    @Composable
    override fun LayoutXLargeLandscape(modifier: Modifier) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.sf_menu_padding))
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                Modifier.weight(1F),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SoundFontWarningWrapper(Modifier.padding(bottom = dimensionResource(R.dimen.sf_menu_padding)))
                ActiveSoundfontButton(Modifier.fillMaxWidth())
                MenuPadder()
                ActiveSoundfontDirectoryButton(Modifier.fillMaxWidth())
                MenuPadder()
                ProjectsDirectoryButton(Modifier.fillMaxWidth())
                MenuPadder()
            }
            MenuPadder()
            Column(Modifier.weight(1.5F)) {
                Row {
                    OptionNightMode(Modifier.weight(1F))
                    MenuPadder()
                    OptionOrientation(Modifier.weight(1F))
                }
                MenuPadder()
                SettingsSectionB()
                MenuPadder()
            }
        }
    }

    @Composable
    override fun LayoutLargePortrait(modifier: Modifier) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.sf_menu_padding))
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoundFontWarningWrapper(Modifier.fillMaxWidth().padding(bottom = dimensionResource(R.dimen.sf_menu_padding)))
            ActiveSoundfontButton(Modifier.fillMaxWidth())
            MenuPadder()
            Row {
                ActiveSoundfontDirectoryButton(Modifier.weight(1F))
                MenuPadder()
                ProjectsDirectoryButton(Modifier.weight(1F))
            }
            MenuPadder()

            Row {
                OptionNightMode(Modifier.weight(1F))
                MenuPadder()
                OptionOrientation(Modifier.weight(1F))
            }
            MenuPadder()
            SettingsSectionB()
            MenuPadder()
        }
    }

    @Composable
    override fun LayoutLargeLandscape(modifier: Modifier) = LayoutXLargeLandscape(modifier)

    @Composable
    override fun LayoutMediumPortrait(modifier: Modifier) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.sf_menu_padding))
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoundFontWarningWrapper(Modifier.fillMaxWidth().padding(bottom = dimensionResource(R.dimen.sf_menu_padding)))
            ActiveSoundfontButton(Modifier.fillMaxWidth())
            MenuPadder()
            ActiveSoundfontDirectoryButton(Modifier.fillMaxWidth())
            MenuPadder()
            ProjectsDirectoryButton(Modifier.fillMaxWidth())
            MenuPadder()
            OptionNightMode(Modifier.fillMaxWidth())
            MenuPadder()
            OptionOrientation(Modifier.fillMaxWidth())
            MenuPadder()
            SettingsSectionB()
            MenuPadder()
        }
    }

    @Composable
    override fun LayoutMediumLandscape(modifier: Modifier) = LayoutXLargePortrait(modifier)

    @Composable
    override fun LayoutSmallPortrait(modifier: Modifier) = LayoutMediumPortrait(modifier)

    @Composable
    override fun LayoutSmallLandscape(modifier: Modifier) = LayoutMediumPortrait(modifier)

    fun show_soundfont_menu() {
        val file_list = this.get_existing_soundfonts()
        if (file_list.isEmpty()) {
            this.import_soundfont()
            return
        }

        val soundfonts = mutableListOf<Pair<Uri, @Composable RowScope.() -> Unit>>()
        for (uri in file_list.sortedBy { it.pathSegments.last().split("/").last().lowercase() }) {
            val relative_path_segments = uri.pathSegments.last().split("/")
            soundfonts.add(Pair(uri, { Text(relative_path_segments.last()) }))
        }

        this.view_model.create_dialog { close ->
            @Composable {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DialogSTitle(R.string.dialog_select_soundfont)
                        Spacer(modifier = Modifier.weight(1F))
                        Button(
                            modifier = Modifier.height(dimensionResource(R.dimen.sf_menu_icon_height)),
                            contentPadding = PaddingValues(8.dp),
                            content = {
                                Icon(
                                    painter = painterResource(R.drawable.icon_import),
                                    contentDescription = stringResource(R.string.option_import_soundfont)
                                )
                            },
                            shape = CircleShape,
                            onClick = {
                                close()
                                this@ComponentActivitySettings.import_soundfont()
                            }
                        )
                        MenuPadder()
                        Button(
                            modifier = Modifier.height(dimensionResource(R.dimen.sf_menu_icon_height)),
                            contentPadding = PaddingValues(8.dp),
                            content = {
                                Icon(
                                    painter = painterResource(R.drawable.no_soundfont),
                                    contentDescription = stringResource(R.string.no_soundfont)
                                )
                            },
                            shape = CircleShape,
                            onClick = {
                                view_model.configuration.soundfont = null
                                view_model.soundfont_name.value = null
                                view_model.save_configuration()
                                this@ComponentActivitySettings.update_result()
                                close()
                            }
                        )
                        MenuPadder()
                    }
                    MenuPadder()
                    UnSortableMenu(
                        modifier = Modifier.weight(1F),
                        options = soundfonts,
                        default_value = this@ComponentActivitySettings.coerce_soundfont_uri()
                    ) { uri ->
                        view_model.set_soundfont_uri(uri)
                        view_model.save_configuration()
                        this@ComponentActivitySettings.update_result()
                        close()
                    }
                    DialogBar(neutral = close)
                }
            }
        }
    }

    @Composable
    fun ActiveSoundfontButton(modifier: Modifier = Modifier) {
        val no_soundfont_text = stringResource(R.string.no_soundfont)
        SettingsColumn(modifier) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                SText(R.string.label_settings_sf)
            }

            MenuPadder()

            Button(
                content = {
                    Text(
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        text = view_model.soundfont_name.value ?: no_soundfont_text,
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis
                    )
                },
                onClick = {
                    if (this@ComponentActivitySettings.view_model.soundfont_directory.value == null) {
                        this@ComponentActivitySettings.view_model.create_dialog { close ->
                            @Composable {
                                DialogSTitle(R.string.settings_need_soundfont_directory)
                                DialogBar(
                                    positive = {
                                        close()
                                        this@ComponentActivitySettings.result_launcher_set_soundfont_directory_and_import.launch(
                                            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also {
                                                it.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        return@Button
                    }
                    this@ComponentActivitySettings.show_soundfont_menu()
                }
            )
        }

    }
    @Composable
    fun ActiveSoundfontDirectoryButton(modifier: Modifier = Modifier) {
        SettingsColumn(modifier) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                SText(R.string.label_settings_soundfont_directory)
            }
            MenuPadder()
            Button(
                onClick = {
                    this@ComponentActivitySettings._set_soundfont_directory_intent_launcher.launch(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                            intent.putExtra(Intent.EXTRA_TITLE, "Soundfonts")
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            view_model.configuration.soundfont_directory?.let {
                                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                            }
                        }
                    )
                },
                content = {
                    Text(
                        text = this@ComponentActivitySettings.view_model.soundfont_directory.value?.pathSegments?.last() ?: stringResource(R.string.label_settings_soundfont_directory),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis
                    )
                }
            )
        }
    }

    @Composable
    fun ProjectsDirectoryButton(modifier: Modifier = Modifier) {
        SettingsColumn(modifier) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                SText(R.string.label_settings_projects_directory)
            }
            MenuPadder()
            Button(
                onClick = {
                    this@ComponentActivitySettings.result_launcher_set_project_directory.launch(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                            intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            view_model.configuration.project_directory?.let {
                                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                            }
                        }
                    )
                },
                content = {
                    Text(
                        text = this@ComponentActivitySettings.view_model.project_directory.value?.pathSegments?.last() ?: stringResource(R.string.label_settings_projects_directory),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis
                    )
                }
            )
        }
    }

    @Composable
    fun SoundFontWarningWrapper(modifier: Modifier = Modifier) {
        if (this@ComponentActivitySettings.view_model.requires_soundfont.value) {
            Box(modifier, contentAlignment = Alignment.Center) {
                SoundFontWarning(true)
            }
        }
    }

    @Composable
    fun SettingsSectionA() {
        ActiveSoundfontButton()
        MenuPadder()
        ActiveSoundfontDirectoryButton()
        MenuPadder()
        ProjectsDirectoryButton()
    }

    @Composable
    fun PlaybackRateMenu(modifier: Modifier = Modifier) {
        val playback_expanded = remember { mutableStateOf(false) }
        val options_playback = integerArrayResource(R.array.sample_rates)
        val active_playback_option = remember {
            mutableStateOf(
                if (options_playback.contains(this.view_model.configuration.sample_rate)) {
                    options_playback.indexOf(this.view_model.configuration.sample_rate)
                } else {
                    options_playback.size - 1
                }
            )
        }

        SettingsRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SText(R.string.label_settings_playback_quality)

            Box(contentAlignment = Alignment.Center) {
                Button(
                    content = {
                        Text(
                            text = "${options_playback[active_playback_option.value]} hz",
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    },
                    onClick = { playback_expanded.value = !playback_expanded.value }
                )
                DropdownMenu(
                    expanded = playback_expanded.value,
                    onDismissRequest = { playback_expanded.value = false }
                ) {
                    for ((i, rate) in options_playback.enumerate()) {
                        DropdownMenuItem(
                            text = { Text("$rate hz") },
                            onClick = {
                                active_playback_option.value = i
                                view_model.configuration.sample_rate = options_playback[i]
                                view_model.save_configuration()
                                this@ComponentActivitySettings.update_result()
                                playback_expanded.value = false
                            }
                        )

                    }
                }
            }
        }
    }

    @Composable
    fun OptionClipNote(modifier: Modifier = Modifier) {
        var clip_same_line_release by remember { mutableStateOf(view_model.configuration.clip_same_line_release) }
        SettingsRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SText(
                R.string.label_settings_same_line_release,
                modifier = Modifier.weight(1F)
            )
            Switch(
                checked = clip_same_line_release,
                onCheckedChange = {
                    clip_same_line_release = it
                    view_model.configuration.clip_same_line_release = it
                    view_model.save_configuration()
                    this@ComponentActivitySettings.update_result()
                }
            )
        }
    }

    @Composable
    fun OptionRelativeMode(modifier: Modifier = Modifier) {
        var relative_mode by remember { mutableStateOf(view_model.configuration.relative_mode) }
        SettingsRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.label_settings_relative),
                modifier = Modifier.weight(1F)
            )
            Switch(
                checked = relative_mode,
                onCheckedChange = {
                    relative_mode = it
                    view_model.configuration.relative_mode = it
                    view_model.save_configuration()
                    this@ComponentActivitySettings.update_result()
                }
            )
        }
    }

    @Composable
    fun OptionUsePreferredSoundfont(modifier: Modifier = Modifier) {
        var use_preferred_soundfont by remember { mutableStateOf(view_model.configuration.use_preferred_soundfont) }
        SettingsRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.label_settings_use_preferred_sf),
                modifier = Modifier.weight(1F)
            )
            Switch(
                checked = use_preferred_soundfont,
                onCheckedChange = {
                    use_preferred_soundfont = it
                    view_model.configuration.use_preferred_soundfont = it
                    view_model.save_configuration()
                    this@ComponentActivitySettings.update_result()
                }
            )
        }
    }

    @Composable
    fun OptionAllowStdPercussion(modifier: Modifier = Modifier) {
        var allow_std_percussion by remember { mutableStateOf(view_model.configuration.allow_std_percussion) }
        SettingsRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.label_settings_allow_std_percussion),
                modifier = Modifier.weight(1F)
            )
            Switch(
                checked = allow_std_percussion,
                onCheckedChange = {
                    allow_std_percussion = it
                    view_model.configuration.allow_std_percussion = it
                    view_model.save_configuration()
                    this@ComponentActivitySettings.update_result()
                }
            )
        }
    }

    @Composable
    fun OptionNightMode(modifier: Modifier = Modifier) {
        SettingsColumn(modifier) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                SText(R.string.settings_screen_orientation)
            }
            MenuPadder()
            RadioMenu(
                options = options_orientation,
                active = remember { mutableStateOf(view_model.configuration.force_orientation) },
                callback = { mode ->
                    this@ComponentActivitySettings.requestedOrientation = mode
                    view_model.configuration.force_orientation = mode
                    view_model.save_configuration()
                    this@ComponentActivitySettings.update_result()
                }
            )
        }
    }

    @Composable
    fun OptionOrientation(modifier: Modifier = Modifier) {
        SettingsColumn(modifier) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                SText(R.string.settings_night_mode)
            }
            MenuPadder()
            RadioMenu(
                options = options_nightmode,
                active = remember { mutableStateOf(view_model.configuration.night_mode) },
                callback = { mode ->
                    view_model.configuration.night_mode = mode
                    view_model.night_mode.value = mode
                    view_model.save_configuration()
                    this@ComponentActivitySettings.update_result()
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsSectionB(modifier: Modifier = Modifier) {
        Column {
            PlaybackRateMenu(Modifier.fillMaxWidth())
            MenuPadder()
            OptionClipNote(Modifier.fillMaxWidth())
            MenuPadder()
            OptionRelativeMode(Modifier.fillMaxWidth())
            MenuPadder()
            OptionUsePreferredSoundfont(Modifier.fillMaxWidth())
            MenuPadder()
            OptionAllowStdPercussion(Modifier.fillMaxWidth())
        }
    }

    fun import_soundfont(uri: Uri? = null) {
        if (this.view_model.configuration.soundfont_directory == null) {
            //this.initial_dialog_select_soundfont_directory()
        } else if (uri == null) {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this.result_launcher_import_soundfont.launch(intent)
        } else {
            TODO("would only be used for debug atm anyway.")
        }
    }
}
