package com.qfs.pagan.Activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.ViewModelSettings
import com.qfs.pagan.find_activity
import kotlin.getValue


class ActivityComposerSettings: ComponentActivity() {
    internal var _set_soundfont_directory_intent_launcher =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.also { result_data ->
                    result_data.data?.also { uri  ->
                        val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        this.contentResolver.takePersistableUriPermission(uri, new_flags)
                        TODO()
                     //   this.set_soundfont_directory(uri)
                     //   this.on_soundfont_directory_set(uri)
                    }//
                }
            }
        }

    internal var result_launcher_set_project_directory =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.also { result_data ->
                    result_data.data?.also { uri  ->
                        val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        this.contentResolver.takePersistableUriPermission(uri, new_flags)
                        this.view_model.configuration.project_directory = uri

                        // TODO
                        // this.get_project_manager().change_project_path(uri, this.intent.data)?.let {
                        //     this.result_intent.putExtra(EXTRA_ACTIVE_PROJECT, it.toString())
                        // }

                        // this.update_result()
                        // this.on_project_directory_set(uri)
                    }
                }
            }
        }

    val view_model: ViewModelSettings by this.viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        this.view_model.configuration_path = "${this.applicationContext.cacheDir}/pagan.cfg"
        super.onCreate(savedInstanceState)

        val width_xl = 800.dp
        val width_l = 600.dp
        val width_m = 480.dp
        val width_s = 380.dp
        val width_xs = 320.dp

        this.setContent {
            CustomTheme {
                ScaffoldWithTopBar {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(it)
                    ) {
                        if (this.minWidth >= width_xl) {
                            Row(
                                modifier = Modifier
                                    .width(width_xl - 8.dp)
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .align(Alignment.Center),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .weight(.8f)
                                ) {
                                    SettingsSectionFirst()
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .weight(1f)
                                ) {
                                    SettingsSectionSecond()
                                }
                            }
                            //} else if (minWidth < width_xl) {
                            //} else if (minWidth < width_l) {
                            //} else if (minWidth < width_m) {
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    SettingsSectionFirst()
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    SettingsSectionSecond()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Light color palette
    val light_color_palette = lightColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC5),
        background = Color.White,
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.Black,
        onSurface = Color.Black,
    )

    val dark_color_scheme = darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC5),
        background = Color.Black,
        surface = Color.Black,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
    )

    MaterialTheme(content = content, colorScheme = if (darkTheme) dark_color_scheme else light_color_palette)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWithTopBar(content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Top App Bar")
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        //Icon(Icons.Filled.ArrowBack, "backIcon")
                    }
                },
                //backgroundColor = MaterialTheme.colors.primary,
                //contentColor = Color.White,
                //elevation = 10.dp
            )
        },
        content = content
    )
}

