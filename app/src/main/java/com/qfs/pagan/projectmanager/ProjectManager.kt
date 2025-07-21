package com.qfs.pagan.projectmanager

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.json.InvalidJSON
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONList
import com.qfs.json.JSONParser
import com.qfs.json.JSONString
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.jsoninterfaces.OpusManagerJSONInterface
import com.qfs.pagan.structure.opusmanager.OpusLayerBase
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Handles project file management. ie caching files, generating file names, etc
 */
class ProjectManager(val context: Context, var uri: Uri?) {
    // Where the cached list of projects is stored
    private val _cache_path = "${this.context.cacheDir}/project_list.json"
    // Where backup data is stored
    private val _bkp_path = "${this.context.cacheDir}/.bkp.json"
    // Where the uri of the backed up data is stored, if it has a uri associated.
    private val _bkp_path_path = "${this.context.cacheDir}/.bkp_path"

    init {
        this.recache_project_list()
    }

    /**
     * Move files from [old_uri] to the ProjectManger's current [uri],
     * return the new uri associated with given [active_project_uri] if it was moved
     **/
    fun move_old_projects_directory(old_uri: Uri, active_project_uri: Uri? = null): Uri? {
        val bkp_path_file = File(this._bkp_path_path)
        val bkp_uri = if (!bkp_path_file.exists()) {
            null
        } else {
            bkp_path_file.readText().toUri()
        }

        val old_directory = DocumentFile.fromTreeUri(this.context, old_uri) ?: return null
        if (!old_directory.isDirectory || old_uri == this.uri) {
            return null
        }

        var output: Uri? = null
        val buffer_size = 1024 * 1024
        for (project in old_directory.listFiles()) {
            if (!project.isFile) {
                continue
            }
            // Use new path instead of copying file name to avoid collisions
            val new_uri = this.get_new_file_uri() ?: continue
            val output_stream = this.context.contentResolver.openOutputStream(new_uri, "wt")
            val input_stream = this.context.contentResolver.openInputStream(project.uri)

            when (project.uri) {
                active_project_uri -> { output = new_uri }
                bkp_uri -> { bkp_path_file.writeText(new_uri.toString()) }
                else -> { }
            }

            val buffer = ByteArray(buffer_size)
            while (true) {
                val read_size = input_stream?.read(buffer) ?: break
                if (read_size == -1) {
                    break
                }
                output_stream?.write(buffer, 0, read_size)
            }

            input_stream?.close()
            output_stream?.flush()
            output_stream?.close()

            project.delete()
        }

        this.recache_project_list()
        return output
    }

    /**
     * Change the where projects are saved to [new_uri]. Also moves existing projects.
     * [active_project_uri] is a uri that may be changed by the move and the returned Uri
     * is the altered version of that, if it is altered.
     */
    fun change_project_path(new_uri: Uri, active_project_uri: Uri? = null): Uri? {
        val new_directory = DocumentFile.fromTreeUri(this.context, new_uri)
        if (new_directory == null || !new_directory.isDirectory) {
            throw InvalidDirectoryException(new_uri)
        }

        val old_uri = this.uri
        this.uri = new_uri

        this.ucheck_update_move_project_files(active_project_uri)

        val output = if (old_uri != null) {
            this.move_old_projects_directory(old_uri, active_project_uri)
        } else {
            null
        }

        this.recache_project_list()
        return output
    }

    /**
     * Check if [uri] is a uri contained by the ProjectManager's Uri.
     */
    fun contains(uri: Uri): Boolean {
        if (this.uri == null) {
            return false
        }

        val parent_segments = this.uri!!.pathSegments.last().split("/")
        val child_segments = uri.pathSegments.last().split("/")

        val output = uri.authority == this.uri?.authority && child_segments.size > parent_segments.size  && child_segments.subList(0, parent_segments.size) == parent_segments
        return output
    }

    /**
     * Delete a project at [uri].
     */
    fun delete(uri: Uri) {
        val document_file = DocumentFile.fromSingleUri(this.context, uri) ?: return
        if (document_file.isFile) {
            document_file.delete()
        }

        this._untrack_uri(uri)
    }

    /**
     * Store [opus_manager] at [uri]
     */
    fun save(opus_manager: OpusLayerBase, uri: Uri?): Uri {
        val active_project_uri = uri ?: this.get_new_file_uri() ?: throw Exception("Failed To create new file")
        // Untrack then track in order to update the project title in the cache
        active_project_uri.let {
            val content = opus_manager.to_json()

            this.context.contentResolver.openOutputStream(it, "wt")?.let { output_stream ->
                output_stream.write(content.to_string().toByteArray(Charsets.UTF_8))
                output_stream.flush()
                output_stream.close()
            }

            this._untrack_uri(it)
            this._track_path(it)
        }

        return active_project_uri
    }

