package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.SettingsRow
import com.qfs.pagan.composable.SoundFontWarning
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.ui.theme.Dimensions
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ComponentActivityLanding: PaganComponentActivity() {
    private var result_launcher_save_crash_report =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val file = File("${this.dataDir}/bkp_crashreport.log")
            if (result.resultCode != RESULT_OK) {
                file.delete()
                return@registerForActivityResult
            }

            val uri = result.data?.data ?: return@registerForActivityResult
            val content = file.readText()
            this.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).write(content.toByteArray())
                file.delete()
            }
        }

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

    override fun onResume() {
        super.onResume()
        this.reload_config()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.check_for_crash_report()
    }

    override fun on_back_press_check(): Boolean {
        return true
    }


    @Composable
    fun RowScope.Padder(factor: Float = 1F) {
        Spacer(Modifier.width(dimensionResource(R.dimen.landing_padding) * factor))
    }
    @Composable
    fun ColumnScope.Padder(factor: Float = 1F) {
        Spacer(Modifier.height(dimensionResource(R.dimen.landing_padding) * factor))
    }

    @Composable
    fun ButtonRecent(
        modifier: Modifier = Modifier,
        shape: Shape = ButtonDefaults.shape
    ) {
        Button(
            modifier = modifier.height(dimensionResource(R.dimen.landing_button_height)),
            content = { Text(stringResource(R.string.btn_landing_most_recent)) },
            shape = shape,
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
            modifier = modifier.height(dimensionResource(R.dimen.landing_button_height)),
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
            modifier = modifier.height(dimensionResource(R.dimen.landing_button_height)),
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
    fun SmallIconButton(
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        onClick: () -> Unit, content: @Composable RowScope.() -> Unit
    ) {
        Button(
            modifier = modifier
                .height(41.dp)
                .width(41.dp),
            onClick = onClick,
            contentPadding = contentPadding,
            shape = CircleShape,
            content = content
        )
    }

    @Composable
    fun ButtonImport(modifier: Modifier = Modifier) {
        SmallIconButton(
            modifier = modifier,
            contentPadding = PaddingValues(8.dp),
            content =  {
                Icon(
                    painter = painterResource(R.drawable.icon_import),
                    contentDescription = stringResource(R.string.btn_landing_import)
                )
            },
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
    fun ButtonSettings(modifier: Modifier = Modifier) {
        SmallIconButton(
            modifier = modifier,
            onClick = {
                this@ComponentActivityLanding.result_launcher_settings.launch(
                    Intent(this@ComponentActivityLanding, ComponentActivitySettings::class.java)
                )
            },
            content =  {
                Icon(
                    painter = painterResource(R.drawable.icon_settings),
                    contentDescription = stringResource(R.string.btn_landing_settings)
                )
            }
        )

        // Button(
        //     modifier = modifier,
        //     //.height(dimensionResource(R.dimen.landing_button_height)),
        //     //content = { Text(stringResource(R.string.btn_landing_settings)) },
        // )
    }

    @Composable
    fun ButtonAbout(
        modifier: Modifier = Modifier,
        shape: Shape = ButtonDefaults.shape
    ) {
        SmallIconButton(
            modifier = modifier,
            onClick = {
                this@ComponentActivityLanding.startActivity(
                    Intent(this@ComponentActivityLanding, ComponentActivityAbout::class.java)
                )
            },
            content =  {
                Icon(
                    painter = painterResource(R.drawable.icon_about),
                    contentDescription = stringResource(R.string.btn_landing_about)
                )
            }
        )
    }

    @Composable
    override fun Drawer(modifier: Modifier) { }

    @Composable
    override fun LayoutXLargePortrait(modifier: Modifier) = LayoutLargePortrait(modifier)

    @Composable
    override fun LayoutXLargeLandscape(modifier: Modifier) = LayoutLargeLandscape(modifier)

    @Composable
    override fun LayoutLargePortrait(modifier: Modifier) {
        val has_backup = this.view_model.project_manager?.has_backup_saved() == true
        val has_saved_project = this.view_model.has_saved_project.value
        Box(
            modifier = modifier.padding(dimensionResource(R.dimen.landing_padding)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .height(Dimensions.Layout.Medium.short)
                    .width(Dimensions.Layout.Large.long),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                    SoundFontWarning()
                } else {
                    Spacer(Modifier.height(41.dp))
                }

                Column(
                    Modifier.width(Dimensions.Layout.Medium.long),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (has_backup) {
                        ButtonRecent(Modifier.fillMaxWidth())
                        Padder()
                    }
                    ButtonNew(Modifier.fillMaxWidth())
                    if (has_saved_project) {
                        Padder()
                        ButtonLoad(Modifier.fillMaxWidth())
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LayoutSmallIconLinks()
                }
            }
        }

    }

    @Composable
    override fun LayoutLargeLandscape(modifier: Modifier) = LayoutLargePortrait(modifier)

    @Composable
    override fun LayoutMediumPortrait(modifier: Modifier) = LayoutSmallPortrait(modifier)

    @Composable
    override fun LayoutMediumLandscape(modifier: Modifier) = LayoutSmallLandscape(modifier)

    @Composable
    override fun LayoutSmallPortrait(modifier: Modifier) {
        val has_backup = this.view_model.project_manager?.has_backup_saved() == true
        val has_saved_project = this.view_model.has_saved_project.value
        Column(
            modifier = modifier
                .padding(Dimensions.LandingPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                SoundFontWarning()
            } else {
                Spacer(Modifier)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (has_backup) {
                    ButtonRecent(Modifier.fillMaxWidth())
                    Padder()
                }
                ButtonNew(Modifier.fillMaxWidth())
                if (has_saved_project) {
                    Padder()
                    ButtonLoad(Modifier.fillMaxWidth())
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LayoutSmallIconLinks()
            }
        }
    }

    @Composable
    override fun LayoutSmallLandscape(modifier: Modifier) {
        if (this.view_model.requires_soundfont.value) {
            this.LayoutSmallLandscapeNeedsSF(modifier)
        } else {
            this.LayoutSmallLandscapeNormal(modifier)
        }
    }

    @Composable
    fun LayoutSmallLandscapeNeedsSF(modifier: Modifier = Modifier) {
        val has_backup = this.view_model.project_manager?.has_backup_saved() == true
        val has_saved_project = this.view_model.has_saved_project.value
        val button_shape = RoundedCornerShape(50F, 0F, 0F, 50F)
        Row(
            modifier.padding(dimensionResource(R.dimen.landing_padding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1F),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SoundFontWarning()
            }
            Column(
                Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier)
                Column(horizontalAlignment = Alignment.End) {
                    if (has_backup) {
                        ButtonRecent(shape = button_shape)
                        Padder()
                    }
                    ButtonNew(shape = button_shape)
                    if (has_saved_project) {
                        Padder()
                        ButtonLoad(shape = button_shape)
                    }
                }

                LayoutSmallIconLinks()
            }
        }
    }

    @Composable
    fun LayoutSmallLandscapeNormal(modifier: Modifier = Modifier) {
        val has_backup = this.view_model.project_manager?.has_backup_saved() == true
        val has_saved_project = this.view_model.has_saved_project.value

        Box(
            modifier.padding(dimensionResource(R.dimen.landing_padding))
        ) {
            Column(
                Modifier
                    .width(Dimensions.Layout.Small.short)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (has_backup) {
                    ButtonRecent(Modifier.fillMaxWidth())
                    Padder()
                }
                ButtonNew(Modifier.fillMaxWidth())
                if (has_saved_project) {
                    Padder()
                    ButtonLoad(Modifier.fillMaxWidth())
                }
            }

            Box(Modifier.align(Alignment.BottomEnd)) {
                LayoutSmallIconLinks()
            }
        }
    }

    @Composable
    fun LayoutSmallIconLinks() {
        SettingsRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ButtonImport()
            Padder(2F)
            ButtonSettings()
            Padder(2F)
            ButtonAbout()
        }
    }

    fun check_for_crash_report() {
        val file = File("${this.dataDir}/bkp_crashreport.log")
        if (!file.isFile) return

        this.view_model.create_medium_dialog { close ->
            @Composable {
                DialogSTitle(R.string.crash_report_save)
                SText(
                    R.string.crash_report_desc,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                DialogBar(
                    negative = {
                        file.delete()
                        close()
                    },
                    positive = {
                        this@ComponentActivityLanding.export_crash_report()
                        close()
                    }
                )
            }
        }
    }

    fun export_crash_report() {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        this.result_launcher_save_crash_report.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).also { intent ->
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TITLE, "pagan.cr-${now.format(formatter)}.log")
            }
        )
    }

}