@Composable
fun SettingsSectionFirst() {
    val context = LocalContext.current.find_activity()
    val view_model = (context as ActivityComposerSettings).view_model
    val no_soundfont_text = stringResource(R.string.no_soundfont)
    val soundfont_button_label by remember { mutableStateOf<String>(view_model.configuration.soundfont ?: no_soundfont_text) }

    Column {
        Text(stringResource(R.string.label_settings_sf))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Text(soundfont_button_label)
        }

        Text(stringResource(R.string.label_settings_soundfont_directory))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context._set_soundfont_directory_intent_launcher.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                        intent.putExtra(Intent.EXTRA_TITLE, "Soundfonts")
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        //configuration.soundfont_directory?.let {
                        //    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                        //}
                    }
                )
            }) {
                Text("SF Directory")
            }


        Text(stringResource(R.string.label_settings_projects_directory))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.result_launcher_set_project_directory.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                        intent.putExtra(Intent.EXTRA_TITLE, "Pagan Projects")
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        view_model.configuration.project_directory?.let {
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                        }
                    }
                )
            }
        ) {
            Text("Project Directory")
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionSecond() {
    val options_orientation = listOf(
        Pair(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, R.string.settings_orientation_landscape),
        Pair(ActivityInfo.SCREEN_ORIENTATION_USER, R.string.settings_orientation_system),
        Pair(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, R.string.settings_orientation_portrait)
    )
    val options_nightmode = listOf(
        Pair(AppCompatDelegate.MODE_NIGHT_YES, R.string.settings_night_mode_yes),
        Pair(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.settings_night_mode_system),
        Pair(AppCompatDelegate.MODE_NIGHT_NO, R.string.settings_night_mode_no)
    )
    val options_playback = integerArrayResource(R.array.sample_rates)
    var slider_position by remember { mutableFloatStateOf(0F) }
    var slider_option_index by remember { mutableIntStateOf(0) }
    val context = LocalContext.current.find_activity()
    val view_model = (context as ActivityComposerSettings).view_model

    var clip_same_line_release by remember { mutableStateOf(view_model.configuration.clip_same_line_release) }
    var relative_mode by remember { mutableStateOf(view_model.configuration.relative_mode) }
    var use_preferred_soundfont by remember { mutableStateOf(view_model.configuration.use_preferred_soundfont) }
    var allow_std_percussion by remember { mutableStateOf(view_model.configuration.allow_std_percussion) }

    var selected_orientation_index by remember {
        for (i in options_orientation.indices) {
            if (options_orientation[i].first == view_model.configuration.force_orientation) {
                return@remember mutableIntStateOf(i)
            }
        }
        mutableIntStateOf(1)
    }
    var selected_night_mode_index by remember {
        for (i in options_nightmode.indices) {
            if (options_nightmode[i].first == view_model.configuration.night_mode) {
                return@remember mutableIntStateOf(i)
            }
        }
        mutableIntStateOf(1)
    }


    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(stringResource(R.string.label_settings_playback_quality))
            Text("${options_playback[slider_option_index]}Hz")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                thumb = {
                    Box(
                        Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(corner = CornerSize(12.dp)))
                            .size(24.dp)
                    )
                },
                steps = options_playback.size - 2,
                value = slider_position,
                onValueChange = {
                    slider_position = it
                    slider_option_index = (it * (options_playback.size - 1).toFloat()).toInt()
                    view_model.configuration.sample_rate = options_playback[slider_option_index]
                },
                modifier = Modifier.fillMaxWidth().weight(2F)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_settings_same_line_release))
            Switch(
                checked = clip_same_line_release,
                onCheckedChange = {
                    view_model.configuration.clip_same_line_release = it
                    clip_same_line_release = it
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_settings_relative))
            Switch(
                checked = relative_mode,
                onCheckedChange = {
                    relative_mode = it
                    view_model.configuration.relative_mode = it
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_settings_use_preferred_sf))
            Switch(
                checked = use_preferred_soundfont,
                onCheckedChange = {
                    use_preferred_soundfont = it
                    view_model.configuration.use_preferred_soundfont = it
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_settings_allow_std_percussion))
            Switch(
                checked = allow_std_percussion,
                onCheckedChange = {
                    allow_std_percussion = it
                    view_model.configuration.allow_std_percussion
                }
            )
        }

        Text(stringResource(R.string.settings_screen_orientation))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            options_orientation.forEachIndexed { index, (mode, resource) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options_orientation.size
                    ),
                    onClick = {
                        selected_orientation_index = index
                        view_model.configuration.force_orientation = mode
                        context.requestedOrientation = mode
                    },
                    selected = index == selected_orientation_index,
                    label = { Text(stringResource(resource)) }
                )
            }
        }

        Text(stringResource(R.string.settings_night_mode))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            options_nightmode.forEachIndexed { index, (mode, resource) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options_nightmode.size
                    ),
                    onClick = {
                        selected_night_mode_index = index
                        view_model.configuration.night_mode = mode
                        AppCompatDelegate.setDefaultNightMode(mode)
                    },
                    selected = index == selected_night_mode_index,
                    label = { Text(stringResource(resource)) }
                )
            }
        }
    }
}