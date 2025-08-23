package com.qfs.pagan.Activity

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.MenuDialogEventHandler
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.PopupMenuRecyclerAdapter
import com.qfs.pagan.R
import com.qfs.pagan.projectmanager.ProjectManager
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

open class PaganActivity: AppCompatActivity() {
    companion object {
        const val EXTRA_ACTIVE_PROJECT = "active_project"
    }
    class PaganViewModel: ViewModel() {
        internal lateinit var project_manager: ProjectManager
    }

    val view_model: PaganViewModel by this.viewModels()

    internal lateinit var configuration_path: String
    lateinit var configuration: PaganConfiguration
    internal var _popup_active: Boolean = false
    private var _progress_bar: ConstraintLayout? = null

    internal var _set_soundfont_directory_intent_launcher =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.also { result_data ->
                    result_data.data?.also { uri  ->
                        val new_flags = result_data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        this.contentResolver.takePersistableUriPermission(uri, new_flags)
                        this.set_soundfont_directory(uri)
                        this.on_soundfont_directory_set(uri)
                    }
                }
            }
        }

    fun get_project_manager(): ProjectManager {
        return this.view_model.project_manager
    }

    // v1.7.7, Using user-specified soundfont directories. Check if externalfiles/soundfonts exists and move to new directory
    fun ucheck_move_soundfonts() {
        val uri = this.configuration.soundfont_directory ?: return
        try {
            File("${this.applicationContext.getExternalFilesDir(null)}/SoundFonts").let { old_directory ->
                DocumentFile.fromTreeUri(this, uri)?.let { new_directory ->
                    val buffer_size = 1024 * 1024
                    val file_list = old_directory.listFiles() ?: arrayOf<File>()
                    for (soundfont in file_list) {
                        val soundfont_name = soundfont.toUri().pathSegments.last().split("/").last()
                        new_directory.createFile("*/*", soundfont_name)?.let { new_file ->
                            val input_stream = soundfont.inputStream()
                            val output_stream = this.contentResolver.openOutputStream(new_file.uri, "wt")

                            val buffer = ByteArray(buffer_size)
                            while (true) {
                                val read_size = input_stream.read(buffer)
                                if (read_size == -1) {
                                    break
                                }
                                output_stream?.write(buffer, 0, read_size)
                            }

                            input_stream.close()
                            output_stream?.flush()
                            output_stream?.close()
                        }
                    }
                    old_directory.deleteRecursively()
                }
            }
        } catch (e: SecurityException) {
            return
        }
    }

    fun set_soundfont_directory(uri: Uri) {
        this.configuration.soundfont_directory = uri
        this.save_configuration()
        this.ucheck_move_soundfonts()
    }

    fun get_soundfont_directory(): DocumentFile {
        return if (this.configuration.soundfont_directory != null) {
            DocumentFile.fromTreeUri(this,this.configuration.soundfont_directory!!)!!
        } else {
            val soundfont_dir = this.applicationContext.getDir("SoundFonts", MODE_PRIVATE)
            if (!soundfont_dir.exists()) {
                soundfont_dir.mkdirs()
            }

            DocumentFile.fromFile(soundfont_dir)
        }
    }

    fun is_soundfont_available(): Boolean {
        val soundfont_dir = this.get_soundfont_directory()
        return soundfont_dir.listFiles().isNotEmpty()
    }


    fun has_projects_saved(): Boolean {
        val project_manager = this.get_project_manager()
        return project_manager.has_projects_saved()
    }

    internal fun save_configuration() {
        try {
            this.configuration.save(this.configuration_path)
        } catch (e: FileNotFoundException) {
            this.feedback_msg(this.resources.getString(R.string.config_file_not_found))
        }
    }

    private fun load_config() {
        this.configuration = try {
            PaganConfiguration.Companion.from_path(this.configuration_path)
        } catch (e: Exception) {
            PaganConfiguration()
        }
        this.requestedOrientation = this.configuration.force_orientation
        AppCompatDelegate.setDefaultNightMode(this.configuration.night_mode)

        this.view_model.project_manager = ProjectManager(this, this.configuration.project_directory)
    }

    private fun reload_config() {
        val new_configuration = try {
            PaganConfiguration.Companion.from_path(this.configuration_path)
        } catch (e: Exception) {
            PaganConfiguration()
        }

        if (new_configuration != this.configuration) {
            val original = this.configuration
            this.configuration = new_configuration
            this.on_paganconfig_change(original)
        }
    }

    open fun on_paganconfig_change(original: PaganConfiguration) {
        this.requestedOrientation = this.configuration.force_orientation
        AppCompatDelegate.setDefaultNightMode(this.configuration.night_mode)
        if (original.project_directory != this.configuration.project_directory) {
            this.get_project_manager().uri = this.configuration.project_directory
        }
    }

    fun is_debug_on(): Boolean {
        return this.packageName.contains("pagandev")
    }
    open fun on_crash() { }

    /**
     * Save text file in storage of a crash report.
     * To be copied and saved somewhere accessible on reload.
     */
    fun bkp_crash_report(e: Throwable) {
        val file = File("${this.dataDir}/bkp_crashreport.log")
        file.writeText(e.stackTraceToString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        this.set_latest_launched_version()

        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable ->
            Log.d("pagandebug", "$paramThrowable")
            this.bkp_crash_report(paramThrowable)
            this.on_crash()

            val ctx = this.applicationContext
            val pm = ctx.packageManager
            val intent = pm.getLaunchIntentForPackage(ctx.packageName) ?: return@setDefaultUncaughtExceptionHandler
            ctx.startActivity(
                Intent.makeRestartActivityTask(intent.component)
            )
            Runtime.getRuntime().exit(0)
        }

        // Default to empty path, it gets set in

        this.configuration_path = "${this.applicationContext.cacheDir}/pagan.cfg"

        this.ucheck_move_configuration()
        this.load_config()

    }

    // changed v.1.7.7
    fun ucheck_move_configuration() {
        try {
            val path = "${this.getExternalFilesDir(null)}/pagan.cfg"
            val file = File(path)
            if (file.exists()) {
                if (!File(this.configuration_path).exists()) {
                    file.copyTo(File(this.configuration_path))
                }
                file.deleteOnExit()
            }
        } catch (e: SecurityException) {
            return
        }
    }

    override fun onResume() {
        super.onResume()
        this.reload_config()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        this.recreate()
    }

    fun parse_file_name(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = this.contentResolver.query(uri, null, null, null, null)
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

    internal fun <T> dialog_popup_sortable_menu(title: String, options: List<Triple<T, Int?, String>>, default: T? = null, sort_options: List<Pair<String, (List<Triple<T, Int?, String>>) -> List<Triple<T, Int?, String>>>>, default_sort_option: Int = 0, event_handler: MenuDialogEventHandler<T>): AlertDialog? {
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
                this.window.decorView.rootView as ViewGroup,
                false
            )

        if (options.size > 1) {
            viewInflated.findViewById<View>(R.id.spinner_sort_options_wrapper).visibility = View.VISIBLE
        }
        val spinner = viewInflated.findViewById<Spinner>(R.id.spinner_sort_options)
        val sortable_labels = List(sort_options.size * 2) { i: Int ->
            if (i % 2 == 0) {
                sort_options[i / 2].first
            } else {
                this.getString(R.string.sorted_list_desc, sort_options[i / 2].first)
            }
        }

        val recycler = viewInflated.findViewById<RecyclerView>(R.id.rvOptions)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setOnDismissListener {
                this._popup_active = false
            }
            .setNegativeButton(this.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        val adapter = PopupMenuRecyclerAdapter<T>(
            recycler,
            sort_options[default_sort_option].second(options),
            default,
            event_handler
        )
        event_handler.dialog = dialog

        spinner.adapter = object: ArrayAdapter<String>(this, R.layout.spinner_list, sortable_labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).gravity = Gravity.END
                return view
            }
        }

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
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
            .setPositiveButton(this.getString(R.string.dlg_confirm)) { dialog, _ ->
                dialog.dismiss()
                callback()
            }
            .setNegativeButton(this.getString(R.string.dlg_decline)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    internal fun <T> dialog_popup_menu(title: String, options: List<Triple<T, Int?, String>>, default: T? = null, callback: (index: Int, value: T) -> Unit): AlertDialog? {
        return this.dialog_popup_menu(title, options, default, object : MenuDialogEventHandler<T>() {
            override fun on_submit(index: Int, value: T) {
                callback(index, value)
            }
        })
    }

    internal fun <T> dialog_popup_menu(title: String, options: List<Triple<T, Int?, String>>, default: T? = null, event_handler: MenuDialogEventHandler<T>): AlertDialog? {
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
                this.window.decorView.rootView as ViewGroup,
                false
            )

        val recycler = viewInflated.findViewById<RecyclerView>(R.id.rvOptions)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(title)
            .setView(viewInflated)
            .setOnDismissListener {
                this._popup_active = false
            }
            .setNegativeButton(this.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        val adapter = PopupMenuRecyclerAdapter<T>(recycler, options, default, event_handler)
        event_handler.dialog = dialog

        adapter.notifyDataSetChanged()

        return dialog
    }

    internal fun dialog_load_project(callback: (project_uri: Uri) -> Unit) {
        if (this._popup_active) {
            return
        }

        val tmp_project_list = this.get_project_manager().get_project_list()
        val project_list = List(tmp_project_list.size) { i: Int ->
            val (uri, path) = tmp_project_list[i]
            Triple(uri, null, path)
        }
        val sort_options = listOf(
            Pair(this.getString(R.string.sort_option_abc)) { original: List<Triple<Uri, Int?, String>> ->
                original.sortedBy { (_, _, label): Triple<Uri, Int?, String> ->
                    label.lowercase()
                }
            },
            Pair(this.getString(R.string.sort_option_date_modified)) { original: List<Triple<Uri, Int?, String>> ->
                original.sortedBy { (uri, _): Triple<Uri, Int?, String> ->
                    val f = DocumentFile.fromSingleUri(this, uri)
                    f?.lastModified()
                }
            },
            //Pair(getString(R.string.sort_option_date_created)) { original: List<Pair<String, String>> ->
            //    original.sortedBy { (path, _): Pair<String, String> ->
            //        val f = Path(path)
            //        val attributes: BasicFileAttributes = Files.readAttributes<BasicFileAttributes>(
            //            f,
            //            BasicFileAttributes::class.java
            //        )
            //        attributes.creationTime()
            //    }
            //}
        )

        this.dialog_popup_sortable_menu<Uri>(this.getString(R.string.menu_item_load_project), project_list, null, sort_options, 0, object: MenuDialogEventHandler<Uri>() {
            override fun on_submit(index: Int, value: Uri) {
                callback(value)
            }
            override fun on_long_click_item(index: Int, value: Uri): Boolean {
                val view: View = LayoutInflater.from(this@PaganActivity)
                    .inflate(
                        R.layout.dialog_project_info,
                        this@PaganActivity.window.decorView.rootView as ViewGroup,
                        false
                    )

                val opus_manager = OpusLayerBase()

                val input_stream = this@PaganActivity.contentResolver.openInputStream(value)
                val reader = BufferedReader(InputStreamReader(input_stream))
                val content = reader.readText().toByteArray(Charsets.UTF_8)
                reader.close()
                input_stream?.close()
                opus_manager.load(content)

                if (opus_manager.project_notes != null) {
                    view.findViewById<TextView>(R.id.project_notes)?.let {
                        it.text = opus_manager.project_notes!!
                        it.visibility = View.VISIBLE
                    }
                }

                view.findViewById<TextView>(R.id.project_size)?.let {
                    it.text = this@PaganActivity.getString(R.string.project_info_beat_count, opus_manager.length)
                }
                view.findViewById<TextView>(R.id.project_channel_count)?.let {
                    var count = opus_manager.channels.size
                    it.text = this@PaganActivity.getString(R.string.project_info_channel_count, count)
                }
                view.findViewById<TextView>(R.id.project_tempo)?.let {
                    it.text = this@PaganActivity.getString(
                        R.string.project_info_tempo, opus_manager.get_global_controller<OpusTempoEvent>(
                            EffectType.Tempo).initial_event.value.roundToInt())
                }
                view.findViewById<TextView>(R.id.project_last_modified)?.let {
                    DocumentFile.fromSingleUri(this@PaganActivity, value)?.let { f ->
                        val time = Date(f.lastModified())
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        it.text = formatter.format(time)
                    }
                }

                AlertDialog.Builder(this@PaganActivity, R.style.Theme_Pagan_Dialog)
                    .setTitle(opus_manager.project_name ?: this@PaganActivity.getString(R.string.untitled_opus))
                    .setView(view)
                    .setOnDismissListener { }
                    .setPositiveButton(R.string.details_load_project) { dialog, _ ->
                        dialog.dismiss()
                        this.do_submit(index, value)
                    }
                    .setNegativeButton(R.string.delete_project) { dialog, _ ->
                        this@PaganActivity.dialog_delete_project(value) {
                            dialog.dismiss()
                            this.dialog?.dismiss()
                        }
                    }
                    .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

                return true
            }
        })
    }

    internal fun dialog_delete_project(uri: Uri, deleted_callback: ((Uri) -> Unit)? = null) {
        val project_manager = this.get_project_manager()
        val title = project_manager.get_file_project_name(uri)
        AlertDialog.Builder(this, R.style.Theme_Pagan_Dialog)
            .setTitle(this.resources.getString(R.string.dlg_delete_title, title))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                this.get_project_manager().delete(uri)
                if (deleted_callback != null) {
                    deleted_callback(uri)
                }
                this.feedback_msg(this.resources.getString(R.string.feedback_delete, title))
                this@PaganActivity.on_project_delete(uri)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    open fun on_project_delete(uri: Uri) { }

    fun loading_reticle_show() {
        this.on_reticle_show()
        this.runOnUiThread {
            if (this._progress_bar == null) {
                this._progress_bar = LayoutInflater.from(this)
                    .inflate(
                        R.layout.loading_reticle,
                        this.window.decorView as ViewGroup,
                        false
                    ) as ConstraintLayout
            }

            this._progress_bar!!.isClickable = true
            val parent = this._progress_bar!!.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(this._progress_bar)
            }

            try {
                (this.window.decorView as ViewGroup).addView(this._progress_bar)
            } catch (e: UninitializedPropertyAccessException) {
                // pass
            }
        }
    }

    fun loading_reticle_hide() {
        this.runOnUiThread {
            this._progress_bar?.let {
                if (it.parent != null) {
                    (it.parent as ViewGroup).removeView(it)
                }
            }
        }
        this.on_reticle_hide()
    }
    open fun on_reticle_show() { }
    open fun on_reticle_hide() { }

    fun get_soundfont_uri(): Uri? {
        if (this.configuration.soundfont == null || this.configuration.soundfont_directory == null) {
            return null
        }

        var working_file = DocumentFile.fromTreeUri(this, this.configuration.soundfont_directory!!) ?: return null
        for (node in this.configuration.soundfont!!.split("/")) {
            working_file = working_file.findFile(node) ?: return null
        }

        if (!working_file.exists()) {
            return null
        }

        return working_file.uri
    }

    fun coerce_relative_soundfont_path(soundfont_uri: Uri): String? {
        if (this.configuration.soundfont_directory == null) {
            return null
        }

        val parent_segments = this.configuration.soundfont_directory!!.pathSegments
        val child_segments = soundfont_uri.pathSegments

        if (parent_segments.size >= child_segments.size || child_segments.subList(0, parent_segments.size) != parent_segments) {
            return null
        }

        val split_child_path = child_segments.last().split("/")
        val split_parent_path = parent_segments.last().split("/")
        val relative_path = split_child_path.subList(split_parent_path.size, split_child_path.size)

        return relative_path.joinToString("/")
    }

    open fun on_soundfont_directory_set(uri: Uri) {}
    open fun on_project_directory_set(uri: Uri) {}

    fun get_version_name(): String {
        val package_info = this.applicationContext.packageManager.getPackageInfo(this.applicationContext.packageName,0)
        return package_info.versionName ?: ""
    }

    internal fun set_latest_launched_version() {
        val file = File("${this.dataDir}/v")
        file.writeText(get_version_name())
    }

    internal fun get_latest_launched_version(): IntArray? {
        val file = File("${this.dataDir}/v")
        if (!file.exists()) return null

        val content = file.readText()
        val string_split = content.split(".")
        if (string_split.size != 3) return null

        return try {
            IntArray(string_split.size) { i: Int ->
                string_split[i].toInt()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}