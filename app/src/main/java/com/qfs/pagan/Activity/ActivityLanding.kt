package com.qfs.pagan.Activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.qfs.pagan.PaganActivity
import com.qfs.pagan.R
import com.qfs.pagan.databinding.ActivityLandingBinding
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivityLanding : PaganActivity() {
    private lateinit var _binding: ActivityLandingBinding
    private var result_launcher_save_crash_report = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val path = this.getExternalFilesDir(null).toString()
        val file = File("$path/bkp_crashreport.log")
        if (result.resultCode == RESULT_OK) {
            result?.data?.data?.also { uri ->
                val content = file.readText()
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(content.toByteArray())
                    file.delete()
                }
            }
        } else {
            file.delete()
        }
    }
    internal var result_launcher_set_project_directory = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result?.data?.also { result_data ->
                result_data.data?.also { uri  ->
                    val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    this.contentResolver.takePersistableUriPermission(uri, new_flags)
                    this.configuration.project_directory = uri
                    this.save_configuration()

                    this.get_project_manager().change_project_path(uri)
                    this.update_view_visibilities()
                }
            }
        }
    }

    internal var result_launcher_import_project = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result?.data?.data?.also { uri ->
                startActivity(
                    Intent(this, ActivityEditor::class.java).apply {
                        setData(uri)
                    }
                )
            }
        }
    }

    fun check_for_crash_report() {
        val path = this.getExternalFilesDir(null).toString()
        val file = File("$path/bkp_crashreport.log")
        if (file.isFile) {
            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setTitle(R.string.crash_report_save)
                .setMessage(R.string.crash_report_desc)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ ->
                    export_crash_report()
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.dlg_decline)) { dialog, _ ->
                    file.delete()
                    dialog.dismiss()
                }
                .show()
        }
    }

    fun export_crash_report() {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        //intent.type = MimeTypes.AUDIO_MIDI
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, "pagan.cr-${now.format(formatter)}.log")

        this.result_launcher_save_crash_report.launch(intent)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        this.check_for_crash_report()

        this._binding = ActivityLandingBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)

        val toolbar = this._binding.toolbar
        toolbar.background = null

        this.findViewById<View>(R.id.btnMostRecent).let { most_recent_button ->
            most_recent_button.setOnClickListener {
                startActivity(
                    Intent(this, ActivityEditor::class.java).apply {
                        putExtra("load_backup", true)
                    }
                )
            }
        }

        this.findViewById<View>(R.id.btnFrontSettings).setOnClickListener {
            startActivity(Intent(this, ActivitySettings::class.java))
        }

        this.findViewById<View>(R.id.btnFrontAbout).setOnClickListener {
            startActivity(Intent(this, ActivityAbout::class.java))
        }

        this.findViewById<View>(R.id.btnFrontNew).setOnClickListener {
            startActivity(
                Intent(this, ActivityEditor::class.java)
            )
        }

        this.findViewById<View>(R.id.btnFrontLoad).setOnClickListener {
            this.dialog_load_project { uri : Uri ->
                startActivity(
                    Intent(this, ActivityEditor::class.java).apply {
                        data = uri
                    }
                )
            }
        }

        this.findViewById<View>(R.id.btnFrontImport).setOnClickListener {
            this.result_launcher_import_project.launch(
                Intent().apply {
                    setAction(Intent.ACTION_GET_CONTENT)
                    setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                }
            )
        }

        this.findViewById<TextView>(R.id.tvFluidUrlLanding).setOnClickListener {
            val url = getString(R.string.url_fluid)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = url.toUri()
            startActivity(intent)
        }

        if (this.get_project_manager().has_external_storage_projects()) {
            AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
                .setMessage(getString(R.string.ucheck_move_projects_dialog))
                .setOnDismissListener {
                    this._popup_active = false
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    this.result_launcher_set_project_directory.launch(intent)
                }
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    fun update_view_visibilities() {
        this.findViewById<View>(R.id.btnMostRecent).let { most_recent_button ->
            most_recent_button.visibility = if (this.view_model.project_manager.has_backup_saved()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Show Space
            val btn_index = (most_recent_button.parent as ViewGroup).indexOfChild(most_recent_button)
            (most_recent_button.parent as ViewGroup).getChildAt(btn_index + 1)?.visibility = most_recent_button.visibility
        }

        if (this.has_projects_saved()) {
            this.findViewById<View>(R.id.btnFrontLoad).visibility = View.VISIBLE
            this.findViewById<Space>(R.id.space_load).visibility = View.VISIBLE
        } else {
            this.findViewById<View>(R.id.btnFrontLoad).visibility = View.GONE
            this.findViewById<Space>(R.id.space_load).visibility = View.GONE
        }

        this.findViewById<LinearLayout>(R.id.llSFWarningLanding).visibility = if (this.is_soundfont_available()) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    override fun on_soundfont_directory_set(uri: Uri) {
        this.get_project_manager().recache_project_list()
    }

    override fun onResume() {
        super.onResume()
        this.update_view_visibilities()
    }
}
