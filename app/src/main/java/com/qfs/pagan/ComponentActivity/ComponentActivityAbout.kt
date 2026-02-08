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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.SettingsColumn
import com.qfs.pagan.composable.button.TopBarIcon
import com.qfs.pagan.composable.button.TopBarNoIcon
import com.qfs.pagan.find_activity
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography

class ComponentActivityAbout: PaganComponentActivity() {
    override val top_bar_wrapper: @Composable RowScope.() -> Unit = {
        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
        TopBarIcon(
            icon = R.drawable.baseline_arrow_back_24,
            description = R.string.go_back,
            contentAlignment = Alignment.CenterStart,
            onClick = { this@ComponentActivityAbout.finish() }
        )
        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
        Text(
            modifier = Modifier.weight(1F),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.app_name)
        )
        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
        TopBarNoIcon()
        Spacer(Modifier.width(Dimensions.TopBarItemSpace))
    }

    fun get_version_name(): String {
        val package_info = this.applicationContext.packageManager.getPackageInfo(this.applicationContext.packageName,0)
        return package_info.versionName ?: ""
    }

    @Composable
    override fun Drawer(modifier: Modifier) { }

    override fun on_back_press_check(): Boolean {
        return true
    }

    @Composable
    fun FillRow(modifier: Modifier = Modifier, content: @Composable (RowScope.() -> Unit)) {
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }

    @Composable
    fun SectionLicense() {
        val context = LocalContext.current.find_activity() ?: return
        val stream = context.assets.open("LICENSE")
        val bytes = ByteArray(stream.available())
        stream.read(bytes)
        stream.close()
        Column {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
            )
            ProvideTextStyle(Typography.About.License) {
                SelectionContainer {
                    SText(
                        string_id = R.string.fira_sans_license_blurb,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            SelectionContainer {
                Text(
                    bytes.toString(charset = Charsets.UTF_8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun UrlManual(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val url_manual = stringResource(R.string.url_manual)
        SettingsColumn(
            modifier = modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = url_manual.toUri()
                    context.startActivity(intent)
                }
        ) {
            ProvideTextStyle(Typography.About.LinkTitle) {
                SText(
                    string_id = R.string.label_manual,
                    textAlign = TextAlign.Start
                )
            }
            ProvideTextStyle(Typography.About.LinkUrl) {
                SelectionContainer {
                    Text(
                        modifier = Modifier.padding(vertical = Dimensions.LinkUrlPadding),
                        text = stringResource(R.string.url_manual)
                    )
                }
            }
        }
    }

    @Composable
    fun UrlSource(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val url_source = stringResource(R.string.url_git)
        SettingsColumn(
            modifier = modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = url_source.toUri()
                    context.startActivity(intent)
                }
        ) {
            ProvideTextStyle(Typography.About.LinkTitle) {
                Text(stringResource(R.string.label_source_code))
            }
            ProvideTextStyle(Typography.About.LinkUrl) {
                SelectionContainer {
                    Text(
                        modifier = Modifier.padding(vertical = Dimensions.LinkUrlPadding),
                        text = stringResource(R.string.url_git)
                    )
                }
            }
        }
    }
    @Composable
    fun UrlIssueTracker(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val url_issues = stringResource(R.string.url_issues)
        SettingsColumn(
            modifier = modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = url_issues.toUri()
                    context.startActivity(intent)
                }
        ) {

            ProvideTextStyle(Typography.About.LinkTitle) {
                Text(stringResource(R.string.label_issues_location))
            }
            ProvideTextStyle(Typography.About.LinkUrl) {
                SelectionContainer {
                    Text(
                        modifier = Modifier.padding(vertical = Dimensions.LinkUrlPadding),
                        text = stringResource(R.string.url_issues)
                    )
                }
            }
        }
    }

    @Composable
    fun SupportEmail(modifier: Modifier = Modifier) {
        val support_email = stringResource(R.string.support_email)
        SettingsColumn(
            modifier = modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = "mailto:".toUri()
                    intent.putExtra(Intent.EXTRA_EMAIL, support_email)
                }
        ) {
            ProvideTextStyle(Typography.About.LinkTitle) {
                Text(stringResource(R.string.suggestions_description))
            }
            ProvideTextStyle(Typography.About.LinkUrl) {
                SelectionContainer {
                    Text(
                        modifier = Modifier.padding(vertical = Dimensions.LinkUrlPadding),
                        text = support_email
                    )
                }
            }
        }
    }

    @Composable
    fun SectionUrls() {
        FlowRow(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(Modifier.padding(6.dp)) {
                UrlManual()
            }
            Box(Modifier.padding(6.dp)) {
                UrlSource()
            }
            Box(Modifier.padding(6.dp)) {
                UrlIssueTracker()
            }
            Box(Modifier.padding(6.dp)) {
                SupportEmail()
            }
        }
    }
    @Composable
    fun Layout(modifier: Modifier = Modifier) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier.padding(Dimensions.AboutPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("v${this@ComponentActivityAbout.get_version_name()}")
                SectionUrls()
                SectionLicense()
            }
        }
    }

    @Composable
    override fun LayoutXLargePortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutXLargeLandscape(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutLargePortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutLargeLandscape(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutMediumPortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutMediumLandscape(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutSmallPortrait(modifier: Modifier) = Layout(modifier)
    @Composable
    override fun LayoutSmallLandscape(modifier: Modifier) = Layout(modifier)
}