package com.qfs.pagan

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path

open class PaganActivity: AppCompatActivity() {
    internal lateinit var configuration_path: String
    lateinit var configuration: PaganConfiguration
    internal var _popup_active: Boolean = false
    internal lateinit var project_manager: ProjectManager

    internal var import_intent_launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data?.also { uri ->
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        setData(uri)
                    }
                )
            }
        }
    }

    fun get_soundfont_directory(): File {
        val soundfont_dir = File("${this.getExternalFilesDir(null)}/SoundFonts")
        if (!soundfont_dir.exists()) {
            soundfont_dir.mkdirs()
        }

        return soundfont_dir
    }

    fun is_soundfont_available(): Boolean {
        val soundfont_dir = this.get_soundfont_directory()
        return soundfont_dir.listFiles()?.isNotEmpty() == true
    }


    fun has_projects_saved(): Boolean {
        return this.project_manager.has_projects_saved()
    }

    internal fun save_configuration() {
        try {
            this.configuration.save(this.configuration_path)
        } catch (e: FileNotFoundException) {
            this.feedback_msg(resources.getString(R.string.config_file_not_found))
        }
    }

    private fun load_config() {
        this.configuration = try {
            PaganConfiguration.from_path(this.configuration_path)
        } catch (e: Exception) {
            PaganConfiguration()
        }
        this.requestedOrientation = this.configuration.force_orientation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.configuration_path = "${this.getExternalFilesDir(null)}/pagan.cfg"
        this.load_config()

        this.project_manager = ProjectManager(this)
        // Move files from applicationInfo.data to externalfilesdir (pre v1.1.2 location)
        val old_projects_dir = File("${applicationInfo.dataDir}/projects")
        if (old_projects_dir.isDirectory) {
            for (f in old_projects_dir.listFiles()!!) {
                val new_file_name = this.project_manager.get_new_path()
                f.copyTo(File(new_file_name))
            }
            old_projects_dir.deleteRecursively()
        }
    }

    fun parse_file_name(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val ci = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ci >= 0) {
                        result = cursor.getString(ci)
                    }
                }
                cursor.close()
            }
        }

        if (result == null && uri.path is String) {
            result = uri.path!!
            result = result.substring(result.lastIndexOf("/") + 1)
        }
        return result
    }

    fun feedback_msg(msg: String) {
        this.runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    internal fun <T> dialog_popup_sortable_menu(title: String, options: List<Pair<T, String>>, default: T? = null, sort_options: List<Pair<String, (List<Pair<T, String>>) -> List<Pair<T, String>>>>, default_sort_option: Int = 0, callback: (index: Int, value: T) -> Unit): AlertDialog? {
        if (this._popup_active) {
            return null
        }
        if (options.isEmpty()) {
            return null
        }

        this._popup_active = true
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_menu,
                window.decorView.rootView as ViewGroup,
                false
            )

        if (options.size > 1) {
            viewInflated.findViewById<View>(R.id.spinner_sort_options_wrapper).visibility =
                View.VISIBLE
        }
        val spinner = viewInflated.findViewById<Spinner>(R.id.spinner_sort_options)
        val sortable_labels = List(sort_options.size * 2) { i: Int ->
            if (i % 2 == 0) {
                sort_options[i / 2].first
            } else {
                getString(R.string.sorted_list_desc, sort_options[i / 2].first)
            }
        }

        val recycler = viewInflated.findViewById<RecyclerView>(R.id.rvOptions)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setOnDismissListener {
                this._popup_active = false
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        val adapter = PopupMenuRecyclerAdapter<T>(recycler, sort_options[default_sort_option].second(options), default) { index: Int, value: T ->
            dialog.dismiss()
            callback(index, value)
        }

        spinner.adapter = ArrayAdapter<String>(this, R.layout.spinner_list, sortable_labels)
        spinner.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                adapter.set_items(
                    if (position % 2 == 1) {
                        sort_options[position / 2].second(options).asReversed()
                    } else {
                        sort_options[position / 2].second(options)
                    }
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        adapter.notifyDataSetChanged()

        return dialog
    }

    fun dialog_confirm(title: String, callback: () -> Unit) {
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dlg_confirm)) { dialog, _ ->
                dialog.dismiss()
                callback()
            }
            .setNegativeButton(getString(R.string.dlg_decline)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    internal fun <T> dialog_popup_menu(title: String, options: List<Pair<T, String>>, default: T? = null, callback: (index: Int, value: T) -> Unit): AlertDialog? {
        if (this._popup_active) {
            return null
        }

        if (options.isEmpty()) {
            return null
        }

        this._popup_active = true
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_menu,
                window.decorView.rootView as ViewGroup,
                false
            )

        val recycler = viewInflated.findViewById<RecyclerView>(R.id.rvOptions)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setOnDismissListener {
                this._popup_active = false
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        val adapter = PopupMenuRecyclerAdapter<T>(recycler, options, default) { index: Int, value: T ->
            dialog.dismiss()
            callback(index, value)
        }

        adapter.notifyDataSetChanged()

        return dialog
    }

    internal fun dialog_load_project(callback: (project_path: String) -> Unit) {
        if (this._popup_active) {
            return
        }

        val project_list = this.project_manager.get_project_list()
        val sort_options = listOf(
            Pair(getString(R.string.sort_option_abc)) { original: List<Pair<String, String>> ->
                original.sortedBy { (_, label): Pair<String, String> -> label }
            },
            Pair(getString(R.string.sort_option_date_modified)) { original: List<Pair<String, String>> ->
                original.sortedBy { (path, _): Pair<String, String> ->
                    val f = File(path)
                    f.lastModified()
                }
            },
            Pair(getString(R.string.sort_option_date_created)) { original: List<Pair<String, String>> ->
                original.sortedBy { (path, _): Pair<String, String> ->
                    val f = Path(path)
                    val attributes: BasicFileAttributes = Files.readAttributes<BasicFileAttributes>(
                        f,
                        BasicFileAttributes::class.java
                    )
                    attributes.creationTime()
                }
            }
        )

        this.dialog_popup_sortable_menu<String>(
            getString(R.string.menu_item_load_project),
            project_list,
            null,
            sort_options,
            0
        ) { _: Int, path: String ->
            callback(path)
        }
    }
}