package com.qfs.pagan.ComponentActivity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.Activity.PaganActivity.Companion.EXTRA_ACTIVE_PROJECT
import com.qfs.pagan.MenuDialogEventHandler
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.SortableMenu
import com.qfs.pagan.composable.SoundFontWarning
import com.qfs.pagan.composable.button.BetterButton
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.Pair
import kotlin.math.roundToInt

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
    override fun TopBar(modifier: Modifier) {
        // Row {
        //     Text(
        //         modifier = Modifier.weight(1F),
        //         textAlign = TextAlign.Center,
        //         text = stringResource(R.string.app_name)
        //     )
        // }
    }

    @Composable
    fun LayoutMenu() {
        Column( verticalArrangement = Arrangement.Center ) {
            Row { ButtonRecent() }
            Row { ButtonNew() }
            if (this@ComponentActivityLanding.view_model.has_saved_project.value) {
                Row { ButtonLoad() }
            }
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
                if (this@ComponentActivityLanding.view_model.has_saved_project.value) {
                    ButtonLoad(
                        Modifier
                            .padding(start = 8.dp)
                            .weight(1F)
                    )
                }
            }
            Row { ButtonImport() }
            Row {
                ButtonSettings(Modifier.weight(1F))
                ButtonAbout(
                    Modifier
                        .padding(start = 8.dp)
                        .weight(1F)
                )
            }
        }
    }

    @Composable
    fun ButtonRecent(modifier: Modifier = Modifier) {
        BetterButton(
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
        BetterButton(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_new)) },
            onClick = {
                this@ComponentActivityLanding.startActivity(Intent(this@ComponentActivityLanding, ComponentActivityEditor::class.java))
            }
        )
    }

    @Composable
    fun ButtonLoad(modifier: Modifier = Modifier) {
        BetterButton(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_load)) },
            onClick = {
                this.load_menu_dialog {
                    this@ComponentActivityLanding.startActivity(
                        Intent(this@ComponentActivityLanding, ActivityEditor::class.java).apply {
                            this.data = it
                        }
                    )
                }
            }
        )
    }


    @Composable
    fun ButtonImport(modifier: Modifier = Modifier) {
        BetterButton(
            modifier = modifier.fillMaxWidth(),
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
    fun ButtonSettings(modifier: Modifier = Modifier) {
        BetterButton(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_settings)) },
            onClick = {
                this@ComponentActivityLanding.result_launcher_settings.launch(
                    Intent(this@ComponentActivityLanding, ComponentActivitySettings::class.java)
                )
            }
        )
    }

    @Composable
    fun ButtonAbout(modifier: Modifier = Modifier) {
        BetterButton(
            modifier = modifier.fillMaxWidth(),
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
                    Row { SoundFontWarning() }
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
                    Row { SoundFontWarning() }
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
                    Row { SoundFontWarning() }
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
                    Row { SoundFontWarning() }
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
                    Row(Modifier.fillMaxWidth()) {
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
                SoundFontWarning()
            }
            LayoutMenuCompact()
        }
    }

}
