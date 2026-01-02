package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.SoundFontWarning
import com.qfs.pagan.composable.button.Button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class ComponentActivityLanding: PaganComponentActivity() {
    internal var result_launcher_import_project =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
                    this.startActivity(
                        Intent(this, ComponentActivityEditor::class.java).apply {
                            this.data = uri
                        }
                    )
                }
            }
        }
    internal var result_launcher_settings =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            this.reload_config()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun RowScope.TopBar() { }

    override fun on_back_press_check(): Boolean {
        return true
    }

    @Composable
    fun LayoutMenu() {
        Column( verticalArrangement = Arrangement.Center ) {
            if (this@ComponentActivityLanding.view_model.project_manager?.has_backup_saved() == true) {
                ButtonRecent()
                Padder()
            }
            ButtonNew()
            Padder()
            if (this@ComponentActivityLanding.view_model.has_saved_project.value) {
                ButtonLoad()
                Padder()
            }
            ButtonImport()
            Padder()
            ButtonSettings()
            Padder()
            ButtonAbout()
        }
    }

    @Composable
    fun LayoutMenuCompact() {
        Column(Modifier.padding(horizontal = 4.dp)) {
            if (this@ComponentActivityLanding.view_model.project_manager?.has_backup_saved() == true) {
                ButtonRecent()
                Padder()
            }

            if (!this@ComponentActivityLanding.view_model.has_saved_project.value) {
                ButtonNew()
            } else {
                val corner_radius = 32.dp
                Row {
                    ButtonNew(
                        Modifier.weight(1F),
                        RoundedCornerShape(corner_radius, 0.dp, 0.dp, corner_radius)
                    )
                    Padder()
                    ButtonLoad(
                        Modifier.weight(1F),
                        RoundedCornerShape(0.dp, corner_radius, corner_radius, 0.dp)
                    )
                }
            }
            Padder()
            ButtonImport()
            Padder()
            Row {
                val corner_radius = 32.dp
                ButtonSettings(
                    Modifier.weight(1F),
                    RoundedCornerShape(corner_radius, 0.dp, 0.dp, corner_radius)
                )
                Padder()
                ButtonAbout(
                    Modifier.weight(1F),
                    RoundedCornerShape(0.dp, corner_radius, corner_radius, 0.dp)
                )
            }
        }
    }

    @Composable
    fun RowScope.Padder() {
        Spacer(Modifier.width(dimensionResource(R.dimen.landing_padding)))
    }
    @Composable
    fun ColumnScope.Padder() {
        Spacer(Modifier.height(dimensionResource(R.dimen.landing_padding)))
    }

    @Composable
    fun ButtonRecent(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier
                .height(dimensionResource(R.dimen.landing_button_height))
                .fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_most_recent)) },
            onClick = {
                this@ComponentActivityLanding.startActivity(
                    Intent(this, ComponentActivityEditor::class.java).apply {
                        this.putExtra("load_backup", true)
                    }
                )
            }
        )
    }

    @Composable
    fun ButtonNew(
        modifier: Modifier = Modifier,
        shape: Shape = ButtonDefaults.shape
    ) {
        Button(
            modifier = modifier
                .height(dimensionResource(R.dimen.landing_button_height))
                .fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_new)) },
            shape = shape,
            onClick = {

                this@ComponentActivityLanding.startActivity(
                    Intent(
                        this@ComponentActivityLanding,
                        ComponentActivityEditor::class.java
                    )
                )
            }
        )
    }

    @Composable
    fun ButtonLoad(
        modifier: Modifier = Modifier,
        shape: Shape = ButtonDefaults.shape
    ) {
        Button(
            modifier = modifier
                .height(dimensionResource(R.dimen.landing_button_height))
                .fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_load)) },
            shape = shape,
            onClick = {
                this.load_menu_dialog {
                    this@ComponentActivityLanding.startActivity(
                        Intent(this@ComponentActivityLanding, ComponentActivityEditor::class.java).apply {
                            this.data = it
                        }
                    )
                }
            }
        )
    }


    @Composable
    fun ButtonImport(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier
                .height(dimensionResource(R.dimen.landing_button_height))
                .fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_import)) },
            onClick = {
                this.result_launcher_import_project.launch(
                    Intent().apply {
                        this.setAction(Intent.ACTION_GET_CONTENT)
                        this.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                    }
                )
            }
        )
    }


    @Composable
    fun ButtonSettings(
        modifier: Modifier = Modifier,
        shape: Shape = ButtonDefaults.shape
    ) {
        Button(
            modifier = modifier
                .height(dimensionResource(R.dimen.landing_button_height))
                .fillMaxWidth(),
            shape = shape,
            content = { Text(stringResource(R.string.btn_landing_settings)) },
            onClick = {
                this@ComponentActivityLanding.result_launcher_settings.launch(
                    Intent(this@ComponentActivityLanding, ComponentActivitySettings::class.java)
                )
            }
        )
    }

    @Composable
    fun ButtonAbout(
        modifier: Modifier = Modifier,
        shape: Shape = ButtonDefaults.shape
    ) {
        Button(
            modifier = modifier
                .height(dimensionResource(R.dimen.landing_button_height))
                .fillMaxWidth(),
            shape = shape,
            content = { Text(stringResource(R.string.btn_landing_about)) },
            onClick = {
                this@ComponentActivityLanding.startActivity(Intent(this@ComponentActivityLanding, ComponentActivityAbout::class.java))
            }
        )
    }

    @Composable
    override fun LayoutXLargePortrait() {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier.width(SIZE_L.first),
                verticalArrangement = Arrangement.Center
            ) {
                if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                    Row(modifier = Modifier.padding(8.dp)) { SoundFontWarning() }
                }
                Row { LayoutMenu() }
            }
        }
    }

    @Composable
    override fun Drawer(modifier: Modifier) { }

    @Composable
    override fun LayoutLargePortrait() {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(SIZE_M.first),
                verticalArrangement = Arrangement.Center
            ) {
                if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                    Row(modifier = Modifier.padding(8.dp)) { SoundFontWarning() }
                }
                Row { LayoutMenu() }
            }
        }
    }

    @Composable
    override fun LayoutMediumPortrait() {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier.width(SIZE_M.first),
                verticalArrangement = Arrangement.Center
            ) {
                if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                    Row(modifier = Modifier.padding(8.dp)) { SoundFontWarning() }
                }
                Row { LayoutMenu() }
            }
        }
    }

    @Composable
    override fun LayoutSmallPortrait() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier.width(SIZE_S.first),
                verticalArrangement = Arrangement.Center
            ) {
                if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                    Row(modifier = Modifier.padding(8.dp)) { SoundFontWarning() }
                }
                Row { LayoutMenuCompact() }
            }
        }
    }

    @Composable
    override fun LayoutXLargeLandscape() = LayoutMediumLandscape()
    @Composable
    override fun LayoutLargeLandscape() = LayoutMediumLandscape()

    @Composable
    override fun LayoutMediumLandscape() {
        Row (
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(SIZE_M.first),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                    Row(Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                    ) {
                        SoundFontWarning()
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    LayoutMenuCompact()
                }
            }
        }
    }

    @Composable
    override fun LayoutSmallLandscape() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
       ) {
            if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                Row(Modifier.padding(8.dp)) {
                    SoundFontWarning()
                }
            }
            LayoutMenuCompact()
        }
    }

}
