/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.ComponentActivity

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.SettingsRow
import com.qfs.pagan.composable.SoundFontWarning
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.put_config
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Layout
import com.qfs.pagan.ui.theme.MasterTheme
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

    override fun on_back_press_check(): Boolean {
        return true
    }

    @Composable
    fun RowScope.Padder(factor: Float = 1F) {
        Spacer(Modifier.width(MasterTheme.dimensions.LandingPadding * factor))
    }
    @Composable
    fun ColumnScope.Padder(factor: Float = 1F) {
        Spacer(Modifier.height(MasterTheme.dimensions.LandingPadding * factor))
    }

    @Composable
    fun ButtonRecent(
        modifier: Modifier = Modifier,
        shape: Shape = MasterTheme.shapes.LandingButtonShape
    ) {
        Button(
            modifier = modifier
                .testTag(TestTag.LandingRecent)
                .height(MasterTheme.dimensions.LandingButtonHeight),
            content = { Text(stringResource(R.string.btn_landing_most_recent)) },
            shape = shape,
            onClick = {
                val config = this.view_model.configuration
                this@ComponentActivityLanding.result_update_config.launch(
                    Intent(this, ComponentActivityEditor::class.java).apply {
                        this.put_config(config)
                        this.putExtra("load_backup", true)
                    }
                )
            }
        )
    }

    @Composable
    fun ButtonNew(
        modifier: Modifier = Modifier,
        shape: Shape = MasterTheme.shapes.LandingButtonShape
    ) {
        Button(
            modifier = modifier
                .testTag(TestTag.LandingNewProject)
                .height(MasterTheme.dimensions.LandingButtonHeight),
            content = { Text(stringResource(R.string.btn_landing_new)) },
            shape = shape,
            onClick = {
                val config = this.view_model.configuration
                this@ComponentActivityLanding.result_update_config.launch(
                    Intent(this@ComponentActivityLanding, ComponentActivityEditor::class.java)
                        .put_config(config)
                )
            }
        )
    }

    @Composable
    fun ButtonLoad(
        modifier: Modifier = Modifier,
        shape: Shape = MasterTheme.shapes.LandingButtonShape
    ) {
        val load_menu_visibility = remember { mutableStateOf(false) }
        Button(
            modifier = modifier
                .testTag(TestTag.LandingLoadProject)
                .height(MasterTheme.dimensions.LandingButtonHeight),
            content = { Text(stringResource(R.string.btn_landing_load)) },
            shape = shape,
            onClick = { load_menu_visibility.value = true }
        )

        LoadMenuDialog(load_menu_visibility, view_model.configuration.sort_load) {
            val config = this.view_model.configuration
            this@ComponentActivityLanding.result_update_config.launch(
                Intent(this@ComponentActivityLanding, ComponentActivityEditor::class.java).apply {
                    this.data = it
                    this.put_config(config)
                }
            )
        }
    }


    @Composable
    fun SmallIconButton(
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues(MasterTheme.dimensions.LandingIconButtonPadding),
        onClick: () -> Unit, content: @Composable RowScope.() -> Unit
    ) {
        Button(
            modifier = modifier
                .height(MasterTheme.dimensions.LandingIconButtonSize)
                .width(MasterTheme.dimensions.LandingIconButtonSize),
            onClick = onClick,
            contentPadding = contentPadding,
            shape = CircleShape,
            content = content
        )
    }

    @Composable
    fun ButtonImport(modifier: Modifier = Modifier) {
        SmallIconButton(
            modifier = modifier.testTag(TestTag.LandingImport),
            content =  {
                Icon(
                    modifier = Modifier.fillMaxSize(),
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
            modifier = modifier.testTag(TestTag.LandingSettings),
            onClick = {
                val config = this.view_model.configuration
                this@ComponentActivityLanding.result_update_config.launch(
                    Intent(this@ComponentActivityLanding, ComponentActivitySettings::class.java)
                        .put_config(config)
                )
            },
            content = {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.icon_settings),
                    contentDescription = stringResource(R.string.btn_landing_settings)
                )
            }
        )
    }

    @Composable
    fun ButtonAbout(
        modifier: Modifier = Modifier,
        shape: Shape = MasterTheme.shapes.LandingButtonShape
    ) {
        SmallIconButton(
            modifier = modifier.testTag(TestTag.LandingAbout),
            onClick = {
                val config = this.view_model.configuration
                this@ComponentActivityLanding.startActivity(
                    Intent(this@ComponentActivityLanding, ComponentActivityAbout::class.java)
                        .put_config(config)
                )
            },
            content =  {
                Icon(
                    modifier = Modifier.fillMaxSize(),
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
        Column(
            modifier = modifier.padding(MasterTheme.dimensions.LandingPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(1.dp))

            if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                SoundFontWarning()
            }

            Column(
                Modifier.width(Layout.Medium.long),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (view_model.has_backup_saved.value) {
                    ButtonRecent(Modifier.fillMaxWidth())
                    Padder()
                }
                ButtonNew(Modifier.fillMaxWidth())
                if (view_model.has_saved_project.value) {
                    Padder()
                    ButtonLoad(Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(1.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LayoutSmallIconLinks()
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
        Column(
            modifier = modifier
                .padding(MasterTheme.dimensions.LandingPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(1.dp))
            if (this@ComponentActivityLanding.view_model.requires_soundfont.value) {
                SoundFontWarning()
            }


            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (view_model.has_backup_saved.value) {
                    ButtonRecent(Modifier.fillMaxWidth())
                    Padder()
                }
                ButtonNew(Modifier.fillMaxWidth())
                if (view_model.has_saved_project.value) {
                    Padder()
                    ButtonLoad(Modifier.fillMaxWidth())
                }
            }


            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                content = { LayoutSmallIconLinks() }
            )
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
        val button_shape = MasterTheme.shapes.LandingButtonShapeNeedsSF
        Row(
            modifier.padding(MasterTheme.dimensions.LandingPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(2F)
                    .padding(MasterTheme.dimensions.SpaceLarge),
                contentAlignment = Alignment.Center
            ) {
                SoundFontWarning()
            }
            Column(
                Modifier
                    .weight(1F)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier)
                Column(horizontalAlignment = Alignment.End) {
                    if (view_model.has_backup_saved.value) {
                        ButtonRecent(shape = MasterTheme.shapes.LandingButtonShapeNeedsSF)
                        Padder()
                    }
                    ButtonNew(shape = button_shape)
                    if (view_model.has_saved_project.value) {
                        Padder()
                        ButtonLoad(shape = MasterTheme.shapes.LandingButtonShapeNeedsSF)
                    }
                }
                LayoutSmallIconLinks()

            }
        }
    }

    @Composable
    fun LayoutSmallLandscapeNormal(modifier: Modifier = Modifier) {
        Column(
            modifier.padding(MasterTheme.dimensions.LandingPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(1.dp))

            Column(
                Modifier.width(Layout.Small.short)
            ) {
                if (view_model.has_backup_saved.value) {
                    ButtonRecent(Modifier.fillMaxWidth())
                    Padder()
                }
                ButtonNew(Modifier.fillMaxWidth())
                if (view_model.has_saved_project.value) {
                    Padder()
                    ButtonLoad(Modifier.fillMaxWidth())
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LayoutSmallIconLinks()
            }
        }
    }

    @Composable
    override fun Dialogs() {
        val file = File("${this.dataDir}/bkp_crashreport.log")
        if (!file.isFile) return

        val crash_dialog_visibility = remember { mutableStateOf(true) }
        PaganDialog(crash_dialog_visibility) {
            DialogSTitle(R.string.crash_report_save)
            Text(
                R.string.crash_report_desc,
                modifier = Modifier.padding(vertical = MasterTheme.dimensions.BugReportPadding)
            )
            DialogBar(
                negative = {
                    file.delete()
                    crash_dialog_visibility.value = false
                },
                positive = {
                    this@ComponentActivityLanding.export_crash_report()
                    crash_dialog_visibility.value = false
                }
            )
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

    override fun on_key_press(e: KeyEvent): Boolean {
        return false
    }

}
