package com.qfs.pagan.ComponentActivity

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import com.qfs.pagan.find_activity

class ComponentActivityAbout: PaganComponentActivity() {
    @Composable
    fun FillRow(content: @Composable (RowScope.() -> Unit)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
    @Composable
    fun SectionLicense() {
        val stream = this.assets.open("LICENSE")
        val bytes = ByteArray(stream.available())
        stream.read(bytes)
        stream.close()

        Column {
            FillRow {
                SelectionContainer {
                    Text(stringResource(R.string.fira_sans_license_blurb))
                }
            }
            FillRow { HorizontalDivider(thickness = 1.dp) }
            FillRow {
                SelectionContainer {
                    Text(
                        bytes.toString(charset = Charsets.UTF_8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    fun SectionUrls() {
        val context = LocalContext.current.find_activity() ?: return
        val url_manual = stringResource(R.string.url_manual)
        val url_source = stringResource(R.string.url_git)
        val url_issues = stringResource(R.string.url_issues)
        val support_email = stringResource(R.string.support_email)
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FillRow {
                Column(
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = url_manual.toUri()
                        context.startActivity(intent)
                    }
                ) {
                    FillRow { Text(stringResource(R.string.label_manual)) }
                    FillRow {
                        SelectionContainer {
                            Text(stringResource(R.string.url_manual))
                        }
                    }
                }
            }
            FillRow {
                Column(
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = url_source.toUri()
                        context.startActivity(intent)
                    }
                ) {
                    FillRow { Text(stringResource(R.string.label_source_code)) }
                    FillRow {
                        SelectionContainer {
                            Text(stringResource(R.string.url_git))
                        }
                    }
                }
            }
            FillRow {
                Column(
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = url_issues.toUri()
                        context.startActivity(intent)
                    }
                ) {
                    FillRow { Text(stringResource(R.string.label_issues_location)) }
                    FillRow {
                        SelectionContainer {
                            Text(stringResource(R.string.url_issues))
                        }
                    }
                }
            }
            FillRow { HorizontalDivider(thickness = 1.dp) }
            FillRow {
                Column(
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = "mailto:".toUri()
                        intent.putExtra(Intent.EXTRA_EMAIL, support_email)
                    }
                ) {
                    FillRow { Text(stringResource(R.string.suggestions_description)) }
                    FillRow {
                        SelectionContainer {
                            Text(support_email)
                        }
                    }
                }
            }
        }

    }

    @Composable
    override fun LayoutXLargePortrait() = LayoutLargePortrait()

    @Composable
    override fun LayoutLargePortrait() {
        FillRow {
            Column(Modifier.fillMaxWidth().weight(1F)) { }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .width(SIZE_L.second).weight(1F)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FillRow { SectionUrls() }
                FillRow { HorizontalDivider(thickness = 1.dp) }
                FillRow { SectionLicense() }
            }
            Column(Modifier.fillMaxWidth().weight(1F)) { }
        }
    }

    @Composable
    override fun LayoutMediumPortrait() {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FillRow { SectionUrls() }
            FillRow { HorizontalDivider(thickness = 1.dp) }
            FillRow { SectionLicense() }
        }
    }

    @Composable
    override fun LayoutSmallPortrait() = LayoutMediumPortrait()

    @Composable
    override fun LayoutXLargeLandscape() = LayoutLargeLandscape()

    @Composable
    override fun LayoutLargeLandscape() {
        FillRow {
            Column(Modifier.fillMaxWidth().weight(1F)) { }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .width(SIZE_L.first).weight(1F)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FillRow { SectionUrls() }
                FillRow { HorizontalDivider(thickness = 1.dp) }
                FillRow { SectionLicense() }
            }
            Column(Modifier.fillMaxWidth().weight(1F)) { }
        }
    }

    @Composable
    override fun LayoutMediumLandscape() = LayoutMediumPortrait()

    @Composable
    override fun LayoutSmallLandscape() = LayoutMediumPortrait()
}