    /**
     * Generate a new uri that will *definitely not* collide with existing project files.
     */
    fun get_new_file_uri(): Uri? {
        if (this.uri == null) {
            throw PathNotSetException()
        }
        val working_directory = DocumentFile.fromTreeUri(this.context, this.uri!!)!!
        var i = 0
        while (working_directory.findFile("opus_$i.json") != null) {
            i += 1
        }

        return working_directory.createFile("application/json", "opus_$i.json")?.uri
    }

    /**
     * Check if there are any projects saved in the ProjectManager's Uri
     */
    fun has_projects_saved(): Boolean {
        if (this.uri == null) {
            return false
        }

        val working_directory = DocumentFile.fromTreeUri(this.context, this.uri!!) ?: return false
        if (!working_directory.isDirectory) {
            return false
        }

        return working_directory.listFiles().isNotEmpty()
    }

    /**
     * Generate a default project name.
     */
    private fun generate_file_project_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return this.context.getString(R.string.untitled_op, now.format(formatter))
    }

    /**
     * Temporary function. Check if the user has projects saved in the ExternalFilesDir where
     * projects were stored before v1.7.7.
     */
    fun has_external_storage_projects(): Boolean {
        // V1.7.7, moved project storage out of ExternalFilesDir
        return try {
            val external_path = this.context.getExternalFilesDir(null) ?: return false
            val old_directory = File("$external_path/projects")
            old_directory.isDirectory && old_directory.listFiles()?.isNotEmpty() ?: false
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Temporary Function.
     * Move projects from external storage (where projects were stored before v1.7.7) to
     * the current uri
     */
    fun ucheck_recache_external_storage_projects(): Boolean {
        // V1.7.7, moved project storage out of ExternalFilesDir
        val old_directory = try {
            val external_path = this.context.getExternalFilesDir(null) ?: return false
            File("$external_path/projects")
        } catch (_: SecurityException) {
            return false
        }
        if (!old_directory.isDirectory || old_directory.listFiles()?.isEmpty() ?: false) {
            return false
        }

        val working_directory = DocumentFile.fromFile(old_directory)

        val project_list = JSONList()
        for (json_file in working_directory.listFiles()) {
            val project_name = try {
                this.get_file_project_name(json_file.uri) ?: this.generate_file_project_name()
            } catch (_: Exception) {
                continue
            }

            project_list.add(
                JSONList(
                    JSONString(json_file.uri.toString()),
                    JSONString(project_name)
                )
            )
        }

        project_list.sort_by { it ->
            (it as JSONList).get_string(1)
        }

        val file = File(this._cache_path)
        file.writeText(project_list.to_string())
        return true
    }

    /**
     * Read through and cache all of the projects with their titles.
     * Reduces lag when opening "Load Project".
     */
    fun recache_project_list() {
        val file = File(this._cache_path)
        if (this.ucheck_recache_external_storage_projects()) {
            return
        }

        if (!this.has_projects_saved()) {
            if (file.exists()) {
                file.delete()
            }
            return
        }

        val working_directory = DocumentFile.fromTreeUri(this.context, this.uri!!) ?: return

        val project_list = JSONList()
        for (json_file in working_directory.listFiles()) {
            val project_name = try {
                this.get_file_project_name(json_file.uri) ?: this.generate_file_project_name()
            } catch (_: Exception) {
                continue
            }

            project_list.add(JSONList(
                JSONString(json_file.uri.toString()),
                JSONString(project_name)
            ))
        }

        project_list.sort_by { it ->
            (it as JSONList).get_string(1)
        }

        file.writeText(project_list.to_string())
    }

    /**
     * Get the stored project's title given the projects location at [uri]
     */
    fun get_file_project_name(uri: Uri): String? {
        val input_stream = this.context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText()

        reader.close()

        val json_obj = JSONParser.Companion.parse<JSONHashMap>(content) ?: return null
        val version = OpusManagerJSONInterface.Companion.detect_version(json_obj)
        return when (version) {
            0, 1, 2 -> json_obj.get_string("name")
            else -> {
                json_obj.get_hashmap("d").get_string("title", this.generate_file_project_name())
            }
        }
    }

    /**
     * Get a List of Uris paired with their Projects' titles.
     */
    fun get_project_list(): List<Pair<Uri, String>> {
        val json_list = this.get_json_project_list()
        return List(json_list.size) { i: Int ->
            val entry = json_list.get_list(i)
            Pair(
                entry.get_string(0).toUri(),
                entry.get_string(1)
            )
        }
    }

    /**
     * Same as get_project_list, but a json version.
     */
    fun get_json_project_list(): JSONList {
        if (!File(this._cache_path).exists()) {
            this.recache_project_list()
        }

        val file = File(this._cache_path)
        if (!file.exists()) {
            return JSONList()
        }

        var string_content = file.readText(Charsets.UTF_8)

        return try {
            JSONParser.parse(string_content)!!
        } catch (_: InvalidJSON) {
            File(this._cache_path).delete()
            this.recache_project_list()
            string_content = File(this._cache_path).readText(Charsets.UTF_8)
            JSONParser.parse(string_content)!!
        }

    }

    /**
     * Add [uri] to cached list of projects.
     */
    private fun _track_path(uri: Uri) {
        val project_list = this.get_json_project_list()
        var is_tracking = false

        for (i in 0 until project_list.size) {
            if (project_list.get_list(i).get_string(0).toUri() == uri) {
                is_tracking = true
                break
            }
        }

        if (is_tracking) {
            return
        }

        val project_name = this.get_file_project_name(uri) ?: return
        project_list.add(
            JSONList(
                JSONString(uri.toString()),
                JSONString(project_name)
            )
        )

        project_list.sort_by { it ->
            (it as JSONList).get_string(1)
        }

        val file = File(this._cache_path)
        file.writeText(project_list.to_string())
    }

    /**
     * Remove [uri] from cached list of projects
     */
    private fun _untrack_uri(uri: Uri) {
        val project_list = this.get_json_project_list()
        var index_to_pop = 0
        for (i in 0 until project_list.size) {
            if (project_list.get_list(i).get_string(0).toUri() == uri) {
                break
            }
            index_to_pop += 1
        }

        if (index_to_pop == project_list.size) {
            return
        }

        project_list.remove_at(index_to_pop)
        project_list.sort_by { it ->
            (it as JSONList).get_string(1)
        }

        val file = File(this._cache_path)
        file.writeText(project_list.to_string())
    }

    // v1.7.7: Using custom projects directories rather than forcing external directory
    fun ucheck_update_move_project_files(active_uri: Uri? = null): Uri? {
        val bkp_path_file = File(this._bkp_path_path)
        val bkp_uri = if (!bkp_path_file.exists()) {
            null
        } else {
            bkp_path_file.readText().toUri()
        }


        var output: Uri? = null
        try {
            this.context.getExternalFilesDir(null)?.let {
                val old_directory = File("$it/projects")
                if (!old_directory.isDirectory) {
                    return null
                }

                val buffer_size = 1024 * 1024
                for (project in old_directory.listFiles() ?: arrayOf()) {
                    // Use new path instead of copying file name to avoid collisions
                    val new_uri = this.get_new_file_uri() ?: continue
                    val output_stream = this.context.contentResolver.openOutputStream(new_uri, "wt")
                    val input_stream = project.inputStream()

                    // Necessary for when user goes into settings, changes project directory, then leaves the app for a while
                    // and the context is lost. When the project is loaded from bkp, the reloaded active project would OTHERWISE be incorrect
                    when (project.toUri()) {
                        active_uri -> {
                            output = new_uri
                        }

                        bkp_uri -> {
                            bkp_path_file.writeText(new_uri.toString())
                        }

                        else -> {}
                    }

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

                this.recache_project_list()
                old_directory.deleteRecursively()
            }
        } catch (_: SecurityException) {
            // pass
        }

        return output
    }

    /**
     * Save [opus_manager] to a known path.
     * [uri] is the Uri the user would otherwise save [opus_manager] to.
     */
    fun save_to_backup(opus_manager: OpusLayerInterface, uri: Uri?) {
        val path_file = File(this._bkp_path_path)
        if (uri == null) {
            if (path_file.exists()) {
                path_file.delete()
            }
        } else {
            path_file.writeText(uri.toString())
        }

        File(this._bkp_path).writeText(opus_manager.to_json().to_string())
    }

    /**
     * Retrieve the backed up project as a byte array and get the uri of where it is to be saved (should the uri exist).
     */
    fun read_backup(): Pair<Uri?, ByteArray> {
        val path_file = File(this._bkp_path_path)
        return Pair(
            if (path_file.exists()) {
                val uri = path_file.readText().toUri()
                if (DocumentFile.fromSingleUri(this.context, uri)?.exists() == true) {
                    uri
                } else {
                    null
                }
            } else {
                null
           },
            FileInputStream(this._bkp_path).readBytes()
        )
    }

    /**
     * Clear backed up project.
     */
    fun delete_backup() {
        File(this._bkp_path).let { file ->
            if (file.exists()) {
                file.delete()
            }
        }

        File(this._bkp_path_path).let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * Check if there is currently a project backed up in the backup location.
     */
    fun has_backup_saved(): Boolean {
        return File(this._bkp_path).exists()
    }
}