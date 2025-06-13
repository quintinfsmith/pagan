package com.qfs.pagan.Activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.qfs.pagan.IntentFragmentToken
import com.qfs.pagan.MainActivity
import com.qfs.pagan.PaganActivity
import com.qfs.pagan.R
import com.qfs.pagan.databinding.ActivityLandingBinding
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class ActivityLanding : PaganActivity() {
    private lateinit var _binding: ActivityLandingBinding

    private var _crash_report_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val path = this.getExternalFilesDir(null).toString()
        val file = File("$path/bkp_crashreport.log")
        if (result.resultCode == Activity.RESULT_OK) {
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
        val name = "pagan.cr-${now.format(formatter)}.log"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        //intent.type = MimeTypes.AUDIO_MIDI
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, "$name")

        this._crash_report_intent_launcher.launch(intent)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        this.check_for_crash_report()

        this._binding = ActivityLandingBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)
        this._binding.root.setBackgroundColor(resources.getColor(R.color.main_bg))

        val toolbar = this._binding.toolbar
        toolbar.background = null

        this.findViewById<View>(R.id.btnMostRecent).let { most_recent_button ->
            val bkp_json_path = "${this.applicationInfo.dataDir}/.bkp.json"
            most_recent_button.setOnClickListener {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        setData(bkp_json_path.toUri())
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
            startActivity(Intent(this, MainActivity::class.java))
        }

        //  KLUDGE Lockout prevents accidentally double clicking. need a better general solution,
        // but right now i  think this is the only place this is a problem
        this.findViewById<View>(R.id.btnFrontLoad).setOnClickListener {
            this.dialog_load_project { path : String ->
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        data = path.toUri()
                    }
                )
            }
        }

        this.findViewById<View>(R.id.btnFrontImport).setOnClickListener {
            this.import_intent_launcher.launch(
                Intent().apply {
                    setAction(Intent.ACTION_GET_CONTENT)
                    setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
                }
            )
        }

        //val main = this.get_activity()
        // if (main.is_soundfont_available()) {
        //     this.binding.root.findViewById<LinearLayout>(R.id.llSFWarningLanding).visibility = View.INVISIBLE
        // }  else {
        //     this.binding.root.findViewById<TextView>(R.id.tvFluidUrlLanding).setOnClickListener {
        //         val url = getString(R.string.url_fluid)
        //         val intent = Intent(Intent.ACTION_VIEW)
        //         intent.data = Uri.parse(url)
        //         startActivity(intent)
        //     }
        // }
    }

    fun update_view_visibilities() {
        this.findViewById<View>(R.id.btnMostRecent).let { most_recent_button ->
            val bkp_json_path = "${this.applicationInfo.dataDir}/.bkp.json"
            if (!File(bkp_json_path).exists()) {
                most_recent_button.visibility = View.GONE
                val btn_index = (most_recent_button.parent as ViewGroup).indexOfChild(most_recent_button)
                // Show Space
                (most_recent_button.parent as ViewGroup).getChildAt(btn_index + 1)?.visibility = View.GONE
            }
        }

        if (this.has_projects_saved()) {
            this.findViewById<View>(R.id.btnFrontLoad).visibility = View.VISIBLE
            this.findViewById<Space>(R.id.space_load).visibility = View.VISIBLE
        } else {
            this.findViewById<View>(R.id.btnFrontLoad).visibility = View.GONE
            this.findViewById<Space>(R.id.space_load).visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        this.update_view_visibilities()
    }
}
