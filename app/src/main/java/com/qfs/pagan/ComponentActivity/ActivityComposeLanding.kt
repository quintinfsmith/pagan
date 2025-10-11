package com.qfs.pagan.ComponentActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qfs.pagan.Activity.ActivityAbout
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.composable.SoundFontWarning

class ActivityComposeLanding: PaganComponentActivity() {
    internal var result_launcher_import_project =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
                    this.startActivity(
                        Intent(this, ActivityEditor::class.java).apply {
                            this.setData(uri)
                        }
                    )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    fun LayoutMenu() {
        Column( verticalArrangement = Arrangement.Center ) {
            Row { ButtonRecent() }
            Row { ButtonNew() }
            Row { ButtonLoad() }
            Row { ButtonImport() }
            Row { ButtonSettings() }
            Row { ButtonAbout() }
        }
    }

    @Composable
    fun LayoutMenuCompact() {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Row { ButtonRecent() }
            Row {
                ButtonNew(Modifier.weight(1F))
                ButtonLoad(Modifier
                    .padding(start=8.dp)
                    .weight(1F))
            }
            Row { ButtonImport() }
            Row {
                ButtonSettings(Modifier.weight(1F))
                ButtonAbout(
                    Modifier
                        .padding(start=8.dp)
                        .weight(1F)
                )
            }
        }
    }

    @Composable
    fun ButtonRecent(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_most_recent)) },
            onClick = {
                // this@ActivityComposeLanding.startActivity(
                //     Intent(this, ActivityEditor::class.java).apply {
                //         this.putExtra("load_backup", true)
                //     }
                // )
            }
        )
    }

    @Composable
    fun ButtonNew(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_new)) },
            onClick = {
                this@ActivityComposeLanding.startActivity(Intent(this@ActivityComposeLanding, ActivityEditor::class.java))
            }
        )
    }

    @Composable
    fun ButtonLoad(modifier: Modifier = Modifier) {
        var show_load_dialog = remember { mutableStateOf(false) }
        dialog(show_load_dialog)

        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_load)) },
            onClick = {
                show_load_dialog.value = true
                // this.dialog_load_project { uri : Uri ->
                //     this.loading_reticle_show()
                //     this.startActivity(
                //         Intent(this, ActivityEditor::class.java).apply {
                //             this.data = uri
                //         }
                //     )
                // }
            }
        )
    }

    @Composable
    fun ButtonImport(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_import)) },
            onClick = {
                // this.result_launcher_import_project.launch(
                //     Intent().apply {
                //         this.setAction(Intent.ACTION_GET_CONTENT)
                //         this.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                //     }
                // )
            }
        )
    }


    @Composable
    fun ButtonSettings(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_settings)) },
            onClick = {
                this@ActivityComposeLanding.startActivity(Intent(this@ActivityComposeLanding, ActivityComposerSettings::class.java))
            }
        )
    }

    @Composable
    fun ButtonAbout(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_about)) },
            onClick = {
                this@ActivityComposeLanding.startActivity(Intent(this@ActivityComposeLanding, ActivityAbout::class.java))
            }
        )
    }

    @Composable
    override fun LayoutXLargePortrait() {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier.width(SIZE_L.first),
                verticalArrangement = Arrangement.Center
            ) {
                Row { SoundFontWarning() }
                Row { LayoutMenu() }
            }
        }
    }

    @Composable
    override fun LayoutLargePortrait() {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(SIZE_M.first),
                verticalArrangement = Arrangement.Center
            ) {
                Row { SoundFontWarning() }
                Row { LayoutMenu() }
            }
        }
    }

    @Composable
    override fun LayoutMediumPortrait() {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                modifier = Modifier.width(SIZE_M.first),
                verticalArrangement = Arrangement.Center
            ) {
                Row { SoundFontWarning() }
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
                Row { SoundFontWarning() }
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
                Row(Modifier.fillMaxWidth()) {
                    SoundFontWarning()
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
           SoundFontWarning()
           LayoutMenuCompact()
        }
    }


    @Composable
    fun dialog(show: MutableState<Boolean>, positive:(() -> Unit) = {}, negative: (() -> Unit) = {}, neutral: (() -> Unit) = {}) {
        if (show.value) {
            Dialog(onDismissRequest = {
                show.value = false
                neutral
            }) {
                // Draw a rectangle shape with rounded corners inside the dialog
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(375.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "This is a dialog with buttons and an image.",
                            modifier = Modifier.padding(16.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = {
                                    show.value = false
                                    neutral
                                },
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Text("Dismiss")
                            }
                            TextButton(
                                onClick = {
                                    show.value = false
                                    positive
                                },
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }
    }
}