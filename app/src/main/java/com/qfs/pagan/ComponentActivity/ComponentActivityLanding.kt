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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.MenuDialogEventHandler
import com.qfs.pagan.R
import com.qfs.pagan.composable.SoundFontWarning
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
                this@ComponentActivityLanding.startActivity(Intent(this@ComponentActivityLanding, ComponentActivityEditor::class.java))
            }
        )
    }

    @Composable
    fun ButtonLoad(modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_load)) },
            onClick = {
                val project_list = this.view_model.project_manager?.get_project_list() ?: return@Button

                val items = mutableListOf<Triple<Uri, Int?, String>>()
                for ((uri, title) in project_list) {
                    items.add(Triple(uri, null, title))
                }

                val sort_options = listOf(
                    Pair(this.getString(R.string.sort_option_abc)) { original: List<Triple<Uri, Int?, String>> ->
                        original.sortedBy { item: Triple<Uri, Int?, String> -> item.third.lowercase() }
                    },
                    Pair(this.getString(R.string.sort_option_date_modified)) { original: List<Triple<Uri, Int?, String>> ->
                        original.sortedBy { (uri, _): Triple<Uri, Int?, String> ->
                            val f = DocumentFile.fromSingleUri(this, uri)
                            f?.lastModified()
                        }
                    },
                )

                this.dialog_popup_sortable_menu(this.getString(R.string.dialog_select_soundfont), items, null, sort_options, 0, object: MenuDialogEventHandler<Uri>() {
                    override fun on_submit(index: Int, value: Uri) {
                        this@ComponentActivityLanding.startActivity(
                            Intent(this@ComponentActivityLanding, ActivityEditor::class.java).apply {
                                this.data = value
                            }
                        )
                    }

                    override fun on_long_click_item(index: Int, value: Uri): Boolean {
                        // val view: View = LayoutInflater.from(this@PaganActivity)
                        //     .inflate(
                        //         R.layout.dialog_project_info,
                        //         this@PaganActivity.window.decorView.rootView as ViewGroup,
                        //         false
                        //     )

                        // val opus_manager = OpusLayerBase()

                        // val input_stream = this@PaganActivity.contentResolver.openInputStream(value)
                        // val reader = BufferedReader(InputStreamReader(input_stream))
                        // val content = reader.readText().toByteArray(Charsets.UTF_8)
                        // reader.close()
                        // input_stream?.close()
                        // opus_manager.load(content)

                        // if (opus_manager.project_notes != null) {
                        //     view.findViewById<TextView>(R.id.project_notes)?.let {
                        //         it.text = opus_manager.project_notes!!
                        //         it.visibility = View.VISIBLE
                        //     }
                        // }

                        // view.findViewById<TextView>(R.id.project_size)?.let {
                        //     it.text = this@PaganActivity.getString(R.string.project_info_beat_count, opus_manager.length)
                        // }
                        // view.findViewById<TextView>(R.id.project_channel_count)?.let {
                        //     var count = opus_manager.channels.size
                        //     it.text = this@PaganActivity.getString(R.string.project_info_channel_count, count)
                        // }
                        // view.findViewById<TextView>(R.id.project_tempo)?.let {
                        //     it.text = this@PaganActivity.getString(
                        //         R.string.project_info_tempo, opus_manager.get_global_controller<OpusTempoEvent>(
                        //             EffectType.Tempo).initial_event.value.roundToInt())
                        // }
                        // view.findViewById<TextView>(R.id.project_last_modified)?.let {
                        //     DocumentFile.fromSingleUri(this@PaganActivity, value)?.let { f ->
                        //         val time = Date(f.lastModified())
                        //         val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        //         it.text = formatter.format(time)
                        //     }
                        // }

                        // AlertDialog.Builder(this@PaganActivity, R.style.Theme_Pagan_Dialog)
                        //     .setTitle(opus_manager.project_name ?: this@PaganActivity.getString(R.string.untitled_opus))
                        //     .setView(view)
                        //     .setOnDismissListener { }
                        //     .setPositiveButton(R.string.details_load_project) { dialog, _ ->
                        //         dialog.dismiss()
                        //         this.do_submit(index, value)
                        //     }
                        //     .setNegativeButton(R.string.delete_project) { dialog, _ ->
                        //         this@PaganActivity.dialog_delete_project(value) {
                        //             dialog.dismiss()
                        //             this.dialog?.dismiss()
                        //         }
                        //     }
                        //     .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                        //         dialog.dismiss()
                        //     }
                        //     .show()

                        return super.on_long_click_item(index, value)
                    }
                })

            }
        )
    }

    @Composable
    fun ButtonImport(modifier: Modifier = Modifier) {
        Button(
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
        Button(
            modifier = modifier.fillMaxWidth(),
            content = { Text(stringResource(R.string.btn_landing_settings)) },
            onClick = {
                this@ComponentActivityLanding.startActivity(Intent(this@ComponentActivityLanding, ComponentActivitySettings::class.java))
            }
        )
    }

    @Composable
    fun ButtonAbout(modifier: Modifier = Modifier) {
        Button(
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
}